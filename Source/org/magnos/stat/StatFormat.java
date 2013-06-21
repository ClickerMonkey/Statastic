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

import org.magnos.data.Store;
import org.magnos.data.StoreAccess;
import org.magnos.data.var.DoubleVar;
import org.magnos.data.var.FloatVar;
import org.magnos.data.var.IntVar;
import org.magnos.data.var.LongVar;
import org.magnos.data.var.StringVar;

/**
 * The format of a database and its archives. A format must be given at the
 * creation of a database to validate the database. If the database is invalid
 * a.k.a. doesn't match this format then an exception is thrown.
 * 
 * <pre>
 * A typical format for a database that tracks data over an extended period of time:
 * 
 * StatFormat sf = new StatFormat(5); // A format that covers a year (365.25 days)
 * sf.set(0, 300205L, 12); 			// one point every 5 minutes for an hour 
 * sf.set(1, 3602467L, 24); 		// one point every hour for a day
 * sf.set(2, 43229589L, 14);		// one point every 12 hours for a week
 * sf.set(3, 86459178L, 31);		// one point every day for a month
 * sf.set(4, 7606876923L, 52);		// one point for every week in a year
 * sf.compile();					// database size = 3328 bytes
 * </pre>
 * 
 * @author Philip Diffenderfer
 * 
 */
public final class StatFormat 
{
	
	/* The number of milliseconds for each unit (with a 365.25 day year)
	 * 		31557600000 = year
	 * 		2629800000  = month
	 * 		606876923   = week
	 * 		86459178.1  = day
	 * 		3602466.75  = hour
	 * 		60041.0959  = min
	 * 		1000.68493  = sec
	 */
	
	
	/**
	 * The size of the database header in bytes.
	 * 		"SRRD"(4) archiveCount(4) creationTime(8)
	 */
	public static final int DATABASE_HEADER_SIZE = 16;
	
	/**
	 * The size of an archives header in bytes. 
	 * 		interval(8), points(4), time(8), index(4) 
	 */
	public static final int ARCHIVE_HEADER_SIZE = 24;
	
	/**
	 * The size of a point in bytes. 
	 * 		total(8) sum(8) min(4) max(4)
	 */
	public static final int POINT_SIZE = StatPoint.SIZE;
	
	
	// The number of archives in the format.
	private final int archiveCount;
	
	// The intervals of each of the archives.
	private final long[] archiveInterval;
	
	// The number of points in each of the archives.
	private final int[] archivePoints;
	
	
	// Whether or not the format is read-write (false) or read only (true).
	private boolean compiled = false;
	
	// The offsets in bytes of each of the archives in the database. This is
	// only set once the format is compiled.
	private final int[] archiveOffset;
	
	// The size of the database in bytes. This is only set once the format is 
	// compiled.
	private int size;
	
	
	/**
	 * Instantiates a new StatFormat.
	 * 
	 * @param archiveCount
	 * 		The number of archives in the database.
	 */
	public StatFormat(int archiveCount) 
	{
		this.archiveCount = archiveCount;
		this.archiveInterval = new long[archiveCount];
		this.archivePoints = new int[archiveCount];
		this.archiveOffset = new int[archiveCount];
	}
	
	/**
	 * Checks whether compiled is equal to the given flag, if its equal a
	 * StatFormatException is thrown.
	 * 
	 * @param match
	 * 		The flag check if its equal to compiled.
	 */
	private void check(boolean match) 
	{
		if (compiled == match) {
			throw new StatFormatException(this, "Invalid access to format");
		}
	}
	
	/**
	 * Sets the format of the archive at the given index.
	 * 
	 * @param index
	 * 		The index of the archive in the database.
	 * @param interval
	 * 		The interval of the archive in milliseconds. 
	 * @param points
	 * 		The number of points in the archive.
	 * @see StatArchive
	 */
	public void set(int index, long interval, int points) 
	{
		// If its already compiled, throw an exception.
		check(true);
		
		archiveInterval[index] = interval;
		archivePoints[index] = points;	
	}
	
	/**
	 * Compiles the format if not compiled already. A compiled format cannot
	 * be modified (its archives interval and points), and if a modification is
	 * attempted an exception is thrown.
	 * 
	 * @return
	 * 		The reference to this StatFormat.
	 */
	public StatFormat compile() 
	{
		// If not already compiled...
		if (!compiled) {
			// Compute the size of the database and the archive offsets.
			int offset = DATABASE_HEADER_SIZE;
			for (int i = 0; i < archiveCount; i++) {
				archiveOffset[i] = offset;
				offset += ARCHIVE_HEADER_SIZE;
				offset += POINT_SIZE * archivePoints[i];
			}
			size = offset;
			compiled = true;
		}
		return this;
	}

