/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2014 Keyle
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

package de.Keyle.MyPet.commands;

import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.entity.types.IMyPetEquipment;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPet.PetState;
import de.Keyle.MyPet.entity.types.MyPetList;
import de.Keyle.MyPet.entity.types.chicken.MyChicken;
import de.Keyle.MyPet.entity.types.cow.MyCow;
import de.Keyle.MyPet.entity.types.creeper.MyCreeper;
import de.Keyle.MyPet.entity.types.enderman.MyEnderman;
import de.Keyle.MyPet.entity.types.horse.MyHorse;
import de.Keyle.MyPet.entity.types.irongolem.MyIronGolem;
import de.Keyle.MyPet.entity.types.magmacube.MyMagmaCube;
import de.Keyle.MyPet.entity.types.mooshroom.MyMooshroom;
import de.Keyle.MyPet.entity.types.ocelot.MyOcelot;
import de.Keyle.MyPet.entity.types.pig.MyPig;
import de.Keyle.MyPet.entity.types.pigzombie.MyPigZombie;
import de.Keyle.MyPet.entity.types.sheep.MySheep;
import de.Keyle.MyPet.entity.types.skeleton.MySkeleton;
import de.Keyle.MyPet.entity.types.slime.MySlime;
import de.Keyle.MyPet.entity.types.villager.MyVillager;
import de.Keyle.MyPet.entity.types.wolf.MyWolf;
import de.Keyle.MyPet.entity.types.zombie.MyZombie;
import de.Keyle.MyPet.skill.skills.implementation.Inventory;
import de.Keyle.MyPet.skill.skills.implementation.inventory.CustomInventory;
import de.Keyle.MyPet.util.Configuration;
import de.Keyle.MyPet.util.Util;
import de.Keyle.MyPet.util.WorldGroup;
import de.Keyle.MyPet.util.locale.Locales;
import de.Keyle.MyPet.util.logger.DebugLogger;
import de.Keyle.MyPet.util.player.MyPetPlayer;
import de.Keyle.MyPet.util.support.Permissions;
import net.minecraft.server.v1_7_R4.EntityItem;
import net.minecraft.server.v1_7_R4.ItemStack;
import net.minecraft.server.v1_7_R4.Items;
import net.minecraft.server.v1_7_R4.World;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftHorse;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftPigZombie;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftSkeleton;
import org.bukkit.entity.*;
import org.bukkit.entity.Ocelot.Type;
import org.bukkit.entity.Skeleton.SkeletonType;
import org.bukkit.entity.Villager.Profession;
import org.bukkit.material.MaterialData;

import java.util.ArrayList;
import java.util.List;

