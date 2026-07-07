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
import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.entity.PersistedPet;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.entity.PetBaby;
import de.Keyle.MyPet.api.entity.PetType;
import de.Keyle.MyPet.api.event.PetSaveEvent;
import de.Keyle.MyPet.api.event.PetSelectSkilltreeEvent;
import de.Keyle.MyPet.commands.help.CommandCategory;
import de.Keyle.MyPet.commands.help.HelpEntry;
import de.Keyle.MyPet.commands.help.HelpRegistry;
import de.Keyle.MyPet.entity.options.PetCreationOptions;
import de.Keyle.MyPet.entity.visual.PetEntitySnapshot;
import de.Keyle.MyPet.api.event.PetCreateEvent;
import de.Keyle.MyPet.api.player.AdminPermissions;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.skill.skilltree.Skilltree;
import de.Keyle.MyPet.api.util.locale.Locale;
import de.Keyle.MyPet.util.translation.PetDefaultNameResolver;
import de.Keyle.MyPet.util.MessageUtil;
import de.Keyle.MyPet.commands.arguments.PetTypeArgument;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Admin subcommand that creates a new pet for a target player.
 *
 * <p>Usage:</p>
 * <ul>
 *   <li>{@code /petadmin create <player> <type> [options...]} -- creates a pet if the player has no active pet</li>
 *   <li>{@code /petadmin create -f <player> <type> [options...]} -- force-creates a pet, deactivating any existing one</li>
 * </ul>
 *
 * <p>The {@code type} argument accepts any Minecraft entity type that maps to a valid {@link PetType}.
 * Optional trailing arguments control the pet's appearance and metadata (e.g. {@code baby}, {@code saddle},
 * {@code variant:lucy}, {@code skilltree:Combat}, {@code name:Fluffy}).</p>
 *
 * <p>Requires the {@code MyPet.admin.create} permission (or the {@code MyPet.admin} bundle).</p>
 *
 * <p>Per-type tab-completion options come from the pet class's
 * {@code CREATION_SPECS} field plus auto-derived flag specs for any marker
 * interface the pet implements ({@link PetBaby}, {@code PetTameable},
 * {@code PetChested}, {@code PetSaddleable}). Option application and
 * tab-completion value-derivation both delegate to {@link PetCreationOptions}.</p>
 */
public class CommandOptionCreate {

    /**
     * Options common to all pet types, appended to every suggestion list.
     */
    private static final List<String> COMMON_OPTIONS = List.of("skilltree:", "name:");

    /**
     * Matches a single raw command-input token (e.g. {@code "minecraft:snow_golem"},
     * {@code "snow_golem"}, {@code "SnowGolem"}) to a registered {@link PetType},
     * or {@code null} if no match.
     */
    static PetType matchPetType(String token) {
        String stripped = token;
        if (stripped.startsWith("minecraft:")) {
            stripped = stripped.substring("minecraft:".length());
        } else if (stripped.startsWith("mypet:")) {
            stripped = stripped.substring("mypet:".length());
        }
        String key = stripped.toLowerCase().replace("_", "");
        for (PetType type : PetType.values()) {
            if (type.name().toLowerCase().equals(key)) {
                return type;
            }
        }
        return null;
    }


