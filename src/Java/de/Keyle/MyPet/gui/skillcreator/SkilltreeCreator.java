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
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.event.*;
import java.io.File;
import java.util.List;

public class SkilltreeCreator
{
    JComboBox mobTypeComboBox;
    JButton addSkilltreeButton;
    JButton deleteSkilltreeButton;
    JTree skilltreeTree;
    JButton skilltreeDownButton;
    JButton skilltreeUpButton;
    JPanel skilltreeCreatorPanel;
    JButton saveButton;
    JFrame skilltreeCreatorFrame;
    JPopupMenu rightclickMenu;
    JMenuItem copyMenuItem;
    JMenuItem pasteMenuItem;

    DefaultTreeModel skilltreeTreeModel;

    private MyPetSkillTree skilltreeCopyPaste;
    MyPetSkillTreeMobType selectedMobtype;

    public SkilltreeCreator()
    {
        this.mobTypeComboBox.addItemListener(new ItemListener()
        {
            public void itemStateChanged(ItemEvent e)
            {
                if (e.getStateChange() == ItemEvent.SELECTED)
                {
                    selectedMobtype = MyPetSkillTreeMobType.getMobTypeByName(mobTypeComboBox.getSelectedItem().toString());
                    skilltreeTreeSetSkilltrees();
                }
            }
        });

        skilltreeTree.addTreeSelectionListener(new TreeSelectionListener()
        {
            public void valueChanged(TreeSelectionEvent e)
            {
                if (skilltreeTree.getSelectionPath() != null && skilltreeTree.getSelectionPath().getPathCount() == 2)
                {
                    MyPetSkillTree skillTree = ((SkillTreeNode) skilltreeTree.getSelectionPath().getPathComponent(1)).getSkillTree();
                    if (skilltreeTreeModel.getChildCount(skilltreeTreeModel.getRoot()) <= 1)
                    {
                        skilltreeDownButton.setEnabled(false);
                        skilltreeUpButton.setEnabled(false);
                    }
                    else if (selectedMobtype.getSkillTreePlace(skillTree) >= skilltreeTreeModel.getChildCount(skilltreeTreeModel.getRoot()) - 1)
                    {
                        skilltreeDownButton.setEnabled(false);
                        skilltreeUpButton.setEnabled(true);
                        deleteSkilltreeButton.setEnabled(true);
                        if (skilltreeDownButton.hasFocus())
                        {
                            skilltreeUpButton.requestFocus();
                        }
                    }
                    else if (selectedMobtype.getSkillTreePlace(skillTree) <= 0)
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

                    }
                    copyMenuItem.setEnabled(true);
                    deleteSkilltreeButton.setEnabled(true);
                }
                else
                {
                    copyMenuItem.setEnabled(false);
                    deleteSkilltreeButton.setEnabled(false);
                    skilltreeDownButton.setEnabled(false);
                    skilltreeUpButton.setEnabled(false);
                }
            }
        });

        skilltreeUpButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                if (skilltreeTree.getSelectionPath().getPath().length == 2)
                {
                    if (skilltreeTree.getSelectionPath().getPathComponent(1) instanceof SkillTreeNode)
                    {
                        MyPetSkillTree skillTree = ((SkillTreeNode) skilltreeTree.getSelectionPath().getPathComponent(1)).getSkillTree();
                        selectedMobtype.moveSkillTreeUp(skillTree);
                        skilltreeTreeSetSkilltrees();
                        selectSkilltree(skillTree);
                    }
                }
            }
        });
        skilltreeDownButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                if (skilltreeTree.getSelectionPath().getPath().length == 2)
                {
                    if (skilltreeTree.getSelectionPath().getPathComponent(1) instanceof SkillTreeNode)
                    {
                        MyPetSkillTree skillTree = ((SkillTreeNode) skilltreeTree.getSelectionPath().getPathComponent(1)).getSkillTree();
                        selectedMobtype.moveSkillTreeDown(skillTree);
                        skilltreeTreeSetSkilltrees();
                        selectSkilltree(skillTree);
                    }
                }
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
                        if (!selectedMobtype.hasSkillTree(response))
                        {
                            MyPetSkillTree skillTree = new MyPetSkillTree(response);
                            selectedMobtype.addSkillTree(skillTree);
                            skilltreeTreeSetSkilltrees();
                            selectSkilltree(skillTree);
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
                if (skilltreeTree.getSelectionPath().getPath().length == 2)
                {
                    if (skilltreeTree.getSelectionPath().getPathComponent(1) instanceof SkillTreeNode)
                    {
                        MyPetSkillTree skillTree = ((SkillTreeNode) skilltreeTree.getSelectionPath().getPathComponent(1)).getSkillTree();
                        selectedMobtype.removeSkillTree(skillTree.getName());
                        skilltreeTreeSetSkilltrees();
                    }
                }
            }
        });
        skilltreeTree.addMouseListener(new MouseAdapter()
        {
            public void mouseClicked(MouseEvent evt)
            {
                if (evt.getClickCount() == 2)
                {
                    if (skilltreeTree.getSelectionPath().getPath().length == 2)
                    {
                        if (skilltreeTree.getSelectionPath().getPathComponent(1) instanceof SkillTreeNode)
                        {
                            MyPetSkillTree skillTree = ((SkillTreeNode) skilltreeTree.getSelectionPath().getPathComponent(1)).getSkillTree();
                            GuiMain.levelCreator.setSkillTree(skillTree, selectedMobtype);
                            GuiMain.levelCreator.getFrame().setVisible(true);
                            skilltreeCreatorFrame.setEnabled(false);
                        }
                    }
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
        skilltreeTree.addKeyListener(new KeyAdapter()
        {
            public void keyPressed(KeyEvent e)
            {
                if (skilltreeTree.getSelectionPath().getPath().length == 2)
                {
                    if (skilltreeTree.getSelectionPath().getPathComponent(1) instanceof SkillTreeNode)
                    {
                        MyPetSkillTree skillTree = ((SkillTreeNode) skilltreeTree.getSelectionPath().getPathComponent(1)).getSkillTree();
                        switch (e.getKeyCode())
                        {
                            case KeyEvent.VK_ENTER:
                                GuiMain.levelCreator.setSkillTree(skillTree, selectedMobtype);
                                GuiMain.levelCreator.getFrame().setVisible(true);
                                skilltreeCreatorFrame.setEnabled(false);
                                break;
                            case KeyEvent.VK_DELETE:
                                selectedMobtype.removeSkillTree(skillTree.getName());
                                skilltreeTreeSetSkilltrees();
                                break;
                        }
                    }
                }

            }
        });
        copyMenuItem.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                skilltreeCopyPaste = ((SkillTreeNode) skilltreeTree.getSelectionPath().getPathComponent(1)).getSkillTree();
                pasteMenuItem.setEnabled(true);
            }
        });
        pasteMenuItem.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                for (int i = 2 ; ; i++)
                {
                    if (!selectedMobtype.hasSkillTree(skilltreeCopyPaste.getName() + "_" + i))
                    {
                        MyPetSkillTree skillTree = skilltreeCopyPaste.clone(skilltreeCopyPaste.getName() + "_" + i);
                        selectedMobtype.addSkillTree(skillTree);
                        skilltreeTreeSetSkilltrees();
                        selectSkilltree(skillTree);
                        break;
                    }
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

    public void selectSkilltree(String skilltreeName)
    {
        DefaultMutableTreeNode root = ((DefaultMutableTreeNode) skilltreeTreeModel.getRoot());
        DefaultMutableTreeNode[] path = new DefaultMutableTreeNode[2];
        path[0] = root;
        for (int i = 0 ; i < root.getChildCount() ; i++)
        {
            if (root.getChildAt(i) instanceof SkillTreeNode)
            {
                SkillTreeNode node = (SkillTreeNode) root.getChildAt(i);
                if (node.getSkillTree().getName().equals(skilltreeName))
                {
                    path[1] = node;
                    TreePath treePath = new TreePath(path);
                    skilltreeTree.setSelectionPath(treePath);
                    return;
                }
            }
        }
    }

    public void selectSkilltree(MyPetSkillTree skilltree)
    {
        DefaultMutableTreeNode root = ((DefaultMutableTreeNode) skilltreeTreeModel.getRoot());
        DefaultMutableTreeNode[] path = new DefaultMutableTreeNode[2];
        path[0] = root;
        for (int i = 0 ; i < root.getChildCount() ; i++)
        {
            if (root.getChildAt(i) instanceof SkillTreeNode)
            {
                SkillTreeNode node = (SkillTreeNode) root.getChildAt(i);
                if (node.getSkillTree() == skilltree)
                {
                    path[1] = node;
                    TreePath treePath = new TreePath(path);
                    skilltreeTree.setSelectionPath(treePath);
                    return;
                }
            }
        }
    }

    public void skilltreeTreeSetSkilltrees()
    {
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(selectedMobtype.getMobTypeName());
        skilltreeTreeModel.setRoot(rootNode);
        for (MyPetSkillTree skillTree : selectedMobtype.getSkillTrees())
        {
            SkillTreeNode skillTreeNode = new SkillTreeNode(skillTree);
            rootNode.add(skillTreeNode);
        }
        skilltreeTreeExpandAll();
    }

    public void skilltreeTreeExpandAll()
    {
        for (int i = 0 ; i < skilltreeTree.getRowCount() ; i++)
        {
            skilltreeTree.expandRow(i);
        }
    }

    private void createUIComponents()
    {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("");
        skilltreeTreeModel = new DefaultTreeModel(root);
        skilltreeTree = new JTree(skilltreeTreeModel);

        createRightclickMenu();

        selectedMobtype = MyPetSkillTreeMobType.getMobTypeByName("default");
        skilltreeTreeSetSkilltrees();
    }

    public void createRightclickMenu()
    {
        rightclickMenu = new JPopupMenu();

        copyMenuItem = new JMenuItem("Copy");
        rightclickMenu.add(copyMenuItem);

        pasteMenuItem = new JMenuItem("Paste");
        pasteMenuItem.setEnabled(false);
        rightclickMenu.add(pasteMenuItem);

        MouseListener popupListener = new PopupListener(rightclickMenu);
        skilltreeTree.addMouseListener(popupListener);
    }

    private class SkillTreeNode extends DefaultMutableTreeNode
    {
        private MyPetSkillTree skillTree;

        public SkillTreeNode(MyPetSkillTree skillTree)
        {
            super(skillTree.getName());
            this.skillTree = skillTree;
        }

        public MyPetSkillTree getSkillTree()
        {
            return skillTree;
        }
    }

    class PopupListener extends MouseAdapter
    {
        JPopupMenu popup;

        PopupListener(JPopupMenu popupMenu)
        {
            popup = popupMenu;
        }

        public void mousePressed(MouseEvent e)
        {
            maybeShowPopup(e);
        }

        public void mouseReleased(MouseEvent e)
        {
            maybeShowPopup(e);
        }

        private void maybeShowPopup(MouseEvent e)
        {
            if (e.isPopupTrigger())
            {
                popup.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }
}
