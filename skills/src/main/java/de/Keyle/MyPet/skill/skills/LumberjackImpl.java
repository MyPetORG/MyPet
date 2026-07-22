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

package de.Keyle.MyPet.skill.skills;

import de.Keyle.MyPet.api.MyPetGlobal;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.skill.SkillStateCodecs;
import de.Keyle.MyPet.api.skill.SkillUpgrades;
import de.Keyle.MyPet.api.skill.ToggleableSkill;
import de.Keyle.MyPet.api.skill.UpgradeComputer;
import de.Keyle.MyPet.api.skill.UpgradeParsers;
import de.Keyle.MyPet.api.skill.UpgradeSchema;
import de.Keyle.MyPet.api.skill.skills.Lumberjack;
import de.Keyle.MyPet.skill.upgrades.LumberjackUpgrade;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class LumberjackImpl extends AbstractGatheringSkill implements Lumberjack {

    public static final SkillUpgrades UPGRADES = SkillUpgrades.of(Lumberjack.class,
            UpgradeSchema.builder()
                    .number("range").label("Range").suffix(" blocks").cumulative()
                    .integer("interval").label("Interval (s)").cumulative()
                    .integer("logs").label("Logs per cycle").cumulative()
                    .bool("toolless").label("No tool required")
                    .build(), json -> new LumberjackUpgrade()
            .setRangeModifier(UpgradeParsers.parseNumber(UpgradeParsers.get(json, "range")))
            .setIntervalModifier(UpgradeParsers.parseInteger(UpgradeParsers.get(json, "interval")))
            .setLogsModifier(UpgradeParsers.parseInteger(UpgradeParsers.get(json, "logs")))
            .setToollessModifier(UpgradeParsers.parseBoolean(UpgradeParsers.get(json, "toolless"))));

    public static final SkillStateCodecs STATE_CODEC = SkillStateCodecs.of(Lumberjack.class,
            ToggleableSkill.ToggleState.class, ToggleableSkill.TOGGLE_CODEC);

    private static final int MAX_TRUNK_HEIGHT = 32;
    private static final BlockFace[] HORIZONTAL = {
            BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST
    };

    /** Logs felled per work cycle — base 1 (the base skill takes one log at a time); the tree-feller upgrade raises it. */
    private final UpgradeComputer<Integer> logs = new UpgradeComputer<>(1);
    /** The tree the pet is currently felling — it stays on this one until it's down or the pet is pulled away. */
    private Location reservedTree;

    public LumberjackImpl(Pet pet) {
        super(pet);
    }

    @Override
    protected boolean isEnabledGlobally() {
        return MyPetGlobal.Skilltree.Skill.Lumberjack.ACTIVE.get();
    }

    @Override
    public UpgradeComputer<Integer> getLogs() {
        return logs;
    }

    @Override
    public void reset() {
        super.reset();
        logs.removeAllUpgrades();
    }

    @Override
    protected void onClaimCleared() {
        reservedTree = null;
        PetWorkFocus.clearReservation(pet, this);
    }

    @Override
    protected Location findWorkTarget(Mob mob) {
        Player owner = pet.getOwner().getPlayer();
        // Stay on the tree we're already felling while it still has logs and the pet is near it.
        if (reservedTree != null) {
            Block base = reservedTreeBase(mob);
            if (base == null) {
                // Tree's down (or the pet was pulled away following the owner) — drop the claim so
                // other work skills get a turn.
                reservedTree = null;
                PetWorkFocus.clearReservation(pet, this);
                return null;
            }
            if (resolveBreakTool(base, AXE_LADDER) == null) {
                warnNoTool(owner, base, requiredTool(base, AXE_LADDER));
                return null; // keep the claim; it just needs an axe restocked
            }
            toolResolved();
            return base.getLocation().toCenterLocation();
        }
        // Otherwise start (and reserve) a new tree.
        Block base = findTrunkBase(mob);
        if (base == null) {
            return null;
        }
        if (resolveBreakTool(base, AXE_LADDER) == null) {
            // No usable axe — tell the owner which log and which axe it needs, and don't commit.
            warnNoTool(owner, base, requiredTool(base, AXE_LADDER));
            return null;
        }
        toolResolved();
        reservedTree = base.getLocation();
        PetWorkFocus.reserve(pet, this);
        return base.getLocation().toCenterLocation();
    }

    @Override
    protected void performWork(Mob mob, Player owner) {
        Block base = reservedTree != null ? reservedTreeBase(mob) : findTrunkBase(mob);
        if (base == null) {
            return;
        }
        WorkTool tool = resolveBreakTool(base, AXE_LADDER);
        if (tool == null) {
            return; // axe used up between committing and arriving
        }
        int toBreak = Math.max(1, logs.getValue()); // base 1; the tree-feller upgrade fells more per cycle
        if (!tool.isVirtual()) {
            holdTool(mob, tool.item()); // show a real axe; toolless works bare-handed
        }
        mob.lookAt(base.getLocation().toCenterLocation());
        mob.swingMainHand();
        Block cursor = base;
        for (int i = 0; i < toBreak && Tag.LOGS.isTagged(cursor.getType()); i++) {
            // Each felled log wears the axe one point (and the last one may snap it).
            if (!animatedBreak(mob, owner, cursor, tool, () -> wearTool(tool, owner))) {
                return;
            }
            cursor = cursor.getRelative(BlockFace.UP);
        }
    }

    /** The current lowest log of the reserved tree, or null if it's felled or the pet has left its area. */
    private Block reservedTreeBase(Mob mob) {
        if (reservedTree.getWorld() != mob.getWorld()) {
            return null;
        }
        double reach = searchRadius() + 3.0;
        if (reservedTree.distanceSquared(mob.getLocation()) > reach * reach) {
            return null; // the pet was pulled out of range (owner left the area) — abandon it
        }
        Block cursor = reservedTree.getBlock();
        for (int i = 0; i < MAX_TRUNK_HEIGHT; i++) {
            if (Tag.LOGS.isTagged(cursor.getType())) {
                return cursor;
            }
            cursor = cursor.getRelative(BlockFace.UP);
        }
        return null;
    }

    /** Nearest tree's trunk base (the lowest log of a leafy column) within range, or null. */
    private Block findTrunkBase(Mob mob) {
        Block log = findNearbyBlock(mob, this::isTreeLog);
        if (log == null) {
            return null;
        }
        Block base = log;
        while (Tag.LOGS.isTagged(base.getRelative(BlockFace.DOWN).getType())) {
            base = base.getRelative(BlockFace.DOWN);
        }
        return base;
    }

    /** True for a log that is part of a tree: its trunk column ends in (or touches) leaves. */
    private boolean isTreeLog(Block block) {
        if (!Tag.LOGS.isTagged(block.getType())) {
            return false;
        }
        Block cursor = block;
        for (int i = 0; i < MAX_TRUNK_HEIGHT && Tag.LOGS.isTagged(cursor.getType()); i++) {
            cursor = cursor.getRelative(BlockFace.UP);
        }
        if (Tag.LEAVES.isTagged(cursor.getType())) {
            return true;
        }
        Block topLog = cursor.getRelative(BlockFace.DOWN);
        for (BlockFace face : HORIZONTAL) {
            if (Tag.LEAVES.isTagged(topLog.getRelative(face).getType())) {
                return true;
            }
        }
        return false;
    }
}
