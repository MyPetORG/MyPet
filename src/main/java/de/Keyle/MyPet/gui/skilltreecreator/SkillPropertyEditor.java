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

package de.Keyle.MyPet.gui.skilltreecreator;

import com.intellij.uiDesigner.core.GridConstraints;
import de.Keyle.MyPet.gui.GuiMain;
import de.Keyle.MyPet.gui.skilltreecreator.skills.SkillPropertiesPanel;
import de.Keyle.MyPet.skill.skills.info.ISkillInfo;
import de.Keyle.MyPet.util.MyPetVersion;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

public class SkillPropertyEditor {
    protected JPanel propertyPanel;
    protected JPanel skillPropertyEditorPanel;
    protected JButton cancelButton;
    private JButton saveButton;
    protected JFrame skillPropertyEditorFrame;

    private ISkillInfo skill;
    private SkillPropertiesPanel skillPropertiesPanel;

    private GridConstraints constraints = new GridConstraints();

    public SkillPropertyEditor() {
        saveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                skillPropertiesPanel.verifyInput();
                skillPropertiesPanel.save();
                GuiMain.levelCreator.getFrame().setEnabled(true);
                skillPropertyEditorFrame.setVisible(false);
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                skillPropertiesPanel.load(skill.getProperties());
                GuiMain.levelCreator.getFrame().setEnabled(true);
                skillPropertyEditorFrame.setVisible(false);
            }
        });
    }

    public void setSkill(ISkillInfo skill) {
        this.skill = skill;
        propertyPanel.removeAll();
        skillPropertiesPanel = skill.getGuiPanel();
        propertyPanel.add(skillPropertiesPanel.getMainPanel(), constraints);
    }

    public JPanel getMainPanel() {
        return skillPropertyEditorPanel;
    }

    public JFrame getFrame() {
        if (skillPropertyEditorFrame == null) {
            skillPropertyEditorFrame = new JFrame("Skill Properties - MyPet " + MyPetVersion.getVersion());
        }
        return skillPropertyEditorFrame;
    }

    public Map<String, String> seperateParameter(String parameterString) {
        Map<String, String> parameterMap = new HashMap<>();

        String[] splittedParameters = parameterString.split("&");

        for (String splittedParameter : splittedParameters) {
            if (splittedParameter.contains("=")) {
                String[] parameters = splittedParameter.split("=", 2);
                parameterMap.put(parameters[0], parameters[1]);
            }
        }
        return parameterMap;
    }
}