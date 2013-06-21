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

/**
 * A problem occurred parsing an existing Store with a given StatFormat.
 * 
 * @author Philip Diffenderfer
 *
 */
public class StatFormatException extends RuntimeException 
{
	
	/**
	 * The format which thrown the exception.
	 */
	private StatFormat format;
	
	
	/**
	 * Instantiates a new StatFormatException.
	 * 
	 * @param format
	 * 		The format thats throwing the exception.
	 * @param message
	 * 		The message describing the reason for throwing the exception.
	 */
	public StatFormatException(StatFormat format, String message) 
	{
		super(message);
		this.format = format;
	}
	
	/**
	 * Returns the format that threw this exception.
	 */
	public StatFormat getFormat() 
	{
		return format;
	}
	
}
