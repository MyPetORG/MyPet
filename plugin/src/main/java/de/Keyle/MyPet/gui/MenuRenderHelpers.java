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

package de.Keyle.MyPet.gui;

import de.Keyle.MyPet.api.gui.HeadSkin;
import de.Keyle.MyPet.api.gui.ItemAppearance;
import de.Keyle.MyPet.api.gui.RenderContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.TranslationArgument;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Stateless helpers shared by every {@link de.Keyle.MyPet.api.gui.SectionRenderer}. */
public final class MenuRenderHelpers {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private MenuRenderHelpers() {}

    /** Build a Bukkit {@link ItemStack} from an {@link ItemAppearance}, resolving placeholders. */
    public static ItemStack toItemStack(ItemAppearance a, RenderContext ctx) {
        ItemStack stack = new ItemStack(a.material(), a.amount());
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;

        TagResolver resolvers = ctx.catalog().build();
        java.util.Locale viewerLocale = ctx.viewer().locale();
        meta.displayName(renderForViewer(a.title(), resolvers, viewerLocale));
        if (!a.lore().isEmpty()) {
            List<Component> lore = new ArrayList<>(a.lore().size());
            for (String line : a.lore()) {
                Component rendered = renderForViewer(line, resolvers, viewerLocale);
                for (Component piece : splitNewlines(rendered)) {
                    if (isBlank(piece)) continue;
                    lore.add(piece);
                }
            }
            meta.lore(lore);
        }
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE,
            ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        if (a.glow()) {
            meta.setEnchantmentGlintOverride(true);
        }
        if (a.customModelData() != 0) {
            meta.setCustomModelData(a.customModelData());
        }
        if (meta instanceof SkullMeta skull && a.headSkin() != HeadSkin.STEVE) {
            applyHeadSkin(skull, a.headSkin(), ctx);
        }
        if (meta instanceof PotionMeta potion && a.potionColor() != null) {
            potion.setColor(a.potionColor());
        }
        stack.setItemMeta(meta);
        return stack;
    }

    /** Vanilla Alex texture URL hosted by Mojang. */
    private static final String ALEX_TEXTURE_URL =
        "http://textures.minecraft.net/texture/3b60a1f6d562f52aaebbf1434f1de147933a3affe0e764fa49ea057536623cd3";
    private static final PlayerProfile ALEX_PROFILE = buildAlexProfile();

    private static void applyHeadSkin(SkullMeta skull, HeadSkin which, RenderContext ctx) {
        switch (which) {
            case VIEWER -> skull.setOwnerProfile(ctx.viewer().getPlayerProfile());
            case ALEX -> skull.setOwnerProfile(ALEX_PROFILE);
            case STEVE -> { /* default — handled by early return */ }
        }
    }

    private static PlayerProfile buildAlexProfile() {
        PlayerProfile profile = Bukkit.createPlayerProfile(
            UUID.fromString("ec561538-f3fd-461d-aff5-086b22154bce"), "MHF_Alex");
        try {
            profile.getTextures().setSkin(URI.create(ALEX_TEXTURE_URL).toURL());
        } catch (Exception e) {
            // Texture URL is a compile-time constant — failure here is impossible in practice.
            throw new IllegalStateException("Failed to set Alex skin", e);
        }
        return profile;
    }

    /**
     * Deserialize MiniMessage and resolve any `<lang:...>` tags server-side via the
     * registered {@link GlobalTranslator} sources (which includes MyPet's Locale).
     * Necessary because Paper does not auto-render TranslatableComponents inside
     * ItemStack metadata before sending to the client.
     */
    public static Component renderForViewer(String miniMessage, TagResolver resolvers, java.util.Locale viewerLocale) {
        Component parsed = MINI.deserialize(miniMessage, resolvers);
        return GlobalTranslator.render(parsed, viewerLocale);
    }

    /**
     * Split a rendered {@link Component} into one entry per newline boundary so each
     * line becomes a separate lore slot, splitting the component tree directly.
     *
     * <p>The tree is flattened depth-first into styled text runs; each {@code '\n'} in a
     * {@link TextComponent}'s content ends the current line. Style is inherited the way
     * Adventure renders it — a child's own style wins, the ancestor style fills the gaps —
     * and baked onto every emitted run, so each line stands alone.</p>
     */
    public static List<Component> splitNewlines(Component c) {
        // Fast path: no newline anywhere -> already a single line, returned as-is so its
        // exact structure (and any non-text leaves) is preserved untouched.
        if (!containsNewline(c)) return List.of(c);
        List<Component> lines = new ArrayList<>();
        List<Component> current = new ArrayList<>();
        flattenLines(c, Style.empty(), lines, current);
        lines.add(joinLine(current));
        return lines;
    }

    /**
     * Depth-first flatten of {@code node} into {@code current} (the line being built),
     * flushing a finished line into {@code lines} at every {@code '\n'}. {@code inherited}
     * is the ancestor style; the effective style is the node's own style with the inherited
     * style filling any unset values.
     */
    private static void flattenLines(Component node, Style inherited,
                                     List<Component> lines, List<Component> current) {
        Style eff = node.style().merge(inherited);
        if (node instanceof TextComponent text) {
            String s = text.content();
            int start = 0;
            for (int i = 0; i < s.length(); i++) {
                if (s.charAt(i) == '\n') {
                    if (i > start) current.add(Component.text(s.substring(start, i), eff));
                    lines.add(joinLine(current));
                    current.clear();
                    start = i + 1;
                }
            }
            if (start < s.length()) current.add(Component.text(s.substring(start), eff));
        } else {
            // Non-text leaf (translatable/keybind/score/selector/nbt): keep atomic, restyled.
            current.add(node.children(List.of()).style(eff));
        }
        for (Component child : node.children()) {
            flattenLines(child, eff, lines, current);
        }
    }

    /** Combine one line's runs into a single component (empty / single-run fast paths). */
    private static Component joinLine(List<Component> parts) {
        if (parts.isEmpty()) return Component.empty();
        if (parts.size() == 1) return parts.get(0);
        return Component.textOfChildren(parts.toArray(new Component[0]));
    }

    private static boolean containsNewline(Component c) {
        if (c instanceof TextComponent text) {
            if (text.content().indexOf('\n') >= 0) return true;
        } else if (c instanceof TranslatableComponent translatable) {
            String fallback = translatable.fallback();
            if (fallback != null && fallback.indexOf('\n') >= 0) return true;
            for (TranslationArgument arg : translatable.arguments()) {
                if (containsNewline(arg.asComponent())) return true;
            }
        } else {
            // Leaf type we can't cheaply inspect (selector/score/nbt/keybind/…):
            // assume it may carry a newline and let the serialize path decide, rather
            // than wrongly collapsing it into a single lore line.
            return true;
        }
        for (Component child : c.children()) {
            if (containsNewline(child)) return true;
        }
        return false;
    }

    /**
     * True if the rendered {@link Component} has no visible text. Lets handlers skip
     * lore lines by emitting an empty placeholder value (the template stays declarative;
     * dynamic data decides what's shown).
     */
    public static boolean isBlank(Component c) {
        return PlainTextComponentSerializer.plainText().serialize(c).isEmpty();
    }

    /** Resolver that exposes a `<viewer_name>` placeholder for fall-back rendering. */
    public static TagResolver viewerTagResolver(org.bukkit.entity.Player viewer) {
        return TagResolver.builder()
            .resolver(net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed(
                "viewer_name", viewer.getName()))
            .build();
    }
}
