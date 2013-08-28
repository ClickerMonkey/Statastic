statastic
=========

![Stable](http://i4.photobucket.com/albums/y123/Freaklotr4/stage_stable.png)

A Java library for tracking statistics over time in a round robin database. A round robin database is a fixed size database with several time spans it accumulates statistics over.

### Terminology
- StatPoint *- a single statistical point in an archive, contains total number of stats, their sum, min, max, and average*
- StatArchive *- An archive contains a fixed number of points. Each point contains a summary for each statistic added to that point in the interval of time. When a point must be added when the archive is full the oldest point is overwritten.*
- StatDatabase *- A database of round robin archives which hold the summary of a statistic over time*

### Features
- A database can be stored in memory, in a file, or in a memory-mapped file
- Adding statistics to databases is a non-blocking operation that adds virtually no overhead.
- All data can be written out to the backing store (memory or file) whenever there's a statistic added or at some interval.
- An archive can be exported to a chart as a PNG, JPG, BMP, or a data file (CSV).
- A group of databases stored in a folder can be handled/loaded all with one class (StatGroup).
- Memory-mapped databases are fast in memory databases that the OS flushes out to a file - this is the most reliable store type.

### Documentation
- [JavaDoc](http://gh.magnos.org/?r=http://clickermonkey.github.com/statastic/)

### Examples

#### Example that writes out to files in the current-working-directory of the application

##### Start of Application

```java
public class Stats
{
  public static StatFormat FORMAT;
  
  static
  {
  	StatService.get();
  
    FORMAT = new StatFormat( ) ;      // A format that covers a year (365.25 days)
    FORMAT.set( 0, 300205L, 12 );     // one point every 5 minutes for an hour 
    FORMAT.set( 1, 3602467L, 24 );    // one point every hour for a day
    FORMAT.set( 2, 43229589L, 14 );   // one point every 12 hours for a week
    FORMAT.set( 3, 86459178L, 31 );   // one point every day for a month
    FORMAT.set( 4, 7606876923L, 52 ); // one point for every week in a year
    FORMAT.compile();                 // database size = 3328 bytes
  }
  
  // Save Statistics for MyClass
  public static StatDatabase DB_1 = new StatDatabase(MyClass.class, FORMAT);

  // Save Statistics in "stat.db" 
  public static StatDatabase DB_2 = new StatDatabase(new FileStore("stat.db"), FORMAT);
}
```

##### During Application Execution

```java
// Adding one when an event occurs.
Stats.DB_1.add(1f);

// Adding a value over time (for example time).
Stats.DB_2.add(2.435f);
```

##### End of Application

```java
// Close the databases
StatGroup.getRoot().close();

// Stop Statistics Thread
StatService.get().stop();
```

#### Example that saves all databases in memory-mapped files in a specific directory AND databases are dynamically created/loaded (through take).

##### Start of Application

```java
public class Stats
{
  public static StatFormat FORMAT;
  public static StatGroup GROUP;
  
  static
  {
  	StatService.get();
  
    FORMAT = new StatFormat( 5 );     // A format that covers a year (365.25 days)
    FORMAT.set( 0, 300205L, 12 );     // one point every 5 minutes for an hour 
    FORMAT.set( 1, 3602467L, 24 );    // one point every hour for a day
    FORMAT.set( 2, 43229589L, 14 );   // one point every 12 hours for a week
    FORMAT.set( 3, 86459178L, 31 );   // one point every day for a month
    FORMAT.set( 4, 7606876923L, 52 ); // one point for every week in a year
    FORMAT.compile();                 // database size = 3328 bytes
    
    GROUP = new StatGroup( "/home/stat/my_app_stats" );
    GROUP.setFactory( new MappedStoreFactory() );
    GROUP.setEnabledDefault(true);
    GROUP.setFormatDefault(FORMAT);
  }
  
  public static void add(String name, float statistic)
  {
    GROUP.take( name ).add( statistic );
  }
  
  public static close()
  {
    // Close the databases
    GROUP.close();
    
    // Stop Statistics Thread
    StatService.get().stop();
  }
}
```

##### During Application Execution

```java
// Adding one when an event occurs and creating the "number_of_events" database if it doesn't exist already.
Stats.add( "number_of_events", 1.0f );

// Adding a value over time (for example elapsed time) and creating the "event_elapsed_time" database if it 
// doesn't exist already.
Stats.add( "event_elapsed_time", 2.435f );
```

##### End of Application

```java
Stats.close();
```

#### Example that exports a StatArchive to an image and CSV file

```java
// Archive 0 = past hour
StatExport.export(Stats.DB_1.getArchive(0), Type.CSV, new File("sdb.csv"));
StatExport.export(Stats.DB_2.getArchive(0), Type.PNG, new File("sdb.png"));
```

### Builds
- [statastics-1.0.0.jar](http://gh.magnos.org/?r=https://github.com/ClickerMonkey/statastic/blob/master/build/statastics-1.0.0.jar?raw=true)
- [statastics-src-1.0.0.jar](http://gh.magnos.org/?r=https://github.com/ClickerMonkey/statastic/blob/master/build/statastics-src-1.0.0.jar?raw=true) *- includes source code*
- [statastics-all-1.0.0.jar](http://gh.magnos.org/?r=https://github.com/ClickerMonkey/statastic/blob/master/build/statastics-1.0.0.jar?raw=true) *- includes all dependencies*
- [statastics-all-src-1.0.0.jar](http://gh.magnos.org/?r=https://github.com/ClickerMonkey/statastic/blob/master/build/statastics-src-1.0.0.jar?raw=true) *- includes all dependencies and source code*

### Dependencies
- [daperz](http://gh.magnos.org/?r=https://github.com/ClickerMonkey/daperz)
- [curity](http://gh.magnos.org/?r=https://github.com/ClickerMonkey/curity)
- [surfice](http://gh.magnos.org/?r=https://github.com/ClickerMonkey/surfice)
- [buffero](http://gh.magnos.org/?r=https://github.com/ClickerMonkey/buffero)
- [testility](http://gh.magnos.org/?r=https://github.com/ClickerMonkey/testility) *for unit tests*

### Testing Examples
- [Testing/org/magnos/stat](http://gh.magnos.org/?r=https://github.com/ClickerMonkey/statastic/tree/master/Testing/org/magnos/stat)
