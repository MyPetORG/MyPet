/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2016 Keyle
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

import de.Keyle.MyPet.api.Util;
import de.keyle.knbt.TagCompound;
import de.keyle.knbt.TagDouble;
import de.keyle.knbt.TagInt;
import de.keyle.knbt.TagString;

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

    public Ranged() {
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
    public void resetInput() {
        damageInput.setText("0.0");
        rateOfFireInput.setText("0");
        addDamageRadioButton.setSelected(true);
        addRateOfFireRadioButton.setSelected(true);
        projectileComboBox.setSelectedIndex(0);
    }

    @Override
    public void save(TagCompound tagCompound) {
        tagCompound.getCompoundData().put("projectile", new TagString(((String) projectileComboBox.getSelectedItem()).replace(" ", "")));

        tagCompound.getCompoundData().put("addset_damage", new TagString(addDamageRadioButton.isSelected() ? "add" : "set"));
        tagCompound.getCompoundData().put("damage_double", new TagDouble(Double.parseDouble(damageInput.getText())));

        tagCompound.getCompoundData().put("addset_rateoffire", new TagString(addRateOfFireRadioButton.isSelected() ? "add" : "set"));
        tagCompound.getCompoundData().put("rateoffire", new TagInt(Integer.parseInt(rateOfFireInput.getText())));
    }

    @Override
    public void load(TagCompound TagCompound) {
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

        if (TagCompound.getCompoundData().containsKey("projectile")) {
            String projectileName = TagCompound.getAs("projectile", TagString.class).getStringData();

            for (int i = 0; i < projectileComboBox.getItemCount(); i++) {
                if (((String) projectileComboBox.getItemAt(i)).replace(" ", "").equalsIgnoreCase(projectileName)) {
                    projectileComboBox.setSelectedIndex(i);
                    break;
                }
            }
        }

        if (!TagCompound.getCompoundData().containsKey("addset_rateoffire") || TagCompound.getAs("addset_rateoffire", TagString.class).getStringData().equals("add")) {
            addRateOfFireRadioButton.setSelected(true);
        } else {
            setRateOfFireRadioButton.setSelected(true);
        }
        if (TagCompound.getCompoundData().containsKey("rateoffire")) {
            rateOfFireInput.setText("" + TagCompound.getAs("rateoffire", TagInt.class).getIntData());
        }
    }
}