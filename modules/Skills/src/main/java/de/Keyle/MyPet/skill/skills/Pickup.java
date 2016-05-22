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

package de.Keyle.MyPet.skill.skills;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPet.PetState;
import de.Keyle.MyPet.api.event.MyPetInventoryActionEvent;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.skill.ActiveSkill;
import de.Keyle.MyPet.api.skill.SkillInfo;
import de.Keyle.MyPet.api.skill.SkillInstance;
import de.Keyle.MyPet.api.skill.skills.PickupInfo;
import de.Keyle.MyPet.api.util.NBTStorage;
import de.Keyle.MyPet.api.util.Scheduler;
import de.Keyle.MyPet.api.util.inventory.CustomInventory;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.keyle.knbt.TagByte;
import de.keyle.knbt.TagCompound;
import de.keyle.knbt.TagDouble;
import de.keyle.knbt.TagString;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Item;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;

public class Pickup extends PickupInfo implements SkillInstance, Scheduler, NBTStorage, ActiveSkill {
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

    public void upgrade(SkillInfo upgrade, boolean quiet) {
        if (upgrade instanceof PickupInfo) {
            if (upgrade.getProperties().getCompoundData().containsKey("range")) {
                if (!upgrade.getProperties().getCompoundData().containsKey("addset_range") || upgrade.getProperties().getAs("addset_range", TagString.class).getStringData().equals("add")) {
                    range += upgrade.getProperties().getAs("range", TagDouble.class).getDoubleData();
                } else {
                    range = upgrade.getProperties().getAs("range", TagDouble.class).getDoubleData();
                }
                if (!quiet) {
                    myPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Skill.Pickup.Upgrade", myPet.getOwner().getLanguage()), myPet.getPetName(), String.format("%1.2f", range)));
                }
            }
            if (upgrade.getProperties().getCompoundData().containsKey("exp_pickup")) {
                expPickup = upgrade.getProperties().getAs("exp_pickup", TagByte.class).getBooleanData();
            }
        }
    }

    public String getFormattedValue() {
        return Translation.getString("Name.Range", myPet.getOwner().getLanguage()) + ": " + String.format("%1.2f", range) + " " + Translation.getString("Name.Blocks", myPet.getOwner().getPlayer());
    }

    public void reset() {
        range = 0;
        pickup = false;
    }

    public boolean activate() {
        if (range > 0) {
            if (myPet.getSkills().isSkillActive(Inventory.class)) {

                if (pickup) {
                    pickup = false;
                } else {
                    MyPetInventoryActionEvent event = new MyPetInventoryActionEvent(myPet, MyPetInventoryActionEvent.Action.Pickup);
                    Bukkit.getServer().getPluginManager().callEvent(event);
                    if (!event.isCancelled()) {
                        pickup = true;
                    }
                }

                String mode = pickup ? Translation.getString("Name.Enabled", myPet.getOwner()) : Translation.getString("Name.Disabled", myPet.getOwner());
                myPet.getOwner().sendMessage(Util.formatText(Translation.getString(("Message.Skill.Pickup.StartStop"), myPet.getOwner().getPlayer()), myPet.getPetName(), mode));
                return true;
            } else {
                myPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Skill.Pickup.NoInventory", myPet.getOwner().getLanguage()), myPet.getPetName()));
                return false;
            }
        } else {
            myPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.No.Skill", myPet.getOwner().getLanguage()), myPet.getPetName(), this.getName(myPet.getOwner().getLanguage())));
            return false;
        }
    }

    public void schedule() {
        MyPetInventoryActionEvent event = new MyPetInventoryActionEvent(myPet, MyPetInventoryActionEvent.Action.Use);
        Bukkit.getServer().getPluginManager().callEvent(event);
        if (pickup && (event.isCancelled() || !Permissions.hasExtendedLegacy(myPet.getOwner().getPlayer(), "MyPet.extended.pickup"))) {
            pickup = false;
            myPet.getOwner().sendMessage(Util.formatText(Translation.getString(("Message.Skill.Pickup.StartStop"), myPet.getOwner().getPlayer()), myPet.getPetName(), Translation.getString("Name.Disabled", myPet.getOwner())));
            return;
        }
        if (pickup && myPet.getOwner().getPlayer().getGameMode() == GameMode.CREATIVE && !Configuration.Skilltree.Skill.Inventory.OPEN_IN_CREATIVE && !Permissions.has(myPet.getOwner().getPlayer(), "MyPet.admin", false)) {
            myPet.getOwner().sendMessage(Translation.getString("Message.Skill.Pickup.Creative", myPet.getOwner()));
            pickup = false;
            return;
        }
        if (range > 0 && pickup && myPet.getStatus() == PetState.Here && myPet.getSkills().isSkillActive(Inventory.class)) {
            for (Entity entity : myPet.getEntity().get().getNearbyEntities(range, range, range)) {
                if (!entity.isDead()) {
                    if (entity instanceof Item) {
                        Item itemEntity = (Item) entity;
                        ItemStack itemStack = itemEntity.getItemStack();

                        if (itemEntity.getPickupDelay() <= 0 && itemStack.getAmount() > 0) {
                            PlayerPickupItemEvent playerPickupEvent = new PlayerPickupItemEvent(myPet.getOwner().getPlayer(), itemEntity, itemStack.getAmount());
                            Bukkit.getServer().getPluginManager().callEvent(playerPickupEvent);

                            if (playerPickupEvent.isCancelled()) {
                                continue;
                            }

                            CustomInventory inv = myPet.getSkills().getSkill(Inventory.class).get().getInventory();
                            int itemAmount = inv.addItem(itemStack);
                            if (itemAmount == 0) {
                                MyPetApi.getPlatformHelper().doPickupAnimation(myPet.getEntity().get(), itemEntity);
                                if (MyPetApi.getCompatUtil().getMinecraftVersion() >= 19) {
                                    myPet.getEntity().get().getHandle().makeSound("entity.item.pickup", 0.2F, 1.0F);
                                } else {
                                    myPet.getEntity().get().getHandle().makeSound("random.pop", 0.2F, 1.0F);
                                }
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
                        MyPetApi.getPlatformHelper().doPickupAnimation(myPet.getEntity().get(), expEntity);
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
    public SkillInstance cloneSkill() {
        Pickup newSkill = new Pickup(this.isAddedByInheritance());
        newSkill.setProperties(getProperties());
        return newSkill;
    }
}