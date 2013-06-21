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

import org.junit.Test;
import org.magnos.data.Store;
import org.magnos.data.StoreAccess;
import org.magnos.data.store.MemoryStore;
import org.magnos.stat.StatPoint;
import org.magnos.test.BaseTest;


public class TestStatPoint extends BaseTest 
{

	@Test
	public void testReadAndWrite()
	{
		Store store = new MemoryStore("Testing", 300);
		store.open(StoreAccess.ReadWrite);
		
		StatPoint sp1 = new StatPoint();
		sp1.add(4f);
		sp1.add(5f);
		sp1.add(3f);
		sp1.write(store);
		
		sp1.add(1f);
		sp1.write(24, store);
		
		assertEquals( 4, sp1.getTotal() );
		assertEquals( 3.25, sp1.getAverage(), 0.0000001 );
		assertEquals( 5.0, sp1.getMax(), 0.0000001 );
		assertEquals( 1.0, sp1.getMin(), 0.0000001 );
		
		StatPoint sp2 = new StatPoint();
		sp2.read(store);
		
		assertEquals( 3, sp2.getTotal() );
		assertEquals( 4.0, sp2.getAverage(), 0.0000001 );
		assertEquals( 5.0, sp2.getMax(), 0.0000001 );
		assertEquals( 3.0, sp2.getMin(), 0.0000001 );
		
		sp2.read(24, store);

		assertEquals( 4, sp2.getTotal() );
		assertEquals( 3.25, sp2.getAverage(), 0.0000001 );
		assertEquals( 5.0, sp2.getMax(), 0.0000001 );
		assertEquals( 1.0, sp2.getMin(), 0.0000001 );
	}
	
}
