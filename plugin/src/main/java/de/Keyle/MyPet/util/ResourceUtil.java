package de.Keyle.MyPet.util;

import de.Keyle.MyPet.api.util.ErrorUtil;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

public final class ResourceUtil {

    private ResourceUtil() {
    }

    public static boolean copyResource(Plugin plugin, String resource, File destination) {
        try (InputStream template = plugin.getResource(resource);
             OutputStream out = Files.newOutputStream(destination.toPath())) {
            if (template == null) {
                return false;
            }
            template.transferTo(out);
            return true;
        } catch (IOException e) {
            ErrorUtil.report(e);
            return false;
        }
    }
}
