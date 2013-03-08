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

import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.gui.GuiMain;
import de.Keyle.MyPet.skill.*;
import de.Keyle.MyPet.skill.skills.info.ISkillInfo;
import de.Keyle.MyPet.util.MyPetUtil;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.util.*;

public class LevelCreator
{
    JTree skillTreeTree;
    JLabel skillTreeNameLabel;
    JButton deleteLevelSkillButton;
    JButton addLevelButton;
    JButton addSkillButton;
    JComboBox inheritanceComboBox;
    JCheckBox inheritanceCheckBox;
    JPanel levelCreatorPanel;
    JButton backButton;
    JLabel mobTypeLabel;
    JTable skillCounterTabel;
    JLabel skilllevelLabel;
    JButton copyButton;
    JTextField permissionDisplayTextField;
    JCheckBox displayNameCheckbox;
    JCheckBox permissionCheckbox;
    JTextField displayNameTextField;
    JTextField permissionTextField;
    JFrame levelCreatorFrame;

    DefaultTableModel skillCounterTabelModel;
    DefaultTreeModel skillTreeTreeModel;
    DefaultComboBoxModel inheritanceComboBoxModel;

    MyPetSkillTree skillTree;
    MyPetSkillTreeMobType skillTreeMobType;

    String[] skillNames = new String[]{"Beacon", "Behavior", "Control", "Damage", "Fire", "HP", "HPregeneration", "Inventory", "Knockback", "Lightning", "Pickup", "Poison", "Ride", "Slow", "Thorns", "Wither"};

