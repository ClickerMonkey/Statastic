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

import static org.junit.Assert.*;

import java.util.Set;


import org.junit.Before;
import org.junit.Test;
import org.magnos.data.store.factory.MemoryStoreFactory;
import org.magnos.stat.StatArchive;
import org.magnos.stat.StatDatabase;
import org.magnos.stat.StatEvent;
import org.magnos.stat.StatFormat;
import org.magnos.stat.StatGroup;
import org.magnos.stat.StatPoint;
import org.magnos.stat.StatService;
import org.magnos.stat.StatTarget;
import org.magnos.test.BaseTest;

public class TestStatDatabase extends BaseTest 
{

	private StatService service;
	private StatFormat format;
	private StatGroup group;
	
	@Before
	public void testBefore()
	{
		service = StatService.get();
		
		format = new StatFormat(2);
		format.set(0, 50, 40);		// every 50ms for 2 sec
		format.set(1, 1000, 60); 	// every 1 sec for 1 min
		format.compile();
		
		group = new StatGroup("test");
		group.setFactory(new MemoryStoreFactory());
		group.setEnableDefault(true);
		group.setFormatDefault(format);
	}
	
	@Test
	public void testAdd()
	{
		final int TOTAL = 1000;
		float[] data = random(TOTAL);
		
		StatDatabase db1 = group.take("db1");
		
		for (float x : data) {
			db1.add(x);
			sleep(1);
		}
		waitForEvents(db1);
		
		contains(data, db1.getArchive(0));
		contains(data, db1.getArchive(1));
		
		for (StatPoint sp : db1.getArchive(0)) {
			System.out.println(sp);
		}
		
		Set<StatDatabase> dbs = group.delete(StatTarget.This);
		for (StatDatabase db : dbs) {
			assertFalse( db.exists() );
		}
	}
	
	@Test
	public void testMultipleTargets()
	{
		float[] data = {-45.367f};
		
		StatDatabase db2 = group.take("db2");
		StatDatabase db3 = group.take("db3");
		
		StatEvent se = db2.getEvent(data[0]);
		se.addTarget(db3);
		se.process();
		
		waitForEvents(db2, db3);

		contains(data, db2.getArchive(0));
		contains(data, db2.getArchive(1));
		contains(data, db3.getArchive(0));
		contains(data, db3.getArchive(1));
		
		Set<StatDatabase> dbs = group.delete(StatTarget.All);
		for (StatDatabase db : dbs) {
			assertFalse( db.exists() );
		}
	}
	
	
	private void contains(float[] data, StatArchive archive) {
		long dataTotal = data.length;
		double dataSum = 0.0;
		float dataMin = Float.MAX_VALUE;
		float dataMax = -Float.MAX_VALUE;
		for (float x : data) {
			dataSum += x;
			dataMin = Math.min(dataMin, x);
			dataMax = Math.max(dataMax, x);
		}
		
		long pointTotal = 0;
		double pointSum = 0.0;
		float pointMin = Float.MAX_VALUE;
		float pointMax = -Float.MAX_VALUE;
		for (StatPoint sp : archive) {
			pointTotal += sp.getTotal();
			pointSum += sp.getSum();
			pointMin = Math.min(pointMin, sp.getMin());
			pointMax = Math.max(pointMax, sp.getMax());
		}
		
		assertEquals( pointTotal, dataTotal );
		assertEquals( pointSum, dataSum, 0.000001 );
		assertEquals( pointMin, dataMin, 0.000001 );
		assertEquals( pointMax, dataMax, 0.000001 );
	}
	
	private void waitForEvents(StatDatabase ... dbs) {
		sleep(100);
		do {
			sleep(100);
		} while (service.getEventQueue().size() > 0);
		
		for (StatDatabase db : dbs) {
			db.getStore().flush();
		}
		sleep(100);
	}
	
	private float[] random(int count) {
		float[] data = new float[count];
		while (--count >= 0) {
			data[count] = rnd.nextFloat();
		}
		return data;
	}
	
	
}
