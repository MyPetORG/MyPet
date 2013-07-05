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

import org.spout.nbt.CompoundTag;
import org.spout.nbt.IntTag;
import org.spout.nbt.StringTag;

import javax.swing.*;

public class Slow implements SkillPropertiesPanel
{
    private JPanel mainPanel;
    private JTextField chanceInput;
    private JRadioButton addChanceRadioButton;
    private JRadioButton setChanceRadioButton;
    private JTextField timeInput;
    private JRadioButton addTimeRadioButton;
    private JRadioButton setTimeRadioButton;

    private CompoundTag compoundTag;

    public Slow(CompoundTag compoundTag)
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
        chanceInput.setText(chanceInput.getText().replaceAll("[^0-9]*", ""));
        if (chanceInput.getText().length() == 0)
        {
            chanceInput.setText("0");
        }

        timeInput.setText(timeInput.getText().replaceAll("[^0-9]*", ""));
        if (timeInput.getText().length() == 0)
        {
            timeInput.setText("0");
        }
    }

    @Override
    public CompoundTag save()
    {
        compoundTag.getValue().put("addset_chance", new StringTag("addset_chance", addChanceRadioButton.isSelected() ? "add" : "set"));
        compoundTag.getValue().put("chance", new IntTag("chance", Integer.parseInt(chanceInput.getText())));

        compoundTag.getValue().put("addset_duration", new StringTag("addset_duration", addTimeRadioButton.isSelected() ? "add" : "set"));
        compoundTag.getValue().put("duration", new IntTag("duration", Integer.parseInt(timeInput.getText())));

        return compoundTag;
    }

    @Override
    public void load(CompoundTag compoundTag)
    {
        if (!compoundTag.getValue().containsKey("addset_chance") || ((StringTag) compoundTag.getValue().get("addset_chance")).getValue().equals("add"))
        {
            addChanceRadioButton.setSelected(true);
        }
        else
        {
            setChanceRadioButton.setSelected(true);
        }
        if (compoundTag.getValue().containsKey("chance"))
        {
            chanceInput.setText("" + ((IntTag) compoundTag.getValue().get("chance")).getValue());
        }

        if (!compoundTag.getValue().containsKey("addset_duration") || ((StringTag) compoundTag.getValue().get("addset_duration")).getValue().equals("add"))
        {
            addTimeRadioButton.setSelected(true);
        }
        else
        {
            setTimeRadioButton.setSelected(true);
        }
        if (compoundTag.getValue().containsKey("duration"))
        {
            timeInput.setText("" + ((IntTag) compoundTag.getValue().get("duration")).getValue());
        }
    }
}