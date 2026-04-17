package de.Keyle.MyPet.migration.migrations;

import de.Keyle.MyPet.api.migration.ConfigMigration;
import de.Keyle.MyPet.api.migration.ConfigMigrationContext;
import de.Keyle.MyPet.api.migration.Migration;
import de.Keyle.MyPet.api.migration.MigrationException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Converts legacy color-code strings in MyPet's YAML config files to MiniMessage format.
 * Handles {@code config.yml} (pet name tag prefix/suffix) and {@code pet-shops.yml} (shop
 * and pet names, descriptions). The v4 runtime parses all of these values through
 * MiniMessage, so any lingering {@code &}/{@code §} codes render as literal text.
 * <p>
 * Idempotent: converted values no longer contain legacy codes, so a second run does nothing.
 */
@Migration(
        version = "4.0.0",
        description = "Convert legacy color codes in MyPet YAML config files to MiniMessage"
)
public class MigrateConfigColorCodesToMiniMessage implements ConfigMigration {

    private static final Logger LOG = Logger.getLogger("MyPet");

    private static final String MAIN_CONFIG = "config.yml";
    private static final String[] MAIN_CONFIG_KEYS = {
            "MyPet.Name.Tag.Prefix",
            "MyPet.Name.Tag.Suffix",
    };

    private static final String PET_SHOPS_CONFIG = "pet-shops.yml";

    private static final Pattern LEGACY_CODE = Pattern.compile("[§&][0-9a-fk-orxA-FK-ORX]");

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private int totalConverted;

    @Override
    public void migrate(ConfigMigrationContext ctx) throws MigrationException {
        totalConverted = 0;
        migrateMainConfig(ctx);
        migratePetShops(ctx);
        LOG.info("Config color-code migration complete.");
        LOG.info("  Total strings converted: " + totalConverted + ".");
    }

    // --------------------------------------------------------------------------------
    // config.yml — fixed list of known keys
    // --------------------------------------------------------------------------------

    private void migrateMainConfig(ConfigMigrationContext ctx) throws MigrationException {
        if (!fileExists(ctx, MAIN_CONFIG)) {
            LOG.info("" + MAIN_CONFIG + " not found — skipping main config color migration.");
            return;
        }
        YamlConfiguration config = ctx.getConfig(MAIN_CONFIG);
        boolean changed = false;
        for (String key : MAIN_CONFIG_KEYS) {
            if (!config.contains(key)) {
                continue;
            }
            changed |= convertStringAt(config, key);
        }
        if (changed) {
            ctx.saveConfig(MAIN_CONFIG);
        }
    }

    // --------------------------------------------------------------------------------
    // pet-shops.yml — walk Shops.*.Name, Shops.*.Pets.*.Name, Shops.*.Pets.*.Description[]
    // --------------------------------------------------------------------------------

    private void migratePetShops(ConfigMigrationContext ctx) throws MigrationException {
        if (!fileExists(ctx, PET_SHOPS_CONFIG)) {
            LOG.info("" + PET_SHOPS_CONFIG + " not found — skipping pet-shops color migration.");
            return;
        }
        YamlConfiguration config = ctx.getConfig(PET_SHOPS_CONFIG);
        ConfigurationSection shops = config.getConfigurationSection("Shops");
        if (shops == null) {
            return;
        }

        boolean changed = false;
        for (String shopKey : shops.getKeys(false)) {
            ConfigurationSection shop = shops.getConfigurationSection(shopKey);
            if (shop == null) {
                continue;
            }
            changed |= convertStringAt(shop, "Name");

            ConfigurationSection pets = shop.getConfigurationSection("Pets");
            if (pets == null) {
                continue;
            }
            for (String petKey : pets.getKeys(false)) {
                ConfigurationSection pet = pets.getConfigurationSection(petKey);
                if (pet == null) {
                    continue;
                }
                changed |= convertStringAt(pet, "Name");
                changed |= convertStringListAt(pet, "Description");
            }
        }

        if (changed) {
            ctx.saveConfig(PET_SHOPS_CONFIG);
        }
    }

    // --------------------------------------------------------------------------------
    // Shared helpers
    // --------------------------------------------------------------------------------

    private boolean convertStringAt(ConfigurationSection section, String key) {
        String oldValue = section.getString(key);
        if (oldValue == null || oldValue.isEmpty() || !hasLegacyCode(oldValue)) {
            return false;
        }
        try {
            String newValue = convert(oldValue);
            section.set(key, newValue);
            totalConverted++;
            LOG.info("Converted " + fullPath(section, key)
                    + ": " + oldValue + "  →  " + newValue);
            return true;
        } catch (Exception e) {
            LOG.warning("Failed to convert " + fullPath(section, key)
                    + ": " + e.getClass().getSimpleName() + ": " + e.getMessage()
                    + " — leaving unchanged");
            return false;
        }
    }

    private boolean convertStringListAt(ConfigurationSection section, String key) {
        List<String> list = section.getStringList(key);
        if (list.isEmpty()) {
            return false;
        }
        List<String> newList = new ArrayList<>(list.size());
        boolean anyChanged = false;
        for (int i = 0; i < list.size(); i++) {
            String line = list.get(i);
            if (line == null || !hasLegacyCode(line)) {
                newList.add(line);
                continue;
            }
            try {
                String converted = convert(line);
                newList.add(converted);
                totalConverted++;
                anyChanged = true;
                LOG.info("Converted " + fullPath(section, key) + "[" + i + "]"
                        + ": " + line + "  →  " + converted);
            } catch (Exception e) {
                LOG.warning("Failed to convert " + fullPath(section, key) + "[" + i + "]"
                        + ": " + e.getClass().getSimpleName() + ": " + e.getMessage()
                        + " — leaving unchanged");
                newList.add(line);
            }
        }
        if (anyChanged) {
            section.set(key, newList);
        }
        return anyChanged;
    }

    private String fullPath(ConfigurationSection section, String key) {
        String current = section.getCurrentPath();
        return (current == null || current.isEmpty()) ? key : current + "." + key;
    }

    private boolean fileExists(ConfigMigrationContext ctx, String filename) {
        return new File(ctx.getDataFolder(), filename).exists();
    }

    private static boolean hasLegacyCode(String value) {
        return LEGACY_CODE.matcher(value).find();
    }

    private static String convert(String oldValue) {
        // Normalize § to & so one legacy serializer handles both. Both prefix characters
        // use the same code alphabet, so a straight character replace is safe.
        String normalized = oldValue.replace('§', '&');
        Component component = LEGACY.deserialize(normalized);
        return MINI_MESSAGE.serialize(component);
    }
}
