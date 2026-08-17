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

package de.Keyle.MyPet.commands.admin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.PetType;
import de.Keyle.MyPet.commands.help.CommandCategory;
import de.Keyle.MyPet.commands.help.HelpEntry;
import de.Keyle.MyPet.commands.help.HelpRegistry;
import de.Keyle.MyPet.api.player.AdminPermissions;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.util.ConfigItem;
import de.Keyle.MyPet.api.util.locale.Locale;
import de.Keyle.MyPet.util.MessageUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Admin subcommand for inspecting item information relevant to MyPet configuration.
 *
 * <p>This command provides two sub-paths:
 * <ul>
 *   <li>{@code /petadmin info item} -- displays the material name of the item currently held in
 *       the executing player's main hand, with a clickable [Copy] button, plus a
 *       [Copy with NBT] button for the lossless form</li>
 *   <li>{@code /petadmin info leashitem <pettype>} -- displays the configured leash item for the
 *       specified pet type, with a clickable [Copy] button</li>
 * </ul>
 *
 * <p>The {@code item} subcommand can only be executed by a player (not from console).
 *
 * <p><b>Two output formats, both config-pasteable.</b> The [Copy] button yields a bare material
 * name (e.g. {@code SHEARS}), which matches any item of that type — the right choice for leash,
 * food, ride and control items, where a partially damaged tool should still count. The
 * [Copy with NBT] button yields the dot-prefixed component string (e.g.
 * {@code . minecraft:shears[minecraft:damage=6]}) for when the exact item must match. Both forms
 * are parsed by {@link ConfigItem}, so either can be pasted into {@code config.yml} /
 * {@code pet-config.yml}, and both are accepted in skilltree drop pools.
 *
 * <p>Requires the {@code MyPet.admin.info} permission (or the {@code MyPet.admin} bundle).
 */
public class CommandOptionInfo {

