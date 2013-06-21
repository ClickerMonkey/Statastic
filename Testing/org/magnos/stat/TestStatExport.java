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

import java.io.File;
import java.io.IOException;

import org.junit.Test;
import org.magnos.stat.StatDatabase;
import org.magnos.stat.StatExport;
import org.magnos.stat.StatFormat;
import org.magnos.stat.StatGroup;
import org.magnos.stat.StatService;
import org.magnos.stat.StatExport.Type;
import org.magnos.test.BaseTest;


public class TestStatExport extends BaseTest 
{

	@Test
	public void testExportImage() throws IOException
	{
		StatService.get();

		StatFormat sf = new StatFormat(1);
		sf.set(0, 20, 50);
		sf.compile();
		
		StatGroup grp = new StatGroup("Testing");
		grp.setEnableDefault(true);
		grp.setFormatDefault(sf);
		
		StatDatabase sdb = grp.take("sdb");
		
		for (int i = 0; i < 1000; i++) {
			sdb.add((float)Math.random() * 100);
			sleep(1);
		}

		// Wait for service to handle all events
		sleep(3000);
		
		StatExport.export(sdb.getArchive(0), Type.CSV, new File("sdb.csv"));
		
		StatExport.export(sdb.getArchive(0), Type.PNG, new File("sdb.png"));
		
		grp.delete();
	}
	
}
