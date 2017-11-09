package com.brein.time.timeintervals.indexes;

import com.brein.time.exceptions.FailedIO;
import com.brein.time.exceptions.IllegalConfiguration;
import com.brein.time.timeintervals.filters.IntervalFilter;
import com.brein.time.timeintervals.intervals.IInterval;
import org.apache.log4j.Logger;

import java.io.Externalizable;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("NullableProblems")
public class IntervalTree implements Collection<IInterval>, Externalizable {
    private static final Logger LOGGER = Logger.getLogger(IntervalTree.class);

    private transient IntervalTreeConfiguration configuration = null;
    protected IntervalTreeNode root = null;

    private long size = 0L;

    public Collection<IInterval> find(final IInterval query) {
        return find(query, this.configuration.getIntervalFilter());
    }

    public Collection<IInterval> find(final IInterval query, final IntervalFilter filter) {
        if (this.root == null) {
            return Collections.emptyList();
        } else {
            return _find(this.root, query, filter);
        }
    }

    /**
     * Returns the <b>current</b> root of the tree. It is not recommended to store the result of this method in any
     * other than a local variable. The root of the tree will change whenever it is rebalanced.
     *
     * @return the current root of the tree
     *
     * @see #balance()
     */
    protected IntervalTreeNode getRoot() {
        return root;
    }

    protected Collection<IInterval> _find(final IntervalTreeNode node,
                                          final IInterval query,
                                          final IntervalFilter filter) {
        if (node == null) {
            return Collections.emptyList();
        }

        // check if the current node overlaps
        final int cmpNode = node.compareTo(query);
        if (cmpNode == 0) {
            return node.find(query, filter);
        } else if (cmpNode < 0) {
            return _find(node.getRight(), query, filter);
        } else {
            return _find(node.getLeft(), query, filter);
        }
    }

    public Stream<IInterval> overlapStream(final IInterval query) {
        if (this.root == null) {
            return Stream.empty();
        } else {
            return _overlap(this.root, query);
        }
    }

    public Collection<IInterval> overlap(final IInterval query) {
        if (this.root == null) {
            return Collections.emptyList();
        } else {
            return _overlap(this.root, query).collect(Collectors.toList());
        }
    }

    protected Stream<IInterval> _overlap(final IntervalTreeNode node, final IInterval query) {

        if (node == null) {
            return Stream.empty();
        }

        // we create three streams:
        //  1. the one of the current node
        //  2. the one coming from the left
        //  3. the one coming form the right
        final Stream<IInterval> nodeStream;
        final Stream<IInterval> leftNodeStream;
        final Stream<IInterval> rightNodeStream;

        if (node.compare(node.getStart(), query.getNormEnd()) <= 0 &&
                node.compare(node.getEnd(), query.getNormStart()) >= 0) {
            nodeStream = node.getIntervals().stream();
        } else {
            nodeStream = Stream.empty();
        }

        if (node.hasLeft() && node.compare(node.getLeft().getMax(), query.getNormStart()) >= 0) {
            leftNodeStream = this._overlap(node.getLeft(), query);
        } else {
            leftNodeStream = Stream.empty();
        }

        rightNodeStream = this._overlap(node.getRight(), query);

        return Stream.of(nodeStream, leftNodeStream, rightNodeStream).flatMap(s -> s);
    }

    public IntervalTree insert(final IInterval interval) {
        add(interval);
        return this;
    }

    protected IntervalTreeNode _add(final IntervalTreeNode node,
                                    final IInterval interval,
                                    final AtomicBoolean changed) {
        if (node == null) {
            changed.set(true);
            return createNode(interval);
        }

        final IntervalTreeNodeChildType childType;
        final int cmpNode = node.compareTo(interval);
        if (cmpNode == 0) {
            changed.set(node.addInterval(interval));
            return node;
        } else if (cmpNode < 0) {
            childType = IntervalTreeNodeChildType.RIGHT;
        } else {
            childType = IntervalTreeNodeChildType.LEFT;
        }

        // add the node to the child and replace the returned value
        node.setChild(_add(node.getChild(childType), interval, changed), childType);

        // the node may have changed, thus we may have to re-balance
        return isAutoBalancing() ? balance(node) : node;
    }

