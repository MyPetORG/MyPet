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

package de.Keyle.MyPet.compat.v1_13_R2.util;

import de.Keyle.MyPet.api.compat.Compat;
import de.Keyle.MyPet.api.util.ReflectionUtil;
import net.minecraft.server.v1_13_R2.AxisAlignedBB;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class FieldCompat {

    public static Compat<Boolean> AxisAlignedBB_Fields = new Compat<Boolean>()
            .v("1.13.1", true)
            .v("1.13.2", false)
            .search();

    public static Compat<Field> AxisAlignedBB_minX = new Compat<Field>()
            .v("1.13.1", () -> ReflectionUtil.getField(AxisAlignedBB.class, "a"))
            .v("1.13.2", () -> ReflectionUtil.getField(AxisAlignedBB.class, "minX"))
            .search();

    public static Compat<Field> AxisAlignedBB_maxX = new Compat<Field>()
            .v("1.13.1", () -> ReflectionUtil.getField(AxisAlignedBB.class, "d"))
            .v("1.13.2", () -> ReflectionUtil.getField(AxisAlignedBB.class, "maxX"))
            .search();

    public static Compat<Field> AxisAlignedBB_minY = new Compat<Field>()
            .v("1.13.1", () -> ReflectionUtil.getField(AxisAlignedBB.class, "b"))
            .v("1.13.2", () -> ReflectionUtil.getField(AxisAlignedBB.class, "minY"))
            .search();

    public static Compat<Field> AxisAlignedBB_maxY = new Compat<Field>()
            .v("1.13.1", () -> ReflectionUtil.getField(AxisAlignedBB.class, "e"))
            .v("1.13.2", () -> ReflectionUtil.getField(AxisAlignedBB.class, "maxY"))
            .search();

    public static Compat<Field> AxisAlignedBB_minZ = new Compat<Field>()
            .v("1.13.1", () -> ReflectionUtil.getField(AxisAlignedBB.class, "c"))
            .v("1.13.2", () -> ReflectionUtil.getField(AxisAlignedBB.class, "minZ"))
            .search();

    public static Compat<Field> AxisAlignedBB_maxZ = new Compat<Field>()
            .v("1.13.1", () -> ReflectionUtil.getField(AxisAlignedBB.class, "f"))
            .v("1.13.2", () -> ReflectionUtil.getField(AxisAlignedBB.class, "maxZ"))
            .search();

    public static Compat<Method> IBlockData_getCollisionShape = new Compat<Method>()
            .v("1.13.1", () -> ReflectionUtil.getMethod(AxisAlignedBB.class, "h"))
            .v("1.13.2", () -> ReflectionUtil.getMethod(AxisAlignedBB.class, "getCollisionShape"))
            .search();

    public static Compat<Method> VoxelShape_isEmpty = new Compat<Method>()
            .v("1.13.1", () -> ReflectionUtil.getMethod(AxisAlignedBB.class, "b"))
            .v("1.13.2", () -> ReflectionUtil.getMethod(AxisAlignedBB.class, "isEmpty"))
            .search();
}
