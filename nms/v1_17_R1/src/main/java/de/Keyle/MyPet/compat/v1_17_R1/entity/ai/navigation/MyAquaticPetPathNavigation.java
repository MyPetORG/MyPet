package de.Keyle.MyPet.compat.v1_17_R1.entity.ai.navigation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.AmphibiousNodeEvaluator;
import net.minecraft.world.level.pathfinder.PathFinder;

public class MyAquaticPetPathNavigation extends WaterBoundPathNavigation {
	public MyAquaticPetPathNavigation(Mob mob, Level level) {
        super(mob, level);
    }

    @Override
    protected boolean canUpdatePath() {
        return true;
    }

    @Override
    protected PathFinder createPathFinder(int i) {
        this.nodeEvaluator = new AmphibiousNodeEvaluator(false);
        return new PathFinder(this.nodeEvaluator, i);
    }

    @Override
    public boolean isStableDestination(BlockPos blockposition) {
        return !this.level.getBlockState(blockposition.down()).isAir();
    }
}
