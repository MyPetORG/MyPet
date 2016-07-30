/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2016 Keyle
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

import de.Keyle.MyPet.api.MyPetVersion;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.skill.SkillInfo;
import de.Keyle.MyPet.api.skill.SkillName;
import de.Keyle.MyPet.api.skill.SkillProperties;
import de.Keyle.MyPet.api.skill.SkillsInfo;
import de.Keyle.MyPet.api.skill.skills.*;
import de.Keyle.MyPet.api.skill.skilltree.SkillTree;
import de.Keyle.MyPet.api.skill.skilltree.SkillTreeLevel;
import de.Keyle.MyPet.api.skill.skilltree.SkillTreeMobType;
import de.Keyle.MyPet.api.skill.skilltree.SkillTreeSkill;
import de.Keyle.MyPet.gui.GuiMain;
import de.Keyle.MyPet.gui.skilltreecreator.skills.*;
import de.keyle.knbt.TagCompound;
import de.keyle.knbt.TagInt;
import de.keyle.knbt.TagShort;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.util.*;

public class LevelCreator {
    JTree skillTreeTree;
    JLabel skillTreeNameLabel;
    JButton deleteLevelSkillButton;
    JButton addLevelButton;
    JButton addSkillButton;
    JComboBox inheritanceComboBox;
    JCheckBox inheritanceCheckBox;
    JPanel levelCreatorPanel;
    JButton backButton;
    JButton copyButton;
    JTextField permissionDisplayTextField;
    JCheckBox displayNameCheckbox;
    JCheckBox permissionCheckbox;
    JTextField displayNameTextField;
    JTextField permissionTextField;
    JCheckBox levelUpMessageCheckBox;
    JTextField levelUpMessageInput;
    JButton editDescriptionButton;
    JButton editIconButton;
    JTextField maxLevelTextField;
    JCheckBox maxLevelCheckBox;
    JTextField requiredLevelTextField;
    JCheckBox requiredLevelCheckBox;
    JFrame levelCreatorFrame;
    JPopupMenu levelListRightclickMenu;

    DefaultTreeModel skillTreeTreeModel;
    DefaultComboBoxModel inheritanceComboBoxModel;

    SkillTree skillTree;
    SkillTreeLevel selectedLevel = null;
    SkillTreeMobType skillTreeMobType;

    private static String[] skillNames = null;
    private Map<Class<? extends SkillInfo>, SkillPropertiesPanel> skillPanels = new HashMap<>();

    int highestLevel = 0;

