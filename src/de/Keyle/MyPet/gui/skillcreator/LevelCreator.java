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

import de.Keyle.MyPet.gui.skillcreator.MyPetSkillTreeConfig.MyPetSkillTree;
import de.Keyle.MyPet.skill.MyPetSkillTreeLevel;
import de.Keyle.MyPet.skill.MyPetSkillTreeMobType;
import de.Keyle.MyPet.skill.MyPetSkillTreeSkill;
import de.Keyle.MyPet.util.MyPetUtil;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Collections;
import java.util.Comparator;

public class LevelCreator
{
    private JTree skillTreeTree;
    private JLabel skillTreeNameLabel;
    private JButton deleteLevelSkillButton;
    private JButton addLevelButton;
    private JButton addSkillButton;
    private JComboBox inheritanceComboBox;
    private JCheckBox inheritanceCheckBox;
    private JPanel levelCreatorPanel;
    private JButton backButton;
    private JLabel mobTypeLabel;
    private JFrame levelCreatorFrame;
    private JFrame parentFrame;
    private JButton saveButton;

    DefaultTreeModel skillTreeTreeModel;
    DefaultComboBoxModel inheritanceComboBoxModel;

    MyPetSkillTree skillTree;
    MyPetSkillTreeMobType skillTreeMobType;

