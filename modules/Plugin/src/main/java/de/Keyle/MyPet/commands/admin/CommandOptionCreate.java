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

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.commands.CommandOptionCreator;
import de.Keyle.MyPet.api.commands.CommandOptionTabCompleter;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.event.MyPetCreateEvent;
import de.Keyle.MyPet.api.event.MyPetSaveEvent;
import de.Keyle.MyPet.api.exceptions.MyPetTypeNotFoundException;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.repository.RepositoryCallback;
import de.Keyle.MyPet.api.skill.skilltree.Skilltree;
import de.Keyle.MyPet.api.util.inventory.material.ItemDatabase;
import de.Keyle.MyPet.api.util.inventory.material.MaterialHolder;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.Keyle.MyPet.api.util.service.types.RepositoryMyPetConverterService;
import de.Keyle.MyPet.entity.InactiveMyPet;
import de.keyle.knbt.TagByte;
import de.keyle.knbt.TagCompound;
import de.keyle.knbt.TagInt;
import de.keyle.knbt.TagString;
import org.apache.commons.lang.ArrayUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.*;

public class CommandOptionCreate implements CommandOptionTabCompleter {

    private static List<String> petTypeList = new ArrayList<>();
    private static Map<String, List<String>> petTypeOptionMap = new HashMap<>();
    private static List<String> commonTypeOptionList = new ArrayList<>();

    static {
        commonTypeOptionList.add("skilltree:");
        commonTypeOptionList.add("name:");

        petTypeOptionMap.put("bee", new CommandOptionCreator()
                .add("baby")
                .add("angry")
                .add("has-stung")
                .add("has-nectar")
                .get());

        petTypeOptionMap.put("blaze", new CommandOptionCreator()
                .add("fire")
                .get());

        petTypeOptionMap.put("chicken", new CommandOptionCreator()
                .add("baby")
                .get());

        petTypeOptionMap.put("cat", new CommandOptionCreator()
                .add("baby")
                .add("type:")
                .add("collar:")
                .add("tamed")
                .get());

        petTypeOptionMap.put("cow", new CommandOptionCreator()
                .add("baby")
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
                .get());

        petTypeOptionMap.put("fox", new CommandOptionCreator()
                .add("type:red")
                .add("type:white")
                .get());

        petTypeOptionMap.put("guardian", new CommandOptionCreator()
                .add("1.7.10", "1.11", "elder")
                .get());

        petTypeOptionMap.put("horse", new CommandOptionCreator()
                .add("baby")
                .add("saddle")
                .add("variant:")
                .add("horse:")
                .add("1.7.10", "1.11", "chest")
                .add("1.7.10", "1.11", "donkey:")
                .add("1.7.10", "1.11", "mule:")
                .add("1.7.10", "1.11", "zombie:")
                .add("1.7.10", "1.11", "skeleton:")
                .get());

        petTypeOptionMap.put("irongolem", new CommandOptionCreator()
                .add("flower")
                .get());

        petTypeOptionMap.put("llama", new CommandOptionCreator()
                .add("baby")
                //.add("chest")
                .add("variant:")
                //.add("decor:")
                .get());

        petTypeOptionMap.put("magmacube", new CommandOptionCreator()
                .add("size:")
                .get());

        petTypeOptionMap.put("mooshroom", new CommandOptionCreator()
                .add("baby")
                .add("1.14", "type:brown")
                .add("1.14", "type:red")
                .get());

        petTypeOptionMap.put("mule", new CommandOptionCreator()
                .add("baby")
                .add("saddle")
                .add("chest")
                .get());

        petTypeOptionMap.put("ocelot", new CommandOptionCreator()
                .add("baby")
                .add("1.7.10", "1.14", "cat:")
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
                .get());

        petTypeOptionMap.put("pigzombie", new CommandOptionCreator()
                .add("baby")
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
                .add("rainbow")
                .get());

        petTypeOptionMap.put("skeleton", new CommandOptionCreator()
                .add("baby")
                .add("1.10", "1.11", "stray")
                .add("1.7.10", "1.11", "wither")
                .get());

        petTypeOptionMap.put("skeletonhorse", new CommandOptionCreator()
                .add("baby")
                .add("saddle")
                .get());

        petTypeOptionMap.put("slime", new CommandOptionCreator()
                .add("size:")
                .get());

        petTypeOptionMap.put("snowman", new CommandOptionCreator()
                .add("sheared")
                .get());

        petTypeOptionMap.put("traderllama", new CommandOptionCreator()
                .add("baby")
                .add("chest")
                .add("variant:")
                .add("decor:")
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
                //                .add("1.14", "level:")
                .add("1.14", "type:")
                .get());

        petTypeOptionMap.put("wither", new CommandOptionCreator()
                .add("baby")
                .get());

        petTypeOptionMap.put("wolf", new CommandOptionCreator()
                .add("baby")
                .add("angry")
                .add("tamed")
                .add("collar:")
                .get());

        petTypeOptionMap.put("zombie", new CommandOptionCreator()
                .add("baby")
                .add("1.10", "1.11", "husk")
                .add("1.7.10", "1.11", "villager")
                .add("1.7.10", "1.11", "profession:")
                .get());

        petTypeOptionMap.put("zombiehorse", new CommandOptionCreator()
                .add("baby")
                .add("saddle")
                .get());

        petTypeOptionMap.put("zombievillager", new CommandOptionCreator()
                .add("baby")
                .add("profession:")
                .add("1.14", "level:")
                .add("1.14", "type:")
                .get());

        for (MyPetType petType : MyPetType.values()) {
            if (petType.checkMinecraftVersion()) {
                petTypeList.add(petType.name());
            }
        }
    }

