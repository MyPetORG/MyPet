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

public class HealthRegeneration implements SkillPropertiesPanel
{
    private JTextField healthInput;
    private JRadioButton addHealthRadioButton;
    private JRadioButton setHealthRadioButton;
    private JPanel mainPanel;
    private JTextField timeInput;
    private JRadioButton decreaseTimeRadioButton;
    private JRadioButton setTimeRadioButton;

    private CompoundTag compoundTag;

    public HealthRegeneration(CompoundTag compoundTag)
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
        timeInput.setText(timeInput.getText().replaceAll("[^0-9]*", ""));
        if (timeInput.getText().length() == 0)
        {
            timeInput.setText("0");
        }

        healthInput.setText(healthInput.getText().replaceAll("[^0-9\\.]*", ""));
        if (healthInput.getText().length() > 0)
        {
            if (healthInput.getText().matches("\\.+"))
            {
                healthInput.setText("0.0");
            }
            else
            {
                try
                {
                    Pattern regex = Pattern.compile("[0-9]+(\\.[0-9]+)?");
                    Matcher regexMatcher = regex.matcher(healthInput.getText());
                    regexMatcher.find();
                    healthInput.setText(regexMatcher.group());
                }
                catch (PatternSyntaxException ignored)
                {
                    healthInput.setText("0.0");
                }
            }
        }
        else
        {
            healthInput.setText("0.0");
        }
    }

    @Override
    public CompoundTag save()
    {
        compoundTag.getValue().put("addset_time", new StringTag("addset_time", decreaseTimeRadioButton.isSelected() ? "add" : "set"));
        compoundTag.getValue().put("time", new IntTag("time", Integer.parseInt(timeInput.getText())));

        compoundTag.getValue().put("addset_hp", new StringTag("addset_hp", addHealthRadioButton.isSelected() ? "add" : "set"));
        compoundTag.getValue().put("hp_double", new DoubleTag("hp_double", Double.parseDouble(healthInput.getText())));

        return compoundTag;
    }

    @Override
    public void load(CompoundTag compoundTag)
    {
        if (!compoundTag.getValue().containsKey("addset_hp") || ((StringTag) compoundTag.getValue().get("addset_hp")).getValue().equals("add"))
        {
            addHealthRadioButton.setSelected(true);
        }
        else
        {
            setHealthRadioButton.setSelected(true);
        }
        if (compoundTag.getValue().containsKey("hp"))
        {
            compoundTag.getValue().put("hp_double", new DoubleTag("hp_double", ((IntTag) compoundTag.getValue().get("hp")).getValue()));
            compoundTag.getValue().remove("hp");
        }
        if (compoundTag.getValue().containsKey("hp_double"))
        {
            healthInput.setText("" + ((DoubleTag) compoundTag.getValue().get("hp_double")).getValue());
        }

        if (!compoundTag.getValue().containsKey("addset_time") || ((StringTag) compoundTag.getValue().get("addset_time")).getValue().equals("add"))
        {
            decreaseTimeRadioButton.setSelected(true);
        }
        else
        {
            setTimeRadioButton.setSelected(true);
        }
        if (compoundTag.getValue().containsKey("time"))
        {
            timeInput.setText("" + ((IntTag) compoundTag.getValue().get("time")).getValue());
        }
    }
}