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

package de.Keyle.MyPet.compat.v1_21_R4.entity.types;

import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyFrog;
import de.Keyle.MyPet.compat.v1_21_R4.entity.EntityMyAquaticPet;
import de.Keyle.MyPet.compat.v1_21_R4.util.VariantConverter;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.frog.FrogVariant;
import net.minecraft.world.entity.animal.frog.FrogVariants;
import net.minecraft.world.level.Level;
import org.bukkit.craftbukkit.v1_21_R4.CraftRegistry;

import java.util.OptionalInt;

@EntitySize(width = 0.7F, height = 1.3F)
public class EntityMyFrog extends EntityMyAquaticPet {
    private static final EntityDataAccessor<Boolean> AGE_WATCHER = SynchedEntityData.defineId(EntityMyFrog.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Holder<FrogVariant>> DATA_VARIANT_ID = SynchedEntityData.defineId(EntityMyFrog.class, EntityDataSerializers.FROG_VARIANT);
    private static final EntityDataAccessor<OptionalInt> TONGUE_TARGET_ID = SynchedEntityData.defineId(EntityMyFrog.class, EntityDataSerializers.OPTIONAL_UNSIGNED_INT);
    int jumpDelay;

    public EntityMyFrog(Level world, MyPet myPet) {
        super(world, myPet);
        this.jumpDelay = (this.random.nextInt(10, 50));
        this.setMaxUpStep(1.0F);
    }

    @Override
    public void tick() {
        super.tick();
        if (!onGround()) {
            setPose(Pose.LONG_JUMPING);
        } else {
            setPose(Pose.STANDING);
        }
    }

    @Override
    protected String getMyPetDeathSound() {
        return "entity.frog.death";
    }

    @Override
    protected String getHurtSound() {
        return "entity.frog.hurt";
    }

    @Override
    protected String getLivingSound() {
        return "entity.frog.ambient";
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(AGE_WATCHER, false);
        Registry<FrogVariant> registry = CraftRegistry.getMinecraftRegistry(Registries.FROG_VARIANT);
        builder.define(EntityMyFrog.DATA_VARIANT_ID, registry.wrapAsHolder(VariantConverter.FROG_REGISTRY.getOrThrow(FrogVariants.TEMPERATE).value()));
        builder.define(TONGUE_TARGET_ID, OptionalInt.empty());
    }

    @Override
    public void updateVisuals() {
        this.setVariant(VariantConverter.convertFrogVariant(getMyPet().getFrogVariant()));
    }

    public FrogVariant getVariant() {
        return this.entityData.get(EntityMyFrog.DATA_VARIANT_ID).value();
    }

    public void setVariant(FrogVariant frogvariant) {
        Registry<FrogVariant> registry = CraftRegistry.getMinecraftRegistry(Registries.FROG_VARIANT);
        this.entityData.set(EntityMyFrog.DATA_VARIANT_ID, registry.wrapAsHolder(frogvariant));
    }

    @Override
    public void playPetStepSound() {
        makeSound("entity.frog.step", 0.15F, 1.0F);
    }

    @Override
    public MyFrog getMyPet() {
        return (MyFrog) myPet;
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (this.onGround && jumpDelay-- <= 0) {
            getJumpControl().jump();
            jumpDelay = (this.random.nextInt(20, 500));
            if (getTarget() != null) {
                jumpDelay /= 3;
            }
            this.level().broadcastEntityEvent(this, (byte) 1);
        }
    }

    public ResourceLocation getResourceLocation() {
        return this.entityData.get(DATA_VARIANT_ID).value().assetInfo().texturePath();
    }
}