    public LevelCreator(final JFrame parentFrame, final JButton saveButton)
    {
        this.parentFrame = parentFrame;
        this.saveButton = saveButton;
        addLevelButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                String response = JOptionPane.showInputDialog(null, "Enter the number for the new level.", "Create new Level", JOptionPane.QUESTION_MESSAGE);
                if (response != null)
                {
                    if (MyPetUtil.isInt(response))
                    {
                        int newLevel = Integer.parseInt(response);
                        for (int i = 0 ; i < skillTreeTreeModel.getChildCount(skillTreeTreeModel.getRoot()) ; i++)
                        {
                            if (MyPetUtil.isInt(((DefaultMutableTreeNode) skillTreeTreeModel.getRoot()).getChildAt(i).toString()))
                            {
                                int level = Integer.parseInt(((DefaultMutableTreeNode) skillTreeTreeModel.getRoot()).getChildAt(i).toString());
                                if (newLevel == level)
                                {
                                    JOptionPane.showMessageDialog(null, response + " already exists!", "Create new Level", JOptionPane.ERROR_MESSAGE);
                                    return;
                                }
                                else if (newLevel < 1)
                                {
                                    JOptionPane.showMessageDialog(null, response + " is smaller than 1!", "Create new Level", JOptionPane.ERROR_MESSAGE);
                                    return;
                                }
                            }
                        }
                        DefaultMutableTreeNode levelNode = new DefaultMutableTreeNode(newLevel);
                        skillTree.addLevel(newLevel);

                        ((SortedDefaultMutableTreeNode) skillTreeTreeModel.getRoot()).add(levelNode);
                        skillTreeTree.updateUI();
                        saveButton.setEnabled(true);
                    }
                    else
                    {
                        JOptionPane.showMessageDialog(null, response + " is not a number!", "Create new Level", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });
        addSkillButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                int level;
                if (skillTreeTree.getSelectionPath().getPath().length == 2 || skillTreeTree.getSelectionPath().getPath().length == 3)
                {
                    if (MyPetUtil.isInt(skillTreeTree.getSelectionPath().getPathComponent(1).toString()))
                    {
                        level = Integer.parseInt(skillTreeTree.getSelectionPath().getPathComponent(1).toString());
                    }
                    else
                    {
                        return;
                    }
                }
                else
                {
                    return;
                }

                String[] skillNames = new String[]{"Behavior", "Control", "Damage", "HP", "HP-Regeneration", "Inventory", "Pickup", "Poison"};
                String choosenSkill = (String) JOptionPane.showInputDialog(null, "Please select the skill you want to add to level " + level + '.', "", JOptionPane.QUESTION_MESSAGE, null, skillNames, "");
                if (choosenSkill != null)
                {
                    MyPetSkillTreeSkill skill = new MyPetSkillTreeSkill(choosenSkill);
                    skillTree.addSkillToLevel(level, skill);
                    DefaultMutableTreeNode skillNode = new DefaultMutableTreeNode(choosenSkill);
                    ((DefaultMutableTreeNode) skillTreeTree.getSelectionPath().getPathComponent(1)).add(skillNode);
                    skillTreeTree.expandPath(skillTreeTree.getSelectionPath());
                    skillTreeTree.updateUI();
                    saveButton.setEnabled(true);
                }
            }
        });
        deleteLevelSkillButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {

                if (skillTreeTree.getSelectionPath().getPath().length == 2)
                {
                    if (MyPetUtil.isInt(skillTreeTree.getSelectionPath().getLastPathComponent().toString()))
                    {
                        int level = Integer.parseInt(skillTreeTree.getSelectionPath().getLastPathComponent().toString());
                        skillTree.removeLevel(level);
                    }
                    else
                    {
                        return;
                    }
                }
                else if (skillTreeTree.getSelectionPath().getPath().length == 3)
                {
                    if (MyPetUtil.isInt(skillTreeTree.getSelectionPath().getPathComponent(1).toString()))
                    {
                        int level = Integer.parseInt(skillTreeTree.getSelectionPath().getPathComponent(1).toString());
                        int index = ((DefaultMutableTreeNode) skillTreeTree.getSelectionPath().getPathComponent(1)).getIndex(((DefaultMutableTreeNode) skillTreeTree.getSelectionPath().getPathComponent(2)));
                        skillTree.getLevel(level).removeSkill(index);
                    }
                    else
                    {
                        return;
                    }
                }
                ((DefaultMutableTreeNode) skillTreeTree.getSelectionPath().getLastPathComponent()).removeFromParent();
                skillTreeTree.updateUI();
                addSkillButton.setEnabled(false);
                deleteLevelSkillButton.setEnabled(false);
                saveButton.setEnabled(true);
            }
        });
        skillTreeTree.addTreeSelectionListener(new TreeSelectionListener()
        {
            public void valueChanged(TreeSelectionEvent e)
            {
                if (e.getPath().getPath().length == 1)
                {
                    addSkillButton.setEnabled(false);
                    deleteLevelSkillButton.setEnabled(false);
                }
                else if (e.getPath().getPath().length == 2)
                {
                    addSkillButton.setEnabled(true);
                    deleteLevelSkillButton.setEnabled(true);
                }
                else if (e.getPath().getPath().length == 3)
                {
                    addSkillButton.setEnabled(true);
                    deleteLevelSkillButton.setEnabled(true);
                }
            }

        });
        inheritanceCheckBox.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                if (inheritanceCheckBox.isSelected())
                {
                    inheritanceComboBox.setEnabled(true);
                    skillTree.setInheritance(inheritanceComboBox.getSelectedItem().toString());
                }
                else
                {
                    inheritanceComboBox.setEnabled(false);
                    skillTree.setInheritance(null);
                }
                saveButton.setEnabled(true);
            }
        });
        backButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                parentFrame.setEnabled(true);
                levelCreatorFrame.setVisible(false);
            }
        });
        inheritanceComboBox.addItemListener(new ItemListener()
        {
            public void itemStateChanged(ItemEvent e)
            {
                if (e.getStateChange() == ItemEvent.SELECTED && inheritanceCheckBox.isSelected())
                {
                    skillTree.setInheritance(e.getItem().toString());
                    saveButton.setEnabled(true);
                }
            }
        });
    }

    public JPanel getMainPanel()
    {
        return levelCreatorPanel;
    }

    public JFrame getFrame()
    {
        if (levelCreatorFrame == null)
        {
            levelCreatorFrame = new JFrame("LevelCreator");
        }
        return levelCreatorFrame;
    }

    public void setSkillTree(MyPetSkillTree skillTree, MyPetSkillTreeMobType skillTreeMobType)
    {
        this.skillTree = skillTree;
        this.skillTreeMobType = skillTreeMobType;

        mobTypeLabel.setText(skillTreeMobType.getMobTypeName());

        this.inheritanceComboBoxModel.removeAllElements();

        inheritanceCheckBox.setSelected(false);
        if (skillTreeMobType.getSkillTreeNames().size() == 1)
        {
            inheritanceCheckBox.setEnabled(false);
            inheritanceComboBox.setEnabled(false);
        }
        else
        {
            inheritanceCheckBox.setEnabled(true);
            for (String skillTreeName : skillTreeMobType.getSkillTreeNames())
            {
                if (!skillTreeName.equals(skillTree.getName()))
                {
                    inheritanceComboBoxModel.addElement(skillTreeName);
                }
            }
            if (skillTree.getInheritance() != null && skillTreeMobType.getSkillTreeNames().contains(skillTree.getInheritance()))
            {
                inheritanceCheckBox.setSelected(true);
                inheritanceComboBox.setEnabled(true);
                this.inheritanceComboBoxModel.setSelectedItem(skillTree.getInheritance());
            }
            else
            {
                inheritanceComboBox.setEnabled(false);
            }
        }

        skillTreeNameLabel.setText("Skilltree: " + skillTree.getName());
        SortedDefaultMutableTreeNode rootNode = new SortedDefaultMutableTreeNode(skillTree.getName());
        skillTreeTreeModel.setRoot(rootNode);

        for (MyPetSkillTreeLevel level : skillTree.getLevelList())
        {
            DefaultMutableTreeNode levelNode = new DefaultMutableTreeNode(level.getLevel());
            rootNode.add(levelNode);
            for (MyPetSkillTreeSkill skill : level.getSkills())
            {
                DefaultMutableTreeNode skillNode = new DefaultMutableTreeNode(skill.getName());
                levelNode.add(skillNode);
            }
        }
        skillTreeTree.updateUI();
        skillTreeTree.setSelectionPath(new TreePath(rootNode));
    }

    private void createUIComponents()
    {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("");
        skillTreeTreeModel = new DefaultTreeModel(root);
        skillTreeTree = new JTree(skillTreeTreeModel);

        inheritanceComboBoxModel = new DefaultComboBoxModel();
        inheritanceComboBox = new JComboBox(inheritanceComboBoxModel);
    }

    private class SortedDefaultMutableTreeNode extends DefaultMutableTreeNode
    {
        public SortedDefaultMutableTreeNode(Object userObject)
        {
            super(userObject);
        }

        public void add(DefaultMutableTreeNode newChild)
        {
            super.add(newChild);
            Collections.sort(this.children, nodeComparator);
        }

        protected Comparator nodeComparator = new Comparator()
        {
            public int compare(Object o1, Object o2)
            {
                if (MyPetUtil.isInt(o1.toString()) && MyPetUtil.isInt(o2.toString()))
                {
                    int n1 = Integer.parseInt(o1.toString());
                    int n2 = Integer.parseInt(o2.toString());
                    if (n1 < n2)
                    {
                        return -1;
                    }
                    else if (n1 == n2)
                    {
                        return 0;
                    }
                    if (n1 > n2)
                    {
                        return 1;
                    }
                }
                return o1.toString().compareToIgnoreCase(o2.toString());
            }

            public boolean equals(Object obj)
            {
                return false;
            }
        };
    }
}
