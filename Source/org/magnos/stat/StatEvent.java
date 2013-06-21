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

import java.util.LinkedList;
import java.util.List;

/**
 * A StatisticEvent is triggered when a statistic (float) is added to a 
 * StatisticDatabase. Once an event is created it is given to the
 * StatisticService which will actually perform adding the statistic to the
 * archives in the given set of databases (targets). 
 * 
 * @author Philip Diffenderfer
 *
 */
public class StatEvent 
{

	// The list of databases to add the statistic to.
	private final List<StatDatabase> targets;
	
	// The time the statistic event was added.
	private final long time;
	
	// The statistic to add to the target databases.
	private final float statistic;
	
	
	/**
	 * Instantiates a new Statistic Event.
	 * 
	 * @param statistic
	 * 		The statistic to add to the target databases.
	 * @param target
	 * 		The inital database to add the statistic to.
	 */
	public StatEvent(float statistic, StatDatabase target) 
	{
		this(statistic);
		this.addTarget(target);
	}

	/**
	 * Instantiates a new Statistic Event.
	 * 
	 * @param statistic
	 * 		The statistics to add to the target databases.
	 */
	public StatEvent(float statistic) 
	{
		this.statistic = statistic;
		this.time = System.currentTimeMillis();
		this.targets = new LinkedList<StatDatabase>();
	}
	
	/**
	 * Executes this event by adding itself to all of its targets.
	 */
	protected void execute()
	{
		for (StatDatabase target : targets) {
			target.addEvent(this);
		}
	}
	
	/**
	 * Adds this event to the service to be executed. This can be invoked any 
	 * number of times but in typical cases should only be executed once.
	 */
	public void process()
	{
		StatService.get().addEvent(this);
	}
	
	/**
	 * Adds a database as a target to add the statistic to if this event has
	 * not been handled by the service.
	 * 
	 * @param target
	 * 		The database to add the statistic to.
	 */
	public void addTarget(StatDatabase target) 
	{
		targets.add(target);
	}
	
	/**
	 * Returns the list of databases to add the statistic to.
	 *  
	 * @return
	 * 		The reference to the internal list of targets.
	 */
	public List<StatDatabase> getTargets() 
	{
		return targets;
	}
	
	/**
	 * The exact time this event was created.
	 * 
	 * @return
	 * 		The time in milliseconds since the Unix Epoch.
	 */
	public long getTime() 
	{
		return time;
	}
	
	/**
	 * The statistic to add to the target databases.
	 *  
	 * @return
	 * 		The value of the statistic.
	 */
	public float getStatistic() 
	{
		return statistic;
	}
	
}
