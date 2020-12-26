package com.brein.time.timeintervals.indexes;

import com.brein.time.timeintervals.collections.IntervalCollection;
import com.brein.time.timeintervals.collections.IntervalCollectionFactory;
import com.brein.time.timeintervals.collections.IntervalCollectionObserver;
import com.brein.time.timeintervals.collections.ObservableIntervalCollection;
import com.brein.time.timeintervals.collections.UnmodifiableIntervalCollection;
import com.brein.time.timeintervals.filters.IntervalFilter;
import com.brein.time.timeintervals.intervals.IInterval;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;

public class IntervalTreeNode extends IntervalTreeNodeContext
        implements Externalizable, Iterable<IInterval>, Comparable<IntervalTreeNode> {
    private transient WeakReference<IntervalCollection> referenceCollection;
    private IntervalCollection collection;

    private String key;
    private Comparable start;
    private Comparable end;

    private Comparable max;
    private long level;
    private long height;

    private IntervalTreeConfiguration configuration;

    public void init(final IInterval interval) {
        this.start = interval.getNormStart();
        this.end = interval.getNormEnd();
        this.max = interval.getNormEnd();
        this.key = interval.getUniqueIdentifier();

        this.level = 0L;
        this.height = 1L;
    }

    public Comparable getStart() {
        return start;
    }

    public Comparable getEnd() {
        return end;
    }

    public Comparable getMax() {
        return max;
    }

    public void setMax(final Comparable max) {
        if (compare(this.max, max) == 0) {
            return;
        }
        this.max = max;

        if (hasParent()) {
            getParent().updateMax();
        }
    }

    public long getLevel() {
        return level;
    }

    public void setLevel(final long level) {
        if (this.level == level) {
            return;
        }
        this.level = level;

        if (hasLeft()) {
            getLeft().setLevel(level + 1);
        }
        if (hasRight()) {
            getRight().setLevel(level + 1);
        }
    }

    @SuppressWarnings("unchecked")
    public void updateMax() {
        if (isLeaf()) {
            setMax(this.end);
        } else if (isSingleParent()) {
            final Comparable singleChildMax = getSingleChild().max;
            setMax(compare(this.end, singleChildMax) < 0 ? singleChildMax : this.end);
        } else {
            final Comparable leftMax = getLeft().max;
            final Comparable rightMax = getRight().max;
            final Comparable thisMax = this.end;

            if (compare(leftMax, rightMax) < 0) {
                setMax(compare(thisMax, rightMax) < 0 ? rightMax : thisMax);
            } else {
                setMax(compare(thisMax, leftMax) < 0 ? leftMax : thisMax);
            }
        }
    }

    public long getHeight() {
        return height;
    }

    public void setHeight(final long height) {
        if (this.height == height) {
            return;
        }
        this.height = height;

        if (hasParent()) {
            getParent().updateHeight();
        }
    }

    public void updateHeight() {
        if (isLeaf()) {
            setHeight(1L);
        } else if (isSingleParent()) {
            setHeight(getSingleChild().getHeight() + 1);
        } else {
            setHeight(Math.max(getLeft().getHeight(), getRight().getHeight()) + 1);
        }
    }

    public IntervalCollection getIntervals() {
        return new UnmodifiableIntervalCollection(getCollection());
    }

    public boolean addInterval(final IInterval interval) {
        assert this.key.equals(interval.getUniqueIdentifier());
        assert compareTo(interval) == 0;

        return getCollection().add(interval);
    }

    public boolean isEmpty() {
        return getCollection().isEmpty();
    }

    public boolean removeInterval(final IInterval interval) {
        return getCollection().remove(interval);
    }

    public Collection<IInterval> find(final IInterval interval,
                                      final IntervalFilter filter) {
        return getCollection().find(interval, this.configuration.getValueComparator(), filter);
    }

    public String getId() {
        return String.format("[%s, %s]", this.start, this.end);
    }

    @Override
    @SuppressWarnings("NullableProblems")
    public int compareTo(final IntervalTreeNode node) {
        return compareTo(node.start, node.end);
    }

    public int compareTo(final IInterval interval) {
        return compareTo(interval.getNormStart(), interval.getNormEnd());
    }

    public int compareTo(final Comparable start, final Comparable end) {

        final int cmpStart = compare(this.start, start);
        if (cmpStart < 0) {
            return -1;
        } else if (cmpStart == 0) {
            return compare(this.end, end);
        } else {
            return 1;
        }
    }

    public int compare(final Object val1, final Object val2) {
        return this.configuration.getValueComparator().compare(val1, val2);
    }

    @Override
    public void setLeft(final IntervalTreeNode left) {
        setChild(left, IntervalTreeNodeChildType.LEFT);
    }

    @Override
    public void setRight(final IntervalTreeNode right) {
        setChild(right, IntervalTreeNodeChildType.RIGHT);
    }

    @Override
    public String toString() {
        return String.format("[%s, %s] (max: %s, count: %d, level: %d, height: %d)",
                this.start, this.end, this.max, getCollection().size(), this.level, this.height);
    }

    protected void setChild(final IntervalTreeNode node, final IntervalTreeNodeChildType childType) {
        if (IntervalTreeNodeChildType.LEFT.equals(childType)) {
            super.setLeft(node);
        } else if (IntervalTreeNodeChildType.RIGHT.equals(childType)) {
            super.setRight(node);
        }

        // set the new parent
        if (node != null) {
            node.setParent(this);
            node.setLevel(this.level + 1);
        }

        updateMax();
        updateHeight();
    }

    protected IntervalTreeNode get(final IntervalTreeNodeChildType childType) {
        if (IntervalTreeNodeChildType.LEFT.equals(childType)) {
            return getLeft();
        } else if (IntervalTreeNodeChildType.RIGHT.equals(childType)) {
            return getRight();
        } else {
            return null;
        }
    }

    public IntervalTreeNodeContext detach() {
        final IntervalTreeNodeContext ctx = new IntervalTreeNodeContext(this);

        // remove it from the parent
        final IntervalTreeNode parent = this.getParent();
        if (parent != null) {
            parent.removeChild(this);
        }

        this.setParent(null);
        this.setLeft(null);
        this.setRight(null);
        this.max = end;
        this.level = 0L;

        return ctx;
    }

    public void removeChild(final IntervalTreeNode node) {
        replaceChild(node, IntervalTreeNode.class.cast(null));
    }

    public void replaceChild(final IntervalTreeNode replacement, final IntervalTreeNodeChildType childType) {
        if (IntervalTreeNodeChildType.LEFT.equals(childType)) {
            setLeft(replacement);
        } else if (IntervalTreeNodeChildType.RIGHT.equals(childType)) {
            setRight(replacement);
        }
    }

    public void replaceChild(final IntervalTreeNode replace, final IntervalTreeNode replacement) {
        if (getLeft() == replace) {
            setLeft(replacement);
        } else if (getRight() == replace) {
            setRight(replacement);
        }
    }

    @Override
    public Iterator<IInterval> iterator() {
        return getCollection().iterator();
    }

    public IntervalTreeNodeChildType determineChildType() {
        if (!hasParent()) {
            return IntervalTreeNodeChildType.NONE;
        } else if (getParent().getLeft() == this) {
            return IntervalTreeNodeChildType.LEFT;
        } else if (getParent().getRight() == this) {
            return IntervalTreeNodeChildType.RIGHT;
        } else {
            return IntervalTreeNodeChildType.NONE;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(getStart(), getEnd());
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (IntervalTreeNode.class.equals(obj.getClass())) {
            final IntervalTreeNode node = IntervalTreeNode.class.cast(obj);
            return compareTo(node) == 0;
        } else {
            return false;
        }
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        out.writeObject(this.key);
        out.writeObject(this.start);
        out.writeObject(this.end);
        out.writeObject(this.max);
        out.writeLong(this.level);
        out.writeLong(this.height);

        if (this.configuration.isWritingCollectionsToFile()) {

            // we never want to write the observable (it's not even serializable)
            if (ObservableIntervalCollection.class.isInstance(this.collection)) {
                final ObservableIntervalCollection observable =
                        ObservableIntervalCollection.class.cast(this.collection);

                // write the wrapped instance and register the observing factory later
                out.writeObject(observable.getWrappedCollection());
            } else {

                // write the default collection as is
                out.writeObject(this.collection);
            }
        }

        writeChild(out, IntervalTreeNodeChildType.LEFT);
        writeChild(out, IntervalTreeNodeChildType.RIGHT);
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        this.key = String.class.cast(in.readObject());
        this.start = Comparable.class.cast(in.readObject());
        this.end = Comparable.class.cast(in.readObject());
        this.max = Comparable.class.cast(in.readObject());
        this.level = in.readLong();
        this.height = in.readLong();

        if (this.configuration.isWritingCollectionsToFile()) {
            this.collection = wrapCollection(IntervalCollection.class.cast(in.readObject()));
        }

        readChild(in, IntervalTreeNodeChildType.LEFT);
        readChild(in, IntervalTreeNodeChildType.RIGHT);
    }

    protected void writeChild(final ObjectOutput out,
                              final IntervalTreeNodeChildType type) throws IOException {
        if (hasChild(type)) {
            out.writeBoolean(true);
            getChild(type).writeExternal(out);
        } else {
            out.writeBoolean(false);
        }
    }

    protected void readChild(final ObjectInput in,
                             final IntervalTreeNodeChildType type) throws IOException, ClassNotFoundException {
        final boolean hasChild = in.readBoolean();
        final IntervalTreeNode node;
        if (hasChild) {
            node = new IntervalTreeNode();
            node.setConfiguration(this.configuration);
            node.readExternal(in);
        } else {
            node = null;
        }

        setChild(node, type);
    }

    public void setConfiguration(final IntervalTreeConfiguration configuration) {
        this.configuration = configuration;
    }

    protected IntervalCollection getCollection() {
        final IntervalCollectionFactory factory = this.configuration.getFactory();
        if (factory == null) {
            return IntervalCollectionFactory.shallow();
        } else if (this.collection != null) {
            return this.collection;
        }

        if (factory.useWeakReferences()) {
            if (this.referenceCollection != null) {
                final IntervalCollection reference = this.referenceCollection.get();

                if (reference != null) {

                    // we have a reference, which contains whatever we need
                    return reference;
                }
            }
        } else {

            // this.collection must be null at this point
            assert this.collection == null;
        }

        // get the value from the factory
        return wrapCollection(factory.load(this.key));
    }

    protected IntervalCollection wrapCollection(final IntervalCollection collection) {
        final IntervalCollectionFactory factory = this.configuration.getFactory();
        if (factory == null) {
            return collection;
        }

        final IntervalCollection wrappedCollection;

        // check if the factory needs observable instances
        if (IntervalCollectionObserver.class.isInstance(factory) &&
                !ObservableIntervalCollection.class.isInstance(collection)) {
            wrappedCollection = new ObservableIntervalCollection(IntervalCollectionObserver.class.cast(factory),
                    collection);
        } else {
            wrappedCollection = collection;
        }

        // if we got so far, we want to keep the result
        if (factory.useWeakReferences()) {
            this.collection = null;
            this.referenceCollection = new WeakReference<>(wrappedCollection);
        } else {
            this.collection = wrappedCollection;
            this.referenceCollection = null;
        }

        return wrappedCollection;
    }
}
