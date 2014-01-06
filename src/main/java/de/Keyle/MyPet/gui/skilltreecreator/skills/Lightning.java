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

import de.keyle.knbt.TagCompound;
import de.keyle.knbt.TagDouble;
import de.keyle.knbt.TagInt;
import de.keyle.knbt.TagString;

import javax.swing.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class Lightning implements SkillPropertiesPanel {
    private JPanel mainPanel;
    private JTextField chanceInput;
    private JRadioButton addChanceRadioButton;
    private JRadioButton setChanceRadioButton;
    private JTextField damageInput;
    private JRadioButton addDamageRadioButton;
    private JRadioButton setDamageRadioButton;

    private TagCompound tagCompound;

    public Lightning(TagCompound tagCompound) {
        this.tagCompound = tagCompound;
        load(tagCompound);
    }

    @Override
    public JPanel getMainPanel() {
        return mainPanel;
    }

    @Override
    public void verifyInput() {
        chanceInput.setText(chanceInput.getText().replaceAll("[^0-9]*", ""));
        if (chanceInput.getText().length() == 0) {
            chanceInput.setText("0");
        }

        damageInput.setText(damageInput.getText().replaceAll("[^0-9\\.]*", ""));
        if (damageInput.getText().length() > 0) {
            if (damageInput.getText().matches("\\.+")) {
                damageInput.setText("0.0");
            } else {
                try {
                    Pattern regex = Pattern.compile("[0-9]+(\\.[0-9]+)?");
                    Matcher regexMatcher = regex.matcher(damageInput.getText());
                    regexMatcher.find();
                    damageInput.setText(regexMatcher.group());
                } catch (PatternSyntaxException ignored) {
                    damageInput.setText("0.0");
                }
            }
        } else {
            damageInput.setText("0.0");
        }
    }

    @Override
    public TagCompound save() {
        tagCompound.getCompoundData().put("addset_chance", new TagString(addChanceRadioButton.isSelected() ? "add" : "set"));
        tagCompound.getCompoundData().put("chance", new TagInt(Integer.parseInt(chanceInput.getText())));

        tagCompound.getCompoundData().put("addset_damage", new TagString(addDamageRadioButton.isSelected() ? "add" : "set"));
        tagCompound.getCompoundData().put("damage_double", new TagDouble(Double.parseDouble(damageInput.getText())));

        return tagCompound;
    }

    @Override
    public void load(TagCompound TagCompound) {
        if (!TagCompound.getCompoundData().containsKey("addset_chance") || TagCompound.getAs("addset_chance", TagString.class).getStringData().equals("add")) {
            addChanceRadioButton.setSelected(true);
        } else {
            setChanceRadioButton.setSelected(true);
        }
        if (TagCompound.getCompoundData().containsKey("chance")) {
            chanceInput.setText("" + TagCompound.getAs("chance", TagInt.class).getIntData());
        }

        if (!TagCompound.getCompoundData().containsKey("addset_damage") || TagCompound.getAs("addset_damage", TagString.class).getStringData().equals("add")) {
            addDamageRadioButton.setSelected(true);
        } else {
            setDamageRadioButton.setSelected(true);
        }
        if (TagCompound.getCompoundData().containsKey("damage")) {
            TagCompound.getCompoundData().put("damage_double", new TagDouble(TagCompound.getAs("damage", TagInt.class).getIntData()));
            TagCompound.getCompoundData().remove("damage");
        }
        if (TagCompound.getCompoundData().containsKey("damage_double")) {
            damageInput.setText("" + TagCompound.getAs("damage_double", TagDouble.class).getDoubleData());
        }
    }
}