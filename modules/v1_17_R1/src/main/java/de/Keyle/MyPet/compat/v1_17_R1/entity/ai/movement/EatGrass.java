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

package de.Keyle.MyPet.compat.v1_17_R1.entity.ai.movement;

import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.ai.AIGoal;
import de.Keyle.MyPet.api.util.Compat;
import de.Keyle.MyPet.compat.v1_17_R1.entity.types.EntityMySheep;
import net.minecraft.core.BlockPosition;
import net.minecraft.util.MathHelper;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.predicate.BlockStatePredicate;
import org.bukkit.GameRule;
import org.bukkit.craftbukkit.v1_17_R1.event.CraftEventFactory;

import java.util.function.Predicate;

@Compat("v1_17_R1")
public class EatGrass implements AIGoal {

	private final EntityMySheep entityMySheep;
	private final World world;
	int eatTicks = 0;

	private static final Predicate<IBlockData> GRASS = BlockStatePredicate.a(Blocks.aX);

	public EatGrass(EntityMySheep entityMySheep) {
		this.entityMySheep = entityMySheep;
		this.world = entityMySheep.t;
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
		int blockLocX = MathHelper.floor(this.entityMySheep.locX());
		int blockLocY = MathHelper.floor(this.entityMySheep.locY());
		int blockLocZ = MathHelper.floor(this.entityMySheep.locZ());

		BlockPosition blockposition = new BlockPosition(blockLocX, blockLocY, blockLocZ);

		return GRASS.test(this.world.getType(blockposition)) || this.world.getType(blockposition.down()).getBlock() == Blocks.aX;
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
			int blockLocX = MathHelper.floor(this.entityMySheep.locX());
			int blockLocY = MathHelper.floor(this.entityMySheep.locY());
			int blockLocZ = MathHelper.floor(this.entityMySheep.locZ());

			BlockPosition blockAt = new BlockPosition(blockLocX, blockLocY, blockLocZ);
			if (GRASS.test(this.world.getType(blockAt))) {
				if (!CraftEventFactory.callEntityChangeBlockEvent(
						this.entityMySheep,
						blockAt,
						Blocks.a.getBlockData(),
						!this.world.getWorld().getGameRuleValue(GameRule.MOB_GRIEFING)
				).isCancelled()) {
					this.world.b(blockAt, false);
				}
				entityMySheep.getMyPet().setSheared(false);
			} else {
				BlockPosition blockUnder = blockAt.down();
				if (this.world.getType(blockUnder).getBlock() == Blocks.aX) {
					if (!CraftEventFactory.callEntityChangeBlockEvent(
							this.entityMySheep,
							blockAt,
							Blocks.a.getBlockData(),
							!this.world.getWorld().getGameRuleValue(GameRule.MOB_GRIEFING)
					).isCancelled()) {
						this.world.triggerEffect(2001, blockUnder, Block.getCombinedId(Blocks.i.getBlockData()));
						this.world.setTypeAndData(blockUnder, Blocks.j.getBlockData(), 2);
					}
					entityMySheep.getMyPet().setSheared(false);
				}
			}
		}
	}
}
