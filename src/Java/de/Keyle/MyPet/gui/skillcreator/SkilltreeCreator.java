/*
 * Copyright (C) 2011-2012 Keyle
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
import de.Keyle.MyPet.gui.skillcreator.MyPetSkillTreeConfig.MyPetSkillTree;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.event.*;

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
                    for (String skillTreeName : MyPetSkillTreeConfig.getMobType(e.getItem().toString()).getSkillTreeNames())
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
                MyPetSkillTreeConfig.getMobType(SkilltreeCreator.this.mobTypeComboBox.getSelectedItem().toString()).moveSkillTreeUp(skilltreeList.getSelectedValue().toString());
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
                MyPetSkillTreeConfig.getMobType(SkilltreeCreator.this.mobTypeComboBox.getSelectedItem().toString()).moveSkillTreeDown(skilltreeList.getSelectedValue().toString());
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
                            MyPetSkillTree skillTree = new MyPetSkillTree(response);
                            MyPetSkillTreeConfig.getMobType(SkilltreeCreator.this.mobTypeComboBox.getSelectedItem().toString()).addSkillTree(skillTree);
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
                MyPetSkillTreeConfig.getMobType(SkilltreeCreator.this.mobTypeComboBox.getSelectedItem().toString()).removeSkillTree(skilltreeList.getSelectedValue().toString());
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

                    GuiMain.levelCreator.setSkillTree((MyPetSkillTree) MyPetSkillTreeConfig.getMobType(SkilltreeCreator.this.mobTypeComboBox.getSelectedItem().toString()).getSkillTree(skilltreeList.getSelectedValue().toString()), MyPetSkillTreeConfig.getMobType(SkilltreeCreator.this.mobTypeComboBox.getSelectedItem().toString()));
                    GuiMain.levelCreator.getFrame().setVisible(true);
                    skilltreeCreatorFrame.setEnabled(false);
                }
            }
        });
        saveButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                MyPetSkillTreeConfig.saveSkillTrees();
            }
        });
        skilltreeList.addKeyListener(new KeyAdapter()
        {
            public void keyPressed(KeyEvent e)
            {
                switch (e.getKeyCode())
                {
                    case KeyEvent.VK_ENTER:
                        GuiMain.levelCreator.setSkillTree((MyPetSkillTree) MyPetSkillTreeConfig.getMobType(SkilltreeCreator.this.mobTypeComboBox.getSelectedItem().toString()).getSkillTree(skilltreeList.getSelectedValue().toString()), MyPetSkillTreeConfig.getMobType(SkilltreeCreator.this.mobTypeComboBox.getSelectedItem().toString()));
                        GuiMain.levelCreator.getFrame().setVisible(true);
                        skilltreeCreatorFrame.setEnabled(false);
                        break;
                    case KeyEvent.VK_DELETE:
                        int index = skilltreeList.getSelectedIndex();
                        MyPetSkillTreeConfig.getMobType(SkilltreeCreator.this.mobTypeComboBox.getSelectedItem().toString()).removeSkillTree(skilltreeList.getSelectedValue().toString());
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
