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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.magnos.data.Store;
import org.magnos.data.StoreFactory;
import org.magnos.data.store.factory.MappedStoreFactory;


/**
 * A StatGroup represents a directory in the filesystem where its databases
 * and child groups are stored. The databases and child groups are synchronized
 * separately but can be synchronized together to perform certain operations.
 * 
 * Most methods can have a target specified to determine precisely what groups 
 * the invokation will be applied to. If no target is specified the group has
 * a default target (which is initially StatTarget.This).
 * 
 * When databases are created in a group and a format is not specified the
 * default format of the group will be used.
 * 
 * @author Philip Diffenderfer
 * @see StatTarget
 * 
 */
public class StatGroup 
{
	
	// The root (default) parent group
	private final static StatGroup root = new StatGroup(".");
	
	/**
	 * Returns the root StatGroup which points to the current working directory.
	 */
	public static StatGroup getRoot() 
	{
		return root;
	}

	
	
	// The directory this group stores databases.
	private final File directory;
	
	// The map of databases by their names (absolute paths).
	private final HashMap<String, StatDatabase> databaseMap;
	
	// The map of child groups by their names (directory name).
	private final HashMap<String, StatGroup> childMap;
	
	// The factory which creates stores when StatDatabases must be created.
	private StoreFactory storeFactory = new MappedStoreFactory();
	
	// The default format for opening and creating databases when not specified.
	private StatFormat formatDefault = new StatFormat(0);
	
	// Whether databases added to this group are enabled by default.
	private boolean enableDefault = false;
	
	// The default target when not specified. 
	private StatTarget targetDefault = StatTarget.This;
	
	
	/**
	 * Instantiates a new StatGroup in the current working directory. If a 
	 * directory with the given name does not exist in the parent directory it 
	 * will be created instantly.
	 * 
	 * @param name
	 * 		The name of the directory in the current working directory.
	 */
	public StatGroup(String name) 
	{
		this(name, new File("."));
	}
	
	/**
	 * Instantiates a new StatGroup given the parent group and the name of this
	 * group within the parent. This will not automatically add this group to
	 * its parent, that must be done explicitly (or just use 
	 * parent.getChild(name)). If a directory with the given name does not exist
	 * in the parent directory it will be created instantly.
	 * 
	 * @param name
	 * 		The name of the group (directory) in the parent group (directory).
	 * @param parent
	 * 		The parent group (directory) to this group.
	 */
	public StatGroup(String name, StatGroup parent) 
	{
		this(name, parent.getDirectory());
	}
	
	/**
	 * Instantiates a new StatGroup given its parent directory and its own
	 * directory. If a directory with the given name does not exist in the
	 * parent directory it will be created instantly.
	 * 
	 * @param name
	 * 		The name of the group (directory) in the parent directory.
	 * @param root
	 * 		The parent directory.
	 */
	public StatGroup(String name, File root) 
	{
		directory = getFile(root, name);
		directory.mkdirs();
		databaseMap = new HashMap<String, StatDatabase>();
		childMap = new HashMap<String, StatGroup>();
	}
	
	/**
	 * Returns the factory this group uses to create stores for databases.
	 * 
	 * @return
	 * 		The reference to this groups StoreFactory.
	 */
	public StoreFactory getFactory() 
	{
		return storeFactory;
	}
	
	/**
	 * Sets the store factory used to create stores for databases. This method
	 * will use the default target to determine precisely what groups and 
	 * databases this invokation will be applied to.
	 * 
	 * @param factory
	 * 		The StoreFactory to use to create stores.
	 */
	public void setFactory(StoreFactory factory) 
	{
		setFactory(factory, targetDefault);
	}
	
