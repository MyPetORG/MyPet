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
import com.mojang.brigadier.tree.LiteralCommandNode;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.PetType;
import de.Keyle.MyPet.commands.help.CommandCategory;
import de.Keyle.MyPet.commands.help.HelpEntry;
import de.Keyle.MyPet.commands.help.HelpRegistry;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.util.ConfigItem;
import de.Keyle.MyPet.util.MessageUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Admin subcommand for inspecting item information relevant to MyPet configuration.
 *
 * <p>This command provides two sub-paths:
 * <ul>
 *   <li>{@code /petadmin info item} -- displays the serialized string representation of the
 *       item currently held in the executing player's main hand, with a clickable [Copy] button</li>
 *   <li>{@code /petadmin info leashitem <pettype>} -- displays the configured leash item for the
 *       specified pet type, with a clickable [Copy] button</li>
 * </ul>
 *
 * <p>The {@code item} subcommand can only be executed by a player (not from console).
 * The serialized item strings use the platform helper's {@code itemstackToString} format,
 * which can be pasted directly into MyPet configuration files.
 *
 * <p>Requires the {@code MyPet.admin} permission.
 */
@SuppressWarnings("UnstableApiUsage")
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
     * <p>The {@code pettype} argument reuses the custom argument type from
     * {@link CommandOptionCreate#PET_ENTITY_TYPE}.
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
                player -> Permissions.has(player, "MyPet.admin")
        ));

        return Commands.literal("info")
                .then(Commands.literal("item")
                        .executes(ctx -> {
                            executeItem(ctx.getSource().getSender());
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(Commands.literal("leashitem")
                        .then(Commands.argument("pettype", CommandOptionCreate.PET_ENTITY_TYPE)
                                .executes(ctx -> {
                                    EntityType entityType = ctx.getArgument("pettype", EntityType.class);
                                    PetType type = PetType.byEntityTypeName(entityType.name());
                                    executeLeashItem(ctx.getSource().getSender(), type);
                                    return Command.SINGLE_SUCCESS;
                                })))
                .build();
    }

    /**
     * Displays the serialized string representation of the item held in the player's main hand.
     *
     * <p>If the held item is air (empty hand), the string {@code "air"} is displayed.
     * The output includes a clickable [Copy] button that copies the item string to the
     * player's clipboard. This command cannot be run from the server console.
     *
     * @param sender the command sender; must be a {@link Player} instance
     */
    private void executeItem(CommandSender sender) {
        if (sender instanceof Player player) {
            ItemStack itemStack = player.getInventory().getItemInMainHand();
            String itemString = itemStack.getType() != Material.AIR
                    ? itemStack.getType().name()
                    : "air";

            Component copyButton = Component.text(" [Copy]").color(NamedTextColor.AQUA)
                    .clickEvent(ClickEvent.copyToClipboard(itemString))
                    .hoverEvent(HoverEvent.showText(
                            Component.text("Click to copy to clipboard").color(NamedTextColor.YELLOW)));

            sender.sendMessage(MessageUtil.prefixed(
                    Component.text("Item: ").color(NamedTextColor.GRAY)
                            .append(Component.text(itemString).color(NamedTextColor.WHITE))
                            .append(copyButton)));
        } else {
            sender.sendMessage("You can't use this command from server console!");
        }
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
