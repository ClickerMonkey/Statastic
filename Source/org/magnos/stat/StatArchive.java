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

import java.util.Iterator;

import org.magnos.data.DataArray;
import org.magnos.data.DataSet;
import org.magnos.data.Store;
import org.magnos.data.var.IntVar;
import org.magnos.data.var.LongVar;


/**
 * An archive contains a fixed number of points. Each point contains a summary
 * for each statistic added to that point in the interval of time. When a point
 * must be added when the archive is full the oldest point is overwritten.
 * 
 * <pre>
 * An example where pointCount = 15 and pointerIndex = 3
 *  ___________________________ _______________________________
 * |                           |                               |
 * | interval(ms)         duration(ms)                         |
 * |    _|_                                                    |
 * |   |   |   pointerTime(ms)                                 |
 * |   |   |       |                                           |
 * +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
 * | 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10| 11| 12| 13| 14| absolute index
 * +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
 * | 11| 12| 13| 14| 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10| relative index
 * +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
 *               |
 *          pointerIndex (newest point)
 * </pre>
 * 
 * @author Philip Diffenderfer
 *
 */
public class StatArchive extends DataSet implements Iterable<StatPoint>
{
	
	// The amount of time between points.
	private final LongVar interval;
	
	// The total number of points in the archive.
	private final IntVar pointCount;
	
	// The time of the last point.
	private final LongVar pointerTime;
	
	// The index of the last point.
	private final IntVar pointerIndex;
	
	// The format of the database and archive.
	private final StatFormat format;
	
	// The array of points, which are lazy loaded (not cached in memory).
	private final DataArray<StatPoint> points;
	
	// The index of the archive in the database.
	private final int index;
	
	
	/**
	 * Instantiates a new StatArchive.
	 * 
	 * @param store
	 * 		The store holding the StatArchive.
	 * @param format
	 * 		The format of the datase and this archive.
	 * @param index
	 * 		The index of this archive in the database.
	 */
	protected StatArchive(Store store, StatFormat format, int index) 
	{
		super(format.getArchiveSize(index));

		this.format = format;
		this.index = index;
		
		// Instantiate the vars.
		this.interval = new LongVar();
		this.pointCount = new IntVar();
		this.pointerTime = new LongVar();
		this.pointerIndex = new IntVar();
		this.points = DataArray.create(StatPoint.class, format.getArchivePoints(index), true);
		
		// Form the data set.
		this.setStore(store);
		this.setLocation(format.getArchiveHeaderOffset(index));
		this.add(interval, pointCount, pointerTime, pointerIndex, points);
		this.read();
	}
	
	/**
	 * Adds the statistic to this archive.
	 * 
	 * @param event
	 * 		The event which states to add the statistic to this archive.
	 */
	protected void addEvent(StatEvent event) 
	{
		// Get the relative index where the statistic should be added to.
		int relative = getRelativeIndex(event.getTime());
		
		// If the point has expired, ignore event.
		if (relative <= -pointCount.get()) {
			return;
		}
	
		// Get the actual index of the place to add the statistic.
		int absolute = index(relative + pointerIndex.get());

		// If the statistic forces old points to be overwritten...
		if (relative > 0) {
			// Clear the given number of points.
			clear(relative, absolute);
		}

		// Get the point, add the statistic, and write it.
		StatPoint point = points.get(absolute);
		point.add(event.getStatistic());
		points.set(absolute, point);
	}
	
	/**
	 * Starts one point after the pointer and clears "relative" number of points. 
	 * 
	 * @param relative
	 * 		The relative index of the point for the statistic.
	 * @param absolute
	 * 		The absolute index of the point for the statistic.
	 */
	private void clear(int relative, int absolute)
	{
		// Get the last pointer index....
		int current = pointerIndex.get();
		
		// Restrict the number of points to clear to the max.
		int max = Math.min(relative, pointCount.get());

		// Loop through each point and overwrite it.
		for (int i = 0; i < max; i++) {
			// The new (actual) index.
			current = index(current + 1);
			
			// Get the point, clear it, and write it.
			StatPoint point = points.get(current);
			point.clear();
			points.set(current, point);
		}
		
		// Increment the pointer time.
		pointerTime.add(interval.get() * relative);
		pointerTime.write(getLocation());
		
		// Set the pointer index.
		pointerIndex.set(absolute);
		pointerIndex.write(getLocation());
	}
	
	/**
	 * Returns the valid index of a point.
	 * 
	 * @param i
	 * 		The given index.
	 * @return
	 * 		The valid index.
	 */
	private int index(int i) 
	{
		return ((i < 0) ? (i - pointCount.get()) : i) % pointCount.get();
	}
	
	/**
	 * Returns an index relative to the pointer in the archive of where a
	 * point with the given time exists.
	 * 
	 * @param time
	 * 		The time in the archive to get the index from.
	 * @return
	 * 		The relative index of the point in the archive.
	 */
	private int getRelativeIndex(long time) 
	{
		return (int)Math.ceil((time - pointerTime.get()) / interval.get());
	}
	
	/**
	 * Returns the interval of the archive. An interval is the amount of time
	 * in a single point.
	 *  
	 * @return
	 * 		The interval in milliseconds.
	 */
	public long getInterval() 
	{
		return interval.get();
	}

	/**
	 * Returns the duration of the archive. This represents the amount of time
	 * this archive holds.
	 * 
	 * @return
	 * 		The duration in milliseconds.
	 */
	public long getDuration() 
	{
		return interval.get() * pointCount.get();
	}

	/**
	 * Returns the time of the last point which had a statistic added to it.
	 * 
	 * @return
	 * 		The pointer time in milliseconds.
	 */
	public long getPointerTime() 
	{
		return pointerTime.get();
	}

	/**
	 * Returns the index of the last point which had a statistic added to it.
	 * 
	 * @return
	 * 		The absolute index of the point.
	 */
	public int getPointerIndex() 
	{
		return pointerIndex.get();
	}

	/**
	 * Returns the maximum number of points this archive can hold. When another
	 * point is required the oldest point is overwritten.
	 * 
	 * @return
	 * 		The number of points.
	 */
	public int getPointCount() 
	{
		return pointCount.get();
	}
	
	/**
	 * Returns the index of this archive in the database.
	 * 
	 * @return
	 * 		The index of this archive.
	 */
	public int getIndex() 
	{
		return index;
	}
	
	/**
	 * Returns the format of the database and this archive.
	 * 
	 * @return
	 * 		The reference to the format.
	 */
	public StatFormat getFormat() 
	{
		return format;
	}
	
	/**
	 * Gets the point at the given relative index. The point at 0 is the oldest
	 * point and the last point in the archive is the newest point.
	 * 
	 * @param index
	 * 		The relative index of the point.
	 * @return
	 * 		The reference to the point at the given index. This should not be
	 *		written to its store, it will not validly change the point in the
	 *		archive.
	 */
	public StatPoint getPoint(int index) 
	{
		StatPoint pt = points.get(index(pointerIndex.get() + index + 1));
		pt.setStartTime(pointerTime.get() + interval.get() * index);
		pt.setEndTime(pt.getStartTime() + interval.get());
		return pt;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Iterator<StatPoint> iterator() 
	{
		return new PointIterator();
	}
	
	/**
	 * An iterator of the points in the archive.
	 * 
	 * @author Philip Diffenderfer
	 *
	 */
	private class PointIterator implements Iterator<StatPoint> 
	{
		int offset = pointerIndex.get();
		int index = 0;
		public boolean hasNext() {
			return (index < pointCount.get());
		}
		public StatPoint next() {
			return points.get(index(++index + offset));
		}
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
	                                 
	
}