	/**
	 * Sets the store factory used to create stores for databases. This method
	 * will be applied based on the given target.
	 * 
	 * @param factory
	 * 		The StoreFactory to use to create stores.
	 * @param target
	 * 		The target of this method.
	 * @see StatTarget
	 */
	public void setFactory(StoreFactory factory, StatTarget target) 
	{
		storeFactory = factory;
		if (target.hasChildren()) {
			synchronized (childMap) {
				for (StatGroup child : childMap.values()) {
					child.setFactory(factory, target.getChild());
				}
			}
		}
	}
	
	/**
	 * Returns whether the databases created by this group are enabled by 
	 * default.
	 * 
	 * @return
	 * 		True if databases created by this group are enabled by default.
	 */
	public boolean isEnableDefault() 
	{
		return enableDefault;
	}

	/**
	 * Sets whether the databases created by this group are enabled by default.
	 * This method will use the default target to determine precisely what
	 * groups and databases this invokation will be applied to.
	 * 
	 * @param enabled
	 * 		True if databases created by this group are enabled by default.
	 */
	public void setEnableDefault(boolean enabled) 
	{ 
		setEnableDefault(enabled, targetDefault);
	}
	
	/**
	 * Sets whether the databases created by this group are enabled by default.
	 * This method will be applied based on the given target.
	 * 
	 * @param enabled
	 * 		True if databases created by this group are enabled by default.
	 * @param target
	 * 		The target of this method.
	 */
	public void setEnableDefault(boolean enabled, StatTarget target) 
	{
		enableDefault = enabled;
		if (target.hasChildren()) {
			synchronized (childMap) {
				for (StatGroup child : childMap.values()) {
					child.setEnableDefault(enabled, target.getChild());
				}
			}
		}
	}

	/**
	 * Returns the default database format to use to create databases in the 
	 * group when a format is not specified.
	 * 
	 * @return
	 * 		The default format of the databases.
	 */
	public StatFormat getFormatDefault() 
	{
		return formatDefault;
	}
	
	/**
	 * Sets the default database format to use to create databases in the group
	 * when a format is not specified. This method will use the default target 
	 * to determine precisely what groups this invokation will be applied to.
	 * 
	 * @param format
	 * 		The default format of the databases in this group.
	 */
	public void setFormatDefault(StatFormat format) 
	{
		setFormatDefault(format, targetDefault);
	}
	
	/**
	 * Sets the default database format to use to create databases in the group
	 * when a format is not specified. This method will be applied based on the
	 * given target.
	 * 
	 * @param format
	 * 		The default format of the databases.
	 * @param target
	 * 		The target of this method.
	 */
	public void setFormatDefault(StatFormat format, StatTarget target) 
	{
		formatDefault = format;
		if (target.hasChildren()) {
			synchronized (childMap) {
				for (StatGroup child : childMap.values()) {
					child.setFormatDefault(format, target.getChild());
				}
			}
		}
	}
	
	/**
	 * Returns the default target of all actions in this group.
	 * 
	 * @return
	 * 		The default target of this group.
	 */
	public StatTarget getTargetDefault() 
	{
		return targetDefault;
	}
	
	/**
	 * Sets the default target of all actions in this group. This method will 
	 * use the previous default target to determine precisely what groups this 
	 * invokation will be applied to.
	 * 
	 * @param newTarget
	 * 		The new default target of all actions in this group.
	 */
	public void setTargetDefault(StatTarget newTarget) 
	{
		setTargetDefault(newTarget, targetDefault);
	}
	
	/**
	 * Sets the default target of all actions in this group. This method will be
	 * applied based on the given target.
	 * 
	 * @param newTarget
	 * 		The new default target of all actions in this group.
	 * @param target
	 * 		The target of this method.
	 */
	public void setTargetDefault(StatTarget newTarget, StatTarget target) 
	{
		targetDefault = newTarget;
		if (target.hasChildren()) {
			synchronized (childMap) {
				for (StatGroup child : childMap.values()) {
					child.setTargetDefault(newTarget, target.getChild());
				}
			}
		}
	}
	
