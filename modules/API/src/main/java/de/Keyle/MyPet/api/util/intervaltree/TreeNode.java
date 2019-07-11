/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2019 Keyle
 * MyPet is licensed under the GNU Lesser General Public License.
 *
 * MyPet is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyPet is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.api.util.intervaltree;

import java.util.*;

/**
 * A representation of a single node in the {@link IntervalTree}.
 * <p>
 * Since the Interval Tree is practically a binary search tree, every node is
 * identified by a unique key - the center (called middlepoint) of the first
 * interval that triggers the creation of this node. The key is immutable and
 * can't be changed during the lifespan of this node. It can however move up or
 * down in the tree as a result of balancing rotations, similarly to a normal
 * balanced search tree. Unlike a traditional binary search tree, every node in
 * the interval tree can store multiple intervals. The invariant is, that each node
 * stores only intervals that contain its middlepoint.
 * </p>
 * <p>
 * There are several invariants that the {@code TreeNode} needs to keep. First,
 * the subtrees rooted in each node are always kept balanced. Second, each node
 * must contain at least one interval, otherwise the node must be removed from
 * the tree to keep it as small as possible. Balancing is done by traditional
 * binary search tree rotations.
 * </p>
 * <p>
 * Given an interval that needs to be found, inserted or removed from the tree,
 * an augmented binary search algorithm is performed in the following fashion.
 * For each visited node, check if the middlepoint of this node is contained in
 * the query interval. If so, then this node is the position where the query interval
 * must be stored. Otherwise, visit the left child, if the interval is entirely
 * to the left of the middlepoint, or visit the right child otherwise.
 * </p>
 * <p>
 * Since intervals may contain middlepoints from multiple nodes in the tree,
 * there are multiple valid locations where an interval may be stored based
 * on the above augmented binary search algorithm. However, to ensure the
 * optimal time complexity of the {@link IntervalTree#add(Interval) add},
 * {@link IntervalTree#remove(Object) remove} and
 * {@link IntervalTree#contains(Object) contains} operations, we define the base
 * position of an interval (or base) as the node closest to root within the set of
 * nodes, where the interval may be stored. To be able to guarantee the
 * correctness of the algorithm, this invariant must be preserved after each
 * tree operation. This is especially important, when tree rotations are
 * performed. Whenever a node is promoted closer to the root as a result of a
 * rotation, the promoted node must assimilate all intervals from the node demoted
 * by the rotation. This ensures that whenever the binary search algorithm encounters
 * a node, which is a valid location for a given interval, this node will be the base
 * of the interval.
 * </p>
 * <p>
 * Since each node can store multiple intervals, we need an efficient way
 * to perform intersecting queries (given a query in the form of a point or
 * an interval, find all intervals intersecting the query). The {@code TreeNode}
 * can't simply keep an ArrayList of all intervals stored in it, because
 * it will have to linearly check each interval in the List. This can
 * degrade the performance of the tree, especially in cases where a single
 * node contains all intervals. Intervals have to be stored in an ordered
 * way. Each node keeps its intervals in two {@link TreeSet}s at the same
 * time - one containing the intervals by their start points in ascending order
 * and the other by the end points in descending order. Whenever we have to provide
 * a set of all intervals contained within this node, that intersect a query point,
 * we check if the query is left or right of the middlepoint of the node.
 * If it is left, we iterate through the TreeSet ordered by the start points
 * until we reach an interval completely to the right of the query. Analogously,
 * if the query is to the right of the middlepoint, we iterate through the set
 * ordered by the end points until we reach an interval that's completely to
 * the left of the query. This allows us to only iterate through these intervals,
 * that we will actually return as a result of the query.
 * </p>
 *
 * @param <T> The type for the start and end point of the interval
 */
public class TreeNode<T extends Comparable<? super T>, S> implements Iterable<Interval<T, S>> {

    /**
     * A set containing all {@link Interval}s stored in this node, ordered by their
     * starting points.
     *
     * @see Interval#sweepLeftToRight
     */
    protected NavigableSet<Interval<T, S>> increasing;

    /**
     * A set containing all {@link Interval}s stored in this node, ordered by their
     * end points.
     *
     * @see Interval#sweepRightToLeft
     */
    protected NavigableSet<Interval<T, S>> decreasing;

    /**
     * A pointer to the left child of the current node. The left child must either be
     * {@code null} or have a midpoint, smaller than the midpoint of the current node. More
     * formally, {@code left.midpoint.compareTo(this.midpoint) < 0} must evaluate to {@code true}.
     */
    protected TreeNode<T, S> left;

    /**
     * A pointer to the right child of the current node. The right child must either be
     * {@code null} or have a midpoint, larger than the midpoint of the current node. More
     * formally, {@code right.midpoint.compareTo(this.midpoint) > 0} must evaluate to {@code true}.
     */
    protected TreeNode<T, S> right;

