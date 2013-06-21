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
 * Specifies the scope to which to perform an operation on a group.
 * 
 * @author Philip Diffenderfer
 *
 */
public enum StatTarget 
{
	
	/**
	 * The scope where only the group being invoked will perform the operation.
	 */
	This(false),
	
	/**
	 * The scope where only the group being invoked and its immediate children 
	 * will perform the operation.
	 */
	Children(This, true),
	
	/**
	 * The scope where the group being involved and all of its children (and
	 * their children etc) perform the operation.
	 */
	All(true);
	
	
	// Whether the children of this target should be included on the operation. 
	private final boolean children;
	
	// The next scope this targets children should use.
	private final StatTarget child;
	
	
	/**
	 * Instantiates a StatTarget that maintains the same scope.
	 *  
	 * @param children
	 * 		Whether the children of this target should be included on the operation.
	 */
	private StatTarget(boolean children) 
	{
		this.child = this;
		this.children = children;
	}
	
	/**
	 * Instantiates a StatTarget that has a next scope.
	 * 
	 * @param child
	 * 		The next scope this targets children should use.
	 * @param children
	 * 		Whether the children of this target should be included on the operation.
	 */
	private StatTarget(StatTarget child, boolean children) 
	{
		this.child = child;
		this.children = children;
	}
	
	/**
	 * Whether the children of this target should be included on the operation.
	 * 
	 * @return
	 * 		True if the children of the target should be included.
	 */
	public boolean hasChildren() 
	{
		return children;
	}
	
	/**
	 * The next scope this targets children should use.
	 *  
	 * @return
	 * 		The reference to the next scope.
	 */
	public StatTarget getChild() 
	{
		return child;
	}
	
	
}
