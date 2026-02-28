<!--
  Title: Breinify Time-Utilities (e.g., IntervalTree, BucketTimeSeries)
  Description: Java library containing data- and index-structures to deal with (temporal) problems, e.g., IntervalTree, BucketTimeSeries.
  Author: breinify
  -->

<p align="center">
  <img src="https://www.breinify.com/img/Breinify_logo.png" alt="Breinify: Leading Temporal AI Engine" width="250">
</p>

# Time-Utilities 
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.breinify/brein-time-utilities/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.breinify/brein-time-utilities)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
<sup>Features: **IntervalTree**, **BucketTimeSeries**, **ContainerBucketTimeSeries**</sup>

## When should I use the Library
This library was mainly developed to make the life of developers easier, when working with temporal data.
The library provides index- and data-structures used to handle real-time temporal data (e.g., time-points, time-intervals).

## Usage: Maven Central

The library is available on [Maven Central](https://mvnrepository.com/artifact/com.breinify/brein-time-utilities). Just 
download the library or add it as dependency.

```pom
<dependency>
    <groupId>com.breinify</groupId>
    <artifactId>brein-time-utilities</artifactId>
    <version>${currentVersion}</version>
</dependency>
```

## Available Index Structures
The current implementation of the library offers the following index structures:
- `com.brein.time.timeintervals.indexes.IntervalTree` (since 1.5.0)

### IntervalTree
The `IntervalTree` is an often used index-structure to find intervals within a data-set. There 
are several implementations available, e.g., [[1]](https://github.com/kevinjdolan/intervaltree)[[2]](https://github.com/search?l=Java&p=1&q=intervaltree&type=Repositories&utf8=%E2%9C%93), and for 
other languages [[3]](https://github.com/chaimleib/intervaltree)[[4]](http://code.google.com/p/intervaltree/), 
as well as the [relational interval tree](http://blogs.solidq.com/en/sqlserver/static-relational-interval-tree/), 
which can be used within relational [database management systems](https://www.scaler.com/topics/dbms/what-is-database/). The presented Java implementation 
is not only well tested, it can also be persisted and can use a database management system to retrieve the different 
intervals data from an established database system, as well as utilize caching techniques.

Further information regarding the actual implementation can be found [here](docs/README.md), [[5]](http://www.geeksforgeeks.org/interval-tree/)
and [[6]](http://www.davismol.net/2016/02/07/data-structures-augmented-interval-tree-to-search-for-interval-overlapping/).

```java
final IntervalTree tree = IntervalTreeBuilder.newBuilder()
                .usePredefinedType(IntervalType.LONG)
                .collectIntervals(interval -> new ListIntervalCollection())
                .build();
tree.add(new LongInterval(1L, 5L));
tree.add(new LongInterval(2L, 5L));
tree.add(new LongInterval(3L, 5L));

final Collection<IInterval> overlap = tree.overlap(new Interval(2L, 2L));
overlap.forEach(System.out::println); // will print out [1, 5] and [2, 5]

final Collection<IInterval> find = tree.find(new Interval(2L, 5L));
find.forEach(System.out::println);    // will print out only [2, 5]
```

The `IntervalTree` implements the `Collection` interface and therefore can be used as such for `Interval`-instances.
In addition, it provides some functionality only meaningful for intervals. The following table shows the different 
functionality, the runtime-complexity, and if available the equivalent `Collection` method.

| IntervalTree          |  Collection           |  Complexity  |
| --------------------- | --------------------- |:------------:|
| insert                | add                   | O(log n)     |
| delete                | remove                | O(log n)     |
| find                  | contains              | O(log n)     |
| overlap               | n/a                   | O(log n)     |

Furthermore, the provided implementation offers the following features:

- storing and querying for multiple (equal) intervals (since 1.5.0), e.g.:
  - calling `insert(new IntegerInterval(1, 2))` twice will actually insert two intervals, using a `ListIntervalCollection`
  - calling `insert(new IntegerInterval(1, 2))` twice will only be inserted once, using a `SetIntervalCollection`
  - calling `insert(new IdInteval<>("ID1", 1, 2)))` and `insert(new IdInteval<>("ID2", 1, 2)))` will inserted two intervals (independent of the storage)
- easy extendable `IInterval` type, so that every type of data associated to intervals can be handled (since 1.5.0)
- `IntervalTree` implements `Collection` interface (since 1.5.0)
- `IntervalTree` provides a real `Stream` for `overlap(...)` operation, see [Streaming](docs/Streaming.md) (since 1.6.3)
- `Interval` (see [documentation](docs/Interval.md)) implements [Allen's Interval Algebra](https://en.wikipedia.org/wiki/Allen's_interval_algebra) (since 1.5.2)
- store, cache, and persist, see [documentation](docs/StoreCachePersist.md) (since 1.6.0)
  - use `IntervalCollectionObserver` and `ObservableIntervalCollection` to keep your database (storage) up-to-date
  - use the (sample) cache-implementation `CaffeineIntervalCollectionFactory`, utilizing [Caffeine](https://github.com/ben-manes/caffeine)
  - persist the in-memory `IntervalTree` on shut-down and avoid re-building, utilizing the methods `IntervalTreeBuilder.saveToFile()` and `IntervalTreeBuilder.loadFromFile()`
- auto-balancing, disable balancing, and manuel balancing
  - auto-balancing (activated by default): `IntervalTree.setAutoBalancing(true)` (since 1.5.0)
  - disable balancing: `IntervalTree.setAutoBalancing(false)` (since 1.5.0)
  - manual balancing: `IntervalTree.balance()`  (since 1.5.0)
- time optimized (handling temporal intervals) (to be added in 1.8.0)

Further information regarding this implementation of the `IntervalTree` are documented [here](docs/README.md).

## Available Data Structures
The current implementation of the library offers the following data structures:
- `com.brein.time.timeseries.BucketTimeSeries` (since 1.0.0)
- `com.brein.time.timeseries.ContainerBucketTimeSeries` (since 1.0.0)

### BucketTimeSeries
The BucketTimeSeries is used to group time-points into buckets and keep a time-series
for these buckets. As all of the time-series of this library, this time-series is also
a rolling time-series regarding the now time point, i.e., it contains information about
now and n time-buckets into the pass. The following illustration explains the structure:

```
 The data structure (which is based on an array) can be explained best with
 an illustration (with n == timeSeriesSize):

   [0] [1] [2] [3] [4] [5] [6] ... [n]
        ↑
  currentNowIdx

 Each array field is a bucket of time-stamps (ordered back from now):

   [1] ==> [1456980000, 1456980300) ← now, 1 == currentNowIdx
   [2] ==> [1456970700, 1456980000)
   ...
   [n] ==> ...
   [0] ==> ...
```

Whenever the now time point "moves" forward using:

```java
public void setNow(final long unixTimeStamp) throws IllegalTimePointMovement
```

the data structure is updated using:

```
 Getting the new currentNowIdx is done by calculating the
 difference between the old now and the new now and moving
 the currentNowIdx forward.

  [0] [1] [2] [3] [4] [5] [6]
       ↑
 currentNowIdx

 Assume we move the now time stamp forward by three buckets:

  [0] [1] [2] [3] [4] [5] [6]
                       ↑
                 currentNowIdx

 So the calculation is done in two steps:
 1.) get the bucket of the new now
 2.) determine the difference between the buckets, if it's negative => error,
     if it is zero => done, otherwise => erase the fields in between and reset
     to zero or null
```

## Miscellaneous
The current implementation of the library offers the following additional utilities:
- `com.brein.time.expressions.Exp4JTemporalExpressionEvaluator` (since 1.7.0)

### Temporal Expression Evaluator
With version 1.7.0 of the library a new feature was added, which enables developers to evaluate "temporal" expressions. 
In general, a `TemporalExpression` can be understood as a formel expression (i.e., a formula), which contains temporal 
elements, e.g., `now() + 4h`. The syntax of the expression may depend on the underlying implementation, thus it is 
recommended to have a closer look at the provided documentation (for the selected implementation).

The following concrete implementations are currently available:
- [Exp4JTemporalExpressionEvaluator](docs/Exp4JTemporalExpressionEvaluator.md), which is based on the [exp4j](https://github.com/fasseg/exp4j) library

