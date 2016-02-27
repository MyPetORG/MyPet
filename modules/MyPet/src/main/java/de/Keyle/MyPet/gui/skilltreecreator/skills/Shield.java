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

import de.keyle.knbt.TagCompound;
import de.keyle.knbt.TagInt;
import de.keyle.knbt.TagString;

import javax.swing.*;

public class Shield implements SkillPropertiesPanel {
    private JPanel mainPanel;
    private JTextField chanceInput;
    private JRadioButton addChanceRadioButton;
    private JRadioButton setChanceRadioButton;
    private JTextField redirectionInput;
    private JRadioButton addRedirectionRadioButton;
    private JRadioButton setRedirectionRadioButton;

    @Override
    public JPanel getMainPanel() {
        return mainPanel;
    }

    @Override
    public void verifyInput() {
        chanceInput.setText(chanceInput.getText().replaceAll("[^0-9]*", ""));
        if (chanceInput.getText().length() == 0) {
            chanceInput.setText("0");
        }

        redirectionInput.setText(redirectionInput.getText().replaceAll("[^0-9]*", ""));
        if (redirectionInput.getText().length() == 0) {
            redirectionInput.setText("0");
        }
    }

    @Override
    public void resetInput() {
        chanceInput.setText("0");
        redirectionInput.setText("0");
        addChanceRadioButton.setSelected(true);
        addRedirectionRadioButton.setSelected(true);
    }

    @Override
    public void save(TagCompound tagCompound) {
        tagCompound.getCompoundData().put("addset_chance", new TagString(addChanceRadioButton.isSelected() ? "add" : "set"));
        tagCompound.getCompoundData().put("chance", new TagInt(Integer.parseInt(chanceInput.getText())));

        tagCompound.getCompoundData().put("addset_redirection", new TagString(addRedirectionRadioButton.isSelected() ? "add" : "set"));
        tagCompound.getCompoundData().put("redirection", new TagInt(Integer.parseInt(redirectionInput.getText())));
    }

    @Override
    public void load(TagCompound TagCompound) {
        if (!TagCompound.getCompoundData().containsKey("addset_chance") || TagCompound.getAs("addset_chance", TagString.class).getStringData().equals("add")) {
            addChanceRadioButton.setSelected(true);
        } else {
            setChanceRadioButton.setSelected(true);
        }
        if (TagCompound.getCompoundData().containsKey("chance")) {
            chanceInput.setText("" + TagCompound.getAs("chance", TagInt.class).getIntData());
        }

        if (!TagCompound.getCompoundData().containsKey("addset_redirection") || TagCompound.getAs("addset_redirection", TagString.class).getStringData().equals("add")) {
            addRedirectionRadioButton.setSelected(true);
        } else {
            setRedirectionRadioButton.setSelected(true);
        }
        if (TagCompound.getCompoundData().containsKey("redirection")) {
            redirectionInput.setText("" + TagCompound.getAs("redirection", TagInt.class).getIntData());
        }
    }
}