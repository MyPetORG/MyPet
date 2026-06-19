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

package de.Keyle.MyPet.commands.admin.npc;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.commands.help.HelpRegistry;
import de.Keyle.MyPet.util.MessageUtil;
import de.Keyle.MyPet.util.hooks.citizens.ShopTrait;
import de.Keyle.MyPet.util.shop.ShopService;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides the {@code /petadmin npc shop} subcommand, which assigns a named pet shop
 * to the currently selected Citizens NPC.
 *
 * <h3>Usage</h3>
 * <p>{@code /petadmin npc shop <shopname>}</p>
 *
 * <p>The selected NPC must already have the {@code mypet-shop} Citizens trait applied.
 * The specified shop name must correspond to an existing shop registered with the
 * {@link ShopService}. Tab completion suggests all known shop names.</p>
 *
 * <p>This command is part of the admin {@code /petadmin npc} group and inherits
 * its permission requirements (typically {@code MyPet.admin}).</p>
 */
public class CommandOptionShop {

    /**
     * Builds and returns the Brigadier {@code "shop"} literal command node with a
     * required {@code shopname} argument. Tab completion suggests all registered shop names.
     *
     * @param helpRegistry the help registry to register the command's help entry with
     * @return the built {@link LiteralCommandNode} representing the {@code shop} subcommand
     */
    public LiteralCommandNode<CommandSourceStack> buildNode(HelpRegistry helpRegistry) {
        return Commands.literal("shop")
                .then(Commands.argument("shopname", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            List<ShopService> shopServiceList = MyPetApi.getServiceManager().getServices(ShopService.class);
                            if (!shopServiceList.isEmpty()) {
                                new ArrayList<>(shopServiceList.get(0).getShopNames())
                                        .forEach(builder::suggest);
                            }
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            execute(ctx.getSource().getSender(),
                                    StringArgumentType.getString(ctx, "shopname"));
                            return Command.SINGLE_SUCCESS;
                        }))
                .build();
    }

    /**
     * Assigns the specified shop to the currently selected Citizens NPC's {@link ShopTrait}.
     * Validates that an NPC is selected, the NPC has the {@code mypet-shop} trait, and the
     * shop name exists in the registered {@link ShopService}.
     *
     * @param sender the command sender (player or console) executing the command
     * @param shop   the name of the pet shop to assign to the NPC
     */
    private void execute(CommandSender sender, String shop) {
        NPC selectedNPC = CitizensAPI.getDefaultNPCSelector().getSelected(sender);
        if (selectedNPC == null) {
            sender.sendMessage(MessageUtil.prefixed(Component.text("No NPC seleced!")));
            return;
        }

        if (!selectedNPC.hasTrait(ShopTrait.class)) {
            sender.sendMessage(MessageUtil.prefixed(Component.text().append(Component.text("This NPC doesn't has the ")).append(Component.text("mypet-shop").color(NamedTextColor.GOLD)).append(Component.text(" trait!")).asComponent()));
            return;
        }

        List<ShopService> shopServiceList = MyPetApi.getServiceManager().getServices(ShopService.class);
        if (!shopServiceList.isEmpty()) {
            boolean shopFound = false;
            for (ShopService shopService : shopServiceList) {
                if (shopService.getShopNames().contains(shop)) {
                    shopFound = true;
                    break;
                }
            }
            if (!shopFound) {
                sender.sendMessage(MessageUtil.prefixed(Component.text("No shop with this name found: " + shop)));
                return;
            }
        }

        ShopTrait trait = selectedNPC.getTrait(ShopTrait.class);
        trait.setShop(shop);
        sender.sendMessage(MessageUtil.prefixed(Component.text("Shop trait updated.")));
    }
}
