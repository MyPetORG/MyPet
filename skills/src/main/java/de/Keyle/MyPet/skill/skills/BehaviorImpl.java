/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2025 Keyle
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
import de.keyle.knbt.*;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static de.Keyle.MyPet.api.skill.skills.Behavior.BehaviorMode.*;

/**
 * Implementation of the Behavior skill for a MyPet entity.
 * <p>
 * This component manages a set of currently usable behavior modes (activeBehaviors),
 * the currently selected mode (selectedBehavior), and a cyclic iterator (behaviorCycler)
 * to rotate through the active modes. It also reacts to upgrade callbacks which enable
 * or disable specific behavior modes at runtime.
 * <p>
 * Robustness: The implementation ensures the selected behavior is always one of the
 * currently active modes and aligns the internal iterator using a bounded algorithm
 * to avoid infinite loops. When an unavailable mode is requested, it falls back to
 * {@link de.Keyle.MyPet.api.skill.skills.Behavior.BehaviorMode#Normal}.
 */
public class BehaviorImpl implements Behavior {

    @Getter
    protected MyPet myPet;
    protected Set<BehaviorMode> activeBehaviors = new HashSet<>();
    protected BehaviorMode selectedBehavior = BehaviorMode.Normal;
    protected static Random random = new Random();
    Iterator<BehaviorMode> behaviorCycler;

    @Getter
    UpgradeComputer<Boolean> farmBehavior = new UpgradeComputer<>(false);
    @Getter
    UpgradeComputer<Boolean> raidBehavior = new UpgradeComputer<>(false);
    @Getter
    UpgradeComputer<Boolean> duelBehavior = new UpgradeComputer<>(false);
    @Getter
    UpgradeComputer<Boolean> aggressiveBehavior = new UpgradeComputer<>(false);
    @Getter
    UpgradeComputer<Boolean> friendlyBehavior = new UpgradeComputer<>(false);

    /**
     * Creates a new Behavior skill instance for the given pet.
     *
     * @param myPet the owning pet (must not be null)
     */
    public BehaviorImpl(@NotNull MyPet myPet) {
        this.myPet = myPet;
        activeBehaviors.add(BehaviorMode.Normal);
        updateCycler();

        farmBehavior.addCallback((newValue, reason) -> setBehaviorMode(Farm, newValue));
        raidBehavior.addCallback((newValue, reason) -> setBehaviorMode(Raid, newValue));
        duelBehavior.addCallback((newValue, reason) -> setBehaviorMode(Duel, newValue));
        aggressiveBehavior.addCallback((newValue, reason) -> setBehaviorMode(Aggressive, newValue));
        friendlyBehavior.addCallback((newValue, reason) -> setBehaviorMode(Friendly, newValue));
    }

    /**
     * Indicates whether this skill currently has more than the default behavior available.
     *
     * @return true if there is at least one additional behavior besides Normal; false otherwise
     */
    public boolean isActive() {
        return activeBehaviors.size() > 1;
    }

    /**
     * Resets the Behavior skill to its default state.
     * <p>
     * Active behaviors are cleared and only Normal remains, the selected behavior is set
     * to Normal, and the internal cycler is rebuilt and aligned.
     */
    @Override
    public void reset() {
        activeBehaviors.clear();
        activeBehaviors.add(BehaviorMode.Normal);
        selectedBehavior = BehaviorMode.Normal;
        updateCycler();
    }

    /**
     * Rebuilds and aligns the internal behavior cycler based on the currently active modes.
     * <p>
     * The active modes are copied and sorted for deterministic iteration order. The
     * selected behavior is validated to ensure it is included; otherwise it falls back to
     * Normal or, if needed, the first available entry. Finally, the iterator is aligned to
     * the selected behavior in a bounded fashion to avoid unbounded loops.
     */
    protected void updateCycler() {
        List<BehaviorMode> activeBehaviors = new ArrayList<>(this.activeBehaviors);
        Collections.sort(activeBehaviors);

        // Ensure selectedBehavior is valid
        if (!activeBehaviors.contains(selectedBehavior)) {
            selectedBehavior = BehaviorMode.Normal;
            if (!activeBehaviors.contains(selectedBehavior) && !activeBehaviors.isEmpty()) {
                selectedBehavior = activeBehaviors.get(0);
            }
        }

        behaviorCycler = Iterables.cycle(activeBehaviors).iterator();

        // Align iterator position to selectedBehavior boundedly
        for (int i = 0; i < activeBehaviors.size(); i++) {
            if (behaviorCycler.next() == selectedBehavior) {
                break;
            }
        }
    }

