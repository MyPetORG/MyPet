/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2016 Keyle
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
import de.keyle.knbt.TagInt;

import javax.swing.*;

public class Inventory implements SkillPropertiesPanel {
    private JTextField rowsInput;
    private JPanel mainPanel;
    private JCheckBox dropContentCheckBox;

    private TagCompound tagCompound;

    public Inventory(TagCompound tagCompound) {
        this.tagCompound = tagCompound;
        load(tagCompound);
    }

    @Override
    public JPanel getMainPanel() {
        return mainPanel;
    }

    @Override
    public void verifyInput() {
        rowsInput.setText(rowsInput.getText().replaceAll("[^0-6]*", ""));
        if (rowsInput.getText().length() > 1) {
            rowsInput.setText(rowsInput.getText().substring(0, 1));
        }
        if (rowsInput.getText().length() == 0) {
            rowsInput.setText("0");
        }
    }

    @Override
    public TagCompound save() {
        tagCompound.getCompoundData().put("add", new TagInt(Integer.parseInt(rowsInput.getText())));
        tagCompound.getCompoundData().put("drop", new TagByte(dropContentCheckBox.isSelected()));

        return tagCompound;
    }

    @Override
    public void load(TagCompound TagCompound) {
        if (TagCompound.getCompoundData().containsKey("add")) {
            rowsInput.setText("" + TagCompound.getAs("add", TagInt.class).getIntData());
        }
        if (TagCompound.getCompoundData().containsKey("drop")) {
            dropContentCheckBox.setSelected(TagCompound.getAs("drop", TagByte.class).getBooleanData());
        }
    }
}