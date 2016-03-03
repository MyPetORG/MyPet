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

import de.keyle.knbt.*;

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
    protected JCheckBox luckChangeCheckBox;
    protected JCheckBox luckEnableCheckBox;

    public Beacon() {
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
        luckChangeCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                luckEnableCheckBox.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
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
    public void resetInput() {
        regenerationEnableCheckBox.setSelected(false);
        regenerationChangeCheckBox.setSelected(false);
        regenerationSpinner.setValue(1);
        speedBostEnableCheckBox.setSelected(false);
        speedBoostChangeCheckBox.setSelected(false);
        speedBoostSpinner.setValue(1);
        hasteEnableCheckBox.setSelected(false);
        hasteChangeCheckBox.setSelected(false);
        hasteSpinner.setValue(1);
        strengthEnableCheckBox.setSelected(false);
        strengthChangeCheckBox.setSelected(false);
        strengthSpinner.setValue(1);
        jumpBoostEnableCheckBox.setSelected(false);
        jumpBoostChangeCheckBox.setSelected(false);
        jumpBoostSpinner.setValue(1);
        regenerationEnableCheckBox.setSelected(false);
        regenerationChangeCheckBox.setSelected(false);
        regenerationSpinner.setValue(1);
        resistanceEnableCheckBox.setSelected(false);
        resistanceChangeCheckBox.setSelected(false);
        resistanceSpinner.setValue(1);
        fireResistanceEnableCheckBox.setSelected(false);
        fireResistanceChangeCheckBox.setSelected(false);
        waterBreathingEnableCheckBox.setSelected(false);
        waterBreathingChangeCheckBox.setSelected(false);
        invisibilityEnableCheckBox.setSelected(false);
        invisibilityChangeCheckBox.setSelected(false);
        nightVisionEnableCheckBox.setSelected(false);
        nightVisionChangeCheckBox.setSelected(false);
        luckEnableCheckBox.setSelected(false);
        luckChangeCheckBox.setSelected(false);
        healthBoostEnableCheckBox.setSelected(false);
        healthBoostChangeCheckBox.setSelected(false);
        healthBoostSpinner.setValue(1);
        absorptionEnableCheckBox.setSelected(false);
        absorptionChangeCheckBox.setSelected(false);
        absorptionSpinner.setValue(1);

        addDurationRadioButton.setSelected(true);
        addRangeRadioButton.setSelected(true);
        setSelectionCount.setSelected(true);

        durationInput.setText("0");
        rangeInput.setText("0.0");
        selectionCountSpinner.setValue(1);
    }

    @Override
    public void save(TagCompound tagCompound) {

        if (speedBoostChangeCheckBox.isSelected()) {
            tagCompound.getCompoundData().put("buff_speed_boost_enable", new TagByte(speedBostEnableCheckBox.isSelected()));
            if (speedBostEnableCheckBox.isSelected()) {
                tagCompound.getCompoundData().put("buff_speed_boost_level", new TagInt(((Number) jumpBoostSpinner.getValue()).intValue()));
            }
        } else {
            tagCompound.getCompoundData().remove("buff_speed_boost_enable");
            tagCompound.getCompoundData().remove("buff_speed_boost_level");
        }
        if (hasteChangeCheckBox.isSelected()) {
            tagCompound.getCompoundData().put("buff_haste_enable", new TagByte(hasteEnableCheckBox.isSelected()));
            if (hasteEnableCheckBox.isSelected()) {
                tagCompound.getCompoundData().put("buff_haste_level", new TagInt(((Number) hasteSpinner.getValue()).intValue()));
            }
        } else {
            tagCompound.getCompoundData().remove("buff_haste_enable");
            tagCompound.getCompoundData().remove("buff_haste_level");
        }
        if (strengthChangeCheckBox.isSelected()) {
            tagCompound.getCompoundData().put("buff_strength_enable", new TagByte(strengthEnableCheckBox.isSelected()));
            if (strengthEnableCheckBox.isSelected()) {
                tagCompound.getCompoundData().put("buff_strength_level", new TagInt(((Number) strengthSpinner.getValue()).intValue()));
            }
        } else {
            tagCompound.getCompoundData().remove("buff_strength_enable");
            tagCompound.getCompoundData().remove("buff_strength_level");
        }
        if (jumpBoostChangeCheckBox.isSelected()) {
            tagCompound.getCompoundData().put("buff_jump_boost_enable", new TagByte(jumpBoostEnableCheckBox.isSelected()));
            if (jumpBoostEnableCheckBox.isSelected()) {
                tagCompound.getCompoundData().put("buff_jump_boost_level", new TagInt(((Number) jumpBoostSpinner.getValue()).intValue()));
            }
        } else {
            tagCompound.getCompoundData().remove("buff_jump_boost_enable");
            tagCompound.getCompoundData().remove("buff_jump_boost_level");
        }
        if (regenerationChangeCheckBox.isSelected()) {
            tagCompound.getCompoundData().put("buff_regeneration_enable", new TagByte(regenerationEnableCheckBox.isSelected()));
            if (regenerationEnableCheckBox.isSelected()) {
                tagCompound.getCompoundData().put("buff_regeneration_level", new TagInt(((Number) regenerationSpinner.getValue()).intValue()));
            }
        } else {
            tagCompound.getCompoundData().remove("buff_regeneration_enable");
            tagCompound.getCompoundData().remove("buff_regeneration_level");
        }
        if (resistanceChangeCheckBox.isSelected()) {
            tagCompound.getCompoundData().put("buff_resistance_enable", new TagByte(resistanceEnableCheckBox.isSelected()));
            if (resistanceEnableCheckBox.isSelected()) {
                tagCompound.getCompoundData().put("buff_resistance_level", new TagInt(((Number) resistanceSpinner.getValue()).intValue()));
            }
        } else {
            tagCompound.getCompoundData().remove("buff_resistance_enable");
            tagCompound.getCompoundData().remove("buff_resistance_level");
        }
        if (fireResistanceChangeCheckBox.isSelected()) {
            tagCompound.getCompoundData().put("buff_fire_resistance_enable", new TagByte(fireResistanceEnableCheckBox.isSelected()));
        } else {
            tagCompound.getCompoundData().remove("buff_fire_resistance_enable");
        }
        if (waterBreathingChangeCheckBox.isSelected()) {
            tagCompound.getCompoundData().put("buff_water_breathing_enable", new TagByte(waterBreathingEnableCheckBox.isSelected()));
        } else {
            tagCompound.getCompoundData().remove("buff_water_breathing_enable");
        }
        if (invisibilityChangeCheckBox.isSelected()) {
            tagCompound.getCompoundData().put("buff_invisibility_enable", new TagByte(invisibilityEnableCheckBox.isSelected()));
        } else {
            tagCompound.getCompoundData().remove("buff_invisibility_enable");
        }
        if (nightVisionChangeCheckBox.isSelected()) {
            tagCompound.getCompoundData().put("buff_night_vision_enable", new TagByte(nightVisionEnableCheckBox.isSelected()));
        } else {
            tagCompound.getCompoundData().remove("buff_night_vision_enable");
        }
        if (luckChangeCheckBox.isSelected()) {
            tagCompound.getCompoundData().put("buff_luck_enable", new TagByte(luckEnableCheckBox.isSelected()));
        } else {
            tagCompound.getCompoundData().remove("buff_luck_enable");
        }
        if (healthBoostChangeCheckBox.isSelected()) {
            tagCompound.getCompoundData().put("buff_health_boost_enable", new TagByte(healthBoostEnableCheckBox.isSelected()));
            if (healthBoostEnableCheckBox.isSelected()) {
                tagCompound.getCompoundData().put("buff_health_boost_level", new TagInt(((Number) healthBoostSpinner.getValue()).intValue()));
            }
        } else {
            tagCompound.getCompoundData().remove("buff_health_boost_enable");
            tagCompound.getCompoundData().remove("buff_health_boost_level");
        }
        if (absorptionChangeCheckBox.isSelected()) {
            tagCompound.getCompoundData().put("buff_absorption_enable", new TagByte(absorptionEnableCheckBox.isSelected()));
            if (absorptionEnableCheckBox.isSelected()) {
                tagCompound.getCompoundData().put("buff_absorption_level", new TagInt(((Number) absorptionSpinner.getValue()).intValue()));
            }
        } else {
            tagCompound.getCompoundData().remove("buff_absorption_enable");
            tagCompound.getCompoundData().remove("buff_absorption_level");
        }

        tagCompound.getCompoundData().put("addset_duration", new TagString(addDurationRadioButton.isSelected() ? "add" : "set"));
        tagCompound.getCompoundData().put("duration", new TagInt(Integer.parseInt(durationInput.getText())));

        tagCompound.getCompoundData().put("addset_range", new TagString(addRangeRadioButton.isSelected() ? "add" : "set"));
        tagCompound.getCompoundData().put("range", new TagDouble(Double.parseDouble(rangeInput.getText())));

        tagCompound.getCompoundData().put("addset_selection_count", new TagString(addSelectionCount.isSelected() ? "add" : "set"));
        tagCompound.getCompoundData().put("selection_count", new TagInt(((Number) selectionCountSpinner.getValue()).intValue()));
    }

    @Override
    public void load(TagCompound tagCompound) {

        if (tagCompound.getCompoundData().containsKey("buff_speed_boost_enable")) {
            speedBoostChangeCheckBox.setSelected(true);
            if (tagCompound.getAs("buff_speed_boost_enable", TagByte.class).getBooleanData()) {
                speedBostEnableCheckBox.setSelected(true);
                if (tagCompound.getCompoundData().containsKey("buff_speed_boost_level")) {
                    speedBoostSpinner.setValue(tagCompound.getAs("buff_speed_boost_level", TagInt.class).getIntData());
                }
            }
        }
        if (tagCompound.getCompoundData().containsKey("buff_haste_enable")) {
            hasteChangeCheckBox.setSelected(true);
            if (tagCompound.getAs("buff_haste_enable", TagByte.class).getBooleanData()) {
                hasteEnableCheckBox.setSelected(true);
                if (tagCompound.getCompoundData().containsKey("buff_haste_level")) {
                    hasteSpinner.setValue(tagCompound.getAs("buff_haste_level", TagInt.class).getIntData());
                }
            }
        }
        if (tagCompound.getCompoundData().containsKey("buff_strength_enable")) {
            strengthChangeCheckBox.setSelected(true);
            if (tagCompound.getAs("buff_strength_enable", TagByte.class).getBooleanData()) {
                strengthEnableCheckBox.setSelected(true);
                if (tagCompound.getCompoundData().containsKey("buff_strength_level")) {
                    strengthSpinner.setValue(tagCompound.getAs("buff_strength_level", TagInt.class).getIntData());
                }
            }
        }
        if (tagCompound.getCompoundData().containsKey("buff_jump_boost_enable")) {
            jumpBoostChangeCheckBox.setSelected(true);
            if (tagCompound.getAs("buff_jump_boost_enable", TagByte.class).getBooleanData()) {
                jumpBoostEnableCheckBox.setSelected(true);
                if (tagCompound.getCompoundData().containsKey("buff_jump_boost_level")) {
                    jumpBoostSpinner.setValue(tagCompound.getAs("buff_jump_boost_level", TagInt.class).getIntData());
                }
            }
        }
        if (tagCompound.getCompoundData().containsKey("buff_regeneration_enable")) {
            regenerationChangeCheckBox.setSelected(true);
            if (tagCompound.getAs("buff_regeneration_enable", TagByte.class).getBooleanData()) {
                regenerationEnableCheckBox.setSelected(true);
                if (tagCompound.getCompoundData().containsKey("buff_regeneration_level")) {
                    regenerationSpinner.setValue(tagCompound.getAs("buff_regeneration_level", TagInt.class).getIntData());
                }
            }
        }
        if (tagCompound.getCompoundData().containsKey("buff_resistance_enable")) {
            resistanceChangeCheckBox.setSelected(true);
            if (tagCompound.getAs("buff_resistance_enable", TagByte.class).getBooleanData()) {
                resistanceEnableCheckBox.setSelected(true);
                if (tagCompound.getCompoundData().containsKey("buff_resistance_level")) {
                    resistanceSpinner.setValue(tagCompound.getAs("buff_resistance_level", TagInt.class).getIntData());
                }
            }
        }
        if (tagCompound.getCompoundData().containsKey("buff_fire_resistance_enable")) {
            fireResistanceChangeCheckBox.setSelected(true);
            if (tagCompound.getAs("buff_fire_resistance_enable", TagByte.class).getBooleanData()) {
                fireResistanceEnableCheckBox.setSelected(true);
            }
        }
        if (tagCompound.getCompoundData().containsKey("buff_water_breathing_enable")) {
            waterBreathingChangeCheckBox.setSelected(true);
            if (tagCompound.getAs("buff_water_breathing_enable", TagByte.class).getBooleanData()) {
                waterBreathingEnableCheckBox.setSelected(true);
            }
        }
        if (tagCompound.getCompoundData().containsKey("buff_invisibility_enable")) {
            invisibilityChangeCheckBox.setSelected(true);
            if (tagCompound.getAs("buff_invisibility_enable", TagByte.class).getBooleanData()) {
                invisibilityEnableCheckBox.setSelected(true);
            }
        }
        if (tagCompound.getCompoundData().containsKey("buff_night_vision_enable")) {
            nightVisionChangeCheckBox.setSelected(true);
            if (tagCompound.getAs("buff_night_vision_enable", TagByte.class).getBooleanData()) {
                nightVisionEnableCheckBox.setSelected(true);
            }
        }
        if (tagCompound.getCompoundData().containsKey("buff_luck_enable")) {
            luckChangeCheckBox.setSelected(true);
            if (tagCompound.getAs("buff_luck_enable", TagByte.class).getBooleanData()) {
                luckEnableCheckBox.setSelected(true);
            }
        }
        if (tagCompound.getCompoundData().containsKey("buff_health_boost_enable")) {
            healthBoostChangeCheckBox.setSelected(true);
            if (tagCompound.getAs("buff_health_boost_enable", TagByte.class).getBooleanData()) {
                healthBoostEnableCheckBox.setSelected(true);
                if (tagCompound.getCompoundData().containsKey("buff_health_boost_level")) {
                    healthBoostSpinner.setValue(tagCompound.getAs("buff_health_boost_level", TagInt.class).getIntData());
                }
            }
        }
        if (tagCompound.getCompoundData().containsKey("buff_absorption_enable")) {
            absorptionChangeCheckBox.setSelected(true);
            if (tagCompound.getAs("buff_absorption_enable", TagByte.class).getBooleanData()) {
                absorptionEnableCheckBox.setSelected(true);
                if (tagCompound.getCompoundData().containsKey("buff_absorption_level")) {
                    absorptionSpinner.setValue(tagCompound.getAs("buff_absorption_level", TagInt.class).getIntData());
                }
            }
        }

        if (!tagCompound.getCompoundData().containsKey("addset_duration") || tagCompound.getAs("addset_duration", TagString.class).getStringData().equals("add")) {
            addDurationRadioButton.setSelected(true);
        } else {
            setDurationRadioButton.setSelected(true);
        }
        if (tagCompound.getCompoundData().containsKey("duration")) {
            durationInput.setText("" + tagCompound.getAs("duration", TagInt.class).getIntData());
        }

        if (!tagCompound.getCompoundData().containsKey("addset_range") || tagCompound.getAs("addset_range", TagString.class).getStringData().equals("add")) {
            addRangeRadioButton.setSelected(true);
        } else {
            setRangeRadioButton.setSelected(true);
        }
        if (tagCompound.getCompoundData().containsKey("range")) {
            rangeInput.setText("" + tagCompound.getAs("range", TagDouble.class).getDoubleData());
        }

        if (tagCompound.getCompoundData().containsKey("addset_selection_count") && tagCompound.getAs("addset_selection_count", TagString.class).getStringData().equals("add")) {
            addSelectionCount.setSelected(false);
        } else {
            setSelectionCount.setSelected(true);
        }
        if (tagCompound.getCompoundData().containsKey("selection_count")) {
            selectionCountSpinner.setValue(tagCompound.getAs("selection_count", TagInt.class).getIntData());
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