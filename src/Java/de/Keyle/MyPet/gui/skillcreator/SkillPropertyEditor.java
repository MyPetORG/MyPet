package de.Keyle.MyPet.gui.skillcreator;

import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.gui.GuiMain;
import de.Keyle.MyPet.skill.MyPetSkillTreeSkill;
import de.Keyle.MyPet.skill.SkillProperties;
import de.Keyle.MyPet.skill.SkillProperties.NBTdatatypes;
import de.Keyle.MyPet.util.MyPetUtil;
import net.minecraft.server.v1_4_R1.NBTTagCompound;

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

    private MyPetSkillTreeSkill skill;

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
                        NBTTagCompound tagCompound = skill.getProperties();
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
                                        tagCompound.setShort(name, Short.parseShort(value));
                                    }
                                    break;
                                case Int:
                                    if (MyPetUtil.isInt(value))
                                    {
                                        tagCompound.setInt(name, Integer.parseInt(value));
                                    }
                                    break;
                                case Long:
                                    if (MyPetUtil.isLong(value))
                                    {
                                        tagCompound.setLong(name, Long.parseLong(value));
                                    }
                                    break;
                                case Float:
                                    if (MyPetUtil.isFloat(value))
                                    {
                                        tagCompound.setFloat(name, Float.parseFloat(value));
                                    }
                                    break;
                                case Double:
                                    if (MyPetUtil.isDouble(value))
                                    {
                                        tagCompound.setDouble(name, Double.parseDouble(value));
                                    }
                                    break;
                                case Byte:
                                    if (MyPetUtil.isByte(value))
                                    {
                                        tagCompound.setByte(name, Byte.parseByte(value));
                                    }
                                    break;
                                case Boolean:
                                    if (value == null || value.equalsIgnoreCase("") || value.equalsIgnoreCase("off"))
                                    {
                                        tagCompound.setBoolean(name, false);
                                    }
                                    else if (value.equalsIgnoreCase("on"))
                                    {
                                        tagCompound.setBoolean(name, true);
                                    }
                                    break;
                                case String:
                                    tagCompound.setString(name, value);
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
            skillPropertyEditorFrame = new JFrame("Skill Properties - MyPet " + MyPetPlugin.MyPetVersion);
        }
        return skillPropertyEditorFrame;
    }

    public boolean setHTML(MyPetSkillTreeSkill skill)
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

        for (int i = 0 ; i < splittedParameters.length ; i++)
        {
            if (splittedParameters[i].contains("="))
            {
                String[] parameters = splittedParameters[i].split("=", 2);
                parameterMap.put(parameters[0], parameters[1]);
            }
        }
        return parameterMap;
    }
}
