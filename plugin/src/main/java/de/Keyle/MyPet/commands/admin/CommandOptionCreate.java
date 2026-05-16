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
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.entity.CreationOptions;
import de.Keyle.MyPet.api.entity.PersistedPet;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.entity.PetBaby;
import de.Keyle.MyPet.api.entity.PetType;
import de.Keyle.MyPet.api.event.PetSaveEvent;
import de.Keyle.MyPet.api.event.PetSelectSkilltreeEvent;
import de.Keyle.MyPet.commands.help.CommandCategory;
import de.Keyle.MyPet.commands.help.HelpEntry;
import de.Keyle.MyPet.commands.help.HelpRegistry;
import de.Keyle.MyPet.entity.visual.PetEntitySnapshot;
import de.Keyle.MyPet.api.event.PetCreateEvent;
import de.Keyle.MyPet.api.exceptions.PetTypeNotFoundException;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.skill.skilltree.Skilltree;
import de.Keyle.MyPet.api.util.locale.Locale;
import de.Keyle.MyPet.util.translation.PetDefaultNameResolver;
import de.Keyle.MyPet.util.MessageUtil;
import de.Keyle.MyPet.commands.arguments.RegistryArgumentType;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.world.WeatheringCopperState;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Axolotl;
import org.bukkit.entity.Bee;
import org.bukkit.entity.Blaze;
import org.bukkit.entity.Camel;
import org.bukkit.entity.Cat;
import org.bukkit.entity.ChestedHorse;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.CopperGolem;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fox;
import org.bukkit.entity.Frog;
import org.bukkit.entity.Goat;
import org.bukkit.entity.Hoglin;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Llama;
import org.bukkit.entity.Mob;
import org.bukkit.entity.MushroomCow;
import org.bukkit.entity.Panda;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Pig;
import org.bukkit.entity.PiglinAbstract;
import org.bukkit.entity.Player;
import org.bukkit.entity.PufferFish;
import org.bukkit.entity.Rabbit;
import org.bukkit.entity.Salmon;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Snowman;
import org.bukkit.entity.Strider;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.TropicalFish;
import org.bukkit.entity.Vex;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Wolf;
import org.bukkit.entity.ZombieVillager;
import org.bukkit.inventory.ItemStack;

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
 * <p>The {@code type} argument accepts any Minecraft entity type that maps to a valid {@link PetType}.
 * Optional trailing arguments control the pet's appearance and metadata (e.g. {@code baby}, {@code saddle},
 * {@code variant:3}, {@code skilltree:Combat}, {@code name:Fluffy}).</p>
 *
 * <p>Requires the {@code MyPet.admin} permission.</p>
 *
 * <p>Per-type tab-completion options live on each {@code Pet<Type>} class via the
 * {@link CreationOptions} annotation; the {@code baby} option is auto-contributed for
 * any class implementing {@link PetBaby}.</p>
 */
public class CommandOptionCreate {

    /**
     * Custom {@link RegistryArgumentType} that filters the Paper entity-type registry to only include
     * entity types that have a corresponding {@link PetType} entry. This ensures tab-completion
     * only suggests valid pet types.
     */
    static final RegistryArgumentType<EntityType> PET_ENTITY_TYPE =
            RegistryArgumentType.of(RegistryKey.ENTITY_TYPE, entityType -> {
                try {
                    PetType.byEntityTypeName(entityType.name());
                    return true;
                } catch (PetTypeNotFoundException e) {
                    return false;
                }
            });

    /**
     * Options common to all pet types, appended to every suggestion list.
     */
    private static final List<String> COMMON_OPTIONS = List.of("skilltree:", "name:");

    /**
     * Returns the option strings accepted by {@code petType}. Composed from the
     * {@link CreationOptions} annotation on the {@code Pet<Type>} class (if
     * present) plus the {@code "baby"} option auto-contributed for every class
     * that implements {@link PetBaby}. Returns an empty list for types with
     * neither.
     */
    private static List<String> optionsFor(PetType petType) {
        Class<? extends Pet> cls = petType.getPetClass();
        List<String> opts = new ArrayList<>();
        if (PetBaby.class.isAssignableFrom(cls)) {
            opts.add("baby");
        }
        CreationOptions annot = cls.getAnnotation(CreationOptions.class);
        if (annot != null) {
            Collections.addAll(opts, annot.value());
        }
        return opts;
    }

