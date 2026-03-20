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

package de.Keyle.MyPet.skill.skills;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPet.PetState;
import de.Keyle.MyPet.api.event.MyPetInventoryActionEvent;
import de.Keyle.MyPet.api.event.MyPetPickupItemEvent;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.skill.UpgradeComputer;
import de.Keyle.MyPet.api.skill.skills.Pickup;
import de.Keyle.MyPet.api.util.inventory.CustomInventory;
import de.Keyle.MyPet.api.util.locale.Translation;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Item;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;

public class PickupImpl implements Pickup {

    protected UpgradeComputer<Number> range = new UpgradeComputer<>(0);
    protected UpgradeComputer<Boolean> expPickup = new UpgradeComputer<>(false);
    private boolean pickup = false;
    private MyPet myPet;

    public PickupImpl(MyPet myPet) {
        this.myPet = myPet;
    }

    public MyPet getMyPet() {
        return myPet;
    }

    public boolean isActive() {
        return range.getValue().doubleValue() > 0;
    }

    @Override
    public void reset() {
        range.removeAllUpgrades();
        expPickup.removeAllUpgrades();
    }

    public Component toPrettyComponent(String locale) {
        return Component.text()
                .append(Translation.getComponent("Name.Range", locale))
                .append(Component.text(": "))
                .append(Component.text(String.format("%1.2f", range.getValue().doubleValue())).color(NamedTextColor.GOLD))
                .append(Component.space())
                .append(Translation.getComponent("Name.Blocks", locale))
                .build();
    }

    @Override
    public Component[] getUpgradeMessage() {
        return new Component[]{
                Translation.getFormattedComponent("Message.Skill.Pickup.Upgrade", myPet.getOwner().getLanguage(), myPet.getDisplayName(), String.format("%1.2f", getRange().getValue().doubleValue()))
        };
    }

    public boolean activate() {
        if (isActive()) {
            if (myPet.getSkills().isActive(BackpackImpl.class)) {
                if (pickup) {
                    pickup = false;
                } else {
                    MyPetInventoryActionEvent event = new MyPetInventoryActionEvent(myPet, MyPetInventoryActionEvent.Action.Pickup);
                    Bukkit.getServer().getPluginManager().callEvent(event);
                    if (!event.isCancelled()) {
                        pickup = true;
                    }
                }

                Component mode = pickup ? Translation.getComponent("Name.Enabled", myPet.getOwner()) : Translation.getComponent("Name.Disabled", myPet.getOwner());
                myPet.getOwner().sendMessage(Translation.getFormattedComponent("Message.Skill.Pickup.StartStop", myPet.getOwner(), myPet.getDisplayName(), mode));
                return true;
            } else {
                myPet.getOwner().sendMessage(Translation.getFormattedComponent("Message.Skill.Pickup.NoInventory", myPet.getOwner(), myPet.getDisplayName()));
                return false;
            }
        } else {
            myPet.getOwner().sendMessage(Translation.getFormattedComponent("Message.No.Skill", myPet.getOwner(), myPet.getDisplayName(), this.getName(myPet.getOwner().getLanguage())));
            return false;
        }
    }

    public void schedule() {
        MyPetInventoryActionEvent event = new MyPetInventoryActionEvent(myPet, MyPetInventoryActionEvent.Action.Use);
        Bukkit.getServer().getPluginManager().callEvent(event);
        if (pickup && (event.isCancelled() || !Permissions.hasExtended(myPet.getOwner().getPlayer(), "MyPet.extended.pickup"))) {
            pickup = false;
            myPet.getOwner().sendMessage(Translation.getFormattedComponent("Message.Skill.Pickup.StartStop", myPet.getOwner().getPlayer(), myPet.getDisplayName(), Translation.getComponent("Name.Disabled", myPet.getOwner())));
            return;
        }
        if (pickup && myPet.getOwner().getPlayer().getGameMode() == GameMode.CREATIVE && !Configuration.Skilltree.Skill.Backpack.OPEN_IN_CREATIVE && !Permissions.has(myPet.getOwner().getPlayer(), "MyPet.admin", false)) {
            myPet.getOwner().sendMessage(Translation.getComponent("Message.Skill.Pickup.Creative", myPet.getOwner()));
            pickup = false;
            return;
        }
        if (isActive() && pickup && myPet.getStatus() == PetState.Here && myPet.getSkills().isActive(BackpackImpl.class)) {
            myPet.getEntity().ifPresent(petEntity -> {
                double range = this.range.getValue().doubleValue();
                for (Entity entity : petEntity.getNearbyEntities(range, range, range)) {
                    if (!entity.isDead()) {
                        if (entity instanceof Item itemEntity) {
                            ItemStack itemStack = itemEntity.getItemStack();

                            if (itemEntity.getPickupDelay() <= 0 && itemStack.getAmount() > 0) {
                                MyPetPickupItemEvent petPickupEvent = new MyPetPickupItemEvent(myPet, itemEntity);
                                Bukkit.getServer().getPluginManager().callEvent(petPickupEvent);

                                if (petPickupEvent.isCancelled()) {
                                    continue;
                                }

                                EntityPickupItemEvent entityPickupEvent = new EntityPickupItemEvent(myPet.getOwner().getPlayer(), itemEntity, 0);
                                Bukkit.getServer().getPluginManager().callEvent(entityPickupEvent);

                                if (entityPickupEvent.isCancelled()) {
                                    continue;
                                }

                                itemStack = itemEntity.getItemStack();

                                CustomInventory inv = myPet.getSkills().get(BackpackImpl.class).getInventory();
                                int itemAmount = inv.addItem(itemStack);
                                if (itemAmount == 0) {
                                    MyPetApi.getPlatformHelper().doPickupAnimation(petEntity, itemEntity);
                                    petEntity.getWorld().playSound(petEntity.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.2F, 1.0F);
                                    itemStack.setAmount(0);
                                    itemEntity.remove();
                                } else {
                                    itemStack.setAmount(itemAmount);
                                    itemEntity.setItemStack(itemStack);
                                }
                            }
                        } else if (expPickup.getValue() && entity instanceof ExperienceOrb expEntity) {
                            myPet.getOwner().getPlayer().giveExp(expEntity.getExperience());
                            MyPetApi.getPlatformHelper().doPickupAnimation(petEntity, expEntity);
                            expEntity.setExperience(0);
                            expEntity.remove();
                        }
                    }
                }
            });
        }
    }

    public void load(CompoundBinaryTag compound) {
        pickup = compound.getBoolean("Active");
    }

    public CompoundBinaryTag save() {
        return CompoundBinaryTag.builder()
                .putBoolean("Active", pickup)
                .build();
    }

    public UpgradeComputer<Number> getRange() {
        return range;
    }

    public UpgradeComputer<Boolean> getExpPickup() {
        return expPickup;
    }

    @Override
    public String toString() {
        return "PickupImpl{" +
                "range=" + range.getValue().doubleValue() +
                ", expPickup=" + expPickup +
                ", pickup=" + pickup +
                '}';
    }
}