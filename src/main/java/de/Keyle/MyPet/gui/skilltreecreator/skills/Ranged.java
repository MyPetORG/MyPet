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

import de.Keyle.MyPet.util.Util;
import org.spout.nbt.CompoundTag;
import org.spout.nbt.DoubleTag;
import org.spout.nbt.IntTag;
import org.spout.nbt.StringTag;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class Ranged implements SkillPropertiesPanel {
    private JPanel mainPanel;
    private JTextField damageInput;
    private JRadioButton addDamageRadioButton;
    private JRadioButton setDamageRadioButton;
    private JComboBox projectileComboBox;
    private JLabel arrowsPerSecondLabel;
    private JRadioButton setRateOfFireRadioButton;
    private JRadioButton addRateOfFireRadioButton;
    private JTextField rateOfFireInput;

    private CompoundTag compoundTag;

    public Ranged(CompoundTag compoundTag) {
        this.compoundTag = compoundTag;
        load(compoundTag);

        rateOfFireInput.addKeyListener(new KeyListener() {
            public void keyTyped(KeyEvent arg0) {
            }

            public void keyReleased(KeyEvent arg0) {
                if (Util.isInt(rateOfFireInput.getText())) {
                    arrowsPerSecondLabel.setText(String.format("%1.2f", 1. / ((Integer.parseInt(rateOfFireInput.getText()) * 50.) / 1000.)) + " Arrows/Second");
                } else {
                    arrowsPerSecondLabel.setText("- Arrows/Second");
                }
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
        if (!rateOfFireInput.getText().replaceAll("[^0-9]*", "").equals(rateOfFireInput.getText())) {
            rateOfFireInput.setText(rateOfFireInput.getText().replaceAll("[^0-9]*", ""));
        }
        if (!rateOfFireInput.getText().matches("[1-9][0-9]*")) {
            rateOfFireInput.setText("1");
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
    public CompoundTag save() {
        compoundTag.getValue().put("projectile", new StringTag("projectile", ((String) projectileComboBox.getSelectedItem()).replace(" ", "")));

        compoundTag.getValue().put("addset_damage", new StringTag("addset_damage", addDamageRadioButton.isSelected() ? "add" : "set"));
        compoundTag.getValue().put("damage_double", new DoubleTag("damage_double", Double.parseDouble(damageInput.getText())));

        compoundTag.getValue().put("addset_rateoffire", new StringTag("addset_rateoffire", addRateOfFireRadioButton.isSelected() ? "add" : "set"));
        compoundTag.getValue().put("rateoffire", new IntTag("rateoffire", Integer.parseInt(rateOfFireInput.getText())));
        return compoundTag;
    }

    @Override
    public void load(CompoundTag compoundTag) {
        if (!compoundTag.getValue().containsKey("addset_damage") || ((StringTag) compoundTag.getValue().get("addset_damage")).getValue().equals("add")) {
            addDamageRadioButton.setSelected(true);
        } else {
            setDamageRadioButton.setSelected(true);
        }
        if (compoundTag.getValue().containsKey("damage")) {
            compoundTag.getValue().put("damage_double", new DoubleTag("damage_double", ((IntTag) compoundTag.getValue().get("damage")).getValue()));
            compoundTag.getValue().remove("damage");
        }
        if (compoundTag.getValue().containsKey("damage_double")) {
            damageInput.setText("" + ((DoubleTag) compoundTag.getValue().get("damage_double")).getValue());
        }

        if (compoundTag.getValue().containsKey("projectile")) {
            String projectileName = ((StringTag) compoundTag.getValue().get("projectile")).getValue();

            for (int i = 0; i < projectileComboBox.getItemCount(); i++) {
                if (((String) projectileComboBox.getItemAt(i)).replace(" ", "").equalsIgnoreCase(projectileName)) {
                    projectileComboBox.setSelectedIndex(i);
                    break;
                }
            }
        }

        if (!compoundTag.getValue().containsKey("addset_rateoffire") || ((StringTag) compoundTag.getValue().get("addset_rateoffire")).getValue().equals("add")) {
            addRateOfFireRadioButton.setSelected(true);
        } else {
            setRateOfFireRadioButton.setSelected(true);
        }
        if (compoundTag.getValue().containsKey("rateoffire")) {
            rateOfFireInput.setText("" + ((IntTag) compoundTag.getValue().get("rateoffire")).getValue());
        }
    }
}