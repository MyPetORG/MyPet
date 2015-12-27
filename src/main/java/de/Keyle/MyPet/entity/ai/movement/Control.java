/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2016 Keyle
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

import de.Keyle.MyPet.api.util.IScheduler;
import de.Keyle.MyPet.entity.ai.AIGoal;
import de.Keyle.MyPet.entity.ai.navigation.AbstractNavigation;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.util.Timer;
import org.bukkit.Location;

public class Control extends AIGoal implements IScheduler {
    private MyPet myPet;
    private float speedModifier;
    public Location moveTo = null;
    private int timeToMove = 0;
    private AbstractNavigation nav;
    private boolean stopControl = false;
    private de.Keyle.MyPet.skill.skills.implementation.Control controlSkill;
    private boolean isRunning = false;

    public Control(MyPet myPet, float speedModifier) {
        this.myPet = myPet;
        this.speedModifier = speedModifier;
        nav = this.myPet.getCraftPet().getHandle().petNavigation;
        controlSkill = myPet.getSkills().getSkill(de.Keyle.MyPet.skill.skills.implementation.Control.class);
    }

    @Override
    public boolean shouldStart() {
        if (!this.myPet.getCraftPet().canMove()) {
            return false;
        } else if (controlSkill == null || !controlSkill.isActive()) {
            return false;
        }
        return controlSkill.getLocation(false) != null;
    }

    @Override
    public boolean shouldFinish() {
        if (!this.myPet.getCraftPet().canMove()) {
            return true;
        }
        if (!controlSkill.isActive()) {
            return true;
        }
        if (moveTo == null) {
            return true;
        }
        if (myPet.getLocation().distance(moveTo) < 1) {
            return true;
        }
        if (timeToMove <= 0) {
            return true;
        }
        if (this.stopControl) {
            return true;
        }
        return false;
    }

    @Override
    public void start() {
        nav.getParameters().addSpeedModifier("Control", speedModifier);
        moveTo = controlSkill.getLocation();
        if (moveTo.getWorld() != myPet.getLocation().getWorld()) {
            stopControl = true;
            moveTo = null;
            return;
        }
        timeToMove = (int) myPet.getLocation().distance(moveTo) / 3;
        timeToMove = timeToMove < 3 ? 3 : timeToMove;
        if (!isRunning) {
            Timer.addTask(this);
            isRunning = true;
        }
        if (!nav.navigateTo(moveTo)) {
            moveTo = null;
        }
    }

    @Override
    public void finish() {
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
        if (controlSkill.getLocation(false) != null && moveTo != controlSkill.getLocation(false)) {
            start();
        }
        timeToMove--;
    }
}