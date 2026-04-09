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

package de.Keyle.MyPet.compat.v1_20_R4.entity.types;

import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.util.ReflectionUtil;
import de.Keyle.MyPet.compat.v1_20_R4.entity.EntityMyFlyingPet;
import de.Keyle.MyPet.compat.v1_20_R4.entity.EntityMyPetPart;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.Arrays;

import static de.Keyle.MyPet.compat.v1_20_R4.PlatformHelper.dragonPartsField;

@EntitySize(width = 1.F, height = 1.F)
public class EntityMyEnderDragon extends EntityMyFlyingPet {

    public EntityMyPetPart[] children;
    protected boolean registered = false;

    public EntityMyEnderDragon(Level world, MyPet myPet) {
        super(world, myPet);
        indirectRiding = true;

        children = new EntityMyPetPart[]{
                new EntityMyPetPart(this, "head", 1.0F, 1.0F),
                new EntityMyPetPart(this, "head", 1.0F, 1.0F),
                new EntityMyPetPart(this, "neck", 3.0F, 3.0F),
                new EntityMyPetPart(this, "body", 5.0F, 3.0F),
                new EntityMyPetPart(this, "tail", 2.0F, 2.0F),
                new EntityMyPetPart(this, "tail", 2.0F, 2.0F),
                new EntityMyPetPart(this, "tail", 2.0F, 2.0F),
                new EntityMyPetPart(this, "wing", 4.0F, 2.0F),
                new EntityMyPetPart(this, "wing", 4.0F, 2.0F),
        };
    }

    @Override
    protected String getMyPetDeathSound() {
        return "entity.ender_dragon.death";
    }

    @Override
    protected String getHurtSound() {
        return "entity.ender_dragon.hurt";
    }

    @Override
    protected String getLivingSound() {
        return "entity.ender_dragon.ambient";
    }

    @Override
    public void setPathfinder() {
        super.setPathfinder();
        var mobGoals = org.bukkit.Bukkit.getMobGoals();
        org.bukkit.entity.Mob mob = (org.bukkit.entity.Mob) getBukkitEntity();
        mobGoals.addGoal(mob, 5, new de.Keyle.MyPet.entity.ai.attack.PetMeleeAttackGoal(getBukkitEntity(), 0.1F, 8.5, 20));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (Configuration.MyPet.EnderDragon.CAN_GLIDE) {
            if (!this.onGround() && this.getDeltaMovement().y() < 0.0D) {
                this.setDeltaMovement(getDeltaMovement().multiply(1, 0.6D, 1));
            }
        }
        if (!registered && this.valid) {
            if (this.getCommandSenderWorld() instanceof ServerLevel world) {

                //The next part used to be prettier but... whilst it is listed everywhere I looked, ServerLevel dragonParts isn't public so...
                Int2ObjectMap dragonParts = (Int2ObjectMap) ReflectionUtil.getFieldValue(dragonPartsField, world);
                Arrays.stream(this.children)
                        .forEach(entityMyPetPart -> dragonParts.put(entityMyPetPart.getId(), entityMyPetPart));
                ReflectionUtil.setFieldValue(dragonPartsField, world, dragonParts);
            }
            this.registered = true;
        }
    }

    @Override
    public void die(DamageSource damagesource) {
        super.die(damagesource);
        Arrays.stream(this.children).forEach(Entity::discard);
    }

    @Override
    public void aiStep() {
        super.aiStep();
    }
}
