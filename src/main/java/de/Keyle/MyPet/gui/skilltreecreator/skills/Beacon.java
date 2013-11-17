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
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
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
    private JTextField durationInput;
    private JRadioButton addDurationRadioButton;
    private JRadioButton setDurationRadioButton;
    private JSpinner hasteSpinner;
    private JSpinner speedBoostSpinner;
    private JSpinner strengthSpinner;
    private JSpinner jumpBoostSpinner;
    private JSpinner regenerationSpinner;
    private JSpinner resistanceSpinner;
    private JSpinner healthBoostSpinner;
    private JSpinner absorptionSpinner;
    private JCheckBox speedBostEnableCheckBox;
    private JCheckBox hasteEnableCheckBox;
    private JCheckBox strengthEnableCheckBox;
    private JCheckBox jumpBoostEnableCheckBox;
    private JCheckBox regenerationEnableCheckBox;
    private JCheckBox resistanceEnableCheckBox;
    private JCheckBox fireResistanceEnableCheckBox;
    private JCheckBox waterBreathingEnableCheckBox;
    private JCheckBox invisibilityEnableCheckBox;
    private JCheckBox nightVisionEnableCheckBox;
    private JCheckBox healthBoostEnableCheckBox;
    private JCheckBox absorptionEnableCheckBox;
    private JCheckBox speedBoostChangeCheckBox;
    private JCheckBox hasteChangeCheckBox;
    private JCheckBox strengthChangeCheckBox;
    private JCheckBox jumpBoostChangeCheckBox;
    private JCheckBox absorptionChangeCheckBox;
    private JCheckBox healthBoostChangeCheckBox;
    private JCheckBox nightVisionChangeCheckBox;
    private JCheckBox invisibilityChangeCheckBox;
    private JCheckBox waterBreathingChangeCheckBox;
    private JCheckBox fireResistanceChangeCheckBox;
    private JCheckBox resistanceChangeCheckBox;
    private JCheckBox regenerationChangeCheckBox;
    private JSpinner selectionCountSpinner;
    private JRadioButton setSelectionCount;
    private JRadioButton addSelectionCount;
    private CompoundTag compoundTag;

    public Beacon(CompoundTag compoundTag) {
        rangeInput.addKeyListener(new KeyListener() {
            public void keyTyped(KeyEvent arg0) {
            }

            public void keyReleased(KeyEvent arg0) {
                rangeInput.setText(rangeInput.getText().replaceAll("[^0-9.]*", ""));
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
        speedBostEnableCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                speedBoostSpinner.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
            }
        });
        hasteEnableCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                hasteSpinner.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
            }
        });
        strengthEnableCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                strengthSpinner.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
            }
        });
        jumpBoostEnableCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                jumpBoostSpinner.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
            }
        });
        regenerationEnableCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                regenerationSpinner.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
            }
        });
        resistanceEnableCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                resistanceSpinner.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
            }
        });
        healthBoostEnableCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                healthBoostSpinner.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
            }
        });
        absorptionEnableCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                absorptionSpinner.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
            }
        });
        speedBoostChangeCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                speedBostEnableCheckBox.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
            }
        });
        hasteChangeCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                hasteEnableCheckBox.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
            }
        });
        strengthChangeCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                strengthEnableCheckBox.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
            }
        });
        jumpBoostChangeCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                jumpBoostEnableCheckBox.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
            }
        });
        regenerationChangeCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                regenerationEnableCheckBox.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
            }
        });
        resistanceChangeCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                resistanceEnableCheckBox.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
            }
        });
        fireResistanceChangeCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                fireResistanceEnableCheckBox.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
            }
        });
        waterBreathingChangeCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                waterBreathingEnableCheckBox.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
            }
        });
        invisibilityChangeCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                invisibilityEnableCheckBox.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
            }
        });
        nightVisionChangeCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                nightVisionEnableCheckBox.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
            }
        });
        healthBoostChangeCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                healthBoostEnableCheckBox.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
            }
        });
        absorptionChangeCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                absorptionEnableCheckBox.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
            }
        });


        this.compoundTag = compoundTag;
        load(compoundTag);
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

        if (speedBoostChangeCheckBox.isSelected()) {
            compoundTag.getValue().put("buff_speed_boost_enable", new ByteTag("buff_speed_boost_enable", speedBostEnableCheckBox.isSelected()));
            if (speedBostEnableCheckBox.isSelected()) {
                compoundTag.getValue().put("buff_speed_boost_level", new IntTag("buff_speed_boost_level", ((Number) jumpBoostSpinner.getValue()).intValue()));
            }
        }
        if (hasteChangeCheckBox.isSelected()) {
            compoundTag.getValue().put("buff_haste_enable", new ByteTag("buff_haste_enable", hasteEnableCheckBox.isSelected()));
            if (hasteEnableCheckBox.isSelected()) {
                compoundTag.getValue().put("buff_haste_level", new IntTag("buff_haste_level", ((Number) hasteSpinner.getValue()).intValue()));
            }
        }
        if (strengthChangeCheckBox.isSelected()) {
            compoundTag.getValue().put("buff_strength_enable", new ByteTag("buff_strength_enable", strengthEnableCheckBox.isSelected()));
            if (strengthEnableCheckBox.isSelected()) {
                compoundTag.getValue().put("buff_strength_level", new IntTag("buff_strength_level", ((Number) strengthSpinner.getValue()).intValue()));
            }
        }
        if (jumpBoostChangeCheckBox.isSelected()) {
            compoundTag.getValue().put("buff_jump_boost_enable", new ByteTag("buff_jump_boost_enable", jumpBoostEnableCheckBox.isSelected()));
            if (jumpBoostEnableCheckBox.isSelected()) {
                compoundTag.getValue().put("buff_jump_boost_level", new IntTag("buff_jump_boost_level", ((Number) jumpBoostSpinner.getValue()).intValue()));
            }
        }
        if (regenerationChangeCheckBox.isSelected()) {
            compoundTag.getValue().put("buff_regeneration_enable", new ByteTag("buff_regeneration_enable", regenerationEnableCheckBox.isSelected()));
            if (regenerationEnableCheckBox.isSelected()) {
                compoundTag.getValue().put("buff_regeneration_level", new IntTag("buff_regeneration_level", ((Number) regenerationSpinner.getValue()).intValue()));
            }
        }
        if (resistanceChangeCheckBox.isSelected()) {
            compoundTag.getValue().put("buff_resistance_enable", new ByteTag("buff_resistance_enable", resistanceEnableCheckBox.isSelected()));
            if (resistanceEnableCheckBox.isSelected()) {
                compoundTag.getValue().put("buff_resistance_level", new IntTag("buff_resistance_level", ((Number) resistanceSpinner.getValue()).intValue()));
            }
        }
        if (fireResistanceChangeCheckBox.isSelected()) {
            compoundTag.getValue().put("buff_fire_resistance_enable", new ByteTag("buff_fire_resistance_enable", fireResistanceEnableCheckBox.isSelected()));
        }
        if (waterBreathingChangeCheckBox.isSelected()) {
            compoundTag.getValue().put("buff_water_breathing_enable", new ByteTag("buff_water_breathing_enable", waterBreathingEnableCheckBox.isSelected()));
        }
        if (invisibilityChangeCheckBox.isSelected()) {
            compoundTag.getValue().put("buff_invisibility_enable", new ByteTag("buff_invisibility_enable", invisibilityEnableCheckBox.isSelected()));
        }
        if (nightVisionChangeCheckBox.isSelected()) {
            compoundTag.getValue().put("buff_night_vision_enable", new ByteTag("buff_night_vision_enable", nightVisionEnableCheckBox.isSelected()));
        }
        if (healthBoostChangeCheckBox.isSelected()) {
            compoundTag.getValue().put("buff_health_boost_enable", new ByteTag("buff_health_boost_enable", healthBoostEnableCheckBox.isSelected()));
            if (healthBoostEnableCheckBox.isSelected()) {
                compoundTag.getValue().put("buff_health_boost_level", new IntTag("buff_health_boost_level", ((Number) healthBoostSpinner.getValue()).intValue()));
            }
        }
        if (absorptionChangeCheckBox.isSelected()) {
            compoundTag.getValue().put("buff_absorption_enable", new ByteTag("buff_absorption_enable", absorptionEnableCheckBox.isSelected()));
            if (absorptionEnableCheckBox.isSelected()) {
                compoundTag.getValue().put("buff_absorption_level", new IntTag("buff_absorption_level", ((Number) absorptionSpinner.getValue()).intValue()));
            }
        }

        compoundTag.getValue().put("addset_duration", new StringTag("addset_duration", addDurationRadioButton.isSelected() ? "add" : "set"));
        compoundTag.getValue().put("duration", new IntTag("duration", Integer.parseInt(durationInput.getText())));

        compoundTag.getValue().put("addset_range", new StringTag("addset_range", addRangeRadioButton.isSelected() ? "add" : "set"));
        compoundTag.getValue().put("range", new DoubleTag("range", Double.parseDouble(rangeInput.getText())));

        compoundTag.getValue().put("addset_selection_count", new StringTag("addset_selection_count", addSelectionCount.isSelected() ? "add" : "set"));
        compoundTag.getValue().put("selection_count", new IntTag("selection_count", ((Number) selectionCountSpinner.getValue()).intValue()));

        return compoundTag;
    }

    @Override
    public void load(CompoundTag compoundTag) {

        if (compoundTag.getValue().containsKey("buff_speed_boost_enable")) {
            speedBoostChangeCheckBox.setSelected(true);
            if (((ByteTag) compoundTag.getValue().get("buff_speed_boost_enable")).getBooleanValue()) {
                speedBostEnableCheckBox.setSelected(true);
                if (compoundTag.getValue().containsKey("buff_speed_boost_level")) {
                    speedBoostSpinner.setValue(((IntTag) compoundTag.getValue().get("buff_speed_boost_level")).getValue());
                }
            }
        }
        if (compoundTag.getValue().containsKey("buff_haste_enable")) {
            hasteChangeCheckBox.setSelected(true);
            if (((ByteTag) compoundTag.getValue().get("buff_haste_enable")).getBooleanValue()) {
                hasteEnableCheckBox.setSelected(true);
                if (compoundTag.getValue().containsKey("buff_haste_level")) {
                    hasteSpinner.setValue(((IntTag) compoundTag.getValue().get("buff_haste_level")).getValue());
                }
            }
        }
        if (compoundTag.getValue().containsKey("buff_strength_enable")) {
            strengthChangeCheckBox.setSelected(true);
            if (((ByteTag) compoundTag.getValue().get("buff_strength_enable")).getBooleanValue()) {
                strengthEnableCheckBox.setSelected(true);
                if (compoundTag.getValue().containsKey("buff_strength_level")) {
                    strengthSpinner.setValue(((IntTag) compoundTag.getValue().get("buff_strength_level")).getValue());
                }
            }
        }
        if (compoundTag.getValue().containsKey("buff_jump_boost_enable")) {
            jumpBoostChangeCheckBox.setSelected(true);
            if (((ByteTag) compoundTag.getValue().get("buff_jump_boost_enable")).getBooleanValue()) {
                jumpBoostEnableCheckBox.setSelected(true);
                if (compoundTag.getValue().containsKey("buff_jump_boost_level")) {
                    jumpBoostSpinner.setValue(((IntTag) compoundTag.getValue().get("buff_jump_boost_level")).getValue());
                }
            }
        }
        if (compoundTag.getValue().containsKey("buff_regeneration_enable")) {
            regenerationChangeCheckBox.setSelected(true);
            if (((ByteTag) compoundTag.getValue().get("buff_regeneration_enable")).getBooleanValue()) {
                regenerationEnableCheckBox.setSelected(true);
                if (compoundTag.getValue().containsKey("buff_regeneration_level")) {
                    regenerationSpinner.setValue(((IntTag) compoundTag.getValue().get("buff_regeneration_level")).getValue());
                }
            }
        }
        if (compoundTag.getValue().containsKey("buff_resistance_enable")) {
            resistanceChangeCheckBox.setSelected(true);
            if (((ByteTag) compoundTag.getValue().get("buff_resistance_enable")).getBooleanValue()) {
                resistanceEnableCheckBox.setSelected(true);
                if (compoundTag.getValue().containsKey("buff_resistance_level")) {
                    resistanceSpinner.setValue(((IntTag) compoundTag.getValue().get("buff_resistance_level")).getValue());
                }
            }
        }
        if (compoundTag.getValue().containsKey("buff_fire_resistance_enable")) {
            fireResistanceChangeCheckBox.setSelected(true);
            if (((ByteTag) compoundTag.getValue().get("buff_fire_resistance_enable")).getBooleanValue()) {
                fireResistanceEnableCheckBox.setSelected(true);
            }
        }
        if (compoundTag.getValue().containsKey("buff_water_breathing_enable")) {
            waterBreathingChangeCheckBox.setSelected(true);
            if (((ByteTag) compoundTag.getValue().get("buff_water_breathing_enable")).getBooleanValue()) {
                waterBreathingEnableCheckBox.setSelected(true);
            }
        }
        if (compoundTag.getValue().containsKey("buff_invisibility_enable")) {
            invisibilityChangeCheckBox.setSelected(true);
            if (((ByteTag) compoundTag.getValue().get("buff_invisibility_enable")).getBooleanValue()) {
                invisibilityEnableCheckBox.setSelected(true);
            }
        }
        if (compoundTag.getValue().containsKey("buff_night_vision_enable")) {
            nightVisionChangeCheckBox.setSelected(true);
            if (((ByteTag) compoundTag.getValue().get("buff_night_vision_enable")).getBooleanValue()) {
                nightVisionEnableCheckBox.setSelected(true);
            }
        }
        if (compoundTag.getValue().containsKey("buff_health_boost_enable")) {
            healthBoostChangeCheckBox.setSelected(true);
            if (((ByteTag) compoundTag.getValue().get("buff_health_boost_enable")).getBooleanValue()) {
                healthBoostEnableCheckBox.setSelected(true);
                if (compoundTag.getValue().containsKey("buff_health_boost_level")) {
                    healthBoostSpinner.setValue(((IntTag) compoundTag.getValue().get("buff_health_boost_level")).getValue());
                }
            }
        }
        if (compoundTag.getValue().containsKey("buff_absorption_enable")) {
            absorptionChangeCheckBox.setSelected(true);
            if (((ByteTag) compoundTag.getValue().get("buff_absorption_enable")).getBooleanValue()) {
                absorptionEnableCheckBox.setSelected(true);
                if (compoundTag.getValue().containsKey("buff_absorption_level")) {
                    absorptionSpinner.setValue(((IntTag) compoundTag.getValue().get("buff_absorption_level")).getValue());
                }
            }
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
            addRangeRadioButton.setSelected(true);
        } else {
            setRangeRadioButton.setSelected(true);
        }
        if (compoundTag.getValue().containsKey("range")) {
            rangeInput.setText("" + ((DoubleTag) compoundTag.getValue().get("range")).getValue());
        }

        if (compoundTag.getValue().containsKey("addset_selection_count") && ((StringTag) compoundTag.getValue().get("addset_selection_count")).getValue().equals("add")) {
            addSelectionCount.setSelected(false);
        } else {
            setSelectionCount.setSelected(true);
        }
        if (compoundTag.getValue().containsKey("selection_count")) {
            selectionCountSpinner.setValue(((IntTag) compoundTag.getValue().get("selection_count")).getValue());
        }
    }

    private void createUIComponents() {
        SpinnerModel sm = new SpinnerNumberModel(1, 1, 5, 1);
        hasteSpinner = new JSpinner(sm);
        ((JSpinner.DefaultEditor) hasteSpinner.getEditor()).getTextField().setEditable(false);
        sm = new SpinnerNumberModel(1, 1, 5, 1);
        speedBoostSpinner = new JSpinner(sm);
        ((JSpinner.DefaultEditor) speedBoostSpinner.getEditor()).getTextField().setEditable(false);
        sm = new SpinnerNumberModel(1, 1, 5, 1);
        strengthSpinner = new JSpinner(sm);
        ((JSpinner.DefaultEditor) strengthSpinner.getEditor()).getTextField().setEditable(false);
        sm = new SpinnerNumberModel(1, 1, 5, 1);
        jumpBoostSpinner = new JSpinner(sm);
        ((JSpinner.DefaultEditor) jumpBoostSpinner.getEditor()).getTextField().setEditable(false);
        sm = new SpinnerNumberModel(1, 1, 5, 1);
        regenerationSpinner = new JSpinner(sm);
        ((JSpinner.DefaultEditor) regenerationSpinner.getEditor()).getTextField().setEditable(false);
        sm = new SpinnerNumberModel(1, 1, 5, 1);
        resistanceSpinner = new JSpinner(sm);
        ((JSpinner.DefaultEditor) resistanceSpinner.getEditor()).getTextField().setEditable(false);
        sm = new SpinnerNumberModel(1, 1, 5, 1);
        healthBoostSpinner = new JSpinner(sm);
        ((JSpinner.DefaultEditor) healthBoostSpinner.getEditor()).getTextField().setEditable(false);
        sm = new SpinnerNumberModel(1, 1, 5, 1);
        absorptionSpinner = new JSpinner(sm);
        ((JSpinner.DefaultEditor) absorptionSpinner.getEditor()).getTextField().setEditable(false);
        sm = new SpinnerNumberModel(1, 1, 12, 1);
        selectionCountSpinner = new JSpinner(sm);
        ((JSpinner.DefaultEditor) selectionCountSpinner.getEditor()).getTextField().setEditable(false);
    }
}