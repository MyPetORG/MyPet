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

/**
 * A class representing an interval with Doubles as start and end points.
 * <p>
 * This class doesn't differentiate between -0.0 and 0.0 and treats them
 * as the same value. In particular, the difference between these two
 * Doubles is disregarded in the equals method.
 * </p>
 * <p>
 * If an intervals has a Double.NaN as a start or an end point, the interval
 * is considered to be empty.
 * </p>
 */
public class DoubleInterval<S> extends Interval<Double, S> {

    private static final int OFFSET = 1_000;

    /**
     * Instantiates an interval extending from positive infinity to negative
     * infinity and thus containing all Doubles.
     */
    public DoubleInterval(S value) {
        super(value);
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
    public DoubleInterval(Double start, Double end, S value, Bounded type) {
        super(0.0 + start, 0.0 + end, value, type);
    }

    /**
     * Instantiates a new unbounded interval - an interval that extends to positive or
     * negative infinity. The interval will be bounded by either the start point
     * or the end point and unbounded in the other point.
     *
     * @param se    The bounding value for either the start or the end point.
     * @param value The bounding value for either the start or the end point.
     * @param type  Describes whether the interval extends to positive or negative infinity,
     *              as well as if it is open or closed at the bounding point. See {@link Unbounded
     *              the Unbounded enum} for description of the different possibilities.
     */
    public DoubleInterval(Double se, S value, Unbounded type) {
        super(0.0 + se, value, type);
    }

    @Override
    protected Interval<Double, S> create(S value) {
        return new DoubleInterval<>(value);
    }

    /**
     * Determines if the interval is empty, meaning it contains no Doubles. Unlike the class
     * {@link IntegerInterval}, the interval (4.0, 5.0), for example, will <strong>not</strong>
     * be considered empty, because there are Double values in this interval.
     * <p>
     * If the start or end point of the interval is Double.NaN, the interval will be
     * considered empty.
     * </p>
     *
     * @return
     */
    @Override
    public boolean isEmpty() {
        if (getStart() != null && getStart().isNaN()) {
            return true;
        }
        if (getEnd() != null && getEnd().isNaN()) {
            return true;
        }
        if (getStart() != null && getEnd() != null) {
            if (getStart() == Double.POSITIVE_INFINITY && getEnd() == Double.POSITIVE_INFINITY) {
                return true;
            }
            if (getStart() == Double.NEGATIVE_INFINITY && getEnd() == Double.NEGATIVE_INFINITY) {
                return true;
            }
        }
        return super.isEmpty();
    }

    /**
     * Determines the center of the interval.
     * <p>
     * This is done in a best-effort basis, because in unbounded intervals the center doesn't exist.
     * This is different from the class {@link IntegerInterval}, where even unbounded intervals
     * are bounded by Integer.MAX_VALUE and/or Integer.MIN_VALUE. In case the DoubleInterval is
     * unbounded, the method returns an arbitrary Double, which is within the interval, but makes
     * no guarantees as to what value it will have exactly. This ensures that unbounded intervals
     * will still work properly with {@link TreeNode}s. The current implementation returns a value,
     * which is at a particular {@link #OFFSET} to the bounded point of the interval. However, this
     * behaviour may change in the future, so don't rely on it.
     * </p>
     *
     * @return The center of the interval, or an arbitrary point within the interval, if it is
     * unbounded.
     */
    @Override
    public Double getMidpoint() {
        if (isEmpty()) {
            return null;
        }

        // Handle null values
        if (getStart() == null && getEnd() == null) {
            return 0.0;
        }
        if (getStart() == null) {
            return getEnd() - OFFSET;
        }
        if (getEnd() == null) {
            return getStart() + OFFSET;
        }

        // Now we are sure there are no more null values involved
        if (getStart() == Double.NEGATIVE_INFINITY && getEnd() == Double.POSITIVE_INFINITY) {
            return 0.0;
        }
        if (getStart() == Double.NEGATIVE_INFINITY) {
            return getEnd() - OFFSET;
        }
        if (getEnd() == Double.POSITIVE_INFINITY) {
            return getStart() + OFFSET;
        }
        return getStart() + (getEnd() - getStart()) / 2;
    }
}
