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

package de.Keyle.MyPet.skilltreecreator.skills;

import de.keyle.knbt.*;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
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
    private JTextField flyLimitInput;
    private JRadioButton addFlyLimitRadioButton;
    private JRadioButton setFlyLimitRadioButton;
    private JTextField flyRegenRateInput;
    private JRadioButton addFlyRegenRateRadioButton;
    private JRadioButton setFlyRegenRateRadioButton;
    protected JCheckBox canFlyCheckBox;

    public Ride() {
        canFlyCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    flyLimitInput.setEnabled(true);
                    addFlyLimitRadioButton.setEnabled(true);
                    setFlyLimitRadioButton.setEnabled(true);
                    flyRegenRateInput.setEnabled(true);
                    addFlyRegenRateRadioButton.setEnabled(true);
                    setFlyRegenRateRadioButton.setEnabled(true);
                } else {
                    flyLimitInput.setEnabled(false);
                    addFlyLimitRadioButton.setEnabled(false);
                    setFlyLimitRadioButton.setEnabled(false);
                    flyRegenRateInput.setEnabled(false);
                    addFlyRegenRateRadioButton.setEnabled(false);
                    setFlyRegenRateRadioButton.setEnabled(false);
                }
            }
        });
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

        flyLimitInput.setText(flyLimitInput.getText().replaceAll("[^0-9\\.]*", ""));
        if (flyLimitInput.getText().length() > 0) {
            if (flyLimitInput.getText().matches("\\.+")) {
                flyLimitInput.setText("0.0");
            } else {
                try {
                    Pattern regex = Pattern.compile("[0-9]+(\\.[0-9]+)?");
                    Matcher regexMatcher = regex.matcher(flyLimitInput.getText());
                    regexMatcher.find();
                    flyLimitInput.setText(regexMatcher.group());
                } catch (PatternSyntaxException ignored) {
                    flyLimitInput.setText("0.0");
                }
            }
        } else {
            flyLimitInput.setText("0.0");
        }

        flyRegenRateInput.setText(flyRegenRateInput.getText().replaceAll("[^0-9\\.]*", ""));
        if (flyRegenRateInput.getText().length() > 0) {
            if (flyRegenRateInput.getText().matches("\\.+")) {
                flyRegenRateInput.setText("0.0");
            } else {
                try {
                    Pattern regex = Pattern.compile("[0-9]+(\\.[0-9]+)?");
                    Matcher regexMatcher = regex.matcher(flyRegenRateInput.getText());
                    regexMatcher.find();
                    flyRegenRateInput.setText(regexMatcher.group());
                } catch (PatternSyntaxException ignored) {
                    flyRegenRateInput.setText("0.0");
                }
            }
        } else {
            flyRegenRateInput.setText("0.0");
        }
    }

    @Override
    public void resetInput() {
        speedInput.setText("0.0");
        jumpHeightInput.setText("0.0");
        flyLimitInput.setText("0.0");
        flyRegenRateInput.setText("0.0");
        addJumpHeightRadioButton.setSelected(true);
        addSpeedRadioButton.setSelected(true);
        addFlyLimitRadioButton.setSelected(true);
        addFlyRegenRateRadioButton.setSelected(true);
        canFlyCheckBox.setSelected(false);
        flyLimitInput.setEnabled(false);
        addFlyLimitRadioButton.setEnabled(false);
        setFlyLimitRadioButton.setEnabled(false);
        flyRegenRateInput.setEnabled(false);
        addFlyRegenRateRadioButton.setEnabled(false);
        setFlyRegenRateRadioButton.setEnabled(false);
    }

    @Override
    public void save(TagCompound tagCompound) {
        tagCompound.getCompoundData().put("addset_speed", new TagString(addSpeedRadioButton.isSelected() ? "add" : "set"));
        tagCompound.getCompoundData().put("speed_percent", new TagInt(Integer.parseInt(speedInput.getText())));

        tagCompound.getCompoundData().put("addset_jump_height", new TagString(addJumpHeightRadioButton.isSelected() ? "add" : "set"));
        tagCompound.getCompoundData().put("jump_height", new TagDouble(Double.parseDouble(jumpHeightInput.getText())));

        tagCompound.getCompoundData().put("addset_fly_limit", new TagString(addFlyLimitRadioButton.isSelected() ? "add" : "set"));
        tagCompound.getCompoundData().put("fly_limit", new TagFloat(Float.parseFloat(flyLimitInput.getText())));

        tagCompound.getCompoundData().put("addset_fly_regen_rate", new TagString(addFlyRegenRateRadioButton.isSelected() ? "add" : "set"));
        tagCompound.getCompoundData().put("fly_regen_rate", new TagFloat(Float.parseFloat(flyRegenRateInput.getText())));

        tagCompound.getCompoundData().put("can_fly", new TagByte(canFlyCheckBox.isSelected()));
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

        if (TagCompound.getCompoundData().containsKey("addset_fly_limit") && TagCompound.getAs("addset_fly_limit", TagString.class).getStringData().equals("add")) {
            addFlyLimitRadioButton.setSelected(true);
        } else {
            setFlyLimitRadioButton.setSelected(true);
        }
        if (TagCompound.getCompoundData().containsKey("fly_limit")) {
            flyLimitInput.setText("" + TagCompound.getAs("fly_limit", TagFloat.class).getFloatData());
        }

        if (TagCompound.getCompoundData().containsKey("addset_fly_regen_rate") && TagCompound.getAs("addset_fly_regen_rate", TagString.class).getStringData().equals("add")) {
            addFlyRegenRateRadioButton.setSelected(true);
        } else {
            setFlyRegenRateRadioButton.setSelected(true);
        }
        if (TagCompound.getCompoundData().containsKey("fly_regen_rate")) {
            flyRegenRateInput.setText("" + TagCompound.getAs("fly_regen_rate", TagFloat.class).getFloatData());
        }

        if (TagCompound.getCompoundData().containsKey("can_fly")) {
            canFlyCheckBox.setSelected(TagCompound.getAs("can_fly", TagByte.class).getBooleanData());
        }
    }
}