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

package de.Keyle.MyPet.gui.skilltreecreator.skills;

import org.spout.nbt.ByteTag;
import org.spout.nbt.CompoundTag;
import org.spout.nbt.IntTag;

import javax.swing.*;

public class Inventory implements SkillPropertiesPanel
{
    private JTextField rowsInput;
    private JPanel mainPanel;
    private JCheckBox dropContentCheckBox;

    private CompoundTag compoundTag;

    public Inventory(CompoundTag compoundTag)
    {
        this.compoundTag = compoundTag;
        load(compoundTag);
    }

    @Override
    public JPanel getMainPanel()
    {
        return mainPanel;
    }

    @Override
    public void verifyInput()
    {
        rowsInput.setText(rowsInput.getText().replaceAll("[^0-6]*", ""));
        if (rowsInput.getText().length() > 1)
        {
            rowsInput.setText(rowsInput.getText().substring(0, 1));
        }
        if (rowsInput.getText().length() == 0)
        {
            rowsInput.setText("0");
        }
    }

    @Override
    public CompoundTag save()
    {
        compoundTag.getValue().put("add", new IntTag("add", Integer.parseInt(rowsInput.getText())));
        compoundTag.getValue().put("drop", new ByteTag("drop", dropContentCheckBox.isSelected()));

        return compoundTag;
    }

    @Override
    public void load(CompoundTag compoundTag)
    {
        if (compoundTag.getValue().containsKey("add"))
        {
            rowsInput.setText("" + ((IntTag) compoundTag.getValue().get("add")).getValue());
        }
        if (compoundTag.getValue().containsKey("drop"))
        {
            dropContentCheckBox.setSelected(((ByteTag) compoundTag.getValue().get("drop")).getBooleanValue());
        }
    }
}