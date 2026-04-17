package de.Keyle.MyPet.migration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mongodb.client.MongoDatabase;
import de.Keyle.MyPet.MyPetApi;
import org.bson.Document;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class MigrationBackupService {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String[] TABLES = {"pets", "players", "info"};

    private final Logger logger;
    private File backupDir;

    public MigrationBackupService(Logger logger) {
        this.logger = logger;
    }

    public File initBackupDir(String version) {
        String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        backupDir = new File(MyPetApi.getPlugin().getDataFolder(),
                "backups/migration-" + version + "-" + timestamp);
        if (!backupDir.mkdirs() && !backupDir.isDirectory()) {
            throw new IllegalStateException("Failed to create backup directory: " + backupDir);
        }
        return backupDir;
    }

    public File getBackupDir() {
        return backupDir;
    }

    public void backupSqliteFile(File dbFile) throws IOException {
        File dbBackupDir = new File(backupDir, "database");
        dbBackupDir.mkdirs();
        logger.info("[MyPet] Backing up database...");
        Files.copy(dbFile.toPath(),
                new File(dbBackupDir, dbFile.getName()).toPath(),
                StandardCopyOption.REPLACE_EXISTING);
    }

    public void backupMysqlTables(Connection connection, String tablePrefix) throws Exception {
        File dbBackupDir = new File(backupDir, "database");
        dbBackupDir.mkdirs();
        logger.info("[MyPet] Backing up database...");

        for (String table : TABLES) {
            String fullTable = tablePrefix + table;
            List<Map<String, Object>> rows = new ArrayList<>();
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM " + fullTable)) {
                ResultSetMetaData meta = rs.getMetaData();
                int colCount = meta.getColumnCount();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= colCount; i++) {
                        Object value = rs.getObject(i);
                        if (value instanceof byte[]) {
                            row.put(meta.getColumnName(i), Base64.getEncoder().encodeToString((byte[]) value));
                        } else {
                            row.put(meta.getColumnName(i), value);
                        }
                    }
                    rows.add(row);
                }
            }
            try (Writer writer = new FileWriter(new File(dbBackupDir, fullTable + ".json"))) {
                GSON.toJson(rows, writer);
            }
        }
    }

    public void backupMongoCollections(MongoDatabase database, String collectionPrefix) throws IOException {
        File dbBackupDir = new File(backupDir, "database");
        dbBackupDir.mkdirs();
        logger.info("[MyPet] Backing up database...");

        for (String collection : TABLES) {
            String fullCollection = collectionPrefix + collection;
            List<String> docs = new ArrayList<>();
            for (Document doc : database.getCollection(fullCollection).find()) {
                docs.add(doc.toJson());
            }
            try (Writer writer = new FileWriter(new File(dbBackupDir, fullCollection + ".json"))) {
                writer.write("[\n");
                for (int i = 0; i < docs.size(); i++) {
                    writer.write(docs.get(i));
                    if (i < docs.size() - 1) {
                        writer.write(",\n");
                    }
                }
                writer.write("\n]");
            }
        }
    }

    public void backupConfigFiles(File dataFolder) throws IOException {
        File configBackupDir = new File(backupDir, "config");
        configBackupDir.mkdirs();
        logger.info("[MyPet] Backing up config files...");

        File[] ymlFiles = dataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (ymlFiles == null) {
            return;
        }
        for (File file : ymlFiles) {
            Files.copy(file.toPath(),
                    new File(configBackupDir, file.getName()).toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public void backupSkilltreeFiles(File skilltreeDir) throws IOException {
        File stBackupDir = new File(backupDir, "skilltrees");
        stBackupDir.mkdirs();
        logger.info("[MyPet] Backing up skilltree files...");

        File[] stFiles = skilltreeDir.listFiles((dir, name) -> name.endsWith(".st.json"));
        if (stFiles == null) {
            return;
        }
        for (File file : stFiles) {
            Files.copy(file.toPath(),
                    new File(stBackupDir, file.getName()).toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