	/**
	 * Returns the database in this group with the given name. The name given
	 * can be a complete path (if the database persists to the filesystem) or
	 * the name provided when the group was added to the group (its filename
	 * without a path).
	 * 
	 * @param name
	 * 		The name of the database to get.
	 * @return
	 * 		The database with the given name or null if one could not be found.
	 */
	public StatDatabase get(String name) 
	{
		synchronized (databaseMap) {
			StatDatabase db = databaseMap.get(name);
			if (db == null) {
				db = databaseMap.get(getPath(name));
			}
			return db;
		}
	}
	
	/**
	 * Adds the given database to this group. The database should not already
	 * exist in another group, unexpexted results may occur.
	 * 
	 * @param database
	 * 		The database to add to this group.
	 * @return
	 * 		The previous database in the group with the same name. This
	 * 		typically should be null.
	 */
	public StatDatabase add(StatDatabase database) 
	{
		synchronized (databaseMap) {
			return databaseMap.put(database.getName(), database);
		}
	}
	
	/**
	 * Takes a database from this group with the given name. If it doesn't exist
	 * in the group already it will be created with the default format. If
	 * the database with the given name is not valid null may be returned. The 
	 * name given can be a complete path (if the database persists to the 
	 * filesystem) or the name provided when the group was added to the group 
	 * (its filename without a path).
	 * 
	 * @param name
	 * 		The name of the database to get.
	 * @return
	 * 		The reference to the database in this group with the given name.
	 */
	public StatDatabase take(String name) 
	{
		return take(name, formatDefault);
	}
	
	/**
	 * Takes a database from this group with the given name and format. If it
	 * doesn't exist in the group already it will be created with the given
	 * format. If the database with the given name is not valid null may be
	 * returned. The name given can be a complete path (if the database persists
	 * to the filesystem) or the name provided when the group was added to the 
	 * group (its filename without a path).
	 * 
	 * @param name
	 * 		The name of the database to get.
	 * @param format
	 * 		The format of the database.
	 * @return
	 * 		The reference to the database in this group with the given name.
	 */
	public StatDatabase take(String name, StatFormat format) 
	{
		synchronized (databaseMap) 
		{
			StatDatabase database = get(name);
			if (database == null) {
				try {
					// Change the name to the resolved path.
					name = getPath(name);
					
					// Create the store with the given factory
					Store store = storeFactory.create(name);
					
					// Create the database finally.
					database = new StatDatabase(store, format, this);
					database.setEnable(enableDefault);
					
					// No formatting problems, add to map.
					databaseMap.put(database.getName(), database);
				} 
				catch (StatFormatException e) {
					e.printStackTrace();
					// ignore, just dont add it
				}
			}
			return database;
		}
	}
	
	/**
	 * Removes the given database from this group. If a database exists with
	 * the same exact name it will be removed whether it is exactly the
	 * given database or not.
	 * 
	 * @param database
	 * 		The database to remove from this group.
	 * @return
	 * 		The database removed from this group, or null if none was removed.
	 */
	public StatDatabase remove(StatDatabase database) 
	{
		synchronized (databaseMap) {
			return databaseMap.remove(database.getName());
		}
	}
	
	/**
	 * Removes the database from this group with the given name. The name given
	 * can be a complete path (if the database persists to the filesystem) or
	 * the name provided when the group was added to the group (its filename
	 * without a path).
	 * 
	 * @param name
	 * 		The name of the database to remove.
	 * @return
	 * 		The database removed from this group, or null if none was removed.
	 */
	public StatDatabase remove(String name) 
	{
		synchronized (databaseMap) {
			StatDatabase db = databaseMap.remove(name);
			if (db == null) {
				db = databaseMap.remove(getPath(name));
			}
			return db;
		}
	}
	
