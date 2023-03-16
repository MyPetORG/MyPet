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

package de.Keyle.MyPet.compat.v1_19_R3.entity.ai.movement;

import java.util.function.Predicate;

import org.bukkit.GameRule;
import org.bukkit.craftbukkit.v1_19_R3.event.CraftEventFactory;

import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.ai.AIGoal;
import de.Keyle.MyPet.api.util.Compat;
import de.Keyle.MyPet.compat.v1_19_R3.entity.types.EntityMySheep;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.predicate.BlockStatePredicate;

@Compat("v1_19_R3")
public class EatGrass implements AIGoal {

	private final EntityMySheep entityMySheep;
	private final Level world;
	int eatTicks = 0;

	private static final Predicate<BlockState> GRASS = BlockStatePredicate.forBlock(Blocks.GRASS);

	public EatGrass(EntityMySheep entityMySheep) {
		this.entityMySheep = entityMySheep;
		this.world = entityMySheep.level;
	}

	@Override
	public boolean shouldStart() {
		if (!Configuration.MyPet.Sheep.CAN_REGROW_WOOL) {
			return false;
		} else if (!this.entityMySheep.getMyPet().isSheared()) {
			return false;
		} else if (entityMySheep.getRandom().nextInt(1000) != 0) {
			return false;
		} else if (this.entityMySheep.getTarget() != null && !this.entityMySheep.getMyPetTarget().isDead()) {
			return false;
		}
		int blockLocX = Mth.floor(this.entityMySheep.getX());
		int blockLocY = Mth.floor(this.entityMySheep.getY());
		int blockLocZ = Mth.floor(this.entityMySheep.getZ());

		BlockPos blockposition = new BlockPos(blockLocX, blockLocY, blockLocZ);

		return GRASS.test(this.world.getBlockState(blockposition)) || this.world.getBlockState(blockposition.below()).getBlock() == Blocks.GRASS;
	}

	@Override
	public boolean shouldFinish() {
		return this.eatTicks <= 0;
	}

	@Override
	public void start() {
		this.eatTicks = 30;
		this.world.broadcastEntityEvent(this.entityMySheep, (byte) 10);
		this.entityMySheep.getPetNavigation().stop();
	}

	@Override
	public void finish() {
		this.eatTicks = 0;
	}

	@Override
	public void tick() {
		if (--this.eatTicks == 0) {
			int blockLocX = Mth.floor(this.entityMySheep.getX());
			int blockLocY = Mth.floor(this.entityMySheep.getY());
			int blockLocZ = Mth.floor(this.entityMySheep.getZ());

			BlockPos blockAt = new BlockPos(blockLocX, blockLocY, blockLocZ);
			if (GRASS.test(this.world.getBlockState(blockAt))) {
				if (!CraftEventFactory.callEntityChangeBlockEvent(
						this.entityMySheep,
						blockAt,
						Blocks.AIR.defaultBlockState(),
						!this.world.getWorld().getGameRuleValue(GameRule.MOB_GRIEFING)
				).isCancelled()) {
					this.world.destroyBlock(blockAt, false);
				}
				entityMySheep.getMyPet().setSheared(false);
			} else {
				BlockPos blockUnder = blockAt.below();
				if (this.world.getBlockState(blockUnder).getBlock() == Blocks.GRASS) {
					if (!CraftEventFactory.callEntityChangeBlockEvent(
							this.entityMySheep,
							blockAt,
							Blocks.AIR.defaultBlockState(),
							!this.world.getWorld().getGameRuleValue(GameRule.MOB_GRIEFING)
					).isCancelled()) {
						this.world.levelEvent(2001, blockUnder, Block.getId(Blocks.GRASS_BLOCK.defaultBlockState()));
						this.world.setBlock(blockUnder, Blocks.DIRT.defaultBlockState(), 2);
					}
					entityMySheep.getMyPet().setSheared(false);
				}
			}
		}
	}
}
