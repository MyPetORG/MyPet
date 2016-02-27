/*
 * This file is part of mypet
 *
 * Copyright (C) 2011-2016 Keyle
 * mypet is licensed under the GNU Lesser General Public License.
 *
 * mypet is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * mypet is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.commands;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.entity.ActiveMyPet;
import de.Keyle.MyPet.api.entity.ActiveMyPet.PetState;
import de.Keyle.MyPet.api.entity.MyPetEquipment;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.Keyle.MyPet.entity.types.*;
import de.Keyle.MyPet.skill.skills.Inventory;
import de.keyle.fanciful.FancyMessage;
import de.keyle.fanciful.ItemTooltip;
import de.keyle.knbt.TagCompound;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.*;
import org.bukkit.entity.Ocelot.Type;
import org.bukkit.entity.Skeleton.SkeletonType;
import org.bukkit.entity.Villager.Profession;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;

import java.util.ArrayList;
import java.util.List;

import static org.bukkit.ChatColor.GOLD;
import static org.bukkit.ChatColor.RESET;

public class CommandRelease implements CommandExecutor, TabCompleter {
    public boolean onCommand(final CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player petOwner = (Player) sender;
            if (MyPetApi.getMyPetList().hasActiveMyPet(petOwner)) {
                ActiveMyPet myPet = MyPetApi.getMyPetList().getMyPet(petOwner);

                if (!Permissions.has(petOwner, "MyPet.user.command.release")) {
                    return true;
                }
                if (myPet.getStatus() == PetState.Despawned) {
                    sender.sendMessage(Util.formatText(Translation.getString("Message.Call.First", petOwner), myPet.getPetName()));
                    return true;
                } else if (myPet.getStatus() == PetState.Dead) {
                    sender.sendMessage(Util.formatText(Translation.getString("Message.Spawn.Respawn.In", petOwner), myPet.getPetName(), myPet.getRespawnTime()));
                    return true;
                }

                String name = "";
                if (args.length > 0) {
                    for (String arg : args) {
                        if (!name.equals("")) {
                            name += " ";
                        }
                        name += arg;
                    }
                }
                if (ChatColor.stripColor(myPet.getPetName()).trim().equalsIgnoreCase(name.trim())) {
                    if (myPet.getSkills().isSkillActive(Inventory.class)) {
                        myPet.getSkills().getSkill(Inventory.class).getInventory().dropContentAt(myPet.getLocation());
                    }

                    if (myPet instanceof MyPetEquipment) {
                        ((MyPetEquipment) myPet).dropEquipment();
                    }


                    if (!Configuration.Misc.REMOVE_PETS_AFTER_RELEASE) {
                        LivingEntity normalEntity = (LivingEntity) myPet.getLocation().getWorld().spawnEntity(myPet.getLocation(), EntityType.valueOf(myPet.getPetType().getBukkitName()));

                        if (myPet instanceof MyChicken) {
                            if (((MyChicken) myPet).isBaby()) {
                                ((Chicken) normalEntity).setBaby();
                            } else {
                                ((Chicken) normalEntity).setAdult();
                            }
                        } else if (myPet instanceof MyCow) {
                            if (((MyCow) myPet).isBaby()) {
                                ((Cow) normalEntity).setBaby();
                            } else {
                                ((Cow) normalEntity).setAdult();
                            }
                        } else if (myPet instanceof MyCreeper) {
                            ((Creeper) normalEntity).setPowered(((MyCreeper) myPet).isPowered());
                        } else if (myPet instanceof MyEnderman) {
                            if (((MyEnderman) myPet).hasBlock()) {
                                MaterialData materialData = new MaterialData(((MyEnderman) myPet).getBlock().getType(), ((MyEnderman) myPet).getBlock().getData().getData());
                                ((Enderman) normalEntity).setCarriedMaterial(materialData);
                            }
                        } else if (myPet instanceof MyIronGolem) {
                            ((IronGolem) normalEntity).setPlayerCreated(true);
                        } else if (myPet instanceof MyMooshroom) {
                            if (((MyMooshroom) myPet).isBaby()) {
                                ((MushroomCow) normalEntity).setBaby();
                            } else {
                                ((MushroomCow) normalEntity).setAdult();
                            }
                        } else if (myPet instanceof MyMagmaCube) {
                            ((MagmaCube) normalEntity).setSize(((MyMagmaCube) myPet).getSize());
                        } else if (myPet instanceof MyOcelot) {
                            ((Ocelot) normalEntity).setCatType(Type.WILD_OCELOT);
                            ((Ocelot) normalEntity).setTamed(false);
                            if (((MyOcelot) myPet).isBaby()) {
                                ((Ocelot) normalEntity).setBaby();
                            } else {
                                ((Ocelot) normalEntity).setAdult();
                            }
                        } else if (myPet instanceof MyPig) {
                            ((Pig) normalEntity).setSaddle(((MyPig) myPet).hasSaddle());
                            if (((MyPig) myPet).isBaby()) {
                                ((Pig) normalEntity).setBaby();
                            } else {
                                ((Pig) normalEntity).setAdult();
                            }
                        } else if (myPet instanceof MySheep) {
                            ((Sheep) normalEntity).setSheared(((MySheep) myPet).isSheared());
                            ((Sheep) normalEntity).setColor(((MySheep) myPet).getColor());
                            if (((MySheep) myPet).isBaby()) {
                                ((Sheep) normalEntity).setBaby();
                            } else {
                                ((Sheep) normalEntity).setAdult();
                            }
                        } else if (myPet instanceof MyVillager) {
                            MyVillager villagerPet = (MyVillager) myPet;
                            ((Villager) normalEntity).setProfession(Profession.getProfession(villagerPet.getProfession()));
                            if (villagerPet.isBaby()) {
                                ((Villager) normalEntity).setBaby();
                            } else {
                                ((Villager) normalEntity).setAdult();
                            }
                            if (villagerPet.hasOriginalData()) {
                                TagCompound villagerTag = MyPetApi.getBukkitHelper().entityToTag(normalEntity);
                                for (String key : villagerPet.getOriginalData().getCompoundData().keySet()) {
                                    villagerTag.put(key, villagerPet.getOriginalData().get(key));
                                }
                                MyPetApi.getBukkitHelper().applyTagToEntity(villagerTag, normalEntity);
                            }
                        } else if (myPet instanceof MyWolf) {
                            ((Wolf) normalEntity).setTamed(false);
                            if (((MyWolf) myPet).isBaby()) {
                                ((Wolf) normalEntity).setBaby();
                            } else {
                                ((Wolf) normalEntity).setAdult();
                            }
                        } else if (myPet instanceof MySlime) {
                            ((Slime) normalEntity).setSize(((MySlime) myPet).getSize());
                        } else if (myPet instanceof MyZombie) {
                            ((Zombie) normalEntity).setBaby(((MyZombie) myPet).isBaby());
                        } else if (myPet instanceof MySkeleton) {
                            if (((MySkeleton) myPet).isWither()) {
                                ((Skeleton) normalEntity).setSkeletonType(SkeletonType.WITHER);
                                normalEntity.getEquipment().setItemInHand(new ItemStack(Material.STONE_SWORD));
                            } else {
                                normalEntity.getEquipment().setItemInHand(new ItemStack(Material.BOW));
                            }
                        } else if (myPet instanceof MyPigZombie) {
                            normalEntity.getEquipment().setItemInHand(new ItemStack(Material.GOLD_SWORD));
                            ((PigZombie) normalEntity).setBaby(((MyPigZombie) myPet).isBaby());
                        } else if (myPet instanceof MyHorse) {
                            Horse.Variant type = Horse.Variant.values()[((MyHorse) myPet).getHorseType()];
                            Horse.Style style = Horse.Style.values()[(((MyHorse) myPet).getVariant() >>> 8)];
                            Horse.Color color = Horse.Color.values()[(((MyHorse) myPet).getVariant() & 0xFF)];

                            ((Horse) normalEntity).setAge(((MyHorse) myPet).getAge());
                            ((Horse) normalEntity).setVariant(type);
                            ((Horse) normalEntity).setColor(color);
                            ((Horse) normalEntity).setStyle(style);
                            ((Horse) normalEntity).setCarryingChest(((MyHorse) myPet).hasChest());

                            if (((MyHorse) myPet).hasSaddle()) {
                                ((Horse) normalEntity).getInventory().setSaddle(((MyHorse) myPet).getSaddle().clone());
                            }
                            if (((MyHorse) myPet).hasArmor()) {
                                ((Horse) normalEntity).getInventory().setArmor(((MyHorse) myPet).getArmor().clone());
                            }
                            ((Horse) normalEntity).setOwner(petOwner);
                        } else if (myPet instanceof MyRabbit) {
                            if (((MyRabbit) myPet).isBaby()) {
                                ((Rabbit) normalEntity).setBaby();
                            } else {
                                ((Rabbit) normalEntity).setAdult();
                            }
                            ((Rabbit) normalEntity).setRabbitType(((MyRabbit) myPet).getVariant().getBukkitType());
                        } else if (myPet instanceof MyGuardian) {
                            ((Guardian) normalEntity).setElder(((MyGuardian) myPet).isElder());
                        }
                    }
                    myPet.removePet();
                    myPet.getOwner().setMyPetForWorldGroup(WorldGroup.getGroupByWorld(petOwner.getWorld().getName()).getName(), null);

                    sender.sendMessage(Util.formatText(Translation.getString("Message.Command.Release.Success", petOwner), myPet.getPetName()));
                    MyPetApi.getMyPetList().deactivateMyPet(myPet.getOwner(), false);
                    MyPetApi.getRepository().removeMyPet(myPet.getUUID(), null);

                    return true;
                } else {
                    FancyMessage message = new FancyMessage(Translation.getString("Message.Command.Release.Confirm", petOwner) + " ");

                    List<String> lore = new ArrayList<>();
                    lore.add(RESET + Translation.getString("Name.Hunger", petOwner) + ": " + GOLD + Math.round(myPet.getHungerValue()));
                    if (myPet.getRespawnTime() > 0) {
                        lore.add(RESET + Translation.getString("Name.Respawntime", petOwner) + ": " + GOLD + myPet.getRespawnTime() + "sec");
                    } else {
                        lore.add(RESET + Translation.getString("Name.HP", petOwner) + ": " + GOLD + String.format("%1.2f", myPet.getHealth()));
                    }
                    lore.add(RESET + Translation.getString("Name.Exp", petOwner) + ": " + GOLD + String.format("%1.2f", myPet.getExp()));
                    lore.add(RESET + Translation.getString("Name.Type", petOwner) + ": " + GOLD + myPet.getPetType().name());
                    lore.add(RESET + Translation.getString("Name.Skilltree", petOwner) + ": " + GOLD + (myPet.getSkilltree() != null ? myPet.getSkilltree().getDisplayName() : "-"));

                    message.then(myPet.getPetName())
                            .color(ChatColor.AQUA)
                            .command("/petrelease " + ChatColor.stripColor(myPet.getPetName()))
                            .itemTooltip(new ItemTooltip().setMaterial(Material.MONSTER_EGG).addLore(lore).setTitle(myPet.getPetName()));
                    MyPetApi.getBukkitHelper().sendMessageRaw((Player) sender, message.toJSONString());
                    return true;
                }
            } else {
                sender.sendMessage(Translation.getString("Message.No.HasPet", petOwner));
            }
            return true;
        }
        sender.sendMessage("You can't use this command from server console!");
        return false;
    }

    @Override
    public List<String> onTabComplete(final CommandSender commandSender, Command command, String s, String[] strings) {
        if (MyPetApi.getMyPetList().hasActiveMyPet((Player) commandSender)) {
            List<String> petnameList = new ArrayList<>();
            petnameList.add(MyPetApi.getMyPetList().getMyPet((MyPetPlayer) commandSender).getPetName());
            return petnameList;
        }
        return CommandAdmin.EMPTY_LIST;
    }
}