	/**
	 * Validates the contents of the store against this format. If this format
	 * is not compiled an exception is thrown immediately. If the given store
	 * does not contain valid data a StatFormatException is thrown. If the
	 * store is valid then true is returned.
	 * 
	 * @param store
	 * 		The store to validate.
	 * @return
	 * 		True if the store is valid.
	 * @throws StatFormatException
	 * 		Thrown if the store format does not match this format.
	 */
	public boolean validate(Store store) throws StatFormatException
	{
		// Ensure the format is compiled.
		check(false);
		
		// Open the store in at least read-only mode.
		store.open(StoreAccess.ReadOnly);
		
		// Check total size
		if (store.capacity() < size) {
			throw new StatFormatException(this, "Not proper size");
		}
		
		// Check tag
		if (!getTag(store).take().equals("SRRD")) {
			throw new StatFormatException(this, "Invalid tag");
		}

		// Check archive count match
		if (getArchiveCount(store).take() != archiveCount) {
			throw new StatFormatException(this, "Negative archives");
		}
		
		// Check non-negative creation time
		if (getCreation(store).take() < 0) {
			throw new StatFormatException(this, "Negative creation time");
		}
		
		// Check archive header
		for (int i = 0; i < archiveCount; i++) 
		{
			LongVar interval = getArchiveInterval(i, store);
			IntVar points = getArchivePoints(i, store);
			LongVar time = getArchiveTime(i, store);
			IntVar index = getArchiveIndex(i, store);
			
			if (interval.take() != archiveInterval[i]) {
				throw new StatFormatException(this, "Archive interval mismatch");
			}
			if (points.take() != archivePoints[i]) {
				throw new StatFormatException(this, "Archive points mismatch");
			}
			if (time.take() < 0) {
				throw new StatFormatException(this, "Negative archive point time");
			}
			if (index.take() < 0 || index.get() >= archivePoints[i]) {
				throw new StatFormatException(this, "Invalid archive point index");
			}
		}
		
		// Check archive points
		for (int i = 0; i < archiveCount; i++) 
		{
			for (int j = 0; j < archivePoints[i]; j++) 
			{
				LongVar ptotal = getPointTotal(i, j, store);
				DoubleVar psum = getPointSum(i, j, store);
				FloatVar pmin = getPointMin(i, j, store);
				FloatVar pmax = getPointMax(i, j, store);
				
				if (ptotal.take() < 0) {
					throw new StatFormatException(this, "Negative point total");
				}
				double average = psum.take() / ptotal.get();
				if (pmin.take() > average) {
					throw new StatFormatException(this, "Invalid point min or average ");
				}
				if (pmax.take() < average) {
					throw new StatFormatException(this, "Invalid point max or average");
				}
			}
		}
		return true;
	}
	
	/**
	 * Writes the initial values of the database to the store. The archives in
	 * the database will contain points with no statistics.
	 * 
	 * @param store
	 * 		The store to write the format to.
	 * @throws StatFormatException
	 * 		An error occurred because this format is not compiled.
	 */
	public void write(Store store) throws StatFormatException 
	{
		// Ensure the format is compiled.
		check(false);
		
		// Right now is the creation of the database
		long currentTime = System.currentTimeMillis();
		
		// We need read-write access to the store.
		store.open(StoreAccess.ReadWrite);
		// Set the capacity of the store to the sice of the database.
		store.capacity(size);
		
		// Add the tag, archive count, and creation time to the database header.
		getTag(store).put("SRRD");
		getArchiveCount(store).put(archiveCount);
		getCreation(store).put(currentTime);
		
		// For each archive in the database...
		for (int i = 0; i < archiveCount; i++) 
		{
			// Write the archives header
			getArchiveInterval(i, store).put(archiveInterval[i]);
			getArchivePoints(i, store).put(archivePoints[i]);
			getArchiveTime(i, store).put(currentTime);
			getArchiveIndex(i, store).put(0);
			
			// Write each point in the header.
			for (int j = 0; j < archivePoints[i]; j++) 
			{
				getPointTotal(i, j, store).put(0);
				getPointSum(i, j, store).put(0.0);
				getPointMin(i, j, store).put(+Float.MAX_VALUE);
				getPointMax(i, j, store).put(-Float.MAX_VALUE);
			}
		}
	}
	
	/**
	 * Returns the number of archives in this format.
	 * 
	 * @return
	 * 		The number of archives in this format.
	 */
	public int getArchiveCount() 
	{
		return archiveCount;
	}
	
	/**
	 * Returns the interval of the given archive. If this format is not compiled
	 * this may return an invalid value.
	 *  
	 * @param index
	 * 		The index of the archive in the database.
	 * @return
	 * 		The interval of the given archive in milliseconds.
	 * @see StatArchive
	 */
	public long getArchiveInterval(int index) 
	{
		return archiveInterval[index];
	}
	
	/**
	 * Returns the number of points in the given archive. If this format is not
	 * compiled this may return an invalid value.
	 * 
	 * @param index
	 * 		The index of the archive in the database.
	 * @return
	 * 		The number of points in the given archive.
	 * @see StatArchive
	 */
	public int getArchivePoints(int index) 
	{
		return archivePoints[index];
	}
	
