/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2020 Keyle
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
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.commands.CommandCategory;
import de.Keyle.MyPet.api.commands.CommandOptionCreator;
import de.Keyle.MyPet.api.commands.HelpEntry;
import de.Keyle.MyPet.api.commands.HelpRegistry;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.event.MyPetCreateEvent;
import de.Keyle.MyPet.api.event.MyPetSaveEvent;
import de.Keyle.MyPet.api.event.MyPetSelectSkilltreeEvent;
import de.Keyle.MyPet.api.exceptions.MyPetTypeNotFoundException;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.skill.skilltree.Skilltree;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.Keyle.MyPet.entity.InactiveMyPet;
import de.Keyle.MyPet.util.MessageUtil;
import de.Keyle.MyPet.commands.arguments.RegistryArgumentType;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Admin subcommand that creates a new pet for a target player.
 *
 * <p>Usage:</p>
 * <ul>
 *   <li>{@code /petadmin create <player> <type> [options...]} -- creates a pet if the player has no active pet</li>
 *   <li>{@code /petadmin create -f <player> <type> [options...]} -- force-creates a pet, deactivating any existing one</li>
 * </ul>
 *
 * <p>The {@code type} argument accepts any Minecraft entity type that maps to a valid {@link MyPetType}.
 * Optional trailing arguments control the pet's appearance and metadata (e.g. {@code baby}, {@code saddle},
 * {@code variant:3}, {@code skilltree:Combat}, {@code name:Fluffy}).</p>
 *
 * <p>Requires the {@code MyPet.admin} permission.</p>
 *
 * @see CommandOptionCreator helper used to build per-type option lists
 */
@SuppressWarnings("UnstableApiUsage")
public class CommandOptionCreate {

    /**
     * Custom {@link RegistryArgumentType} that filters the Paper entity-type registry to only include
     * entity types that have a corresponding {@link MyPetType} entry. This ensures tab-completion
     * only suggests valid pet types.
     */
    static final RegistryArgumentType<EntityType> PET_ENTITY_TYPE =
            RegistryArgumentType.of(RegistryKey.ENTITY_TYPE, entityType -> {
                try {
                    MyPetType.byEntityTypeName(entityType.name());
                    return true;
                } catch (MyPetTypeNotFoundException e) {
                    return false;
                }
            });

    /**
     * Maps lowercase pet type names (with underscores removed, e.g. {@code "zombievillager"}) to
     * the list of type-specific option strings available for that pet. Used by {@link #suggestOptions}
     * to provide context-aware tab-completion. Populated in the static initializer block below.
     */
    private static final Map<String, List<String>> petTypeOptionMap = new HashMap<>();

    /**
     * Options common to all pet types, appended to every suggestion list. Currently includes
     * {@code skilltree:} and {@code name:}.
     */
    private static final List<String> commonTypeOptionList = new ArrayList<>();

