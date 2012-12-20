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

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class BukkitDownloader
{
    JProgressBar downloadProgressBar;
    JButton cancelButton;
    JButton restartButton;
    JPanel downloaderPanel;
    JLabel progressLabel;
    JLabel nameLabel;
    JFrame bukkitDownloaderFrame;

    private static final String downloadAddress = "http://dl.bukkit.org/latest-rb/craftbukkit.jar";
    InternetDataTask downloader;

    public BukkitDownloader()
    {
        cancelButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                if (downloader != null)
                {
                    downloader.cancel(true);
                }
            }
        });
        restartButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                try
                {
                    Runtime.getRuntime().exec("java -jar MyPet.jar");
                }
                catch (IOException e1)
                {
                    e1.printStackTrace();
                }
                System.exit(0);
            }
        });
    }

    public void startDownload()
    {
        downloader = new InternetDataTask();
        downloader.run();
    }

    class InternetDataTask extends SwingWorker<String, Void>
    {
        @Override
        protected String doInBackground() throws Exception
        {
            String path = SkilltreeCreator.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            path = path.replace("/MyPet.jar", "").replace("/", File.separator).substring(1);
            path += File.separator + "MyPet";
            nameLabel.setText("<HTML>Downloading lates Craftbukkit RB<BR>   from: dl.bukkit.org<BR>   to: " + path + "\\craftbukkit.jar</HTML>");
            File bukkitPath = new File(path);
            bukkitPath.mkdirs();

            URL url = new URL(downloadAddress);
            int size = url.openConnection().getContentLength();
            if (size == -1)
            {
                JOptionPane.showMessageDialog(null, "Can't download CraftBukkit!", "Downloading CraftBukkit", JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            }
            progressLabel.setText("Downloaded: 0/" + String.format("%.2f", size / 1024F / 1024F) + "MiB");
            InputStream reader = url.openStream();

            FileOutputStream writer = new FileOutputStream(bukkitPath.getAbsolutePath() + File.separator + "craftbukkit.jar");
            byte[] buffer = new byte[153600];
            int totalBytesRead = 0;
            int bytesRead;

            while ((bytesRead = reader.read(buffer)) > 0)
            {
                writer.write(buffer, 0, bytesRead);
                buffer = new byte[153600];
                totalBytesRead += bytesRead;
                downloadProgressBar.setValue((int) ((totalBytesRead / 1024F / 1024F) * 100 / (size / 1024F / 1024F)));
                progressLabel.setText("Downloaded: " + String.format("%.2f", totalBytesRead / 1024F / 1024F) + "/" + String.format("%.2f", size / 1024F / 1024F) + "MiB");
                if (isCancelled())
                {
                    writer.close();
                    reader.close();
                    File deletePath = new File(path + File.separator + "craftbukkit.jar");
                    deletePath.delete();
                    System.exit(0);
                }
            }
            writer.close();
            reader.close();

            return null;
        }

        @Override
        protected void done()
        {
            restartButton.setEnabled(true);
            cancelButton.setEnabled(false);
        }
    }

    public JPanel getMainPanel()
    {
        return downloaderPanel;
    }

    public JFrame getFrame()
    {
        if (bukkitDownloaderFrame == null)
        {
            bukkitDownloaderFrame = new JFrame("Bukkit Downloader");
        }
        return bukkitDownloaderFrame;
    }
}