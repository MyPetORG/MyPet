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

package de.Keyle.MyPet.api.util.locale;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.util.Colorizer;
import de.Keyle.MyPet.api.util.ComponentColorizer;
import de.Keyle.MyPet.api.util.ErrorUtil;
import de.Keyle.MyPet.api.util.MiniMessageColorizer;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class Translation {
    private static Translation instance = null;

    private final Map<String, Language> languages = new HashMap<>();

    private Translation() {
    }

    public static void init() {
        instance = new Translation();
    }

    public static String getString(String key, Player player) {
        if (player == null) {
            return key;
        }

        return getString(key, MyPetApi.getPlatformHelper().getPlayerLanguage(player));
    }

    public static String getString(String key, CommandSender sender) {
        if (sender == null) {
            return key;
        }
        if (sender instanceof Player) {
            return getString(key, (Player) sender);
        }

        return getString(key, "en");
    }

    public static String getString(String key, MyPetPlayer player) {
        if (player == null) {
            return key;
        }

        return getString(key, player.getLanguage());
    }

    public static String getString(String key, String localeString) {
        if (instance == null) {
            return key;
        }
        if (!Configuration.Misc.OVERWRITE_LANGUAGE.equalsIgnoreCase("")) {
            localeString = Configuration.Misc.OVERWRITE_LANGUAGE;
        }

        return instance.getText(key, localeString);
    }

    // ========== Component-Based Translation Methods (Adventure API) ==========

    /**
     * Gets translation as an Adventure Component for a Player.
     * Respects player's client language setting.
     *
     * @param key    Translation key
     * @param player The player whose language to use
     * @return Translation as Component with colors applied
     */
    public static Component getComponent(String key, Player player) {
        if (player == null) {
            return Component.text(key);
        }

        return getComponent(key, MyPetApi.getPlatformHelper().getPlayerLanguage(player));
    }

    /**
     * Gets translation as an Adventure Component for a CommandSender.
     * For Players, respects their language. For console, defaults to English.
     *
     * @param key    Translation key
     * @param sender The command sender
     * @return Translation as Component with colors applied
     */
    public static Component getComponent(String key, CommandSender sender) {
        if (sender == null) {
            return Component.text(key);
        }
        if (sender instanceof Player) {
            return getComponent(key, (Player) sender);
        }

        return getComponent(key, "en");
    }

    /**
     * Gets translation as an Adventure Component for a MyPetPlayer.
     * Uses the stored player language preference.
     *
     * @param key    Translation key
     * @param player The MyPet player whose language to use
     * @return Translation as Component with colors applied
     */
    public static Component getComponent(String key, MyPetPlayer player) {
        if (player == null) {
            return Component.text(key);
        }

        return getComponent(key, player.getLanguage());
    }

    /**
     * Gets translation as an Adventure Component for a specific locale.
     * This is the base method that all other Component methods delegate to.
     *
     * @param key          Translation key
     * @param localeString Locale string (e.g., "en", "de_DE", "fr")
     * @return Translation as Component with colors applied
     */
    public static Component getComponent(String key, String localeString) {
        if (instance == null) {
            return Component.text(key);
        }
        if (!Configuration.Misc.OVERWRITE_LANGUAGE.equalsIgnoreCase("")) {
            localeString = Configuration.Misc.OVERWRITE_LANGUAGE;
        }

        return instance.getComponentText(key, localeString);
    }

    // ========== MiniMessage-Based Translation Methods (Advanced Formatting) ==========

    /**
     * Gets translation as an Adventure Component with MiniMessage support for a Player.
     * Respects player's client language setting.
     *
     * @param key    Translation key
     * @param player The player whose language to use
     * @return Translation as Component with MiniMessage formatting applied
     */
    public static Component getComponentMiniMessage(String key, Player player) {
        if (player == null) {
            return Component.text(key);
        }

        return getComponentMiniMessage(key, MyPetApi.getPlatformHelper().getPlayerLanguage(player));
    }

    /**
     * Gets translation as an Adventure Component with MiniMessage support for a CommandSender.
     * For Players, respects their language. For console, defaults to English.
     *
     * @param key    Translation key
     * @param sender The command sender
     * @return Translation as Component with MiniMessage formatting applied
     */
    public static Component getComponentMiniMessage(String key, CommandSender sender) {
        if (sender == null) {
            return Component.text(key);
        }
        if (sender instanceof Player) {
            return getComponentMiniMessage(key, (Player) sender);
        }

        return getComponentMiniMessage(key, "en");
    }

    /**
     * Gets translation as an Adventure Component with MiniMessage support for a MyPetPlayer.
     * Uses the stored player language preference.
     *
     * @param key    Translation key
     * @param player The MyPet player whose language to use
     * @return Translation as Component with MiniMessage formatting applied
     */
    public static Component getComponentMiniMessage(String key, MyPetPlayer player) {
        if (player == null) {
            return Component.text(key);
        }

        return getComponentMiniMessage(key, player.getLanguage());
    }

    /**
     * Gets translation as an Adventure Component with MiniMessage support for a specific locale.
     * This is the base method that all other MiniMessage methods delegate to.
     *
     * @param key          Translation key
     * @param localeString Locale string (e.g., "en", "de_DE", "fr")
     * @return Translation as Component with MiniMessage formatting applied
     */
    public static Component getComponentMiniMessage(String key, String localeString) {
        if (instance == null) {
            return Component.text(key);
        }
        if (!Configuration.Misc.OVERWRITE_LANGUAGE.equalsIgnoreCase("")) {
            localeString = Configuration.Misc.OVERWRITE_LANGUAGE;
        }

        return instance.getComponentTextMiniMessage(key, localeString);
    }

    // ========== Core translation + placeholder normalization ==========

    /**
     * Internal method that retrieves the raw translation text without color processing.
     * This is used by both legacy getString() and modern getComponent() methods.
     * <p>
     * On top of the underlying language lookup, this method also normalizes
     * known placeholder mistakes from existing locale files, e.g. replacing
     * &lt;r&gt; with &lt;reset&gt;.
     *
     * @param key          Translation key
     * @param localeString Locale string (e.g., "en", "de_DE")
     * @return Raw translation text (no color codes processed) with placeholders normalized
     */
    private String getRawText(String key, String localeString) {
        String[] codes = localeString.toLowerCase().split("_");

        String languageCode = codes[0];

        if (!languages.containsKey(languageCode)) {
            languages.put(languageCode, new Language(languageCode));
        }

        Language language = languages.get(languageCode);

        String translatedPhrase = key;
        if (codes.length >= 2) {
            translatedPhrase = language.translate(key, codes[1]);
        }
        if (translatedPhrase.equals(key)) {
            translatedPhrase = language.translate(key);
        }
        if (translatedPhrase.equals(key) && !languageCode.equals("en")) {
            translatedPhrase = getRawText(key, "en");
        }

        // Normalize any incorrect or legacy placeholders from the locale files.
        return normalizePlaceholders(translatedPhrase);
    }

    /**
     * Legacy method that retrieves translation with ChatColor formatting.
     * Maintained for backward compatibility.
     *
     * @param key          Translation key
     * @param localeString Locale string (e.g., "en", "de_DE")
     * @return Translation with ChatColor codes applied
     */
    public String getText(String key, String localeString) {
        return Colorizer.setColors(getRawText(key, localeString));
    }

    /**
     * Modern method that retrieves translation as an Adventure Component.
     * Parses MyPet color codes (<red>, &c, etc.) into proper Components.
     *
     * @param key          Translation key
     * @param localeString Locale string (e.g., "en", "de_DE")
     * @return Translation as Adventure Component with colors applied
     */
    private Component getComponentText(String key, String localeString) {
        return ComponentColorizer.parseToComponent(getRawText(key, localeString));
    }

    /**
     * Advanced method that retrieves translation as an Adventure Component with MiniMessage support.
     * Parses MiniMessage tags (gradients, hover, click, etc.) and falls back to legacy color codes.
     *
     * @param key          Translation key
     * @param localeString Locale string (e.g., "en", "de_DE")
     * @return Translation as Adventure Component with MiniMessage formatting applied
     */
    private Component getComponentTextMiniMessage(String key, String localeString) {
        return MiniMessageColorizer.parseToComponent(getRawText(key, localeString));
    }

    /**
     * Normalizes common placeholder / tag mistakes from existing locale files so they
     * work correctly with MiniMessage and the colorizers.
     * <p>
     * At the moment this handles:
     * - &lt;r&gt;  → &lt;reset&gt;
     * <p>
     * You can extend this method if you discover more broken patterns in shipped locales.
     *
     * @param input Raw translation string from the bundle
     * @return Normalized string
     */
    private static String normalizePlaceholders(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        String fixed = input;

        // Fix legacy/broken reset tag used in older locale files.
        // Example: "<gold>{0}<r>" → "<gold>{0}<reset>"
        fixed = fixed.replace("<r>", "<reset>");

        // Additional normalization rules can be added here if needed:
        // - Converting legacy color codes to MiniMessage tags
        // - Fixing malformed tags, etc.

        return fixed;
    }

    // ========== Locale file loading ==========

    /**
     * Loads a locale bundle from the plugin JAR and the data folder.
     * After loading, all values are passed through {@link #normalizePlaceholders(String)}
     * to fix known placeholder/tag mistakes at load time.
     *
     * This means downstream users of the bundle (e.g. Language, translators) will
     * see the corrected values.
     */
    public static TranslationBundle loadLocale(String localeString) {

        JarFile jarFile;
        try {
            jarFile = new JarFile(MyPetApi.getPlugin().getFile());
        } catch (IOException ignored) {
            return new TranslationBundle();
        }

        TranslationBundle newLocale = new TranslationBundle();
        try {
            JarEntry jarEntry = jarFile.getJarEntry("locale/MyPet_" + localeString + ".properties");
            if (jarEntry != null) {
                newLocale.load(new InputStreamReader(jarFile.getInputStream(jarEntry), StandardCharsets.UTF_8));
            }
        } catch (UnsupportedEncodingException e) {
            ErrorUtil.report(e);
        } catch (IOException ignored) {
        }

        File localeFile = new File(MyPetApi.getPlugin().getDataFolder() + File.separator + "locale" + File.separator + "MyPet_" + localeString + ".properties");
        if (localeFile.exists()) {
            try {
                newLocale.load(new InputStreamReader(Files.newInputStream(localeFile.toPath()), StandardCharsets.UTF_8));
            } catch (IOException e) {
                ErrorUtil.report(e);
            }
        }

        // Normalize all loaded values so any incorrect placeholders in the
        // .properties are fixed immediately after load.
        normalizeBundlePlaceholders(newLocale);

        return newLocale;
    }

    /**
     * Applies {@link #normalizePlaceholders(String)} to every entry in the given bundle.
     * This assumes TranslationBundle is backed by a java.util.Properties-like structure.
     */
    private static void normalizeBundlePlaceholders(TranslationBundle bundle) {
        try {
            for (Map.Entry<String, String> entry : bundle.translations.entrySet()) {
                String value = entry.getValue();
                if (value != null) {
                    String fixed = normalizePlaceholders(value);
                    if (!fixed.equals(value)) {
                        entry.setValue(fixed);
                    }
                }
            }
        } catch (UnsupportedOperationException ignored) {
            // If TranslationBundle does not support entrySet() mutation, we silently skip.
            // getRawText() still normalizes placeholders on access, so behavior is correct.
        }
    }
}