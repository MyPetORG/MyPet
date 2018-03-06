/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2018 Keyle
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

package de.Keyle.MyPet.skilltreecreator;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class GuiMain {
    public static String configPath;

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
        File defaultSkilltreePath = new File(configPath + "skilltrees" + File.separator);


        //SkillTreeLoaderNBT.getSkilltreeLoader().loadSkillTrees(configPath + "skilltrees", petTypes);
        //SkillTreeLoaderJSON.getSkilltreeLoader().loadSkillTrees(configPath + "skilltrees", petTypes);

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        Image logoImage = new ImageIcon(ClassLoader.getSystemResource("gui/assets/img/logo_100.png")).getImage();
        final JFileChooser fc = new JFileChooser() {
            @Override
            protected JDialog createDialog(Component parent) throws HeadlessException {
                JDialog dialog = super.createDialog(parent);
                dialog.setIconImage(logoImage);
                return dialog;
            }
        };
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (defaultSkilltreePath.exists()) {
            fc.setCurrentDirectory(defaultSkilltreePath);
        }
        int returnVal = fc.showOpenDialog(null);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            try {
                new WebServer(fc.getSelectedFile());
                Desktop.getDesktop().browse(new URI("http://localhost:64712"));
            } catch (IOException | URISyntaxException e) {
                e.printStackTrace();
            }
        }
    }
}