	/**
	 * Returns the size of an archive. If this format is not compiled this
	 * may return an invalid value.
	 * 
	 * @param index
	 * 		The index of the archive in the database.
	 * @return
	 * 		The size of the archive in bytes.
	 */
	public int getArchiveSize(int index) 
	{
		return archivePoints[index] * POINT_SIZE + ARCHIVE_HEADER_SIZE;
	}
	
	/**
	 * Returns the size of the database. If this format is not compiled this
	 * will return zero.
	 * 
	 * @return
	 * 		The size of the database in bytes.
	 */
	public int getDatabaseSize() 
	{
		return size;
	}
	
	/**
	 * Returns the var for the tag of the database.
	 */
	private StringVar getTag(Store store) 
	{
		return new StringVar(4, store, 0);
	}
	
	/**
	 * Returns the var for the archive count of the database.
	 */
	private IntVar getArchiveCount(Store store) 
	{
		return new IntVar(store, 4);
	}
	
	/**
	 * Returns the var for the creation time of the database.
	 */
	private LongVar getCreation(Store store) 
	{
		return new LongVar(store, 8);
	}
	
	/**
	 * Returns the offset for the header of the archive.
	 */
	protected int getArchiveHeaderOffset(int index) 
	{
		return archiveOffset[index];
	}

	/**
	 * Returns the offset for the interval of the archive.
	 */
	private int getArchiveIntervalOffset(int index) 
	{
		return archiveOffset[index];
	}
	
	/**
	 * Returns the var for the interval of the archive.
	 */
	private LongVar getArchiveInterval(int index, Store store) 
	{
		return new LongVar(store, getArchiveIntervalOffset(index));
	}
	
	/**
	 * Returns the offset for the point total of the archive.
	 */
	private int getArchivePointsOffset(int index) 
	{
		return archiveOffset[index] + 8;
	}
	
	/**
	 * Returns the var for the point total of the archive.
	 */
	private IntVar getArchivePoints(int index, Store store) 
	{
		return new IntVar(store, getArchivePointsOffset(index));
	}
	
	/**
	 * Returns the offset for the pointer time of the archive.
	 */
	private int getArchiveTimeOffset(int index) 
	{
		return archiveOffset[index] + 12;
	}
	
	/**
	 * Returns the var for the pointer time of the archive.
	 */
	private LongVar getArchiveTime(int index, Store store) 
	{
		return new LongVar(store, getArchiveTimeOffset(index));
	}
	
	/**
	 * Returns the offset for the pointer index of the archive.
	 */
	private int getArchiveIndexOffset(int index) 
	{
		return archiveOffset[index] + 20;
	}
	
	/**
	 * Returns the var for the pointer index of the archive.
	 */
	private IntVar getArchiveIndex(int index, Store store) 
	{
		return new IntVar(store, getArchiveIndexOffset(index));
	}

	/**
	 * Returns the offset for a point in the archive.
	 */
	private int getPointOffset(int archive, int index) 
	{
		return archiveOffset[archive] + ARCHIVE_HEADER_SIZE + (POINT_SIZE * index);
	}

	/**
	 * Returns the offset for the total statistics of the point.
	 */
	private int getPointTotalOffset(int archive, int index) 
	{
		return getPointOffset(archive, index);
	}
	
	/**
	 * Returns the var for the total statistics of the point.
	 */
	private LongVar getPointTotal(int archive, int index, Store store) 
	{
		return new LongVar(store, getPointTotalOffset(archive, index));
	}

	/**
	 * Returns the offset for the sum of the statistics of the point.
	 */
	private int getPointSumOffset(int archive, int index) 
	{
		return getPointOffset(archive, index) + 8;
	}
	
	/**
	 * Returns the var for the sum of the statistics of the point.
	 */
	private DoubleVar getPointSum(int archive, int index, Store store) 
	{
		return new DoubleVar(store, getPointSumOffset(archive, index));
	}

	/**
	 * Returns the offset for the smallest statistic of the point.
	 */
	private int getPointMinOffset(int archive, int index) 
	{
		return getPointOffset(archive, index) + 16;
	}
	
	/**
	 * Returns the var for the smallest statistic of the point.
	 */
	private FloatVar getPointMin(int archive, int index, Store store) 
	{
		return new FloatVar(store, getPointMinOffset(archive, index));
	}

	/**
	 * Returns the offset for the largest statistic of the point.
	 */
	private int getPointMaxOffset(int archive, int index) 
	{
		return getPointOffset(archive, index) + 20;
	}
	
	/**
	 * Returns the var for the largest statistic of the point.
	 */
	private FloatVar getPointMax(int archive, int index, Store store) 
	{
		return new FloatVar(store, getPointMaxOffset(archive, index));
	}
	
	
}
