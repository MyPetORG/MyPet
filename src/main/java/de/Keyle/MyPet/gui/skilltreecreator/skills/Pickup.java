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
import org.spout.nbt.DoubleTag;
import org.spout.nbt.StringTag;

import javax.swing.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class Pickup implements SkillPropertiesPanel {
    private JPanel mainPanel;
    private JTextField rangeInput;
    private JRadioButton addRangeRadioButton;
    private JRadioButton setRangeRadioButton;

    private CompoundTag compoundTag;

    public Pickup(CompoundTag compoundTag) {
        this.compoundTag = compoundTag;
        load(compoundTag);
    }

    @Override
    public JPanel getMainPanel() {
        return mainPanel;
    }

    @Override
    public void verifyInput() {
        rangeInput.setText(rangeInput.getText().replaceAll("[^0-9\\.]*", ""));
        if (rangeInput.getText().length() > 0) {
            if (rangeInput.getText().matches("\\.+")) {
                rangeInput.setText("0.0");
            } else {
                try {
                    Pattern regex = Pattern.compile("[0-9]+(\\.[0-9]+)?");
                    Matcher regexMatcher = regex.matcher(rangeInput.getText());
                    regexMatcher.find();
                    rangeInput.setText(regexMatcher.group());
                } catch (PatternSyntaxException ignored) {
                    rangeInput.setText("0.0");
                }
            }
        } else {
            rangeInput.setText("0.0");
        }
    }

    @Override
    public CompoundTag save() {
        compoundTag.getValue().put("addset_range", new StringTag("addset_range", addRangeRadioButton.isSelected() ? "add" : "set"));
        compoundTag.getValue().put("range", new DoubleTag("range", Double.parseDouble(rangeInput.getText())));

        return compoundTag;
    }

    @Override
    public void load(CompoundTag compoundTag) {
        if (!compoundTag.getValue().containsKey("addset_range") || ((StringTag) compoundTag.getValue().get("addset_range")).getValue().equals("add")) {
            addRangeRadioButton.setSelected(true);
        } else {
            setRangeRadioButton.setSelected(true);
        }
        if (compoundTag.getValue().containsKey("range")) {
            rangeInput.setText("" + ((DoubleTag) compoundTag.getValue().get("range")).getValue());
        }
    }
}