    public LevelCreator() {
        registerSkillPanels();

        if (skillNames == null) {
            skillNames = new String[SkillsInfo.getRegisteredSkillsInfo().size()];
            int skillCounter = 0;
            for (Class<? extends SkillTreeSkill> clazz : SkillsInfo.getRegisteredSkillsInfo()) {
                SkillName sn = clazz.getAnnotation(SkillName.class);
                if (sn != null) {
                    skillNames[skillCounter++] = sn.value();
                }
            }
            Arrays.sort(skillNames);
        }

        addLevelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String response = (String) JOptionPane.showInputDialog(null, "Enter the number for the new level.", "Create new Level", JOptionPane.QUESTION_MESSAGE, null, null, "" + (highestLevel + 1));
                if (response != null) {
                    if (Util.isInt(response)) {
                        int newLevel = Integer.parseInt(response);
                        for (int i = 0; i < skillTreeTreeModel.getChildCount(skillTreeTreeModel.getRoot()); i++) {
                            if (Util.isInt(((DefaultMutableTreeNode) skillTreeTreeModel.getRoot()).getChildAt(i).toString())) {
                                int level = Integer.parseInt(((DefaultMutableTreeNode) skillTreeTreeModel.getRoot()).getChildAt(i).toString());
                                if (newLevel == level) {
                                    JOptionPane.showMessageDialog(null, response + " already exists!", "Create new Level", JOptionPane.ERROR_MESSAGE);
                                    return;
                                } else if (newLevel < 1) {
                                    JOptionPane.showMessageDialog(null, response + " is smaller than 1!", "Create new Level", JOptionPane.ERROR_MESSAGE);
                                    return;
                                }
                            }
                        }
                        highestLevel = Math.max(highestLevel, newLevel);
                        DefaultMutableTreeNode levelNode = new DefaultMutableTreeNode(newLevel);
                        skillTree.addLevel(newLevel);

                        ((SortedDefaultMutableTreeNode) skillTreeTreeModel.getRoot()).add(levelNode);
                        skillTreeTree.setSelectionPath(new TreePath(new Object[]{skillTreeTreeModel.getRoot(), levelNode}));
                        skillTreeTree.expandPath(skillTreeTree.getSelectionPath());
                        skillTreeTree.updateUI();
                    } else {
                        JOptionPane.showMessageDialog(null, response + " is not a number!", "Create new Level", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });
        addSkillButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int level;
                if (skillTreeTree.getSelectionPath().getPath().length == 2 || skillTreeTree.getSelectionPath().getPath().length == 3) {
                    if (Util.isInt(skillTreeTree.getSelectionPath().getPathComponent(1).toString())) {
                        level = Integer.parseInt(skillTreeTree.getSelectionPath().getPathComponent(1).toString());
                    } else {
                        return;
                    }
                } else {
                    return;
                }
                String choosenSkill = (String) JOptionPane.showInputDialog(null, "Please select the skill you want to add to level " + level + '.', "", JOptionPane.QUESTION_MESSAGE, null, skillNames, "");
                if (choosenSkill != null) {
                    SkillInfo skill = SkillsInfo.getNewSkillInfoInstance(choosenSkill);
                    if (skill != null) {
                        skillTree.addSkillToLevel(level, skill);
                        SkillTreeSkillNode skillNode = new SkillTreeSkillNode(skill);
                        skill.setDefaultProperties();
                        ((DefaultMutableTreeNode) skillTreeTree.getSelectionPath().getPathComponent(1)).add(skillNode);
                        skillTreeTree.expandPath(skillTreeTree.getSelectionPath());
                        skillTreeTree.updateUI();
                    }
                }
            }
        });
        deleteLevelSkillButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                if (skillTreeTree.getSelectionPath().getPath().length == 2) {
                    if (Util.isInt(skillTreeTree.getSelectionPath().getLastPathComponent().toString())) {
                        int level = Integer.parseInt(skillTreeTree.getSelectionPath().getLastPathComponent().toString());
                        skillTree.removeLevel(level);
                    } else {
                        return;
                    }
                } else if (skillTreeTree.getSelectionPath().getPath().length == 3) {
                    if (Util.isInt(skillTreeTree.getSelectionPath().getPathComponent(1).toString())) {
                        short level = Short.parseShort(skillTreeTree.getSelectionPath().getPathComponent(1).toString());
                        int index = ((DefaultMutableTreeNode) skillTreeTree.getSelectionPath().getPathComponent(1)).getIndex(((DefaultMutableTreeNode) skillTreeTree.getSelectionPath().getPathComponent(2)));
                        skillTree.getLevel(level).removeSkill(index);
                    } else {
                        return;
                    }
                }
                ((DefaultMutableTreeNode) skillTreeTree.getSelectionPath().getLastPathComponent()).removeFromParent();
                skillTreeTree.updateUI();
                addSkillButton.setEnabled(false);
                deleteLevelSkillButton.setEnabled(false);
            }
        });
        skillTreeTree.addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
                if (e.getPath().getPath().length == 1) {
                    addSkillButton.setEnabled(false);
                    deleteLevelSkillButton.setEnabled(false);
                    levelUpMessageCheckBox.setEnabled(false);
                    levelUpMessageCheckBox.setSelected(false);
                    levelUpMessageInput.setEnabled(false);
                    levelUpMessageInput.setText("");
                    selectedLevel = null;
                } else if (e.getPath().getPath().length == 2) {
                    addSkillButton.setEnabled(true);
                    deleteLevelSkillButton.setEnabled(true);
                    levelUpMessageCheckBox.setEnabled(false);
                    levelUpMessageInput.setEnabled(false);
                    levelUpMessageInput.setText("");

                    if (skillTreeTree.getSelectionPath() == null) {
                        return;
                    }

                    if (Util.isInt(skillTreeTree.getSelectionPath().getLastPathComponent().toString())) {
                        int level = Integer.parseInt(skillTreeTree.getSelectionPath().getLastPathComponent().toString());
                        if (level > 1 && skillTree.hasLevel(level)) {
                            levelUpMessageCheckBox.setEnabled(true);
                            selectedLevel = skillTree.getLevel(level);
                            if (selectedLevel.hasLevelupMessage()) {
                                levelUpMessageCheckBox.setSelected(true);
                                levelUpMessageInput.setEnabled(true);
                                levelUpMessageInput.setText(selectedLevel.getLevelupMessage());
                            } else {
                                levelUpMessageCheckBox.setSelected(false);
                                levelUpMessageInput.setText("");
                            }
                        }
                    }
                } else if (e.getPath().getPath().length == 3) {
                    addSkillButton.setEnabled(true);
                    deleteLevelSkillButton.setEnabled(true);
                    levelUpMessageCheckBox.setEnabled(false);
                    levelUpMessageInput.setEnabled(false);
                    levelUpMessageCheckBox.setSelected(false);
                    levelUpMessageInput.setText("");
                    selectedLevel = null;
                }
            }

        });
        inheritanceCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (inheritanceCheckBox.isSelected()) {
                    inheritanceComboBox.setEnabled(true);
                    skillTree.setInheritance(inheritanceComboBox.getSelectedItem().toString());
                } else {
                    inheritanceComboBox.setEnabled(false);
                    skillTree.setInheritance(null);
                }
            }
        });
        maxLevelCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (maxLevelCheckBox.isSelected()) {
                    maxLevelTextField.setEnabled(true);
                    skillTree.setMaxLevel(0);
                } else {
                    maxLevelTextField.setEnabled(false);
                    skillTree.setMaxLevel(0);
                    maxLevelTextField.setText("" + 0);
                }
            }
        });
        requiredLevelCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (requiredLevelCheckBox.isSelected()) {
                    requiredLevelTextField.setEnabled(true);
                    skillTree.setRequiredLevel(0);
                } else {
                    requiredLevelTextField.setEnabled(false);
                    skillTree.setRequiredLevel(0);
                    requiredLevelTextField.setText("" + 0);
                }
            }
        });
        permissionCheckbox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (permissionCheckbox.isSelected()) {
                    permissionTextField.setEnabled(true);
                    if (!permissionTextField.getText().equalsIgnoreCase("")) {
                        skillTree.setPermission(permissionTextField.getText());
                    } else {
                        skillTree.setPermission(null);
                    }
                } else {
                    permissionTextField.setEnabled(false);
                    skillTree.setPermission(null);
                }
                permissionDisplayTextField.setText("MyPet.skilltree." + skillTree.getPermission());
            }
        });
        displayNameCheckbox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (displayNameCheckbox.isSelected()) {
                    displayNameTextField.setEnabled(true);
                    if (!displayNameTextField.getText().equalsIgnoreCase("")) {
                        skillTree.setDisplayName(displayNameTextField.getText());
                    } else {
                        skillTree.setDisplayName(null);
                    }
                } else {
                    displayNameTextField.setEnabled(false);
                    skillTree.setDisplayName(null);
                }
            }
        });
        backButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                GuiMain.skilltreeCreator.getFrame().setEnabled(true);
                levelCreatorFrame.setVisible(false);
            }
        });
        inheritanceComboBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED && inheritanceCheckBox.isSelected()) {
                    if (!skillTree.getInheritance().equals(e.getItem().toString())) {
                        skillTree.setInheritance(e.getItem().toString());
                    }
                }
            }
        });
        copyButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                StringSelection stringSelection = new StringSelection("MyPet.skilltree." + skillTree.getName());
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(stringSelection, null);
            }
        });
        skillTreeTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    if (skillTreeTree.getSelectionPath().getPath().length == 3) {
                        if (skillTreeTree.getSelectionPath().getPathComponent(2) instanceof SkillTreeSkillNode) {
                            SkillInfo skill = ((SkillTreeSkillNode) skillTreeTree.getSelectionPath().getPathComponent(2)).getSkill();
                            if (skill.getClass().getAnnotation(SkillProperties.class) == null) {
                                JOptionPane.showMessageDialog(null, skill.getName() + " has no options.", "Skill options", JOptionPane.INFORMATION_MESSAGE);
                                return;
                            }
                            if (SkillsInfo.getSkillInfoClass(skill.getName()) != null) {
                                SkillPropertiesPanel panel = skillPanels.get(skill.getClass());
                                if (panel != null) {
                                    GuiMain.skillPropertyEditor.setSkill(skill, panel);
                                    GuiMain.skillPropertyEditor.getFrame().setVisible(true);
                                    getFrame().setEnabled(false);
                                    GuiMain.skillPropertyEditor.getFrame().setSize(GuiMain.skillPropertyEditor.getFrame().getWidth(), panel.getMainPanel().getHeight() + 90);
                                } else {
                                    JOptionPane.showMessageDialog(null, skill.getName() + " has no options.", "Skill options", JOptionPane.INFORMATION_MESSAGE);
                                }
                            }
                        }
                    }
                }
            }
        });
        permissionTextField.addKeyListener(new KeyListener() {
            public void keyTyped(KeyEvent arg0) {
            }

            public void keyReleased(KeyEvent arg0) {
                permissionTextField.setText(permissionTextField.getText().replaceAll("[^a-zA-Z0-9]*", ""));
                if (permissionCheckbox.isSelected() && !skillTree.getPermission().equals(permissionTextField.getText())) {
                    if (!permissionTextField.getText().equalsIgnoreCase("")) {
                        skillTree.setPermission(permissionTextField.getText());
                    } else {
                        skillTree.setPermission(null);
                    }
                    permissionDisplayTextField.setText("MyPet.skilltree." + skillTree.getPermission());
                }
            }

            public void keyPressed(KeyEvent arg0) {
            }
        });
        maxLevelTextField.addKeyListener(new KeyListener() {
            public void keyTyped(KeyEvent arg0) {
            }

            public void keyReleased(KeyEvent arg0) {
                maxLevelTextField.setText(maxLevelTextField.getText().replaceAll("[^0-9]*", ""));
                if (maxLevelCheckBox.isSelected()) {
                    if (!maxLevelTextField.getText().equalsIgnoreCase("") && maxLevelTextField.getText().matches("[0-9]*")) {
                        skillTree.setMaxLevel(Integer.parseInt(maxLevelTextField.getText()));
                    } else {
                        skillTree.setMaxLevel(0);
                    }
                }
            }

            public void keyPressed(KeyEvent arg0) {
            }
        });
        requiredLevelTextField.addKeyListener(new KeyListener() {
            public void keyTyped(KeyEvent arg0) {
            }

            public void keyReleased(KeyEvent arg0) {
                requiredLevelTextField.setText(requiredLevelTextField.getText().replaceAll("[^0-9]*", ""));
                if (requiredLevelCheckBox.isSelected()) {
                    if (!requiredLevelTextField.getText().equalsIgnoreCase("") && requiredLevelTextField.getText().matches("[0-9]*")) {
                        skillTree.setRequiredLevel(Integer.parseInt(requiredLevelTextField.getText()));
                    } else {
                        skillTree.setRequiredLevel(0);
                    }
                }
            }

            public void keyPressed(KeyEvent arg0) {
            }
        });
        displayNameTextField.addKeyListener(new KeyListener() {
            public void keyTyped(KeyEvent arg0) {
            }

            public void keyReleased(KeyEvent arg0) {
                if (displayNameCheckbox.isSelected() && !skillTree.getDisplayName().equals(displayNameTextField.getText())) {
                    if (!displayNameTextField.getText().equalsIgnoreCase("")) {
                        skillTree.setDisplayName(displayNameTextField.getText());
                    } else {
                        skillTree.setDisplayName(null);
                    }
                }
            }

            public void keyPressed(KeyEvent arg0) {
            }
        });
        levelUpMessageInput.addKeyListener(new KeyListener() {
            public void keyTyped(KeyEvent arg0) {
            }

            public void keyReleased(KeyEvent arg0) {
                if (levelUpMessageCheckBox.isSelected() && selectedLevel != null && (selectedLevel.getLevelupMessage() == null || !selectedLevel.getLevelupMessage().equals(levelUpMessageInput.getText()))) {
                    if (!levelUpMessageInput.getText().equalsIgnoreCase("")) {
                        selectedLevel.setLevelupMessage(levelUpMessageInput.getText());
                    } else {
                        selectedLevel.setLevelupMessage(null);
                    }
                }
            }

            public void keyPressed(KeyEvent arg0) {
            }
        });
        levelUpMessageCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                levelUpMessageInput.setEnabled(levelUpMessageCheckBox.isSelected());
            }
        });
        editDescriptionButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String oldDescription = "";
                for (String line : skillTree.getDescription()) {
                    if (!oldDescription.equals("")) {
                        oldDescription += "\n";
                    }
                    oldDescription += line;
                }

                JTextArea msg = new JTextArea(oldDescription);
                msg.setRows(5);
                msg.setColumns(50);
                msg.setLineWrap(true);
                msg.setWrapStyleWord(true);

                JScrollPane scrollPane = new JScrollPane(msg);
                JOptionPane.showMessageDialog(null, scrollPane, "Edit Skilltree Description", JOptionPane.QUESTION_MESSAGE);

                String[] description = msg.getText().split("\\n");

                skillTree.clearDescription();
                skillTree.addDescription(description);
            }
        });
        editIconButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                short id, damage;
                boolean glowing = false;

                id = skillTree.getIconItem().getAs("id", TagShort.class).getShortData();
                damage = skillTree.getIconItem().getAs("Damage", TagShort.class).getShortData();
                if (skillTree.getIconItem().getCompoundData().containsKey("tag")) {
                    TagCompound tag = skillTree.getIconItem().getAs("tag", TagCompound.class);
                    glowing = tag.getCompoundData().containsKey("ench");
                }

                JPanel iconPanel = new JPanel();

                iconPanel.add(new JLabel("ID: "));
                JTextField idTextField = new JTextField("" + id);
                idTextField.setColumns(4);
                iconPanel.add(idTextField);

                iconPanel.add(new JLabel("Damage: "));
                JTextField damageTextField = new JTextField("" + damage);
                damageTextField.setColumns(4);
                iconPanel.add(damageTextField);

                JCheckBox glowingCheckbox = new JCheckBox("Glowing: ");
                glowingCheckbox.setHorizontalTextPosition(SwingConstants.LEFT);
                glowingCheckbox.setSelected(glowing);
                iconPanel.add(glowingCheckbox);

                JOptionPane.showMessageDialog(null, iconPanel, "Edit Skilltree Icon Item", JOptionPane.QUESTION_MESSAGE);

                damage = -1;
                if (Util.isShort(damageTextField.getText())) {
                    damage = Short.parseShort(damageTextField.getText());
                }
                id = -1;
                if (Util.isShort(idTextField.getText())) {
                    id = Short.parseShort(idTextField.getText());
                }
                glowing = glowingCheckbox.isSelected();

                skillTree.setIconItem(id, damage, glowing);

                if (id >= 298 && id <= 301) {
                    int color = 0;
                    if (skillTree.getIconItem().getCompoundData().containsKey("tag")) {
                        TagCompound tag = skillTree.getIconItem().getAs("tag", TagCompound.class);
                        //CompoundMap tag = ((TagCompound) values.get("tag")).getValue();
                        if (tag.getCompoundData().containsKey("display")) {
                            TagCompound display = tag.getAs("tag", TagCompound.class);
                            if (display.getCompoundData().containsKey("color")) {
                                color = display.getAs("color", TagInt.class).getIntData();
                            }
                        }
                    }

                    JPanel colorPanel = new JPanel();

                    iconPanel.add(new JLabel("Color: "));
                    JTextField colorTextField = new JTextField("" + color);
                    colorTextField.setColumns(19);
                    colorPanel.add(colorTextField);

                    JOptionPane.showMessageDialog(null, colorPanel, "Edit Leather Armor Color", JOptionPane.QUESTION_MESSAGE);

                    if (Util.isInt(colorTextField.getText())) {
                        color = Integer.parseInt(colorTextField.getText());
                    }
                    color = Math.max(0, color);

                    TagCompound tag, display;
                    if (!skillTree.getIconItem().getCompoundData().containsKey("tag")) {
                        tag = new TagCompound();
                        skillTree.getIconItem().getCompoundData().put("tag", tag);
                    } else {
                        tag = skillTree.getIconItem().getAs("tag", TagCompound.class);
                    }
                    if (!skillTree.getIconItem().getCompoundData().containsKey("display")) {
                        display = new TagCompound();
                        tag.getCompoundData().put("display", display);
                    } else {
                        display = tag.get("display");
                    }
                    display.getCompoundData().put("color", new TagInt(color));
                }
            }
        });
    }

    public void registerSkillPanels() {
        skillPanels.put(BeaconInfo.class, new Beacon());
        skillPanels.put(BehaviorInfo.class, new Behavior());
        skillPanels.put(DamageInfo.class, new Damage());
        skillPanels.put(FireInfo.class, new Fire());
        skillPanels.put(LifeInfo.class, new Life());
        skillPanels.put(HealInfo.class, new Heal());
        skillPanels.put(InventoryInfo.class, new Inventory());
        skillPanels.put(KnockbackInfo.class, new Knockback());
        skillPanels.put(LightningInfo.class, new Lightning());
        skillPanels.put(PickupInfo.class, new Pickup());
        skillPanels.put(PoisonInfo.class, new Poison());
        skillPanels.put(RangedInfo.class, new Ranged());
        skillPanels.put(RideInfo.class, new Ride());
        skillPanels.put(ShieldInfo.class, new Shield());
        skillPanels.put(SlowInfo.class, new Slow());
        skillPanels.put(StompInfo.class, new Stomp());
        skillPanels.put(ThornsInfo.class, new Thorns());
        skillPanels.put(WitherInfo.class, new Wither());
    }

    public JPanel getMainPanel() {
        return levelCreatorPanel;
    }

    public JFrame getFrame() {
        if (levelCreatorFrame == null) {
            levelCreatorFrame = new JFrame("LevelCreator - MyPet " + MyPetVersion.getVersion());
        }
        return levelCreatorFrame;
    }

    public void setSkillTree(SkillTree skillTree, SkillTreeMobType skillTreeMobType) {
        this.skillTree = skillTree;
        this.skillTreeMobType = skillTreeMobType;
        highestLevel = 0;

        if (skillTree.hasDisplayName()) {
            displayNameTextField.setEnabled(true);
            displayNameCheckbox.setSelected(true);
        } else {
            displayNameTextField.setEnabled(false);
            displayNameCheckbox.setSelected(false);
        }
        displayNameTextField.setText(skillTree.getDisplayName());
        if (skillTree.hasCustomPermissions()) {
            permissionTextField.setEnabled(true);
            permissionCheckbox.setSelected(true);
        } else {
            permissionTextField.setEnabled(false);
            permissionCheckbox.setSelected(false);
        }
        permissionTextField.setText(skillTree.getPermission());
        permissionDisplayTextField.setText("MyPet.skilltree." + skillTree.getPermission());

        if (skillTree.getMaxLevel() > 0) {
            maxLevelCheckBox.setSelected(true);
            maxLevelTextField.setEnabled(true);
            maxLevelTextField.setText("" + skillTree.getMaxLevel());
        } else {
            maxLevelCheckBox.setSelected(false);
            maxLevelTextField.setEnabled(false);
            maxLevelTextField.setText("");
        }
        if (skillTree.getRequiredLevel() > 1) {
            requiredLevelCheckBox.setSelected(true);
            requiredLevelTextField.setEnabled(true);
            requiredLevelTextField.setText("" + skillTree.getRequiredLevel());
        } else {
            requiredLevelCheckBox.setSelected(false);
            requiredLevelTextField.setEnabled(false);
            requiredLevelTextField.setText("");
        }

        this.inheritanceComboBoxModel.removeAllElements();

        inheritanceCheckBox.setSelected(false);
        inheritanceCheckBox.setEnabled(false);
        if (skillTreeMobType.getSkillTreeNames().size() > 1 || (skillTreeMobType != SkillTreeMobType.DEFAULT && SkillTreeMobType.DEFAULT.getSkillTreeNames().size() > 0)) {
            inheritanceCheckBox.setEnabled(true);
            ArrayList<String> skilltreeNames = new ArrayList<>();
            for (String skillTreeName : skillTreeMobType.getSkillTreeNames()) {
                if (!skillTreeName.equals(skillTree.getName()) && !skilltreeNames.contains(skillTreeName)) {
                    skilltreeNames.add(skillTreeName);
                    inheritanceComboBoxModel.addElement(skillTreeName);
                }
            }
            for (String skillTreeName : SkillTreeMobType.DEFAULT.getSkillTreeNames()) {
                if (!skillTreeName.equals(skillTree.getName()) && !skilltreeNames.contains(skillTreeName)) {
                    skilltreeNames.add(skillTreeName);
                    inheritanceComboBoxModel.addElement(skillTreeName);
                }
            }
            if (skillTree.getInheritance() != null) {
                if (skillTreeMobType.getSkillTreeNames().contains(skillTree.getInheritance())) {
                    inheritanceCheckBox.setSelected(true);
                    inheritanceComboBox.setEnabled(true);
                    this.inheritanceComboBoxModel.setSelectedItem(skillTree.getInheritance());
                }
                if (SkillTreeMobType.DEFAULT.hasSkillTree(skillTree.getInheritance())) {
                    inheritanceCheckBox.setSelected(true);
                    inheritanceComboBox.setEnabled(true);
                    this.inheritanceComboBoxModel.setSelectedItem(skillTree.getInheritance());
                }
            } else {
                inheritanceComboBox.setEnabled(false);
            }
        }

        skillTreeNameLabel.setText("Skilltree: " + skillTree.getName());
        SortedDefaultMutableTreeNode rootNode = new SortedDefaultMutableTreeNode(skillTree.getName());
        skillTreeTreeModel.setRoot(rootNode);
        int skillcount = 0;
        for (SkillTreeLevel level : skillTree.getLevelList()) {
            highestLevel = Math.max(highestLevel, level.getLevel());
            DefaultMutableTreeNode levelNode = new DefaultMutableTreeNode(level.getLevel());
            rootNode.add(levelNode);
            for (SkillInfo skill : level.getSkills()) {
                SkillTreeSkillNode skillNode = new SkillTreeSkillNode(skill);
                levelNode.add(skillNode);
                skillcount++;
            }
        }

        if (skillcount <= 15) {
            for (int i = 0; i < skillTreeTree.getRowCount(); i++) {
                skillTreeTree.expandRow(i);
            }
        } else {
            skillTreeTree.expandRow(0);
        }
        skillTreeTree.updateUI();
        skillTreeTree.setSelectionPath(new TreePath(rootNode));
    }

    private void createUIComponents() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("");
        skillTreeTreeModel = new DefaultTreeModel(root);
        skillTreeTree = new JTree(skillTreeTreeModel);
        skillTreeTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

        createRightclickMenus();

        inheritanceComboBoxModel = new DefaultComboBoxModel();
        inheritanceComboBox = new JComboBox(inheritanceComboBoxModel);
    }

    public void createRightclickMenus() {
        levelListRightclickMenu = new JPopupMenu();

        JMenuItem expandMenuItem = new JMenuItem("Expand all");
        levelListRightclickMenu.add(expandMenuItem);
        expandMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                for (int i = 0; i < skillTreeTree.getRowCount(); i++) {
                    skillTreeTree.expandRow(i);
                }
            }
        });

        JMenuItem collapseMenuItem = new JMenuItem("Collapse all");
        levelListRightclickMenu.add(collapseMenuItem);
        collapseMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                for (int i = 1; i < skillTreeTree.getRowCount(); i++) {
                    skillTreeTree.collapseRow(i);
                }
            }
        });

        MouseListener popupListener = new SkilltreeCreator.PopupListener(levelListRightclickMenu);
        skillTreeTree.addMouseListener(popupListener);
    }

    private class SkillTreeSkillNode extends DefaultMutableTreeNode {
        private SkillInfo skill;

        public SkillTreeSkillNode(SkillInfo skill) {
            super(skill.getName());
            this.skill = skill;
        }

        public SkillInfo getSkill() {
            return skill;
        }
    }

    private class SortedDefaultMutableTreeNode extends DefaultMutableTreeNode {
        public SortedDefaultMutableTreeNode(Object userObject) {
            super(userObject);
        }

        @SuppressWarnings("unchecked")
        public void add(DefaultMutableTreeNode newChild) {
            super.add(newChild);
            Collections.sort(this.children, nodeComparator);
        }

        protected Comparator nodeComparator = new Comparator() {
            public int compare(Object o1, Object o2) {
                if (Util.isInt(o1.toString()) && Util.isInt(o2.toString())) {
                    int n1 = Integer.parseInt(o1.toString());
                    int n2 = Integer.parseInt(o2.toString());
                    if (n1 < n2) {
                        return -1;
                    } else if (n1 == n2) {
                        return 0;
                    }
                    if (n1 > n2) {
                        return 1;
                    }
                }
                return o1.toString().compareToIgnoreCase(o2.toString());
            }

            public boolean equals(Object obj) {
                return false;
            }
        };
    }
}