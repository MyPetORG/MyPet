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

import com.destroystokyo.paper.MaterialTags;
import de.Keyle.MyPet.api.MyPetGlobal;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.skill.SkillStateCodecs;
import de.Keyle.MyPet.api.skill.SkillUpgrades;
import de.Keyle.MyPet.api.skill.ToggleableSkill;
import de.Keyle.MyPet.api.skill.UpgradeParsers;
import de.Keyle.MyPet.api.skill.UpgradeSchema;
import de.Keyle.MyPet.api.skill.skills.Mining;
import de.Keyle.MyPet.skill.upgrades.MiningUpgrade;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class MiningImpl extends AbstractGatheringSkill implements Mining {

    public static final SkillUpgrades UPGRADES = SkillUpgrades.of(Mining.class,
            UpgradeSchema.builder()
                    .number("range").label("Range").suffix(" blocks").cumulative()
                    .integer("interval").label("Interval (s)").cumulative()
                    .bool("toolless").label("No tool required")
                    .build(), json -> new MiningUpgrade()
            .setRangeModifier(UpgradeParsers.parseNumber(UpgradeParsers.get(json, "range")))
            .setIntervalModifier(UpgradeParsers.parseInteger(UpgradeParsers.get(json, "interval")))
            .setToollessModifier(UpgradeParsers.parseBoolean(UpgradeParsers.get(json, "toolless"))));

    public static final SkillStateCodecs STATE_CODEC = SkillStateCodecs.of(Mining.class,
            ToggleableSkill.ToggleState.class, ToggleableSkill.TOGGLE_CODEC);

    private static final BlockFace[] FACES = {
            BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST
    };
    /** Hard cap on how many connected ore blocks count as one vein (keeps the flood-fill bounded). */
    private static final int MAX_VEIN_SIZE = 64;

    /** The ore vein the pet is currently clearing — it stays on this until it's mined out or the pet is pulled away. */
    private Set<Location> reservedVein;

    public MiningImpl(Pet pet) {
        super(pet);
    }

    @Override
    protected boolean isEnabledGlobally() {
        return MyPetGlobal.Skilltree.Skill.Mining.ACTIVE.get();
    }

    @Override
    protected void onClaimCleared() {
        reservedVein = null;
        PetWorkFocus.clearReservation(pet, this);
    }

    @Override
    protected Location findWorkTarget(Mob mob) {
        Player owner = pet.getOwner().getPlayer();
        // Stay on the vein we're already mining while ore remains and the pet is near it.
        if (reservedVein != null) {
            Block ore = nextVeinBlock(mob);
            if (ore == null) {
                // Vein's mined out (or the pet was pulled away following the owner) — drop the claim.
                reservedVein = null;
                PetWorkFocus.clearReservation(pet, this);
                return null;
            }
            if (resolveBreakTool(ore, PICKAXE_LADDER) == null) {
                warnNoTool(owner, ore, requiredTool(ore, PICKAXE_LADDER));
                return null; // keep the claim; it just needs a pickaxe restocked
            }
            toolResolved();
            return ore.getLocation().toCenterLocation();
        }
        // Start (and reserve) a new vein from the nearest exposed ore.
        Block ore = findNearbyBlock(mob, this::isExposedOre);
        if (ore == null) {
            return null;
        }
        if (resolveBreakTool(ore, PICKAXE_LADDER) == null) {
            // No usable pickaxe — tell the owner which ore and which pickaxe it needs, and don't commit.
            warnNoTool(owner, ore, requiredTool(ore, PICKAXE_LADDER));
            return null;
        }
        toolResolved();
        reservedVein = computeVein(ore);
        PetWorkFocus.reserve(pet, this);
        return ore.getLocation().toCenterLocation();
    }

    @Override
    protected void performWork(Mob mob, Player owner) {
        Block ore = reservedVein != null ? nextVeinBlock(mob) : findNearbyBlock(mob, this::isExposedOre);
        if (ore == null) {
            return; // vanished while the pet walked over
        }
        WorkTool tool = resolveBreakTool(ore, PICKAXE_LADDER);
        if (tool == null) {
            return; // pickaxe used up between committing and arriving
        }
        if (!tool.isVirtual()) {
            holdTool(mob, tool.item()); // show a real pickaxe; toolless works bare-handed
        }
        mob.lookAt(ore.getLocation().toCenterLocation());
        mob.swingMainHand();
        animatedBreak(mob, owner, ore, tool, () -> wearTool(tool, owner));
    }

    /** Flood-fills the connected cluster of same-type ore starting at {@code start} (capped in size). */
    private Set<Location> computeVein(Block start) {
        Material type = start.getType();
        Set<Location> vein = new HashSet<>();
        Deque<Block> queue = new ArrayDeque<>();
        vein.add(start.getLocation());
        queue.add(start);
        while (!queue.isEmpty() && vein.size() < MAX_VEIN_SIZE) {
            Block current = queue.poll();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) {
                            continue;
                        }
                        Block neighbor = current.getRelative(dx, dy, dz);
                        if (neighbor.getType() == type && vein.add(neighbor.getLocation())) {
                            if (vein.size() >= MAX_VEIN_SIZE) {
                                return vein;
                            }
                            queue.add(neighbor);
                        }
                    }
                }
            }
        }
        return vein;
    }

    /** Nearest still-standing ore of the reserved vein within reach, or null (mined out / pet left). */
    private Block nextVeinBlock(Mob mob) {
        double reach = searchRadius() + 6.0;
        double reachSquared = reach * reach;
        Location petLocation = mob.getLocation();
        Block best = null;
        double bestDistance = Double.MAX_VALUE;
        Iterator<Location> it = reservedVein.iterator();
        while (it.hasNext()) {
            Location loc = it.next();
            if (loc.getWorld() != mob.getWorld() || !MaterialTags.ORES.isTagged(loc.getBlock().getType())) {
                it.remove(); // wrong world, or already mined — drop it from the vein
                continue;
            }
            double distance = loc.distanceSquared(petLocation);
            if (distance <= reachSquared && distance < bestDistance) {
                bestDistance = distance;
                best = loc.getBlock();
            }
        }
        return best;
    }

    private boolean isExposedOre(Block block) {
        if (!MaterialTags.ORES.isTagged(block.getType())) {
            return false;
        }
        for (BlockFace face : FACES) {
            if (block.getRelative(face).getType().isAir()) {
                return true;
            }
        }
        return false;
    }
}