    /**
     * The midpoint of the initial interval added to the node. It is an immutable value
     * and can not be changed, even if the initial interval has been removed from the
     * node.
     */
    protected final T midpoint;

    /**
     * The height of the node.
     */
    protected int height;

    /**
     * Instantiates a new node in an {@link IntervalTree}.
     *
     * @param interval The initial interval stored in the node. The middlepoint of
     *                 the node will be set based on this interval.
     */
    public TreeNode(Interval<T, S> interval) {
        decreasing = new TreeSet<>(Interval.sweepRightToLeft);
        increasing = new TreeSet<>(Interval.sweepLeftToRight);

        decreasing.add(interval);
        increasing.add(interval);
        midpoint = interval.getMidpoint();
        height = 1;
    }

    /**
     * A helper function for the {@link IntervalTree#add(Interval)} method. Adds a new
     * interval to the subtree rooted at a {@code TreeNode}.
     *
     * @param tree     The {@link IntervalTree} containing the subtree. Used primarily for
     *                 housekeeping, such as adjusting the size of the tree, if needed.
     * @param root     The root of the subtree, to which we are adding a new interval.
     * @param interval The {@link Interval} that we are adding.
     * @param <T>      The type of the start and end points of the interval.
     * @return The new root of the subtree. It may be different than the current root,
     * if the subtree had to be rebalanced after the operation.
     */
    public static <T extends Comparable<? super T>, S> TreeNode<T, S> addInterval(IntervalTree<T, S> tree, TreeNode<T, S> root, Interval<T, S> interval) {
        if (root == null) {
            tree.size++;
            return new TreeNode<>(interval);
        }
        if (interval.contains(root.midpoint)) {
            if (root.decreasing.add(interval)) {
                tree.size++;
            }
            root.increasing.add(interval);
            return root;
        } else if (interval.isLeftOf(root.midpoint)) {
            root.left = addInterval(tree, root.left, interval);
            root.height = Math.max(height(root.left), height(root.right)) + 1;
        } else {
            root.right = addInterval(tree, root.right, interval);
            root.height = Math.max(height(root.left), height(root.right)) + 1;
        }

        return root.balanceOut();
    }

    /**
     * Returns the height of the subtree, rooted at the current node.
     *
     * @return The height of the subtree, rooted ad the current node. It will be 1, if
     * the node is a leaf.
     */
    public int height() {
        return height;
    }

    /**
     * Returns the height of a subtree, rooted at a given node. This function accepts
     * {@code null} values and returns 0 as height for them.
     *
     * @param node The node, whose height has to be determined.
     * @return The height of the subtree rooted at {@code node}. Returns 0, if {@code node}
     * is {@code null}.
     */
    private static int height(TreeNode node) {
        return node == null ? 0 : node.height();
    }

    /**
     * Checks if the subtree rooted at the current node is balanced and balances it
     * if necessary.
     *
     * @return The new root of the subtree, after the balancing operation has been
     * performed. It may return a {@code null} value, if the balancing has been
     * triggered by a {@link #removeInterval(IntervalTree, TreeNode, Interval)} operation
     * and the removed interval had been the last one in the subtree.
     */
    private TreeNode<T, S> balanceOut() {
        int balance = height(left) - height(right);
        if (balance < -1) {
            // The tree is right-heavy.
            if (height(right.left) > height(right.right)) {
                this.right = this.right.rightRotate();
                return leftRotate();
            } else {
                return leftRotate();
            }
        } else if (balance > 1) {
            // The tree is left-heavy.
            if (height(left.right) > height(left.left)) {
                this.left = this.left.leftRotate();
                return rightRotate();
            } else {
                return rightRotate();
            }
        } else {
            // The tree is already balanced.
            return this;
        }
    }

    /**
     * Performs a left rotation of the current node, by promoting its right child
     * and demoting the current node. After the left rotation, the promoted node
     * {@link #assimilateOverlappingIntervals(TreeNode) assimilates} the intervals in
     * the demoted node, which intersect its middlepoint.
     *
     * @return The new root of the subtree rooted at the current node, after the
     * rotation has been performed.
     */
    private TreeNode<T, S> leftRotate() {
        TreeNode<T, S> head = right;
        right = head.left;
        head.left = this;
        height = Math.max(height(right), height(left)) + 1;
        head.left = head.assimilateOverlappingIntervals(this);
        return head;
    }

    /**
     * Performs a right rotation of the current node, by promoting its left child
     * and demoting the current node. After the right rotation, the promoted node
     * {@link #assimilateOverlappingIntervals(TreeNode) assimilates} the intervals in
     * the demoted node, which intersect its middlepoint.
     *
     * @return The new root of the subtree rooted at the current node, after the
     * rotation has been performed.
     */
    private TreeNode<T, S> rightRotate() {
        TreeNode<T, S> head = left;
        left = head.right;
        head.right = this;
        height = Math.max(height(right), height(left)) + 1;
        head.right = head.assimilateOverlappingIntervals(this);
        return head;
    }

