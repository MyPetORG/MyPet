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

package de.Keyle.MyPet.gui;

import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.gui.skillcreator.BukkitDownloader;
import de.Keyle.MyPet.gui.skillcreator.LevelCreator;
import de.Keyle.MyPet.gui.skillcreator.SkillPropertyEditor;
import de.Keyle.MyPet.gui.skillcreator.SkilltreeCreator;
import de.Keyle.MyPet.skill.skilltreeloader.MyPetSkillTreeLoaderJSON;
import de.Keyle.MyPet.skill.skilltreeloader.MyPetSkillTreeLoaderNBT;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.net.URISyntaxException;

public class GuiMain
{
    public static LevelCreator levelCreator;
    public static SkilltreeCreator skilltreeCreator;
    public static BukkitDownloader bukkitDownloader;
    public static SkillPropertyEditor skillPropertyEditor;
    public static String configPath;

    public static void main(String[] args)
    {
        String path = "";
        try
        {
            path = GuiMain.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
        }
        catch (URISyntaxException e)
        {
            e.printStackTrace();
        }
        path = path.replace("/", File.separator);
        path = path.replaceAll(String.format("\\%s[^\\%s]*\\.jar", File.separator, File.separator), "");
        File pluginDirFile = new File(path);
        configPath = pluginDirFile.getAbsolutePath() + File.separator + "MyPet" + File.separator;

        try
        {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (Exception ignored)
        {
        }
        Image logoImage = new ImageIcon(ClassLoader.getSystemResource("resources/logo.png")).getImage();
        if (!canFindBukkit())
        {
            String[] buttons = {"Exit", "Download CraftBukkit", "Choose a CraftBukkit.jar"};
            int result = JOptionPane.showOptionDialog(null, "Can't find a CraftBukkit executable\n" +
                    "\nin one of these folders:" +
                    "\n   " + pluginDirFile.getAbsolutePath() + File.separator + "MyPet" + File.separator +
                    "\n   " + pluginDirFile.getAbsolutePath() + File.separator +
                    "\n   " + pluginDirFile.getParent() + File.separator, "Skilltree-Creator", JOptionPane.ERROR_MESSAGE, 0, null, buttons, buttons[1]);

            if (result == 0)
            {
                System.exit(0);
            }
            else if (result == 1)
            {
                bukkitDownloader = new BukkitDownloader();
                JFrame bukkitDownloaderFrame = bukkitDownloader.getFrame();
                bukkitDownloaderFrame.setContentPane(bukkitDownloader.getMainPanel());
                bukkitDownloaderFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                bukkitDownloaderFrame.setIconImage(logoImage);
                bukkitDownloaderFrame.pack();
                bukkitDownloaderFrame.setVisible(true);
                bukkitDownloaderFrame.setLocationRelativeTo(null);

                bukkitDownloader.startDownload();
            }
            else if (result == 2)
            {
                JFileChooser fileChooser = new JFileChooser(new File(path));
                fileChooser.setAcceptAllFileFilterUsed(false);
                fileChooser.setFileFilter(new FileFilter()
                {
                    @Override
                    public boolean accept(File f)
                    {
                        return f.isDirectory() || f.getName().matches(".*\\.(jar)");
                    }

                    @Override
                    public String getDescription()
                    {
                        return "Craftbukkit (*.jar)";
                    }
                });
                if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
                {
                    try
                    {
                        Runtime.getRuntime().exec("java -cp \"" + fileChooser.getSelectedFile().getAbsolutePath() + "\"" + System.getProperties().getProperty("path.separator") + "\"" + (SkilltreeCreator.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()) + "\" de.Keyle.MyPet.gui.skillcreator.SkilltreeCreator");
                    }
                    catch (Exception e1)
                    {
                        e1.printStackTrace();
                    }
                }
                System.exit(0);
            }
            return;
        }

        MyPetPlugin.registerSkills();


        new File(configPath + "skilltrees" + File.separator).mkdirs();
        MyPetSkillTreeLoaderNBT.getSkilltreeLoader().loadSkillTrees(configPath + "skilltrees", false);
        MyPetSkillTreeLoaderJSON.getSkilltreeLoader().loadSkillTrees(configPath + "skilltrees", false);

        skilltreeCreator = new SkilltreeCreator();
        final JFrame skilltreeCreatorFrame = skilltreeCreator.getFrame();
        skilltreeCreatorFrame.setContentPane(skilltreeCreator.getMainPanel());
        skilltreeCreatorFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        skilltreeCreatorFrame.setIconImage(logoImage);
        skilltreeCreatorFrame.pack();
        skilltreeCreatorFrame.setVisible(true);
        skilltreeCreatorFrame.setLocationRelativeTo(null);
        skilltreeCreatorFrame.addWindowListener(new WindowListener()
        {
            public void windowOpened(WindowEvent e)
            {
            }

            public void windowClosing(WindowEvent e)
            {
                int result = JOptionPane.showConfirmDialog(skilltreeCreatorFrame, "Are you sure that you want to close the SkilltreeCreator?", "Skilltree-Creator", JOptionPane.OK_CANCEL_OPTION);
                if (result == 0)
                {
                    System.exit(0);
                }
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

        levelCreator = new LevelCreator();
        final JFrame levelCreatorFrame = levelCreator.getFrame();
        levelCreatorFrame.setContentPane(levelCreator.getMainPanel());
        levelCreatorFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        levelCreatorFrame.setIconImage(logoImage);
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

        skillPropertyEditor = new SkillPropertyEditor();
        final JFrame skillPropertyEditorFrame = skillPropertyEditor.getFrame();
        skillPropertyEditorFrame.setContentPane(skillPropertyEditor.getMainPanel());
        skillPropertyEditorFrame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        skillPropertyEditorFrame.setIconImage(logoImage);
        skillPropertyEditorFrame.pack();
        skillPropertyEditorFrame.setLocationRelativeTo(null);
        skillPropertyEditorFrame.addWindowListener(new WindowListener()
        {
            public void windowOpened(WindowEvent e)
            {
            }

            public void windowClosing(WindowEvent e)
            {
                levelCreatorFrame.setEnabled(true);
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

    private static boolean canFindBukkit()
    {
        try
        {
            Class.forName("net.minecraft.server.v{@MINECRAFT_VERSION_UNDERSCORE@}.NBTBase");
            return true;
        }
        catch (Exception e)
        {
            return false;
        }
    }
}