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

package de.Keyle.MyPet.util.itemstringinterpreter;

import net.minecraft.server.v1_6_R2.NBTBase;
import net.minecraft.server.v1_6_R2.NBTTagList;

import java.util.ArrayList;

/**
 * This class will be removed with Minecraft 1.7
 */
public class NBTTagListHolder extends NBTHolder
{
    protected ArrayList<NBTHolder> holderList = new ArrayList<NBTHolder>();

    public NBTTagListHolder(String paramString)
    {
        this.name = paramString;
    }

    public NBTBase getNBT()
    {
        NBTTagList tagList = new NBTTagList(this.name);

        for (NBTHolder localcs : this.holderList)
        {
            tagList.add(localcs.getNBT());
        }

        return tagList;
    }
}