    /**
     * Transfers all intervals from a target node to the current node, if they
     * intersect the middlepoint of the current node. After this operation, it
     * is possible that the target node remains empty. If so, it needs to be
     * deleted, possible causing the subtree to be rebalanced.
     *
     * @param from The target node, from which intervals will be assimilated.
     * @return The new root of subtree, rooted at the current node.
     */
    private TreeNode<T, S> assimilateOverlappingIntervals(TreeNode<T, S> from) {
        ArrayList<Interval<T, S>> tmp = new ArrayList<>();

        if (midpoint.compareTo(from.midpoint) < 0) {
            for (Interval<T, S> next : from.increasing) {
                if (next.isRightOf(midpoint)) {
                    break;
                }
                tmp.add(next);
            }
        } else {
            for (Interval<T, S> next : from.decreasing) {
                if (next.isLeftOf(midpoint)) {
                    break;
                }
                tmp.add(next);
            }
        }

        from.increasing.removeAll(tmp);
        from.decreasing.removeAll(tmp);
        increasing.addAll(tmp);
        decreasing.addAll(tmp);
        if (from.increasing.size() == 0) {
            return deleteNode(from);
        }
        return from;
    }

    /**
     * A helper function for the {@link IntervalTree#query(Comparable)} method.
     * It searches recursively for all intervals stored in the subtree rooted at
     * the current node, that intersect a target point.
     *
     * @param root  The root of the currently traversed subtree. May be {@code null}.
     * @param point The query point.
     * @param res   The set used to store all intervals to be returned.
     * @param <T,S> The type of the start and end points of the intervals, as well as
     *              the query point.
     * @return The set of all intervals from the current subtree, containing the query.
     */
    public static <T extends Comparable<? super T>, S> Set<Interval<T, S>> query(TreeNode<T, S> root, T point, Set<Interval<T, S>> res) {
        if (root == null) {
            return res;
        }
        if (point.compareTo(root.midpoint) <= 0) {
            for (Interval<T, S> next : root.increasing) {
                if (next.isRightOf(point)) {
                    break;
                }
                res.add(next);
            }
            return TreeNode.query(root.left, point, res);
        } else {
            for (Interval<T, S> next : root.decreasing) {
                if (next.isLeftOf(point)) {
                    break;
                }
                res.add(next);
            }
            return TreeNode.query(root.right, point, res);
        }
    }


    /**
     * A helper function for the {@link IntervalTree#remove(Interval)} method.
     * It searches recursively for the base node of a target interval and
     * removes the interval from the base node, if it is stored there. This is
     * a more efficient way to remove an interval from the tree, since it
     * doesn't iterate through all intervals, but performs a binary search in
     * O(logn).
     *
     * @param tree     The {@link IntervalTree} containing the subtree. Used primarily for
     *                 housekeeping, such as adjusting the size of the tree, if needed.
     * @param root     The root of the currently traversed subtree. May be {@code null}.
     * @param interval The target interval to be removed.
     * @param <T,S>    The type of the start and end points of the intervals, as well as
     *                 the query point.
     * @return The new root of the subtree, rooted at the current node, after the
     * interval has been removed. This could be {@code null} if the interval
     * was the last one stored at the subtree.
     */
    public static <T extends Comparable<? super T>, S> TreeNode<T, S> removeInterval(IntervalTree<T, S> tree, TreeNode<T, S> root, Interval<T, S> interval) {
        if (root == null) {
            return null;
        }
        if (interval.contains(root.midpoint)) {
            if (root.decreasing.remove(interval)) {
                tree.size--;
            }
            root.increasing.remove(interval);
            if (root.increasing.size() == 0) {
                return deleteNode(root);
            }

        } else if (interval.isLeftOf(root.midpoint)) {
            root.left = removeInterval(tree, root.left, interval);
        } else {
            root.right = removeInterval(tree, root.right, interval);
        }
        return root.balanceOut();
    }

