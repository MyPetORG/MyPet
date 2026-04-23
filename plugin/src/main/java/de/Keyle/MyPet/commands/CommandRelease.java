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
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.commands.CommandTabCompleter;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPet.PetState;
import de.Keyle.MyPet.api.entity.MyPetEquipment;
import de.Keyle.MyPet.api.event.MyPetRemoveEvent;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.skill.skills.Backpack;
import de.Keyle.MyPet.api.util.Colorizer;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.Keyle.MyPet.api.util.service.types.EntityConverterService;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class CommandRelease implements CommandTabCompleter {

    public boolean onCommand(final CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player petOwner = (Player) sender;
            if (WorldGroup.getGroupByWorld(petOwner.getWorld()).isDisabled()) {
                petOwner.sendMessage(Translation.getString("Message.No.AllowedHere", petOwner));
                return true;
            }
            if (MyPetApi.getMyPetManager().hasActiveMyPet(petOwner)) {
                MyPet myPet = MyPetApi.getMyPetManager().getMyPet(petOwner);

                if (!Permissions.has(petOwner, "MyPet.command.release")) {
                    return true;
                }
                if (myPet.getStatus() == PetState.Despawned) {
                    sender.sendMessage(Util.formatText(Translation.getString("Message.Call.First", petOwner), myPet.getPetName()));
                    return true;
                } else if (myPet.getStatus() == PetState.Dead) {
                    sender.sendMessage(Util.formatText(Translation.getString("Message.Spawn.Respawn.In", petOwner), myPet.getPetName(), myPet.getRespawnTime()));
                    return true;
                }

                StringBuilder name = new StringBuilder();
                if (args.length > 0) {
                    for (String arg : args) {
                        if (name.length() > 0) {
                            name.append(" ");
                        }
                        name.append(arg);
                    }
                }
                if (ChatColor.stripColor(myPet.getPetName()).trim().equalsIgnoreCase(name.toString().trim())) {
                    MyPetRemoveEvent removeEvent = new MyPetRemoveEvent(myPet, MyPetRemoveEvent.Source.Release);
                    Bukkit.getServer().getPluginManager().callEvent(removeEvent);

                    if (!MyPetApi.getMyPetInfo().getRemoveAfterRelease(myPet.getPetType())) {
                        LivingEntity normalEntity = (LivingEntity) myPet.getLocation().get().getWorld().spawnEntity(myPet.getLocation().get(), EntityType.valueOf(myPet.getPetType().getBukkitName()));

                        Optional<EntityConverterService> converter = MyPetApi.getServiceManager().getService(EntityConverterService.class);
                        try {
                            converter.ifPresent(entityConverterService -> entityConverterService.convertEntity(myPet, normalEntity));
                        } catch (Exception e) {
                            normalEntity.remove();
                            return true;
                        }
                    }

                    if (myPet.getSkills().isActive(Backpack.class)) {
                        myPet.getSkills().get(Backpack.class).getInventory().dropContentAt(myPet.getLocation().get());
                    }

                    if (myPet instanceof MyPetEquipment) {
                        ((MyPetEquipment) myPet).dropEquipment();
                    }

                    myPet.removePet();
                    myPet.getOwner().setMyPetForWorldGroup(WorldGroup.getGroupByWorld(petOwner.getWorld().getName()), null);

                    sender.sendMessage(Util.formatText(Translation.getString("Message.Command.Release.Success", petOwner), myPet.getPetName()));
                    MyPetApi.getMyPetManager().deactivateMyPet(myPet.getOwner(), false);
                    MyPetApi.getRepository().removeMyPet(myPet.getUUID(), null);

                    return true;
                } else {
                    // Build hover text with pet stats
                    TextComponent hoverText = new TextComponent("");
                    TextComponent petTitle = new TextComponent(myPet.getPetName());
                    petTitle.setColor(net.md_5.bungee.api.ChatColor.WHITE);
                    petTitle.setBold(true);
                    hoverText.addExtra(petTitle);

                    hoverText.addExtra("\n");
                    TextComponent hungerLabel = new TextComponent(Translation.getString("Name.Hunger", petOwner) + ": ");
                    TextComponent hungerVal = new TextComponent(String.valueOf(Math.round(myPet.getSaturation())));
                    hungerVal.setColor(net.md_5.bungee.api.ChatColor.GOLD);
                    hungerLabel.addExtra(hungerVal);
                    hoverText.addExtra(hungerLabel);

                    if (myPet.getRespawnTime() > 0) {
                        hoverText.addExtra("\n");
                        TextComponent rtLabel = new TextComponent(Translation.getString("Name.Respawntime", petOwner) + ": ");
                        TextComponent rtVal = new TextComponent(myPet.getRespawnTime() + "sec");
                        rtVal.setColor(net.md_5.bungee.api.ChatColor.GOLD);
                        rtLabel.addExtra(rtVal);
                        hoverText.addExtra(rtLabel);
                    } else {
                        hoverText.addExtra("\n");
                        TextComponent hpLabel = new TextComponent(Translation.getString("Name.HP", petOwner) + ": ");
                        TextComponent hpVal = new TextComponent(String.format("%1.2f", myPet.getHealth()));
                        hpVal.setColor(net.md_5.bungee.api.ChatColor.GOLD);
                        hpLabel.addExtra(hpVal);
                        hoverText.addExtra(hpLabel);
                    }

                    hoverText.addExtra("\n");
                    TextComponent expLabel = new TextComponent(Translation.getString("Name.Exp", petOwner) + ": ");
                    TextComponent expVal = new TextComponent(String.format("%1.2f", myPet.getExp()));
                    expVal.setColor(net.md_5.bungee.api.ChatColor.GOLD);
                    expLabel.addExtra(expVal);
                    hoverText.addExtra(expLabel);

                    hoverText.addExtra("\n");
                    TextComponent typeLabel = new TextComponent(Translation.getString("Name.Type", petOwner) + ": ");
                    TextComponent typeVal = new TextComponent(Translation.getString("Name." + myPet.getPetType().name(), petOwner));
                    typeVal.setColor(net.md_5.bungee.api.ChatColor.GOLD);
                    typeLabel.addExtra(typeVal);
                    hoverText.addExtra(typeLabel);

                    hoverText.addExtra("\n");
                    TextComponent stLabel = new TextComponent(Translation.getString("Name.Skilltree", petOwner) + ": ");
                    TextComponent stVal = new TextComponent(myPet.getSkilltree() != null ? Colorizer.setColors(myPet.getSkilltree().getDisplayName()) : "-");
                    stVal.setColor(net.md_5.bungee.api.ChatColor.GOLD);
                    stLabel.addExtra(stVal);
                    hoverText.addExtra(stLabel);

                    HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new BaseComponent[]{hoverText});

                    TextComponent confirmMsg = new TextComponent(Translation.getString("Message.Command.Release.Confirm", petOwner) + " ");
                    TextComponent petNamePart = new TextComponent(myPet.getPetName());
                    petNamePart.setColor(net.md_5.bungee.api.ChatColor.AQUA);
                    petNamePart.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                        "/petrelease " + ChatColor.stripColor(myPet.getPetName())));
                    petNamePart.setHoverEvent(hoverEvent);
                    confirmMsg.addExtra(petNamePart);
                    ((Player) sender).spigot().sendMessage(confirmMsg);

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
    public List<String> onTabComplete(final CommandSender sender, Command command, String s, String[] strings) {
        if (sender instanceof Player) {
            if (MyPetApi.getMyPetManager().hasActiveMyPet((Player) sender)) {
                List<String> petnameList = new ArrayList<>();
                petnameList.add(Colorizer.stripColors(MyPetApi.getMyPetManager().getMyPet((Player) sender).getPetName()));
                return petnameList;
            }
        }
        return Collections.emptyList();
    }

    @Override
    public String getHelpTranslationKey() {
        return "Message.Command.Help.Release";
    }

    @Override
    public String getHelpCommand() {
        return "/petrelease";
    }

    @Override
    public boolean isVisibleTo(Player player) {
        return MyPetApi.getMyPetManager().hasActiveMyPet(player)
                && Permissions.has(player, "MyPet.command.release");
    }

    @Override
    public int getHelpOrder() {
        return 100;
    }
}
