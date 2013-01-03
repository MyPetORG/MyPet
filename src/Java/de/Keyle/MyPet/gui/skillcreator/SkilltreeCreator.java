/*
 * Copyright (C) 2011-2013 Keyle
 *
 * This file is part of MyPet
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
 * along with MyPet. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.gui.skillcreator;

import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.gui.GuiMain;
import de.Keyle.MyPet.skill.MyPetSkillTree;
import de.Keyle.MyPet.skill.MyPetSkillTreeLoader;
import de.Keyle.MyPet.skill.MyPetSkillTreeMobType;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.event.*;
import java.io.File;
import java.util.List;

public class SkilltreeCreator
{
    JComboBox mobTypeComboBox;
    JButton addSkilltreeButton;
    JButton deleteSkilltreeButton;
    JList skilltreeList;
    JButton skilltreeDownButton;
    JButton skilltreeUpButton;
    JPanel skilltreeCreatorPanel;
    JButton saveButton;
    JFrame skilltreeCreatorFrame;

    private DefaultListModel skillTreeListModel;

    public SkilltreeCreator()
    {
        this.mobTypeComboBox.addItemListener(new ItemListener()
        {
            public void itemStateChanged(ItemEvent e)
            {
                if (e.getStateChange() == ItemEvent.SELECTED)
                {
                    if (SkilltreeCreator.this.mobTypeComboBox.getItemAt(0).equals(""))
                    {
                        SkilltreeCreator.this.mobTypeComboBox.removeItemAt(0);
                        addSkilltreeButton.setEnabled(true);
                    }

                    skillTreeListModel.removeAllElements();
                    for (String skillTreeName : MyPetSkillTreeMobType.getMobTypeByName(e.getItem().toString()).getSkillTreeNames())
                    {
                        skillTreeListModel.addElement(skillTreeName);
                    }
                    skilltreeList.setSelectedIndex(0);
                }

            }
        });
        skilltreeList.addListSelectionListener(new ListSelectionListener()
        {
            public void valueChanged(ListSelectionEvent e)
            {
                if (skillTreeListModel.getSize() == 0 || skillTreeListModel.getSize() == 1)
                {
                    skilltreeDownButton.setEnabled(false);
                    skilltreeUpButton.setEnabled(false);
                }
                else if (skilltreeList.getSelectedIndex() == skillTreeListModel.getSize() - 1)
                {
                    skilltreeDownButton.setEnabled(false);
                    skilltreeUpButton.setEnabled(true);
                    deleteSkilltreeButton.setEnabled(true);
                    if (skilltreeDownButton.hasFocus())
                    {
                        skilltreeUpButton.requestFocus();
                    }
                }
                else if (skilltreeList.getSelectedIndex() == 0)
                {
                    skilltreeDownButton.setEnabled(true);
                    skilltreeUpButton.setEnabled(false);
                    deleteSkilltreeButton.setEnabled(true);
                    if (skilltreeUpButton.hasFocus())
                    {
                        skilltreeDownButton.requestFocus();
                    }
                }
                else
                {
                    skilltreeDownButton.setEnabled(true);
                    skilltreeUpButton.setEnabled(true);
                    deleteSkilltreeButton.setEnabled(true);
                }
            }
        });
        skilltreeUpButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                MyPetSkillTreeMobType.getMobTypeByName(SkilltreeCreator.this.mobTypeComboBox.getSelectedItem().toString()).moveSkillTreeUp(skilltreeList.getSelectedValue().toString());
                String skillTreeName = (String) skillTreeListModel.get(skilltreeList.getSelectedIndex() - 1);
                skillTreeListModel.set(skilltreeList.getSelectedIndex() - 1, skillTreeListModel.get(skilltreeList.getSelectedIndex()));
                skillTreeListModel.set(skilltreeList.getSelectedIndex(), skillTreeName);
                skilltreeList.setSelectedIndex(skilltreeList.getSelectedIndex() - 1);
            }
        });
        skilltreeDownButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                MyPetSkillTreeMobType.getMobTypeByName(SkilltreeCreator.this.mobTypeComboBox.getSelectedItem().toString()).moveSkillTreeDown(skilltreeList.getSelectedValue().toString());
                String skillTreeName = (String) skillTreeListModel.get(skilltreeList.getSelectedIndex() + 1);
                skillTreeListModel.set(skilltreeList.getSelectedIndex() + 1, skillTreeListModel.get(skilltreeList.getSelectedIndex()));
                skillTreeListModel.set(skilltreeList.getSelectedIndex(), skillTreeName);
                skilltreeList.setSelectedIndex(skilltreeList.getSelectedIndex() + 1);
            }
        });
        addSkilltreeButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                String response = JOptionPane.showInputDialog(null, "Enter the name of the new skilltree.", "Create new Skilltree", JOptionPane.QUESTION_MESSAGE);
                if (response != null)
                {
                    if (response.matches("(?m)[\\w-]+"))
                    {
                        if (!skillTreeListModel.contains(response))
                        {
                            skillTreeListModel.addElement(response);
                            MyPetSkillTreeMobType mobType = MyPetSkillTreeMobType.getMobTypeByName(SkilltreeCreator.this.mobTypeComboBox.getSelectedItem().toString());
                            Short place = mobType.getNextPlace();
                            MyPetSkillTree skillTree = new MyPetSkillTree(response, place);
                            mobType.addSkillTree(skillTree);
                            skilltreeList.setSelectedIndex(skillTreeListModel.getSize() - 1);
                            deleteSkilltreeButton.setEnabled(true);
                        }
                        else
                        {
                            JOptionPane.showMessageDialog(null, "There is already a skilltree with this name!", "Create new Skilltree", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                    else
                    {
                        JOptionPane.showMessageDialog(null, "This is not a valid skilltree name!\n\na-z\nA-Z\n0-9\n_ -", "Create new Skilltree", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });
        deleteSkilltreeButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                int index = skilltreeList.getSelectedIndex();
                MyPetSkillTreeMobType.getMobTypeByName(SkilltreeCreator.this.mobTypeComboBox.getSelectedItem().toString()).removeSkillTree(skilltreeList.getSelectedValue().toString());
                skillTreeListModel.remove(skilltreeList.getSelectedIndex());
                if (index == skillTreeListModel.size())
                {
                    skilltreeList.setSelectedIndex(index - 1);
                }
                else
                {
                    skilltreeList.setSelectedIndex(index);
                }
                if (skillTreeListModel.size() == 0)
                {
                    deleteSkilltreeButton.setEnabled(false);
                }
            }
        });
        skilltreeList.addMouseListener(new MouseAdapter()
        {
            public void mouseClicked(MouseEvent evt)
            {
                if (evt.getClickCount() == 2)
                {

                    GuiMain.levelCreator.setSkillTree(MyPetSkillTreeMobType.getMobTypeByName(SkilltreeCreator.this.mobTypeComboBox.getSelectedItem().toString()).getSkillTree(skilltreeList.getSelectedValue().toString()), MyPetSkillTreeMobType.getMobTypeByName(SkilltreeCreator.this.mobTypeComboBox.getSelectedItem().toString()));
                    GuiMain.levelCreator.getFrame().setVisible(true);
                    skilltreeCreatorFrame.setEnabled(false);
                }
            }
        });
        saveButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                List<String> savedPetTypes = MyPetSkillTreeLoader.saveSkillTrees(GuiMain.configPath + "skilltrees");
                String savedPetsString = "";
                for (String petType : savedPetTypes)
                {
                    savedPetsString += "\n   " + petType.toLowerCase() + ".st";
                }
                JOptionPane.showMessageDialog(null, "Saved to:\n" + GuiMain.configPath + File.separator + savedPetsString, "Saved following configs", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        skilltreeList.addKeyListener(new KeyAdapter()
        {
            public void keyPressed(KeyEvent e)
            {
                switch (e.getKeyCode())
                {
                    case KeyEvent.VK_ENTER:
                        GuiMain.levelCreator.setSkillTree(MyPetSkillTreeMobType.getMobTypeByName(SkilltreeCreator.this.mobTypeComboBox.getSelectedItem().toString()).getSkillTree(skilltreeList.getSelectedValue().toString()), MyPetSkillTreeMobType.getMobTypeByName(SkilltreeCreator.this.mobTypeComboBox.getSelectedItem().toString()));
                        GuiMain.levelCreator.getFrame().setVisible(true);
                        skilltreeCreatorFrame.setEnabled(false);
                        break;
                    case KeyEvent.VK_DELETE:
                        int index = skilltreeList.getSelectedIndex();
                        MyPetSkillTreeMobType.getMobTypeByName(SkilltreeCreator.this.mobTypeComboBox.getSelectedItem().toString()).removeSkillTree(skilltreeList.getSelectedValue().toString());
                        skillTreeListModel.remove(skilltreeList.getSelectedIndex());
                        if (index == skillTreeListModel.size())
                        {
                            skilltreeList.setSelectedIndex(index - 1);
                        }
                        else
                        {
                            skilltreeList.setSelectedIndex(index);
                        }
                        if (skillTreeListModel.size() == 0)
                        {
                            deleteSkilltreeButton.setEnabled(false);
                        }
                        break;
                }
            }
        });
    }

    public JPanel getMainPanel()
    {
        return skilltreeCreatorPanel;
    }

    public JFrame getFrame()
    {
        if (skilltreeCreatorFrame == null)
        {
            skilltreeCreatorFrame = new JFrame("SkilltreeCreator - MyPet " + MyPetPlugin.MyPetVersion);
        }
        return skilltreeCreatorFrame;
    }

    private void createUIComponents()
    {
        skillTreeListModel = new DefaultListModel();
        skilltreeList = new JList(skillTreeListModel);
    }
}
