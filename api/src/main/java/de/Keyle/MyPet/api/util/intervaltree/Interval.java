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

import java.util.Comparator;

/**
 * A representation of a generic interval. The interval can be open or closed (the start
 * and end points may be inclusive or exclusive), as well as bounded and unbounded (it can
 * extend to positive or negative infinity).
 * <p>
 * The class doesn't assume that the intervals are numeric, instead it is generalized to
 * represent a contiguous subset of elements, where contiguity is defined with respect to
 * an arbitrary total order function. These elements can be for example {@link java.util.Date}s
 * or basically any type, the elements of which can be compared to one another. Since the
 * class requires its generic variable to implement the {@link Comparable} interface, all
 * comparisons in the internals of the {@code Interval} class are done via the interface method.
 * <p>
 * When subclassing the {@code Interval} class, note that the start and end points of the
 * interval <strong>can</strong> be {@code null}. A {@code null} start point represents
 * the negative infinity and a {@code null} end point represents positive infinity. This
 * fact needs to be kept in mind in particular when overwriting methods with default
 * implementations in the {@code Interval} class, such as {@link #contains(Comparable)},
 * {@link #isLeftOf(Comparable)}, and in particular {@link #equals(Object)} and
 * {@link #hashCode()}.
 *
 * @param <T> The type that represents a single point from the domain of definition of the
 *            interval.
 */
@SuppressWarnings("ALL")
public abstract class Interval<T extends Comparable<? super T>, S> {

    private S value;
    private T start, end;
    private boolean isStartInclusive, isEndInclusive;

    /**
     * An enum representing all possible types of bounded intervals.
     */
    public enum Bounded {
        /**
         * An interval, in which both start and end point are exclusive.
         */
        OPEN,

        /**
         * An interval, in which both start and end point are inclusive.
         */
        CLOSED,

        /**
         * An interval, in which the start is exclusive and the end is inclusive.
         */
        CLOSED_RIGHT,

        /**
         * An interval, in which the start is inclusive and the end is exclusive.
         */
        CLOSED_LEFT
    }

    public enum Unbounded {
        /**
         * An interval extending to positive infinity and having an exclusive start
         * point as a lower bound. For example, (5, +inf)
         */
        OPEN_LEFT,

        /**
         * An interval extending to positive infinity and having an inclusive start
         * point as a lower bound. For example, [5, +inf)
         */
        CLOSED_LEFT,

        /**
         * An interval extending to negative infinity and having an exclusive end
         * point as an upper bound. For example, (-inf, 5)
         */
        OPEN_RIGHT,

        /**
         * An interval extending to negative infinity and having an inclusive end
         * point as an upper bound. For example, (-inf, 5]
         */
        CLOSED_RIGHT
    }

    /**
     * Instantiates a new interval representing all points in the domain of definition,
     * i.e. this will instantiate the interval (-inf, +inf).
     *
     * @param value
     */
    public Interval(S value) {
        this.value = value;
        isStartInclusive = true;
        isEndInclusive = true;
    }

    /**
     * Instantiates a new bounded interval.
     *
     * @param start The start point of the interval
     * @param end   The end point of the interval.
     * @param value
     * @param type  Description of whether the interval is open/closed at one or both
     *              of its ends. See {@link Bounded the documentation of the Bounded enum}
     */
    public Interval(T start, T end, S value, Bounded type) {
        this.value = value;
        this.start = start;
        this.end = end;
        if (type == null) {
            type = Bounded.CLOSED;
        }
        switch (type) {
            case OPEN:
                break;
            case CLOSED:
                isStartInclusive = true;
                isEndInclusive = true;
                break;
            case CLOSED_RIGHT:
                isEndInclusive = true;
                break;
            default:
                isStartInclusive = true;
                break;
        }
    }

    /**
     * Instantiates a new unbounded interval - an interval that extends to positive or
     * negative infinity. The interval will be bounded by either the start point
     * or the end point and unbounded in the other point.
     *
     * @param se    The bounding value for either the start or the end point.
     * @param value
     * @param type  Describes, if the interval extends to positive or negative infinity,
     *              as well as if it is open or closed at the bounding point. See {@link Unbounded
     *              the Unbounded enum} for description of the different possibilities.
     */
    public Interval(T se, S value, Unbounded type) {
        this.value = value;
        if (type == null) {
            type = Unbounded.CLOSED_RIGHT;
        }
        switch (type) {
            case OPEN_LEFT:
                start = se;
                isStartInclusive = false;
                isEndInclusive = true;
                break;
            case CLOSED_LEFT:
                start = se;
                isStartInclusive = true;
                isEndInclusive = true;
                break;
            case OPEN_RIGHT:
                end = se;
                isStartInclusive = true;
                isEndInclusive = false;
                break;
            default:
                end = se;
                isStartInclusive = true;
                isEndInclusive = true;
                break;
        }
    }