    public LevelCreator()
    {
        addLevelButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                String response = JOptionPane.showInputDialog(null, "Enter the number for the new level.", "Create new Level", JOptionPane.QUESTION_MESSAGE);
                if (response != null)
                {
                    if (MyPetUtil.isInt(response))
                    {
                        short newLevel = Short.parseShort(response);
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
                        skillTreeTree.expandPath(skillTreeTree.getSelectionPath());
                        skillTreeTree.updateUI();
                    }
                    else
                    {
                        JOptionPane.showMessageDialog(null, response + " is not a number!", "Create new Level", JOptionPane.ERROR_MESSAGE);
                    }
                }
                countSkillLevels();
            }
        });
        addSkillButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                short level;
                if (skillTreeTree.getSelectionPath().getPath().length == 2 || skillTreeTree.getSelectionPath().getPath().length == 3)
                {
                    if (MyPetUtil.isInt(skillTreeTree.getSelectionPath().getPathComponent(1).toString()))
                    {
                        level = Short.parseShort(skillTreeTree.getSelectionPath().getPathComponent(1).toString());
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
                String choosenSkill = (String) JOptionPane.showInputDialog(null, "Please select the skill you want to add to level " + level + '.', "", JOptionPane.QUESTION_MESSAGE, null, skillNames, "");
                if (choosenSkill != null)
                {
                    ISkillInfo skill = MyPetSkillsInfo.getNewSkillInfoInstance(choosenSkill);
                    skillTree.addSkillToLevel(level, skill);
                    SkillTreeSkillNode skillNode = new SkillTreeSkillNode(skill);
                    skill.setDefaultProperties();
                    ((DefaultMutableTreeNode) skillTreeTree.getSelectionPath().getPathComponent(1)).add(skillNode);
                    skillTreeTree.expandPath(skillTreeTree.getSelectionPath());
                    skillTreeTree.updateUI();
                }
                countSkillLevels();
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
                        short level = Short.parseShort(skillTreeTree.getSelectionPath().getLastPathComponent().toString());
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
                        short level = Short.parseShort(skillTreeTree.getSelectionPath().getPathComponent(1).toString());
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
                countSkillLevels();
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
                    skillCounterTabelModel.getDataVector().removeAllElements();
                    skillCounterTabel.updateUI();
                    skilllevelLabel.setText("Skilllevels for level: -");
                }
                else if (e.getPath().getPath().length == 2)
                {
                    addSkillButton.setEnabled(true);
                    deleteLevelSkillButton.setEnabled(true);
                    countSkillLevels();
                }
                else if (e.getPath().getPath().length == 3)
                {
                    addSkillButton.setEnabled(true);
                    deleteLevelSkillButton.setEnabled(true);
                    countSkillLevels();
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
            }
        });
        permissionCheckbox.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                if (permissionCheckbox.isSelected())
                {
                    permissionTextField.setEnabled(true);
                    if (!permissionTextField.getText().equalsIgnoreCase(""))
                    {
                        skillTree.setPermission(permissionTextField.getText());
                    }
                    else
                    {
                        skillTree.setPermission(null);
                    }
                }
                else
                {
                    permissionTextField.setEnabled(false);
                    skillTree.setPermission(null);
                }
                permissionDisplayTextField.setText("MyPet.custom.skilltree." + skillTree.getPermission());
            }
        });
        displayNameCheckbox.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                if (displayNameCheckbox.isSelected())
                {
                    displayNameTextField.setEnabled(true);
                    if (!displayNameTextField.getText().equalsIgnoreCase(""))
                    {
                        skillTree.setDisplayName(displayNameTextField.getText());
                    }
                    else
                    {
                        skillTree.setDisplayName(null);
                    }
                }
                else
                {
                    displayNameTextField.setEnabled(false);
                    skillTree.setDisplayName(null);
                }
            }
        });
        backButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                GuiMain.skilltreeCreator.getFrame().setEnabled(true);
                levelCreatorFrame.setVisible(false);
            }
        });
        inheritanceComboBox.addItemListener(new ItemListener()
        {
            public void itemStateChanged(ItemEvent e)
            {
                if (e.getStateChange() == ItemEvent.SELECTED && inheritanceCheckBox.isSelected())
                {
                    if (!skillTree.getInheritance().equals(e.getItem().toString()))
                    {
                        skillTree.setInheritance(e.getItem().toString());
                        countSkillLevels();
                    }
                }
            }
        });
        copyButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                StringSelection stringSelection = new StringSelection("MyPet.custom.skilltree." + skillTree.getName());
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(stringSelection, null);
            }
        });
        skillTreeTree.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent evt)
            {
                if (evt.getClickCount() == 2)
                {
                    if (skillTreeTree.getSelectionPath().getPath().length == 3)
                    {
                        if (skillTreeTree.getSelectionPath().getPathComponent(2) instanceof SkillTreeSkillNode)
                        {
                            ISkillInfo skill = ((SkillTreeSkillNode) skillTreeTree.getSelectionPath().getPathComponent(2)).getSkill();
                            if (skill.getClass().getAnnotation(SkillProperties.class) == null)
                            {
                                JOptionPane.showMessageDialog(null, skill.getName() + " has no options.", "Skill options", JOptionPane.INFORMATION_MESSAGE);
                                return;
                            }
                            if (MyPetSkills.isValidSkill(skill.getName()))
                            {
                                GuiMain.skillPropertyEditor.setHTML((ISkillInfo) skill);
                            }
                            GuiMain.skillPropertyEditor.getFrame().setVisible(true);
                            getFrame().setEnabled(false);
                        }
                    }
                }
            }
        });
        permissionTextField.addKeyListener(new KeyListener()
        {
            public void keyTyped(KeyEvent arg0)
            {
            }

            public void keyReleased(KeyEvent arg0)
            {
                permissionTextField.setText(permissionTextField.getText().replaceAll("[^a-zA-Z0-9]*", ""));
                if (permissionCheckbox.isSelected() && !skillTree.getPermission().equals(permissionTextField.getText()))
                {
                    if (!permissionTextField.getText().equalsIgnoreCase(""))
                    {
                        skillTree.setPermission(permissionTextField.getText());
                    }
                    else
                    {
                        skillTree.setPermission(null);
                    }
                    permissionDisplayTextField.setText("MyPet.custom.skilltree." + skillTree.getPermission());
                }
            }

            public void keyPressed(KeyEvent arg0)
            {
            }
        });
        displayNameTextField.addKeyListener(new KeyListener()
        {
            public void keyTyped(KeyEvent arg0)
            {
            }

            public void keyReleased(KeyEvent arg0)
            {
                if (displayNameCheckbox.isSelected() && !skillTree.getDisplayName().equals(displayNameTextField.getText()))
                {
                    if (!displayNameTextField.getText().equalsIgnoreCase(""))
                    {
                        skillTree.setDisplayName(displayNameTextField.getText());
                    }
                    else
                    {
                        skillTree.setDisplayName(null);
                    }
                }
            }

            public void keyPressed(KeyEvent arg0)
            {
            }
        });
    }

    private void countSkillLevels()
    {
        int level = 0;
        if (skillTreeTree.getSelectionPath() != null && skillTreeTree.getSelectionPath().getPathCount() > 1 && MyPetUtil.isInt(skillTreeTree.getSelectionPath().getPathComponent(1).toString()))
        {
            level = Integer.parseInt(skillTreeTree.getSelectionPath().getPathComponent(1).toString());
        }

        Map<String, Integer> skillCount = new HashMap<String, Integer>();
        for (MyPetSkillTreeLevel skillTreeLevel : this.skillTree.getLevelList())
        {
            if (skillTreeLevel.getLevel() <= level)
            {
                for (ISkillInfo skill : skillTreeLevel.getSkills())
                {
                    if (skillCount.containsKey(skill.getName()))
                    {
                        skillCount.put(skill.getName(), skillCount.get(skill.getName()) + 1);
                    }
                    else
                    {
                        skillCount.put(skill.getName(), 1);
                    }
                }
            }
        }
        if (skillTree.getInheritance() != null && skillTreeMobType.getSkillTree(skillTree.getInheritance()) != null)
        {
            for (MyPetSkillTreeLevel skillTreeLevel : this.skillTreeMobType.getSkillTree(skillTree.getInheritance()).getLevelList())
            {
                if (skillTreeLevel.getLevel() <= level)
                {
                    for (ISkillInfo skill : skillTreeLevel.getSkills())
                    {
                        if (skillCount.containsKey(skill.getName()))
                        {
                            skillCount.put(skill.getName(), skillCount.get(skill.getName()) + 1);
                        }
                        else
                        {
                            skillCount.put(skill.getName(), 1);
                        }
                    }
                }
            }
        }
        skillCounterTabelModel.getDataVector().removeAllElements();
        for (String skillName : skillCount.keySet())
        {
            String[] row = {skillName, "" + skillCount.get(skillName)};
            skillCounterTabelModel.addRow(row);
        }
        skilllevelLabel.setText("Skilllevels for level: " + level);
        skillCounterTabel.updateUI();
    }

    public JPanel getMainPanel()
    {
        return levelCreatorPanel;
    }

    public JFrame getFrame()
    {
        if (levelCreatorFrame == null)
        {
            levelCreatorFrame = new JFrame("LevelCreator - MyPet " + MyPetPlugin.MyPetVersion);
        }
        return levelCreatorFrame;
    }

    public void setSkillTree(MyPetSkillTree skillTree, MyPetSkillTreeMobType skillTreeMobType)
    {
        this.skillTree = skillTree;
        this.skillTreeMobType = skillTreeMobType;


        if (skillTree.hasDisplayName())
        {
            displayNameTextField.setEnabled(true);
            displayNameCheckbox.setSelected(true);
        }
        else
        {
            displayNameTextField.setEnabled(false);
            displayNameCheckbox.setSelected(false);
        }
        displayNameTextField.setText(skillTree.getDisplayName());
        if (skillTree.hasCustomPermissions())
        {
            permissionTextField.setEnabled(true);
            permissionCheckbox.setSelected(true);
        }
        else
        {
            permissionTextField.setEnabled(false);
            permissionCheckbox.setSelected(false);
        }
        permissionTextField.setText(skillTree.getPermission());
        permissionDisplayTextField.setText("MyPet.custom.skilltree." + skillTree.getPermission());
        mobTypeLabel.setText(skillTreeMobType.getMobTypeName());

        this.inheritanceComboBoxModel.removeAllElements();

        inheritanceCheckBox.setSelected(false);
        if (skillTreeMobType.getSkillTreeNames().size() == 1 && MyPetSkillTreeMobType.getMobTypeByName("default").getSkillTreeNames().size() == 0)
        {
            inheritanceCheckBox.setEnabled(false);
            inheritanceComboBox.setEnabled(false);
        }
        else
        {
            inheritanceCheckBox.setEnabled(true);
            ArrayList<String> skilltreeNames = new ArrayList<String>();
            for (String skillTreeName : skillTreeMobType.getSkillTreeNames())
            {
                if (!skillTreeName.equals(skillTree.getName()) && !skilltreeNames.contains(skillTreeName))
                {
                    skilltreeNames.add(skillTreeName);
                    inheritanceComboBoxModel.addElement(skillTreeName);
                }
            }
            for (String skillTreeName : MyPetSkillTreeMobType.getMobTypeByName("default").getSkillTreeNames())
            {
                if (!skillTreeName.equals(skillTree.getName()) && !skilltreeNames.contains(skillTreeName))
                {
                    skilltreeNames.add(skillTreeName);
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
        int skillcount = 0;
        for (MyPetSkillTreeLevel level : skillTree.getLevelList())
        {
            DefaultMutableTreeNode levelNode = new DefaultMutableTreeNode(level.getLevel());
            rootNode.add(levelNode);
            for (ISkillInfo skill : level.getSkills())
            {
                SkillTreeSkillNode skillNode = new SkillTreeSkillNode(skill);
                levelNode.add(skillNode);
                skillcount++;
            }
        }

        if (skillcount <= 15)
        {
            for (int i = 0 ; i < skillTreeTree.getRowCount() ; i++)
            {
                skillTreeTree.expandRow(i);
            }
        }
        else
        {
            skillTreeTree.expandRow(0);
        }
        skillTreeTree.updateUI();
        skillTreeTree.setSelectionPath(new TreePath(rootNode));
    }

    private void createUIComponents()
    {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("");
        skillTreeTreeModel = new DefaultTreeModel(root);
        skillTreeTree = new JTree(skillTreeTreeModel);
        skillTreeTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

        inheritanceComboBoxModel = new DefaultComboBoxModel();
        inheritanceComboBox = new JComboBox(inheritanceComboBoxModel);

        String[] columnNames = {"Skill", "Level"};
        String[][] data = new String[0][0];
        skillCounterTabelModel = new DefaultTableModel(data, columnNames);
        skillCounterTabel = new JTable(skillCounterTabelModel)
        {
            public boolean isCellEditable(int rowIndex, int colIndex)
            {
                return false;
            }
        };
        skillCounterTabel.getColumnModel().getColumn(0).setPreferredWidth(160);
        skillCounterTabel.getColumnModel().getColumn(1).setPreferredWidth(50);
    }

    private class SkillTreeSkillNode extends DefaultMutableTreeNode
    {
        private ISkillInfo skill;

        public SkillTreeSkillNode(ISkillInfo skill)
        {
            super(skill.getName());
            this.skill = skill;
        }

        public ISkillInfo getSkill()
        {
            return skill;
        }
    }

    private class SortedDefaultMutableTreeNode extends DefaultMutableTreeNode
    {
        public SortedDefaultMutableTreeNode(Object userObject)
        {
            super(userObject);
        }

        @SuppressWarnings("unchecked")
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