	/**
	 * Returns the child group of this group with the given name. If none exist
	 * one will be created (and its subsequent directory if it doesn't exist
	 * already). The store factory and all defaults will be applied to the 
	 * child if one had to be created.
	 * 
	 * @param name
	 * 		The name of the child to return.
	 * @return
	 * 		The reference to the child group.
	 */
	public StatGroup getChild(String name) 
	{
		synchronized (childMap) {
			StatGroup child = childMap.get(name);
			if (child == null) {
				child = new StatGroup(name, this);
				child.setEnableDefault(enableDefault);
				child.setFormatDefault(formatDefault);
				child.setTargetDefault(targetDefault);
				child.setFactory(storeFactory);
				childMap.put(name, child);
			}
			return child;
		}
	}
	
	/**
	 * Returns the children of this group. This method will use the default 
	 * target to determine precisely what groups this invokation will be applied
	 * to.
	 * 
	 * @return
	 * 		The children of this group.
	 */
	public Set<StatGroup> getChildren() 
	{
		return getChildren(targetDefault);
	}
	
	/**
	 * Returns the children of this group. This method will be applied based on 
	 * the given target. 
	 * 
	 * @param target
	 * 		The target of this method.
	 * @return
	 * 		The children of this group.
	 */
	public Set<StatGroup> getChildren(StatTarget target) 
	{
		synchronized (childMap) {
			Set<StatGroup> children = new HashSet<StatGroup>();
			for (StatGroup child : childMap.values()) {
				children.add(child);
				if (target.hasChildren()) {
					children.addAll( child.getChildren(target.getChild()) );
				}
			}
			return children;
		}
	}
	
	/**
	 * Returns the number of enabled databases in the group. This method will 
	 * use the default target to determine precisely what groups this invokation
	 * will be applied to.
	 * 
	 * @return
	 * 		The number of enabled databases in the group.
	 */
	public int getEnabled() 
	{
		return getEnabled(targetDefault);
	}
	
	/**
	 * Returns the number of enabled databases in the group. This method will be
	 * applied based on the given target. 
	 * 
	 * @param target
	 * 		The target of this method.
	 * @return
	 * 		The number of enabled databases in the group.
	 */
	public int getEnabled(StatTarget target) 
	{
		synchronized (databaseMap) {
			int total = 0;
			for (StatDatabase sd : databaseMap.values()) {
				if (sd.isEnabled()) {
					total++;
				}
			}
			if (target.hasChildren()) {
				synchronized (childMap) {
					for (StatGroup child : childMap.values()) {
						total += child.getEnabled(target.getChild());
					}
				}
			}
			return total;
		}
	}
	
	/**
	 * Sets whether the databases of this group are enabled. This method will 
	 * use the default target to determine precisely what groups this invokation
	 * will be applied to.
	 * 
	 * @param enabled
	 * 		True if the databases of this group should be enabled, otherwise 
	 * 		false.
	 */
	public void setEnabled(boolean enabled) 
	{
		setEnabled(enabled, targetDefault);
	}
	
	/**
	 * Sets whether the databases of this group are enabled. This method will be
	 * applied based on the given target. 
	 * 
	 * @param enabled
	 * 		True if the databases of this group should be enabled, otherwise 
	 * 		false.
	 * @param target
	 * 		The target of this method.
	 */
	public void setEnabled(boolean enabled, StatTarget target) 
	{
		synchronized (databaseMap) {
			for (StatDatabase sd : databaseMap.values()) {
				sd.setEnable(enabled);
			}
			if (target.hasChildren()) {
				synchronized (childMap) { 
					for (StatGroup child : childMap.values()) {
						child.setEnabled(enabled, target.getChild());
					}
				}
			}
		}
	}
	
	/**
	 * Returns the number of databases in this group. This method will use the 
	 * default target to determine precisely what groups this invokation will be 
	 * applied to.
	 * 
	 * @return
	 * 		The number of databases in this group.
	 */
	public int size() 
	{
		return size(targetDefault);
	}
	
