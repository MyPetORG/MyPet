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
import de.Keyle.MyPet.util.WalletType;
import de.Keyle.MyPet.util.MessageUtil;
import de.Keyle.MyPet.util.hooks.VaultHook;
import de.Keyle.MyPet.util.hooks.citizens.WalletTrait;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

import java.util.Optional;

/**
 * Provides the {@code /petadmin npc wallet} subcommand, which configures the wallet trait
 * on the currently selected Citizens NPC. The wallet trait controls where shop revenue
 * is deposited.
 *
 * <h3>Usage</h3>
 * <ul>
 *   <li>{@code /petadmin npc wallet <type>} -- set the wallet type (tab-completes {@link WalletType} values)</li>
 *   <li>{@code /petadmin npc wallet <type> <account>} -- set the wallet type and target account name</li>
 * </ul>
 *
 * <h3>Wallet types</h3>
 * <p>The available types are defined by {@link WalletType}. The {@code Player} and {@code Bank}
 * types require a Vault-compatible economy plugin; the {@code Bank} type additionally requires
 * bank support from the economy plugin.</p>
 *
 * <p>This command is part of the admin {@code /petadmin npc} group and inherits
 * its permission requirements (typically {@code MyPet.admin}).</p>
 */
@SuppressWarnings("UnstableApiUsage")
public class CommandOptionWallet {

    /**
     * Builds and returns the Brigadier {@code "wallet"} literal command node with a
     * required {@code type} argument and an optional {@code account} argument.
     * Tab completion suggests all {@link WalletType} enum values.
     *
     * @param helpRegistry the help registry to register the command's help entry with
     * @return the built {@link LiteralCommandNode} representing the {@code wallet} subcommand
     */
    public LiteralCommandNode<CommandSourceStack> buildNode(HelpRegistry helpRegistry) {
        return Commands.literal("wallet")
                .then(Commands.argument("type", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            for (WalletType walletType : WalletType.values()) {
                                builder.suggest(walletType.name());
                            }
                            return builder.buildFuture();
                        })
                        // /petadmin npc wallet <type>
                        .executes(ctx -> {
                            execute(ctx.getSource().getSender(),
                                    StringArgumentType.getString(ctx, "type"),
                                    null);
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(Commands.argument("account", StringArgumentType.word())
                                .executes(ctx -> {
                                    execute(ctx.getSource().getSender(),
                                            StringArgumentType.getString(ctx, "type"),
                                            StringArgumentType.getString(ctx, "account"));
                                    return Command.SINGLE_SUCCESS;
                                })))
                .build();
    }

    /**
     * Configures the wallet trait on the currently selected Citizens NPC. Validates that an
     * NPC is selected, the NPC has the {@code mypet-wallet} trait, the wallet type is valid,
     * and that economy/bank support is available when required.
     *
     * @param sender         the command sender executing the command
     * @param walletTypeName the name of the {@link WalletType} to set
     * @param account        the optional account name for the wallet (may be {@code null})
     */
    private void execute(CommandSender sender, String walletTypeName, String account) {
        NPC selectedNPC = CitizensAPI.getDefaultNPCSelector().getSelected(sender);
        if (selectedNPC == null) {
            sender.sendMessage(MessageUtil.prefixed(Component.text("No NPC seleced!")));
            return;
        }

        if (!selectedNPC.hasTrait(WalletTrait.class)) {
            sender.sendMessage(MessageUtil.prefixed(Component.text().append(Component.text("This NPC doesn't has the ")).append(Component.text("mypet-wallet").color(NamedTextColor.GOLD)).append(Component.text(" trait!")).build()));
            return;
        }

        Optional<WalletType> optWalletType = WalletType.getByName(walletTypeName);
        if (optWalletType.isEmpty()) {
            sender.sendMessage(MessageUtil.prefixed(Component.text("Invalid wallet type!")));
            return;
        }
        WalletType newWalletType = optWalletType.get();

        WalletTrait trait = selectedNPC.getTrait(WalletTrait.class);

        if (!MyPetApi.getHookHelper().isEconomyEnabled()) {
            if (newWalletType == WalletType.Bank || newWalletType == WalletType.Player) {
                sender.sendMessage(MessageUtil.prefixed(Component.text("You can not use the \"Player\" and \"Bank\" wallet types without an economy plugin installed!")));
                return;
            }
        } else {
            if (newWalletType == WalletType.Bank && !((VaultHook) MyPetApi.getHookHelper().getEconomy()).getEconomy().hasBankSupport()) {
                sender.sendMessage(MessageUtil.prefixed(Component.text("Your economy plugin doesn't has \"Banks\" support!")));
                return;
            }
        }

        trait.setWalletType(newWalletType);

        if (account != null) {
            trait.setAccount(account);
        }

        sender.sendMessage(MessageUtil.prefixed(Component.text("wallet trait updated.")));
    }
}
