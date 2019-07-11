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


import org.nanohttpd.protocols.http.tempfiles.DefaultTempFileManagerFactory;
import org.nanohttpd.protocols.http.tempfiles.ITempFile;
import org.nanohttpd.protocols.http.tempfiles.ITempFileManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

import static org.nanohttpd.protocols.http.NanoHTTPD.safeClose;

public class FileManager extends DefaultTempFileManagerFactory {

    File tmpdir = new File(System.getProperty("java.io.tmpdir"));

    @Override
    public ITempFileManager create() {
        return new TempFileManager();
    }

    class TempFileManager implements ITempFileManager {

        private final List<TimerTempFile> tempFiles;

        public TempFileManager() {
            if (!tmpdir.exists()) {
                tmpdir.mkdirs();
            }
            this.tempFiles = new ArrayList<>();
        }

        @Override
        public void clear() {
            for (TimerTempFile file : this.tempFiles) {
                file.delete();
            }
            this.tempFiles.clear();
        }

        @Override
        public TimerTempFile createTempFile(String filename_hint) throws Exception {
            TimerTempFile tempFile = new TimerTempFile(tmpdir);
            this.tempFiles.add(tempFile);
            return tempFile;
        }
    }

    static class TimerTempFile implements ITempFile {

        private static Set<File> undeletedFiles = Collections.synchronizedSet(new HashSet<>());

        static {
            Timer timer = new Timer(true);
            timer.schedule(new DeleteTempFileTask(), 1000, 1000);
        }

        private final File file;

        private final OutputStream fstream;

        public TimerTempFile(File tempdir) throws IOException {
            this.file = File.createTempFile("NanoHTTPD-", "", tempdir);
            this.fstream = new FileOutputStream(this.file);
        }

        @Override
        public void delete() {
            try {
                safeClose(this.fstream);
            } catch (Exception ignored) {
            }
            undeletedFiles.add(file);
        }

        @Override
        public String getName() {
            return this.file.getAbsolutePath();
        }

        @Override
        public OutputStream open() {
            return this.fstream;
        }

        static class DeleteTempFileTask extends TimerTask {
            @Override
            public void run() {
                System.gc();
                undeletedFiles.removeIf(File::delete);
            }
        }
    }
}