    /**
     * Checks if the current interval contains no points.
     *
     * <p>In particular, if the end point is less than the start point, then the interval is
     * considered to be empty. There are, however other instances, in which an interval is empty.
     * For example, in the class {@link IntegerInterval}, an open interval, whose start and end
     * points differ by one, for example the interval (4, 5), is empty, because it contains no integers
     * in it. The same interval, however, will <strong>not</strong> be considered empty in the
     * {@link DoubleInterval} class, because there are Double numbers within this interval.
     * </p>
     *
     * @return {@code true}, if the current interval is empty or {@code false} otherwise.
     */
    public boolean isEmpty() {
        if (start == null || end == null) {
            return false;
        }
        int compare = start.compareTo(end);
        if (compare > 0) {
            return true;
        }
        if (compare == 0 && (!isEndInclusive || !isStartInclusive)) {
            return true;
        }
        return false;
    }

    /**
     * Used to create new instances of a specific {@code Interval} subclass.
     * <p>
     * The {@code Interval} class aims to avoid reflexion. On several occasions, however, the class
     * needs to create new instances of the {@code Interval} class. To be able to guarantee that they
     * will have the desired runtime type, the {@link #create(Object)} method of a specific reference object
     * is called.
     * </p>
     * <p>
     * Generally, the only thing you need to do in your implementation of this abstract method is
     * to call the default constructor of your subclass and return the new interval.
     * </p>
     *
     * @param value
     * @return A new instance of the particular {@code Interval} class.
     */
    protected abstract Interval<T, S> create(S value);

    /**
     * Returns the center of the current interval. If the center of the interval exists, but can't
     * be determined, return any point inside the interval. This method will be used only to
     * instantiate the midpoint of a new {@link TreeNode} inside a {@link IntervalTree}, which is why
     * it is not necessary to return exactly the center of the interval, but it will help the
     * {@link IntervalTree} perform slightly better.
     *
     * @return The center point of the current interval, if it exists or {@code null} otherwise. If the
     * center exists but can't be determined correctly, return any point inside the interval.
     */
    public abstract T getMidpoint();

    /**
     * Creates a new instance of the particular {@code Interval} subclass.
     *
     * @param start            The start point of the interval
     * @param isStartInclusive {@code true}, if the start is inclusive or false otherwise
     * @param end              The end point of the interval
     * @param isEndInclusive   {@code true}, if the end is inclusive or false otherwise
     * @param value
     * @return The newly created interval.
     */
    protected Interval<T, S> create(T start, boolean isStartInclusive, T end, boolean isEndInclusive, S value) {
        Interval<T, S> interval = create(value);
        interval.start = start;
        interval.isStartInclusive = isStartInclusive;
        interval.end = end;
        interval.isEndInclusive = isEndInclusive;
        return interval;
    }

    /**
     * Returns the start point of the interval.
     */
    public T getStart() {
        return start;
    }

    /**
     * Returns the end point of the interval.
     */
    public T getEnd() {
        return end;
    }

    /**
     * Returns {@code true}, if the start point is a part of the interval, or false otherwise.
     */
    public boolean isStartInclusive() {
        return isStartInclusive;
    }

    /**
     * Returns {@code true}, if the end point is a part of the interval, or false otherwise.
     */
    public boolean isEndInclusive() {
        return isEndInclusive;
    }

    public S getValue() {
        return value;
    }

    /**
     * Determines if the current interval is a single point.
     *
     * @return {@code true}, if the current interval represents a single point.
     */
    public boolean isPoint() {
        if (start == null || end == null) {
            return false;
        }
        return start.compareTo(end) == 0 && isStartInclusive && isEndInclusive;
    }

