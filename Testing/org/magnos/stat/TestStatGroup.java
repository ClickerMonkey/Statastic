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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.magnos.data.store.factory.FileStoreFactory;
import org.magnos.stat.StatDatabase;
import org.magnos.stat.StatFormat;
import org.magnos.stat.StatGroup;
import org.magnos.stat.StatTarget;
import org.magnos.test.BaseTest;
import org.magnos.test.Ordered;
import org.magnos.test.OrderedRunner;


@RunWith(value=OrderedRunner.class)
public class TestStatGroup extends BaseTest 
{

	private final StatFormat format;
	private final StatGroup group;
	
	public TestStatGroup()
	{
		format = new StatFormat(2);
		format.set(0, 1000, 60); 	// every 1 sec for 1 min
		format.set(1, 60000, 10); 	// every 1 min for 10 min
		format.compile();
		
		group = new StatGroup("test");
		group.setFactory(new FileStoreFactory());
		group.setFormatDefault(format);
	}
	
	@Test @Ordered(index=0)
	public void testRoot() 
	{
		StatGroup root = StatGroup.getRoot();
		
		assertNotNull( root );
	
		assertTrue( root.exists() );
	}
	
	@Test @Ordered(index=1)
	public void testCreation() 
	{
		assertTrue( group.exists() );
		
		StatDatabase rrd1 = group.take("rrd1");
		assertTrue( rrd1.exists() );
		
		StatDatabase rrd1b = group.take("rrd1");
		assertSame( rrd1, rrd1b );
		
		StatDatabase rrd1c = group.get("rrd1");
		assertSame( rrd1, rrd1c );
		
		StatDatabase rrd2 = group.take("rrd2");
		assertTrue( rrd2.exists() );
		
		assertEquals( 2, group.size() );

		group.close();
	}
	
	@Test @Ordered(index=2)
	public void testLoad()
	{
		Set<StatDatabase> dbs = group.load();
		assertEquals( 2, dbs.size() );
		assertEquals( 2, group.size() );
		
		StatDatabase rrd1 = group.get("rrd1");
		assertNotNull( rrd1 );
		assertTrue( dbs.contains(rrd1) );
		
		StatDatabase rrd2 = group.get("rrd2");
		assertNotNull( rrd2 );
		assertTrue( dbs.contains(rrd2) );

		group.close();
	}
	
	@Test @Ordered(index=3)
	public void testEnabled()
	{
		group.load();
		
		assertEquals( 2, group.size() );
		assertEquals( 0, group.getEnabled() );
		
		group.setEnabled(true);
		
		assertEquals( 2, group.getEnabled() );

		group.close();
	}
	
	@Test @Ordered(index=3)
	public void testEnabledChildren()
	{
		group.load();
		
		StatGroup c1 = group.getChild("c1");
		assertTrue( c1.exists() );
		
		StatDatabase db1 = c1.take("db1");
		assertTrue( db1.exists() );
		
		StatDatabase db2 = c1.take("db2");
		assertTrue( db2.exists() );
		
		assertEquals( 2, group.size() );
		assertEquals( 0, group.getEnabled(StatTarget.All) );

		group.setEnabled(true, StatTarget.All);

		assertEquals( 4, group.getEnabled(StatTarget.All) );
		
		c1.close();
		group.close();
	}
	
	@Test @Ordered(index=4)
	public void testClearClose()
	{
		group.load(StatTarget.All);

		assertEquals( 2, group.size() );
		assertEquals( 4, group.size(StatTarget.All) );
		
		Set<StatDatabase> dbs = group.close(StatTarget.All);
		
		assertEquals( 4, dbs.size() );
		assertEquals( 0, group.size() );
	}
	
	@Test @Ordered(index=5)
	public void testDelete()
	{
		Set<StatDatabase> dbs = group.load(StatTarget.All);
		assertEquals( 4, dbs.size() );
		
		group.delete(StatTarget.All);
		
		for (StatDatabase db : dbs) {
			assertFalse( db.exists() );
		}
		
		assertFalse( group.exists() );
	}
	
}
