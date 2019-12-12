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

package de.Keyle.MyPet.compat.v1_15_R1.entity.ai.movement;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPetMinecraftEntity;
import de.Keyle.MyPet.api.entity.ai.AIGoal;
import de.Keyle.MyPet.api.entity.ai.navigation.AbstractNavigation;
import de.Keyle.MyPet.api.util.Compat;
import de.Keyle.MyPet.api.util.Scheduler;
import de.Keyle.MyPet.api.util.Timer;
import de.Keyle.MyPet.skill.skills.ControlImpl;
import org.bukkit.Location;

@Compat("v1_15_R1")
public class Control implements AIGoal, Scheduler {

    private MyPet myPet;
    private float speedModifier;
    public Location moveTo = null;
    private int timeToMove = 0;
    private AbstractNavigation nav;
    private boolean stopControl = false;
    private boolean isRunning = false;

    public Control(MyPetMinecraftEntity entity, float speedModifier) {
        this.myPet = entity.getMyPet();
        this.speedModifier = speedModifier;
        nav = entity.getPetNavigation();
    }

    @Override
    public boolean shouldStart() {
        if (!this.myPet.getEntity().get().canMove()) {
            return false;
        }
        ControlImpl controlSkill = myPet.getSkills().get(ControlImpl.class);
        if (controlSkill == null || !controlSkill.getActive().getValue()) {
            return false;
        }
        return controlSkill.getLocation(false) != null;
    }

    @Override
    public boolean shouldFinish() {
        if (!this.myPet.getEntity().get().canMove()) {
            return true;
        }
        ControlImpl controlSkill = myPet.getSkills().get(ControlImpl.class);
        if (!controlSkill.getActive().getValue()) {
            return true;
        }
        if (moveTo == null) {
            return true;
        }
        if (MyPetApi.getPlatformHelper().distance(myPet.getLocation().get(), moveTo) < 1) {
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
        ControlImpl controlSkill = myPet.getSkills().get(ControlImpl.class);
        moveTo = controlSkill.getLocation();
        if (moveTo.getWorld() != myPet.getLocation().get().getWorld()) {
            stopControl = true;
            moveTo = null;
            return;
        }
        timeToMove = (int) MyPetApi.getPlatformHelper().distance(myPet.getLocation().get(), moveTo) / 3;
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
        ControlImpl controlSkill = myPet.getSkills().get(ControlImpl.class);
        if (controlSkill.getLocation(false) != null && moveTo != controlSkill.getLocation(false)) {
            start();
        }
        timeToMove--;
    }
}