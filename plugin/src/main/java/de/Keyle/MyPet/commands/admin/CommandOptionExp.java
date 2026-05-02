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

package de.Keyle.MyPet.commands.admin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.commands.help.CommandCategory;
import de.Keyle.MyPet.commands.help.HelpEntry;
import de.Keyle.MyPet.commands.help.HelpRegistry;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.util.locale.Locale;
import de.Keyle.MyPet.util.MessageUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Admin subcommand that modifies the experience or level of a player's active pet.
 *
 * <p>Usage:</p>
 * <ul>
 *   <li>{@code /petadmin exp <player> <amount> [add|set|remove]} -- manipulates raw experience points</li>
 *   <li>{@code /petadmin exp <player> <levels> levels [add|set|remove]} -- manipulates levels (converted to exp)</li>
 * </ul>
 *
 * <p>When no operator is specified, {@code set} is used by default. Experience is clamped
 * to the range {@code [0, maxExp]} and levels are clamped to {@code [1, LEVEL_CAP]}.</p>
 *
 * <p>Requires the {@code MyPet.admin} permission.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public class CommandOptionExp {

    /**
     * Arithmetic operators for experience/level modification.
     */
    private enum Operator { ADD, SET, REMOVE }

    /**
     * Builds the Brigadier command tree for the {@code exp} subcommand.
     *
     * <p>The tree has two branches under the {@code <player>} argument:</p>
     * <ol>
     *   <li>Raw exp branch: {@code <amount>} (double) followed by optional {@code add|set|remove} literals.</li>
     *   <li>Level branch: {@code <levels>} (integer) followed by the {@code levels} literal keyword and
     *       optional {@code add|set|remove} literals.</li>
     * </ol>
     *
     * @param helpRegistry the help registry to register the command's help entry with
     * @return the built {@link LiteralCommandNode} representing the {@code exp} subcommand
     */
    public LiteralCommandNode<CommandSourceStack> buildNode(HelpRegistry helpRegistry) {
        helpRegistry.register(new HelpEntry(
                "Message.Command.Help.Admin.Exp",
                "/petadmin exp",
                CommandCategory.ADMIN,
                26,
                player -> Permissions.has(player, "MyPet.admin")
        ));

        // /petadmin exp <player> <amount> [add|set|remove]
        // /petadmin exp <player> <levels> levels [add|set|remove]
        LiteralArgumentBuilder<CommandSourceStack> expNode = Commands.literal("exp")
                .then(Commands.argument("player", ArgumentTypes.player())
                        // Raw exp branch: /petadmin exp <player> <amount> [operator]
                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0))
                                .executes(ctx -> {
                                    Player player = ctx.getArgument("player", PlayerSelectorArgumentResolver.class)
                                            .resolve(ctx.getSource()).getFirst();
                                    double amount = DoubleArgumentType.getDouble(ctx, "amount");
                                    executeExp(ctx.getSource().getSender(), player, amount, Operator.SET);
                                    return Command.SINGLE_SUCCESS;
                                })
                                .then(Commands.literal("add").executes(ctx -> {
                                    Player player = ctx.getArgument("player", PlayerSelectorArgumentResolver.class)
                                            .resolve(ctx.getSource()).getFirst();
                                    executeExp(ctx.getSource().getSender(), player,
                                            DoubleArgumentType.getDouble(ctx, "amount"), Operator.ADD);
                                    return Command.SINGLE_SUCCESS;
                                }))
                                .then(Commands.literal("set").executes(ctx -> {
                                    Player player = ctx.getArgument("player", PlayerSelectorArgumentResolver.class)
                                            .resolve(ctx.getSource()).getFirst();
                                    executeExp(ctx.getSource().getSender(), player,
                                            DoubleArgumentType.getDouble(ctx, "amount"), Operator.SET);
                                    return Command.SINGLE_SUCCESS;
                                }))
                                .then(Commands.literal("remove").executes(ctx -> {
                                    Player player = ctx.getArgument("player", PlayerSelectorArgumentResolver.class)
                                            .resolve(ctx.getSource()).getFirst();
                                    executeExp(ctx.getSource().getSender(), player,
                                            DoubleArgumentType.getDouble(ctx, "amount"), Operator.REMOVE);
                                    return Command.SINGLE_SUCCESS;
                                })))
                        // Level branch: /petadmin exp <player> <levels> levels [operator]
                        .then(Commands.argument("levels", IntegerArgumentType.integer(0))
                                .then(Commands.literal("levels")
                                        .executes(ctx -> {
                                            Player player = ctx.getArgument("player", PlayerSelectorArgumentResolver.class)
                                                    .resolve(ctx.getSource()).getFirst();
                                            int levels = IntegerArgumentType.getInteger(ctx, "levels");
                                            executeLevels(ctx.getSource().getSender(), player, levels, Operator.SET);
                                            return Command.SINGLE_SUCCESS;
                                        })
                                        .then(Commands.literal("add").executes(ctx -> {
                                            Player player = ctx.getArgument("player", PlayerSelectorArgumentResolver.class)
                                                    .resolve(ctx.getSource()).getFirst();
                                            executeLevels(ctx.getSource().getSender(), player,
                                                    IntegerArgumentType.getInteger(ctx, "levels"), Operator.ADD);
                                            return Command.SINGLE_SUCCESS;
                                        }))
                                        .then(Commands.literal("set").executes(ctx -> {
                                            Player player = ctx.getArgument("player", PlayerSelectorArgumentResolver.class)
                                                    .resolve(ctx.getSource()).getFirst();
                                            executeLevels(ctx.getSource().getSender(), player,
                                                    IntegerArgumentType.getInteger(ctx, "levels"), Operator.SET);
                                            return Command.SINGLE_SUCCESS;
                                        }))
                                        .then(Commands.literal("remove").executes(ctx -> {
                                            Player player = ctx.getArgument("player", PlayerSelectorArgumentResolver.class)
                                                    .resolve(ctx.getSource()).getFirst();
                                            executeLevels(ctx.getSource().getSender(), player,
                                                    IntegerArgumentType.getInteger(ctx, "levels"), Operator.REMOVE);
                                            return Command.SINGLE_SUCCESS;
                                        })))));

        return expNode.build();
    }

    /**
     * Applies an experience modification to the target player's active pet.
     *
     * <p>The final experience value is computed based on the operator: {@code SET} replaces
     * the current exp (clamped to max), {@code ADD} adds to current exp (clamped to max),
     * and {@code REMOVE} subtracts from current exp (floored at 0).</p>
     *
     * @param sender   the command sender (for feedback messages)
     * @param petOwner the player whose pet's experience will be modified
     * @param amount   the raw experience amount to apply
     * @param operator the arithmetic operation to perform
     */
    private void executeExp(CommandSender sender, Player petOwner, double amount, Operator operator) {
        String lang = Locale.getCommandSenderLanguage(sender);

        if (!MyPetApi.getMyPetManager().hasActiveMyPet(petOwner)) {
            sender.sendMessage(MessageUtil.prefixed(Locale.getFormattedComponent("Message.No.UserHavePet", lang, petOwner.getName())));
            return;
        }
        MyPet myPet = MyPetApi.getMyPetManager().getMyPet(petOwner);
        double exp = switch (operator) {
            case SET -> Math.min(amount, myPet.getExperience().getMaxExp());
            case ADD -> Math.min(myPet.getExp() + amount, myPet.getExperience().getMaxExp());
            case REMOVE -> Math.max(0, myPet.getExp() - amount);
        };

        myPet.getExperience().setExp(exp);
        sender.sendMessage(MessageUtil.prefixed(Component.text(
                "set exp to " + exp + ". Pet is now level " + myPet.getExperience().getLevel() + ".")));
    }

    /**
     * Applies a level-based experience modification to the target player's active pet.
     *
     * <p>Computes the target level based on the operator ({@code SET}, {@code ADD}, or {@code REMOVE}),
     * clamps it to {@code [1, LEVEL_CAP]}, converts the target level to the equivalent experience
     * value, and sets it on the pet.</p>
     *
     * @param sender   the command sender (for feedback messages)
     * @param petOwner the player whose pet's level will be modified
     * @param levels   the number of levels to apply
     * @param operator the arithmetic operation to perform
     */
    private void executeLevels(CommandSender sender, Player petOwner, int levels, Operator operator) {
        String lang = Locale.getCommandSenderLanguage(sender);

        if (!MyPetApi.getMyPetManager().hasActiveMyPet(petOwner)) {
            sender.sendMessage(MessageUtil.prefixed(Locale.getFormattedComponent("Message.No.UserHavePet", lang, petOwner.getName())));
            return;
        }
        MyPet myPet = MyPetApi.getMyPetManager().getMyPet(petOwner);
        int targetLevel = switch (operator) {
            case SET -> Math.min(levels, Configuration.LevelSystem.Experience.LEVEL_CAP);
            case ADD -> Math.min(myPet.getExperience().getLevel() + levels, Configuration.LevelSystem.Experience.LEVEL_CAP);
            case REMOVE -> Math.max(1, myPet.getExperience().getLevel() - levels);
        };

        double exp = targetLevel <= 1 ? 0 : myPet.getExperience().getExpByLevel(targetLevel);
        myPet.getExperience().setExp(exp);
        sender.sendMessage(MessageUtil.prefixed(Component.text(
                "set exp to " + exp + ". Pet is now level " + myPet.getExperience().getLevel() + ".")));
    }
}
