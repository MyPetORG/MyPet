/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2020 Keyle
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

package de.Keyle.MyPet.compat.v1_15_R1.entity;

import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPetBukkitEntity;
import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.entity.ai.target.TargetPriority;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.util.Compat;
import org.bukkit.craftbukkit.v1_15_R1.CraftServer;
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftMob;
import org.bukkit.craftbukkit.v1_15_R1.inventory.CraftEntityEquipment;
import org.bukkit.craftbukkit.v1_15_R1.inventory.CraftItemStack;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

@Compat("v1_15_R1")
public class CraftMyPet extends CraftMob implements MyPetBukkitEntity {

    protected MyPetPlayer petOwner;
    protected EntityMyPet petEntity;
    protected CraftEntityEquipment fakeEquipment;

    public CraftMyPet(CraftServer server, EntityMyPet entityMyPet) {
        super(server, entityMyPet);
        petEntity = entityMyPet;
        fakeEquipment = new FakeEquipment(this);
    }

    public boolean canMove() {
        return petEntity.canMove();
    }

    @Override
    public void setSitting(boolean sitting) {
        getHandle().setSitting(sitting);
    }

    @Override
    public boolean isSitting() {
        return getHandle().isSitting();
    }

    @Override
    public EntityMyPet getHandle() {
        return petEntity;
    }

    public MyPet getMyPet() {
        return petEntity.getMyPet();
    }

    public MyPetPlayer getOwner() {
        if (petOwner == null) {
            petOwner = getMyPet().getOwner();
        }
        return petOwner;
    }

    @Override
    public void removeEntity() {
        getHandle().die();
    }

    public MyPetType getPetType() {
        return getMyPet().getPetType();
    }

    @Override
    public EntityType getType() {
        return EntityType.UNKNOWN;
    }

    @Override
    public void remove() {
        // do nothing to prevent other plugins from removing the MyPet
        // user removeEntity() to remove the MyPet
    }

    @Override
    public boolean isPersistent() {
        return false;
    }

    @Override
    public void setPersistent(boolean b) {
    }

    @Override
    public void setHealth(double health) {
        if (health < 0) {
            health = 0;
        }
        if (health > getMaxHealth()) {
            health = getMaxHealth();
        }
        super.setHealth(health);
    }

    @Override
    public EntityEquipment getEquipment() {
        return fakeEquipment;
    }

    public void attack(@NotNull Entity entity) {
        this.petEntity.attack(((CraftEntity) entity).getHandle());
    }

    public void swingMainHand() {
    }

    public void swingOffHand() {
    }

    public void setTarget(LivingEntity target) {
        setTarget(target, TargetPriority.Bukkit);
    }

    public void setAware(boolean b) {
    }

    public boolean isAware() {
        return true;
    }

    @Override
    public void forgetTarget() {
        getHandle().forgetTarget();
    }

    public void setTarget(LivingEntity target, TargetPriority priority) {
        getHandle().setTarget(target, priority);
    }

    @Override
    public String toString() {
        return "CraftMyPet{MyPet=" + getHandle().isMyPet() + ",owner=" + getOwner() + ",type=" + getPetType() + "}";
    }

    private class FakeEquipment extends CraftEntityEquipment {

        public FakeEquipment(CraftLivingEntity entity) {
            super(entity);
        }

        @NotNull
        @Override
        public ItemStack getItemInMainHand() {
            return CraftItemStack.asBukkitCopy(net.minecraft.server.v1_15_R1.ItemStack.a);
        }

        @NotNull
        @Override
        public ItemStack getItemInOffHand() {
            return CraftItemStack.asBukkitCopy(net.minecraft.server.v1_15_R1.ItemStack.a);
        }

        @NotNull
        @Override
        public ItemStack getItemInHand() {
            return CraftItemStack.asBukkitCopy(net.minecraft.server.v1_15_R1.ItemStack.a);
        }

        @Override
        public ItemStack getHelmet() {
            return CraftItemStack.asBukkitCopy(net.minecraft.server.v1_15_R1.ItemStack.a);
        }

        @Override
        public ItemStack getChestplate() {
            return CraftItemStack.asBukkitCopy(net.minecraft.server.v1_15_R1.ItemStack.a);
        }

        @Override
        public ItemStack getLeggings() {
            return CraftItemStack.asBukkitCopy(net.minecraft.server.v1_15_R1.ItemStack.a);
        }

        @Override
        public ItemStack getBoots() {
            return CraftItemStack.asBukkitCopy(net.minecraft.server.v1_15_R1.ItemStack.a);
        }

        @NotNull
        @Override
        public ItemStack[] getArmorContents() {
            ItemStack empty = CraftItemStack.asBukkitCopy(net.minecraft.server.v1_15_R1.ItemStack.a);
            return new ItemStack[]{empty, empty, empty, empty};
        }
    }
}