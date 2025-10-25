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

package de.Keyle.MyPet.api.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class LogFormat extends Formatter {
    @Override
    public String format(LogRecord record) {
        String text = new SimpleDateFormat("MM-dd-yyyy HH:mm").format(new Date(record.getMillis()));
        Level level = record.getLevel();

        if (level == Level.WARNING) {
            text += " [WARNING]";
        } else if (level == Level.SEVERE) {
            text += " [SEVERE]";
        } else {
            text += " [INFO]";
        }

        text += " " + record.getMessage();
        text += "\r\n";

        Throwable thrown = record.getThrown();
        if (thrown != null) {
            StringWriter stringWriter = new StringWriter();
            thrown.printStackTrace(new PrintWriter(stringWriter));
            text += stringWriter;
        }

        text = text.replaceAll("(\\u001b\\[\\d{1,3}(?:;\\d+)*m|(?:\\u001b\\[m)*)", "");
        text = Colorizer.stripColors(text);

        return text;
    }
}