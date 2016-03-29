/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2016 Keyle
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
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPet.PetState;
import de.Keyle.MyPet.api.entity.StoredMyPet;
import de.Keyle.MyPet.api.player.DonateCheck;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.util.ConfigItem;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.Keyle.MyPet.skill.skills.Behavior;
import de.Keyle.MyPet.skill.skills.Damage;
import de.keyle.fanciful.FancyMessage;
import de.keyle.fanciful.ItemTooltip;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class CommandInfo implements CommandExecutor, TabCompleter {
    public enum PetInfoDisplay {
        Name(false), HP(false), Damage(false), Hunger(true), Exp(true), Level(true), Owner(false), Skilltree(true), RangedDamage(false), RespawnTime(true), Behavior(true);

        public boolean adminOnly = false;

        PetInfoDisplay(boolean adminOnly) {
            this.adminOnly = adminOnly;
        }
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            MyPetPlayer petOwner;

            if (args.length == 0) {
                if (MyPetApi.getPlayerManager().isMyPetPlayer(player)) {
                    petOwner = MyPetApi.getPlayerManager().getMyPetPlayer(player);
                } else {
                    sender.sendMessage(Translation.getString("Message.No.HasPet", player));
                    return true;
                }
            } else if (Permissions.hasLegacy(player, "MyPet.command.info.other")) {
                Player p = Bukkit.getServer().getPlayer(args[0]);
                if (p == null || !p.isOnline()) {
                    sender.sendMessage(Translation.getString("Message.No.PlayerOnline", player));
                    return true;
                }
                if (MyPetApi.getPlayerManager().isMyPetPlayer(args[0])) {
                    petOwner = MyPetApi.getPlayerManager().getMyPetPlayer(args[0]);
                } else {
                    sender.sendMessage(Util.formatText(Translation.getString("Message.No.UserHavePet", player), args[0]));
                    return true;
                }
            } else {
                sender.sendMessage(Translation.getString("Message.No.Allowed", player));
                return true;
            }

            if (petOwner.hasMyPet()) {
                boolean infoShown = false;
                MyPet myPet = petOwner.getMyPet();

                if (canSee(PetInfoDisplay.Name.adminOnly, player, myPet)) {
                    player.sendMessage(ChatColor.AQUA + myPet.getPetName() + ChatColor.RESET + ":");
                    infoShown = true;
                }
                if (!petOwner.equals(player) && canSee(!PetInfoDisplay.Owner.adminOnly, player, myPet)) {
                    player.sendMessage("   " + Translation.getString("Name.Owner", player) + ": " + myPet.getOwner().getName());
                    infoShown = true;
                }
                if (canSee(PetInfoDisplay.HP.adminOnly, player, myPet)) {
                    String msg;
                    if (myPet.getStatus() == PetState.Dead) {
                        msg = ChatColor.RED + Translation.getString("Name.Dead", player);
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
                    player.sendMessage("   " + Translation.getString("Name.HP", player) + ": " + msg);
                    infoShown = true;
                }
                if (canSee(PetInfoDisplay.RespawnTime.adminOnly, player, myPet)) {
                    if (myPet.getStatus() == PetState.Dead) {
                        player.sendMessage("   " + Translation.getString("Name.Respawntime", player) + ": " + myPet.getRespawnTime());
                        infoShown = true;
                    }
                }
                if (!myPet.isPassiv() && canSee(PetInfoDisplay.Damage.adminOnly, player, myPet)) {
                    double damage = (myPet.getSkills().isSkillActive(Damage.class) ? myPet.getSkills().getSkill(Damage.class).get().getDamage() : 0);
                    player.sendMessage("   " + Translation.getString("Name.Damage", player) + ": " + String.format("%1.2f", damage));
                    infoShown = true;
                }
                if (myPet.getRangedDamage() > 0 && CommandInfo.canSee(PetInfoDisplay.RangedDamage.adminOnly, player, myPet)) {
                    double damage = myPet.getRangedDamage();
                    player.sendMessage("   " + Translation.getString("Name.RangedDamage", player) + ": " + String.format("%1.2f", damage));
                    infoShown = true;
                }
                if (Configuration.HungerSystem.USE_HUNGER_SYSTEM && canSee(PetInfoDisplay.Hunger.adminOnly, player, myPet)) {
                    player.sendMessage("   " + Translation.getString("Name.Hunger", player) + ": " + Math.round(myPet.getHungerValue()));

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
                            m.then(WordUtils.capitalizeFully(MyPetApi.getPlatformHelper().getMaterialName(material.getItem().getTypeId()).replace("_", " ")));
                        }
                        m.color(ChatColor.GOLD);
                        ItemTooltip it = new ItemTooltip();
                        it.setMaterial(is.getType());
                        if (is.hasItemMeta()) {
                            if (is.getItemMeta().hasDisplayName()) {
                                it.setTitle(is.getItemMeta().getDisplayName());
                            }
                            if (is.getItemMeta().hasLore()) {
                                it.setLore(is.getItemMeta().getLore().toArray(new String[is.getItemMeta().getLore().size()]));
                            }
                        }
                        m.itemTooltip(it);
                        comma = true;
                    }
                    MyPetApi.getPlatformHelper().sendMessageRaw(player, m.toJSONString());

                    infoShown = true;
                }
                if (canSee(PetInfoDisplay.Behavior.adminOnly, player, myPet)) {
                    if (myPet.getSkills().hasSkill(Behavior.class)) {
                        Behavior behavior = myPet.getSkills().getSkill(Behavior.class).get();
                        player.sendMessage("   " + Translation.getString("Name.Skill.Behavior", player) + ": " + Translation.getString("Name." + behavior.getBehavior().name(), player));
                        infoShown = true;
                    }
                }
                if (canSee(PetInfoDisplay.Skilltree.adminOnly, player, myPet) && myPet.getSkilltree() != null) {
                    player.sendMessage("   " + Translation.getString("Name.Skilltree", player) + ": " + myPet.getSkilltree().getName());
                    infoShown = true;
                }
                if (canSee(PetInfoDisplay.Level.adminOnly, player, myPet)) {
                    int lvl = myPet.getExperience().getLevel();
                    player.sendMessage("   " + Translation.getString("Name.Level", player) + ": " + lvl);
                    infoShown = true;
                }
                int maxLevel = myPet.getSkilltree() != null ? myPet.getSkilltree().getMaxLevel() : Configuration.LevelSystem.Experience.LEVEL_CAP;
                if (canSee(PetInfoDisplay.Exp.adminOnly, player, myPet) && myPet.getExperience().getLevel() < maxLevel) {
                    double exp = myPet.getExperience().getCurrentExp();
                    double reqEXP = myPet.getExperience().getRequiredExp();
                    player.sendMessage("   " + Translation.getString("Name.Exp", player) + ": " + String.format("%1.2f", exp) + "/" + String.format("%1.2f", reqEXP));
                    infoShown = true;
                }
                if (myPet.getOwner().getDonationRank() != DonateCheck.DonationRank.None) {
                    infoShown = true;
                    sender.sendMessage("   " + myPet.getOwner().getDonationRank().getDisplayText());
                }
                if (!infoShown) {
                    sender.sendMessage(Translation.getString("Message.CantViewPetInfo", player));
                }
                return true;
            } else {
                if (args.length > 0) {
                    sender.sendMessage(Util.formatText(Translation.getString("Message.No.UserHavePet", player), args[0]));
                } else {
                    sender.sendMessage(Translation.getString("Message.No.HasPet", player));
                }
            }
            return true;
        }
        sender.sendMessage("You can't use this command from server console!");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        if (commandSender instanceof Player && strings.length == 1 && Permissions.has((Player) commandSender, "MyPet.command.info.other")) {
            return null;
        }
        return CommandAdmin.EMPTY_LIST;
    }

    public static boolean canSee(boolean adminOnly, Player player, StoredMyPet storedMyPet) {
        return !adminOnly || storedMyPet.getOwner().getPlayer() == player || Permissions.has(player, "MyPet.admin", false);
    }
}