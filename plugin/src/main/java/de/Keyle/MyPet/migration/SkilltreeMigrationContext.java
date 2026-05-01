package de.Keyle.MyPet.migration;

import com.google.gson.JsonObject;

import java.io.File;
import java.util.List;

public interface SkilltreeMigrationContext {
    List<File> getSkilltreeFiles();

    JsonObject readSkilltree(File file) throws MigrationException;

    void writeSkilltree(File file, JsonObject data) throws MigrationException;
}
