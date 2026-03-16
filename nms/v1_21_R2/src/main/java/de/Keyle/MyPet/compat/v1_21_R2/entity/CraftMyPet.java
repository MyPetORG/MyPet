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

package de.Keyle.MyPet.compat.v1_21_R2.entity;

import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPetBukkitEntity;
import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.entity.ai.target.TargetPriority;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.entity.CraftMob;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.SpawnCategory;
import org.jetbrains.annotations.NotNull;

public class CraftMyPet extends CraftMob implements MyPetBukkitEntity {

    protected MyPetPlayer petOwner;
    protected EntityMyPet petEntity;

    public CraftMyPet(CraftServer server, EntityMyPet entityMyPet) {
        super(server, entityMyPet);
        petEntity = entityMyPet;
    }

    @Override
    public boolean canMove() {
        return petEntity.canMove();
    }

    @Override
    public boolean isSitting() {
        return getHandle().isSitting();
    }

    @Override
    public void setSitting(boolean sitting) {
        getHandle().setSitting(sitting);
    }

    @Override
    public boolean isInvisible() {
        return getHandle().isInvisible();
    }

    @Override
    public void setInvisible(boolean invisible) {
    }

    @Override
    public EntityMyPet getHandle() {
        return petEntity;
    }

    @Override
    public MyPet getMyPet() {
        return petEntity.getMyPet();
    }

    @Override
    public MyPetPlayer getOwner() {
        if (petOwner == null) {
            petOwner = getMyPet().getOwner();
        }
        return petOwner;
    }

    @Override
    public void removeEntity() {
        getHandle().discard();
    }

    @Override
    public MyPetType getPetType() {
        return getMyPet().getPetType();
    }

    @NotNull
    @Override
    public SpawnCategory getSpawnCategory() {
        return SpawnCategory.MISC;
    }

    @Override
    public boolean isInWater() {
        return getHandle().isInWater();
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
    public void damage(double v, @NotNull DamageSource damageSource) {

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
    public int getArrowCooldown() {
        return 0;
    }

    @Override
    public void setArrowCooldown(int i) {

    }

    @Override
    public int getArrowsInBody() {
        return 0;
    }

    @Override
    public void setArrowsInBody(int i) {

    }

    @Override
    public void setRiptiding(boolean b) {
    }

    @Override
    public void attack(@NotNull Entity entity) {
        this.petEntity.attack(((CraftEntity) entity).getHandle());
    }

    @Override
    public void swingMainHand() {
    }

    @Override
    public void swingOffHand() {
    }

    @Override
    public void setTarget(LivingEntity target) {
        setTarget(target, TargetPriority.Bukkit);
    }

    @Override
    public boolean isAware() {
        return true;
    }

    @Override
    public void setAware(boolean b) {
    }

    @Override
    public void forgetTarget() {
        getHandle().forgetTarget();
    }

    @Override
    public void setTarget(LivingEntity target, TargetPriority priority) {
        getHandle().setMyPetTarget(target, priority);
    }

    @Override
    public String toString() {
        return "CraftMyPet{MyPet=" + getHandle().isMyPet() + ",owner=" + getOwner() + ",type=" + getPetType() + "}";
    }
}
