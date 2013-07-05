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

import javax.swing.*;

public class Behavior implements SkillPropertiesPanel
{
    private JCheckBox duelCheckBox;
    private JCheckBox raidCheckBox;
    private JCheckBox farmCheckBox;
    private JCheckBox aggressiveCheckBox;
    private JCheckBox friendlyCheckBox;
    private JPanel mainPanel;

    private CompoundTag compoundTag;

    public Behavior(CompoundTag compoundTag)
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
    }

    @Override
    public CompoundTag save()
    {
        compoundTag.getValue().put("friend", new ByteTag("friend", friendlyCheckBox.isSelected()));
        compoundTag.getValue().put("aggro", new ByteTag("aggro", aggressiveCheckBox.isSelected()));
        compoundTag.getValue().put("farm", new ByteTag("farm", farmCheckBox.isSelected()));
        compoundTag.getValue().put("raid", new ByteTag("raid", raidCheckBox.isSelected()));
        compoundTag.getValue().put("duel", new ByteTag("duel", duelCheckBox.isSelected()));

        return compoundTag;
    }

    @Override
    public void load(CompoundTag compoundTag)
    {
        if (compoundTag.getValue().containsKey("friend"))
        {
            friendlyCheckBox.setSelected(((ByteTag) compoundTag.getValue().get("friend")).getBooleanValue());
        }
        if (compoundTag.getValue().containsKey("aggro"))
        {
            aggressiveCheckBox.setSelected(((ByteTag) compoundTag.getValue().get("aggro")).getBooleanValue());
        }
        if (compoundTag.getValue().containsKey("farm"))
        {
            farmCheckBox.setSelected(((ByteTag) compoundTag.getValue().get("farm")).getBooleanValue());
        }
        if (compoundTag.getValue().containsKey("raid"))
        {
            raidCheckBox.setSelected(((ByteTag) compoundTag.getValue().get("raid")).getBooleanValue());
        }
        if (compoundTag.getValue().containsKey("duel"))
        {
            duelCheckBox.setSelected(((ByteTag) compoundTag.getValue().get("duel")).getBooleanValue());
        }
    }
}