/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2019 Keyle
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
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.prefs.Preferences;

public class Main {
    public static String configPath;
    public static WebServer webServer = null;
    public static TrayIcon trayIcon = null;

    static class MyPetPlugin {
    }

    public static void main(String[] args) {
        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("You can not run this without a graphical interface!");
            return;
        }

        String path = "";
        try {
            path = Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        path = path.replace("/", File.separator);
        path = path.replaceAll(String.format("\\%s[^\\%s]*\\.jar", File.separator, File.separator), "");
        File pluginDirFile = new File(path);
        configPath = pluginDirFile.getAbsolutePath() + File.separator + "MyPet" + File.separator;
        File defaultSkilltreePath = new File(configPath + "skilltrees" + File.separator);

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        Image logoImage = Toolkit
                .getDefaultToolkit()
                .getImage(ClassLoader.getSystemResource("gui/assets/img/logo_16.png"));

        if (!portAvailable(64712)) {
            String message = "Can't start the SkilltreeCreator. Another instance might be running.";
            System.out.println(message);
            JOptionPane.showMessageDialog(null, message, "SkilltreeCreator", JOptionPane.INFORMATION_MESSAGE);
            System.exit(0);
        }

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
        } else {
            String lastPath = Preferences.userNodeForPackage(MyPetPlugin.class).get("LastOpenedPath", "NO-PATH");
            if (!lastPath.equals("NO-PATH")) {
                defaultSkilltreePath = new File(lastPath);
                fc.setCurrentDirectory(defaultSkilltreePath);
            }
        }
        int returnVal = fc.showOpenDialog(null);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            try {
                Preferences.userNodeForPackage(MyPetPlugin.class).put("LastOpenedPath", fc.getSelectedFile().getAbsolutePath());
                webServer = new WebServer(fc.getSelectedFile());
                Desktop.getDesktop().browse(new URI("http://localhost:64712"));
                if (SystemTray.isSupported()) {
                    createTraymenu(logoImage);
                }
            } catch (IOException | URISyntaxException e) {
                e.printStackTrace();
            }
        }
    }

    public static void close() {
        webServer.stop();
        System.exit(0);
    }

    private static void createTraymenu(Image logoImage) {
        trayIcon = new TrayIcon(logoImage);
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.addActionListener(e -> {
            close();
        });
        MenuItem reopenItem = new MenuItem("Reopen");
        reopenItem.addActionListener(e -> {
            try {
                Desktop.getDesktop().browse(new URI("http://localhost:64712"));
            } catch (URISyntaxException | IOException e2) {
                e2.printStackTrace();
            }
        });
        PopupMenu popup = new PopupMenu();
        popup.add(reopenItem);
        popup.add(exitItem);
        trayIcon.setPopupMenu(popup);
        trayIcon.setToolTip("MyPet - SkilltreeCreator");
        try {
            SystemTray.getSystemTray().add(trayIcon);
        } catch (AWTException e) {
            e.printStackTrace();
        }
        trayIcon.displayMessage("MyPet - SkilltreeCreator", "The SkilltreeCreator is running. You can exit it via the tray icon.", TrayIcon.MessageType.INFO);
    }

    public static boolean portAvailable(int port) {
        ServerSocket ss = null;
        DatagramSocket ds = null;
        //noinspection TryFinallyCanBeTryWithResources
        try {
            ss = new ServerSocket(port);
            ss.setReuseAddress(true);
            ds = new DatagramSocket(port);
            ds.setReuseAddress(true);
            return true;
        } catch (IOException ignored) {
        } finally {
            if (ds != null) {
                ds.close();
            }
            if (ss != null) {
                try {
                    ss.close();
                } catch (IOException ignored) {
                }
            }
        }
        return false;
    }
}