/* 
 * NOTICE OF LICENSE
 * 
 * This source file is subject to the Open Software License (OSL 3.0) that is 
 * bundled with this package in the file LICENSE.txt. It is also available 
 * through the world-wide-web at http://opensource.org/licenses/osl-3.0.php
 * If you did not receive a copy of the license and are unable to obtain it 
 * through the world-wide-web, please send an email to pdiffenderfer@gmail.com 
 * so we can send you a copy immediately. If you use any of this software please
 * notify me via my website or email, your feedback is much appreciated. 
 * 
 * @copyright   Copyright (c) 2011 Magnos Software (http://www.magnos.org)
 * @license     http://opensource.org/licenses/osl-3.0.php
 * 				Open Software License (OSL 3.0)
 */

package org.magnos.stat;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Set;

import javax.imageio.ImageIO;

public class StatExport 
{

	public static enum Type 
	{
		PNG, JPG, BMP, CSV;
	}
	
	public static void export(StatArchive archive, Graphics2D gr)
	{
		Rectangle bounds = gr.getClipBounds();
		if (bounds != null) {
			gr = (Graphics2D)gr.create(bounds.x, bounds.y, bounds.width, bounds.height);
		}
		
		int pointCount = archive.getPointCount();
		long[] totals = new long[pointCount];
		double[] sums = new double[pointCount];
		double[] avgs = new double[pointCount];
		float[] mins = new float[pointCount];
		float[] maxs = new float[pointCount];
		long[] times = new long[pointCount];
		
		// Cache all points
		for (int i = 0; i < pointCount; i++) {
			StatPoint p = archive.getPoint(i);
			
			totals[i] = p.getTotal();
			if (totals[i] > 0) {
				sums[i] = p.getSum();
				mins[i] = p.getMin();
				maxs[i] = p.getMax();
				avgs[i] = p.getAverage();
			}	
			times[i] = p.getEndTime();
		}
		
		// Average of all data points.
		double mean = getMean(totals, avgs);

		// If the mean is NaN there exist no statistics to export.
		if (Double.isNaN(mean)) {
			System.out.println("Archive is empty!");
			return;
		}
		
//		System.out.format("mean: %f\n", mean);
		
		// Calculate the standard deviation.
		double stddev = getStdDev(avgs, totals, mean);

//		System.out.format("stddev: %f\n", stddev);
		
		// The standard deviations away the average, min, and max are.
		double[] stddevAvg = new double[pointCount];
		double[] stddevMin = new double[pointCount];
		double[] stddevMax = new double[pointCount];

		// The maximum and minimum points as standard deviations
		double maxStddev = -Double.MAX_VALUE;
		double minStddev = +Double.MAX_VALUE;
		
		for (int i = 0; i < pointCount; i++) {
			if (totals[i] > 0) {
				stddevAvg[i] = (avgs[i] - mean) / stddev;
				stddevMin[i] = (mins[i] - mean) / stddev;
				stddevMax[i] = (maxs[i] - mean) / stddev;
			
				maxStddev = Math.max(maxStddev, stddevMax[i]);
				minStddev = Math.min(minStddev, stddevMin[i]);
			}
		}

		// The maximum and minimum deviation should at least be 3 away
		maxStddev = Math.max(maxStddev, +stddev)*1.1;
		minStddev = Math.min(minStddev, -stddev)*1.1;
		
//		System.out.format("max stdev: %f\n", maxStddev);
//		System.out.format("min stdev: %f\n", minStddev);
		
		// Finally drawing!
		int height = bounds.height;
		int width = bounds.width;
		double gap = width / (pointCount - 1);
		int moved = -1;
		GeneralPath minPath = new GeneralPath();
		GeneralPath maxPath = new GeneralPath();
		GeneralPath avgPath = new GeneralPath();
		
		gr.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		gr.setStroke(new BasicStroke(2f));
		gr.setColor(Color.white);
		gr.fillRect(0, 0, width, height);
		
		gr.setColor(Color.black);
		gr.draw(new Line2D.Double(0, getY(0, minStddev, maxStddev)*height, 
				width, getY(0, minStddev, maxStddev)*height));
		
		gr.setColor(Color.darkGray);
		gr.draw(new Line2D.Double(0, getY(+stddev, minStddev, maxStddev)*height, 
				width, getY(+stddev, minStddev, maxStddev)*height));
		gr.draw(new Line2D.Double(0, getY(-stddev, minStddev, maxStddev)*height, 
				width, getY(-stddev, minStddev, maxStddev)*height));
		
		gr.setColor(Color.gray);
		gr.draw(new Line2D.Double(0, getY(+stddev*2, minStddev, maxStddev)*height, 
				width, getY(+stddev*2, minStddev, maxStddev)*height));
		gr.draw(new Line2D.Double(0, getY(-stddev*2, minStddev, maxStddev)*height, 
				width, getY(-stddev*2, minStddev, maxStddev)*height));
		
		gr.setColor(Color.lightGray);
		gr.draw(new Line2D.Double(0, getY(+stddev*3, minStddev, maxStddev)*height, 
				width, getY(+stddev*3, minStddev, maxStddev)*height));
		gr.draw(new Line2D.Double(0, getY(-stddev*3, minStddev, maxStddev)*height, 
				width, getY(-stddev*3, minStddev, maxStddev)*height));
		
		
		boolean open = false;
		
		double x = 0;
		for (int i = 0; i < pointCount; i++) {
			if (totals[i] == 0) {
				if (open) {
					
					GeneralPath fillMax = new GeneralPath(maxPath);
					GeneralPath fillMin = new GeneralPath(minPath);
					double x0 = x - gap;
					for (int j = i - 1; j >= moved; j--) {
						fillMax.lineTo(x0, getY(stddevAvg[j], minStddev, maxStddev) * height);
						fillMin.lineTo(x0, getY(stddevAvg[j], minStddev, maxStddev) * height);
						x0 -= gap;
					}
					fillMax.closePath();
					fillMin.closePath();

					gr.setColor(new Color(0, 255, 0, 100));
					gr.fill(fillMax);
					gr.setColor(new Color(0, 0, 255, 100));
					gr.fill(fillMin);
					
					// wrap up minimum traveling back to lastValid
					gr.setColor(Color.green);
					gr.draw(maxPath);
					gr.setColor(Color.blue);
					gr.draw(minPath);
					gr.setColor(Color.red);
					gr.draw(avgPath);
					
					maxPath.reset();
					minPath.reset();
					avgPath.reset();
					open = false;
				}
			}
			else {
				if (!open) {
					// move to
					minPath.moveTo(x, getY(stddevMin[i], minStddev, maxStddev) * height);
					maxPath.moveTo(x, getY(stddevMax[i], minStddev, maxStddev) * height);
					avgPath.moveTo(x, getY(stddevAvg[i], minStddev, maxStddev) * height);
					moved = i;
				}
				else {
					// line to
					minPath.lineTo(x, getY(stddevMin[i], minStddev, maxStddev) * height);
					maxPath.lineTo(x, getY(stddevMax[i], minStddev, maxStddev) * height);
					avgPath.lineTo(x, getY(stddevAvg[i], minStddev, maxStddev) * height);
				}
				open = true;
			}
			x += gap;
		}
		if (open) {
			GeneralPath fillMax = new GeneralPath(maxPath);
			GeneralPath fillMin = new GeneralPath(minPath);
			double x0 = x - gap;
			for (int j = pointCount - 1; j >= moved; j--) {
				fillMax.lineTo(x0, getY(stddevAvg[j], minStddev, maxStddev) * height);
				fillMin.lineTo(x0, getY(stddevAvg[j], minStddev, maxStddev) * height);
				x0 -= gap;
			}
			fillMax.closePath();
			fillMin.closePath();

			gr.setColor(new Color(0, 255, 0, 100));
			gr.fill(fillMax);
			gr.setColor(new Color(0, 0, 255, 100));
			gr.fill(fillMin);
			
			// wrap up minimum traveling back to lastValid
			gr.setColor(Color.green);
			gr.draw(maxPath);
			gr.setColor(Color.blue);
			gr.draw(minPath);
			gr.setColor(Color.red);
			gr.draw(avgPath);
		}
	}
	
