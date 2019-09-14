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
import de.Keyle.MyPet.api.util.chat.FancyMessage;
import de.Keyle.MyPet.api.util.chat.parts.ItemTooltip;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.Keyle.MyPet.api.util.service.types.EntityConverterService;
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

import static org.bukkit.ChatColor.GOLD;
import static org.bukkit.ChatColor.RESET;

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
                    FancyMessage message = new FancyMessage(Translation.getString("Message.Command.Release.Confirm", petOwner) + " ");

                    List<String> lore = new ArrayList<>();
                    lore.add(RESET + Translation.getString("Name.Hunger", petOwner) + ": " + GOLD + Math.round(myPet.getSaturation()));
                    if (myPet.getRespawnTime() > 0) {
                        lore.add(RESET + Translation.getString("Name.Respawntime", petOwner) + ": " + GOLD + myPet.getRespawnTime() + "sec");
                    } else {
                        lore.add(RESET + Translation.getString("Name.HP", petOwner) + ": " + GOLD + String.format("%1.2f", myPet.getHealth()));
                    }
                    lore.add(RESET + Translation.getString("Name.Exp", petOwner) + ": " + GOLD + String.format("%1.2f", myPet.getExp()));
                    lore.add(RESET + Translation.getString("Name.Type", petOwner) + ": " + GOLD + Translation.getString("Name." + myPet.getPetType().name(), petOwner));
                    lore.add(RESET + Translation.getString("Name.Skilltree", petOwner) + ": " + GOLD + (myPet.getSkilltree() != null ? Colorizer.setColors(myPet.getSkilltree().getDisplayName()) : "-"));

                    message.then(myPet.getPetName())
                            .color(ChatColor.AQUA)
                            .command("/petrelease " + ChatColor.stripColor(myPet.getPetName()))
                            .itemTooltip(new ItemTooltip().addLore(lore).setTitle(myPet.getPetName()));
                    MyPetApi.getPlatformHelper().sendMessageRaw((Player) sender, message.toJSONString());
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
}