    /**
     * Deletes a node from the tree. The caller of this method needs to check, if the
     * node is actually empty, because this method only performs the deletion.
     *
     * @param root  The node that needs to be deleted.
     * @param <T,S> The type of the start and end points of the intervals.
     * @return The new root of the subtree rooted at the node to be deleted. It may
     * be {@code null}, if the deleted node was the last in the subtree.
     */
    private static <T extends Comparable<? super T>, S> TreeNode<T, S> deleteNode(TreeNode<T, S> root) {
        if (root.left == null && root.right == null) {
            return null;
        }

        if (root.left == null) {
            // If the left child is empty, then the right subtree can consist of at most
            // one node, otherwise it would have been unbalanced. So, just return
            // the right child.
            return root.right;
        } else {
            TreeNode<T, S> node = root.left;
            Stack<TreeNode<T, S>> stack = new Stack<>();
            while (node.right != null) {
                stack.push(node);
                node = node.right;
            }
            if (!stack.isEmpty()) {
                stack.peek().right = node.left;
                node.left = root.left;
            }
            node.right = root.right;

            TreeNode<T, S> newRoot = node;
            while (!stack.isEmpty()) {
                node = stack.pop();
                if (!stack.isEmpty()) {
                    stack.peek().right = newRoot.assimilateOverlappingIntervals(node);
                } else {
                    newRoot.left = newRoot.assimilateOverlappingIntervals(node);
                }
            }
            return newRoot.balanceOut();
        }
    }

    /**
     * A helper method for the range search used in the interval intersection query in the tree.
     * This corresponds to the left branch of the range search, once we find a node, whose
     * midpoint is contained in the query interval. All intervals in the left subtree of that node
     * are guaranteed to intersect with the query, if they have an endpoint greater or equal than
     * the start of the query interval. Basically, this means that every time we branch to the left
     * in the binary search, we need to add the whole right subtree to the result set.
     *
     * @param node   The left child of the node, whose midpoint is contained in the query interval.
     * @param query  The query interval.
     * @param result The set which stores all intervals in the tree, intersecting the query.
     */
    static <T extends Comparable<? super T>, S> void rangeQueryLeft(TreeNode<T, S> node, Interval<T, S> query, Set<Interval<T, S>> result) {
        while (node != null) {
            if (query.contains(node.midpoint)) {
                result.addAll(node.increasing);
                if (node.right != null) {
                    for (Interval<T, S> next : node.right) {
                        result.add(next);
                    }
                }
                node = node.left;
            } else {
                for (Interval<T, S> next : node.decreasing) {
                    if (next.isLeftOf(query)) {
                        break;
                    }
                    result.add(next);
                }
                node = node.right;
            }
        }
    }

    /**
     * A helper method for the range search used in the interval intersection query in the tree.
     * This corresponds to the right branch of the range search, once we find a node, whose
     * midpoint is contained in the query interval. All intervals in the right subtree of that node
     * are guaranteed to intersect with the query, if they have an endpoint smaller or equal than
     * the end of the query interval. Basically, this means that every time we branch to the right
     * in the binary search, we need to add the whole left subtree to the result set.
     *
     * @param node   The right child of the node, whose midpoint is contained in the query interval.
     * @param query  The query interval.
     * @param result The set which stores all intervals in the tree, intersecting the query.
     */
    static <T extends Comparable<? super T>, S> void rangeQueryRight(TreeNode<T, S> node, Interval<T, S> query, Set<Interval<T, S>> result) {
        while (node != null) {
            if (query.contains(node.midpoint)) {
                result.addAll(node.increasing);
                if (node.left != null) {
                    for (Interval<T, S> next : node.left) {
                        result.add(next);
                    }
                }
                node = node.right;
            } else {
                for (Interval<T, S> next : node.increasing) {
                    if (next.isRightOf(query)) {
                        break;
                    }
                    result.add(next);
                }
                node = node.left;
            }
        }
    }


    /**
     * An iterator over all intervals stored in subtree rooted at the current node. Traversal
     * is done via classic iterative in-order tree traversal where each iteration is in
     * amortized O(1) time. The iterator requires O(logn) space - at each point of the
     * traversal we keep a stack of the currently traversed branch of the tree.
     */
    @Override
    public TreeNodeIterator iterator() {
        return new TreeNodeIterator();
    }

    class TreeNodeIterator implements Iterator<Interval<T, S>> {

        Stack<TreeNode<T, S>> stack = new Stack<>();
        TreeNode<T, S> subtreeRoot = TreeNode.this;
        TreeNode<T, S> currentNode;
        Interval<T, S> currentInterval;
        Iterator<Interval<T, S>> iterator = Collections.emptyIterator();

        @Override
        public boolean hasNext() {
            return subtreeRoot != null || !stack.isEmpty() || iterator.hasNext();
        }

        @Override
        public Interval<T, S> next() {
            if (!iterator.hasNext()) {
                while (subtreeRoot != null) {
                    stack.push(subtreeRoot);
                    subtreeRoot = subtreeRoot.left;
                }
                if (stack.isEmpty()) {
                    throw new NoSuchElementException();
                }
                currentNode = stack.pop();
                iterator = currentNode.increasing.iterator();
                subtreeRoot = currentNode.right;
            }
            currentInterval = iterator.next();
            return currentInterval;
        }

        @Override
        public void remove() {
            iterator.remove();
        }
    }
}
