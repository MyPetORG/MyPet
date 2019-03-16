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

package de.Keyle.MyPet.commands;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.commands.CommandTabCompleter;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPet.PetState;
import de.Keyle.MyPet.api.entity.StoredMyPet;
import de.Keyle.MyPet.api.player.DonateCheck;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.util.Colorizer;
import de.Keyle.MyPet.api.util.ConfigItem;
import de.Keyle.MyPet.api.util.chat.FancyMessage;
import de.Keyle.MyPet.api.util.chat.parts.ItemTooltip;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.Keyle.MyPet.skill.skills.BehaviorImpl;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CommandInfo implements CommandTabCompleter {

    public enum PetInfoDisplay {
        Name(false), HP(false), Damage(false), Hunger(true), Exp(true), Level(true), Owner(false), Skilltree(true), RangedDamage(false), RespawnTime(true), Behavior(true);

        public boolean adminOnly;

        PetInfoDisplay(boolean adminOnly) {
            this.adminOnly = adminOnly;
        }
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        MyPetPlayer petOwner;

        if (args.length == 0 && sender instanceof Player) {
            Player player = (Player) sender;
            if (WorldGroup.getGroupByWorld(player.getWorld()).isDisabled()) {
                player.sendMessage(Translation.getString("Message.No.AllowedHere", player));
                return true;
            }
            if (MyPetApi.getPlayerManager().isMyPetPlayer(player)) {
                petOwner = MyPetApi.getPlayerManager().getMyPetPlayer(player);
            } else {
                sender.sendMessage(Translation.getString("Message.No.HasPet", player));
                return true;
            }
        } else if (args.length > 0 && (!(sender instanceof Player) || Permissions.has((Player) sender, "MyPet.command.info.other"))) {
            Player p = Bukkit.getServer().getPlayer(args[0]);
            if (p == null || !p.isOnline()) {
                sender.sendMessage(Translation.getString("Message.No.PlayerOnline", sender));
                return true;
            }
            if (MyPetApi.getPlayerManager().isMyPetPlayer(args[0])) {
                petOwner = MyPetApi.getPlayerManager().getMyPetPlayer(args[0]);
            } else {
                sender.sendMessage(Util.formatText(Translation.getString("Message.No.UserHavePet", sender), args[0]));
                return true;
            }
        } else {
            if (sender instanceof Player) {
                sender.sendMessage(Translation.getString("Message.No.AllowedHere", sender));
            } else {
                sender.sendMessage("You can't use this command from server console!");
            }
            return true;
        }

        if (petOwner.hasMyPet()) {
            boolean infoShown = false;
            MyPet myPet = petOwner.getMyPet();

            if (canSee(PetInfoDisplay.Name.adminOnly, sender, myPet)) {
                sender.sendMessage(ChatColor.AQUA + myPet.getPetName() + ChatColor.RESET + ":");
                infoShown = true;
            }
            if (!petOwner.equals(sender) && canSee(!PetInfoDisplay.Owner.adminOnly, sender, myPet)) {
                sender.sendMessage("   " + Translation.getString("Name.Owner", sender) + ": " + myPet.getOwner().getName());
                infoShown = true;
            }
            if (canSee(PetInfoDisplay.HP.adminOnly, sender, myPet)) {
                String msg;
                if (myPet.getStatus() == PetState.Dead) {
                    msg = ChatColor.RED + Translation.getString("Name.Dead", sender);
                } else {
                    if (myPet.getHealth() > myPet.getMaxHealth() / 3 * 2) {
                        msg = "" + ChatColor.GREEN;
                    } else if (myPet.getHealth() > myPet.getMaxHealth() / 3) {
                        msg = "" + ChatColor.YELLOW;
                    } else {
                        msg = "" + ChatColor.RED;
                    }
                    msg += String.format("%1.2f", myPet.getHealth()) + ChatColor.WHITE + "/" + String.format("%1.2f", myPet.getMaxHealth());
                }
                sender.sendMessage("   " + Translation.getString("Name.HP", sender) + ": " + msg);
                infoShown = true;
            }
            if (canSee(PetInfoDisplay.RespawnTime.adminOnly, sender, myPet)) {
                if (myPet.getStatus() == PetState.Dead) {
                    sender.sendMessage("   " + Translation.getString("Name.Respawntime", sender) + ": " + myPet.getRespawnTime());
                    infoShown = true;
                }
            }
            if (myPet.getDamage() > 0 && canSee(PetInfoDisplay.Damage.adminOnly, sender, myPet)) {
                sender.sendMessage("   " + Translation.getString("Name.Damage", sender) + ": " + String.format("%1.2f", myPet.getDamage()));
                infoShown = true;
            }
            if (myPet.getRangedDamage() > 0 && canSee(PetInfoDisplay.RangedDamage.adminOnly, sender, myPet)) {
                sender.sendMessage("   " + Translation.getString("Name.RangedDamage", sender) + ": " + String.format("%1.2f", myPet.getRangedDamage()));
                infoShown = true;
            }
            if (Configuration.HungerSystem.USE_HUNGER_SYSTEM && canSee(PetInfoDisplay.Hunger.adminOnly, sender, myPet)) {
                sender.sendMessage("   " + Translation.getString("Name.Hunger", sender) + ": " + Math.round(myPet.getSaturation()));

                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    FancyMessage m = new FancyMessage("   " + Translation.getString("Name.Food", player) + ": ");
                    boolean comma = false;
                    for (ConfigItem material : MyPetApi.getMyPetInfo().getFood(myPet.getPetType())) {
                        ItemStack is = material.getItem();
                        if (is == null || is.getType() == Material.AIR) {
                            continue;
                        }
                        if (comma) {
                            m.then(", ");
                        }
                        if (is.hasItemMeta() && is.getItemMeta().hasDisplayName()) {
                            m.then(is.getItemMeta().getDisplayName());
                        } else {
                            try {
                                m.thenTranslate(MyPetApi.getPlatformHelper().getVanillaName(is));
                            } catch (Exception e) {
                                MyPetApi.getLogger().warning("A food item caused an error. If you think this is a bug please report it to the MyPet developer.");
                                MyPetApi.getLogger().warning("" + is);
                                e.printStackTrace();
                                continue;
                            }
                        }
                        m.color(ChatColor.GOLD);
                        ItemTooltip it = new ItemTooltip();
                        it.setMaterial(is.getType());
                        if (is.hasItemMeta()) {
                            if (is.getItemMeta().hasDisplayName()) {
                                it.setTitle(is.getItemMeta().getDisplayName());
                            }
                            if (is.getItemMeta().hasLore()) {
                                it.setLore(is.getItemMeta().getLore().toArray(new String[0]));
                            }
                        }
                        m.itemTooltip(it);
                        comma = true;
                    }
                    MyPetApi.getPlatformHelper().sendMessageRaw(player, m.toJSONString());
                } else {
                    String foodString = "   " + Translation.getString("Name.Food", sender) + ": ";
                    foodString += String.join(
                            ", ",
                            MyPetApi.getMyPetInfo().getFood(myPet.getPetType())
                                    .stream()
                                    .filter(configItem -> configItem.getItem() != null && configItem.getItem().getType() != Material.AIR)
                                    .map(configItem -> configItem.getItem().getType().name())
                                    .collect(Collectors.toList())
                    );
                    sender.sendMessage(foodString);
                }
                infoShown = true;
            }
            if (canSee(PetInfoDisplay.Behavior.adminOnly, sender, myPet)) {
                if (myPet.getSkills().has(BehaviorImpl.class)) {
                    BehaviorImpl behavior = myPet.getSkills().get(BehaviorImpl.class);
                    sender.sendMessage("   " + Translation.getString("Name.Skill.Behavior", sender) + ": " + Translation.getString("Name." + behavior.getBehavior().name(), sender));
                    infoShown = true;
                }
            }
            if (canSee(PetInfoDisplay.Skilltree.adminOnly, sender, myPet) && myPet.getSkilltree() != null) {
                sender.sendMessage("   " + Translation.getString("Name.Skilltree", sender) + ": " + Colorizer.setColors(myPet.getSkilltree().getDisplayName()));
                infoShown = true;
            }
            if (canSee(PetInfoDisplay.Level.adminOnly, sender, myPet)) {
                int lvl = myPet.getExperience().getLevel();
                sender.sendMessage("   " + Translation.getString("Name.Level", sender) + ": " + lvl);
                infoShown = true;
            }
            int maxLevel = myPet.getSkilltree() != null ? myPet.getSkilltree().getMaxLevel() : Configuration.LevelSystem.Experience.LEVEL_CAP;
            if (canSee(PetInfoDisplay.Exp.adminOnly, sender, myPet) && myPet.getExperience().getLevel() < maxLevel) {
                double exp = myPet.getExperience().getCurrentExp();
                double reqEXP = myPet.getExperience().getRequiredExp();
                sender.sendMessage("   " + Translation.getString("Name.Exp", sender) + ": " + String.format("%1.2f", exp) + "/" + String.format("%1.2f", reqEXP));
                infoShown = true;
            }
            if (myPet.getOwner().getDonationRank() != DonateCheck.DonationRank.None) {
                infoShown = true;
                String donationMessage = "" + ChatColor.GOLD;
                donationMessage += myPet.getOwner().getDonationRank().getDefaultIcon();
                donationMessage += " " + Translation.getString("Name.Title." + myPet.getOwner().getDonationRank().name(), sender) + " ";
                donationMessage += myPet.getOwner().getDonationRank().getDefaultIcon();
                sender.sendMessage("   " + donationMessage);
            }
            if (!infoShown) {
                sender.sendMessage(Translation.getString("Message.CantViewPetInfo", sender));
            }
            return true;
        } else {
            if (args.length > 0) {
                sender.sendMessage(Util.formatText(Translation.getString("Message.No.UserHavePet", sender), args[0]));
            } else {
                sender.sendMessage(Translation.getString("Message.No.HasPet", sender));
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String s, String[] strings) {
        if (strings.length == 1) {
            if (sender instanceof Player) {
                if (Permissions.has((Player) sender, "MyPet.command.info.other")) {
                    return null;
                }
            } else {
                return null;
            }
        }
        return Collections.emptyList();
    }

    public static boolean canSee(boolean adminOnly, CommandSender sender, StoredMyPet storedMyPet) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            return !adminOnly || storedMyPet.getOwner().getPlayer() == player || Permissions.has(player, "MyPet.admin", false);
        } else {
            return true;
        }
    }
}