    protected IntervalTreeNode createNode(final IInterval interval) {
        final IntervalTreeNode node = new IntervalTreeNode();
        node.setConfiguration(this.configuration);
        node.init(interval);
        node.addInterval(interval);

        return node;
    }

    public void balance() {
        this.nodeIterator().forEachRemaining(node -> {
            if (node.isLeaf()) {
                // nothing to do
            } else if (node.isRoot()) {
                this.root = balance(node);
            } else {
                node.setLeft(balance(node.getLeft()));
                node.setRight(balance(node.getRight()));
            }
        });
    }

    public boolean isBalanced() {
        return isBalanced(this.root);
    }

    @SuppressWarnings("SimplifiableIfStatement")
    protected boolean isBalanced(final IntervalTreeNode node) {
        if (node == null) {
            return true;
        }
        return Math.abs(determineBalance(node)) <= 1L &&
                isBalanced(node.getLeft()) && isBalanced(node.getRight());
    }

    protected IntervalTreeNode balance(final IntervalTreeNode node) {

        // check the balance
        final long balance = determineBalance(node);
        if (Math.abs(balance) <= 1) {
            return node;
        }

        // find the deepest unbalanced sub-tree (there may be more down the road)

        // validate the different four cases four unbalanced tree's
        final long balanceLeft = balance > 1L ? determineBalance(node.getLeft()) : 0L;
        final long balanceRight = balance < -1L ? determineBalance(node.getRight()) : 0L;

        // Left Left Case
        if (balance > 1 && balanceLeft >= 0) {
            return rightRotate(node);
        }
        // Right Right Case
        else if (balance < -1 && balanceRight <= 0) {
            return leftRotate(node);
        }
        // Left Right Case
        else if (balance > 1 && balanceLeft < 0) {
            node.setLeft(leftRotate(node.getLeft()));
            return rightRotate(node);
        }
        // Right Left Case
        else if (balance < -1 && balanceRight > 0) {
            node.setRight(rightRotate(node.getRight()));
            return leftRotate(node);
        }
        // any other Case, no changes - should never happen
        else {
            LOGGER.warn(String.format("Balancing node '%s' reached an unexpected state (b: %d, l: %d, r: %d)",
                    node, balance, balanceLeft, balanceRight));
            LOGGER.warn(this);
            return node;
        }
    }

    public IntervalTree delete(final IInterval interval) {
        remove(interval);
        return this;
    }

    protected IntervalTreeNode _remove(final IntervalTreeNode node,
                                       final IInterval interval,
                                       final AtomicBoolean changed) {
        if (node == null) {
            changed.set(false);
            return null;
        }

        final IntervalTreeNodeChildType childType;
        final int cmpNode = node.compareTo(interval);
        if (cmpNode == 0) {
            if (node.removeInterval(interval)) {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Removed interval '" + interval + "' from node '" + node + "'.");
                }

                changed.set(true);
                return removeEmptyNode(node);
            } else {
                changed.set(false);
                return node;
            }
        } else if (cmpNode < 0) {
            childType = IntervalTreeNodeChildType.RIGHT;
        } else {
            childType = IntervalTreeNodeChildType.LEFT;
        }

        // add the node to the child and replace the returned value
        node.setChild(_remove(node.getChild(childType), interval, changed), childType);

