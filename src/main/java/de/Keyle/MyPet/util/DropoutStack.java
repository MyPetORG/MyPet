/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2013 Keyle
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

package de.Keyle.MyPet.util;

import java.util.Stack;

public class DropoutStack<E> extends Stack<E>
{
    int dropout = 5;

    public DropoutStack()
    {
        super();
    }

    public DropoutStack(int i)
    {
        super();
        if (i > 0)
        {
            dropout = i;
        }
    }

    public int getDropout()
    {
        return dropout;
    }

    public E push(E element)
    {
        if (size() >= dropout)
        {
            removeEnd();
        }
        this.add(0, element);
        return element;
    }

    private E removeEnd()
    {
        E removedElement = this.lastElement();
        this.remove(removedElement);
        return removedElement;
    }
}