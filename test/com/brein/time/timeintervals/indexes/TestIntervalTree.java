package com.brein.time.timeintervals.indexes;

import com.brein.time.timeintervals.collections.ListIntervalCollection;
import com.brein.time.timeintervals.collections.SetIntervalCollection;
import com.brein.time.timeintervals.indexes.IntervalTreeBuilder.IntervalType;
import com.brein.time.timeintervals.intervals.DoubleInterval;
import com.brein.time.timeintervals.intervals.IInterval;
import com.brein.time.timeintervals.intervals.IdInterval;
import com.brein.time.timeintervals.intervals.IntegerInterval;
import com.brein.time.timeintervals.intervals.LongInterval;
import com.brein.time.timeintervals.intervals.NumberInterval;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TestIntervalTree {
    private static final Logger LOGGER = Logger.getLogger(TestIntervalTree.class);

    @Test
    public void testSimpleUsage() {
        final IntervalTree tree = IntervalTreeBuilder.newBuilder()
                .usePredefinedType(IntervalType.NUMBER)
                .collectIntervals(interval -> new ListIntervalCollection())
                .build();

        tree.insert(new LongInterval(1L, 2L));
        tree.insert(new IntegerInterval(5, 10));
        tree.insert(new DoubleInterval(5.0, 10.0));
        tree.insert(new DoubleInterval(0.1, 1.5));
        tree.insert(new IntegerInterval(5, 10));
        tree.insert(new DoubleInterval(5.0, 10.0));

        tree.nodeIterator().forEachRemaining(n -> {
            if (n.isRoot()) {
                Assert.assertEquals(1, n.getIntervals().size());
                Assert.assertEquals(new LongInterval(1L, 2L), n.getIntervals().iterator().next());
            } else if (n.compare(0.1, n.getStart()) == 0) {
                Assert.assertEquals(1.5, n.getEnd());
                Assert.assertEquals(1, n.getIntervals().size());
            } else if (n.compare(5, n.getStart()) == 0) {
                Assert.assertEquals(10, n.getEnd());
                Assert.assertEquals(4, n.getIntervals().size());
            } else {
                Assert.fail("Unexpected node '" + n + "' found");
            }
        });
    }

    @Test
    public void testEmptyTree() {
        assertIsEmpty(IntervalTreeBuilder.newBuilder()
                .usePredefinedType(IntervalType.NUMBER)
                .collectIntervals(interval -> new ListIntervalCollection())
                .build());

        assertIsEmpty(IntervalTreeBuilder.newBuilder()
                .usePredefinedType(IntervalType.NUMBER)
                .collectIntervals(interval -> new SetIntervalCollection())
                .build());

        assertIsEmpty(IntervalTreeBuilder.newBuilder()
                .usePredefinedType(IntervalType.NUMBER)
                .build());
    }

    @Test
    public void testBalancing() {
        final IntervalTree tree = IntervalTreeBuilder.newBuilder()
                .usePredefinedType(IntervalType.NUMBER)
                .collectIntervals(interval -> new ListIntervalCollection())
                .build();

        tree.insert(new IntegerInterval(1, 10));
        tree.insert(new IntegerInterval(1, 33));
        tree.insert(new IntegerInterval(1, 36));

        tree.nodeIterator().forEachRemaining(node -> assertNode(node, tree, true));

        tree.clear();

        tree.insert(new IntegerInterval(36, 100));
        tree.insert(new IntegerInterval(33, 100));
        tree.insert(new IntegerInterval(10, 100));

        tree.nodeIterator().forEachRemaining(node -> assertNode(node, tree, true));

        final int nrOfRuns = 1;
        for (int i = 0; i < nrOfRuns; i++) {
            final int inserts = Double.valueOf(Math.random() * 100).intValue() + 50;
            final IntervalTree t = createRandomTree(inserts, Double.valueOf(Math.random() * inserts).intValue(), false);
            if (t.isBalanced()) {
                i--;
            } else {
                t.balance();
                Assert.assertTrue(tree.toString(), tree.isBalanced());
            }
        }
    }

    @Test
    public void testTrees() {
        final int nrOfRuns = 1;
        final int minNrOfInserts = 50;
        final int maxNrOfInserts = 200;

        final Random rnd = new Random();

        for (int i = 0; i < nrOfRuns; i++) {
            final int variableInserts = minNrOfInserts + rnd.nextInt(maxNrOfInserts - minNrOfInserts + 1);
            final int variableRemoves = (int) Math.floor(variableInserts * rnd.nextDouble());

            LOGGER.trace("Validated:" + System.lineSeparator() +
                    createRandomTree(variableInserts, variableRemoves, rnd.nextBoolean()));
        }
    }

    @Test
    public void testEmptyAfterInsertAndDelete() {
        final IntervalTree tree = IntervalTreeBuilder.newBuilder()
                .usePredefinedType(IntervalType.NUMBER)
                .collectIntervals(interval -> new ListIntervalCollection())
                .build();

        assertIsEmpty(tree
                .insert(new LongInterval(3L, 3L))
                .insert(new LongInterval(3L, 3L))
                .insert(new LongInterval(4L, 4L))
                .insert(new LongInterval(2L, 2L))
                .delete(new LongInterval(3L, 3L))
                .delete(new LongInterval(3L, 3L))
                .delete(new LongInterval(4L, 4L))
                .delete(new LongInterval(2L, 2L)));
    }

    @Test
    public void testSetSize() {
        final IntervalTree tree = IntervalTreeBuilder.newBuilder()
                .usePredefinedType(IntervalType.NUMBER)
                .collectIntervals(interval -> new SetIntervalCollection())
                .build();

        Assert.assertEquals(tree.size(), 0);

        tree.insert(new IntegerInterval(1, 1));
        tree.insert(new IntegerInterval(1, 1));
        tree.insert(new IntegerInterval(1, 1));
        tree.insert(new DoubleInterval(1.0, 1.0));
        Assert.assertEquals(tree.size(), 1);

        tree.remove(new IntegerInterval(1, 1));
        Assert.assertEquals(tree.size(), 0);
        tree.remove(new IntegerInterval(1, 1));
        Assert.assertEquals(tree.size(), 0);

        tree.insert(new IdInterval<>("ID1", 1, 1));
        tree.insert(new IdInterval<>("ID2", 1, 1));
        Assert.assertEquals(tree.size(), 2);
        tree.remove(new IdInterval<>("ID3", 1, 1));
        Assert.assertEquals(tree.size(), 2);
        tree.remove(new IdInterval<>("ID1", 1, 1));
        Assert.assertEquals(tree.size(), 1);
        tree.remove(new IdInterval<>("ID2", 1, 1));
        Assert.assertEquals(tree.size(), 0);
    }

    @Test
    public void testListSize() {
        final IntervalTree tree = IntervalTreeBuilder.newBuilder()
                .usePredefinedType(IntervalType.NUMBER)
                .collectIntervals(interval -> new ListIntervalCollection())
                .build();

        Assert.assertEquals(tree.size(), 0);

        tree.insert(new IntegerInterval(1, 1));
        tree.insert(new IntegerInterval(1, 1));
        tree.insert(new IntegerInterval(1, 1));
        Assert.assertEquals(tree.size(), 3);

        tree.remove(new IntegerInterval(1, 1));
        Assert.assertEquals(tree.size(), 2);
        tree.remove(new IntegerInterval(1, 1));
        Assert.assertEquals(tree.size(), 1);

        tree.insert(new IdInterval<>("ID1", 1, 1));
        tree.insert(new IdInterval<>("ID2", 1, 1));
        Assert.assertEquals(tree.size(), 3);

        tree.remove(new IdInterval<>("ID3", 1, 1));
        Assert.assertEquals(tree.size(), 3);
        tree.remove(new IdInterval<>("ID1", 1, 1));
        Assert.assertEquals(tree.size(), 2);
        tree.remove(new IdInterval<>("ID2", 1, 1));
        Assert.assertEquals(tree.size(), 1);
    }

    @Test
    public void testFind() {
        Collection<IInterval> found;

        final IntervalTree tree = IntervalTreeBuilder.newBuilder()
                .usePredefinedType(IntervalType.NUMBER)
                .collectIntervals(interval -> new SetIntervalCollection())
                .build();

        tree.insert(new IdInterval<>("ID1", 1L, 5L));
        found = tree.find(new IdInterval<>("ID1", 1L, 5L));
        Assert.assertEquals(1, found.size());

        tree.insert(new IdInterval<>("ID2", 1L, 5L));
        found = tree.find(new IdInterval<>("ID2", 1L, 5L));
        Assert.assertEquals(1, found.size());

        found = tree.find(new LongInterval(1L, 5L));
        Assert.assertEquals(2, found.size());
    }

    @Test
    public void testOverlap() throws IOException {
        Collection<IInterval> overlap;

        final IntervalTree tree = IntervalTreeBuilder.newBuilder()
                .usePredefinedType(IntervalType.NUMBER)
                .collectIntervals(interval -> new ListIntervalCollection())
                .build();

        // check the empty tree
        overlap = tree.overlap(new LongInterval(1L, 5L));
        Assert.assertEquals(0, overlap.size());

        // add the interval [3, 3]
        tree.insert(new LongInterval(3L, 3L));
        overlap = tree.overlap(new LongInterval(1L, 5L));
        Assert.assertEquals(1, overlap.size());
        Assert.assertTrue(overlap.contains(new LongInterval(3L, 3L)));

        overlap = tree.overlap(new LongInterval(4L, 5L));
        Assert.assertEquals(0, overlap.size());

        overlap = tree.overlap(new LongInterval(1L, 2L));
        Assert.assertEquals(0, overlap.size());

        // add the interval [3, 10]
        tree.insert(new LongInterval(3L, 10L));

        overlap = tree.overlap(new LongInterval(1L, 5L));
        Assert.assertEquals(2, overlap.size());
        Assert.assertTrue(overlap.containsAll(Arrays.asList(
                new LongInterval(3L, 3L),
                new LongInterval(3L, 10L)
        )));

        // add intervals from [1, x] with x ∈ {1, n}
        final int n = 50;
        tree.clear();
        final List<Integer> shuffledValues = IntStream.range(1, n + 1)
                .boxed().collect(Collectors.toList());
        Collections.shuffle(shuffledValues);
        shuffledValues.forEach(i -> tree.insert(new IntegerInterval(1, i)));

        for (int k = 1; k <= n; k++) {
            final NumberInterval query = new IntegerInterval(k, k);

            overlap = tree.overlap(query);
            Assert.assertEquals(n - k + 1, overlap.size());
            for (int i = k; i <= n; i++) {
                Assert.assertTrue(overlap.contains(new IntegerInterval(1, i)));
            }
        }
    }

    protected boolean assertContains(final IntervalTree tree, final IInterval interval) {
        return tree.contains(interval);
    }

    protected void assertIsEmpty(final IntervalTree tree) {
        Assert.assertEquals(0, tree.size());
        Assert.assertTrue(tree.isEmpty());

        //noinspection RedundantOperationOnEmptyContainer
        Assert.assertFalse(tree.iterator().hasNext());
        Assert.assertFalse(tree.nodeIterator().hasNext());
        Assert.assertFalse(tree.positionIterator().hasNext());
    }

    @SuppressWarnings("SimplifiableIfStatement")
    public IntervalTree createRandomTree(final int inserts, final int deletes, final boolean balancing) {
        final IntervalTree tree = IntervalTreeBuilder.newBuilder()
                .usePredefinedType(IntervalType.NUMBER, false)
                .collectIntervals(key -> new ListIntervalCollection())
                .setAutoBalancing(balancing)
                .build();

        final int totalOperations = inserts + deletes;
        final List<IInterval> intervals = new ArrayList<>();
        final Random rnd = new Random();

        // load the intervals that will be inserted
        while (intervals.size() < inserts) {
            final int start = rnd.nextInt(900);
            final IInterval interval = new IntegerInterval(start, start + rnd.nextInt(100));

            if (!intervals.contains(interval)) {
                intervals.add(interval);
            }
        }

        int totalInserts = 0;
        int totalDeletes = 0;
        final List<IInterval> added = new ArrayList<>();

        try {
            for (int i = 0; i < totalOperations; i++) {
                final boolean insert;
                if (added.size() == 0) {
                    insert = true;
                } else if (totalInserts >= inserts) {
                    insert = false;
                } else if (totalDeletes >= deletes) {
                    insert = true;
                } else {
                    insert = rnd.nextInt(2) == 0;
                }

                if (insert) {
                    final IInterval interval = intervals.remove(rnd.nextInt(intervals.size()));
                    tree.insert(interval);
                    Assert.assertTrue(interval.toString(), added.add(interval));
                    totalInserts++;
                } else {
                    final IInterval interval = added.remove(rnd.nextInt(added.size()));
                    Assert.assertTrue(interval.toString(), tree.remove(interval));
                    totalDeletes++;
                }

                // validate the tree after each operation
                final AtomicLong counter = new AtomicLong(0L);
                final String ops = String.format("Operations %d/%d/%d", totalInserts, totalDeletes, totalOperations);
                tree.nodeIterator().forEachRemaining(node -> {
                    assertNode(node, tree, balancing);
                    Assert.assertTrue(counter.incrementAndGet() <= tree.size());

                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace(String.format("Checking %d/%d (%s)", counter.get(), tree.size(), ops));
                    }
                });
            }

            LOGGER.info(String.format("inserts: %d (planned: %d), deletes: %d (planned: %d)",
                    totalInserts, inserts, totalDeletes, deletes));
        } catch (final Throwable t) {
            LOGGER.error("Validation failed for: " + System.lineSeparator() + tree.toString(), t);
            throw t;
        }

        return tree;
    }

    public void assertNode(final IntervalTreeNode node, final IntervalTree tree, final boolean balancing) {
        if (node.isRoot()) {
            Assert.assertEquals(0, node.getLevel());
        }
        if (node.hasLeft()) {
            Assert.assertSame(node.getLeft().getParent(), node);
            Assert.assertEquals(node.getLevel() + 1, node.getLeft().getLevel());
            Assert.assertTrue(node + " < " + node.getLeft(),
                    (
                            node.compare(node.getStart(), node.getLeft().getStart()) > 0
                    ) || (
                            node.compare(node.getStart(), node.getLeft().getStart()) == 0 &&
                                    node.compare(node.getEnd(), node.getLeft().getEnd()) > 0
                    )
            );
        }
        if (node.hasRight()) {
            Assert.assertSame(node.getRight().getParent(), node);
            Assert.assertEquals(node.getLevel() + 1, node.getRight().getLevel());
            Assert.assertTrue(node + " > " + node.getRight(),
                    (
                            node.compare(node.getStart(), node.getRight().getStart()) < 0
                    ) || (
                            node.compare(node.getStart(), node.getRight().getStart()) == 0 &&
                                    node.compare(node.getEnd(), node.getRight().getEnd()) < 0
                    )
            );
        }

        Assert.assertEquals(node.getHeight(),
                Math.max(
                        node.hasLeft() ? node.getLeft().getHeight() : 0,
                        node.hasRight() ? node.getRight().getHeight() : 0
                ) + 1);
        Assert.assertEquals(node.toString(), 1, node.getIntervals().size());
        Assert.assertTrue(tree.contains(node.getIntervals().iterator().next()));
        Assert.assertTrue(tree.overlap(node.getIntervals().iterator().next()).size() > 0);

        // check the maximum value defined
        final Comparable leftMax = node.hasLeft() ? node.getLeft().getMax() : null;
        final Comparable rightMax = node.hasRight() ? node.getRight().getMax() : null;
        final Comparable leftRightMax = max(leftMax, rightMax);
        final Comparable nodeMax = max(node.getStart(), node.getEnd());
        final Comparable overallMax = max(nodeMax, leftRightMax);
        Assert.assertEquals(node.getMax(), overallMax);

        if (balancing) {
            Assert.assertTrue(tree.toString(), tree.isBalanced());
        }
    }

    @SuppressWarnings("unchecked")
    protected Comparable max(final Comparable a, final Comparable b) {
        if (a == null && b == null) {
            return null;
        } else if (a == null) {
            return b;
        } else if (b == null) {
            return a;
        } else {
            return a.compareTo(b) > 0 ? a : b;
        }
    }

    @Test
    public void testSaveAndLoad() throws IOException, ClassNotFoundException {
        final File treeFile = new File(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString());

        try {
            final IntervalTree tree = IntervalTreeBuilder.newBuilder()
                    .usePredefinedType(IntervalType.NUMBER, false)
                    .collectIntervals(interval -> new ListIntervalCollection())
                    .enableWriteCollections()
                    .build();

            tree.insert(new LongInterval(1L, 100L));
            tree.insert(new LongInterval(2L, 100L));
            tree.insert(new DoubleInterval(2.0, 100.0));
            tree.insert(new DoubleInterval(3.0, 100.0));
            tree.insert(new LongInterval(3L, 100L));
            tree.insert(new IntegerInterval(3, 100));

            IntervalTreeBuilder.saveToFile(treeFile, tree);

            // check what we saved
            Assert.assertEquals(tree.size(), 6);

            tree.insert(new DoubleInterval(4.0, 100.0));
            tree.insert(new LongInterval(4L, 100L));
            tree.insert(new IntegerInterval(4, 100));
            tree.insert(new IntegerInterval(4, 100));

            // check what we have after the modification
            Assert.assertEquals(tree.size(), 10);
            tree.nodeIterator().forEachRemaining(n ->
                    Assert.assertEquals(Number.class.cast(n.getStart()).intValue(), n.getIntervals().size()));

            final IntervalTree loadedTree = IntervalTreeBuilder.newBuilder()
                    .loadFromFile(treeFile)
                    .build();

            // check what we loaded
            Assert.assertEquals(loadedTree.size(), 6);
            loadedTree.nodeIterator().forEachRemaining(n ->
                    Assert.assertEquals(Number.class.cast(n.getStart()).intValue(), n.getIntervals().size()));
        } catch (final Exception e) {
            e.printStackTrace();
            Assert.assertNull(e.getMessage(), e);
        } finally {
            Assert.assertTrue(treeFile.delete());
        }
    }

    @Test
    public void testToArray() {
        final IntervalTree tree = IntervalTreeBuilder.newBuilder()
                .usePredefinedType(IntervalType.NUMBER, false)
                .collectIntervals(interval -> new ListIntervalCollection())
                .enableWriteCollections()
                .build();

        IInterval[] result;

        // check the empty result
        result = tree.toArray(new IInterval[0]);
        Assert.assertEquals(0, result.length);

        // check if fields are empty
        result = tree.toArray(new IInterval[10]);
        Assert.assertEquals(10, result.length);
        for (int i = 0; i < 10; i++) {
            Assert.assertNull(result[i]);
        }

        // add a value in the tree and check that it is in the array
        final DoubleInterval addedInterval = new DoubleInterval(4.0, 100.0);
        tree.insert(addedInterval);

        result = tree.toArray(new IInterval[10]);
        Assert.assertEquals(addedInterval, result[0]);
        for (int i = 1; i < 10; i++) {
            Assert.assertNull(result[i]);
        }
    }
}
