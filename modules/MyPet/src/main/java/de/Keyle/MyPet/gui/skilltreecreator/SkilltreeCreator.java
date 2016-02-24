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

import de.Keyle.MyPet.api.skill.skilltree.SkillTree;
import de.Keyle.MyPet.api.skill.skilltree.SkillTreeMobType;
import de.Keyle.MyPet.api.skill.skilltreeloader.SkillTreeLoaderJSON;
import de.Keyle.MyPet.api.skill.skilltreeloader.SkillTreeLoaderNBT;
import de.Keyle.MyPet.gui.GuiMain;
import de.Keyle.MyPet.util.MyPetVersion;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.event.*;
import java.io.File;
import java.util.List;

public class SkilltreeCreator {
    JComboBox mobTypeComboBox;
    JButton addSkilltreeButton;
    JButton deleteSkilltreeButton;
    JTree skilltreeTree;
    JButton skilltreeDownButton;
    JButton skilltreeUpButton;
    JPanel skilltreeCreatorPanel;
    JButton saveButton;
    JButton renameSkilltreeButton;
    JFrame skilltreeCreatorFrame;
    JPopupMenu skilltreeListRightclickMenu;
    JMenuItem copyMenuItem;
    JMenuItem pasteMenuItem;
    JPopupMenu saveRightclickMenu;
    JMenuItem jsonMenuItem;
    JMenuItem nbtMenuItem;

    DefaultTreeModel skilltreeTreeModel;

    SkillTree skilltreeCopyPaste;
    SkillTreeMobType selectedMobtype;

