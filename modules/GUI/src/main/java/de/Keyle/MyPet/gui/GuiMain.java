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

package de.Keyle.MyPet.gui;

import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.skill.SkillsInfo;
import de.Keyle.MyPet.api.skill.skills.*;
import de.Keyle.MyPet.gui.skilltreecreator.LevelCreator;
import de.Keyle.MyPet.gui.skilltreecreator.SkillPropertyEditor;
import de.Keyle.MyPet.gui.skilltreecreator.SkilltreeCreator;
import de.Keyle.MyPet.skill.skilltreeloader.SkillTreeLoaderNBT;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.net.URISyntaxException;

public class GuiMain {
    public static LevelCreator levelCreator;
    public static SkilltreeCreator skilltreeCreator;
    public static SkillPropertyEditor skillPropertyEditor;
    public static String configPath;

    public static String[] petTypes = new String[]{"default", "Bat", "Blaze", "CaveSpider", "Chicken", "Cow", "Creeper", "EnderDragon", "Enderman", "Endermite", "Evoker", "Ghast", "Giant", "Guardian", "Horse", "IronGolem", "Llama", "MagmaCube", "Mooshroom", "Ocelot", "Pig", "PigZombie", "PolarBear", "Rabbit", "Sheep", "Silverfish", "Skeleton", "Slime", "Snowman", "Spider", "Squid", "Witch", "Wither", "Wolf", "Vex", "Villager", "Vindicator", "Zombie"};

    public static void main(String[] args) {
        String path = "";
        try {
            path = GuiMain.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        path = path.replace("/", File.separator);
        path = path.replaceAll(String.format("\\%s[^\\%s]*\\.jar", File.separator, File.separator), "");
        File pluginDirFile = new File(path);
        configPath = pluginDirFile.getAbsolutePath() + File.separator + "MyPet" + File.separator;

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        Image logoImage = new ImageIcon(ClassLoader.getSystemResource("images/logo.png")).getImage();

        registerSkillsInfo();

        Configuration.LevelSystem.Experience.LEVEL_CAP = Integer.MAX_VALUE;

        new File(configPath + "skilltrees" + File.separator).mkdirs();

        SkillTreeLoaderNBT.getSkilltreeLoader().loadSkillTrees(configPath + "skilltrees", petTypes);

        skilltreeCreator = new SkilltreeCreator();
        final JFrame skilltreeCreatorFrame = skilltreeCreator.getFrame();
        skilltreeCreatorFrame.setContentPane(skilltreeCreator.getMainPanel());
        skilltreeCreatorFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        skilltreeCreatorFrame.setIconImage(logoImage);
        skilltreeCreatorFrame.pack();
        skilltreeCreatorFrame.setVisible(true);
        skilltreeCreatorFrame.setLocationRelativeTo(null);
        skilltreeCreatorFrame.addWindowListener(new WindowListener() {
            public void windowOpened(WindowEvent e) {
            }

            public void windowClosing(WindowEvent e) {
                int result = JOptionPane.showConfirmDialog(skilltreeCreatorFrame, "Are you sure that you want to close the SkilltreeCreator?", "Skilltree-Creator", JOptionPane.OK_CANCEL_OPTION);
                if (result == 0) {
                    System.exit(0);
                }
            }

            public void windowClosed(WindowEvent e) {
            }

            public void windowIconified(WindowEvent e) {
            }

            public void windowDeiconified(WindowEvent e) {
            }

            public void windowActivated(WindowEvent e) {
            }

            public void windowDeactivated(WindowEvent e) {
            }
        });

        levelCreator = new LevelCreator();
        final JFrame levelCreatorFrame = levelCreator.getFrame();
        levelCreatorFrame.setContentPane(levelCreator.getMainPanel());
        levelCreatorFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        levelCreatorFrame.setIconImage(logoImage);
        levelCreatorFrame.pack();
        levelCreatorFrame.setLocationRelativeTo(null);
        levelCreatorFrame.addWindowListener(new WindowListener() {
            public void windowOpened(WindowEvent e) {
            }

            public void windowClosing(WindowEvent e) {
                skilltreeCreatorFrame.setEnabled(true);
            }

            public void windowClosed(WindowEvent e) {
            }

            public void windowIconified(WindowEvent e) {
            }

            public void windowDeiconified(WindowEvent e) {
            }

            public void windowActivated(WindowEvent e) {
            }

            public void windowDeactivated(WindowEvent e) {
            }
        });

        skillPropertyEditor = new SkillPropertyEditor();
        final JFrame skillPropertyEditorFrame = skillPropertyEditor.getFrame();
        skillPropertyEditorFrame.setContentPane(skillPropertyEditor.getMainPanel());
        skillPropertyEditorFrame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        skillPropertyEditorFrame.setIconImage(logoImage);
        skillPropertyEditorFrame.pack();
        skillPropertyEditorFrame.setLocationRelativeTo(null);
        skillPropertyEditorFrame.addWindowListener(new WindowListener() {
            public void windowOpened(WindowEvent e) {
            }

            public void windowClosing(WindowEvent e) {
                levelCreatorFrame.setEnabled(true);
            }

            public void windowClosed(WindowEvent e) {
            }

            public void windowIconified(WindowEvent e) {
            }

            public void windowDeiconified(WindowEvent e) {
            }

            public void windowActivated(WindowEvent e) {
            }

            public void windowDeactivated(WindowEvent e) {
            }
        });
    }

    public static void registerSkillsInfo() {
        SkillsInfo.registerSkill(BeaconInfo.class);
        SkillsInfo.registerSkill(BehaviorInfo.class);
        SkillsInfo.registerSkill(ControlInfo.class);
        SkillsInfo.registerSkill(DamageInfo.class);
        SkillsInfo.registerSkill(FireInfo.class);
        SkillsInfo.registerSkill(LifeInfo.class);
        SkillsInfo.registerSkill(HealInfo.class);
        SkillsInfo.registerSkill(InventoryInfo.class);
        SkillsInfo.registerSkill(KnockbackInfo.class);
        SkillsInfo.registerSkill(LightningInfo.class);
        SkillsInfo.registerSkill(PickupInfo.class);
        SkillsInfo.registerSkill(PoisonInfo.class);
        SkillsInfo.registerSkill(RangedInfo.class);
        SkillsInfo.registerSkill(RideInfo.class);
        SkillsInfo.registerSkill(SlowInfo.class);
        SkillsInfo.registerSkill(SprintInfo.class);
        SkillsInfo.registerSkill(ThornsInfo.class);
        SkillsInfo.registerSkill(WitherInfo.class);
        SkillsInfo.registerSkill(StompInfo.class);
    }
}