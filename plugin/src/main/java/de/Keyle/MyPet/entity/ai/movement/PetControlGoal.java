/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2026 Keyle
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

package de.Keyle.MyPet.entity.ai.movement;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import de.Keyle.MyPet.api.entity.Pet;
import org.bukkit.entity.Mob;
import de.Keyle.MyPet.api.entity.ai.navigation.AbstractNavigation;
import de.Keyle.MyPet.api.util.Scheduler;
import de.Keyle.MyPet.util.Timer;
import de.Keyle.MyPet.entity.ai.PetGoalKey;
import de.Keyle.MyPet.entity.ai.PetGoalWorlds;
import de.Keyle.MyPet.skill.skills.ControlImpl;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

public class PetControlGoal implements Goal<Mob>, Scheduler {

    private final Pet pet;
    private final Mob mob;
    private final float speedModifier;
    private final AbstractNavigation nav;
    public Location moveTo = null;
    private int timeToMove = 0;
    private boolean stopControl = false;
    private boolean isRunning = false;

    public PetControlGoal(Pet pet, Mob mob, float speedModifier) {
        this.pet = pet;
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.nav = pet.getPetNavigation();
    }

    @Override
    public boolean shouldActivate() {
        if (!pet.canMove()) {
            return false;
        }
        ControlImpl controlSkill = pet.getSkills().get(ControlImpl.class);
        if (controlSkill == null || !controlSkill.getActive().getValue()) {
            return false;
        }
        return controlSkill.getLocation(false) != null;
    }

    @Override
    public boolean shouldStayActive() {
        if (!pet.canMove()) {
            return false;
        }
        ControlImpl controlSkill = pet.getSkills().get(ControlImpl.class);
        // Mirror the null guard in shouldActivate(): the Control skill can be
        // removed from the pet's skilltree between activation and the next
        // stay-active check (reload, skill reset, etc.), so do not assume
        // non-null here.
        if (controlSkill == null || !controlSkill.getActive().getValue()) {
            return false;
        }
        if (moveTo == null) {
            return false;
        }
        // If the user set a new control location mid-movement, end the
        // current goal activation so the goal selector re-enters via
        // stop() → shouldActivate() → start() with the new target. Calling
        // start() directly from here would race with the selector and risk
        // a destructive read on an already-consumed skill location.
        Location newLocation = controlSkill.getLocation(false);
        if (newLocation != null && !newLocation.equals(moveTo)) {
            return false;
        }
        // start() only checks the world once, when moveTo is set; the pet itself can
        // leave that world afterwards, and measuring against moveTo would then throw.
        if (moveTo == null || PetGoalWorlds.isCrossWorld(mob, moveTo)) {
            return false;
        }
        if (pet.getLocation().get().distance(moveTo) < 1) {
            return false;
        }
        if (timeToMove <= 0) {
            return false;
        }
        return !this.stopControl;
    }

    @Override
    public void start() {
        ControlImpl controlSkill = pet.getSkills().get(ControlImpl.class);
        moveTo = controlSkill.getLocation();
        // Bail before touching navigation state if the target is in another
        // world — avoids leaking a "Control" speed modifier that stop() would
        // otherwise need to clean up on a separate tick.
        if (moveTo.getWorld() != pet.getLocation().get().getWorld()) {
            stopControl = true;
            moveTo = null;
            return;
        }
        nav.getParameters().addSpeedModifier("Control", speedModifier);
        timeToMove = (int) pet.getLocation().get().distance(moveTo) / 3;
        timeToMove = Math.max(timeToMove, 3);
        if (!isRunning) {
            Timer.addTask(this);
            isRunning = true;
        }
        if (!nav.navigateTo(moveTo)) {
            // navigation rejected the path — undo the speed modifier we just
            // added so it doesn't linger until stop() runs on a later tick.
            nav.getParameters().removeSpeedModifier("Control");
            moveTo = null;
        }
    }

    @Override
    public void stop() {
        nav.getParameters().removeSpeedModifier("Control");
        nav.stop();
        moveTo = null;
        stopControl = false;
        Timer.removeTask(this);
        isRunning = false;
    }

    public void stopControl() {
        this.stopControl = true;
    }

    @Override
    public void schedule() {
        // Intentionally minimal: the Paper goal selector is the sole
        // authority on start()/stop() for this goal. schedule() is called
        // by the Pet Timer independently of the goal selector, and
        // calling start() from here races with the selector's own
        // activation pass — two start() calls in one tick would do a
        // second destructive read of ControlImpl.getLocation() (returning
        // null the second time) and duplicate the "Control" speed
        // modifier. New control clicks are now picked up via
        // shouldStayActive(), which ends the current activation and lets
        // the selector re-enter with the fresh target.
        timeToMove--;
    }

    @Override
    public @NotNull GoalKey<Mob> getKey() {
        return PetGoalKey.CONTROL;
    }

    @Override
    public @NotNull EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.MOVE);
    }
}