	/**
	 * Returns the number of databases in this group. This method will be 
	 * applied based on the given target. 
	 * 
	 * @param target
	 * 		The target of this method.
	 * @return
	 * 		The number of databases in this group.
	 */
	public int size(StatTarget target) 
	{
		synchronized (databaseMap) {
			int total = databaseMap.size();
			if (target.hasChildren()) {
				synchronized (childMap) {
					for (StatGroup child : childMap.values()) {
						total += child.size(target.getChild());
					}
				}
			}
			return total;
		}
	}
	
	/**
	 * Loads all databases in this group from its directory. If any of the 
	 * databases in the group have not been previously loaded the default
	 * format of this group will used. This method will use the default target 
	 * to determine precisely what groups this invokation will be applied to.
	 * 
	 * @return
	 * 		The set of databases loaded in this group.
	 */
	public Set<StatDatabase> load() 
	{
		return load(formatDefault, targetDefault);
	}
	
	/**
	 * Loads all databases in this group from its directory. If any of the 
	 * databases in the group have not been previously loaded the default
	 * format of this group will used. This method will be applied based on the 
	 * given target.
	 * 
	 * @param target
	 * 		The target of this method.
	 * @return
	 * 		The set of databases loaded in this group.
	 */
	public Set<StatDatabase> load(StatTarget target) 
	{
		return load(formatDefault, target);
	}
	
	/**
	 * Loads all databases in this group from its directory. If any of the 
	 * databases in the group have not been previously loaded the given format
	 * will be used. This method will be applied based on the given target.
	 * 
	 * @param format
	 * 		The format to use to load databases.
	 * @param target
	 * 		The target of this method.
	 * @return
	 * 		The set of databases loaded in this group.
	 */
	public Set<StatDatabase> load(StatFormat format, StatTarget target) 
	{
		synchronized (databaseMap) {
			Set<StatDatabase> databases = new HashSet<StatDatabase>();
			String[] names = directory.list();
			for (String name : names) {
				File file = getFile(name);
				if (file.isFile()) {
					StatDatabase db = take(name, format);
					if (db != null) {
						databases.add(db);
					}
				}
				if (file.isDirectory() && target.hasChildren()) {
					StatGroup child = getChild(name);
					databases.addAll( child.load(format, target.getChild()) );
				}
			}
			return databases;
		}
	}
	
	/**
	 * Returns the databases in this group. This method will use the default 
	 * target to determine precisely what groups this invokation will be applied 
	 * to.
	 * 
	 * @return
	 * 		The set of databases in the group.
	 */
	public Set<StatDatabase> getDatabases() 
	{
		return getDatabases(targetDefault);
	}
	
	/**
	 * Returns the databases in this group. This method will be applied based on
	 * the given target.
	 * 
	 * @param target
	 * 		The target of this method.
	 * @return
	 * 		The set of databases in the group.
	 */
	public Set<StatDatabase> getDatabases(StatTarget target) 
	{
		synchronized (databaseMap) {
			Set<StatDatabase> databases = new HashSet<StatDatabase>();
			for (StatDatabase db : databaseMap.values()) {
				databases.add(db);
			}
			if (target.hasChildren()) {
				synchronized (childMap) {
					for (StatGroup child : childMap.values()) {
						databases.addAll( child.getDatabases(target.getChild()) );
					}
				}
			}
			return databases;
		}
	}
	
	/**
	 * Closes the databases in the group. This method will use the default 
	 * target to determine precisely what groups this invokation will be applied 
	 * to.
	 * 
	 * @return
	 * 		The set of closed databases.
	 */
	public Set<StatDatabase> close() 
	{
		return close(targetDefault);
	}
	
	/**
	 * Closes the databases in the group. This method will be applied based on
	 * the given target.
	 * 
	 * @param target
	 * 		The target of this method.
	 * @return
	 * 		The set of closed databases.
	 */
	public Set<StatDatabase> close(StatTarget target) 
	{
		Set<StatDatabase> dbs = clear(target);
		for (StatDatabase db : dbs) {
			db.getStore().close();
		}
		return dbs;
	}
	
