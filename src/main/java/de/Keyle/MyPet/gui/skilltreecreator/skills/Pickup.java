/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2014 Keyle
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
import de.keyle.knbt.TagDouble;
import de.keyle.knbt.TagString;

import javax.swing.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class Pickup implements SkillPropertiesPanel {
    private JPanel mainPanel;
    private JTextField rangeInput;
    private JRadioButton addRangeRadioButton;
    private JRadioButton setRangeRadioButton;
    private JCheckBox expPickupCheckBox;

    private TagCompound tagCompound;

    public Pickup(TagCompound tagCompound) {
        this.tagCompound = tagCompound;
        load(tagCompound);
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
    public TagCompound save() {
        tagCompound.getCompoundData().put("addset_range", new TagString(addRangeRadioButton.isSelected() ? "add" : "set"));
        tagCompound.getCompoundData().put("range", new TagDouble(Double.parseDouble(rangeInput.getText())));
        tagCompound.getCompoundData().put("exp_pickup", new TagByte(expPickupCheckBox.isSelected()));

        return tagCompound;
    }

    @Override
    public void load(TagCompound TagCompound) {
        if (!TagCompound.getCompoundData().containsKey("addset_range") || TagCompound.getAs("addset_range", TagString.class).getStringData().equals("add")) {
            addRangeRadioButton.setSelected(true);
        } else {
            setRangeRadioButton.setSelected(true);
        }
        if (TagCompound.getCompoundData().containsKey("range")) {
            rangeInput.setText("" + TagCompound.getAs("range", TagDouble.class).getDoubleData());
        }
        if (TagCompound.getCompoundData().containsKey("exp_pickup")) {
            expPickupCheckBox.setSelected(TagCompound.getAs("exp_pickup", TagByte.class).getBooleanData());
        }
    }
}