    /**
     * Matches a single raw command-input token (e.g. {@code "minecraft:snow_golem"},
     * {@code "snow_golem"}, {@code "SnowGolem"}) to a registered {@link PetType},
     * or {@code null} if no match.
     */
    private static PetType matchPetType(String token) {
        String stripped = token.startsWith("minecraft:")
                ? token.substring("minecraft:".length())
                : token;
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
                player -> Permissions.has(player, "MyPet.admin")
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
     * <p>Combines the common options ({@code skilltree:}, {@code name:}) with the type-specific
     * options for the pet type found in the command input. Per-type options come from
     * {@link #optionsFor} (which consults the {@link CreationOptions} annotation on the
     * {@code Pet<Type>} class plus the {@link PetBaby} marker). Options already present in
     * the input are excluded; only the last whitespace-delimited word is prefix-matched.</p>
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
            options.addAll(optionsFor(matchedType));
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
     * {@link MyPetPlayer}, builds a {@link PersistedPet} with the parsed options, fires
     * {@link PetCreateEvent} and {@link PetSaveEvent}, persists the pet to the repository,
     * and activates it if the owner has no current pet.</p>
     *
     * @param sender     the command sender (for feedback messages)
     * @param force      if {@code true}, deactivates the player's current pet before creation
     * @param owner      the target player who will own the new pet
     * @param entityType the Bukkit entity type to create as a pet
     * @param options    additional creation options (e.g. {@code "baby"}, {@code "variant:3"}, {@code "skilltree:Combat"})
     */
    private void executeCreate(CommandSender sender, boolean force, Player owner, EntityType entityType, String[] options) {
        String lang = Locale.getCommandSenderLanguage(sender);

        try {
            PetType petType = PetType.byEntityTypeName(entityType.name());
            if (petType.checkMinecraftVersion() && MyPetApi.getPetInfo().isLeashableEntityType(entityType)) {
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
                CompoundBinaryTag optionsInfo = PetEntitySnapshot.captureForOptions(
                        petType, options, owner.getWorld(), owner.getLocation());

                PersistedPet base = PersistedPet.builder(newOwner)
                        .petType(petType)
                        .petName(PetDefaultNameResolver.resolve(petType, newOwner))
                        .info(optionsInfo)
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
        } catch (PetTypeNotFoundException e) {
            sender.sendMessage(Locale.getComponent("Message.Command.PetType.Unknown", lang));
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
     * <p>This is separated from {@link #createInfo} because these properties live
     * on the {@link PersistedPet} object rather than in the NBT info compound.</p>
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

    /**
     * Applies the option strings to a freshly-constructed Bukkit {@link Mob} by
     * calling per-type Bukkit setters directly. The caller is responsible for
     * supplying a mob of the type that matches {@code petType} (typically
     * obtained via {@code World#createEntity}, so the entity is detached from
     * the world's entity list — see Cluster L in
     * {@code docs/pet-type-issue-tracker.md}).
     *
     * <p>Options that don't match {@code mob}'s actual Bukkit type are silently
     * skipped (an admin's {@code variant:} on a type that has no variant just
     * does nothing). Per-option exceptions are logged and swallowed so a bad
     * single option doesn't prevent the rest from applying.
     *
     * @param petType the {@link PetType} being created — used to disambiguate
     *                option semantics where the same option string maps to
     *                different setters per type (e.g. {@code variant:} for
     *                Horse vs. Llama vs. TropicalFish)
     * @param args    the option strings to apply
     * @param mob     the target Bukkit mob — typically detached from any world
     */
    public static void applyOptions(PetType petType, String[] args, Mob mob) {
        for (String arg : args) {
            try {
                applyOption(petType, arg, mob);
            } catch (Throwable t) {
                MyPetApi.getLogger().warning("CommandOptionCreate.applyOptions: option '"
                        + arg + "' for " + petType.name() + " threw "
                        + t.getClass().getSimpleName() + ": " + t.getMessage());
            }
        }
    }

    private static void applyOption(PetType petType, String arg, Mob mob) {
        // Universal markers
        if (arg.equalsIgnoreCase("baby")) {
            if (mob instanceof Ageable a) a.setBaby();
            return;
        }
        if (arg.equalsIgnoreCase("tamed")) {
            if (mob instanceof Tameable t) t.setTamed(true);
            return;
        }
        // Type-narrowed booleans
        if (arg.equalsIgnoreCase("fire")) {
            if (mob instanceof Blaze) mob.setVisualFire(true);
            return;
        }
        if (arg.equalsIgnoreCase("noshake")) {
            if (mob instanceof Hoglin h) h.setImmuneToZombification(true);
            else if (mob instanceof PiglinAbstract p) p.setImmuneToZombification(true);
            return;
        }
        if (arg.equalsIgnoreCase("powered")) {
            if (mob instanceof Creeper c) c.setPowered(true);
            return;
        }
        if (arg.equalsIgnoreCase("screaming")) {
            if (mob instanceof Goat g) g.setScreaming(true);
            else if (mob instanceof Enderman e) e.setScreaming(true);
            return;
        }
        if (arg.equalsIgnoreCase("noLeftHorn")) {
            if (mob instanceof Goat g) g.setLeftHorn(false);
            return;
        }
        if (arg.equalsIgnoreCase("noRightHorn")) {
            if (mob instanceof Goat g) g.setRightHorn(false);
            return;
        }
        if (arg.equalsIgnoreCase("saddle")) {
            applySaddle(mob);
            return;
        }
        if (arg.equalsIgnoreCase("sheared")) {
            if (mob instanceof Sheep s) s.setSheared(true);
            else if (mob instanceof Snowman s) s.setDerp(true);
            return;
        }
        if (arg.equalsIgnoreCase("angry")) {
            if (mob instanceof Wolf w) w.setAngry(true);
            else if (mob instanceof Bee b) b.setAnger(400);
            return;
        }
        if (arg.equalsIgnoreCase("chest")) {
            if (mob instanceof ChestedHorse h) h.setCarryingChest(true);
            return;
        }
        if (arg.equalsIgnoreCase("glowing")) {
            if (mob instanceof Vex v) v.setCharging(true);
            return;
        }
        if (arg.equalsIgnoreCase("has-stung")) {
            if (mob instanceof Bee b) b.setHasStung(true);
            return;
        }
        if (arg.equalsIgnoreCase("has-nectar")) {
            if (mob instanceof Bee b) b.setHasNectar(true);
            return;
        }
        if (arg.equalsIgnoreCase("waxed")) {
            if (mob instanceof CopperGolem golem) {
                golem.setOxidizing(CopperGolem.Oxidizing.waxed());
            }
            return;
        }
        // Key:value options
        if (arg.startsWith("size:")) {
            applySize(arg.substring("size:".length()), mob);
        } else if (arg.startsWith("variant:")) {
            applyVariant(petType, arg.substring("variant:".length()), mob);
        } else if (arg.startsWith("profession:")) {
            applyProfession(arg.substring("profession:".length()), mob);
        } else if (arg.startsWith("color:")) {
            applyColor(arg.substring("color:".length()), mob);
        } else if (arg.startsWith("collar:")) {
            applyCollar(arg.substring("collar:".length()), mob);
        } else if (arg.startsWith("block:")) {
            applyBlock(arg.substring("block:".length()), mob);
        } else if (arg.startsWith("oxidation:")) {
            applyOxidation(arg.substring("oxidation:".length()), mob);
        } else if (arg.startsWith("puff:")) {
            applyPuff(arg, mob);
        } else if (arg.startsWith("main-gene:") || arg.startsWith("hidden-gene:")) {
            applyPandaGene(arg, mob);
        } else if (arg.startsWith("type:")) {
            applyType(arg.substring("type:".length()), mob);
        }
    }

    private static void applySaddle(Mob mob) {
        if (mob instanceof Pig p) p.setSaddle(true);
        else if (mob instanceof Strider s) s.setSaddle(true);
        else if (mob instanceof AbstractHorse h) h.getInventory().setSaddle(new ItemStack(Material.SADDLE));
        else if (mob instanceof Camel c) c.getInventory().setSaddle(new ItemStack(Material.SADDLE));
    }

    private static void applySize(String s, Mob mob) {
        if (!Util.isInt(s)) return;
        int n = Integer.parseInt(s);
        if (mob instanceof Slime slime) slime.setSize(Math.max(1, Math.min(8, n)));
        else if (mob instanceof Phantom phantom) phantom.setSize(Math.max(1, Math.min(64, n)));
    }

    /**
     * Applies the {@code variant:} option. The value is either an integer (most
     * variant-capable mobs use ordinal indices) or a registry key string
     * (Wolf / Cow / Chicken / Pig in 1.21.5+).
     */
    private static void applyVariant(PetType petType, String value, Mob mob) {
        if (Util.isInt(value)) {
            int n = Integer.parseInt(value);
            if (mob instanceof Axolotl axolotl) {
                Axolotl.Variant[] vals = Axolotl.Variant.values();
                axolotl.setVariant(vals[Math.floorMod(n, vals.length)]);
            } else if (mob instanceof Frog frog) {
                Frog.Variant[] vals = Frog.Variant.values();
                frog.setVariant(vals[Math.floorMod(n, vals.length)]);
            } else if (mob instanceof Parrot parrot) {
                Parrot.Variant[] vals = Parrot.Variant.values();
                parrot.setVariant(vals[Math.floorMod(n, vals.length)]);
            } else if (mob instanceof Rabbit rabbit) {
                if (n == 99) {
                    rabbit.setRabbitType(Rabbit.Type.THE_KILLER_BUNNY);
                } else {
                    Rabbit.Type[] vals = Rabbit.Type.values();
                    int idx = (n >= 0 && n < vals.length) ? n : 0;
                    rabbit.setRabbitType(vals[idx]);
                }
            } else if (mob instanceof Horse horse) {
                int packed = Math.max(0, Math.min(1030, n));
                Horse.Color[] colors = Horse.Color.values();
                Horse.Style[] styles = Horse.Style.values();
                horse.setColor(colors[Math.floorMod(packed & 0xFF, colors.length)]);
                horse.setStyle(styles[Math.floorMod((packed >> 8) & 0xFF, styles.length)]);
            } else if (mob instanceof Llama llama) {
                Llama.Color[] vals = Llama.Color.values();
                int idx = (n >= 0 && n < vals.length) ? n : 0;
                llama.setColor(vals[idx]);
            } else if (mob instanceof TropicalFish fish) {
                TropicalFish.Pattern[] patterns = TropicalFish.Pattern.values();
                fish.setPattern(patterns[Math.floorMod((n >> 8) & 0xFF, patterns.length)]);
            } else if (mob instanceof Salmon salmon) {
                // Salmon.Variant + setVariant were added in 1.21.2; on older
                // Paper the reference fails with LinkageError.
                try {
                    Salmon.Variant[] vals = Salmon.Variant.values();
                    salmon.setVariant(vals[Math.floorMod(n, vals.length)]);
                } catch (LinkageError ignored) {}
            }
            return;
        }
        // String form — Wolf via legacy Registry; Cow / Chicken / Pig via
        // 1.21.5+ Paper RegistryAccess. Calling the 1.21.5+ APIs on older
        // runtimes fails with LinkageError, which we swallow.
        String name = value.toLowerCase();
        if (mob instanceof Wolf wolf) {
            try {
                Wolf.Variant v = Registry.WOLF_VARIANT.get(NamespacedKey.minecraft(name));
                if (v != null) wolf.setVariant(v);
            } catch (Throwable ignored) {}
        } else if (mob instanceof Cow cow) {
            try {
                Cow.Variant v = RegistryAccess.registryAccess()
                        .getRegistry(RegistryKey.COW_VARIANT)
                        .get(Key.key("minecraft", name));
                if (v != null) cow.setVariant(v);
            } catch (LinkageError ignored) {}
        } else if (mob instanceof Chicken chicken) {
            try {
                Chicken.Variant v = RegistryAccess.registryAccess()
                        .getRegistry(RegistryKey.CHICKEN_VARIANT)
                        .get(Key.key("minecraft", name));
                if (v != null) chicken.setVariant(v);
            } catch (LinkageError ignored) {}
        } else if (mob instanceof Pig pig) {
            try {
                Pig.Variant v = RegistryAccess.registryAccess()
                        .getRegistry(RegistryKey.PIG_VARIANT)
                        .get(Key.key("minecraft", name));
                if (v != null) pig.setVariant(v);
            } catch (LinkageError ignored) {}
        }
    }

    private static void applyProfession(String s, Mob mob) {
        if (!Util.isInt(s)) return;
        int n = Math.max(0, Math.min(14, Integer.parseInt(s)));
        Villager.Profession[] profs = Villager.Profession.values();
        Villager.Profession prof = profs[Math.floorMod(n, profs.length)];
        if (mob instanceof Villager villager) {
            villager.setProfession(prof);
            villager.setVillagerLevel(1);
        } else if (mob instanceof ZombieVillager zv) {
            zv.setVillagerProfession(prof);
        }
    }

    private static void applyColor(String s, Mob mob) {
        if (!Util.isInt(s)) return;
        int n = Math.max(0, Math.min(15, Integer.parseInt(s)));
        DyeColor color = DyeColor.values()[n];
        if (mob instanceof Sheep sheep) sheep.setColor(color);
    }

    private static void applyCollar(String s, Mob mob) {
        if (!Util.isInt(s)) return;
        int n = Math.max(0, Math.min(15, Integer.parseInt(s)));
        DyeColor color = DyeColor.values()[n];
        if (mob instanceof Cat cat) cat.setCollarColor(color);
        else if (mob instanceof Wolf wolf) wolf.setCollarColor(color);
    }

    private static void applyBlock(String s, Mob mob) {
        if (!(mob instanceof Enderman enderman)) return;
        Material material = Material.matchMaterial(s);
        if (material == null) return;
        try {
            enderman.setCarriedBlock(material.createBlockData());
        } catch (Throwable ignored) {}
    }

    private static void applyOxidation(String s, Mob mob) {
        if (!(mob instanceof CopperGolem golem)) return;
        try {
            WeatheringCopperState state = WeatheringCopperState.valueOf(s.toUpperCase());
            golem.setWeatheringState(state);
        } catch (Throwable ignored) {}
    }

    private static void applyPuff(String arg, Mob mob) {
        if (!(mob instanceof PufferFish puffer)) return;
        switch (arg) {
            case "puff:none" -> puffer.setPuffState(0);
            case "puff:semi" -> puffer.setPuffState(1);
            case "puff:fully" -> puffer.setPuffState(2);
        }
    }

    private static void applyPandaGene(String arg, Mob mob) {
        if (!(mob instanceof Panda panda)) return;
        boolean isMain = arg.startsWith("main-gene:");
        String geneName = arg.substring(isMain ? "main-gene:".length() : "hidden-gene:".length()).toLowerCase();
        Panda.Gene gene = switch (geneName) {
            case "normal" -> Panda.Gene.NORMAL;
            case "lazy" -> Panda.Gene.LAZY;
            case "worried" -> Panda.Gene.WORRIED;
            case "playful" -> Panda.Gene.PLAYFUL;
            case "brown" -> Panda.Gene.BROWN;
            case "weak" -> Panda.Gene.WEAK;
            case "aggressive" -> Panda.Gene.AGGRESSIVE;
            default -> null;
        };
        if (gene == null) return;
        if (isMain) panda.setMainGene(gene);
        else panda.setHiddenGene(gene);
    }

    /**
     * Applies the {@code type:} option. Semantics vary by pet type:
     * {@code type:brown|red} for Mooshroom maps to {@link MushroomCow.Variant};
     * {@code type:red|white} for Fox maps to {@link Fox.Type}; {@code type:N}
     * (ordinal int) for Cat / Villager / ZombieVillager maps to their respective
     * type enums.
     */
    private static void applyType(String value, Mob mob) {
        String lower = value.toLowerCase();
        if (mob instanceof MushroomCow mooshroom) {
            mooshroom.setVariant(lower.equals("brown") ? MushroomCow.Variant.BROWN : MushroomCow.Variant.RED);
        } else if (mob instanceof Fox fox) {
            fox.setFoxType(lower.equals("white") ? Fox.Type.SNOW : Fox.Type.RED);
        } else if (mob instanceof Cat cat && Util.isInt(value)) {
            int n = Util.clamp(Integer.parseInt(value), 0, 10);
            Cat.Type[] vals = Cat.Type.values();
            cat.setCatType(vals[Math.floorMod(n, vals.length)]);
        } else if (mob instanceof Villager villager && Util.isInt(value)) {
            int n = Util.clamp(Integer.parseInt(value), 0, 6);
            Villager.Type[] vals = Villager.Type.values();
            villager.setVillagerType(vals[Math.floorMod(n, vals.length)]);
        } else if (mob instanceof ZombieVillager zv && Util.isInt(value)) {
            int n = Util.clamp(Integer.parseInt(value), 0, 6);
            Villager.Type[] vals = Villager.Type.values();
            zv.setVillagerType(vals[Math.floorMod(n, vals.length)]);
        }
    }
}
