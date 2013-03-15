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

package de.Keyle.MyPet.gui.skillcreator;

import de.Keyle.MyPet.gui.GuiMain;
import de.Keyle.MyPet.skill.SkillProperties;
import de.Keyle.MyPet.skill.SkillProperties.NBTdatatypes;
import de.Keyle.MyPet.skill.skills.info.ISkillInfo;
import de.Keyle.MyPet.util.MyPetUtil;
import de.Keyle.MyPet.util.MyPetVersion;
import org.spout.nbt.*;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.Document;
import javax.swing.text.html.FormSubmitEvent;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

public class SkillPropertyEditor
{
    protected JTextPane propertyTextPane;
    protected JPanel skillPropertyEditorPanel;
    protected JButton cancelButton;
    protected JFrame skillPropertyEditorFrame;

    private ISkillInfo skill;

    public SkillPropertyEditor()
    {
        propertyTextPane.addHyperlinkListener(new HyperlinkListener()
        {
            public void hyperlinkUpdate(HyperlinkEvent e)
            {
                if (e instanceof FormSubmitEvent)
                {
                    SkillProperties ph = skill.getClass().getAnnotation(SkillProperties.class);
                    if (ph != null)
                    {
                        if (skill == null)
                        {
                            return;
                        }
                        CompoundTag tagCompound = skill.getProperties();
                        System.out.println(((FormSubmitEvent) e).getData());
                        Map<String, String> parameterMap = seperateParameter(((FormSubmitEvent) e).getData());
                        for (int i = 0 ; i < ph.parameterNames().length ; i++)
                        {
                            if (i >= ph.parameterTypes().length)
                            {
                                break;
                            }
                            NBTdatatypes type = ph.parameterTypes()[i];
                            String name = ph.parameterNames()[i];
                            String value = parameterMap.get(name);
                            switch (type)
                            {
                                case Short:
                                    if (MyPetUtil.isShort(value))
                                    {
                                        tagCompound.getValue().put(name, new ShortTag(name, Short.parseShort(value)));
                                    }
                                    break;
                                case Int:
                                    if (MyPetUtil.isInt(value))
                                    {
                                        tagCompound.getValue().put(name, new IntTag(name, Integer.parseInt(value)));
                                    }
                                    break;
                                case Long:
                                    if (MyPetUtil.isLong(value))
                                    {
                                        tagCompound.getValue().put(name, new LongTag(name, Long.parseLong(value)));
                                    }
                                    break;
                                case Float:
                                    if (MyPetUtil.isFloat(value))
                                    {
                                        tagCompound.getValue().put(name, new FloatTag(name, Float.parseFloat(value)));
                                    }
                                    break;
                                case Double:
                                    if (MyPetUtil.isDouble(value))
                                    {
                                        tagCompound.getValue().put(name, new DoubleTag(name, Double.parseDouble(value)));
                                    }
                                    break;
                                case Byte:
                                    if (MyPetUtil.isByte(value))
                                    {
                                        tagCompound.getValue().put(name, new ByteTag(name, Byte.parseByte(value)));
                                    }
                                    break;
                                case Boolean:
                                    if (value == null || value.equalsIgnoreCase("") || value.equalsIgnoreCase("off"))
                                    {
                                        tagCompound.getValue().put(name, new ByteTag(name, false));
                                    }
                                    else if (value.equalsIgnoreCase("on"))
                                    {
                                        tagCompound.getValue().put(name, new ByteTag(name, true));
                                    }
                                    break;
                                case String:
                                    tagCompound.getValue().put(name, new StringTag(name, value));
                                    break;
                            }
                        }
                    }
                }
                propertyTextPane.setText("");
                GuiMain.levelCreator.getFrame().setEnabled(true);
                skillPropertyEditorFrame.setVisible(false);
            }
        });
        cancelButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                propertyTextPane.setText("");
                GuiMain.levelCreator.getFrame().setEnabled(true);
                skillPropertyEditorFrame.setVisible(false);
            }
        });
        HTMLEditorKit kit = (HTMLEditorKit) propertyTextPane.getEditorKit();
        kit.setAutoFormSubmission(false);
        Document doc = kit.createDefaultDocument();
        propertyTextPane.setDocument(doc);
    }

    private void createUIComponents()
    {
    }

    public JPanel getMainPanel()
    {
        return skillPropertyEditorPanel;
    }

    public JFrame getFrame()
    {
        if (skillPropertyEditorFrame == null)
        {
            skillPropertyEditorFrame = new JFrame("Skill Properties - MyPet " + MyPetVersion.getMyPetVersion());
        }
        return skillPropertyEditorFrame;
    }

    public boolean setHTML(ISkillInfo skill)
    {
        SkillProperties ph = skill.getClass().getAnnotation(SkillProperties.class);
        if (ph != null)
        {
            propertyTextPane.setText(skill.getHtml());
        }
        this.skill = skill;
        return false;
    }

    public Map<String, String> seperateParameter(String parameterString)
    {
        Map<String, String> parameterMap = new HashMap<String, String>();

        String[] splittedParameters = parameterString.split("&");

        for (String splittedParameter : splittedParameters)
        {
            if (splittedParameter.contains("="))
            {
                String[] parameters = splittedParameter.split("=", 2);
                parameterMap.put(parameters[0], parameters[1]);
            }
        }
        return parameterMap;
    }
}
