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

import org.junit.Test;
import org.magnos.service.Service;
import org.magnos.stat.StatArchive;
import org.magnos.stat.StatDatabase;
import org.magnos.stat.StatFormat;
import org.magnos.stat.StatPoint;
import org.magnos.stat.StatService;
import org.magnos.test.BaseTest;


public class TestStatArchive extends BaseTest 
{

	@Test
	public void testOverwrite()
	{
		StatService service = StatService.get();
		service.waitFor(Service.Running);
		
		StatFormat format = new StatFormat(1);
		format.set(0, 10, 100);
		
		StatDatabase data = StatDatabase.inMemory("test", format);
		data.setEnable(true);
		
		StatArchive archive = data.getArchive(0);
		for (int i = 0; i < 2000; i++) {
			data.add(rnd.nextFloat());
			sleep(1);
		}
		// should be filled with data (approx. 10 stats a point)
		output(archive);
		
		sleep(500);
		data.add(rnd.nextFloat());
		sleep(200);
		// half cleared before the last point
		output(archive);
		
		sleep(2000);
		for (int i = 0; i < 100; i++) {
			data.add(rnd.nextFloat());
		}
		sleep(200);
		// all cleared, the last point(s) should be 100
		output(archive);
	}
	
	private void output(StatArchive archive) 
	{
		for (StatPoint sp : archive) {
			System.out.println(sp);
		}
		System.out.println();
	}
	
}