    /**
     * Builds the Brigadier command node for the {@code info} admin subcommand.
     *
     * <p>The resulting command tree structure is:
     * <pre>
     *   info
     *     item
     *       (executes) -- show held item's serialized string
     *     leashitem
     *       &lt;pettype: entity_type&gt;
     *         (executes) -- show leash item for the given pet type
     * </pre>
     *
     * <p>The {@code pettype} argument accepts any registered {@link PetType} name (vanilla or custom),
     * resolved via {@link CommandOptionCreate#matchPetType}.
     *
     * @param helpRegistry the help registry to register the command's help entry with
     * @return the built {@link LiteralCommandNode} representing the {@code info} subcommand
     */
    public LiteralCommandNode<CommandSourceStack> buildNode(HelpRegistry helpRegistry) {
        helpRegistry.register(new HelpEntry(
                "Message.Command.Help.Admin.Info",
                "/petadmin info",
                CommandCategory.ADMIN,
                40,
                player -> Permissions.has(player, AdminPermissions.INFO)
        ));

        return Commands.literal("info")
                .requires(AdminPermissions.requiresNode(AdminPermissions.INFO))
                .then(Commands.literal("item")
                        .executes(ctx -> {
                            executeItem(ctx.getSource().getSender());
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(Commands.literal("leashitem")
                        .then(Commands.argument("pettype", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    String partial = builder.getRemaining().toLowerCase();
                                    for (PetType pt : PetType.values()) {
                                        String name = pt.name().toLowerCase();
                                        if (name.startsWith(partial)) {
                                            builder.suggest(name);
                                        }
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    String token = StringArgumentType.getString(ctx, "pettype");
                                    PetType type = CommandOptionCreate.matchPetType(token);
                                    if (type != null) {
                                        executeLeashItem(ctx.getSource().getSender(), type);
                                    } else {
                                        ctx.getSource().getSender().sendMessage(Locale.getComponent("Message.Command.PetType.Unknown", ctx.getSource().getSender()));
                                    }
                                    return Command.SINGLE_SUCCESS;
                                })))
                .build();
    }

    /**
     * Displays the material name of the item held in the player's main hand.
     *
     * <p>If the held item is air (empty hand), the string {@code "air"} is displayed with no
     * copy buttons. Otherwise the output carries two: [Copy] for the material name, and
     * [Copy with NBT] for the dot-prefixed component string. This command cannot be run from
     * the server console.
     *
     * @param sender the command sender; must be a {@link Player} instance
     */
    private void executeItem(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("You can't use this command from server console!");
            return;
        }
        ItemStack itemStack = player.getInventory().getItemInMainHand();
        if (itemStack.getType() == Material.AIR) {
            sender.sendMessage(MessageUtil.prefixed(
                    Component.text("Item: ").color(NamedTextColor.GRAY)
                            .append(Component.text("air").color(NamedTextColor.WHITE))));
            return;
        }

        // Primary token: the plain material name. Matches loosely — any shears, not "a
        // shears damaged exactly 6 points" — which is what leash/food/ride/control item
        // comparisons want, since ConfigItem#compare only demands an exact meta match
        // when the configured item carries meta of its own.
        String itemString = itemStack.getType().name();
        Component copyButton = Component.text(" [Copy]").color(NamedTextColor.AQUA)
                .clickEvent(ClickEvent.copyToClipboard(itemString))
                .hoverEvent(HoverEvent.showText(
                        Component.text("Click to copy to clipboard").color(NamedTextColor.YELLOW)));

        // Secondary token: the dot-prefixed component string, for when the exact item
        // (custom name, lore, enchantments, damage) has to be matched rather than any
        // item of that material. ConfigItem reads the dot prefix as "modern component
        // string" and hands the remainder to ItemFactory#createItemStack.
        Component copyNbtButton = Component.text(" [Copy with NBT]").color(NamedTextColor.DARK_AQUA)
                .clickEvent(ClickEvent.copyToClipboard(toComponentString(itemStack)))
                .hoverEvent(HoverEvent.showText(
                        Component.text("Click to copy the full item incl. NBT").color(NamedTextColor.YELLOW)));

        sender.sendMessage(MessageUtil.prefixed(
                Component.text("Item: ").color(NamedTextColor.GRAY)
                        .append(Component.text(itemString).color(NamedTextColor.WHITE))
                        .append(copyButton)
                        .append(copyNbtButton)));
    }

    /**
     * Builds the dot-prefixed component string for an item, e.g.
     * {@code . minecraft:diamond_sword[minecraft:damage=53]}.
     *
     * <p>The leading {@code ". "} is MyPet's marker for "modern component string" —
     * {@link ConfigItem} strips it and passes the rest to
     * {@link org.bukkit.inventory.ItemFactory#createItemStack(String)}. The body follows Paper's
     * documented recipe for {@link org.bukkit.inventory.meta.ItemMeta#getAsComponentString()}:
     * the item type key concatenated with the component list.
     *
     * <p>Items with no meta yield a bare {@code . minecraft:shears} (Paper renders an empty
     * component list as {@code []}, which is dropped here rather than pasted into a config).
     *
     * @param itemStack the item to describe; must not be air
     * @return a string {@link ConfigItem} can parse back into an equivalent item
     */
    private String toComponentString(ItemStack itemStack) {
        String typeKey = itemStack.getType().getKey().toString();
        ItemMeta meta = itemStack.getItemMeta();
        String components = meta == null ? "" : meta.getAsComponentString();
        if (components == null || components.isBlank() || components.equals("[]")) {
            return ". " + typeKey;
        }
        return ". " + typeKey + components;
    }

    /**
     * Displays the configured leash item for a specific pet type.
     *
     * <p>Retrieves the leash item from the pet type's configuration via
     * {@link MyPetApi#getPetInfo()} and displays its serialized string representation.
     * If no leash item is configured (null item stack), {@code "air"} is shown.
     * The output includes a clickable [Copy] button for clipboard copying.
     *
     * @param sender the command sender to receive the output message
     * @param type   the pet type whose leash item should be displayed
     */
    private void executeLeashItem(CommandSender sender, PetType type) {
        ConfigItem configItem = MyPetApi.getPetInfo().getLeashItem(type);
        ItemStack configItemStack = configItem.getItem();
        String itemString = "air";
        if (configItemStack != null) {
            itemString = configItemStack.getType().isAir() ? "AIR" : configItemStack.getType().name();
        }

        Component copyButton = Component.text(" [Copy]").color(NamedTextColor.AQUA)
                .clickEvent(ClickEvent.copyToClipboard(itemString))
                .hoverEvent(HoverEvent.showText(
                        Component.text("Click to copy to clipboard").color(NamedTextColor.YELLOW)));

        sender.sendMessage(MessageUtil.prefixed(
                Component.text("Leash Item (").color(NamedTextColor.GRAY)
                        .append(Component.text(type.name()).color(NamedTextColor.GOLD))
                        .append(Component.text("): ").color(NamedTextColor.GRAY))
                        .append(Component.text(itemString).color(NamedTextColor.WHITE))
                        .append(copyButton)));
    }
}
