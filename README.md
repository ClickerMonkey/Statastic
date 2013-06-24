statastic
=========

A Java library for tracking statistics over time in a round robin database. A round robin database is a fixed size database with several time spans it accumulates statistics over.

**Terminology**
- StatPoint *- a single statistical point in an archive, contains total number of stats, their sum, min, max, and average*
- StatArchive *- An archive contains a fixed number of points. Each point contains a summary for each statistic added to that point in the interval of time. When a point must be added when the archive is full the oldest point is overwritten.*
- StatDatabase *- A database of round robin archives which hold the summary of a statistic over time*

**Features**
- A database can be stored in memory, in a file, or in a memory-mapped file
- All data can be written out to the backing store (memory or file) whenever there's a statistic added or at some interval.
- An archive can be exported to a chart as a PNG, JPG, BMP, or a data file (CSV).
- A group of databases stored in a folder can be handled/loaded all with one class (StatGroup).
- Memory-mapped databases are fast in memory databases that the OS flushes out to a file - this is the most reliable store type.

**Documentation**
- [JavaDoc](http://gh.magnos.org/?r=http://clickermonkey.github.com/statastic/)

**Example**

```java
// Create a format for saving statistics.
StatFormat sf = new StatFormat(5);  // A format that covers a year (365.25 days)
sf.set(0, 300205L, 12);             // one point every 5 minutes for an hour 
sf.set(1, 3602467L, 24);            // one point every hour for a day
sf.set(2, 43229589L, 14);           // one point every 12 hours for a week
sf.set(3, 86459178L, 31);           // one point every day for a month
sf.set(4, 7606876923L, 52);         // one point for every week in a year
sf.compile();                       // database size = 3328 bytes

// Save Statistics for MyClass
StatDatabase db1 = new StatDatabase(MyClass.class, sf);

// Save Statistics in "stat.db" 
StatDatabase db2 = new StatDatabase(new FileStore("stat.db"), sf);

// Adding one when an event occurs.
db1.add(1f);
// Adding a value over time (for example time).
db2.add(2.435f);

/**Processing**/

// Close the databases
StatGroup.getRoot().close();

// Stop Statistics Thread
StatService.get().stop();
```

**Builds**
- [statastics-1.0.0.jar](http://gh.magnos.org/?r=https://github.com/ClickerMonkey/statastic/blob/master/build/statastics-1.0.0.jar?raw=true)
- [statastics-src-1.0.0.jar](http://gh.magnos.org/?r=https://github.com/ClickerMonkey/statastic/blob/master/build/statastics-src-1.0.0.jar?raw=true) *- includes source code*
- [statastics-all-1.0.0.jar](http://gh.magnos.org/?r=https://github.com/ClickerMonkey/statastic/blob/master/build/statastics-1.0.0.jar?raw=true) *- includes all dependencies*
- [statastics-all-src-1.0.0.jar](http://gh.magnos.org/?r=https://github.com/ClickerMonkey/statastic/blob/master/build/statastics-src-1.0.0.jar?raw=true) *- includes all dependencies and source code*

**Dependencies**
- [daperz](http://gh.magnos.org/?r=https://github.com/ClickerMonkey/daperz)
- [curity](http://gh.magnos.org/?r=https://github.com/ClickerMonkey/curity)
- [surfice](http://gh.magnos.org/?r=https://github.com/ClickerMonkey/surfice)
- [buffero](http://gh.magnos.org/?r=https://github.com/ClickerMonkey/buffero)
- [testility](http://gh.magnos.org/?r=https://github.com/ClickerMonkey/testility) *for unit tests*

**Testing Examples**
- [Testing/org/magnos/stat](http://gh.magnos.org/?r=https://github.com/ClickerMonkey/statastic/tree/master/Testing/org/magnos/stat)
