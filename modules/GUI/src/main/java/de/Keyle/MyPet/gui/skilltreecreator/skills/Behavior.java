/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2017 Keyle
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

import de.keyle.knbt.TagByte;
import de.keyle.knbt.TagCompound;

import javax.swing.*;

public class Behavior implements SkillPropertiesPanel {
    private JCheckBox duelCheckBox;
    private JCheckBox raidCheckBox;
    private JCheckBox farmCheckBox;
    private JCheckBox aggressiveCheckBox;
    private JCheckBox friendlyCheckBox;
    private JPanel mainPanel;

    @Override
    public JPanel getMainPanel() {
        return mainPanel;
    }

    @Override
    public void verifyInput() {
    }

    @Override
    public void resetInput() {
        friendlyCheckBox.setSelected(false);
        aggressiveCheckBox.setSelected(false);
        farmCheckBox.setSelected(false);
        raidCheckBox.setSelected(false);
        duelCheckBox.setSelected(false);
    }

    @Override
    public void save(TagCompound tagCompound) {
        tagCompound.getCompoundData().put("friend", new TagByte(friendlyCheckBox.isSelected()));
        tagCompound.getCompoundData().put("aggro", new TagByte(aggressiveCheckBox.isSelected()));
        tagCompound.getCompoundData().put("farm", new TagByte(farmCheckBox.isSelected()));
        tagCompound.getCompoundData().put("raid", new TagByte(raidCheckBox.isSelected()));
        tagCompound.getCompoundData().put("duel", new TagByte(duelCheckBox.isSelected()));
    }

    @Override
    public void load(TagCompound TagCompound) {
        if (TagCompound.getCompoundData().containsKey("friend")) {
            friendlyCheckBox.setSelected(TagCompound.getAs("friend", TagByte.class).getBooleanData());
        }
        if (TagCompound.getCompoundData().containsKey("aggro")) {
            aggressiveCheckBox.setSelected(TagCompound.getAs("aggro", TagByte.class).getBooleanData());
        }
        if (TagCompound.getCompoundData().containsKey("farm")) {
            farmCheckBox.setSelected(TagCompound.getAs("farm", TagByte.class).getBooleanData());
        }
        if (TagCompound.getCompoundData().containsKey("raid")) {
            raidCheckBox.setSelected(TagCompound.getAs("raid", TagByte.class).getBooleanData());
        }
        if (TagCompound.getCompoundData().containsKey("duel")) {
            duelCheckBox.setSelected(TagCompound.getAs("duel", TagByte.class).getBooleanData());
        }
    }
}