    public SkilltreeCreator() {
        mobTypeComboBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    selectedMobtype = SkillTreeMobType.getMobTypeByName(mobTypeComboBox.getSelectedItem().toString());
                    skilltreeTreeSetSkilltrees();
                }
            }
        });
        skilltreeTree.addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
                if (skilltreeTree.getSelectionPath() != null && skilltreeTree.getSelectionPath().getPathCount() == 2) {
                    SkillTree skillTree = ((SkillTreeNode) skilltreeTree.getSelectionPath().getPathComponent(1)).getSkillTree();
                    if (skilltreeTreeModel.getChildCount(skilltreeTreeModel.getRoot()) <= 1) {
                        skilltreeDownButton.setEnabled(false);
                        skilltreeUpButton.setEnabled(false);
                    } else if (selectedMobtype.getSkillTreePlace(skillTree) >= skilltreeTreeModel.getChildCount(skilltreeTreeModel.getRoot()) - 1) {
                        skilltreeDownButton.setEnabled(false);
                        skilltreeUpButton.setEnabled(true);
                        deleteSkilltreeButton.setEnabled(true);
                        if (skilltreeDownButton.hasFocus()) {
                            skilltreeUpButton.requestFocus();
                        }
                    } else if (selectedMobtype.getSkillTreePlace(skillTree) <= 0) {
                        skilltreeDownButton.setEnabled(true);
                        skilltreeUpButton.setEnabled(false);
                        deleteSkilltreeButton.setEnabled(true);
                        if (skilltreeUpButton.hasFocus()) {
                            skilltreeDownButton.requestFocus();
                        }
                    } else {
                        skilltreeDownButton.setEnabled(true);
                        skilltreeUpButton.setEnabled(true);

                    }
                    copyMenuItem.setEnabled(true);
                    deleteSkilltreeButton.setEnabled(true);
                    renameSkilltreeButton.setEnabled(true);
                } else {
                    copyMenuItem.setEnabled(false);
                    deleteSkilltreeButton.setEnabled(false);
                    renameSkilltreeButton.setEnabled(false);
                    skilltreeDownButton.setEnabled(false);
                    skilltreeUpButton.setEnabled(false);
                }
            }
        });
        skilltreeUpButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (skilltreeTree.getSelectionPath().getPath().length == 2) {
                    if (skilltreeTree.getSelectionPath().getPathComponent(1) instanceof SkillTreeNode) {
                        SkillTree skillTree = ((SkillTreeNode) skilltreeTree.getSelectionPath().getPathComponent(1)).getSkillTree();
                        selectedMobtype.moveSkillTreeUp(skillTree);
                        skilltreeTreeSetSkilltrees();
                        selectSkilltree(skillTree);
                    }
                }
            }
        });
        skilltreeDownButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (skilltreeTree.getSelectionPath().getPath().length == 2) {
                    if (skilltreeTree.getSelectionPath().getPathComponent(1) instanceof SkillTreeNode) {
                        SkillTree skillTree = ((SkillTreeNode) skilltreeTree.getSelectionPath().getPathComponent(1)).getSkillTree();
                        selectedMobtype.moveSkillTreeDown(skillTree);
                        skilltreeTreeSetSkilltrees();
                        selectSkilltree(skillTree);
                    }
                }
            }
        });
        addSkilltreeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String response = JOptionPane.showInputDialog(null, "Enter the name of the new skilltree.", "Create new Skilltree", JOptionPane.QUESTION_MESSAGE);
                if (response != null) {
                    if (response.matches("(?m)[\\w-]+")) {
                        if (!selectedMobtype.hasSkillTree(response)) {
                            SkillTree skillTree = new SkillTree(response);
                            selectedMobtype.addSkillTree(skillTree);
                            skilltreeTreeSetSkilltrees();
                            selectSkilltree(skillTree);
                        } else {
                            JOptionPane.showMessageDialog(null, "There is already a skilltree with this name!", "Create new Skilltree", JOptionPane.ERROR_MESSAGE);
                        }
                    } else {
                        JOptionPane.showMessageDialog(null, "This is not a valid skilltree name!\n\na-z\nA-Z\n0-9\n_ -", "Create new Skilltree", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });
        renameSkilltreeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (skilltreeTree.getSelectionPath().getPath().length == 2) {
                    if (skilltreeTree.getSelectionPath().getPathComponent(1) instanceof SkillTreeNode) {
                        SkillTree skillTree = ((SkillTreeNode) skilltreeTree.getSelectionPath().getPathComponent(1)).getSkillTree();
                        String response = (String) JOptionPane.showInputDialog(null, "Enter the name of the new skilltree.", "Create new Skilltree", JOptionPane.QUESTION_MESSAGE, null, null, skillTree.getName());
                        if (response != null) {
                            if (response.matches("(?m)[\\w-]+")) {
                                if (!selectedMobtype.hasSkillTree(response)) {
                                    SkillTree newSkillTree = skillTree.clone(response);
                                    selectedMobtype.removeSkillTree(skillTree.getName());
                                    selectedMobtype.addSkillTree(newSkillTree);
                                    skilltreeTreeSetSkilltrees();
                                    selectSkilltree(newSkillTree);
                                } else {
                                    JOptionPane.showMessageDialog(null, "There is already a skilltree with this name!", "Create new Skilltree", JOptionPane.ERROR_MESSAGE);
                                }
                            } else {
                                JOptionPane.showMessageDialog(null, "This is not a valid skilltree name!\n\na-z\nA-Z\n0-9\n_ -", "Create new Skilltree", JOptionPane.ERROR_MESSAGE);
                            }
                        }

                        skilltreeTreeSetSkilltrees();
                    }
                }
            }
        });
        deleteSkilltreeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (skilltreeTree.getSelectionPath().getPath().length == 2) {
                    if (skilltreeTree.getSelectionPath().getPathComponent(1) instanceof SkillTreeNode) {
                        SkillTree skillTree = ((SkillTreeNode) skilltreeTree.getSelectionPath().getPathComponent(1)).getSkillTree();
                        selectedMobtype.removeSkillTree(skillTree.getName());
                        skilltreeTreeSetSkilltrees();
                    }
                }
            }
        });
        skilltreeTree.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2 && skilltreeTree.getSelectionPath() != null) {
                    if (skilltreeTree.getSelectionPath().getPath().length == 2) {
                        if (skilltreeTree.getSelectionPath().getPathComponent(1) instanceof SkillTreeNode) {
                            SkillTree skillTree = ((SkillTreeNode) skilltreeTree.getSelectionPath().getPathComponent(1)).getSkillTree();
                            GuiMain.levelCreator.setSkillTree(skillTree, selectedMobtype);
                            GuiMain.levelCreator.getFrame().setVisible(true);
                            skilltreeCreatorFrame.setEnabled(false);
                        }
                    }
                }
            }
        });
        saveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                saveRightclickMenu.show(saveButton, -21, saveButton.getHeight());
            }
        });
        nbtMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String savedPetsString = "";
                List<String> savedPetTypes;

                savedPetTypes = SkillTreeLoaderNBT.getSkilltreeLoader().saveSkillTrees(GuiMain.configPath + "skilltrees", GuiMain.petTypes);
                for (String petType : savedPetTypes) {
                    savedPetsString += "\n   " + petType.toLowerCase() + ".st";
                }

                JOptionPane.showMessageDialog(null, "Saved to:\n" + GuiMain.configPath + "skilltrees" + File.separator + savedPetsString, "Saved following configs", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        jsonMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String savedPetsString = "";
                List<String> savedPetTypes;

                savedPetTypes = SkillTreeLoaderJSON.getSkilltreeLoader().saveSkillTrees(GuiMain.configPath + "skilltrees", GuiMain.petTypes);
                for (String petType : savedPetTypes) {
                    savedPetsString += "\n   " + petType.toLowerCase() + ".json";
                }

                JOptionPane.showMessageDialog(null, "Saved to:\n" + GuiMain.configPath + "skilltrees" + File.separator + savedPetsString, "Saved following configs", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        skilltreeTree.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (skilltreeTree.getSelectionPath().getPath().length == 2) {
                    if (skilltreeTree.getSelectionPath().getPathComponent(1) instanceof SkillTreeNode) {
                        SkillTree skillTree = ((SkillTreeNode) skilltreeTree.getSelectionPath().getPathComponent(1)).getSkillTree();
                        switch (e.getKeyCode()) {
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
        copyMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                skilltreeCopyPaste = ((SkillTreeNode) skilltreeTree.getSelectionPath().getPathComponent(1)).getSkillTree();
                pasteMenuItem.setEnabled(true);
            }
        });
        pasteMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                for (int i = 2; ; i++) {
                    if (!selectedMobtype.hasSkillTree(skilltreeCopyPaste.getName() + "_" + i)) {
                        SkillTree skillTree = skilltreeCopyPaste.clone(skilltreeCopyPaste.getName() + "_" + i);
                        selectedMobtype.addSkillTree(skillTree);
                        skilltreeTreeSetSkilltrees();
                        selectSkilltree(skillTree);
                        break;
                    }
                }
            }
        });
    }

    public JPanel getMainPanel() {
        return skilltreeCreatorPanel;
    }

    public JFrame getFrame() {
        if (skilltreeCreatorFrame == null) {
            skilltreeCreatorFrame = new JFrame("SkilltreeCreator - MyPet " + MyPetVersion.getVersion());
        }
        return skilltreeCreatorFrame;
    }

    public void selectSkilltree(String skilltreeName) {
        DefaultMutableTreeNode root = ((DefaultMutableTreeNode) skilltreeTreeModel.getRoot());
        DefaultMutableTreeNode[] path = new DefaultMutableTreeNode[2];
        path[0] = root;
        for (int i = 0; i < root.getChildCount(); i++) {
            if (root.getChildAt(i) instanceof SkillTreeNode) {
                SkillTreeNode node = (SkillTreeNode) root.getChildAt(i);
                if (node.getSkillTree().getName().equals(skilltreeName)) {
                    path[1] = node;
                    TreePath treePath = new TreePath(path);
                    skilltreeTree.setSelectionPath(treePath);
                    return;
                }
            }
        }
    }

    public void selectSkilltree(SkillTree skilltree) {
        DefaultMutableTreeNode root = ((DefaultMutableTreeNode) skilltreeTreeModel.getRoot());
        DefaultMutableTreeNode[] path = new DefaultMutableTreeNode[2];
        path[0] = root;
        for (int i = 0; i < root.getChildCount(); i++) {
            if (root.getChildAt(i) instanceof SkillTreeNode) {
                SkillTreeNode node = (SkillTreeNode) root.getChildAt(i);
                if (node.getSkillTree() == skilltree) {
                    path[1] = node;
                    TreePath treePath = new TreePath(path);
                    skilltreeTree.setSelectionPath(treePath);
                    return;
                }
            }
        }
    }

    public void skilltreeTreeSetSkilltrees() {
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(selectedMobtype.getPetType());
        skilltreeTreeModel.setRoot(rootNode);
        for (SkillTree skillTree : selectedMobtype.getSkillTrees()) {
            SkillTreeNode skillTreeNode = new SkillTreeNode(skillTree);
            rootNode.add(skillTreeNode);
        }
        skilltreeTreeExpandAll();
    }

    public void skilltreeTreeExpandAll() {
        for (int i = 0; i < skilltreeTree.getRowCount(); i++) {
            skilltreeTree.expandRow(i);
        }
    }

    private void createUIComponents() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("");
        skilltreeTreeModel = new DefaultTreeModel(root);
        skilltreeTree = new JTree(skilltreeTreeModel);
        skilltreeTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

        createRightclickMenus();

        mobTypeComboBox = new JComboBox(GuiMain.petTypes);
        selectedMobtype = SkillTreeMobType.getMobTypeByName("default");
        skilltreeTreeSetSkilltrees();
    }

    public void createRightclickMenus() {
        skilltreeListRightclickMenu = new JPopupMenu();

        copyMenuItem = new JMenuItem("Copy");
        skilltreeListRightclickMenu.add(copyMenuItem);

        pasteMenuItem = new JMenuItem("Paste");
        pasteMenuItem.setEnabled(false);
        skilltreeListRightclickMenu.add(pasteMenuItem);

        MouseListener popupListener = new PopupListener(skilltreeListRightclickMenu);
        skilltreeTree.addMouseListener(popupListener);

        saveRightclickMenu = new JPopupMenu();

        nbtMenuItem = new JMenuItem("NBT (.st)");
        saveRightclickMenu.add(nbtMenuItem);

        jsonMenuItem = new JMenuItem("JSON (.json)");
        saveRightclickMenu.add(jsonMenuItem);
    }

    private class SkillTreeNode extends DefaultMutableTreeNode {
        private SkillTree skillTree;

        public SkillTreeNode(SkillTree skillTree) {
            super(skillTree.getName());
            this.skillTree = skillTree;
        }

        public SkillTree getSkillTree() {
            return skillTree;
        }
    }

    public static class PopupListener extends MouseAdapter {
        JPopupMenu popup;

        PopupListener(JPopupMenu popupMenu) {
            popup = popupMenu;
        }

        public void mousePressed(MouseEvent e) {
            maybeShowPopup(e);
        }

        public void mouseReleased(MouseEvent e) {
            maybeShowPopup(e);
        }

        private void maybeShowPopup(MouseEvent e) {
            if (e.isPopupTrigger()) {
                popup.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }
}