	private static double getY(double y, double min, double max) {
		return 1.0 - ((y - min) / (max - min));
	}
	
	private static double getMean(long[] total, double[] avg)
	{
		double mean = 0.0;
		int points = 0;
		for (int i = 0; i < total.length; i++) {
			if (total[i] > 0) {
				points++;
				mean += avg[i];
			}
		}

		if (points == 0) {
			return Double.NaN;
		}
		
		// Average of all data points.
		return mean / points;
	}
	
	private static double getStdDev(double[] avg, long[] total, double mean)
	{
		double dv, variance = 0.0;
		int points = -1;
		for (int i = 0; i < avg.length; i++) {
			if (total[i] > 0) {
				points++;
				dv = avg[i] - mean;
				variance += dv * dv;
			}
		}
		return (points > 0 ? Math.sqrt(variance / points) : 0.0);
	}
	
	public static void export(StatArchive archive, Type type, File file) throws IOException
	{
//		if (!file.exists()) {
//			file.createNewFile();
//		}
		
		switch (type) {
		case CSV:
			exportCsv(archive, file);
			break;
		case PNG:
		case BMP:
		case JPG:
			exportImage(archive, type, file);
			break;
		}
	}
	
	private static void exportCsv(StatArchive archive, File file) throws IOException 
	{
		PrintStream out = new PrintStream(file);
		DateFormat df = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss.SSS a");
		try {
			out.format("index,start,end,total,sum,avg,min,max\n");
			int pointCount = archive.getPointCount();
			for (int i = 0; i < pointCount; i++) 
			{
				StatPoint p = archive.getPoint(i);
				
				String start = df.format(new Date(p.getStartTime()));
				String end = df.format(new Date(p.getEndTime()));
				
				out.format("%d,%s,%s,%d,%f,%f,%f,%f\n", i, start, end, p.getTotal(), 
						p.getSum(), p.getAverage(), p.getMin(), p.getMax());
			}
		}
		finally {
			out.close();
		}
	}

	private static void exportImage(StatArchive archive, Type type, File file) throws IOException
	{
		BufferedImage image = new BufferedImage(400, 300, BufferedImage.TYPE_INT_ARGB);
		Graphics2D gr = (Graphics2D)image.getGraphics();
		gr.setClip(0, 0, 400, 300);
		export(archive, gr);
		ImageIO.write(image, type.name(), file);
	}
	
	public static void export(StatDatabase database, Type type, File file) throws IOException
	{
		for (StatArchive arc : database) {
			export(arc, type, new File(file, database.getName() + arc.getIndex()));
		}
	}
	
	public static void export(StatGroup group, Type type, File file) throws IOException
	{
		Set<StatDatabase> dbs = group.getDatabases();
		for (StatDatabase db : dbs) {
			export(db, type, file);
		}
	}
	
}
