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

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.event.*;
import java.io.File;

public class SkilltreeCreator
{
    private JComboBox mobTypeComboBox;
    private JButton addSkilltreeButton;
    private JButton deleteSkilltreeButton;
    private JList skilltreeList;
    private JButton skilltreeDownButton;
    private JButton skilltreeUpButton;
    private JPanel skilltreeCreatorPanel;
    private JButton saveButton;

    private DefaultListModel skillTreeListModel;

    private static LevelCreator levelCreator;
    private static SkilltreeCreator skilltreeCreator;
    private static BukkitDownloader bukkitDownloader;
    private static JFrame levelCreatorFrame;
    private static JFrame bukkitDownloaderFrame;
    private static JFrame skilltreeCreatorFrame;

    public SkilltreeCreator()
    {
        mobTypeComboBox.addItemListener(new ItemListener()
        {
            public void itemStateChanged(ItemEvent e)
            {
                if (e.getStateChange() == ItemEvent.SELECTED)
                {
                    if (mobTypeComboBox.getItemAt(0).equals(""))
                    {
                        mobTypeComboBox.removeItemAt(0);
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
                MyPetSkillTreeConfig.getMobType(mobTypeComboBox.getSelectedItem().toString()).moveSkillTreeUp(skilltreeList.getSelectedValue().toString());
                String skillTreeName = (String) skillTreeListModel.get(skilltreeList.getSelectedIndex() - 1);
                skillTreeListModel.set(skilltreeList.getSelectedIndex() - 1, skillTreeListModel.get(skilltreeList.getSelectedIndex()));
                skillTreeListModel.set(skilltreeList.getSelectedIndex(), skillTreeName);
                skilltreeList.setSelectedIndex(skilltreeList.getSelectedIndex() - 1);
                saveButton.setEnabled(true);
            }
        });
        skilltreeDownButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                MyPetSkillTreeConfig.getMobType(mobTypeComboBox.getSelectedItem().toString()).moveSkillTreeDown(skilltreeList.getSelectedValue().toString());
                String skillTreeName = (String) skillTreeListModel.get(skilltreeList.getSelectedIndex() + 1);
                skillTreeListModel.set(skilltreeList.getSelectedIndex() + 1, skillTreeListModel.get(skilltreeList.getSelectedIndex()));
                skillTreeListModel.set(skilltreeList.getSelectedIndex(), skillTreeName);
                skilltreeList.setSelectedIndex(skilltreeList.getSelectedIndex() + 1);
                saveButton.setEnabled(true);
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
                            MyPetSkillTreeConfig.getMobType(mobTypeComboBox.getSelectedItem().toString()).addSkillTree(skillTree);
                            skilltreeList.setSelectedIndex(skillTreeListModel.getSize() - 1);
                            deleteSkilltreeButton.setEnabled(true);
                            saveButton.setEnabled(true);
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
                MyPetSkillTreeConfig.getMobType(mobTypeComboBox.getSelectedItem().toString()).removeSkillTree(skilltreeList.getSelectedValue().toString());
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
                saveButton.setEnabled(true);
            }
        });
        skilltreeList.addMouseListener(new MouseAdapter()
        {
            public void mouseClicked(MouseEvent evt)
            {
                if (evt.getClickCount() == 2)
                {

                    levelCreator.setSkillTree((MyPetSkillTree) MyPetSkillTreeConfig.getMobType(mobTypeComboBox.getSelectedItem().toString()).getSkillTree(skilltreeList.getSelectedValue().toString()), MyPetSkillTreeConfig.getMobType(mobTypeComboBox.getSelectedItem().toString()));
                    levelCreatorFrame.setVisible(true);
                    skilltreeCreatorFrame.setEnabled(false);
                }
            }
        });
        saveButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                saveButton.setEnabled(false);
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
                        levelCreator.setSkillTree((MyPetSkillTree) MyPetSkillTreeConfig.getMobType(mobTypeComboBox.getSelectedItem().toString()).getSkillTree(skilltreeList.getSelectedValue().toString()), MyPetSkillTreeConfig.getMobType(mobTypeComboBox.getSelectedItem().toString()));
                        levelCreatorFrame.setVisible(true);
                        skilltreeCreatorFrame.setEnabled(false);
                    case KeyEvent.VK_DELETE:
                        int index = skilltreeList.getSelectedIndex();
                        MyPetSkillTreeConfig.getMobType(mobTypeComboBox.getSelectedItem().toString()).removeSkillTree(skilltreeList.getSelectedValue().toString());
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
                        saveButton.setEnabled(true);
                }
            }
        });
    }

    public static void main(String[] args)
    {
        String path = SkilltreeCreator.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        path = path.replace("/MyPet.jar", "").replace("/", File.separator).substring(1);
        File bukkitFile = new File(path);

        try
        {
            Class.forName("org.bukkit.configuration.file.FileConfiguration");
        }
        catch (ClassNotFoundException e)
        {
            String[] buttons = {"Cancel", "Download CraftBukkit"};
            int result = JOptionPane.showOptionDialog(null, "Can't find a CraftBukkit executable\n" +
                    "\nin one of these folders:" +
                    "\n   " + bukkitFile.getAbsolutePath() +
                    "\n   " + bukkitFile.getParent(), "Skilltree-Creator", JOptionPane.ERROR_MESSAGE, 0, null, buttons, buttons[1]);

            if (result == 0)
            {
                System.exit(0);
            }
            else if (result == 1)
            {
                bukkitDownloader = new BukkitDownloader();
                bukkitDownloaderFrame = new JFrame("Bukkit Downloader");
                bukkitDownloaderFrame.setContentPane(bukkitDownloader.downloaderPanel);
                bukkitDownloaderFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                bukkitDownloaderFrame.pack();
                bukkitDownloaderFrame.setVisible(true);
                bukkitDownloaderFrame.setLocationRelativeTo(null);

                bukkitDownloader.startDownload();
            }
            return;
        }
        MyPetSkillTreeConfig.setConfigPath(bukkitFile.getAbsolutePath() + File.separator + "MyPet" + File.separator + "skilltrees");
        MyPetSkillTreeConfig.loadSkillTrees();

        skilltreeCreator = new SkilltreeCreator();
        skilltreeCreatorFrame = new JFrame("SkilltreeCreator");
        skilltreeCreatorFrame.setContentPane(skilltreeCreator.skilltreeCreatorPanel);
        skilltreeCreatorFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        skilltreeCreatorFrame.pack();
        skilltreeCreatorFrame.setVisible(true);
        skilltreeCreatorFrame.setLocationRelativeTo(null);

        levelCreator = new LevelCreator(skilltreeCreatorFrame, skilltreeCreator.saveButton);
        levelCreatorFrame = levelCreator.getFrame();
        levelCreatorFrame.setContentPane(levelCreator.getMainPanel());
        levelCreatorFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        levelCreatorFrame.pack();
        levelCreatorFrame.setLocationRelativeTo(null);
        levelCreatorFrame.addWindowListener(new WindowListener()
        {
            public void windowOpened(WindowEvent e)
            {
            }

            public void windowClosing(WindowEvent e)
            {
                skilltreeCreatorFrame.setEnabled(true);
            }

            public void windowClosed(WindowEvent e)
            {
            }

            public void windowIconified(WindowEvent e)
            {
            }

            public void windowDeiconified(WindowEvent e)
            {
            }

            public void windowActivated(WindowEvent e)
            {
            }

            public void windowDeactivated(WindowEvent e)
            {
            }
        });
    }

    private void createUIComponents()
    {
        skillTreeListModel = new DefaultListModel();
        skilltreeList = new JList(skillTreeListModel);
    }
}
