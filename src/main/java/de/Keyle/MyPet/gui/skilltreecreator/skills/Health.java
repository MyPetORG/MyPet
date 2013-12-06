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

import de.keyle.knbt.TagCompound;
import de.keyle.knbt.TagDouble;
import de.keyle.knbt.TagInt;
import de.keyle.knbt.TagString;

import javax.swing.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class Health implements SkillPropertiesPanel {
    private JTextField healthInput;
    private JRadioButton addHealthRadioButton;
    private JRadioButton setHealthRadioButton;
    private JPanel mainPanel;

    private TagCompound tagCompound;

    public Health(TagCompound tagCompound) {
        this.tagCompound = tagCompound;
        load(tagCompound);
    }

    @Override
    public JPanel getMainPanel() {
        return mainPanel;
    }

    @Override
    public void verifyInput() {
        healthInput.setText(healthInput.getText().replaceAll("[^0-9\\.]*", ""));
        if (healthInput.getText().length() > 0) {
            if (healthInput.getText().matches("\\.+")) {
                healthInput.setText("0.0");
            } else {
                try {
                    Pattern regex = Pattern.compile("[0-9]+(\\.[0-9]+)?");
                    Matcher regexMatcher = regex.matcher(healthInput.getText());
                    regexMatcher.find();
                    healthInput.setText(regexMatcher.group());
                } catch (PatternSyntaxException ignored) {
                    healthInput.setText("0.0");
                }
            }
        } else {
            healthInput.setText("0.0");
        }
    }

    @Override
    public TagCompound save() {
        tagCompound.getCompoundData().put("addset_hp", new TagString(addHealthRadioButton.isSelected() ? "add" : "set"));
        tagCompound.getCompoundData().put("hp_double", new TagDouble(Double.parseDouble(healthInput.getText())));

        return tagCompound;
    }

    @Override
    public void load(TagCompound TagCompound) {
        if (!TagCompound.getCompoundData().containsKey("addset_hp") || TagCompound.getAs("addset_hp", TagString.class).getStringData().equals("add")) {
            addHealthRadioButton.setSelected(true);
        } else {
            setHealthRadioButton.setSelected(true);
        }

        if (TagCompound.getCompoundData().containsKey("hp")) {
            TagCompound.getCompoundData().put("hp_double", new TagDouble(TagCompound.getAs("hp", TagInt.class).getIntData()));
            TagCompound.getCompoundData().remove("hp");
        }
        if (TagCompound.getCompoundData().containsKey("hp_double")) {
            healthInput.setText("" + TagCompound.getAs("hp_double", TagDouble.class).getDoubleData());
        }
    }
}