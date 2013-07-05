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

public class Lightning implements SkillPropertiesPanel
{
    private JPanel mainPanel;
    private JTextField chanceInput;
    private JRadioButton addChanceRadioButton;
    private JRadioButton setChanceRadioButton;
    private JTextField damageInput;
    private JRadioButton addDamageRadioButton;
    private JRadioButton setDamageRadioButton;

    private CompoundTag compoundTag;

    public Lightning(CompoundTag compoundTag)
    {
        this.compoundTag = compoundTag;
        load(compoundTag);
    }

    @Override
    public JPanel getMainPanel()
    {
        return mainPanel;
    }

    @Override
    public void verifyInput()
    {
        chanceInput.setText(chanceInput.getText().replaceAll("[^0-9]*", ""));
        if (chanceInput.getText().length() == 0)
        {
            chanceInput.setText("0");
        }

        damageInput.setText(damageInput.getText().replaceAll("[^0-9\\.]*", ""));
        if (damageInput.getText().length() > 0)
        {
            if (damageInput.getText().matches("\\.+"))
            {
                damageInput.setText("0.0");
            }
            else
            {
                try
                {
                    Pattern regex = Pattern.compile("[0-9]+(\\.[0-9]+)?");
                    Matcher regexMatcher = regex.matcher(damageInput.getText());
                    regexMatcher.find();
                    damageInput.setText(regexMatcher.group());
                }
                catch (PatternSyntaxException ignored)
                {
                    damageInput.setText("0.0");
                }
            }
        }
        else
        {
            damageInput.setText("0.0");
        }
    }

    @Override
    public CompoundTag save()
    {
        compoundTag.getValue().put("addset_chance", new StringTag("addset_chance", addChanceRadioButton.isSelected() ? "add" : "set"));
        compoundTag.getValue().put("chance", new IntTag("chance", Integer.parseInt(chanceInput.getText())));

        compoundTag.getValue().put("addset_damage", new StringTag("addset_damage", addDamageRadioButton.isSelected() ? "add" : "set"));
        compoundTag.getValue().put("damage_double", new DoubleTag("damage_double", Double.parseDouble(damageInput.getText())));

        return compoundTag;
    }

    @Override
    public void load(CompoundTag compoundTag)
    {
        if (!compoundTag.getValue().containsKey("addset_chance") || ((StringTag) compoundTag.getValue().get("addset_chance")).getValue().equals("add"))
        {
            addChanceRadioButton.setSelected(true);
        }
        else
        {
            setChanceRadioButton.setSelected(true);
        }
        if (compoundTag.getValue().containsKey("chance"))
        {
            chanceInput.setText("" + ((IntTag) compoundTag.getValue().get("chance")).getValue());
        }

        if (!compoundTag.getValue().containsKey("addset_damage") || ((StringTag) compoundTag.getValue().get("addset_damage")).getValue().equals("add"))
        {
            addDamageRadioButton.setSelected(true);
        }
        else
        {
            setDamageRadioButton.setSelected(true);
        }
        if (compoundTag.getValue().containsKey("damage"))
        {
            compoundTag.getValue().put("damage_double", new DoubleTag("damage_double", ((IntTag) compoundTag.getValue().get("damage")).getValue()));
            compoundTag.getValue().remove("damage");
        }
        if (compoundTag.getValue().containsKey("damage_double"))
        {
            damageInput.setText("" + ((DoubleTag) compoundTag.getValue().get("damage_double")).getValue());
        }
    }
}