    /**
     * Builds the Brigadier command tree for the {@code create} subcommand.
     *
     * <p>The tree has two branches:</p>
     * <ol>
     *   <li>{@code create -f <player> <type> [options...]} -- force variant that deactivates the player's
     *       current pet before creating a new one.</li>
     *   <li>{@code create <player> <type> [options...]} -- standard variant that fails if the player already
     *       has an active pet.</li>
     * </ol>
     *
     * <p>Both branches accept an optional greedy-string {@code options} argument with
     * type-aware tab-completion provided by {@link #suggestOptions}.</p>
     *
     * @param helpRegistry the help registry to register the command's help entry with
     * @return the built {@link LiteralCommandNode} representing the {@code create} subcommand
     */
    public LiteralCommandNode<CommandSourceStack> buildNode(HelpRegistry helpRegistry) {
        helpRegistry.register(new HelpEntry(
                "Message.Command.Help.Admin.Create",
                "/petadmin create",
                CommandCategory.ADMIN,
                20,
                player -> Permissions.has(player, AdminPermissions.CREATE)
        ));

        return Commands.literal("create")
                .requires(AdminPermissions.requiresNode(AdminPermissions.CREATE))
                // /petadmin create -f <player> <type> [options...]
                .then(Commands.literal("-f")
                        .then(Commands.argument("player", ArgumentTypes.player())
                                .then(Commands.argument("type", new PetTypeArgument())
                                        .executes(ctx -> {
                                            Player player = ctx.getArgument("player", PlayerSelectorArgumentResolver.class)
                                                    .resolve(ctx.getSource()).getFirst();
                                            executeCreate(ctx.getSource().getSender(),
                                                    true, player,
                                                    ctx.getArgument("type", PetType.class),
                                                    new String[0]);
                                            return Command.SINGLE_SUCCESS;
                                        })
                                        .then(Commands.argument("options", StringArgumentType.greedyString())
                                                .suggests((ctx, builder) -> {
                                                    suggestOptions(ctx.getInput(), builder);
                                                    return builder.buildFuture();
                                                })
                                                .executes(ctx -> {
                                                    Player player = ctx.getArgument("player", PlayerSelectorArgumentResolver.class)
                                                            .resolve(ctx.getSource()).getFirst();
                                                    executeCreate(ctx.getSource().getSender(),
                                                            true, player,
                                                            ctx.getArgument("type", PetType.class),
                                                            StringArgumentType.getString(ctx, "options").split(" "));
                                                    return Command.SINGLE_SUCCESS;
                                                })))))
                // /petadmin create <player> <type> [options...]
                .then(Commands.argument("player", ArgumentTypes.player())
                        .then(Commands.argument("type", new PetTypeArgument())
                                .executes(ctx -> {
                                    Player player = ctx.getArgument("player", PlayerSelectorArgumentResolver.class)
                                            .resolve(ctx.getSource()).getFirst();
                                    executeCreate(ctx.getSource().getSender(),
                                            false, player,
                                            ctx.getArgument("type", PetType.class),
                                            new String[0]);
                                    return Command.SINGLE_SUCCESS;
                                })
                                .then(Commands.argument("options", StringArgumentType.greedyString())
                                        .suggests((ctx, builder) -> {
                                            suggestOptions(ctx.getInput(), builder);
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> {
                                            Player player = ctx.getArgument("player", PlayerSelectorArgumentResolver.class)
                                                    .resolve(ctx.getSource()).getFirst();
                                            executeCreate(ctx.getSource().getSender(),
                                                    false, player,
                                                    ctx.getArgument("type", PetType.class),
                                                    StringArgumentType.getString(ctx, "options").split(" "));
                                            return Command.SINGLE_SUCCESS;
                                        }))))
                .build();
    }

    /**
     * Populates Brigadier tab-completion suggestions for the trailing {@code options} argument.
     *
     * <p>Combines the common options ({@code skilltree:}, {@code name:}) with the type-specific
     * options for the pet type found in the command input. Per-type options come from
     * {@link PetCreationOptions#optionsFor} (which derives them from the pet's
     * {@code CREATION_SPECS} field plus marker-interface flag specs).
     * Options already present in the input are excluded; only the last whitespace-delimited
     * word is prefix-matched.</p>
     *
     * @param input   the full raw command input string from the Brigadier context
     * @param builder the suggestions builder to populate with matching completions
     */
    private void suggestOptions(String input, com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
        String[] parts = input.split(" ");

        PetType matchedType = null;
        for (String part : parts) {
            PetType candidate = matchPetType(part);
            if (candidate != null) {
                matchedType = candidate;
                break;
            }
        }

        List<String> options = new ArrayList<>(COMMON_OPTIONS);
        if (matchedType != null) {
            options.addAll(PetCreationOptions.optionsFor(matchedType));
        }

        // Collect already-entered options so we don't suggest them again.
        // The LAST whitespace-delimited token is the word the user is currently
        // typing — we exclude it here so that typing "variant:" doesn't filter
        // out every "variant:<name>" completion as "already used".
        String remaining = builder.getRemaining();
        int lastSpace = remaining.lastIndexOf(' ');
        String completed = lastSpace >= 0 ? remaining.substring(0, lastSpace) : "";
        Set<String> alreadyUsed = new HashSet<>();
        for (String word : completed.split(" ")) {
            if (word.isEmpty()) continue;
            String lower = word.toLowerCase();
            // For key:value options (e.g. "variant:red"), track the key prefix
            int colon = lower.indexOf(':');
            if (colon >= 0) {
                alreadyUsed.add(lower.substring(0, colon + 1));
            } else {
                alreadyUsed.add(lower);
            }
        }

        // Position of the last-word's start within the greedy string
        int lastWordStart = builder.getStart() + lastSpace + 1;
        String partial = remaining.substring(lastSpace + 1).toLowerCase();

        // Two-stage completion. Before the admin types a colon we show only
        // keys (literals like "baby" and bare keys like "variant:"). After
        // the colon we show only the values for the typed key — and we shift
        // the suggestion-builder offset past the colon so the dropdown shows
        // each value bare ("lucy" instead of "variant:lucy"), since the
        // "variant:" prefix is already in the command line.
        int colonInPartial = partial.indexOf(':');
        if (colonInPartial < 0) {
            // Key stage
            com.mojang.brigadier.suggestion.SuggestionsBuilder keyBuilder =
                    builder.createOffset(lastWordStart);
            for (String option : options) {
                String optionLower = option.toLowerCase();
                int colon = optionLower.indexOf(':');
                boolean isValueEntry = colon >= 0 && colon < optionLower.length() - 1;
                if (isValueEntry) continue;
                String key = colon >= 0 ? optionLower.substring(0, colon + 1) : optionLower;
                if (alreadyUsed.contains(key)) continue;
                if (optionLower.startsWith(partial)) {
                    keyBuilder.suggest(option);
                }
            }
            builder.add(keyBuilder);
        } else {
            // Value stage — the admin has committed to a key. Suggestion
            // offset is right after the colon so the dropdown only contains
            // the value (e.g. "lucy"), not the key prefix.
            String typedKey = partial.substring(0, colonInPartial + 1);
            String valuePartial = partial.substring(colonInPartial + 1);
            com.mojang.brigadier.suggestion.SuggestionsBuilder valueBuilder =
                    builder.createOffset(lastWordStart + colonInPartial + 1);
            for (String option : options) {
                String optionLower = option.toLowerCase();
                int colon = optionLower.indexOf(':');
                boolean isValueEntry = colon >= 0 && colon < optionLower.length() - 1;
                if (!isValueEntry) continue;
                if (!optionLower.substring(0, colon + 1).equals(typedKey)) continue;
                String optionValue = option.substring(colon + 1);
                if (optionValue.toLowerCase().startsWith(valuePartial)) {
                    valueBuilder.suggest(optionValue);
                }
            }
            builder.add(valueBuilder);
        }
    }

    /**
     * Executes the pet creation logic.
     *
     * <p>Validates the pet type, checks world-group restrictions, resolves or registers the
     * {@link MyPetPlayer}, builds a {@link PersistedPet} with the parsed options, fires
     * {@link PetCreateEvent} and {@link PetSaveEvent}, persists the pet to the repository,
     * and activates it if the owner has no current pet.</p>
     *
     * @param sender     the command sender (for feedback messages)
     * @param force      if {@code true}, deactivates the player's current pet before creation
     * @param owner      the target player who will own the new pet
     * @param petType    the pet type to create (vanilla or custom ModelPet)
     * @param options    additional creation options (e.g. {@code "baby"}, {@code "variant:lucy"}, {@code "skilltree:Combat"})
     */
    private void executeCreate(CommandSender sender, boolean force, Player owner, PetType petType, String[] options) {
        // petType is resolved and validated by PetTypeArgument (vanilla via minecraft:, custom
        // ModelPet via mypet:), so no entity-type lookup or leashable check is needed here.
        if (petType.checkMinecraftVersion()) {
                if (WorldGroup.getGroupByWorld(owner.getWorld()).isDisabled()) {
                    sender.sendMessage(MessageUtil.prefixed(Component.text("Pets are not allowed in ").append(Component.text(owner.getWorld().getName()).color(NamedTextColor.GOLD))));
                    return;
                }

                final MyPetPlayer newOwner;
                if (MyPetApi.getPlayerManager().isMyPetPlayer(owner)) {
                    newOwner = MyPetApi.getPlayerManager().getMyPetPlayer(owner);

                    if (newOwner.hasPet() && force) {
                        MyPetApi.getPetManager().deactivatePet(newOwner, true);
                    }
                } else {
                    newOwner = MyPetApi.getPlayerManager().registerMyPetPlayer(owner);
                }

                // Apply per-type creation options (baby, variant:, type:, etc.)
                // via the same detached-mob bridge the petshop uses. Without
                // this, executeCreate silently drops every option except
                // skilltree: and name: (which updateData handles below) — the
                // mirror of the petshop bug fixed in Cluster L.
                PetEntitySnapshot.Result captured = PetEntitySnapshot.captureForOptions(
                        petType, options, owner.getWorld(), owner.getLocation());

                // Per-option validation errors (e.g. "variant:nonexistent") abort
                // creation. We surface each error to the sender so the admin can
                // correct the option, rather than silently spawning a default-
                // variant pet that doesn't match what they asked for.
                if (!captured.errors().isEmpty()) {
                    for (String err : captured.errors()) {
                        sender.sendMessage(MessageUtil.prefixed(Component.text(err).color(NamedTextColor.RED)));
                    }
                    return;
                }

                PersistedPet base = PersistedPet.builder(newOwner)
                        .petType(petType)
                        .petName(PetDefaultNameResolver.resolve(petType, newOwner))
                        .info(captured.info())
                        .build();
                final WorldGroup wg = WorldGroup.getGroupByWorld(owner.getWorld().getName());
                final PersistedPet inactivePet = updateData(base, options).withWorldGroup(wg.getName());

                PetCreateEvent createEvent = new PetCreateEvent(inactivePet, PetCreateEvent.Source.ADMIN_COMMAND);
                Bukkit.getServer().getPluginManager().callEvent(createEvent);

                PetSaveEvent saveEvent = new PetSaveEvent(inactivePet);
                Bukkit.getServer().getPluginManager().callEvent(saveEvent);

                MyPetPlugin.getInstance().getRepository().addPet(inactivePet).thenAccept(added -> owner.getScheduler().run(MyPetApi.getPlugin(), folaTask -> {
                        if (added) {
                            if (!newOwner.hasPet()) {
                                inactivePet.getOwner().setPetForWorldGroup(wg, inactivePet.getUUID());
                                MyPetPlugin.getInstance().getRepository().updateMyPetPlayer(inactivePet.getOwner());

                                Optional<Pet> pet = MyPetApi.getPetManager().activatePet(inactivePet);
                                if (pet.isPresent()) {
                                    pet.get().createEntity();
                                    sender.sendMessage(Locale.getComponent("Message.Command.Success", sender));
                                } else {
                                    sender.sendMessage(MessageUtil.prefixed(Component.text("Can't create Pet for " + newOwner.getName() + ". Is this player online?")));
                                }
                            } else {
                                sender.sendMessage(Locale.getComponent("Message.Command.Success", sender));
                            }
                        }
                }, null));
            }
    }

    /**
     * Applies non-NBT metadata from the options array to a {@link PersistedPet}
     * and returns the updated record (records being immutable).
     *
     * <p>Currently handles:</p>
     * <ul>
     *   <li>{@code skilltree:<name>} -- assigns the named skilltree, firing
     *       {@link PetSelectSkilltreeEvent} with source
     *       {@link PetSelectSkilltreeEvent.Source#ADMIN_CREATION}.</li>
     *   <li>{@code name:<petName>} -- sets the pet's display name.</li>
     * </ul>
     *
     * <p>This is separated from {@link PetCreationOptions#applyOptions} because
     * these properties live on the {@link PersistedPet} object rather than in
     * the NBT info compound.</p>
     */
    public static PersistedPet updateData(PersistedPet pet, String[] args) {
        for (String arg : args) {
            if (arg.startsWith("skilltree:")) {
                String skilltreeName = arg.replace("skilltree:", "");
                Skilltree skilltree = MyPetApi.getSkilltreeManager().getSkilltree(skilltreeName);
                if (skilltree != null) {
                    pet = pet.withSkilltree(skilltree);
                    Bukkit.getServer().getPluginManager().callEvent(
                            new PetSelectSkilltreeEvent(pet, skilltree, PetSelectSkilltreeEvent.Source.ADMIN_CREATION));
                }
            } else if (arg.startsWith("name:")) {
                String petName = arg.substring("name:".length());
                if (!petName.isBlank()) {
                    pet = pet.withPetName(petName);
                }
            }
        }
        return pet;
    }
}