public class CommandRelease implements CommandExecutor, TabCompleter {
    private static List<String> emptyList = new ArrayList<String>();

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player petOwner = (Player) sender;
            if (MyPetList.hasMyPet(petOwner)) {
                MyPet myPet = MyPetList.getMyPet(petOwner);

                if (!Permissions.has(petOwner, "MyPet.user.command.release")) {
                    return true;
                }
                if (myPet.getStatus() == PetState.Despawned) {
                    sender.sendMessage(Util.formatText(Locales.getString("Message.Call.First", petOwner), myPet.getPetName()));
                    return true;
                } else if (myPet.getStatus() == PetState.Dead) {
                    sender.sendMessage(Util.formatText(Locales.getString("Message.Spawn.Respawn.In", petOwner), myPet.getPetName(), myPet.getRespawnTime()));
                    return true;
                }
                if (args.length < 1) {
                    return false;
                }
                String name = "";
                for (String arg : args) {
                    if (!name.equals("")) {
                        name += " ";
                    }
                    name += arg;
                }
                if (ChatColor.stripColor(myPet.getPetName()).equalsIgnoreCase(name)) {
                    if (myPet.getSkills().isSkillActive(Inventory.class)) {
                        CustomInventory inv = myPet.getSkills().getSkill(Inventory.class).inv;
                        inv.dropContentAt(myPet.getLocation());
                    }

                    if (!Configuration.REMOVE_PETS_AFTER_RELEASE) {
                        LivingEntity normalEntity = (LivingEntity) myPet.getLocation().getWorld().spawnEntity(myPet.getLocation(), myPet.getPetType().getEntityType());

                        if (myPet instanceof IMyPetEquipment) {
                            World world = myPet.getCraftPet().getHandle().world;
                            Location petLocation = myPet.getLocation();
                            for (ItemStack is : ((IMyPetEquipment) myPet).getEquipment()) {
                                if (is != null) {
                                    EntityItem itemEntity = new EntityItem(world, petLocation.getX(), petLocation.getY(), petLocation.getZ(), is);
                                    itemEntity.pickupDelay = 10;
                                    world.addEntity(itemEntity);
                                }
                            }
                        }

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
                            ((Villager) normalEntity).setProfession(Profession.getProfession(((MyVillager) myPet).getProfession()));
                            if (((MyVillager) myPet).isBaby()) {
                                ((Villager) normalEntity).setBaby();
                            } else {
                                ((Villager) normalEntity).setAdult();
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
                                ((CraftSkeleton) normalEntity).getHandle().setEquipment(0, new ItemStack(Items.STONE_SWORD));
                            } else {
                                ((CraftSkeleton) normalEntity).getHandle().setEquipment(0, new ItemStack(Items.BOW));
                            }
                        } else if (myPet instanceof MyPigZombie) {
                            ((CraftPigZombie) normalEntity).getHandle().setEquipment(0, new ItemStack(Items.GOLD_SWORD));
                            ((PigZombie) normalEntity).setBaby(((MyPigZombie) myPet).isBaby());
                        } else if (myPet instanceof MyHorse) {
                            ((Horse) normalEntity).setAge(((MyHorse) myPet).getAge());
                            ((CraftHorse) normalEntity).getHandle().setVariant(((MyHorse) myPet).getVariant());
                            ((CraftHorse) normalEntity).getHandle().setType(((MyHorse) myPet).getHorseType());
                            ((Horse) normalEntity).setCarryingChest(((MyHorse) myPet).hasChest());

                            if (((MyHorse) myPet).hasSaddle()) {
                                ((Horse) normalEntity).getInventory().setSaddle(((MyHorse) myPet).getSaddle().clone());
                            }
                            if (((MyHorse) myPet).hasArmor()) {
                                ((Horse) normalEntity).getInventory().setArmor(((MyHorse) myPet).getArmor().clone());
                            }
                        }
                    }
                    myPet.removePet();
                    myPet.getOwner().setMyPetForWorldGroup(WorldGroup.getGroupByWorld(petOwner.getWorld().getName()).getName(), null);

                    sender.sendMessage(Util.formatText(Locales.getString("Message.Command.Release.Success", petOwner), myPet.getPetName()));
                    MyPetList.removeInactiveMyPet(MyPetList.setMyPetInactive(myPet.getOwner()));
                    DebugLogger.info(sender.getName() + " released pet.");
                    if (Configuration.STORE_PETS_ON_PET_RELEASE) {
                        MyPetPlugin.getPlugin().saveData(false);
                    }
                    return true;
                } else {
                    sender.sendMessage(Util.formatText(Locales.getString("Message.Command.Release.ShowPetName", petOwner), myPet.getPetName()));
                    return true;
                }
            } else {
                sender.sendMessage(Locales.getString("Message.No.HasPet", petOwner));
            }
            return true;
        }
        sender.sendMessage("You can't use this command from server console!");
        return false;
    }

    @Override
    public List<String> onTabComplete(final CommandSender commandSender, Command command, String s, String[] strings) {
        if (MyPetList.hasMyPet((Player) commandSender)) {
            List<String> petnameList = new ArrayList<String>();
            petnameList.add(MyPetPlayer.getOrCreateMyPetPlayer((Player) commandSender).getMyPet().getPetName());
            return petnameList;
        }
        return emptyList;
    }
}