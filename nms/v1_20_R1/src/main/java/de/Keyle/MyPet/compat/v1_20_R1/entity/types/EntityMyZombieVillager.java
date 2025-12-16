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

package de.Keyle.MyPet.compat.v1_20_R1.entity.types;

import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyVillager;
import de.Keyle.MyPet.api.entity.types.MyZombieVillager;
import de.Keyle.MyPet.compat.v1_20_R1.entity.EntityMyPet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.bukkit.Sound;

@EntitySize(width = 0.6F, height = 1.9F)
public class EntityMyZombieVillager extends EntityMyPet {

    private static final EntityDataAccessor<Boolean> BABY_WATCHER = SynchedEntityData.defineId(EntityMyZombieVillager.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> TYPE_WATCHER = SynchedEntityData.defineId(EntityMyZombieVillager.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DROWN_CONVERTING = SynchedEntityData.defineId(EntityMyZombieVillager.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SHIVER_WATCHER = SynchedEntityData.defineId(EntityMyZombieVillager.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<VillagerData> PROFESSION_WATCHER = SynchedEntityData.defineId(EntityMyZombieVillager.class, EntityDataSerializers.VILLAGER_DATA);

    public EntityMyZombieVillager(Level world, MyPet myPet) {
        super(world, myPet);
    }

    /**
     * Returns the sound that is played when the MyPet dies
     */
    @Override
    protected String getMyPetDeathSound() {
        return "entity.zombie.death";
    }

    /**
     * Returns the sound that is played when the MyPet get hurt
     */
    @Override
    protected String getHurtSound() {
        return "entity.zombie.hurt";
    }

    /**
     * Returns the default sound of the MyPet
     */
    @Override
    protected String getLivingSound() {
        return "entity.zombie.ambient";
    }

    /**
     * Is called when player rightclicks this MyPet
     * return:
     * true: there was a reaction on rightclick
     * false: no reaction on rightclick
     */
    @Override
    public InteractionResult handlePlayerInteraction(Player entityhuman, InteractionHand enumhand, ItemStack itemStack) {
        if (super.handlePlayerInteraction(entityhuman, enumhand, itemStack).consumesAction()) {
            return InteractionResult.CONSUME;
        }

        if (getOwner().equals(entityhuman) && itemStack != null && itemStack.getItem() != Items.AIR) {
            if (Configuration.MyPet.ZombieVillager.GROW_UP_ITEM.compare(itemStack) && getMyPet().isBaby() && getOwner().getPlayer().isSneaking()) {
                if (!entityhuman.getAbilities().instabuild) {
                    itemStack.shrink(1);
                    if (itemStack.getCount() <= 0) {
                        entityhuman.getInventory().setItem(entityhuman.getInventory().selected, ItemStack.EMPTY);
                    }
                }
                getMyPet().setBaby(false);
                return InteractionResult.CONSUME;
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        getEntityData().define(BABY_WATCHER, false);
        getEntityData().define(TYPE_WATCHER, 0);
        getEntityData().define(DROWN_CONVERTING, false);
        getEntityData().define(SHIVER_WATCHER, false);

        getEntityData().define(PROFESSION_WATCHER, new VillagerData(VillagerType.PLAINS, VillagerProfession.NONE, 1));
    }

    @Override
    public void updateVisuals() {
        getEntityData().set(BABY_WATCHER, getMyPet().isBaby());
        String professionKey = MyVillager.Profession.values()[getMyPet().getProfession()].getKey();
        VillagerProfession profession = BuiltInRegistries.VILLAGER_PROFESSION.get(new ResourceLocation(professionKey));
        VillagerType type = BuiltInRegistries.VILLAGER_TYPE.get(new ResourceLocation(getMyPet().getType().getKey())); //TODO
        getEntityData().set(PROFESSION_WATCHER, new VillagerData(type, profession, getMyPet().getTradingLevel()));

        super.updateVisuals();
    }

    @Override
    public void playPetStepSound() {
        getBukkitEntity().getWorld().playSound(getBukkitEntity().getLocation(), Sound.ENTITY_ZOMBIE_STEP, 0.15F, 1.0F);
    }

    @Override
    public MyZombieVillager getMyPet() {
        return (MyZombieVillager) myPet;
    }
}
