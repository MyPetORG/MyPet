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

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.MyPetGlobal;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.entity.Pet.PetState;
import de.Keyle.MyPet.api.skill.SkillStateCodecs;
import de.Keyle.MyPet.api.skill.SkillUpgrades;
import de.Keyle.MyPet.api.skill.ToggleableSkill;
import de.Keyle.MyPet.api.skill.UpgradeParsers;
import de.Keyle.MyPet.api.skill.UpgradeSchema;
import de.Keyle.MyPet.api.skill.skills.Fishing;
import de.Keyle.MyPet.skill.upgrades.FishingUpgrade;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTables;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.List;
import java.util.Random;

public class FishingImpl extends AbstractGatheringSkill implements Fishing {

    public static final SkillUpgrades UPGRADES = SkillUpgrades.of(Fishing.class,
            UpgradeSchema.builder()
                    .number("range").label("Range").suffix(" blocks").cumulative()
                    .integer("interval").label("Interval (s)").cumulative()
                    .bool("toolless").label("No tool required")
                    .build(), json -> new FishingUpgrade()
            .setRangeModifier(UpgradeParsers.parseNumber(UpgradeParsers.get(json, "range")))
            .setIntervalModifier(UpgradeParsers.parseInteger(UpgradeParsers.get(json, "interval")))
            .setToollessModifier(UpgradeParsers.parseBoolean(UpgradeParsers.get(json, "toolless"))));

    public static final SkillStateCodecs STATE_CODEC = SkillStateCodecs.of(Fishing.class,
            ToggleableSkill.ToggleState.class, ToggleableSkill.TOGGLE_CODEC);

    private static final Random RANDOM = new Random();
    /** Base wait before a catch; Lure shortens it. */
    private static final int BASE_WAIT_TICKS = 40;
    /** Ticks Lure shaves off the wait per level (vanilla Lure speeds up bites the same way). */
    private static final int LURE_TICKS_PER_LEVEL = 8;
    /** Floor on the wait so even Lure III still shows a brief cast. */
    private static final int MIN_WAIT_TICKS = 12;
    /** Particle points drawn along the "line" from the pet's hand to the bobber. */
    private static final int LINE_POINTS = 8;

    /** The in-flight cast, held so {@link #reset()} can cancel a cast that outlives the skill. */
    private ScheduledTask castTask;

    public FishingImpl(Pet pet) {
        super(pet);
    }

    @Override
    public void reset() {
        if (castTask != null) {
            castTask.cancel(); // a skilltree change mid-cast must not still drop a catch
            castTask = null;
        }
        super.reset();
    }

    @Override
    protected boolean isEnabledGlobally() {
        return MyPetGlobal.Skilltree.Skill.Fishing.ACTIVE.get();
    }

    @Override
    protected boolean canWorkNow(Mob mob) {
        // Fish from the bank, not while actively swimming (deep water). Shallow wading is fine.
        return !mob.isSwimming();
    }

    @Override
    protected boolean requiresApproach() {
        return false; // fishes from the bank — no walking onto the water
    }

    @Override
    protected long workHoldTicks() {
        // Hold the focus through the longest wait before releasing the hand.
        return BASE_WAIT_TICKS + 12L;
    }

    @Override
    protected Location findWorkTarget(Mob mob) {
        Block water = findNearbyBlock(mob, this::isOpenWater);
        if (water == null) {
            return null;
        }
        if (resolveRod() == null) {
            warnNoTool(pet.getOwner().getPlayer()); // no fishing rod in the backpack — don't cast
            return null;
        }
        toolResolved();
        return water.getLocation().toCenterLocation().add(0, 0.5, 0);
    }

    @Override
    protected void performWork(Mob mob, Player owner) {
        Block water = findNearbyBlock(mob, this::isOpenWater);
        if (water == null) {
            return;
        }
        WorkTool rod = resolveRod();
        if (rod == null) {
            return; // rod used up between committing and casting
        }
        if (!rod.isVirtual()) {
            holdTool(mob, rod.item()); // show a real rod; toolless casts bare-handed
        }
        Location bobber = water.getLocation().toCenterLocation().add(0, 0.5, 0);
        mob.lookAt(bobber);
        mob.swingMainHand(); // the cast flick
        mob.getWorld().playSound(mob.getLocation(), Sound.ENTITY_FISHING_BOBBER_THROW, 0.6F, 1F);
        // Lure speeds up the bite; Luck of the Sea improves the catch — both read off the actual rod.
        int lure = enchantLevel(rod, Enchantment.LURE);
        int luck = enchantLevel(rod, Enchantment.LUCK_OF_THE_SEA);
        int waitTicks = Math.max(MIN_WAIT_TICKS, BASE_WAIT_TICKS - lure * LURE_TICKS_PER_LEVEL);
        animateCast(mob, owner, rod, bobber, waitTicks, luck);
    }

