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

import org.magnos.data.Data;
import org.magnos.data.DataSet;
import org.magnos.data.var.DoubleVar;
import org.magnos.data.var.FloatVar;
import org.magnos.data.var.LongVar;

/**
 * A single statistical point in an archive. A point can hold several statistics
 * by keeping a running summary of the statistics added to the point:
 * <ol>
 * <li>Total statistics added to the point</li>
 * <li>The sum of the statistics</li>
 * <li>The average of the statistics (sum / total)</li>
 * <li>The smallest statistic added</li>
 * <li>The largest statistic added</li>
 * </ol>
 * 
 * @author Philip Diffenderfer
 *
 */
public class StatPoint extends DataSet
{
	
	/**
	 * The size of the StatPoint in memory in bytes.
	 */
	public static final int SIZE = 24;
	
	
	// Total statistics added to the point 
	private final LongVar total;
	
	// The sum of the statistics
	private final DoubleVar sum;
	
	// The smallest statistic added
	private final FloatVar min;
	
	// The largest statistic added
	private final FloatVar max;
	
	// The starting time of this point.
	private long startTime;
	
	// The ending time of this point.
	private long endTime;
	
	/**
	 * Instantiates a new StatPoint.
	 */
	public StatPoint() 
	{
		super(SIZE);
		
		this.total = new LongVar(0);
		this.sum = new DoubleVar(0.0);
		this.min = new FloatVar(+Float.MAX_VALUE);
		this.max = new FloatVar(-Float.MAX_VALUE);
		
		this.add(total, sum, min, max);
	}
	
	/**
	 * Adds a statistic to this point.
	 * 
	 * @param value
	 * 		The statistic to add.
	 */
	protected void add(float value) 
	{
		total.add(1);
		sum.add(value);
		min.min(value);
		max.max(value);
	}
	
	/**
	 * Removes all statistics from this point.
	 */
	protected void clear()
	{
		total.set(0);
		sum.set(0.0);
		min.set(+Float.MAX_VALUE);
		max.set(-Float.MAX_VALUE);
	}
	
	/**
	 * Sets the end time of this point in milliseconds.
	 * 
	 * @param time
	 * 		This points time.
	 */
	protected void setEndTime(long time)
	{
		this.endTime = time;
	}
	
	/**
	 * Sets the start time of this point in milliseconds.
	 * 
	 * @param time
	 * 		This points time.
	 */
	protected void setStartTime(long time)
	{
		this.startTime = time;
	}
	
	/**
	 * Returns the number of statistics added to this point. 
	 */
	public long getTotal() 
	{
		return total.get();
	}
	
	/**
	 * Returns the sum of all statistics added to this point.
	 */
	public double getSum() 
	{
		return sum.get();
	}
	
	/**
	 * Returns the average of all statistics added to this point.
	 */
	public double getAverage() 
	{
		return sum.get() / total.get();
	}

	/**
	 * Returns the smallest statistic added to this point.
	 */
	public float getMin() 
	{
		return min.get();
	}

	/**
	 * Returns the largest statistic added to this point.
	 */
	public float getMax() 
	{
		return max.get();
	}
	
	/**
	 * Returns the ending time of this point.
	 * 
	 * @return
	 * 		The ending time of this point in milliseconds since the Unix epoch.
	 */
	public long getEndTime()
	{
		return endTime;
	}
	
	/**
	 * Returns the starting time of this point.
	 * 
	 * @return
	 * 		The starting time of this point in milliseconds since the Unix epoch.
	 */
	public long getStartTime()
	{
		return startTime;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Data copy() 
	{
		StatPoint copy = new StatPoint();
		copy.setStore(getStore());
		copy.setLocation(getLocation());
		return copy;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() 
	{
		return String.format("{total: %d, sum: %.3f, avg: %.3f, min: %.2f, max: %.2f}",
				getTotal(), getSum(), getAverage(), getMin(), getMax());
	}
	
}
