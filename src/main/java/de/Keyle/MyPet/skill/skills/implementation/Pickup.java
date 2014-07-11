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

package de.Keyle.MyPet.skill.skills.implementation;

import de.Keyle.MyPet.api.util.IScheduler;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPet.PetState;
import de.Keyle.MyPet.skill.skills.ISkillActive;
import de.Keyle.MyPet.skill.skills.ISkillStorage;
import de.Keyle.MyPet.skill.skills.implementation.inventory.CustomInventory;
import de.Keyle.MyPet.skill.skills.info.ISkillInfo;
import de.Keyle.MyPet.skill.skills.info.PickupInfo;
import de.Keyle.MyPet.util.Util;
import de.Keyle.MyPet.util.locale.Locales;
import de.Keyle.MyPet.util.support.Permissions;
import de.keyle.knbt.TagByte;
import de.keyle.knbt.TagCompound;
import de.keyle.knbt.TagDouble;
import de.keyle.knbt.TagString;
import net.minecraft.server.v1_7_R4.PacketPlayOutCollect;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;

public class Pickup extends PickupInfo implements ISkillInstance, IScheduler, ISkillStorage, ISkillActive {
    private boolean pickup = false;
    private MyPet myPet;

    public Pickup(boolean addedByInheritance) {
        super(addedByInheritance);
    }

    public void setMyPet(MyPet myPet) {
        this.myPet = myPet;
    }

    public MyPet getMyPet() {
        return myPet;
    }

    public boolean isActive() {
        return range > 0;
    }

    public void upgrade(ISkillInfo upgrade, boolean quiet) {
        if (upgrade instanceof PickupInfo) {
            if (upgrade.getProperties().getCompoundData().containsKey("range")) {
                if (!upgrade.getProperties().getCompoundData().containsKey("addset_range") || upgrade.getProperties().getAs("addset_range", TagString.class).getStringData().equals("add")) {
                    range += upgrade.getProperties().getAs("range", TagDouble.class).getDoubleData();
                } else {
                    range = upgrade.getProperties().getAs("range", TagDouble.class).getDoubleData();
                }
                if (!quiet) {
                    myPet.sendMessageToOwner(Util.formatText(Locales.getString("Message.Skill.Pickup.Upgrade", myPet.getOwner().getLanguage()), myPet.getPetName(), String.format("%1.2f", range)));
                }
            }
            if (upgrade.getProperties().getCompoundData().containsKey("exp_pickup")) {
                expPickup = upgrade.getProperties().getAs("exp_pickup", TagByte.class).getBooleanData();
            }
        }
    }

    public String getFormattedValue() {
        return Locales.getString("Name.Range", myPet.getOwner().getLanguage()) + ": " + String.format("%1.2f", range) + " " + Locales.getString("Name.Blocks", myPet.getOwner().getPlayer());
    }

    public void reset() {
        range = 0;
        pickup = false;
    }

    public boolean activate() {
        if (range > 0) {
            if (myPet.getSkills().isSkillActive(Inventory.class)) {
                pickup = !pickup;
                String mode = pickup ? Locales.getString("Name.Enabled", myPet.getOwner()) : Locales.getString("Name.Disabled", myPet.getOwner());
                myPet.sendMessageToOwner(Util.formatText(Locales.getString(("Message.Skill.Pickup.StartStop"), myPet.getOwner().getPlayer()), myPet.getPetName(), mode));
                return true;
            } else {
                myPet.sendMessageToOwner(Util.formatText(Locales.getString("Message.Skill.Pickup.NoInventory", myPet.getOwner().getLanguage()), myPet.getPetName()));
                return false;
            }
        } else {
            myPet.sendMessageToOwner(Util.formatText(Locales.getString("Message.No.Skill", myPet.getOwner().getLanguage()), myPet.getPetName(), this.getName(myPet.getOwner().getLanguage())));
            return false;
        }
    }

    public void schedule() {
        if (pickup && (!Permissions.hasExtended(myPet.getOwner().getPlayer(), "MyPet.user.extended.Pickup") || myPet.getOwner().isInExternalGames())) {
            pickup = false;
            myPet.sendMessageToOwner(Util.formatText(Locales.getString(("Message.Skill.Pickup.StartStop"), myPet.getOwner().getPlayer()), myPet.getPetName(), Locales.getString("Name.Disabled", myPet.getOwner())));
            return;
        }
        if (pickup && myPet.getOwner().getPlayer().getGameMode() == GameMode.CREATIVE && !Inventory.OPEN_IN_CREATIVEMODE && !Permissions.has(myPet.getOwner().getPlayer(), "MyPet.admin", false)) {
            myPet.sendMessageToOwner(Locales.getString("Message.Skill.Pickup.Creative", myPet.getOwner()));
            pickup = false;
            return;
        }
        if (range > 0 && pickup && myPet.getStatus() == PetState.Here && myPet.getSkills().isSkillActive(Inventory.class)) {
            for (Entity entity : myPet.getCraftPet().getNearbyEntities(range, range, range)) {
                if (!entity.isDead()) {
                    if (entity instanceof Item) {
                        Item itemEntity = (Item) entity;
                        ItemStack itemStack = itemEntity.getItemStack();

                        if (itemStack.getAmount() > 0) {
                            PlayerPickupItemEvent playerPickupEvent = new PlayerPickupItemEvent(myPet.getOwner().getPlayer(), itemEntity, itemStack.getAmount());
                            Bukkit.getServer().getPluginManager().callEvent(playerPickupEvent);

                            if (playerPickupEvent.isCancelled()) {
                                continue;
                            }

                            CustomInventory inv = myPet.getSkills().getSkill(Inventory.class).inv;
                            int itemAmount = inv.addItem(itemStack);
                            if (itemAmount == 0) {
                                for (Entity p : itemEntity.getNearbyEntities(20, 20, 20)) {
                                    if (p instanceof Player) {
                                        ((CraftPlayer) p).getHandle().playerConnection.sendPacket(new PacketPlayOutCollect(entity.getEntityId(), myPet.getCraftPet().getEntityId()));
                                    }
                                }
                                myPet.getCraftPet().getHandle().makeSound("random.pop", 0.2F, 1.0F);
                                itemStack.setAmount(0);
                                itemEntity.remove();
                            } else {
                                itemStack.setAmount(itemAmount);
                                itemEntity.setItemStack(itemStack);
                            }
                        }
                    }
                    if (expPickup && entity instanceof ExperienceOrb) {
                        ExperienceOrb expEntity = (ExperienceOrb) entity;
                        myPet.getOwner().getPlayer().giveExp(expEntity.getExperience());
                        for (Entity p : expEntity.getNearbyEntities(20, 20, 20)) {
                            if (p instanceof Player) {
                                ((CraftPlayer) p).getHandle().playerConnection.sendPacket(new PacketPlayOutCollect(entity.getEntityId(), myPet.getCraftPet().getEntityId()));
                            }
                        }
                        expEntity.setExperience(0);
                        expEntity.remove();
                    }
                }
            }
        }
    }

    public void load(TagCompound compound) {
        pickup = compound.getAs("Active", TagByte.class).getBooleanData();
    }

    public TagCompound save() {
        TagCompound nbtTagCompound = new TagCompound();
        nbtTagCompound.getCompoundData().put("Active", new TagByte(pickup));
        return nbtTagCompound;

    }

    @Override
    public ISkillInstance cloneSkill() {
        Pickup newSkill = new Pickup(this.isAddedByInheritance());
        newSkill.setProperties(getProperties());
        return newSkill;
    }
}