    /**
     * Determines if the current interval contains a query point.
     *
     * @param query The point.
     * @return {@code true}, if the current interval contains the {@code query} point or false otherwise.
     */
    public boolean contains(T query) {
        if (isEmpty() || query == null) {
            return false;
        }

        int startCompare = start == null ? 1 : query.compareTo(start);
        int endCompare = end == null ? -1 : query.compareTo(end);
        if (startCompare > 0 && endCompare < 0) {
            return true;
        }
        return (startCompare == 0 && isStartInclusive) || (endCompare == 0 && isEndInclusive);
    }

    /**
     * Returns an interval, representing the intersection of two intervals. More formally, for every
     * point {@code x} in the returned interval, {@code x} will belong in both the current interval
     * and the {@code other} interval.
     *
     * @param other The other interval
     * @return The intersection of the current interval wih the {@code other} interval.
     */
    public Interval<T, S> getIntersection(Interval<T, S> other) {
        if (other == null || isEmpty() || other.isEmpty()) {
            return null;
        }
        // Make sure that the one with the smaller starting point gets intersected with the other.
        // If necessary, swap the intervals
        if ((other.start == null && start != null) || (start != null && start.compareTo(other.start) > 0)) {
            return other.getIntersection(this);
        }
        if (end != null && other.start != null && (end.compareTo(other.start) < 0 || (end.compareTo(other.start) == 0 && (!isEndInclusive || !other.isStartInclusive)))) {
            return null;
        }

        T newStart, newEnd;
        boolean isNewStartInclusive, isNewEndInclusive;

        // If other.start is null, this means my start is also null, because we made sure
        // that the caller object hast the smaller start point => the new start is null
        if (other.start == null) {
            newStart = null;
            isNewStartInclusive = true;
        } else {
            newStart = other.start;
            if (start != null && other.start.compareTo(start) == 0) {
                isNewStartInclusive = other.isStartInclusive && isStartInclusive;
            } else {
                isNewStartInclusive = other.isStartInclusive;
            }
        }

        if (end == null) {
            newEnd = other.end;
            isNewEndInclusive = other.isEndInclusive;
        } else if (other.end == null) {
            newEnd = end;
            isNewEndInclusive = isEndInclusive;
        } else {
            int compare = end.compareTo(other.end);
            if (compare == 0) {
                newEnd = end;
                isNewEndInclusive = isEndInclusive && other.isEndInclusive;
            } else if (compare < 0) {
                newEnd = end;
                isNewEndInclusive = isEndInclusive;
            } else {
                newEnd = other.end;
                isNewEndInclusive = other.isEndInclusive;
            }
        }
        Interval<T, S> intersection = create(newStart, isNewStartInclusive, newEnd, isNewEndInclusive, other.value);
        return intersection.isEmpty() ? null : intersection;
    }

    /**
     * Checks if the current interval contains the entirety of another interval. More formally,
     * this method returns {@code true}, if for every point {@code x} from the interval {@code another}
     * this point {@code x} also belongs to the current interval.
     *
     * @param another Another interval.
     * @return {@code true}, if the interval {@code another} is contained in the current interval in
     * its entirety, or {@code false} otherwise.
     */
    public boolean contains(Interval<T, S> another) {
        if (another == null || isEmpty() || another.isEmpty()) {
            return false;
        }
        Interval<T, S> intersection = getIntersection(another);
        return intersection != null && intersection.equals(another);
    }

    /**
     * Checks if the current interval intersects another interval. More formally, this method
     * returns {@code true} if there is at least one point the current interval, that also
     * belongs to the {@code query} interval.
     *
     * @param query The interval being checked for intersection with the current interval.
     * @return {@code true}, if the two intervals intersect or {@code false} otherwise.
     */
    public boolean intersects(Interval<T, S> query) {
        if (query == null) {
            return false;
        }
        Interval<T, S> intersection = getIntersection(query);
        return intersection != null;
    }

    /**
     * This method checks, if this current interval is entirely to the right of a point. More formally,
     * the method will return {@code true}, if for every point {@code x} from the current interval the inequality
     * {@code x} &gt; {@code point} holds. If the parameter {@code inclusive} is set to {@code false}, this
     * method will return {@code true} also if the start point of the interval is equal to the reference
     * {@code point}.
     *
     * @param point     The reference point
     * @param inclusive {@code false} if the reference {@code point} is allowed to be the start point
     *                  of the current interval.
     * @return {@code true}, if the current interval is entirely to the right of the {@code other}
     * interval, or {@code false} instead.
     */
    public boolean isRightOf(T point, boolean inclusive) {
        if (point == null || start == null) {
            return false;
        }
        int compare = point.compareTo(start);
        if (compare != 0) {
            return compare < 0;
        }
        return !isStartInclusive() || !inclusive;
    }