    /**
     * Selects the given behavior mode for this pet.
     * <p>
     * Note: The selected mode must be present in the active behaviors set; otherwise the
     * internal alignment could misbehave. The current implementation of this class ensures
     * safety by validating and aligning in {@link #updateCycler()}.
     *
     * @param mode the behavior mode to select (must not be null)
     */
    @Override
    public void setBehavior(@NotNull BehaviorMode mode) {
        // If the requested mode isn’t currently usable, fall back to Normal
        if (!activeBehaviors.contains(mode)) {
            mode = BehaviorMode.Normal;
        }
        selectedBehavior = mode;
        // Rebuild and safely align the cycler
        updateCycler();
    }

    /**
     * Enables or disables a specific behavior mode and updates the cycler accordingly.
     * <p>
     * If a mode is disabled while it is currently selected, the selection falls back to Normal.
     *
     * @param mode  the behavior mode to change
     * @param value true to enable (make usable); false to disable
     */
    protected void setBehaviorMode(@NotNull BehaviorMode mode, boolean value) {
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

    /**
     * Gets the currently selected behavior mode.
     *
     * @return the current behavior mode (never null)
     */
    @Override
    public @NotNull BehaviorMode getBehavior() {
        return selectedBehavior;
    }

    /**
     * Checks if the given behavior mode is currently usable (i.e., active) for this pet.
     *
     * @param mode the mode to check (must not be null)
     * @return true if the mode is contained in the active behavior set; false otherwise
     */
    public boolean isModeUsable(@NotNull BehaviorMode mode) {
        return activeBehaviors.contains(mode);
    }

    /**
     * Returns a human-readable list of all currently active behavior modes in the given locale.
     *
     * @param locale the locale key used for translations (must not be null)
     * @return a formatted String describing the active behavior modes
     */
    public @NotNull String toPrettyString(@NotNull String locale) {
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

    /**
     * Creates the localized upgrade message shown to the pet owner when the Behavior skill
     * gains or changes the availability of modes.
     *
     * @return a two-line, localized message array (never null)
     */
    @Override
    public @NotNull String[] getUpgradeMessage() {
        return new String[]{
                Util.formatText(Translation.getString("Message.Skill.Behavior.Upgrade", myPet.getOwner().getLanguage()), myPet.getPetName()),
                "  " + toPrettyString(myPet.getOwner().getLanguage())
        };
    }

    /**
     * Cycles to the next available behavior mode and informs the pet owner.
     * <p>
     * If additional modes are available, the cycler advances until it finds a mode that is either
     * Normal or a mode the player has the corresponding permission for. The selection is updated
     * and a localized chat message is sent to the owner.
     *
     * @return true if a mode change occurred or could be confirmed; false if the skill has only Normal
     */
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

    /**
     * Periodic tick hook from the Scheduler interface.
     * <p>
     * Emits a cosmetic angry villager particle above the pet’s head occasionally when the
     * current mode is Aggressive and the pet is present (Here).
     */
    @Override
    public void schedule() {
        if (selectedBehavior == Aggressive && random.nextBoolean() && myPet.getStatus() == MyPet.PetState.Here) {
            myPet.getEntity().ifPresent(entity -> MyPetApi.getPlatformHelper()
                    .playParticleEffect(entity.getLocation().add(0, entity.getEyeHeight(), 0), ParticleCompat.VILLAGER_ANGRY.get(), 0.2F, 0.2F, 0.2F, 0.5F, 1, 20));
        }
    }

    /**
     * Debug String containing the set of active behaviors and the currently selected mode.
     */
    @Override
    public String toString() {
        return "BehaviorImpl{" +
                "activeBehaviors=" + activeBehaviors +
                ", selectedBehavior=" + selectedBehavior +
                '}';
    }

    /**
     * Serializes the current state of this skill to NBT.
     * <p>
     * Currently stores only the name of the selected behavior under the key "selectedBehavior".
     *
     * @return a compound tag with this skill’s persisted data (never null)
     */
    @Override
    public @NotNull TagCompound save() {
        TagCompound nbtTagCompound = new TagCompound();
        nbtTagCompound.getCompoundData().put("selectedBehavior", new TagString(this.selectedBehavior.name()));
        return nbtTagCompound;
    }

    /**
     * Deserializes the saved state from NBT and restores the selected behavior.
     * <p>
     * Unknown or currently unavailable modes are mapped to Normal to guarantee a valid state.
     *
     * @param tagCompound the NBT data for this skill (must not be null)
     */
    @Override
    public void load(@NotNull TagCompound tagCompound) {
        if (tagCompound.containsKey("selectedBehavior")) {
            String behaviorString = tagCompound.getAs("selectedBehavior", TagString.class).getStringData();
            switch (behaviorString) {
                case "Friendly":
                    setBehavior(Friendly);
                    break;
                case "Aggressive":
                    setBehavior(Aggressive);
                    break;
                case "Raid":
                    setBehavior(Raid);
                    break;
                case "Farm":
                    setBehavior(Farm);
                    break;
                case "Duel":
                    setBehavior(Duel);
                    break;
                default:
                    setBehavior(Normal);
                    break;
            }
        }
    }
}