    @Override
    public boolean onCommandOption(final CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Translation.getString("Message.Command.Help.MissingParameter", sender));
            sender.sendMessage(" -> " + ChatColor.DARK_AQUA + "/petadmin create " + ChatColor.RED + "<a player name>");
            return false;
        }

        int forceOffset = 0;
        if (args[0].equalsIgnoreCase("-f")) {
            forceOffset = 1;
        }

        if (args.length < 2 + forceOffset) {
            sender.sendMessage(Translation.getString("Message.Command.Help.MissingParameter", sender));
            sender.sendMessage(" -> " + ChatColor.DARK_AQUA + "/petadmin create " + (forceOffset > 0 ? " -f " : "") + args[0] + " " + ChatColor.RED + "<a pet-type>");
            return false;
        }

        String lang = MyPetApi.getPlatformHelper().getCommandSenderLanguage(sender);

        try {
            MyPetType myPetType = MyPetType.byName(args[1 + forceOffset]);
            if (MyPetApi.getMyPetInfo().isLeashableEntityType(EntityType.valueOf(myPetType.getBukkitName()))) {
                Player owner = Bukkit.getPlayer(args[forceOffset]);
                if (owner == null || !owner.isOnline()) {
                    sender.sendMessage("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] " + Translation.getString("Message.No.PlayerOnline", lang));
                    return true;
                }

                if (WorldGroup.getGroupByWorld(owner.getWorld()).isDisabled()) {
                    sender.sendMessage("Pets are not allowed in " + ChatColor.GOLD + owner.getWorld().getName());
                    return true;
                }

                final MyPetPlayer newOwner;
                if (MyPetApi.getPlayerManager().isMyPetPlayer(owner)) {
                    newOwner = MyPetApi.getPlayerManager().getMyPetPlayer(owner);

                    if (newOwner.hasMyPet() && forceOffset == 1) {
                        MyPetApi.getMyPetManager().deactivateMyPet(newOwner, true);
                    }
                } else {
                    newOwner = MyPetApi.getPlayerManager().registerMyPetPlayer(owner);
                }

                final InactiveMyPet inactiveMyPet = new InactiveMyPet(newOwner);
                inactiveMyPet.setPetType(myPetType);
                inactiveMyPet.setPetName(Translation.getString("Name." + inactiveMyPet.getPetType().name(), inactiveMyPet.getOwner()));

                TagCompound compound = inactiveMyPet.getInfo();
                createInfo(myPetType, Arrays.copyOfRange(args, 2 + forceOffset, args.length), compound);
                updateData(inactiveMyPet, Arrays.copyOfRange(args, 2 + forceOffset, args.length));

                final WorldGroup wg = WorldGroup.getGroupByWorld(owner.getWorld().getName());

                inactiveMyPet.setWorldGroup(wg.getName());

                List<RepositoryMyPetConverterService> converters = MyPetApi.getServiceManager().getServices(RepositoryMyPetConverterService.class);
                for (RepositoryMyPetConverterService converter : converters) {
                    converter.convert(inactiveMyPet);
                }

                MyPetCreateEvent createEvent = new MyPetCreateEvent(inactiveMyPet, MyPetCreateEvent.Source.AdminCommand);
                Bukkit.getServer().getPluginManager().callEvent(createEvent);

                MyPetSaveEvent saveEvent = new MyPetSaveEvent(inactiveMyPet);
                Bukkit.getServer().getPluginManager().callEvent(saveEvent);

                MyPetApi.getRepository().addMyPet(inactiveMyPet, new RepositoryCallback<Boolean>() {
                    @Override
                    public void callback(Boolean added) {
                        if (added) {
                            if (!newOwner.hasMyPet()) {
                                inactiveMyPet.getOwner().setMyPetForWorldGroup(wg, inactiveMyPet.getUUID());
                                MyPetApi.getRepository().updateMyPetPlayer(inactiveMyPet.getOwner(), null);

                                Optional<MyPet> myPet = MyPetApi.getMyPetManager().activateMyPet(inactiveMyPet);
                                if (myPet.isPresent()) {
                                    myPet.get().createEntity();
                                    sender.sendMessage(Translation.getString("Message.Command.Success", sender));
                                } else {
                                    sender.sendMessage("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] Can't create MyPet for " + newOwner.getName() + ". Is this player online?");
                                }
                            } else {
                                sender.sendMessage(Translation.getString("Message.Command.Success", sender));
                            }
                        }
                    }
                });
            }
        } catch (MyPetTypeNotFoundException e) {
            sender.sendMessage(Translation.getString("Message.Command.PetType.Unknown", lang));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, String[] strings) {
        int forceOffset = 0;
        if (strings.length >= 2 && strings[1].equalsIgnoreCase("-f")) {
            forceOffset = 1;
        }
        if (strings.length == 2 + forceOffset) {
            return null;
        }
        if (strings.length == 3 + forceOffset) {
            return filterTabCompletionResults(petTypeList, strings[2 + forceOffset]);
        }
        if (strings.length >= 4 + forceOffset) {
            if (petTypeOptionMap.containsKey(strings[2 + forceOffset].toLowerCase())) {
                List<String> options = petTypeOptionMap.get(strings[2 + forceOffset].toLowerCase());
                if (!options.contains(commonTypeOptionList.get(0))) {
                    options.addAll(commonTypeOptionList);
                }
                return filterTabCompletionResults(options, strings[strings.length - 1]);
            } else {
                return filterTabCompletionResults(commonTypeOptionList, strings[3 + forceOffset]);
            }
        }
        return Collections.emptyList();
    }

    public static void updateData(InactiveMyPet inactiveMyPet, String[] args) {
        for (String arg : args) {
            if (arg.startsWith("skilltree:")) {
                String skilltreeName = arg.replace("skilltree:", "");
                Skilltree skilltree = MyPetApi.getSkilltreeManager().getSkilltree(skilltreeName);
                if (skilltree != null) {
                    inactiveMyPet.setSkilltree(skilltree);
                }
            } else if (arg.startsWith("name:")) {
                String name = arg.replace("name:", "");
                int index = ArrayUtils.indexOf(args, arg);
                if (args.length > index + 1) {
                    name += " " + String.join(" ", Arrays.copyOfRange(args, index + 1, args.length));
                }
                inactiveMyPet.setPetName(name);
                break;
            }
        }
    }

    public static void createInfo(MyPetType petType, String[] args, TagCompound compound) {
        for (String arg : args) {
            if (arg.equalsIgnoreCase("baby")) {
                compound.getCompoundData().put("Baby", new TagByte(true));
            } else if (arg.equalsIgnoreCase("fire")) {
                compound.getCompoundData().put("Fire", new TagByte(true));
            } else if (arg.equalsIgnoreCase("powered")) {
                compound.getCompoundData().put("Powered", new TagByte(true));
            } else if (arg.equalsIgnoreCase("saddle")) {
                compound.getCompoundData().put("Saddle", new TagByte(true));
            } else if (arg.equalsIgnoreCase("sheared")) {
                compound.getCompoundData().put("Sheared", new TagByte(true));
            } else if (arg.equalsIgnoreCase("wither")) {
                compound.getCompoundData().put("Type", new TagInt(1));
            } else if (arg.equalsIgnoreCase("stray")) {
                compound.getCompoundData().put("Type", new TagInt(2));
            } else if (arg.equalsIgnoreCase("husk")) {
                compound.getCompoundData().put("Type", new TagInt(6));
            } else if (arg.equalsIgnoreCase("tamed")) {
                compound.getCompoundData().put("Tamed", new TagByte(true));
            } else if (arg.equalsIgnoreCase("angry")) {
                compound.getCompoundData().put("Angry", new TagByte(true));
            } else if (arg.equalsIgnoreCase("villager")) {
                compound.getCompoundData().put("Villager", new TagByte(true));
            } else if (arg.equalsIgnoreCase("chest")) {
                compound.getCompoundData().put("Chest", new TagByte(true));
            } else if (arg.equalsIgnoreCase("elder")) {
                compound.getCompoundData().put("Elder", new TagByte(true));
            } else if (arg.equalsIgnoreCase("donkey")) {
                compound.getCompoundData().put("Type", new TagByte((byte) 1));
            } else if (arg.equalsIgnoreCase("mule")) {
                compound.getCompoundData().put("Type", new TagByte((byte) 2));
            } else if (arg.equalsIgnoreCase("zombie")) {
                compound.getCompoundData().put("Type", new TagByte((byte) 3));
            } else if (arg.equalsIgnoreCase("skeleton")) {
                compound.getCompoundData().put("Type", new TagByte((byte) 4));
            } else if (arg.equalsIgnoreCase("glowing")) {
                compound.getCompoundData().put("Glowing", new TagByte(true));
            } else if (arg.equalsIgnoreCase("rainbow")) {
                compound.getCompoundData().put("Rainbow", new TagByte(true));
            } else if (arg.equalsIgnoreCase("has-stung")) {
                compound.getCompoundData().put("HasStung", new TagByte(true));
            } else if (arg.equalsIgnoreCase("has-nectar")) {
                compound.getCompoundData().put("HasNectar", new TagByte(true));
            } else if (arg.startsWith("size:")) {
                String size = arg.replace("size:", "");
                if (Util.isInt(size)) {
                    compound.getCompoundData().put("Size", new TagInt(Integer.parseInt(size)));
                }
            } else if (arg.startsWith("horse:")) {
                String horseTypeString = arg.replace("horse:", "");
                if (Util.isByte(horseTypeString)) {
                    int horseType = Integer.parseInt(horseTypeString);
                    horseType = Math.min(Math.max(0, horseType), 4);
                    compound.getCompoundData().put("Type", new TagByte((byte) horseType));
                }
            } else if (arg.startsWith("variant:")) {
                String variantString = arg.replace("variant:", "");
                if (Util.isInt(variantString)) {
                    int variant = Integer.parseInt(variantString);
                    if (petType == MyPetType.Horse) {
                        variant = Math.min(Math.max(0, variant), 1030);
                        compound.getCompoundData().put("Variant", new TagInt(variant));
                    } else if (petType == MyPetType.Rabbit) {
                        if (variant != 99 && (variant > 5 || variant < 0)) {
                            variant = 0;
                        }
                        compound.getCompoundData().put("Variant", new TagByte(variant));
                    } else if (petType == MyPetType.Llama || petType == MyPetType.TraderLlama) {
                        if (variant > 3 || variant < 0) {
                            variant = 0;
                        }
                        compound.getCompoundData().put("Variant", new TagInt(variant));
                    } else if (petType == MyPetType.Parrot) {
                        compound.getCompoundData().put("Variant", new TagInt(variant));
                    } else if (petType == MyPetType.TropicalFish) {
                        compound.getCompoundData().put("Variant", new TagInt(variant));
                    }
                }
            } else if (arg.startsWith("cat:")) {
                String catTypeString = arg.replace("cat:", "");
                if (Util.isInt(catTypeString)) {
                    int catType = Integer.parseInt(catTypeString);
                    catType = Math.min(Math.max(0, catType), 3);
                    compound.getCompoundData().put("CatType", new TagInt(catType));
                }
            } else if (arg.startsWith("profession:")) {
                String professionString = arg.replace("profession:", "");
                if (Util.isInt(professionString)) {
                    int profession = Integer.parseInt(professionString);
                    profession = Math.min(Math.max(0, profession), MyPetApi.getCompatUtil().isCompatible("1.14") ? 15 : 5);
                    if (petType == MyPetType.Villager) {
                        compound.getCompoundData().put("Profession", new TagInt(profession));
                        if (!compound.getCompoundData().containsKey("VillagerLevel")) {
                            compound.getCompoundData().put("VillagerLevel", new TagInt(1));
                        }
                    } else if (petType == MyPetType.Zombie || petType == MyPetType.ZombieVillager) {
                        compound.getCompoundData().put("Villager", new TagByte(true));
                        compound.getCompoundData().put("Profession", new TagInt(profession));
                        if (!compound.getCompoundData().containsKey("TradingLevel")) {
                            compound.getCompoundData().put("TradingLevel", new TagInt(1));
                        }
                    }
                }
            } else if (arg.startsWith("color:")) {
                String colorString = arg.replace("color:", "");
                if (Util.isByte(colorString)) {
                    byte color = Byte.parseByte(colorString);
                    color = color > 15 ? 15 : color < 0 ? 0 : color;
                    compound.getCompoundData().put("Color", new TagByte(color));
                }
            } else if (arg.startsWith("collar:")) {
                String colorString = arg.replace("collar:", "");
                if (Util.isByte(colorString)) {
                    byte color = Byte.parseByte(colorString);
                    color = color > 15 ? 15 : color < 0 ? 0 : color;
                    compound.getCompoundData().put("CollarColor", new TagByte(color));
                }
            } else if (arg.startsWith("block:")) {
                String block = arg.replace("block:", "");
                ItemDatabase itemDatabase = MyPetApi.getServiceManager().getService(ItemDatabase.class).get();
                MaterialHolder materialHolder = itemDatabase.getByID(block.toLowerCase());
                if (materialHolder != null) {
                    compound.getCompoundData().put("BlockName", new TagString(materialHolder.getId()));
                }
            } else if (arg.startsWith("puff:")) {
                switch (arg) {
                    case "puff:none":
                        compound.getCompoundData().put("PuffState", new TagInt(0));
                        break;
                    case "puff:semi":
                        compound.getCompoundData().put("PuffState", new TagInt(1));
                        break;
                    case "puff:fully":
                        compound.getCompoundData().put("PuffState", new TagInt(2));
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
                        compound.getCompoundData().put(key, new TagInt(0));
                        break;
                    case "lazy":
                        compound.getCompoundData().put(key, new TagInt(1));
                        break;
                    case "worried":
                        compound.getCompoundData().put(key, new TagInt(2));
                        break;
                    case "playful":
                        compound.getCompoundData().put(key, new TagInt(3));
                        break;
                    case "brown":
                        compound.getCompoundData().put(key, new TagInt(4));
                        break;
                    case "weak":
                        compound.getCompoundData().put(key, new TagInt(5));
                        break;
                    case "aggressive":
                        compound.getCompoundData().put(key, new TagInt(6));
                        break;
                }
            } else if (arg.startsWith("type:")) {
                switch (petType) {
                    case Fox:
                        switch (arg) {
                            case "type:white":
                                compound.getCompoundData().put("FoxType", new TagInt(1));
                                break;
                            case "type:red":
                            default:
                                compound.getCompoundData().put("FoxType", new TagInt(0));
                                break;
                        }
                        break;
                    case Mooshroom:
                        switch (arg) {
                            case "type:brown":
                                compound.getCompoundData().put("CowType", new TagInt(1));
                                break;
                            case "type:red":
                            default:
                                compound.getCompoundData().put("CowType", new TagInt(0));
                                break;
                        }
                        break;
                    case Cat:
                        String catTypeString = arg.replace("type:", "");
                        if (Util.isInt(catTypeString)) {
                            int catType = Integer.parseInt(catTypeString);
                            catType = Util.clamp(catType, 0, 10);
                            compound.getCompoundData().put("CatType", new TagInt(catType));
                        }
                        break;
                    case Villager:
                    case ZombieVillager:
                        String villagerTypeString = arg.replace("type:", "");
                        if (Util.isInt(villagerTypeString)) {
                            int villagerType = Integer.parseInt(villagerTypeString);
                            villagerType = Util.clamp(villagerType, 0, 6);
                            compound.getCompoundData().put("VillagerType", new TagInt(villagerType));
                        }
                        break;
                }
            }
        }
    }
}