    /**
     * This method checks, if this current interval is entirely to the right of a point. More formally,
     * the method will return true, if for every point {@code x} from the current interval the inequality
     * {@code x} &gt; {@code point} holds. This formal definition implies in particular that if the start point
     * of the current interval is equal to the reference {@code point} and the end point is open, the method
     * will return {@code true}.
     *
     * @param point The reference point
     * @return {@code true}, if the current interval is entirely to the right of the {@code other}
     * interval, or {@code false} instead.
     */
    public boolean isRightOf(T point) {
        return isRightOf(point, true);
    }

    /**
     * This method checks, if this current interval is entirely to the right of another interval
     * with no common points. More formally, the method will return true, if for every point {@code x}
     * from the current interval and for every point {@code y} from the {@code other} interval the
     * inequality {@code x} &gt; {@code y} holds. This formal definition implies in particular that if the start point
     * of the current interval is equal to the end point of the {@code other} interval, the method
     * will return {@code false} only if both points are inclusive and {@code true} in all other cases.
     *
     * @param other The reference interval
     * @return {@code true}, if the current interval is entirely to the right of the {@code other}
     * interval, or {@code false} instead.
     */
    public boolean isRightOf(Interval<T, S> other) {
        if (other == null || other.isEmpty()) {
            return false;
        }
        return isRightOf(other.end, other.isEndInclusive());
    }

    /**
     * This method checks, if this current interval is entirely to the left of a point. More formally,
     * the method will return {@code true}, if for every point {@code x} from the current interval the inequality
     * {@code x} &lt; {@code point} holds. If the parameter {@code inclusive} is set to {@code false}, this
     * method will return {@code true} also if the end point of the interval is equal to the reference
     * {@code point}.
     *
     * @param point     The reference point
     * @param inclusive {@code false} if the reference {@code point} is allowed to be the end point
     *                  of the current interval.
     * @return {@code true}, if the current interval is entirely to the left of the {@code other}
     * interval, or {@code false} instead.
     */
    public boolean isLeftOf(T point, boolean inclusive) {
        if (point == null || end == null) {
            return false;
        }
        int compare = point.compareTo(end);
        if (compare != 0) {
            return compare > 0;
        }
        return !isEndInclusive() || !inclusive;
    }

    /**
     * This method checks, if this current interval is entirely to the left of a point. More formally,
     * the method will return true, if for every point {@code x} from the current interval the inequality
     * {@code x} &lt; {@code point} holds. This formal definition implies in particular that if the end point
     * of the current interval is equal to the reference {@code point} and the end point is open, the method
     * will return {@code true}.
     *
     * @param point The reference point
     * @return {@code true}, if the current interval is entirely to the left of the {@code other}
     * interval, or {@code false} instead.
     */
    public boolean isLeftOf(T point) {
        return isLeftOf(point, true);
    }

    /**
     * This method checks, if this current interval is entirely to the left of another interval
     * with no common points. More formally, the method will return true, if for every point {@code x}
     * from the current interval and for every point {@code y} from the {@code other} interval the
     * inequality {@code x} &lt; {@code y} holds. This formal definition implies in particular that if the end point
     * of the current interval is equal to the start point of the {@code other} interval, the method
     * will return {@code false} only if both points are inclusive and {@code true} in all other cases.
     *
     * @param other The reference interval
     * @return {@code true}, if the current interval is entirely to the left of the {@code other}
     * interval, or {@code false} instead.
     */
    public boolean isLeftOf(Interval<T, S> other) {
        if (other == null || other.isEmpty()) {
            return false;
        }
        return isLeftOf(other.start, other.isStartInclusive());
    }

    /**
     * A {@link Comparator} that only considers the start points of the intervals. It can not and must
     * not be used as a standalone {@link Comparator}. It only serves to create a more readable and
     * modular code.
     */
    private int compareStarts(Interval<T, S> other) {
        if (start == null && other.start == null) {
            return 0;
        }
        if (start == null) {
            return -1;
        }
        if (other.start == null) {
            return 1;
        }
        int compare = start.compareTo(other.start);
        if (compare != 0) {
            return compare;
        }
        if (isStartInclusive ^ other.isStartInclusive) {
            return isStartInclusive ? -1 : 1;
        }
        return 0;
    }

