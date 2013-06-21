statastic
=========

A Java library for tracking statistics over time in a round robin database. A round robin database is a fixed size database with several time spans it accumulates statistics over.

**Example**

```java
// Create a format for saving statistics.
StatFormat sf = new StatFormat(5);    // A format that covers a year (365.25 days)
sf.set(0, 300205L, 12);             // one point every 5 minutes for an hour 
sf.set(1, 3602467L, 24);             // one point every hour for a day
sf.set(2, 43229589L, 14);            // one point every 12 hours for a week
sf.set(3, 86459178L, 31);            // one point every day for a month
sf.set(4, 7606876923L, 52);            // one point for every week in a year
sf.compile();                        // database size = 3328 bytes

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

https://github.com/ClickerMonkey/statastic/tree/master/build

**Testing Examples**

https://github.com/ClickerMonkey/statastic/tree/master/Testing/org/magnos/stat