    /**
     * Runs the cast: the bobber splashes down at the water, the vanilla fishing ripple shows while it
     * waits, then on the bite it retrieves and drops the catch.
     *
     * <p>A genuine {@link org.bukkit.entity.FishHook} entity can't be driven by a pet — the server
     * discards a hook whose owner isn't a player actively holding a rod within 32 blocks, and an
     * owner-owned hook would fight the owner's own fishing — so the cast is shown with the water's
     * real fishing particles and sounds, and the catch is rolled from the vanilla fishing loot tables.
     */
    private void animateCast(Mob mob, Player owner, WorkTool rod, Location bobber, int waitTicks, int luck) {
        World world = mob.getWorld();
        world.spawnParticle(Particle.SPLASH, bobber, 20, 0.3, 0.1, 0.3, 0);
        world.playSound(bobber, Sound.ENTITY_FISHING_BOBBER_SPLASH, 0.5F, 1F);
        int[] t = {0};
        castTask = mob.getScheduler().runAtFixedRate(MyPetApi.getPlugin(), task -> {
            if (!mob.isValid() || pet.getStatus() != PetState.Here || !toolPresent(rod) || backpackOpen()) {
                castTask = null;
                task.cancel(); // pet gone, rod pulled, or backpack opened mid-cast — abandon the catch.
                // The rod display is left as-is; it's cleared cleanly on the next idle cycle (no rod →
                // findWorkTarget returns null → restoreHand), so a transient trip here won't flicker it.
                return;
            }
            t[0]++;
            if (t[0] % 2 == 0) {
                drawLine(world, mob.getEyeLocation(), bobber); // the fishing line from the pet's hand to the bobber
            }
            world.spawnParticle(Particle.FISHING, bobber, 2, 0.1, 0.02, 0.1, 0.01); // the vanilla water-fishing ripple
            if (t[0] >= waitTicks) {
                castTask = null;
                task.cancel();
                world.spawnParticle(Particle.SPLASH, bobber, 30, 0.25, 0.1, 0.25, 0.1);
                world.playSound(bobber, Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 0.6F, 1F);
                dropCatch(mob, rod, owner, bobber, luck);
            }
        }, () -> castTask = null, 1L, 1L);
        if (castTask == null) {
            dropCatch(mob, rod, owner, bobber, luck); // scheduler retired (mob mid-teleport) — resolve the catch directly
        }
    }

    /** Draws the fishing line as a short trail of particles sagging from the pet's hand to the bobber. */
    private static void drawLine(World world, Location from, Location bobber) {
        Vector delta = bobber.clone().subtract(from).toVector();
        for (int i = 1; i <= LINE_POINTS; i++) {
            double f = i / (double) (LINE_POINTS + 1);
            Location point = from.clone().add(delta.clone().multiply(f)).subtract(0, Math.sin(Math.PI * f) * 0.15, 0);
            world.spawnParticle(Particle.END_ROD, point, 1, 0, 0, 0, 0);
        }
    }

    private void dropCatch(Mob mob, WorkTool rod, Player owner, Location bobber, int luck) {
        if (!mob.isValid() || pet.getStatus() != PetState.Here || !toolPresent(rod)) {
            return; // pet gone, or the rod was pulled from the backpack mid-cast — no catch
        }
        Location dropAt = mob.getLocation();
        for (ItemStack stack : rollLoot(bobber, luck)) {
            if (stack != null && !stack.getType().isAir()) {
                mob.getWorld().dropItemNaturally(dropAt, stack);
            }
        }
        wearTool(rod, owner); // a catch costs the rod one durability
    }

    /** Reads an enchant level off the actual rod (0 for a toolless/virtual rod). */
    private static int enchantLevel(WorkTool rod, Enchantment enchant) {
        ItemStack item = rod.item();
        return item == null ? 0 : item.getEnchantmentLevel(enchant);
    }

    private static final Material[] FISH_POOL = {
            Material.COD, Material.COD, Material.COD, Material.SALMON, Material.SALMON,
            Material.PUFFERFISH, Material.TROPICAL_FISH
    };
    private static final Material[] JUNK_POOL = {
            Material.LILY_PAD, Material.BOWL, Material.LEATHER, Material.LEATHER_BOOTS,
            Material.ROTTEN_FLESH, Material.STICK, Material.STRING, Material.BONE,
            Material.INK_SAC, Material.TRIPWIRE_HOOK, Material.BAMBOO
    };
    private static final Material[] TREASURE_POOL = {
            Material.NAME_TAG, Material.SADDLE, Material.NAUTILUS_SHELL,
            Material.BOW, Material.ENCHANTED_BOOK, Material.FISHING_ROD
    };

    /**
     * Rolls the catch. Splits between fish, junk, and treasure like real fishing, with Luck of the Sea
     * shifting the odds toward treasure, so the pet reels in the full range — enchanted books, bows,
     * name tags, saddles — not just fish. It tries the real loot table first, then falls back to a
     * curated pool (the vanilla loot tables can't populate junk/treasure without a real fishing hook).
     */
    private Collection<ItemStack> rollLoot(Location location, int luck) {
        LootTables pool = pickPool(luck);
        try {
            LootContext context = new LootContext.Builder(location).luck(luck).build();
            Collection<ItemStack> loot = pool.getLootTable().populateLoot(RANDOM, context);
            if (!loot.isEmpty()) {
                return loot;
            }
        } catch (Exception ignored) {
            // fall through to the curated pool
        }
        return List.of(curatedCatch(pool));
    }

    private static ItemStack curatedCatch(LootTables pool) {
        Material[] table = pool == LootTables.FISHING_TREASURE ? TREASURE_POOL
                : pool == LootTables.FISHING_JUNK ? JUNK_POOL : FISH_POOL;
        return new ItemStack(table[RANDOM.nextInt(table.length)]);
    }

    /** Vanilla-style pool split (≈5% treasure / 10% junk / rest fish), shifted by Luck of the Sea. */
    private static LootTables pickPool(int luck) {
        int treasure = 5 + 2 * luck;
        int junk = Math.max(1, 10 - 2 * luck);
        int roll = RANDOM.nextInt(100);
        if (roll < treasure) {
            return LootTables.FISHING_TREASURE;
        }
        if (roll < treasure + junk) {
            return LootTables.FISHING_JUNK;
        }
        return LootTables.FISHING_FISH;
    }

    private boolean isOpenWater(Block block) {
        return block.getType() == Material.WATER && block.getRelative(BlockFace.UP).getType().isAir();
    }
}
