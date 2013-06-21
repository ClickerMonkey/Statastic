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

import java.util.Iterator;

import org.magnos.data.Store;
import org.magnos.data.StoreAccess;
import org.magnos.data.store.FileStore;
import org.magnos.data.store.MemoryStore;


/**
 * A database of round robin archives which hold the summary of a statistic
 * over time. 
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
public class StatDatabase implements Iterable<StatArchive>
{
	
	// The archives held in this database.
	private final StatArchive[] archives;
	
	// The store which holds all archivedata.
	private final Store store;
	
	// The format of the database and its archives.
	private final StatFormat format;
	
	// The group (folder) this database exists in.
	private final StatGroup group;
	
	// The reference name for this database within the group (filename).
	private final String name;
	
	// If the database can accept statistics to be added.
	private boolean enabled = false;
	
	
	/**
	 * Instantiates a new StatDatabase for a single class.
	 * 
	 * @param classType
	 * 		The class the database would save statistics for.
	 * @param format
	 * 		The format of the database and its archives.
	 * @throws StatFormatException
	 * 		The given format is invalid.
	 */
	public StatDatabase(Class<?> classType, StatFormat format) throws StatFormatException
	{
		this(classType, format, StatGroup.getRoot());
	}
	
	/**
	 * Instantiates a new StatDatabase with the given name existing in the root
	 * StatGroup.
	 * 
	 * @param name
	 * 		The name (filename) of the database in the root group.
	 * @param format
	 * 		The format of the database and its archives.
	 * @throws StatFormatException
	 * 		The given format is invalid.
	 */
	public StatDatabase(String name, StatFormat format) throws StatFormatException
	{
		this(name, format, StatGroup.getRoot());
	}
	
	/**
	 * Instantiates a new StatDatabase with the given store existing in the
	 * root StatGroup.
	 * 
	 * @param store
	 * 		The store to persist the database to.
	 * @param format
	 * 		The format of the database and its archives.
	 * @throws StatFormatException
	 * 		The given format is invalid.
	 */
	public StatDatabase(Store store, StatFormat format) throws StatFormatException
	{
		this(store, format, StatGroup.getRoot());
	}
	
	/**
	 * Instantiates a new StatDatabase for a single class in the given group.
	 * 
	 * @param classType
	 * 		The class type. This is used to determine the filename for the
	 * 		database in the given group.
	 * @param format
	 * 		The format of the database and its archive.
	 * @param group
	 * 		The group (folder) to persist the database to.
	 * @throws StatFormatException
	 * 		The given format ia invalid.
	 */
	public StatDatabase(Class<?> classType, StatFormat format, StatGroup group) throws StatFormatException
	{
		this(classType.getCanonicalName(), format, group);
	}
	
	/**
	 * Instantiates a new StatDatabase with the given name in the given group.
	 * 
	 * @param name
	 * 		The name (filename) of the database in the given group.
	 * @param format
	 * 		The format of the database and its archive.
	 * @param group
	 * 		The group (folder) to persist the database to.
	 * @throws StatFormatException
	 * 		The given format ia invalid.
	 */
	public StatDatabase(String name, StatFormat format, StatGroup group) throws StatFormatException
	{
		this(new FileStore(group.getFile(name)), format, group);
	}
	
	/**
	 * Instantiates a new StatDatabase with the given store existing in the
	 * given StatGroup.
	 * 
	 * @param store
	 * 		The store to persist the database to.
	 * @param format
	 * 		The format of the database and its archive.
	 * @param group
	 * 		The group (folder) to persist the database to.
	 * @throws StatFormatException
	 * 		The given format ia invalid.
	 */
	public StatDatabase(Store store, StatFormat format, StatGroup group) throws StatFormatException
	{
		this.name = store.getName();
		this.group = group;
		this.store = store;
		this.format = format.compile();
		
		// If the store already exists...
		if (store.exists()) {
			try {
				// Validate it to ensure its in the proper format.
				format.validate(store);	
			}
			catch (StatFormatException e) { 
				// Its not, just close the store and quit instantiation.
				store.close();
				throw e;
			}
			
			// The store is valid, open it.
			store.open(StoreAccess.ReadWrite);
		}
		else {
			// The store doesn't exist create an database.
			format.write(store);
		}
		
		// Create all archives and load their headers.
		this.archives = new StatArchive[format.getArchiveCount()];
		for (int i = 0; i < format.getArchiveCount(); i++) {
			this.archives[i] = new StatArchive(store, format, i);
		}
	}
	
	/**
	 * Adds the given statistic to the database. This will add the given 
	 * statistic to the StatServer which will then process the statistic by
	 * adding it to all target databases (only this one by default).
	 * 
	 * @param statistic
	 * 		The statistic to add to the database.
	 * @return
	 * 		The event that was generated to add the statistic to this database.
	 * 		This has already been added to the StatService for processing but
	 * 		additional target databases can be added if done immediately. If
	 * 		the database is disabled null will be returned.
	 */
	public StatEvent add(float statistic) 
	{
		StatEvent event = null;
		if (enabled) {
			event = new StatEvent(statistic, this);
			event.process();
		}
		return event;
	}
	
	/**
	 * Returns a statistic event which will add the given statistic to this
	 * database. The returned event has not been processed and will not be added
	 * to the database until the process method is invoked. Before the event is
	 * processed however target databases can be added to the event. If the
	 * event is not processed soon enough its statistic might not make the
	 * archives with the shortest intervals.
	 * 
	 * @param statistic
	 * 		The statistic to add to the database.
	 * @return
	 * 		The event that was generated to add the statistic to this database. 
	 * 		If the database is disabled null will be returned.
	 */
	public StatEvent getEvent(float statistic) 
	{
		return (enabled ? new StatEvent(statistic, this) : null);
	}
	
	/**
	 * Adds the given event to all archives in the database.
	 * 
	 * @param event
	 * 		The event to add to all archives in the database.
	 */
	protected void addEvent(StatEvent event) 
	{
		for (StatArchive a : archives) {
			a.addEvent(event);
		}
	}
	
	/**
	 * Returns whether this database exists.
	 * 
	 * @return
	 * 		True if the underlying store exists, otherwise false.
	 */
	public boolean exists() 
	{
		return store.exists();
	}
	
	/**
	 * Returns the format of this database and its archives.
	 * 
	 * @return
	 * 		The reference to the format which created the database.
	 */
	public StatFormat getFormat() 
	{
		return format;
	}
	
	/**
	 * Returns the underlying store in the database.
	 * 
	 * @return
	 * 		The reference to the underlying store.
	 */
	public Store getStore() 
	{
		return store;
	}

	/**
	 * Returns the number of archives in this database.
	 * 
	 * @return
	 * 		The number of archives in this database.
	 */
	public int getArchiveCount()
	{
		return archives.length;
	}
	
	/**
	 * Returns the archive at the given index. If the index is outside the
	 * bounds of the archive array an IndexOutOfBoundsException will be thrown.
	 * 
	 * @param index
	 * 		The index of the archive to receive.
	 * @return
	 * 		The reference of the archive at the given index.
	 */
	public StatArchive getArchive(int index) 
	{
		return archives[index];
	}
	
	/**
	 * Returns the name of the database. If the store is persisted to the
	 * filesystem this is typically a filename.
	 * 
	 * @return
	 * 		The name of the database.
	 */
	public String getName() 
	{
		return name;
	}
	
	/**
	 * Returns the group which holds this database.
	 * 
	 * @return
	 * 		The group this databases exists in.
	 */
	public StatGroup getGroup() 
	{
		return group;
	}
	
	/**
	 * Returns whether this database can accept statistics.
	 * 
	 * @return
	 * 		True if this database can have statistics added to it, otherwise
	 * 		false and statistics will not be added to the database.
	 */
	public boolean isEnabled() 
	{
		return enabled;
	}
	
	/**
	 * Sets whether this database can accept statistics.
	 * 
	 * @param enabled
	 * 		True if this database can have statistics added to it, otherwise
	 * 		false and statistics can not be added to the database.
	 */
	public void setEnable(boolean enabled) 
	{
		this.enabled = enabled;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() 
	{
		return name;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() 
	{
		return name.hashCode();
	}

	/**
	 * Returns an iterator for iterating through the archives in this database.
	 */
	public Iterator<StatArchive> iterator() 
	{
		return new ArchiveIterator();
	}
	
	/**
	 * An iterator of a databases Archives.
	 * 
	 * @author Philip Diffenderfer
	 *
	 */
	private class ArchiveIterator implements Iterator<StatArchive> 
	{
		// The current archive index.
		int index = 0;
		public boolean hasNext() {
			return (index < format.getArchiveCount());
		}
		public StatArchive next() {
			return archives[index++];
		}
		public void remove() {
			// Cannot remove archives form the database.
			throw new UnsupportedOperationException();
		}
	}

	/**
	 * Creates an in memory database with the given reference name and with
	 * the given format.
	 * 
	 * @param name
	 * 		The reference name of the database.
	 * @param format
	 * 		The format of the database and its archives.
	 * @return
	 * 		A newly allocated in memory database.
	 */
	public static StatDatabase inMemory(String name, StatFormat format) 
	{
		return inMemory(name, format, StatGroup.getRoot());
	}
	
	/**
	 * Creates an in memory database with the given reference name, format, and
	 * group.
	 * 
	 * @param name
	 * 		The reference name of the database.
	 * @param format
	 * 		The format of the database and its archives.
	 * @param group
	 * 		The group the database exists in.
	 * @return
	 * 		A newly allocated in memory database.
	 */
	public static StatDatabase inMemory(String name, StatFormat format, StatGroup group) 
	{
		MemoryStore store = new MemoryStore(name);
		format.compile().write(store);
		return new StatDatabase(store, format, group);
	}
	
}
