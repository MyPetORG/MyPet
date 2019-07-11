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

package de.Keyle.MyPet.compat.v1_7_R4.entity.ai.movement;

import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.ai.AIGoal;
import de.Keyle.MyPet.api.util.Compat;
import de.Keyle.MyPet.compat.v1_7_R4.entity.types.EntityMySheep;
import net.minecraft.server.v1_7_R4.Block;
import net.minecraft.server.v1_7_R4.Blocks;
import net.minecraft.server.v1_7_R4.MathHelper;
import net.minecraft.server.v1_7_R4.World;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_7_R4.event.CraftEventFactory;

@Compat("v1_7_R4")
public class EatGrass implements AIGoal {
    private EntityMySheep entityMySheep;
    private World world;
    int eatTicks = 0;

    public EatGrass(EntityMySheep entityMySheep) {
        this.entityMySheep = entityMySheep;
        this.world = entityMySheep.world;
    }

    @Override
    public boolean shouldStart() {
        if (!Configuration.MyPet.Sheep.CAN_REGROW_WOOL) {
            return false;
        } else if (!this.entityMySheep.getMyPet().isSheared()) {
            return false;
        } else if (entityMySheep.getRandom().nextInt(1000) != 0) {
            return false;
        } else if (this.entityMySheep.getTarget() != null && !this.entityMySheep.getTarget().isDead()) {
            return false;
        }
        int blockLocX = MathHelper.floor(this.entityMySheep.locX);
        int blockLocY = MathHelper.floor(this.entityMySheep.locY);
        int blockLocZ = MathHelper.floor(this.entityMySheep.locZ);

        return this.world.getType(blockLocX, blockLocY, blockLocZ) == Blocks.LONG_GRASS || this.world.getType(blockLocX, blockLocY - 1, blockLocZ) == Blocks.GRASS;
    }

    @Override
    public boolean shouldFinish() {
        return this.eatTicks <= 0;
    }

    @Override
    public void start() {
        this.eatTicks = 30;
        this.world.broadcastEntityEffect(this.entityMySheep, (byte) 10);
        this.entityMySheep.getPetNavigation().stop();
    }

    @Override
    public void finish() {
        this.eatTicks = 0;
    }

    @Override
    public void tick() {
        if (--this.eatTicks == 0) {
            int blockLocX = MathHelper.floor(this.entityMySheep.locX);
            int blockLocY = MathHelper.floor(this.entityMySheep.locY);
            int blockLocZ = MathHelper.floor(this.entityMySheep.locZ);

            if (this.world.getType(blockLocX, blockLocY, blockLocZ) == Blocks.LONG_GRASS) {
                if (!CraftEventFactory.callEntityChangeBlockEvent(this.entityMySheep.getBukkitEntity(), this.entityMySheep.world.getWorld().getBlockAt(blockLocX, blockLocY, blockLocZ), Material.AIR).isCancelled()) {
                    this.world.triggerEffect(2001, blockLocX, blockLocY, blockLocZ, Block.getId(Blocks.LONG_GRASS) + 4096);
                    this.world.setAir(blockLocX, blockLocY, blockLocZ);
                    this.entityMySheep.getMyPet().setSheared(false);
                }
            } else if (this.world.getType(blockLocX, blockLocY - 1, blockLocZ) == Blocks.GRASS) {
                if (!CraftEventFactory.callEntityChangeBlockEvent(this.entityMySheep.getBukkitEntity(), this.entityMySheep.world.getWorld().getBlockAt(blockLocX, blockLocY - 1, blockLocZ), Material.DIRT).isCancelled()) {
                    this.world.triggerEffect(2001, blockLocX, blockLocY - 1, blockLocZ, Block.getId(Blocks.GRASS));
                    this.world.setTypeAndData(blockLocX, blockLocY - 1, blockLocZ, Blocks.DIRT, 0, 2);
                    this.entityMySheep.getMyPet().setSheared(false);
                }
            }
        }
    }
}