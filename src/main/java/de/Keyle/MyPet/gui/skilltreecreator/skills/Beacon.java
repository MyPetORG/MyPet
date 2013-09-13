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

import org.spout.nbt.*;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class Beacon implements SkillPropertiesPanel {
    private JTextField rangeInput;
    private JRadioButton addRangeRadioButton;
    private JRadioButton setRangeRadioButton;
    private JPanel mainPanel;
    private JCheckBox speedPrimaryCheckBox;
    private JCheckBox resistancePrimaryCheckBox;
    private JCheckBox jumpBoostPrimaryCheckBox;
    private JCheckBox strengthPrimaryCheckBox;
    private JCheckBox hastePrimaryCheckBox;
    private JCheckBox regenerationSecundaryCheckBox;
    private JCheckBox speedSecundaryCheckBox;
    private JCheckBox resistanceSecundaryCheckBox;
    private JCheckBox strengthSecundaryCheckBox;
    private JCheckBox jumpBoostSecundaryCheckBox;
    private JCheckBox hasteSecundaryCheckBox;
    private JTextField durationInput;
    private JRadioButton addDurationRadioButton;
    private JRadioButton setDurationRadioButton;
    private CompoundTag compoundTag;

    public Beacon(CompoundTag compoundTag) {
        this.compoundTag = compoundTag;
        load(compoundTag);

        speedPrimaryCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (speedPrimaryCheckBox.isSelected()) {
                    speedSecundaryCheckBox.setEnabled(true);
                } else {
                    speedSecundaryCheckBox.setSelected(false);
                    speedSecundaryCheckBox.setEnabled(false);
                }
            }
        });
        resistancePrimaryCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (resistancePrimaryCheckBox.isSelected()) {
                    resistanceSecundaryCheckBox.setEnabled(true);
                } else {
                    resistanceSecundaryCheckBox.setSelected(false);
                    resistanceSecundaryCheckBox.setEnabled(false);
                }
            }
        });
        jumpBoostPrimaryCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (jumpBoostPrimaryCheckBox.isSelected()) {
                    jumpBoostSecundaryCheckBox.setEnabled(true);
                } else {
                    jumpBoostSecundaryCheckBox.setSelected(false);
                    jumpBoostSecundaryCheckBox.setEnabled(false);
                }
            }
        });
        strengthPrimaryCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (strengthPrimaryCheckBox.isSelected()) {
                    strengthSecundaryCheckBox.setEnabled(true);
                } else {
                    strengthSecundaryCheckBox.setSelected(false);
                    strengthSecundaryCheckBox.setEnabled(false);
                }
            }
        });
        hastePrimaryCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (hastePrimaryCheckBox.isSelected()) {
                    hasteSecundaryCheckBox.setEnabled(true);
                } else {
                    hasteSecundaryCheckBox.setSelected(false);
                    hasteSecundaryCheckBox.setEnabled(false);
                }
            }
        });
        durationInput.addKeyListener(new KeyListener() {
            public void keyTyped(KeyEvent arg0) {
            }

            public void keyReleased(KeyEvent arg0) {
                durationInput.setText(durationInput.getText().replaceAll("[^0-9]*", ""));
            }

            public void keyPressed(KeyEvent arg0) {
            }
        });
        durationInput.addKeyListener(new KeyListener() {
            public void keyTyped(KeyEvent arg0) {
            }

            public void keyReleased(KeyEvent arg0) {
                durationInput.setText(durationInput.getText().replaceAll("[^0-9]*", ""));
            }

            public void keyPressed(KeyEvent arg0) {
            }
        });
    }

    @Override
    public JPanel getMainPanel() {
        return mainPanel;
    }

    @Override
    public void verifyInput() {
        durationInput.setText(durationInput.getText().replaceAll("[^0-9]*", ""));
        if (durationInput.getText().length() == 0) {
            durationInput.setText("0");
        }

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
        compoundTag.getValue().put("1_1", new ByteTag("1_1", speedPrimaryCheckBox.isSelected()));
        compoundTag.getValue().put("1_3", new ByteTag("1_3", hastePrimaryCheckBox.isSelected()));
        compoundTag.getValue().put("1_5", new ByteTag("1_5", strengthPrimaryCheckBox.isSelected()));
        compoundTag.getValue().put("1_8", new ByteTag("1_8", jumpBoostPrimaryCheckBox.isSelected()));
        compoundTag.getValue().put("1_11", new ByteTag("1_11", resistancePrimaryCheckBox.isSelected()));

        compoundTag.getValue().put("2_1", new ByteTag("2_1", speedSecundaryCheckBox.isSelected()));
        compoundTag.getValue().put("2_3", new ByteTag("2_3", hasteSecundaryCheckBox.isSelected()));
        compoundTag.getValue().put("2_5", new ByteTag("2_5", strengthSecundaryCheckBox.isSelected()));
        compoundTag.getValue().put("2_8", new ByteTag("2_8", jumpBoostSecundaryCheckBox.isSelected()));
        compoundTag.getValue().put("2_10", new ByteTag("2_10", regenerationSecundaryCheckBox.isSelected()));
        compoundTag.getValue().put("2_11", new ByteTag("2_11", resistanceSecundaryCheckBox.isSelected()));

        compoundTag.getValue().put("addset_duration", new StringTag("addset_duration", addDurationRadioButton.isSelected() ? "add" : "set"));
        compoundTag.getValue().put("duration", new IntTag("duration", Integer.parseInt(durationInput.getText())));

        compoundTag.getValue().put("addset_range", new StringTag("addset_range", addRangeRadioButton.isSelected() ? "add" : "set"));
        compoundTag.getValue().put("range", new DoubleTag("range", Double.parseDouble(rangeInput.getText())));

        return compoundTag;
    }

    @Override
    public void load(CompoundTag compoundTag) {
        if (compoundTag.getValue().containsKey("1_1")) {
            speedPrimaryCheckBox.setSelected(((ByteTag) compoundTag.getValue().get("1_1")).getBooleanValue());
        }
        if (compoundTag.getValue().containsKey("1_3")) {
            hastePrimaryCheckBox.setSelected(((ByteTag) compoundTag.getValue().get("1_3")).getBooleanValue());
        }
        if (compoundTag.getValue().containsKey("1_5")) {
            strengthPrimaryCheckBox.setSelected(((ByteTag) compoundTag.getValue().get("1_5")).getBooleanValue());
        }
        if (compoundTag.getValue().containsKey("1_8")) {
            jumpBoostPrimaryCheckBox.setSelected(((ByteTag) compoundTag.getValue().get("1_8")).getBooleanValue());
        }
        if (compoundTag.getValue().containsKey("1_11")) {
            resistancePrimaryCheckBox.setSelected(((ByteTag) compoundTag.getValue().get("1_11")).getBooleanValue());
        }

        if (compoundTag.getValue().containsKey("2_1")) {
            speedSecundaryCheckBox.setSelected(((ByteTag) compoundTag.getValue().get("2_1")).getBooleanValue());
        }
        if (compoundTag.getValue().containsKey("2_3")) {
            hasteSecundaryCheckBox.setSelected(((ByteTag) compoundTag.getValue().get("2_3")).getBooleanValue());
        }
        if (compoundTag.getValue().containsKey("2_5")) {
            strengthSecundaryCheckBox.setSelected(((ByteTag) compoundTag.getValue().get("2_5")).getBooleanValue());
        }
        if (compoundTag.getValue().containsKey("2_8")) {
            jumpBoostSecundaryCheckBox.setSelected(((ByteTag) compoundTag.getValue().get("2_8")).getBooleanValue());
        }
        if (compoundTag.getValue().containsKey("2_10")) {
            regenerationSecundaryCheckBox.setSelected(((ByteTag) compoundTag.getValue().get("2_10")).getBooleanValue());
        }
        if (compoundTag.getValue().containsKey("2_11")) {
            resistanceSecundaryCheckBox.setSelected(((ByteTag) compoundTag.getValue().get("2_11")).getBooleanValue());
        }

        if (!compoundTag.getValue().containsKey("addset_duration") || ((StringTag) compoundTag.getValue().get("addset_duration")).getValue().equals("add")) {
            addDurationRadioButton.setSelected(true);
        } else {
            setDurationRadioButton.setSelected(true);
        }
        if (compoundTag.getValue().containsKey("duration")) {
            durationInput.setText("" + ((IntTag) compoundTag.getValue().get("duration")).getValue());
        }

        if (!compoundTag.getValue().containsKey("addset_range") || ((StringTag) compoundTag.getValue().get("addset_range")).getValue().equals("add")) {
            setRangeRadioButton.setSelected(true);
        } else {
            setRangeRadioButton.setSelected(true);
        }
        if (compoundTag.getValue().containsKey("range")) {
            rangeInput.setText("" + ((DoubleTag) compoundTag.getValue().get("range")).getValue());
        }
    }
}