	/**
	 * Removes all databases from the group. This method will use the default 
	 * target to determine precisely what groups this invokation will be applied 
	 * to.
	 * 
	 * @return
	 * 		The set of removed databases.
	 */
	public Set<StatDatabase> clear() 
	{
		return clear(targetDefault);
	}
	
	/**
	 * Removes all databases from the group. This method will be applied based 
	 * on the given target.
	 * 
	 * @param target
	 * 		The target of this method.
	 * @return
	 * 		The set of removed databases.
	 */
	public Set<StatDatabase> clear(StatTarget target) 
	{
		synchronized (databaseMap) {
			Set<StatDatabase> databases = new HashSet<StatDatabase>();
			for (StatDatabase db : databaseMap.values()) {
				databases.add(db);
			}
			if (target.hasChildren()) {
				synchronized (childMap) {
					for (StatGroup child : childMap.values()) {
						databases.addAll( child.clear(target.getChild()) );
					}
				}
			}
			databaseMap.clear();
			return databases;
		}
	}
	
	/**
	 * Permanently deletes all databases in this group and if possible the
	 * group itself (if it has no subfolders that have not been deleted).
	 * This method will use the default target to determine precisely what
	 * groups this invokation will be applied to.
	 * 
	 * @return
	 * 		The set of deleted databases.
	 */
	public Set<StatDatabase> delete() 
	{
		return delete(targetDefault);
	}
	
	/**
	 * Permanently deletes all databases in this group and if possible the
	 * group itself (if it has no subfolders that have not been deleted).
	 * This method will be applied based on the given target.
	 * 
	 * @param target
	 * 		The target of this method.
	 * @return
	 * 		The set of deleted databases.
	 */
	public Set<StatDatabase> delete(StatTarget target) 
	{
		synchronized (databaseMap) {
			Set<StatDatabase> databases = new HashSet<StatDatabase>();
			for (StatDatabase db : databaseMap.values()) {
				db.getStore().delete();
				databases.add(db);
			}
			if (target.hasChildren()) {
				synchronized (childMap) {
					for (StatGroup child : childMap.values()) {
						databases.addAll( child.delete(target.getChild()) );
					}
				}
			}
			databaseMap.clear();
			if (!directory.delete()) {
				directory.deleteOnExit();
			}
			return databases;
		}
	}
	
	/**
	 * Returns the file in this groups directory with the given name.
	 * 
	 * @param name
	 * 		The name of the file to return.
	 * @return
	 * 		The file in this directory if it exists.
	 */
	public File getFile(String name) 
	{
		return getFile(directory, name);
	}
	
	/**
	 * Returns the absolute path in this groups directory with the given name.
	 * 
	 * @param name
	 * 		The name of the filepath to return.
	 * @return
	 * 		The filename in this directory if it exists.
	 */
	public String getPath(String name) 
	{
		return getFile(directory, name).getAbsolutePath();
	}
	
	/**
	 * Returns the directory of this group.
	 * 
	 * @return
	 * 		The directory of this group.
	 */
	public File getDirectory() 
	{
		return directory;
	}
	
	/**
	 * Returns whether this group's directory exists.
	 * 
	 * @return
	 * 		True if the group's directory exists, otherwise false.
	 */
	public boolean exists() 
	{
		return directory.exists();
	}
	
	/**
	 * Returns the resolved file of the file with the given name in the given
	 * directory.
	 * 
	 * @param directory
	 * 		The directory to get the file from.
	 * @param name
	 * 		The name of the file.
	 * @return
	 * 		The resolved file at the specified location.
	 */
	private static File getFile(File directory, String name) 
	{
		File file = new File(directory, name);
		try {
			file = file.getCanonicalFile();
		} catch (IOException e) { 
			// Ignore exception, just settle with default file.
		}
		return file;
	}
	
}