    /**
     * A {@link Comparator} that only considers the end points of the intervals. It can not and must
     * not be used as a standalone {@link Comparator}. It only serves to create a more readable and
     * modular code.
     */
    private int compareEnds(Interval<T, S> other) {
        if (end == null && other.end == null) {
            return 0;
        }
        if (end == null) {
            return 1;
        }
        if (other.end == null) {
            return -1;
        }
        int compare = end.compareTo(other.end);
        if (compare != 0) {
            return compare;
        }
        if (isEndInclusive ^ other.isEndInclusive) {
            return isEndInclusive ? 1 : -1;
        }
        return 0;
    }

    /**
     * A comparator that can be used as a parameter for sorting functions. The start comparator sorts the intervals
     * in <em>ascending</em> order by placing the intervals with a smaller start point before intervals with greater
     * start points. This corresponds to a line sweep from left to right.
     * <p>
     * Intervals with start point null (negative infinity) are considered smaller than all other intervals.
     * If two intervals have the same start point, the closed start point is considered smaller than the open one.
     * For example, [0, 2) is considered smaller than (0, 2).
     * </p>
     * <p>
     * To ensure that this comparator can also be used in sets it considers the end points of the intervals, if the
     * start points are the same. Otherwise the set will not be able to handle two different intervals, sharing
     * the same starting point, and omit one of the intervals.
     * </p>
     * <p>
     * Since this is a static method of a generic class, it involves unchecked calls to class methods. It is left to
     * ths user to ensure that she compares intervals from the same class, otherwise an exception might be thrown.
     * </p>
     */
    public static Comparator<Interval> sweepLeftToRight = (a, b) -> {
        int compare = a.compareStarts(b);
        if (compare != 0) {
            return compare;
        }
        compare = a.compareEnds(b);
        if (compare != 0) {
            return compare;
        }
        return a.compareSpecialization(b);
    };

    /**
     * A comparator that can be used as a parameter for sorting functions. The end comparator sorts the intervals
     * in <em>descending</em> order by placing the intervals with a greater end point before intervals with smaller
     * end points. This corresponds to a line sweep from right to left.
     * <p>
     * Intervals with end point null (positive infinity) are placed before all other intervals. If two intervals
     * have the same end point, the closed end point is placed before the open one. For example,  [0, 10) is placed
     * after (0, 10].
     * </p>
     * <p>
     * To ensure that this comparator can also be used in sets it considers the start points of the intervals, if the
     * end points are the same. Otherwise the set will not be able to handle two different intervals, sharing
     * the same end point, and omit one of the intervals.
     * </p>
     * <p>
     * Since this is a static method of a generic class, it involves unchecked calls to class methods. It is left to
     * ths user to ensure that she compares intervals from the same class, otherwise an exception might be thrown.
     * </p>
     */
    public static Comparator<Interval> sweepRightToLeft = (a, b) -> {
        int compare = b.compareEnds(a);
        if (compare != 0) {
            return compare;
        }
        compare = b.compareStarts(a);
        if (compare != 0) {
            return compare;
        }
        return a.compareSpecialization(b);
    };

    /**
     * A method that should be overwritten by subclasses of {@code Interval}, if they have properties
     * that characterize the objects of the class and are used to identify them. It is used to create
     * a total order between distinct objects, that would otherwise be considered equal, if only
     * the start and end points were considered. If you don't have any such special properties, you
     * may leave the default implementation of this method.
     * <p>
     * This method functions as a traditional {@link Comparator}, bit can not and should not be used
     * on its own, nor should it be implemented as a full standalone comparator. Instead, it is always
     * used in conjunction with one of the two base {@link Comparator}s in the {@code Interval} class -
     * {@link #sweepLeftToRight} and {@link #sweepRightToLeft}. This method will only be executed if
     * the main comparator returns 0, i.e. if it considers the intervals to be equal. At that moment,
     * the start and end points would already have been compared to one another, which is why this method
     * should <strong>disregard the start and end points</strong> completely and only consider the
     * special properties defined in the particular subclass.
     * </p>
     * <p>
     * It is vital to overwrite this method, if you have any properties in your subclass, that identify
     * the interval, such as for example user IDs, student IDs or room numbers. The two base comparators
     * are used within the underlying {@link java.util.TreeSet}s, which may discard two distinct interval
     * objects, that have the same start and end points.
     * </p>
     *
     * @param other The object that is being compared to this interval
     * @return <ul>
     * <li>-1, if this object is less than the {@code other},</li>
     * <li>0, if the two objects are equal,</li>
     * <li>1, if this object is greater than the {@code other}.</li>
     * </ul>
     */
    protected int compareSpecialization(Interval<T, S> other) {
        return 0;
    }

