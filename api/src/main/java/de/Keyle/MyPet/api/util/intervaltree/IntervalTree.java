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
 * An implementation of a Centered Interval Tree for efficient search in a set of intervals. See
 * <a href="https://en.wikipedia.org/wiki/Interval_tree">https://en.wikipedia.org/wiki/Interval_tree</a>.
 *
 * <p>
 * The tree functions as a set, meaning that it will not store an interval more than
 * once. More formally, for any two distinct intervals x and y within the tree, it is
 * guaranteed that x.equals(y) will evaluate to false. If you try to add an interval to the tree,
 * which is already in it, the tree will reject it. See the documentation of
 * {@link #add(Interval) the add method} for more information. The tree will also <strong>not</strong> accept
 * {@code null} or empty intervals, meaning intervals whose {@link Interval#isEmpty()}
 * method evaluates to {@code true}.
 * </p>
 * <p>
 * The {@link #iterator()} method of the tree returns a fail-fast iterator, which will
 * throw a {@link ConcurrentModificationException}, if the tree is modified in any form
 * during the iteration, other than by using the iterator's own {@code remove} method. However,
 * this is done in a best-effort manner, since it is generally hard to guarantee this behaviour
 * while using non-atomic and not synchronized methods.
 * </p>
 * <p>
 * The tree relies on the usage of a subclass of the {@link Interval} class to represent the
 * intervals. The majority of the interval methods are already implemented within the
 * {@code Interval} class and don't have to be implemented by the extending class. Comparisons
 * between the intervals are also pre-implemented in the {@code Interval} class and use the
 * start and endpoints to create a total order of all stored intervals. However, if the tree
 * needs to store intervals that have the same start and end points but represent different
 * logical entities, you need a subclass that overwrites the {@code equals}, {@code hashCode}
 * and {@code compareTo} methods. See the documentation of {@link Interval} for more information.
 * </p>
 *
 * @param <T> The type for the start and end point of the interval
 */
public class IntervalTree<T extends Comparable<? super T>, S> extends AbstractSet<Interval<T, S>> {

    /**
     * The root of the current interval tree. It is {@code null} initially, when the tree is
     * empty and may change as the result of adding or removing intervals to the tree.
     */
    TreeNode<T, S> root;

    /**
     * The size of the interval tree, or the amount of intervals stored in it.
     */
    int size;

    /**
     * Adds an interval to the tree. If the interval is empty, it is rejected and not
     * stored in the tree. This operation may cause a rebalancing of the tree, which
     * in turn may cause intervals to be {@link TreeNode#assimilateOverlappingIntervals(TreeNode) assimilated}.
     * This is why this operation may run in {@code O(n)} worst-case time, even though
     * on average it should run in {@code O(logn)} due to the nature binary trees.
     *
     * @param interval The interval to be added to the tree.
     * @return {@code true}, if the tree has been modified as a result of the operation,
     * or {@code false} otherwise.
     */
    @Override
    public boolean add(Interval<T, S> interval) {
        if (interval.isEmpty()) {
            return false;
        }
        int sizeBeforeOperation = size;
        root = TreeNode.addInterval(this, root, interval);
        return size == sizeBeforeOperation;
    }

    /**
     * Searches for and returns all intervals stored in the tree, that contain a given
     * query point. This operation is guaranteed to run in {@code O(logn + k)}, where
     * {@code n} is the size of the tree and {@code k} is the size of the returned set,
     * provided that the time complexity of iterating over the intervals stored in each
     * visited node is amortized {@code O(1)}. This assumption is met for the current
     * implementation of {@link TreeNode}, where {@link TreeSet}s are used.
     *
     * @param point The query point.
     * @return A set containing all intervals from the tree, intersecting the query point.
     */
    public Set<Interval<T, S>> query(T point) {
        return TreeNode.query(root, point, new HashSet<>());
    }

    /**
     * Searches for and returns all intervals stored in the tree, that intersect a given
     * query interval. This operation is guaranteed to run in {@code O(logn + k)}, where
     * {@code n} is the size of the tree and {@code k} is the size of the returned set,
     * provided that the time complexity of iterating over the intervals stored in each
     * visited node is amortized {@code O(1)}. This assumption is met for the current
     * implementation of {@link TreeNode}, where {@link TreeSet}s are used.
     *
     * @param interval The query interval.
     * @return A set containing all intervals from the tree, intersecting the query interval.
     */
    public Set<Interval<T, S>> query(Interval<T, S> interval) {
        Set<Interval<T, S>> result = new HashSet<>();

        if (root == null || interval.isEmpty()) {
            return result;
        }
        TreeNode<T, S> node = root;
        while (node != null) {
            if (interval.contains(node.midpoint)) {
                result.addAll(node.increasing);
                TreeNode.rangeQueryLeft(node.left, interval, result);
                TreeNode.rangeQueryRight(node.right, interval, result);
                break;
            }
            if (interval.isLeftOf(node.midpoint)) {
                for (Interval<T, S> next : node.increasing) {
                    if (!interval.intersects(next)) {
                        break;
                    }
                    result.add(next);
                }
                node = node.left;
            } else {
                for (Interval<T, S> next : node.decreasing) {
                    if (!interval.intersects(next)) {
                        break;
                    }
                    result.add(next);
                }
                node = node.right;
            }
        }
        return result;
    }

    /**
     * Removes an interval from the tree, if it was stored in it. This operation may cause the
     * {@link TreeNode#deleteNode(TreeNode) deletion of a node}, which in turn may cause
     * rebalancing of the tree and the {@link TreeNode#assimilateOverlappingIntervals(TreeNode) assimilation}
     * of intervals from one node to another. This is why this operation may run in {@code O(n)}
     * worst-case time, even though on average it should run in {@code O(logn)} due to the
     * nature binary trees.
     *
     * @param interval
     * @return
     */
    public boolean remove(Interval<T, S> interval) {
        if (interval.isEmpty() || root == null) {
            return false;
        }
        int sizeBeforeOperation = size;
        root = TreeNode.removeInterval(this, root, interval);
        return size == sizeBeforeOperation;
    }


    // =========================================================================
    // ============== Iterator over the Intervals in the tree ==================
    // =========================================================================

    @Override
    public Iterator<Interval<T, S>> iterator() {
        if (root == null) {
            return Collections.emptyIterator();
        } else {
            final TreeNode.TreeNodeIterator it = root.iterator();
            return new Iterator<Interval<T, S>>() {
                @Override
                public void remove() {
                    if (it.currentNode.increasing.size() == 1) {
                        root = TreeNode.removeInterval(IntervalTree.this, root, it.currentInterval);

                        // Rebuild the whole branch stack in the iterator, because we might have
                        // moved nodes around and introduced new nodes into the branch. The rule
                        // is, add all nodes to the branch stack, to which the current node is
                        // a left descendant.
                        TreeNode<T, S> node = root;
                        it.stack = new Stack<>();

                        // Continue pushing elements according to the aforementioned rule until
                        // you reach the subtreeRoot - this is the root of the subtree, which
                        // the iterator has marked for traversal next. This subtree must not
                        // become a part of the branch stack, or otherwise you will iterate over
                        // some intervals twice.
                        while (node != it.subtreeRoot) {
                            if (it.currentNode.midpoint.compareTo(node.midpoint) < 0) {
                                it.stack.push(node);
                                node = node.left;
                            } else {
                                node = node.right;
                            }
                        }
                    } else {
                        it.remove();
                    }
                }

                @Override
                public boolean hasNext() {
                    return it.hasNext();
                }

                @Override
                public Interval<T, S> next() {
                    return it.next();
                }
            };
        }
    }


    // =========================================================================
    // ================== Methods from the Set interface =======================
    // =========================================================================

    /**
     * Returns the size of the tree.
     *
     * @return The amount of intervals, stored in the tree.
     */
    public int size() {
        return size;
    }

    /**
     * Removes all intervals from the tree. This is an {@code O(1)} worst-case
     * time operation.
     */
    @Override
    public void clear() {
        size = 0;
        root = null;
    }

    /**
     * Checks if a given object is stored in the tree. This method uses binary
     * search instead of iteration over all intervals, which is why it runs in
     * guaranteed {@code O(logn)} worst-case time.
     *
     * @param o The query object.
     * @return {@code true}, if the object is stored in the tree, or {@code false}
     * otherwise.
     */
    @Override
    public boolean contains(Object o) {
        if (root == null || o == null) {
            return false;
        }
        if (!(o instanceof Interval)) {
            return false;
        }
        Interval<T, S> query;
        query = (Interval<T, S>) o;
        TreeNode<T, S> node = root;
        while (node != null) {
            if (query.contains(node.midpoint)) {
                return node.increasing.contains(query);
            }
            if (query.isLeftOf(node.midpoint)) {
                node = node.left;
            } else {
                node = node.right;
            }
        }

        return false;
    }
}