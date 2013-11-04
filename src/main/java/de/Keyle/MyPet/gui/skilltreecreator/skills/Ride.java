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
import org.spout.nbt.IntTag;
import org.spout.nbt.StringTag;

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

    private CompoundTag compoundTag;

    public Ride(CompoundTag compoundTag) {
        this.compoundTag = compoundTag;
        load(compoundTag);
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
    public CompoundTag save() {
        compoundTag.getValue().put("addset_speed", new StringTag("addset_speed", addSpeedRadioButton.isSelected() ? "add" : "set"));
        compoundTag.getValue().put("speed_percent", new IntTag("speed_percent", Integer.parseInt(speedInput.getText())));

        compoundTag.getValue().put("addset_jump_height", new StringTag("addset_jump_height", addJumpHeightRadioButton.isSelected() ? "add" : "set"));
        compoundTag.getValue().put("jump_height", new DoubleTag("jump_height", Double.parseDouble(jumpHeightInput.getText())));

        return compoundTag;
    }

    @Override
    public void load(CompoundTag compoundTag) {
        if (!compoundTag.getValue().containsKey("addset_speed") || ((StringTag) compoundTag.getValue().get("addset_speed")).getValue().equals("add")) {
            addSpeedRadioButton.setSelected(true);
        } else {
            setSpeedRadioButton.setSelected(true);
        }
        if (compoundTag.getValue().containsKey("speed_percent")) {
            speedInput.setText("" + ((IntTag) compoundTag.getValue().get("speed_percent")).getValue());
        }

        if (compoundTag.getValue().containsKey("addset_jump_height") && ((StringTag) compoundTag.getValue().get("addset_jump_height")).getValue().equals("add")) {
            addJumpHeightRadioButton.setSelected(true);
        } else {
            setJumpHeightRadioButton.setSelected(true);
        }
        if (compoundTag.getValue().containsKey("jump_height")) {
            jumpHeightInput.setText("" + ((DoubleTag) compoundTag.getValue().get("jump_height")).getValue());
        }
    }
}