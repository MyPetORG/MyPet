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
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.util.ErrorUtil;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.Style;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Translation {
    private static Translation instance = null;

    private final Map<String, Language> languages = new HashMap<>();

    private Translation() {
    }

    public static void init() {
        instance = new Translation();
    }

    /**
     * Resolves a player's client locale, falling back to {@code "en_us"} when
     * Bukkit returns an empty string.
     */
    public static String getPlayerLanguage(Player player) {
        String locale = player.getLocale();
        if (locale.isEmpty()) {
            return "en_us";
        }
        return locale;
    }

    /**
     * Resolves a command sender's locale: the player's client locale for
     * {@link Player} senders, {@code "en"} for console.
     */
    public static String getCommandSenderLanguage(CommandSender sender) {
        if (sender instanceof Player player) {
            return getPlayerLanguage(player);
        }
        return "en";
    }

    public static String getString(String key, Player player) {
        if (player == null) {
            return key;
        }

        return getString(key, getPlayerLanguage(player));
    }

    public static String getString(String key, CommandSender sender) {
        if (sender == null) {
            return key;
        }
        if (sender instanceof Player p) {
            return getString(key, p);
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

        return getComponent(key, getPlayerLanguage(player));
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
        if (sender instanceof Player p) {
            return getComponent(key, p);
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

    // ========== Formatted Component Methods (Translation + Placeholder Substitution) ==========

    /**
     * Gets a translated Component and replaces placeholders {0}, {1}, etc. with provided arguments.
     * Arguments can be Components (inserted directly) or Objects (deserialized via SANITIZED_MINIMESSAGE).
     *
     * @param key    Translation key
     * @param player Player for language detection
     * @param values Arguments to replace placeholders
     * @return Formatted Component
     */
    public static Component getFormattedComponent(String key, Player player, Object... values) {
        return formatComponent(getComponent(key, player), values);
    }

    /**
     * Gets a translated and formatted Component for a CommandSender.
     */
    public static Component getFormattedComponent(String key, CommandSender sender, Object... values) {
        return formatComponent(getComponent(key, sender), values);
    }

    /**
     * Gets a translated and formatted Component for a MyPetPlayer.
     */
    public static Component getFormattedComponent(String key, MyPetPlayer player, Object... values) {
        return formatComponent(getComponent(key, player), values);
    }

    /**
     * Gets a translated and formatted Component for a specific locale.
     */
    public static Component getFormattedComponent(String key, String localeString, Object... values) {
        return formatComponent(getComponent(key, localeString), values);
    }

    // ========== Component Placeholder Substitution ==========

    /**
     * Formats a Component by replacing placeholders {0}, {1}, {2}... with provided arguments.
     * Arguments can be Components (inserted with their styling) or Objects (converted via SANITIZED_MINIMESSAGE).
     */
    private static Component formatComponent(Component component, Object... values) {
        if (component == null || values == null || values.length == 0) {
            return component != null ? component : Component.empty();
        }
        return replaceInComponent(component, values);
    }

    /**
     * Recursively processes a Component tree to replace placeholders.
     */
    private static Component replaceInComponent(Component component, Object[] values) {
        if (component == null) {
            return Component.empty();
        }

        TextComponent.Builder builder = Component.text().style(component.style());

        if (component instanceof TextComponent textComponent) {
            String content = textComponent.content();
            List<Component> replacedContent = replacePlaceholders(content, textComponent.style(), values);
            for (Component part : replacedContent) {
                builder.append(part);
            }
        } else {
            builder.append(component);
        }

        for (Component child : component.children()) {
            builder.append(replaceInComponent(child, values));
        }

        return builder.build();
    }

    /**
     * Replaces placeholders in a text string and returns a list of Components.
     */
    private static List<Component> replacePlaceholders(String text, Style style, Object[] values) {
        List<Component> result = new ArrayList<>();

        if (text == null || text.isEmpty()) {
            return result;
        }

        int lastIndex = 0;
        Pattern pattern = Pattern.compile("\\{(\\d+)}");
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            if (matcher.start() > lastIndex) {
                result.add(Component.text(text.substring(lastIndex, matcher.start())).style(style));
            }

            int index = Integer.parseInt(matcher.group(1));

            if (index < values.length && values[index] != null) {
                Object value = values[index];
                if (value instanceof Component) {
                    result.add((Component) value);
                } else {
                    result.add(Util.SANITIZED_MINIMESSAGE.deserialize(value.toString()));
                }
            } else {
                result.add(Component.text(matcher.group()).style(style));
            }

            lastIndex = matcher.end();
        }

        if (lastIndex < text.length()) {
            result.add(Component.text(text.substring(lastIndex)).style(style));
        }

        if (result.isEmpty() && !text.isEmpty()) {
            result.add(Component.text(text).style(style));
        }

        return result;
    }

    // ========== Core translation + placeholder normalization ==========

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

        // Locale files use camelCase color names (e.g. <darkblue>) but MiniMessage
        // requires underscore-separated names (e.g. <dark_blue>). Convert them.
        fixed = fixed.replace("<darkblue>", "<dark_blue>");
        fixed = fixed.replace("<darkgreen>", "<dark_green>");
        fixed = fixed.replace("<darkaqua>", "<dark_aqua>");
        fixed = fixed.replace("<darkred>", "<dark_red>");
        fixed = fixed.replace("<darkpurple>", "<dark_purple>");
        fixed = fixed.replace("<darkgray>", "<dark_gray>");
        fixed = fixed.replace("<lightpurple>", "<light_purple>");

        return fixed;
    }

    /**
     * Loads a locale bundle from the plugin JAR and the data folder.
     * After loading, all values are passed through {@link #normalizePlaceholders(String)}
     * to fix known placeholder/tag mistakes at load time.
     * <p>
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
     * Retrieves translation text with raw {@code <COLOR>} tags preserved.
     * Callers that display to players should use {@link #getComponent} instead.
     *
     * @param key          Translation key
     * @param localeString Locale string (e.g., "en", "de_DE")
     * @return Translation text with {@code <COLOR>} tags
     */
    public String getText(String key, String localeString) {
        return getRawText(key, localeString);
    }

    // ========== Locale file loading ==========

    /**
     * Modern method that retrieves translation as an Adventure Component.
     * Parses MyPet color codes (<red>, &c, etc.) into proper Components.
     *
     * @param key          Translation key
     * @param localeString Locale string (e.g., "en", "de_DE")
     * @return Translation as Adventure Component with colors applied
     */
    private Component getComponentText(String key, String localeString) {
        return MiniMessage.miniMessage().deserialize(getRawText(key, localeString));
    }
}