/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2026 Keyle
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
import de.Keyle.MyPet.api.MyPetGlobal;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.util.ErrorUtil;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.Translator;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MyPet's translation facility. Combines two roles:
 *
 * <ul>
 *   <li><b>Static facade</b> — {@code getComponent}, {@code getFormattedComponent},
 *       {@code renderPlain}, etc. that callers across the codebase use to look up
 *       a translation for a player, command sender, or explicit locale tag.</li>
 *   <li><b>Adventure {@link Translator} implementation</b> — a single instance is
 *       registered with {@link GlobalTranslator} on {@link #init()}, so that any
 *       {@link Component#translatable(String) Component.translatable(...)} created
 *       with a MyPet key resolves through this class regardless of where in the
 *       codebase it is rendered.</li>
 * </ul>
 *
 * <p>Bundles are loaded from {@code locale/MyPet_<tag>.properties} entries inside
 * the plugin JAR, with sparse per-key overlay from {@code plugins/MyPet/locale/}
 * files of the same name. Values may contain MiniMessage tags and
 * {@code {0}}/{@code {1}}-style placeholders that are substituted with the
 * arguments passed via {@link TranslatableComponent#arguments()}.</p>
 */
public final class Locale implements Translator {

    private static final Key NAME = Key.key("mypet", "messages");
    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final Pattern BUNDLE_FILENAME = Pattern.compile("MyPet_([a-zA-Z0-9_\\-]+)\\.properties");
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{(\\d+)}");

    /** Single instance — created and registered by {@link #init()}. */
    private static Locale instance;

    /** Outer key = locale tag like "en", "en_us", "de_de" (lowercased). Inner key = translation key (lowercased). */
    private final Map<String, Map<String, Component>> bundles = new HashMap<>();

    private Locale() {
    }

    /**
     * Loads all translation bundles and registers this class as a translation source
     * with {@link GlobalTranslator}. Idempotent — safe to call from both
     * {@code MyPetPlugin.onEnable} and {@code /mypet reload}; an existing registration
     * is removed before a fresh one is added.
     */
    public static void init() {
        if (instance != null) {
            GlobalTranslator.translator().removeSource(instance);
        }
        instance = new Locale();
        instance.loadAllBundles();
        GlobalTranslator.translator().addSource(instance);
    }

    // ========== Bukkit locale extractors ==========

    /**
     * Resolves a player's client locale, falling back to {@code "en_us"} when
     * Bukkit returns an empty string.
     */
    public static String getPlayerLanguage(Player player) {
        java.util.Locale locale = player.locale();
        String tag = locale.toString().toLowerCase();
        if (tag.isEmpty()) {
            return "en_us";
        }
        return tag;
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

    // ========== Static facade — what callers use ==========

    public static Component getComponent(String key, Player player) {
        if (player == null) {
            return Component.text(key);
        }
        return getComponent(key, getPlayerLanguage(player));
    }

    public static Component getComponent(String key, CommandSender sender) {
        if (sender == null) return Component.text(key);
        return getComponent(key, getCommandSenderLanguage(sender));
    }

    public static Component getComponent(String key, MyPetPlayer player) {
        if (player == null) {
            return Component.text(key);
        }
        return getComponent(key, player.getLanguage());
    }

    public static Component getComponent(String key, String localeString) {
        return renderTranslatable(key, localeString);
    }

    public static Component getFormattedComponent(String key, Player player, Object... values) {
        if (player == null) return Component.text(key);
        return getFormattedComponent(key, getPlayerLanguage(player), values);
    }

    public static Component getFormattedComponent(String key, CommandSender sender, Object... values) {
        if (sender == null) return Component.text(key);
        return getFormattedComponent(key, getCommandSenderLanguage(sender), values);
    }

    public static Component getFormattedComponent(String key, MyPetPlayer player, Object... values) {
        if (player == null) return Component.text(key);
        return getFormattedComponent(key, player.getLanguage(), values);
    }

    public static Component getFormattedComponent(String key, String localeString, Object... values) {
        return renderTranslatable(key, localeString, toComponentLikes(values));
    }

    /**
     * Renders a translation key to plain text against the given locale. Used by call sites
     * that must materialize a translation into a String for storage (e.g. persisted pet names).
     * All styling tags in the translation value are stripped.
     */
    public static String renderPlain(String key, String localeString) {
        return PlainTextComponentSerializer.plainText().serialize(renderTranslatable(key, localeString));
    }

    private static Component renderTranslatable(String key, String localeString, ComponentLike... args) {
        java.util.Locale loc = parseJdkLocale(localeString);
        return GlobalTranslator.render(Component.translatable(key.toLowerCase(), args), loc);
    }

    // ========== Translator SPI — what Adventure calls ==========

    @Override
    public @NotNull Key name() {
        return NAME;
    }

    @Override
    public @Nullable MessageFormat translate(@NotNull String key, @NotNull java.util.Locale locale) {
        // Not used — we override the Component-returning translate() instead so we can
        // emit pre-styled MiniMessage trees. Returning null tells Adventure to fall back.
        return null;
    }

    @Override
    public @Nullable Component translate(@NotNull TranslatableComponent component, @NotNull java.util.Locale locale) {
        java.util.Locale effective = locale;
        if (!MyPetGlobal.Misc.OVERWRITE_LANGUAGE.get().isEmpty()) {
            effective = parseJdkLocale(MyPetGlobal.Misc.OVERWRITE_LANGUAGE.get());
        }

        String key = component.key().toLowerCase(java.util.Locale.ROOT);
        Component raw = lookup(key, effective);
        if (raw == null) {
            return null; // Let Adventure fall through to other sources.
        }

        List<? extends ComponentLike> args = component.arguments();
        if (args.isEmpty()) {
            return raw;
        }
        Object[] valueArray = new Object[args.size()];
        for (int i = 0; i < args.size(); i++) {
            valueArray[i] = args.get(i).asComponent();
        }
        return replaceInComponent(raw, valueArray);
    }

    // ========== Bundle loading ==========

    private void loadAllBundles() {
        try (JarFile jarFile = new JarFile(MyPetApi.getPlugin().getFile())) {
            var entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (!name.startsWith("locale/")) continue;
                Matcher m = BUNDLE_FILENAME.matcher(name.substring("locale/".length()));
                if (!m.matches()) continue;
                try (Reader r = new InputStreamReader(jarFile.getInputStream(entry), StandardCharsets.UTF_8)) {
                    loadBundle(m.group(1), r);
                }
            }
        } catch (IOException e) {
            ErrorUtil.report(e);
        }

        File dataDir = new File(MyPetApi.getPlugin().getDataFolder(), "locale");
        File[] files = dataDir.listFiles();
        if (files != null) {
            for (File f : files) {
                Matcher m = BUNDLE_FILENAME.matcher(f.getName());
                if (!m.matches()) continue;
                try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                    loadBundle(m.group(1), r);
                } catch (IOException e) {
                    ErrorUtil.report(e);
                }
            }
        }

        MyPetApi.getLogger().info("Loaded " + bundles.size() + " MyPet translation bundles: " + bundles.keySet());
    }

    /**
     * Loads keys from one {@code .properties} reader and merges them into the bundle
     * for {@code localeTag}. Sparse overlay: only the keys present in the reader are
     * overwritten — any pre-existing keys in the bundle (e.g. from an earlier JAR load
     * of the same tag) are preserved. A bad MiniMessage entry is skipped, not fatal.
     */
    private void loadBundle(String localeTag, Reader reader) {
        Properties props = new Properties();
        try {
            props.load(reader);
        } catch (IOException e) {
            ErrorUtil.report(e);
            return;
        }
        String tag = localeTag.toLowerCase(java.util.Locale.ROOT);
        Map<String, Component> bundle = bundles.computeIfAbsent(tag, k -> new HashMap<>());
        for (Object rawKey : props.keySet()) {
            String key = rawKey.toString().toLowerCase(java.util.Locale.ROOT);
            String value = props.get(rawKey).toString();
            try {
                bundle.put(key, MINI.deserialize(value));
            } catch (Exception e) {
                ErrorUtil.report(e);
            }
        }
    }

    private @Nullable Component lookup(String key, java.util.Locale locale) {
        Component hit;
        if (!locale.getCountry().isEmpty()) {
            hit = lookupIn(locale.getLanguage() + "_" + locale.getCountry().toLowerCase(java.util.Locale.ROOT), key);
            if (hit != null) return hit;
        }
        hit = lookupIn(locale.getLanguage().toLowerCase(java.util.Locale.ROOT), key);
        if (hit != null) return hit;
        return "en".equals(locale.getLanguage()) ? null : lookupIn("en", key);
    }

    private @Nullable Component lookupIn(String tag, String key) {
        Map<String, Component> bundle = bundles.get(tag);
        return bundle == null ? null : bundle.get(key);
    }

    // ========== Private helpers ==========

    private static java.util.Locale parseJdkLocale(String tag) {
        if (tag == null || tag.isEmpty()) return java.util.Locale.US;
        String[] parts = tag.toLowerCase(java.util.Locale.ROOT).split("[_\\-]");
        if (parts.length >= 2) return java.util.Locale.of(parts[0], parts[1].toUpperCase(java.util.Locale.ROOT));
        return java.util.Locale.of(parts[0]);
    }

    private static ComponentLike[] toComponentLikes(Object[] values) {
        if (values == null || values.length == 0) return new ComponentLike[0];
        ComponentLike[] out = new ComponentLike[values.length];
        for (int i = 0; i < values.length; i++) {
            Object v = values[i];
            if (v == null) {
                out[i] = Component.empty();
            } else if (v instanceof ComponentLike c) {
                out[i] = c;
            } else {
                out[i] = Util.SANITIZED_MINIMESSAGE.deserialize(v.toString());
            }
        }
        return out;
    }

    private static Component replaceInComponent(Component component, Object[] values) {
        if (component instanceof TextComponent textComponent) {
            TextComponent.Builder builder = Component.text().style(component.style());
            String content = textComponent.content();
            List<Component> replacedContent = replacePlaceholders(content, textComponent.style(), values);
            for (Component part : replacedContent) {
                builder.append(part);
            }
            for (Component child : component.children()) {
                builder.append(replaceInComponent(child, values));
            }
            return builder.build();
        }

        // Non-TextComponent nodes (e.g. TranslatableComponent) carry their children inside
        // themselves — re-iterating component.children() would double-emit them.
        return component;
    }

    private static List<Component> replacePlaceholders(String text, Style style, Object[] values) {
        List<Component> result = new ArrayList<>();
        if (text == null || text.isEmpty()) return result;

        int lastIndex = 0;
        Matcher matcher = PLACEHOLDER.matcher(text);

        while (matcher.find()) {
            if (matcher.start() > lastIndex) {
                result.add(Component.text(text.substring(lastIndex, matcher.start())).style(style));
            }
            int index = Integer.parseInt(matcher.group(1));
            if (index < values.length && values[index] != null) {
                Object value = values[index];
                if (value instanceof Component c) {
                    result.add(c);
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
        return result;
    }
}