        return isAutoBalancing() ? balance(node) : node;
    }

    /**
     * Removes an empty node (i.e., does not contain any intervals). When removing an empty node, the resulting tree
     * must be rebalanced (if activated, see {@link #isAutoBalancing()}). The method returns the re-organized, balanced
     * sub-tree. The returned sub-tree can be at most better (in height) by one - if the tree was balanced before.
     * <p>
     * - http://www.mathcs.emory.edu/~cheung/Courses/323/Syllabus/Trees/AVL-delete.html<br/>
     * - http://quiz.geeksforgeeks.org/binary-search-tree-set-2-delete/
     *
     * @param node the node which contained the removed interval
     *
     * @return the new root to be used in replacement for the passed {@code node}
     */
    protected IntervalTreeNode removeEmptyNode(final IntervalTreeNode node) {
        if (!node.isEmpty()) {
            return node;
        }

        /*
         * There is a different between the new root (rootNode) of the sub-tree and the position where the
         * action really took place (i.e., the parent of the node, which was modified). To understand the difference,
          * it is best to look at:
         *
         * - http://www.mathcs.emory.edu/~cheung/Courses/323/Syllabus/Trees/AVL-delete.html
         */
        final IntervalTreeNode rootNode;
        final IntervalTreeNode actionNode;

        // the empty not is a leaf, just remove it
        if (node.isLeaf()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Removing leaf '" + node + "' from tree.");
            }

            rootNode = null;
            actionNode = null; // the action-node would be node.getParent(), but will be balanced in the _remove
        }
        // we have an empty node, which just have one parent, so we just keep that one parent
        else if (node.isSingleParent()) {
            rootNode = node.getSingleChild();
            actionNode = null; // the action-node would be node.getParent(), but will be balanced in the _remove

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Removing node '" + node + "' and replacing with '" +
                        rootNode + "' from tree.");
            }
        }
        // we have two sub-trees
        else {
            rootNode = findLeftLeaf(node.getRight());
            actionNode = node == rootNode.getParent() ? null : rootNode.getParent();

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Removing node '" + node + "' and replacing with smallest '" +
                        rootNode + "' from tree.");
            }

            final IntervalTreeNodeContext nodeCtx = node.detach();
            final IntervalTreeNodeChildType replacementChildType = rootNode.determineChildType();
            final IntervalTreeNodeContext replacementCtx = rootNode.detach();

            // if the replacementCtx has children, we have to move them to the old parent (can only have one)
            if (replacementCtx.isSingleParent()) {
                replacementCtx.getParent().setChild(replacementCtx.getSingleChild(), replacementChildType);
            }

            // it may be that the node had the smallestNode as child, in that case we have to keep the old child
            if (nodeCtx.getLeft() == rootNode) {
                rootNode.setLeft(replacementCtx.getLeft());

                rootNode.setRight(nodeCtx.getRight());
                rootNode.setParent(nodeCtx.getParent());
            } else if (nodeCtx.getRight() == rootNode) {
                rootNode.setRight(replacementCtx.getRight());

                rootNode.setLeft(nodeCtx.getLeft());
                rootNode.setParent(nodeCtx.getParent());
            } else {
                rootNode.setContext(nodeCtx);
            }
        }

        // if we replace the root, we have to let the
        if (rootNode != null && node.isRoot()) {
            rootNode.setParent(null);
            rootNode.setLevel(0L);
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("rootNode: " + rootNode);
            LOGGER.trace("actionNode: " + actionNode);
        }

        if (!isAutoBalancing()) {
            return rootNode;
        } else if (rootNode == null) {
            return null;
        } else if (actionNode == null) {
            return balance(rootNode);
        } else {

            /*
             * In this case, we have to follow up from the actionNode up to the rootNode
             * and make sure everything is balanced within this area.
             */
            IntervalTreeNodeChildType childType = actionNode.determineChildType();
            IntervalTreeNode n = actionNode.getParent();

            do {
                final IntervalTreeNode child = n.getChild(childType);
                n.setChild(balance(child), childType);

                if (n == rootNode) {
                    break;
                }

                childType = n.determineChildType();
                n = n.getParent();
            } while (true);

            return balance(rootNode);
        }
    }

    // Get Balance factor of node N
    protected long determineBalance(final IntervalTreeNode node) {
        if (node == null) {
            return 0L;
        }

        return (node.hasLeft() ? node.getLeft().getHeight() : 0) -
                (node.hasRight() ? node.getRight().getHeight() : 0);
    }

    protected IntervalTreeNode leftRotate(final IntervalTreeNode node) {
        final IntervalTreeNode right = node.getRight();

        final IntervalTreeNodeContext rightCtx = right.detach();
        final IntervalTreeNodeContext nodeCtx = node.detach();

        node.setLeft(nodeCtx.getLeft());
        node.setRight(rightCtx.getLeft());
        right.setLeft(node);
        right.setRight(rightCtx.getRight());

        return right;
    }

    protected IntervalTreeNode rightRotate(final IntervalTreeNode node) {
        final IntervalTreeNode left = node.getLeft();

        final IntervalTreeNodeContext leftCtx = left.detach();
        final IntervalTreeNodeContext nodeCtx = node.detach();

        node.setLeft(leftCtx.getRight());
        node.setRight(nodeCtx.getRight());
        left.setRight(node);
        left.setLeft(leftCtx.getLeft());

        return left;
    }

    protected IntervalTreeNode findLeftLeaf(final IntervalTreeNode startNode) {
        if (startNode == null) {
            return null;
        }

        IntervalTreeNode node = startNode;
        while (node.getLeft() != null) {
            node = node.getLeft();
        }

        return node;
    }

    protected PositionedNode findLeftLeaf(final PositionedNode posNode) {
        if (posNode == null || posNode.getNode() == null) {
            return null;
        }

        IntervalTreeNode node = posNode.getNode();
        long offset = 0;
        while (node.getLeft() != null) {
            node = node.getLeft();
            offset++;
        }

        return PositionedNode.moveLeft(node, posNode, offset);
    }

    @Override
    public int size() {
        return size < Integer.MAX_VALUE ? Long.valueOf(size).intValue() : Integer.MAX_VALUE;
    }

    @Override
    public boolean isEmpty() {
        return this.root == null;
    }

    @Override
    @SuppressWarnings("SimplifiableIfStatement")
    public boolean contains(final Object o) {

        if (o instanceof IInterval) {
            return !find(IInterval.class.cast(o)).isEmpty();
        } else {
            return false;
        }
    }

    @Override
    public Iterator<IInterval> iterator() {
        final Iterator<IntervalTreeNode> outerNodeIt = nodeIterator();

        return new Iterator<IInterval>() {
            private Iterator<IInterval> nodeCollectionIt = null;
            private IInterval next = findNext();

            @Override
            public boolean hasNext() {
                return this.next != null;
            }

            protected IInterval findNext() {
                if (this.nodeCollectionIt != null && this.nodeCollectionIt.hasNext()) {
                    // nothing to do, next will return something
                } else if (outerNodeIt.hasNext()) {
                    this.nodeCollectionIt = outerNodeIt.next().iterator();
                } else {
                    return null;
                }

                return this.nodeCollectionIt.next();
            }

            @Override
            public IInterval next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }

                final IInterval result = this.next;
                this.next = findNext();

                return result;
            }

        };
    }

    @Override
    public Object[] toArray() {
        if (this.size() == 0L) {
            return new IInterval[0];
        }

        final IInterval[] intervals = new IInterval[size()];
        final AtomicInteger pos = new AtomicInteger(0);
        iterator().forEachRemaining(i -> intervals[pos.getAndIncrement()] = i);

        return intervals;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(final T[] arr) {

        final T[] intervals;
        if (arr.length < this.size) {
            intervals = (T[]) Array.newInstance(arr.getClass().getComponentType(), size());
        } else {
            intervals = arr;
        }

        final AtomicInteger pos = new AtomicInteger(0);
        iterator().forEachRemaining(i -> intervals[pos.getAndIncrement()] = (T) i);

        for (int i = intervals.length; i < size(); i++) {
            intervals[i] = null;
        }

        return intervals;
    }

    @Override
    public boolean add(final IInterval interval) {
        final AtomicBoolean changed = new AtomicBoolean(false);

        this.root = _add(this.root, interval, changed);
        this.size += changed.get() ? 1 : 0;

        return changed.get();
    }

    @Override
    @SuppressWarnings("SimplifiableIfStatement")
    public boolean remove(final Object o) {
        final IInterval interval;
        if (o instanceof IInterval) {
            interval = IInterval.class.cast(o);
        } else {
            return false;
        }

        final AtomicBoolean changed = new AtomicBoolean(false);

        this.root = _remove(this.root, interval, changed);
        this.size -= changed.get() ? 1 : 0;

        return changed.get();
    }

    @Override
    public boolean containsAll(final Collection<?> c) {
        for (final Object o : c) {
            if (!contains(o)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean addAll(final Collection<? extends IInterval> c) {
        final AtomicBoolean changed = new AtomicBoolean(false);
        c.forEach(interval -> changed.compareAndSet(false, add(interval)));
        return changed.get();
    }

    @Override
    public boolean removeAll(final Collection<?> c) {
        final AtomicBoolean changed = new AtomicBoolean(false);
        c.forEach(interval -> changed.compareAndSet(false, remove(interval)));
        return changed.get();
    }

    @Override
    public boolean retainAll(final Collection<?> c) {
        final List<IInterval> contained = c.stream()
                .filter(this::contains)
                .map(IInterval.class::cast)
                .collect(Collectors.toList());

        // if there is nothing to be removed, we can return false
        if (contained.size() == this.size()) {
            return false;
        }

        clear();
        addAll(contained);

        return true;
    }

    @Override
    public void clear() {
        this.root = null;
        this.size = 0;
    }

    public Iterator<IntervalTreeNode> nodeIterator() {
        final IntervalTreeNode outerFirstNext = findLeftLeaf(this.root);

        return new Iterator<IntervalTreeNode>() {
            private IntervalTreeNode next = outerFirstNext;

            @Override
            public boolean hasNext() {
                return this.next != null;
            }

            @Override
            public IntervalTreeNode next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }

                final IntervalTreeNode result = this.next;
                if (this.next.getRight() != null) {
                    this.next = findLeftLeaf(this.next.getRight());
                    return result;
                } else {
                    while (true) {
                        final IntervalTreeNode parent = this.next.getParent();
                        if (parent == null) {
                            this.next = null;
                            return result;
                        } else if (parent.getLeft() == this.next) {
                            this.next = parent;
                            return result;
                        } else {
                            this.next = parent;
                        }
                    }
                }
            }
        };
    }

    public Iterator<PositionedNode> positionIterator() {
        final PositionedNode outerFirstNext = findLeftLeaf(new PositionedNode(this.root, 0L, 0L));

        return new Iterator<PositionedNode>() {
            private PositionedNode next = outerFirstNext;

            @Override
            public boolean hasNext() {
                return next != null;
            }

            @Override
            public PositionedNode next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }

                final PositionedNode result = this.next;
                final IntervalTreeNode right = this.next.getNode().getRight();

                if (right == null) {
                    while (true) {
                        final IntervalTreeNode node = this.next.getNode();
                        final IntervalTreeNode parent = node.getParent();

                        if (parent == null) {
                            this.next = null;
                            return result;
                        }

                        final PositionedNode posParent = PositionedNode.moveUp(parent, this.next, 1L);
                        if (parent.getLeft() == node) {
                            this.next = posParent;
                            return result;
                        } else {
                            this.next = posParent;
                        }
                    }
                } else {
                    final PositionedNode posRight = PositionedNode.moveRight(right, this.next, 1L);
                    this.next = findLeftLeaf(posRight);
                    return result;
                }
            }
        };
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        toString("", this.root, sb, true);
        return sb.toString();
    }

    private void toString(final String prefix,
                          final IntervalTreeNode node,
                          final StringBuilder sb,
                          final boolean tail) {

        sb.append(prefix)
                .append(tail ? "└── " : "├── ")
                .append(node)
                .append(System.lineSeparator());

        if (node == null || node.isLeaf()) {
            return;
        }

        final String newPrefix = prefix + (tail ? "    " : "│   ");
        toString(newPrefix, node.getLeft(), sb, false);
        toString(newPrefix, node.getRight(), sb, true);
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        out.writeLong(this.size);

        if (this.root != null) {
            this.root.writeExternal(out);
        }
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        this.size = in.readLong();

        if (this.size > 0) {
            this.root = new IntervalTreeNode();
            this.root.setConfiguration(this.configuration);
            this.root.readExternal(in);
        }
    }

    public boolean isAutoBalancing() {
        return this.configuration.isAutoBalancing();
    }

    public IntervalTreeConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(final IntervalTreeConfiguration configuration) {
        if (this.root != null) {
            throw new IllegalConfiguration("The configuration cannot be changed once the tree is created.");
        }

        this.configuration = configuration;
    }

    public void saveToFile(final File file) throws FailedIO {
        IntervalTreeBuilder.saveToFile(file, this);
    }
}
