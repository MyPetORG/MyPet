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

public class Ride implements SkillPropertiesPanel {
    private JPanel mainPanel;
    private JTextField speedInput;
    private JRadioButton addSpeedRadioButton;
    private JRadioButton setSpeedRadioButton;
    private JTextField jumpHeightInput;
    private JRadioButton addJumpHeightRadioButton;
    private JRadioButton setJumpHeightRadioButton;

    private TagCompound tagCompound;

    public Ride(TagCompound tagCompound) {
        this.tagCompound = tagCompound;
        load(tagCompound);
    }

    @Override
    public JPanel getMainPanel() {
        return mainPanel;
    }

    @Override
    public void verifyInput() {
        speedInput.setText(speedInput.getText().replaceAll("[^0-9]*", ""));
        if (speedInput.getText().length() == 0) {
            speedInput.setText("0");
        }

        jumpHeightInput.setText(jumpHeightInput.getText().replaceAll("[^0-9\\.]*", ""));
        if (jumpHeightInput.getText().length() > 0) {
            if (jumpHeightInput.getText().matches("\\.+")) {
                jumpHeightInput.setText("0.0");
            } else {
                try {
                    Pattern regex = Pattern.compile("[0-9]+(\\.[0-9]+)?");
                    Matcher regexMatcher = regex.matcher(jumpHeightInput.getText());
                    regexMatcher.find();
                    jumpHeightInput.setText(regexMatcher.group());
                } catch (PatternSyntaxException ignored) {
                    jumpHeightInput.setText("0.0");
                }
            }
        } else {
            jumpHeightInput.setText("0.0");
        }
    }

    @Override
    public TagCompound save() {
        tagCompound.getCompoundData().put("addset_speed", new TagString(addSpeedRadioButton.isSelected() ? "add" : "set"));
        tagCompound.getCompoundData().put("speed_percent", new TagInt(Integer.parseInt(speedInput.getText())));

        tagCompound.getCompoundData().put("addset_jump_height", new TagString(addJumpHeightRadioButton.isSelected() ? "add" : "set"));
        tagCompound.getCompoundData().put("jump_height", new TagDouble(Double.parseDouble(jumpHeightInput.getText())));

        return tagCompound;
    }

    @Override
    public void load(TagCompound TagCompound) {
        if (!TagCompound.getCompoundData().containsKey("addset_speed") || TagCompound.getAs("addset_speed", TagString.class).getStringData().equals("add")) {
            addSpeedRadioButton.setSelected(true);
        } else {
            setSpeedRadioButton.setSelected(true);
        }
        if (TagCompound.getCompoundData().containsKey("speed_percent")) {
            speedInput.setText("" + TagCompound.getAs("speed_percent", TagInt.class).getIntData());
        }

        if (TagCompound.getCompoundData().containsKey("addset_jump_height") && TagCompound.getAs("addset_jump_height", TagString.class).getStringData().equals("add")) {
            addJumpHeightRadioButton.setSelected(true);
        } else {
            setJumpHeightRadioButton.setSelected(true);
        }
        if (TagCompound.getCompoundData().containsKey("jump_height")) {
            jumpHeightInput.setText("" + TagCompound.getAs("jump_height", TagDouble.class).getDoubleData());
        }
    }
}