    /* Populates petTypeOptionMap and commonTypeOptionList with all known pet types and their options. */
    static {
        commonTypeOptionList.add("skilltree:");
        commonTypeOptionList.add("name:");

        petTypeOptionMap.put("axolotl", new CommandOptionCreator()
                .add("baby")
                .add("variant:")
                .get());

        petTypeOptionMap.put("armadillo", new CommandOptionCreator()
                .add("baby")
                .get());

        petTypeOptionMap.put("bee", new CommandOptionCreator()
                .add("baby")
                .add("angry")
                .add("has-stung")
                .add("has-nectar")
                .get());

        petTypeOptionMap.put("blaze", new CommandOptionCreator()
                .add("fire")
                .get());

        petTypeOptionMap.put("bogged", new CommandOptionCreator()
                .get());

        petTypeOptionMap.put("breeze", new CommandOptionCreator()
                .get());

        petTypeOptionMap.put("chicken", new CommandOptionCreator()
                .add("baby")
                .add("1.21.5", "variant:")
                .get());

        petTypeOptionMap.put("camel", new CommandOptionCreator()
                .add("baby")
                .add("saddle")
                .get());

        petTypeOptionMap.put("cat", new CommandOptionCreator()
                .add("baby")
                .add("type:")
                .add("collar:")
                .add("tamed")
                .get());

        petTypeOptionMap.put("cow", new CommandOptionCreator()
                .add("baby")
                .add("1.21.5", "variant:")
                .get());

        petTypeOptionMap.put("creeper", new CommandOptionCreator()
                .add("powered")
                .get());

        petTypeOptionMap.put("donkey", new CommandOptionCreator()
                .add("baby")
                .add("saddle")
                .add("chest")
                .get());

        petTypeOptionMap.put("drowned", new CommandOptionCreator()
                .add("baby")
                .get());

        petTypeOptionMap.put("enderman", new CommandOptionCreator()
                .add("block:")
                .add("screaming")
                .get());

        petTypeOptionMap.put("frog", new CommandOptionCreator()
                .add("variant:")
                .get());

        petTypeOptionMap.put("fox", new CommandOptionCreator()
                .add("baby")
                .add("type:red")
                .add("type:white")
                .get());

        petTypeOptionMap.put("goat", new CommandOptionCreator()
                .add("baby")
                .add("screaming")
                .add("noLeftHorn")
                .add("noRightHorn")
                .get());

        petTypeOptionMap.put("glowsquid", new CommandOptionCreator()
                .get());

        petTypeOptionMap.put("guardian", new CommandOptionCreator()
                .get());

        petTypeOptionMap.put("hoglin", new CommandOptionCreator()
                .add("baby")
                .add("noshake")
                .get());

        petTypeOptionMap.put("horse", new CommandOptionCreator()
                .add("baby")
                .add("saddle")
                .add("variant:")
                .get());

        petTypeOptionMap.put("husk", new CommandOptionCreator()
                .add("baby")
                .get());

        petTypeOptionMap.put("llama", new CommandOptionCreator()
                .add("baby")
                .add("chest")
                .add("variant:")
                //.add("decor:")
                .get());

        petTypeOptionMap.put("magmacube", new CommandOptionCreator()
                .add("size:")
                .get());

        petTypeOptionMap.put("mooshroom", new CommandOptionCreator()
                .add("baby")
                .add("type:brown")
                .add("type:red")
                .get());

        petTypeOptionMap.put("mule", new CommandOptionCreator()
                .add("baby")
                .add("saddle")
                .add("chest")
                .get());

        petTypeOptionMap.put("ocelot", new CommandOptionCreator()
                .add("baby")
                .get());

        petTypeOptionMap.put("panda", new CommandOptionCreator()
                .add("baby")
                .add("main-gene:lazy")
                .add("main-gene:worried")
                .add("main-gene:playful")
                .add("main-gene:aggressive")
                .add("main-gene:weak")
                .add("main-gene:brown")
                .add("main-gene:normal")
                .add("hidden-gene:lazy")
                .add("hidden-gene:worried")
                .add("hidden-gene:playful")
                .add("hidden-gene:aggressive")
                .add("hidden-gene:weak")
                .add("hidden-gene:brown")
                .add("hidden-gene:normal")
                .get());

        petTypeOptionMap.put("parrot", new CommandOptionCreator()
                .add("variant:")
                .get());

        petTypeOptionMap.put("phantom", new CommandOptionCreator()
                .add("size:")
                .get());

        petTypeOptionMap.put("pig", new CommandOptionCreator()
                .add("baby")
                .add("saddle")
                .add("1.21.5", "variant:")
                .get());

        petTypeOptionMap.put("piglin", new CommandOptionCreator()
                .add("baby")
                .add("noshake")
                .get());

        petTypeOptionMap.put("piglinbrute", new CommandOptionCreator()
                .add("noshake")
                .get());

        petTypeOptionMap.put("polarbear", new CommandOptionCreator()
                .add("baby")
                .get());

        petTypeOptionMap.put("pufferfish", new CommandOptionCreator()
                .add("puff:none")
                .add("puff:semi")
                .add("puff:fully")
                .get());

        petTypeOptionMap.put("rabbit", new CommandOptionCreator()
                .add("baby")
                .add("variant:")
                .get());

        petTypeOptionMap.put("sheep", new CommandOptionCreator()
                .add("baby")
                .add("color:")
                .add("sheared")
                .get());

        petTypeOptionMap.put("skeleton", new CommandOptionCreator()
                .get());

        petTypeOptionMap.put("skeletonhorse", new CommandOptionCreator()
                .add("baby")
                .add("saddle")
                .get());

        petTypeOptionMap.put("slime", new CommandOptionCreator()
                .add("size:")
                .get());

        petTypeOptionMap.put("sniffer", new CommandOptionCreator()
                .add("baby")
                .get());

        petTypeOptionMap.put("snowgolem", new CommandOptionCreator()
                .add("sheared")
                .get());

        petTypeOptionMap.put("strider", new CommandOptionCreator()
                .add("saddle")
                .add("baby")
                .get());

        petTypeOptionMap.put("traderllama", new CommandOptionCreator()
                .add("baby")
                .add("chest")
                .add("variant:")
                //.add("decor:")
                .get());

        petTypeOptionMap.put("tropicalfish", new CommandOptionCreator()
                .add("variant:")
                .get());

        petTypeOptionMap.put("turtle", new CommandOptionCreator()
                .add("baby")
                .get());

        petTypeOptionMap.put("vex", new CommandOptionCreator()
                .add("glowing")
                .get());

        petTypeOptionMap.put("villager", new CommandOptionCreator()
                .add("baby")
                .add("profession:")
                .add("type:")
                .get());

        petTypeOptionMap.put("wither", new CommandOptionCreator()
                .add("baby")
                .get());

        petTypeOptionMap.put("wolf", new CommandOptionCreator()
                .add("baby")
                .add("angry")
                .add("tamed")
                .add("collar:")
                .add("variant:")
                .get());

        petTypeOptionMap.put("zombie", new CommandOptionCreator()
                .add("baby")
                .get());

        petTypeOptionMap.put("zombiehorse", new CommandOptionCreator()
                .add("baby")
                .add("saddle")
                .get());

        petTypeOptionMap.put("zombievillager", new CommandOptionCreator()
                .add("baby")
                .add("profession:")
                .add("type:")
                .get());

        petTypeOptionMap.put("zombifiedpiglin", new CommandOptionCreator()
                .add("baby")
                .get());

        petTypeOptionMap.put("zoglin", new CommandOptionCreator()
                .add("baby")
                .get());
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
                player -> Permissions.has(player, "MyPet.admin", false)
        ));

        return Commands.literal("create")
                // /petadmin create -f <player> <type> [options...]
                .then(Commands.literal("-f")
                        .then(Commands.argument("player", ArgumentTypes.player())
                                .then(Commands.argument("type", PET_ENTITY_TYPE)
                                        .executes(ctx -> {
                                            Player player = ctx.getArgument("player", PlayerSelectorArgumentResolver.class)
                                                    .resolve(ctx.getSource()).getFirst();
                                            executeCreate(ctx.getSource().getSender(),
                                                    true, player,
                                                    ctx.getArgument("type", EntityType.class),
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
                                                            ctx.getArgument("type", EntityType.class),
                                                            StringArgumentType.getString(ctx, "options").split(" "));
                                                    return Command.SINGLE_SUCCESS;
                                                })))))
                // /petadmin create <player> <type> [options...]
                .then(Commands.argument("player", ArgumentTypes.player())
                        .then(Commands.argument("type", PET_ENTITY_TYPE)
                                .executes(ctx -> {
                                    Player player = ctx.getArgument("player", PlayerSelectorArgumentResolver.class)
                                            .resolve(ctx.getSource()).getFirst();
                                    executeCreate(ctx.getSource().getSender(),
                                            false, player,
                                            ctx.getArgument("type", EntityType.class),
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
                                                    ctx.getArgument("type", EntityType.class),
                                                    StringArgumentType.getString(ctx, "options").split(" "));
                                            return Command.SINGLE_SUCCESS;
                                        }))))
                .build();
    }

    /**
     * Populates Brigadier tab-completion suggestions for the trailing {@code options} argument.
     *
     * <p>The method inspects the full command input to determine the pet type being created, then
     * combines the common options ({@code skilltree:}, {@code name:}) with the type-specific options
     * from {@link #petTypeOptionMap}. Options already present in the input are excluded from suggestions.
     * Only the last whitespace-delimited word is used for prefix matching.</p>
     *
     * @param input   the full raw command input string from the Brigadier context
     * @param builder the suggestions builder to populate with matching completions
     */
    private void suggestOptions(String input, com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
        // Extract the pet type from the full command input to provide type-specific suggestions
        String[] parts = input.split(" ");

        // Find pet type in the command parts — match minecraft:zombie or zombie format
        String petTypeLower = null;
        for (String part : parts) {
            String stripped = part.startsWith("minecraft:") ? part.substring("minecraft:".length()) : part;
            String key = stripped.toLowerCase().replace("_", "");
            if (petTypeOptionMap.containsKey(key)) {
                petTypeLower = key;
                break;
            }
        }

        List<String> options = new ArrayList<>(commonTypeOptionList);
        if (petTypeLower != null && petTypeOptionMap.containsKey(petTypeLower)) {
            options.addAll(petTypeOptionMap.get(petTypeLower));
        }

        // Collect already-entered options so we don't suggest them again
        String remaining = builder.getRemaining();
        String[] enteredWords = remaining.split(" ");
        Set<String> alreadyUsed = new HashSet<>();
        for (String word : enteredWords) {
            String lower = word.toLowerCase();
            // For key:value options (e.g. "variant:red"), track the key prefix
            int colon = lower.indexOf(':');
            if (colon >= 0) {
                alreadyUsed.add(lower.substring(0, colon + 1));
            } else {
                alreadyUsed.add(lower);
            }
        }

        // Rebuild the builder offset to only complete the last word,
        // not replace the entire greedy string
        int lastSpace = remaining.lastIndexOf(' ');
        com.mojang.brigadier.suggestion.SuggestionsBuilder lastWordBuilder =
                builder.createOffset(builder.getStart() + lastSpace + 1);
        String partial = lastWordBuilder.getRemaining().toLowerCase();

        for (String option : options) {
            String optionLower = option.toLowerCase();
            // Check if this option (or its key prefix) was already used
            int colon = optionLower.indexOf(':');
            String key = colon >= 0 ? optionLower.substring(0, colon + 1) : optionLower;
            if (alreadyUsed.contains(key)) {
                continue;
            }
            if (optionLower.startsWith(partial)) {
                lastWordBuilder.suggest(option);
            }
        }

        builder.add(lastWordBuilder);
    }

    /**
     * Executes the pet creation logic.
     *
     * <p>Validates the pet type, checks world-group restrictions, resolves or registers the
     * {@link MyPetPlayer}, builds an {@link InactiveMyPet} with the parsed options, fires
     * {@link MyPetCreateEvent} and {@link MyPetSaveEvent}, persists the pet to the repository,
     * and activates it if the owner has no current pet.</p>
     *
     * @param sender     the command sender (for feedback messages)
     * @param force      if {@code true}, deactivates the player's current pet before creation
     * @param owner      the target player who will own the new pet
     * @param entityType the Bukkit entity type to create as a pet
     * @param options    additional creation options (e.g. {@code "baby"}, {@code "variant:3"}, {@code "skilltree:Combat"})
     */
    private void executeCreate(CommandSender sender, boolean force, Player owner, EntityType entityType, String[] options) {
        String lang = MyPetApi.getPlatformHelper().getCommandSenderLanguage(sender);

        try {
            MyPetType myPetType = MyPetType.byEntityTypeName(entityType.name());
            if (myPetType.checkMinecraftVersion() && MyPetApi.getMyPetInfo().isLeashableEntityType(entityType)) {
                if (WorldGroup.getGroupByWorld(owner.getWorld()).isDisabled()) {
                    sender.sendMessage(MessageUtil.prefixed(Component.text("Pets are not allowed in ").append(Component.text(owner.getWorld().getName()).color(NamedTextColor.GOLD))));
                    return;
                }

                final MyPetPlayer newOwner;
                if (MyPetApi.getPlayerManager().isMyPetPlayer(owner)) {
                    newOwner = MyPetApi.getPlayerManager().getMyPetPlayer(owner);

                    if (newOwner.hasMyPet() && force) {
                        MyPetApi.getMyPetManager().deactivateMyPet(newOwner, true);
                    }
                } else {
                    newOwner = MyPetApi.getPlayerManager().registerMyPetPlayer(owner);
                }

                final InactiveMyPet inactiveMyPet = new InactiveMyPet(newOwner);
                inactiveMyPet.setPetType(myPetType);
                inactiveMyPet.setPetName(Translation.getString("Name." + inactiveMyPet.getPetType().name(), inactiveMyPet.getOwner()));

                updateData(inactiveMyPet, options);

                final WorldGroup wg = WorldGroup.getGroupByWorld(owner.getWorld().getName());

                inactiveMyPet.setWorldGroup(wg.getName());

                MyPetCreateEvent createEvent = new MyPetCreateEvent(inactiveMyPet, MyPetCreateEvent.Source.AdminCommand);
                Bukkit.getServer().getPluginManager().callEvent(createEvent);

                MyPetSaveEvent saveEvent = new MyPetSaveEvent(inactiveMyPet);
                Bukkit.getServer().getPluginManager().callEvent(saveEvent);

                MyPetApi.getRepository().addPet(inactiveMyPet).thenAccept(added -> owner.getScheduler().run(MyPetApi.getPlugin(), folaTask -> {
                        if (added) {
                            if (!newOwner.hasMyPet()) {
                                inactiveMyPet.getOwner().setMyPetForWorldGroup(wg, inactiveMyPet.getUUID());
                                MyPetApi.getRepository().updateMyPetPlayer(inactiveMyPet.getOwner());

                                Optional<MyPet> myPet = MyPetApi.getMyPetManager().activateMyPet(inactiveMyPet);
                                if (myPet.isPresent()) {
                                    myPet.get().createEntity();
                                    sender.sendMessage(Translation.getComponent("Message.Command.Success", sender));
                                } else {
                                    sender.sendMessage(MessageUtil.prefixed(Component.text("Can't create MyPet for " + newOwner.getName() + ". Is this player online?")));
                                }
                            } else {
                                sender.sendMessage(Translation.getComponent("Message.Command.Success", sender));
                            }
                        }
                }, null));
            }
        } catch (MyPetTypeNotFoundException e) {
            sender.sendMessage(Translation.getComponent("Message.Command.PetType.Unknown", lang));
        }
    }

    /**
     * Applies non-NBT metadata from the options array to an {@link InactiveMyPet}.
     *
     * <p>Currently handles:</p>
     * <ul>
     *   <li>{@code skilltree:<name>} -- assigns the named skilltree to the pet</li>
     *   <li>{@code name:<petName>} -- sets the pet's display name</li>
     * </ul>
     *
     * <p>This is separated from {@link #createInfo} because these properties live on the
     * {@link InactiveMyPet} object rather than in the NBT info compound.</p>
     *
     * @param inactiveMyPet the pet instance to update
     * @param args          the option strings to parse
     */
    public static void updateData(InactiveMyPet inactiveMyPet, String[] args) {
        for (String arg : args) {
            if (arg.startsWith("skilltree:")) {
                String skilltreeName = arg.replace("skilltree:", "");
                Skilltree skilltree = MyPetApi.getSkilltreeManager().getSkilltree(skilltreeName);
                if (skilltree != null) {
                    inactiveMyPet.setSkilltree(skilltree, MyPetSelectSkilltreeEvent.Source.AdminCreation);
                }
            } else if (arg.startsWith("name:")) {
                String petName = arg.substring("name:".length());
                if (!petName.isBlank()) {
                    inactiveMyPet.setPetName(petName);
                }
            }
        }
    }

    /**
     * Parses the option strings and writes type-specific NBT data into the provided builder.
     *
     * <p>Handles boolean flags (e.g. {@code baby}, {@code saddle}, {@code powered}), numeric
     * values (e.g. {@code size:}, {@code variant:}, {@code color:}), string identifiers
     * (e.g. {@code block:}, wolf/cow/chicken/pig string variants), and composite keys
     * (e.g. {@code puff:semi}, {@code main-gene:lazy}, {@code type:red}).</p>
     *
     * <p>Pet-type-specific validation is applied where appropriate (e.g. clamping horse variant
     * to 0-1030, rabbit variant to 0-5 or 99, llama variant to 0-3).</p>
     *
     * @param petType the {@link MyPetType} being created, used for type-specific variant handling
     * @param args    the option strings to parse
     * @param builder the NBT compound builder to populate with parsed data
     */
    public static void createInfo(MyPetType petType, String[] args, CompoundBinaryTag.Builder builder) {
        for (String arg : args) {
            if (arg.equalsIgnoreCase("baby")) {
                builder.putBoolean("Baby", true);
            } else if (arg.equalsIgnoreCase("fire")) {
                builder.putBoolean("Fire", true);
            } else if (arg.equalsIgnoreCase("noshake")) {
                builder.putBoolean("ShakeImmune", true);
            } else if (arg.equalsIgnoreCase("powered")) {
                builder.putBoolean("Powered", true);
            } else if (arg.equalsIgnoreCase("screaming")) {
                builder.putBoolean("Screaming", true);
            } else if (arg.equalsIgnoreCase("noLeftHorn")) {
                builder.putBoolean("LeftHorn", false);
            } else if (arg.equalsIgnoreCase("noRightHorn")) {
                builder.putBoolean("RightHorn", false);
            } else if (arg.equalsIgnoreCase("saddle")) {
                builder.putBoolean("Saddle", true);
            } else if (arg.equalsIgnoreCase("sheared")) {
                builder.putBoolean("Sheared", true);
            } else if (arg.equalsIgnoreCase("tamed")) {
                builder.putBoolean("Tamed", true);
            } else if (arg.equalsIgnoreCase("angry")) {
                builder.putBoolean("Angry", true);
            } else if (arg.equalsIgnoreCase("villager")) {
                builder.putBoolean("Villager", true);
            } else if (arg.equalsIgnoreCase("chest")) {
                builder.putBoolean("Chest", true);
            } else if (arg.equalsIgnoreCase("glowing")) {
                builder.putBoolean("Glowing", true);
            } else if (arg.equalsIgnoreCase("has-stung")) {
                builder.putBoolean("HasStung", true);
            } else if (arg.equalsIgnoreCase("has-nectar")) {
                builder.putBoolean("HasNectar", true);
            } else if (arg.startsWith("size:")) {
                String size = arg.replace("size:", "");
                if (Util.isInt(size)) {
                    builder.putInt("Size", Integer.parseInt(size));
                }
            } else if (arg.startsWith("variant:")) {
                String variantString = arg.replace("variant:", "");
                if (Util.isInt(variantString)) {
                    int variant = Integer.parseInt(variantString);
                    if (petType.equals(MyPetType.byName("Horse"))) {
                        variant = Math.min(Math.max(0, variant), 1030);
                        builder.putInt("Variant", variant);
                    } else if (petType.equals(MyPetType.byName("Rabbit"))) {
                        if (variant != 99 && (variant > 5 || variant < 0)) {
                            variant = 0;
                        }
                        builder.putByte("Variant", (byte) variant);
                    } else if (petType.equals(MyPetType.byName("Llama")) || petType.equals(MyPetType.byName("TraderLlama"))) {
                        if (variant > 3 || variant < 0) {
                            variant = 0;
                        }
                        builder.putInt("Variant", variant);
                    } else if (petType.equals(MyPetType.byName("Parrot"))) {
                        builder.putInt("Variant", variant);
                    } else if (petType.equals(MyPetType.byName("Axolotl"))) {
                        builder.putInt("Variant", variant);
                    } else if (petType.equals(MyPetType.byName("Frog"))) {
                        builder.putInt("FrogType", variant);
                    } else if (petType.equals(MyPetType.byName("TropicalFish"))) {
                        builder.putInt("Variant", variant);
                    }
                } else if (petType.equals(MyPetType.byName("Wolf"))) {
                    // Wolf Variants are handled as (lowercase) Strings.
                    builder.putString("Variant", variantString.toLowerCase());
                } else if (petType.equals(MyPetType.byName("Cow")) || petType.equals(MyPetType.byName("Chicken")) || petType.equals(MyPetType.byName("Pig"))) {
                    // Cow/chicken/pig Variants are handled as (lowercase) Strings.
                    builder.putString("Variant", variantString.toLowerCase());
                }
            } else if (arg.startsWith("heartattack") && petType.equals(MyPetType.byName("Warden"))) {
                builder.putBoolean("HeartAttack", true);
            } else if (arg.startsWith("profession:")) {
                String professionString = arg.replace("profession:", "");
                if (Util.isInt(professionString)) {
                    int profession = Integer.parseInt(professionString);
                    profession = Math.min(Math.max(0, profession), 14);
                    if (petType.equals(MyPetType.byName("Villager"))) {
                        builder.putInt("Profession", profession);
                        builder.putInt("VillagerLevel", 1);
                    } else if (petType.equals(MyPetType.byName("Zombie")) || petType.equals(MyPetType.byName("ZombieVillager"))) {
                        builder.putBoolean("Villager", true);
                        builder.putInt("Profession", profession);
                        builder.putInt("TradingLevel", 1);
                    }
                }
            } else if (arg.startsWith("color:")) {
                String colorString = arg.replace("color:", "");
                if (Util.isByte(colorString)) {
                    byte color = Byte.parseByte(colorString);
                    color = color > 15 ? 15 : color < 0 ? 0 : color;
                    builder.putByte("Color", color);
                }
            } else if (arg.startsWith("collar:")) {
                String colorString = arg.replace("collar:", "");
                if (Util.isByte(colorString)) {
                    byte color = Byte.parseByte(colorString);
                    color = color > 15 ? 15 : color < 0 ? 0 : color;
                    builder.putByte("CollarColor", color);
                }
            } else if (arg.startsWith("block:")) {
                String block = arg.replace("block:", "");
                if (Material.matchMaterial(block) != null) {
                    builder.putString("BlockName", block.toLowerCase());
                }
            } else if (arg.startsWith("puff:")) {
                switch (arg) {
                    case "puff:none":
                        builder.putInt("PuffState", 0);
                        break;
                    case "puff:semi":
                        builder.putInt("PuffState", 1);
                        break;
                    case "puff:fully":
                        builder.putInt("PuffState", 2);
                        break;
                }
            } else if (arg.startsWith("main-gene:") || arg.startsWith("hidden-gene:")) {
                String gene;
                String key;
                if (arg.startsWith("main-gene:")) {
                    key = "MainGene";
                    gene = arg.substring(10);
                } else {
                    key = "HiddenGene";
                    gene = arg.substring(12);
                }
                switch (gene.toLowerCase()) {
                    case "normal":
                        builder.putInt(key, 0);
                        break;
                    case "lazy":
                        builder.putInt(key, 1);
                        break;
                    case "worried":
                        builder.putInt(key, 2);
                        break;
                    case "playful":
                        builder.putInt(key, 3);
                        break;
                    case "brown":
                        builder.putInt(key, 4);
                        break;
                    case "weak":
                        builder.putInt(key, 5);
                        break;
                    case "aggressive":
                        builder.putInt(key, 6);
                        break;
                }
            } else if (arg.startsWith("type:")) {
                switch (petType.name()) {
                    case "Fox":
                        switch (arg) {
                            case "type:white":
                                builder.putInt("FoxType", 1);
                                break;
                            case "type:red":
                            default:
                                builder.putInt("FoxType", 0);
                                break;
                        }
                        break;
                    case "Mooshroom":
                        switch (arg) {
                            case "type:brown":
                                builder.putInt("CowType", 1);
                                break;
                            case "type:red":
                            default:
                                builder.putInt("CowType", 0);
                                break;
                        }
                        break;
                    case "Cat":
                        String catTypeString = arg.replace("type:", "");
                        if (Util.isInt(catTypeString)) {
                            int catType = Integer.parseInt(catTypeString);
                            catType = Util.clamp(catType, 0, 10);
                            builder.putInt("CatType", catType);
                        }
                        break;
                    case "Villager":
                    case "ZombieVillager":
                        String villagerTypeString = arg.replace("type:", "");
                        if (Util.isInt(villagerTypeString)) {
                            int villagerType = Integer.parseInt(villagerTypeString);
                            villagerType = Util.clamp(villagerType, 0, 6);
                            builder.putInt("VillagerType", villagerType);
                        }
                        break;
                }
            }
        }
    }
}
