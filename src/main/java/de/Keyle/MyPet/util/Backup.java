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

package de.Keyle.MyPet.util;

import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.util.logger.DebugLogger;
import org.bukkit.Bukkit;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.concurrent.TimeUnit;

public class Backup
{
    public static int SAVE_INTERVAL = 1440;
    public static String DATE_FORMAT = "yyyy_MM_dd_HH.mm";
    public static boolean MAKE_BACKUPS = true;

    File backupFile;
    File backupFolder;
    long lastBackup;

    public Backup(File backupFile, File backupFolder)
    {
        this.backupFile = backupFile;
        this.backupFolder = backupFolder;

        try
        {
            String lastBackup = Util.readFileAsString(backupFolder.getAbsolutePath() + File.separator + "lastbackup");
            if (Util.isLong(lastBackup))
            {
                this.lastBackup = Long.parseLong(lastBackup);
                long difference = System.currentTimeMillis() - this.lastBackup;
                long minutes = TimeUnit.MILLISECONDS.toMinutes(difference);
                if (minutes >= SAVE_INTERVAL)
                {
                    createBackup();
                }
            }
        }
        catch (IOException e)
        {
            DebugLogger.info("Creating first My.Pets backup.");
            createBackup();
        }
        Bukkit.getScheduler().scheduleSyncRepeatingTask(MyPetPlugin.getPlugin(), new Runnable()
        {
            public void run()
            {
                createBackup();
            }
        }, 20L * 60L * (SAVE_INTERVAL - TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - this.lastBackup)), 20L * 60L * SAVE_INTERVAL);
    }

    public String createBackup()
    {
        lastBackup = System.currentTimeMillis();
        try
        {
            PrintWriter out = new PrintWriter(backupFolder.getAbsolutePath() + File.separator + "lastbackup");
            out.print(lastBackup);
            out.close();
            return backupFile();
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        return "[No Backup Created!]";
    }

    public String backupFile()
    {
        SimpleDateFormat df = new SimpleDateFormat(DATE_FORMAT);
        File destFile = new File(backupFolder, df.format(lastBackup) + "_My.Pets");
        DebugLogger.info("Creating database (My.Pets) backup -> " + df.format(lastBackup) + "_My.Pets");
        try
        {
            copyFile(backupFile, destFile);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return df.format(lastBackup) + "_My.Pets";
    }

    public static void copyFile(File source, File dest) throws IOException
    {
        if (source == null || !source.exists() || dest == null)
        {
            return;
        }
        if (!dest.exists())
        {
            dest.createNewFile();
        }
        InputStream in = null;
        OutputStream out = null;
        try
        {
            in = new FileInputStream(source);
            out = new FileOutputStream(dest);

            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0)
            {
                out.write(buf, 0, len);
            }
        }
        finally
        {
            if (in != null)
            {
                in.close();
            }
            if (out != null)
            {
                out.close();
            }
        }
    }
}