    @Override
    public int hashCode() {
        int prime = 31;
        int result = start == null ? 0 : start.hashCode();
        result = prime * result + (end == null ? 0 : end.hashCode());
        result = prime * result + (isStartInclusive ? 1 : 0);
        result = prime * result + (isEndInclusive ? 1 : 0);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Interval)) {
            return false;
        }
        Interval<T, S> other = (Interval<T, S>) obj;
        if (start == null ^ other.start == null) {
            return false;
        }
        if (value == null ^ other.value == null) {
            return false;
        }
        if (!value.equals(other.value)) {
            return false;
        }
        if (end == null ^ other.end == null) {
            return false;
        }
        if (isEndInclusive ^ other.isEndInclusive) {
            return false;
        }
        if (isStartInclusive ^ other.isStartInclusive) {
            return false;
        }
        if (start != null && !start.equals(other.start)) {
            return false;
        }
        if (end != null && !end.equals(other.end)) {
            return false;
        }
        return true;
    }


    public Builder builder() {
        return new Builder(this, value);
    }

    /**
     * Used to create new intervals in an intuitive fashion by using the builder pattern.
     * Since the implementation of the {@code Interval} class strives to avoid reflexion,
     * despite being generic class, the {@code Builder} inner class is not static. Instead
     * it is always tied to a particular object, so that it can build new objects from this
     * specific runtime type.
     */
    public class Builder {

        private Interval<T, S> interval;

        /**
         * {@code private} constructor, used only in the internals of the {@link Interval}
         * class. You can create new instances of the class by using either an existing
         * object:
         * <pre>existingInterval.builder()</pre>
         * or by instantiating an "everything" interval with the default constructor and
         * calling its {@link Interval#builder(Object) builder} method:
         * <pre>new IntegerInterval().builder()</pre>
         *
         * @param ref   A reference object used only to determine the runtime type of the
         *              new object. The reference interval doesn't influence the start and
         *              end points of the new interval in any way.
         * @param value
         */
        private Builder(Interval<T, S> ref, S value) {
            interval = ref.create(value);
        }

        /**
         * Sets the start point of the currently building interval to the given value.
         * The interval will be open to the left. If this method is called more than
         * once or in conjunction with the {@link #greaterEqual(Comparable)} method, only
         * the last call in the subsequence will take effect.
         *
         * @param start The value for the start point of the new interval.
         */
        public Builder greater(T start) {
            interval.start = start;
            interval.isStartInclusive = false;
            return this;
        }

        /**
         * Sets the start point of the currently building interval to the given value.
         * The interval will be closed to the left. If this method is called more than
         * once or in conjunction with the {@link #greater(Comparable)} method, only
         * the last call in the subsequence will take effect.
         *
         * @param start The value for the start point of the new interval.
         */
        public Builder greaterEqual(T start) {
            interval.start = start;
            interval.isStartInclusive = true;
            return this;
        }

        /**
         * Sets the end point of the currently building interval to the given value.
         * The interval will be open to the right. If this method is called more than
         * once or in conjunction with the {@link #lessEqual(Comparable)} method, only
         * the last call in the subsequence will take effect.
         *
         * @param end The value for the end point of the new interval.
         */
        public Builder less(T end) {
            interval.end = end;
            interval.isEndInclusive = false;
            return this;
        }

        /**
         * Sets the end point of the currently building interval to the given value.
         * The interval will be closed to the right. If this method is called more than
         * once or in conjunction with the {@link #lessEqual(Comparable)} method, only
         * the last call in the subsequence will take effect.
         *
         * @param end The value for the end point of the new interval.
         */
        public Builder lessEqual(T end) {
            interval.end = end;
            interval.isEndInclusive = true;
            return this;
        }

        /**
         * Builds the new interval
         *
         * @return The newly created interval.
         */
        public Interval<T, S> build() {
            return interval;
        }
    }
}
