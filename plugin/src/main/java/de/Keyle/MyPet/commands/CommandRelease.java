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
import de.Keyle.MyPet.api.util.locale.Translation;
import de.Keyle.MyPet.api.util.service.types.EntityConverterService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
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
        if (sender instanceof Player petOwner) {
            if (WorldGroup.getGroupByWorld(petOwner.getWorld()).isDisabled()) {
                sender.sendMessage(Translation.getComponent("Message.No.AllowedHere", petOwner));
                return true;
            }
            if (MyPetApi.getMyPetManager().hasActiveMyPet(petOwner)) {
                MyPet myPet = MyPetApi.getMyPetManager().getMyPet(petOwner);

                if (!Permissions.has(petOwner, "MyPet.command.release")) {
                    return true;
                }
                if (myPet.getStatus() == PetState.Despawned) {
                    sender.sendMessage(Translation.getFormattedComponent("Message.Call.First", petOwner, myPet.getDisplayName()));
                    return true;
                } else if (myPet.getStatus() == PetState.Dead) {
                    sender.sendMessage(Translation.getFormattedComponent("Message.Spawn.Respawn.In", petOwner, myPet.getDisplayName(), myPet.getRespawnTime()));
                    return true;
                }

                StringBuilder name = new StringBuilder();
                if (args.length > 0) {
                    for (String arg : args) {
                        if (!name.isEmpty()) {
                            name.append(" ");
                        }
                        name.append(arg);
                    }
                }
                if (Util.SANITIZED_MINIMESSAGE.stripTags(myPet.getPetName()).trim().equalsIgnoreCase(name.toString().trim())) {
                    MyPetRemoveEvent removeEvent = new MyPetRemoveEvent(myPet, MyPetRemoveEvent.Source.Release);
                    Bukkit.getServer().getPluginManager().callEvent(removeEvent);

                    boolean entityConverted = false;
                    if (!MyPetApi.getMyPetInfo().getRemoveAfterRelease(myPet.getPetType())) {
                        LivingEntity normalEntity = (LivingEntity) myPet.getLocation().get().getWorld().spawnEntity(myPet.getLocation().get(), EntityType.valueOf(myPet.getPetType().getBukkitName()));

                        Optional<EntityConverterService> converter = MyPetApi.getServiceManager().getService(EntityConverterService.class);
                        try {
                            converter.ifPresent(entityConverterService -> entityConverterService.convertEntity(myPet, normalEntity));
                            entityConverted = true;
                        } catch (Exception e) {
                            normalEntity.remove();
                            return true;
                        }
                    }

                    if (myPet.getSkills().isActive(Backpack.class)) {
                        myPet.getSkills().get(Backpack.class).getInventory().dropContentAt(myPet.getLocation().get());
                    }

                    // Only drop equipment if the entity wasn't converted (equipment already transferred to the converted entity)
                    if (myPet instanceof MyPetEquipment && !entityConverted) {
                        ((MyPetEquipment) myPet).dropEquipment();
                    }

                    myPet.removePet();
                    myPet.getOwner().setMyPetForWorldGroup(WorldGroup.getGroupByWorld(petOwner.getWorld().getName()), null);

                    sender.sendMessage(Translation.getFormattedComponent("Message.Command.Release.Success", petOwner, myPet.getDisplayName()));
                    MyPetApi.getMyPetManager().deactivateMyPet(myPet.getOwner(), false);
                    MyPetApi.getRepository().removeMyPet(myPet.getUUID(), null);

                    return true;
                } else {
                    // Build hover item with pet stats
                    TextComponent.Builder hoverBuilder = Component.text();

                    hoverBuilder.append(Translation.getComponent("Name.Hunger", petOwner))
                            .append(Component.text(": "))
                            .append(Component.text(Math.round(myPet.getSaturation())).color(NamedTextColor.GOLD));

                    if (myPet.getRespawnTime() > 0) {
                        hoverBuilder.append(Component.newline())
                                .append(Translation.getComponent("Name.Respawntime", petOwner))
                                .append(Component.text(": "))
                                .append(Component.text(myPet.getRespawnTime() + "sec").color(NamedTextColor.GOLD));
                    } else {
                        hoverBuilder.append(Component.newline())
                                .append(Translation.getComponent("Name.HP", petOwner))
                                .append(Component.text(": "))
                                .append(Component.text(String.format("%1.2f", myPet.getHealth())).color(NamedTextColor.GOLD));
                    }

                    hoverBuilder.append(Component.newline())
                            .append(Translation.getComponent("Name.Exp", petOwner))
                            .append(Component.text(": "))
                            .append(Component.text(String.format("%1.2f", myPet.getExp())).color(NamedTextColor.GOLD))
                            .append(Component.newline())
                            .append(Translation.getComponent("Name.Type", petOwner))
                            .append(Component.text(": "))
                            .append(Translation.getComponent("Name." + myPet.getPetType().name(), petOwner).color(NamedTextColor.GOLD))
                            .append(Component.newline())
                            .append(Translation.getComponent("Name.Skilltree", petOwner))
                            .append(Component.text(": "))
                            .append(Util.SANITIZED_MINIMESSAGE.deserialize(myPet.getSkilltree() != null ? myPet.getSkilltree().getDisplayName() : "-")
                                    .color(NamedTextColor.GOLD));

                    HoverEvent<Component> hoverEvent = HoverEvent.showText(hoverBuilder.build());

                    petOwner.sendMessage(
                            Translation.getComponent("Message.Command.Release.Confirm", petOwner).append(Component.text(" "))
                                    .append(
                                            myPet.getDisplayName()
                                                    .clickEvent(ClickEvent.runCommand("/petrelease " + Util.SANITIZED_MINIMESSAGE.stripTags(myPet.getPetName())))
                                                    .hoverEvent(hoverEvent)
                                    )
                    );

                    return true;
                }
            } else {
                sender.sendMessage(Translation.getComponent("Message.No.HasPet", petOwner));
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
                petnameList.add(Util.SANITIZED_MINIMESSAGE.stripTags(MyPetApi.getMyPetManager().getMyPet((Player) sender).getPetName()));
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
