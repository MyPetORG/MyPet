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

package de.Keyle.MyPet.skill.skills;

import com.google.common.collect.Iterables;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.compat.ParticleCompat;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.skill.UpgradeComputer;
import de.Keyle.MyPet.api.skill.skills.Behavior;
import de.Keyle.MyPet.api.util.locale.Translation;
import org.bukkit.ChatColor;

import java.util.*;

import static de.Keyle.MyPet.api.skill.skills.Behavior.BehaviorMode.*;

public class BehaviorImpl implements Behavior {

    protected MyPet myPet;
    protected Set<BehaviorMode> activeBehaviors = new HashSet<>();
    protected BehaviorMode selectedBehavior = BehaviorMode.Normal;
    protected static Random random = new Random();
    Iterator<BehaviorMode> behaviorCycler;

    UpgradeComputer<Boolean> farmBehavior = new UpgradeComputer<>(false);
    UpgradeComputer<Boolean> raidBehavior = new UpgradeComputer<>(false);
    UpgradeComputer<Boolean> duelBehavior = new UpgradeComputer<>(false);
    UpgradeComputer<Boolean> aggressiceBehavior = new UpgradeComputer<>(false);
    UpgradeComputer<Boolean> friendlyBehavior = new UpgradeComputer<>(false);

    public BehaviorImpl(MyPet myPet) {
        this.myPet = myPet;
        activeBehaviors.add(BehaviorMode.Normal);
        updateCycler();

        farmBehavior.addCallback((newValue, reason) -> setBehaviorMode(Farm, newValue));
        raidBehavior.addCallback((newValue, reason) -> setBehaviorMode(Raid, newValue));
        duelBehavior.addCallback((newValue, reason) -> setBehaviorMode(Duel, newValue));
        aggressiceBehavior.addCallback((newValue, reason) -> setBehaviorMode(Aggressive, newValue));
        friendlyBehavior.addCallback((newValue, reason) -> setBehaviorMode(Friendly, newValue));
    }

    public MyPet getMyPet() {
        return myPet;
    }

    public boolean isActive() {
        return activeBehaviors.size() > 1;
    }

    @Override
    public void reset() {
        activeBehaviors.clear();
        activeBehaviors.add(BehaviorMode.Normal);
        selectedBehavior = BehaviorMode.Normal;
        updateCycler();
    }

    protected void updateCycler() {
        List<BehaviorMode> activeBehaviors = new ArrayList<>(this.activeBehaviors);
        Collections.sort(activeBehaviors);
        behaviorCycler = Iterables.cycle(activeBehaviors).iterator();
        //noinspection StatementWithEmptyBody
        while (behaviorCycler.next() != selectedBehavior) {
        }
    }

    public void setBehavior(BehaviorMode mode) {
        selectedBehavior = mode;
        //noinspection StatementWithEmptyBody
        while (behaviorCycler.next() != selectedBehavior) {
        }
    }

    protected void setBehaviorMode(BehaviorMode mode, boolean value) {
        if (value) {
            activeBehaviors.add(mode);
        } else {
            activeBehaviors.remove(mode);
            if (mode == selectedBehavior) {
                selectedBehavior = BehaviorMode.Normal;
            }
        }
        updateCycler();
    }

    public BehaviorMode getBehavior() {
        return selectedBehavior;
    }

    public boolean isModeUsable(BehaviorMode mode) {
        return activeBehaviors.contains(mode);
    }

    public UpgradeComputer<Boolean> getFarmBehavior() {
        return farmBehavior;
    }

    public UpgradeComputer<Boolean> getRaidBehavior() {
        return raidBehavior;
    }

    public UpgradeComputer<Boolean> getDuelBehavior() {
        return duelBehavior;
    }

    public UpgradeComputer<Boolean> getAggressiveBehavior() {
        return aggressiceBehavior;
    }

    public UpgradeComputer<Boolean> getFriendlyBehavior() {
        return friendlyBehavior;
    }

    public String toPrettyString(String locale) {
        String activeModes = ChatColor.GOLD + Translation.getString("Name.Normal", locale) + ChatColor.RESET;
        if (activeBehaviors.contains(Friendly)) {
            activeModes += ", " + ChatColor.GOLD + Translation.getString("Name.Friendly", locale) + ChatColor.RESET;
        }
        if (activeBehaviors.contains(Aggressive)) {
            if (!activeModes.equalsIgnoreCase("")) {
                activeModes += ", ";
            }
            activeModes += ChatColor.GOLD + Translation.getString("Name.Aggressive", locale) + ChatColor.RESET;
        }
        if (activeBehaviors.contains(Farm)) {
            if (!activeModes.equalsIgnoreCase("")) {
                activeModes += ", ";
            }
            activeModes += ChatColor.GOLD + Translation.getString("Name.Farm", locale) + ChatColor.RESET;
        }
        if (activeBehaviors.contains(Raid)) {
            if (!activeModes.equalsIgnoreCase("")) {
                activeModes += ", ";
            }
            activeModes += ChatColor.GOLD + Translation.getString("Name.Raid", locale) + ChatColor.RESET;
        }
        if (activeBehaviors.contains(Duel)) {
            if (!activeModes.equalsIgnoreCase("")) {
                activeModes += ", ";
            }
            activeModes += ChatColor.GOLD + Translation.getString("Name.Duel", locale) + ChatColor.RESET;
        }
        return Translation.getString("Name.Modes", locale) + ": " + activeModes;
    }

    @Override
    public String[] getUpgradeMessage() {
        return new String[]{
                Util.formatText(Translation.getString("Message.Skill.Behavior.Upgrade", myPet.getOwner().getLanguage()), myPet.getPetName()),
                "  " + toPrettyString(myPet.getOwner().getLanguage())
        };
    }

    public boolean activate() {
        if (isActive()) {
            while (true) {
                selectedBehavior = behaviorCycler.next();
                if (selectedBehavior != Normal) {
                    if (Permissions.has(myPet.getOwner().getPlayer(), "MyPet.extended.behavior." + selectedBehavior.name().toLowerCase())) {
                        break;
                    }
                } else {
                    break;
                }
            }
            myPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Skill.Behavior.NewMode", myPet.getOwner()), myPet.getPetName(), Translation.getString("Name." + selectedBehavior.name(), myPet.getOwner().getPlayer())));
            return true;
        } else {
            myPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.No.Skill", myPet.getOwner()), myPet.getPetName(), this.getName(myPet.getOwner().getLanguage())));
            return false;
        }
    }

    public void schedule() {
        if (selectedBehavior == Aggressive && random.nextBoolean() && myPet.getStatus() == MyPet.PetState.Here) {
            myPet.getEntity().ifPresent(entity -> MyPetApi.getPlatformHelper()
                    .playParticleEffect(entity.getLocation().add(0, entity.getEyeHeight(), 0), ParticleCompat.VILLAGER_ANGRY.get(), 0.2F, 0.2F, 0.2F, 0.5F, 1, 20));
        }
    }

    @Override
    public String toString() {
        return "BehaviorImpl{" +
                "activeBehaviors=" + activeBehaviors +
                ", selectedBehavior=" + selectedBehavior +
                '}';
    }
}