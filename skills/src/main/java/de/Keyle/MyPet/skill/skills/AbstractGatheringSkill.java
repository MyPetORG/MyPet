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
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.entity.Pet.PetState;
import de.Keyle.MyPet.api.skill.SkillState;
import de.Keyle.MyPet.api.skill.ToggleableSkill;
import de.Keyle.MyPet.api.skill.UpgradeComputer;
import de.Keyle.MyPet.api.util.Scheduler;
import de.Keyle.MyPet.api.util.locale.Locale;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

/**
 * Shared base for the timer-driven gathering skills (Mining, Lumberjack, Fishing):
 * one unit of work near the owner every {@code interval} seconds within {@code range} blocks.
 */
public abstract class AbstractGatheringSkill extends AbstractSkill implements Scheduler {

    /** Max distance (squared) the owner may be from the pet for the skill to work. */
    protected static final double OWNER_RANGE_SQUARED = 16 * 16;
    /** Hard cap on the block-scan radius, whatever the skilltree grants. */
    protected static final int MAX_SEARCH_RADIUS = 8;

    protected final UpgradeComputer<Number> range = new UpgradeComputer<>(0);
    protected final UpgradeComputer<Integer> interval = new UpgradeComputer<>(0);
    /** When true the pet works bare-pawed (virtual best tool); otherwise it needs a real tool in its Backpack. */
    protected final UpgradeComputer<Boolean> toolless = new UpgradeComputer<>(false);
    /** Owner's runtime on/off toggle from the pet menu; defaults on, persisted via the skill's state codec. */
    private boolean enabled = true;
    private int timeCounter = 0;
    /** Throttle for the "no usable tool" hint so it fires once per depletion, not every interval. */
    private boolean warnedNoTool = false;

    protected AbstractGatheringSkill(Pet pet) {
        super(pet);
    }

    @Override
    public boolean isActive() {
        return range.getValue().doubleValue() > 0 && interval.getValue() > 0;
    }

    @Override
    public void reset() {
        range.removeAllUpgrades();
        interval.removeAllUpgrades();
        toolless.removeAllUpgrades();
        warnedNoTool = false;
        timeCounter = 0;
        // Abandon crack animations still in flight so a disabled skill can't finish a break.
        for (ScheduledTask task : breakTasks) {
            task.cancel();
        }
        breakTasks.clear();
        breaking.clear();
        if (approachTask != null) {
            approachTask.cancel();
            approachTask = null;
        }
        Mob mob = pet.getBukkitEntity();
        if (mob != null) {
            restoreHand(mob); // don't leave a tool stuck in the pet's hand after a skilltree change
        }
        onClaimCleared();
        releaseFocus();
    }

    public UpgradeComputer<Number> getRange() {
        return range;
    }

    public UpgradeComputer<Integer> getInterval() {
        return interval;
    }

    public UpgradeComputer<Boolean> getToolless() {
        return toolless;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public Optional<ToggleableSkill.ToggleState> getState() {
        return Optional.of(new ToggleableSkill.ToggleState(enabled));
    }

    @Override
    public void applyState(SkillState state) {
        if (state instanceof ToggleableSkill.ToggleState toggle) {
            enabled = toggle.enabled();
        }
    }

    @Override
    public Component toPrettyComponent(String locale) {
        return Component.text()
                .append(Locale.getComponent("Name.Range", locale))
                .append(Component.text(": "))
                .append(Component.text(String.format("%1.2f", range.getValue().doubleValue())).color(NamedTextColor.GOLD))
                .append(Component.space())
                .append(Locale.getComponent("Name.Blocks", locale))
                .append(Component.text(" -> "))
                .append(Component.text(interval.getValue()).color(NamedTextColor.GOLD))
                .append(Component.space())
                .append(Locale.getComponent("Name.Seconds", locale))
                .asComponent();
    }

    @Override
    public Component[] getUpgradeMessage() {
        return new Component[]{
                upgradeMessage("Message.Skill." + getName() + ".Upgrade",
                        String.format("%1.2f", range.getValue().doubleValue()), interval.getValue())
        };
    }

    @Override
    public void schedule() {
        Mob mob = pet.getBukkitEntity();
        Player owner = pet.getOwner() != null ? pet.getOwner().getPlayer() : null;
        boolean canWork = enabled && isActive() && isEnabledGlobally() && pet.getStatus() == PetState.Here
                && mob != null && !mob.isDead() && owner != null && owner.isOnline()
                && owner.getWorld() == mob.getWorld()
                && owner.getLocation().distanceSquared(mob.getLocation()) <= OWNER_RANGE_SQUARED;
        if (!canWork) {
            // Can't work right now (toggled off, owner far/away, despawned, …) — drop any claim so the
            // pet is free to follow its owner again and other work skills can run.
            releaseWorkClaim(mob);
            return;
        }
        if (!canWorkNow(mob)) {
            return; // transient per-skill pause (e.g. Fishing while swimming) — keep any claim
        }
        if (backpackOpen()) {
            return; // owner is rearranging the backpack; its contents are stale until they close it
        }
        // One chore at a time: if this pet is already mid-task for another work skill (or one has
        // reserved it, e.g. Lumberjack mid-tree), wait without spending the interval.
        if (PetWorkFocus.isBusy(pet, this)) {
            return;
        }
        if (--timeCounter > 0) {
            return;
        }
        timeCounter = Math.max(1, interval.getValue());
        beginWork(mob, owner);
    }

    /**
     * Frees this skill's work claim — its multi-cycle reservation, the pet's held tool, any pending
     * approach, and the transient focus — so the pet can follow its owner and other skills can run.
     * Idempotent: safe to call every tick while the pet can't work.
     */
    private void releaseWorkClaim(Mob mob) {
        if (approachTask != null) {
            approachTask.cancel();
            approachTask = null;
        }
        onClaimCleared();
        if (mob != null) {
            restoreHand(mob);
        }
        releaseFocus();
    }

    /** Subclass hook: drop any multi-cycle reservation (Lumberjack tree / Mining vein). Default: none. */
    protected void onClaimCleared() {
    }

    /**
     * Per-skill gate for transient conditions checked every tick (e.g. Fishing pauses while the
     * pet is swimming). Returning false holds the work timer without consuming it. Default: allowed.
     */
    protected boolean canWorkNow(Mob mob) {
        return true;
    }

    /** True if the per-skill global kill-switch in config.yml allows this skill. */
    protected abstract boolean isEnabledGlobally();

    /**
     * Where the pet should stand to do this cycle's work (a block to break, water to fish), or
     * null if there is nothing to do right now. Called while the focus is still free — returning
     * null costs nothing and leaves the turn open for another work skill.
     */
    protected abstract Location findWorkTarget(Mob mob);

    /** Does the actual work once the pet is in position (breaks the block, casts, drops). */
    protected abstract void performWork(Mob mob, Player owner);

    /** Whether the pet must first pathfind to the target; Fishing overrides this to work from the bank. */
    protected boolean requiresApproach() {
        return true;
    }

    /** Ticks to hold the focus after {@link #performWork} fires, covering the animation before release. */
    protected long workHoldTicks() {
        return 24L;
    }

    // --- Focus: one visible chore at a time ---------------------------------------------------

    /** Speed multiplier used while walking to a work target. */
    private static final double APPROACH_SPEED = 1.15D;
    /** How close (squared) the pet must get before it starts working. */
    private static final double ARRIVE_DISTANCE_SQUARED = 2.75 * 2.75;
    /** Ticks between approach re-paths / arrival checks. */
    private static final long APPROACH_POLL = 4L;
    /** Give up (and free the focus) if the target can't be reached within this many ticks. */
    private static final long APPROACH_TIMEOUT_TICKS = 100L;

    /** The in-flight approach watcher, held so {@link #reset()} can cancel it. */
    private ScheduledTask approachTask;
    /** The pet's main-hand item, stashed while it visibly holds a work tool. */
    private ItemStack stashedMainHand;
    /** True while a work tool is displayed in the pet's hand (so it's restored exactly once). */
    private boolean holdingTool;

    /** Claims the focus, then walks the pet to its target (or works in place) and works there. */
    private void beginWork(Mob mob, Player owner) {
        Location target = findWorkTarget(mob);
        if (target == null) {
            restoreHand(mob); // no more work right now — only now put the tool away
            return; // nothing in range this cycle — leave the focus free for another skill
        }
        if (!PetWorkFocus.acquire(pet, this)) {
            restoreHand(mob);
            return; // lost the race to another work skill
        }
        if (requiresApproach()) {
            approachThenWork(mob, owner, target);
        } else {
            workAndRelease(mob, owner);
        }
    }

    /** Pathfinds to {@code target}, works on arrival, and frees the focus if it can't be reached. */
    private void approachThenWork(Mob mob, Player owner, Location target) {
        mob.getPathfinder().moveTo(target, APPROACH_SPEED);
        mob.lookAt(target);
        long[] waited = {0};
        approachTask = mob.getScheduler().runAtFixedRate(MyPetApi.getPlugin(), task -> {
            // The entity scheduler follows the mob across worlds (the follow goal snap-teleports the
            // pet to an owner who changed world), but `target` stays behind — and distanceSquared
            // throws on a cross-world compare. An unreachable target is a dead chore: drop the focus.
            if (mob.isDead() || !mob.isValid() || pet.getStatus() != PetState.Here
                    || !mob.getWorld().equals(target.getWorld())) {
                approachTask = null;
                task.cancel();
                releaseFocus();
                return;
            }
            if (mob.getLocation().distanceSquared(target) <= ARRIVE_DISTANCE_SQUARED) {
                approachTask = null;
                task.cancel();
                workAndRelease(mob, owner);
                return;
            }
            waited[0] += APPROACH_POLL;
            if (waited[0] >= APPROACH_TIMEOUT_TICKS) {
                approachTask = null;
                task.cancel();
                releaseFocus(); // couldn't get there — give up so another target/skill can go
                return;
            }
            mob.getPathfinder().moveTo(target, APPROACH_SPEED);
            mob.lookAt(target);
        }, this::releaseFocus, APPROACH_POLL, APPROACH_POLL);
        if (approachTask == null) {
            releaseFocus(); // scheduler already retired (mob mid-teleport)
        }
    }

    /** Fires the work, then releases the focus after the animation (plus a Pickup linger). */
    private void workAndRelease(Mob mob, Player owner) {
        performWork(mob, owner);
        long hold = workHoldTicks();
        if (pet.getSkills().isActive(PickupImpl.class)) {
            hold += PetWorkFocus.PICKUP_LINGER_TICKS; // stay put a beat so Pickup grabs the drops
        }
        ScheduledTask release = mob.getScheduler().runDelayed(MyPetApi.getPlugin(),
                task -> endWorkCycle(), this::endWorkCycle, hold);
        if (release == null) {
            endWorkCycle();
        }
    }

    /** Puts {@code tool} in the pet's main hand for the visible work, stashing whatever it held. */
    protected void holdTool(Mob mob, ItemStack tool) {
        if (holdingTool) {
            return; // already showing a tool — re-stashing now would trap the display copy in the stash
        }
        EntityEquipment equipment = mob.getEquipment();
        if (equipment == null || tool == null || tool.getType().isAir()) {
            return;
        }
        ItemStack current = equipment.getItemInMainHand();
        // Never stash our own display copy (or a stray one left behind by an earlier bug): stashing the
        // tool would trap it so "restore" just puts it back. If the hand already looks like the tool,
        // treat the real hand as empty so restore clears it — this also self-heals a stuck display.
        stashedMainHand = current != null && current.isSimilar(tool) ? null : current;
        equipment.setItemInMainHand(tool.clone()); // a display copy — real tool wear/removal won't touch it
        holdingTool = true;
    }

    /** Restores the pet's stashed main-hand item once the work finishes. Region-safe (may be called from reset). */
    protected void restoreHand(Mob mob) {
        if (!holdingTool) {
            return;
        }
        holdingTool = false;
        ItemStack previous = stashedMainHand;
        stashedMainHand = null;
        if (!mob.isValid()) {
            return; // gone — a fresh entity gets fresh equipment on respawn anyway
        }
        if (Bukkit.isOwnedByCurrentRegion(mob)) {
            applyMainHand(mob, previous);
        } else {
            mob.getScheduler().run(MyPetApi.getPlugin(), task -> applyMainHand(mob, previous), null);
        }
    }

    private static void applyMainHand(Mob mob, ItemStack item) {
        EntityEquipment equipment = mob.getEquipment();
        if (equipment != null) {
            equipment.setItemInMainHand(item);
        }
    }

    /**
     * Ends one work cycle: frees the focus so another skill can run, but deliberately leaves the tool
     * in the pet's hand. The tool is only put away when the pet actually stops working — the next cycle
     * finds no target ({@link #beginWork}), it can no longer work ({@link #releaseWorkClaim}), the break
     * is cancelled, or the skilltree changes — so a pet mining a vein or fishing keeps its tool out
     * instead of flickering it away between blocks.
     */
    private void endWorkCycle() {
        releaseFocus();
    }

    private void releaseFocus() {
        PetWorkFocus.release(pet, this);
    }

    /** Effective search radius, clamped so the cube scan stays cheap. */
    protected int searchRadius() {
        return Math.max(1, Math.min(MAX_SEARCH_RADIUS, (int) range.getValue().doubleValue()));
    }

    /** Returns the matching block nearest to the pet within the search radius, or null. */
    protected Block findNearbyBlock(Mob mob, Predicate<Block> filter) {
        Block center = mob.getLocation().getBlock();
        int radius = searchRadius();
        Block best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    int distance = x * x + y * y + z * z;
                    if (distance >= bestDistance) {
                        continue;
                    }
                    Block block = center.getRelative(x, y, z);
                    if (filter.test(block)) {
                        best = block;
                        bestDistance = distance;
                    }
                }
            }
        }
        return best;
    }

    // --- Tool gating ------------------------------------------------------------------------

    /** Pickaxe tiers low→high — Mining resolves and names the tool it needs from this ladder. */
    protected static final Material[] PICKAXE_LADDER = {
            Material.WOODEN_PICKAXE, Material.STONE_PICKAXE, Material.IRON_PICKAXE,
            Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE
    };
    /** Axe tiers low→high — Lumberjack resolves and names the tool it needs from this ladder. */
    protected static final Material[] AXE_LADDER = {
            Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE,
            Material.DIAMOND_AXE, Material.NETHERITE_AXE
    };

    /** A tool the pet will use: the ItemStack plus the backpack slot to wear it in ({@code slot < 0} = virtual). */
    protected record WorkTool(ItemStack item, int slot) {
        boolean isVirtual() {
            return slot < 0;
        }
    }

    /** The pet's Backpack if it's active (where tools live), else null. */
    private BackpackImpl activeBackpack() {
        return pet.getSkills().isActive(BackpackImpl.class) ? pet.getSkills().get(BackpackImpl.class) : null;
    }

    /** True while the owner has the pet's backpack menu open — its contents are stale, so work pauses. */
    protected boolean backpackOpen() {
        BackpackImpl backpack = activeBackpack();
        return backpack != null && backpack.isMenuOpen();
    }

    /**
     * Resolves the tool the pet should break {@code block} with, from the given tool {@code ladder}
     * (pickaxes or axes). When {@code toolless} is granted, a virtual best tool (top of the ladder);
     * otherwise the best real tool of that class in the Backpack that actually drops the block (so a
     * wooden pick on diamond ore, or a non-axe on a log, is rejected), or null if none qualifies.
     */
    protected WorkTool resolveBreakTool(Block block, Material[] ladder) {
        if (toolless.getValue()) {
            return new WorkTool(new ItemStack(ladder[ladder.length - 1]), -1);
        }
        BackpackImpl backpack = activeBackpack();
        if (backpack == null) {
            return null;
        }
        ItemStack[] contents = backpack.rawContents();
        int bestSlot = -1;
        int bestYield = 0;
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || !isInLadder(item.getType(), ladder)) {
                continue; // only the right kind of tool (pickaxe / axe) qualifies
            }
            Collection<ItemStack> drops = block.getDrops(item);
            if (drops.isEmpty()) {
                continue; // wrong tier — wouldn't actually harvest the block
            }
            int yield = drops.stream().mapToInt(ItemStack::getAmount).sum();
            if (yield > bestYield) {
                bestYield = yield;
                bestSlot = i;
            }
        }
        return bestSlot < 0 ? null : new WorkTool(contents[bestSlot], bestSlot);
    }

    private static boolean isInLadder(Material type, Material[] ladder) {
        for (Material m : ladder) {
            if (m == type) {
                return true;
            }
        }
        return false;
    }

    /** The lowest-tier tool in {@code ladder} that actually harvests {@code block} — for the hint message. */
    protected Material requiredTool(Block block, Material[] ladder) {
        for (Material m : ladder) {
            if (!block.getDrops(new ItemStack(m)).isEmpty()) {
                return m;
            }
        }
        return ladder[ladder.length - 1];
    }

    /** Resolves a fishing rod from the Backpack (or a virtual "no rod" when toolless), or null. */
    protected WorkTool resolveRod() {
        if (toolless.getValue()) {
            return new WorkTool(null, -1); // bare-handed: no rod to display
        }
        BackpackImpl backpack = activeBackpack();
        if (backpack == null) {
            return null;
        }
        ItemStack[] contents = backpack.rawContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType() == Material.FISHING_ROD) {
                return new WorkTool(item, i);
            }
        }
        return null;
    }

    /** True if {@code tool} is still in its backpack slot (virtual/toolless is always "present") — for mid-work checks. */
    protected boolean toolPresent(WorkTool tool) {
        if (tool == null || tool.isVirtual() || tool.item() == null) {
            return true;
        }
        BackpackImpl backpack = activeBackpack();
        if (backpack == null) {
            return false;
        }
        ItemStack[] contents = backpack.rawContents();
        return tool.slot() < contents.length && contents[tool.slot()] == tool.item();
    }

    /**
     * Wears down a real tool by one use, removing it (and telling the owner) if it breaks. No-op for
     * virtual/toolless work. Re-reads the slot so a tool that already moved or broke is skipped safely.
     */
    protected void wearTool(WorkTool tool, Player owner) {
        if (tool == null || tool.isVirtual() || tool.item() == null) {
            return;
        }
        BackpackImpl backpack = activeBackpack();
        if (backpack == null) {
            return;
        }
        ItemStack[] contents = backpack.rawContents();
        if (tool.slot() >= contents.length || contents[tool.slot()] != tool.item()) {
            return;
        }
        ItemStack item = tool.item();
        short max = item.getType().getMaxDurability();
        if (max <= 0 || !(item.getItemMeta() instanceof Damageable meta) || meta.isUnbreakable()) {
            return; // unbreakable material or item
        }
        int damage = meta.getDamage() + 1;
        if (damage >= max) {
            contents[tool.slot()] = null; // snapped
            owner.playSound(owner.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 1f);
            warnedNoTool = false; // a fresh "restock a tool" hint is warranted next cycle
            owner.sendMessage(Locale.getFormattedComponent(
                    "Message.Skill." + getName() + ".ToolBroke", pet.getOwner(), pet.getDisplayName()));
        } else {
            meta.setDamage(damage);
            item.setItemMeta(meta);
        }
    }

    /** Block-breaker "no usable tool" hint, naming the block the pet wanted and the tool that would work. */
    protected void warnNoTool(Player owner, Block block, Material tool) {
        if (warnedNoTool) {
            return;
        }
        warnedNoTool = true;
        owner.sendMessage(Locale.getFormattedComponent(
                "Message.Skill." + getName() + ".NoTool", pet.getOwner(), pet.getDisplayName(),
                Component.translatable(block.getType().getBlockTranslationKey()),
                Component.translatable(tool.getItemTranslationKey())));
    }

    /** One-shot "no usable tool" hint to the owner (re-armed when a tool is next found or breaks). */
    protected void warnNoTool(Player owner) {
        if (warnedNoTool) {
            return;
        }
        warnedNoTool = true;
        owner.sendMessage(Locale.getFormattedComponent(
                "Message.Skill." + getName() + ".NoTool", pet.getOwner(), pet.getDisplayName()));
    }

    /** Clears the no-tool throttle once the pet has a usable tool again. */
    protected void toolResolved() {
        warnedNoTool = false;
    }

    // --- Animated block breaking -------------------------------------------------------------

    /** Vanilla crack stages (0..9); the block pops on the last one. */
    private static final int CRACK_STAGES = 9;
    /** Floor on the per-stage period, so even the fastest tool still shows all 9 crack stages. */
    private static final long MIN_CRACK_PERIOD = 1L;
    /** Ceiling on the per-stage period, so very hard blocks (obsidian, ancient debris) don't crack for ages. */
    private static final long MAX_CRACK_PERIOD = 6L;
    /** Only players within this distance (squared) of the block are sent the crack overlay. */
    private static final double CRACK_VIEW_DISTANCE_SQUARED = 48 * 48;
    /** Per-break "breaker" ids for the crack packet, kept clear of real entity ids. */
    private static final AtomicInteger BREAKER_ID = new AtomicInteger(0x40000000);

    /** Blocks currently mid-animation, so the interval timer never starts a second break on one. */
    private final Set<Location> breaking = ConcurrentHashMap.newKeySet();
    /** In-flight crack tasks, held so {@link #reset()} can cancel any that outlive the skill. */
    private final Set<ScheduledTask> breakTasks = ConcurrentHashMap.newKeySet();

    /**
     * Breaks a block on the owner's behalf after a short vanilla-style cracking animation, so
     * onlookers see the pet chipping away at it before it drops. Probes protection plugins with a
     * cancellable BlockBreakEvent first and aborts (returns false) if any plugin cancels it. The
     * crack is shown to nearby players; when it completes the block breaks with {@code tool}.
     *
     * @return false only when a protection plugin blocked the break; true once the animation starts
     */
    protected boolean animatedBreak(Mob mob, Player owner, Block block, WorkTool tool, Runnable onBroken) {
        BlockBreakEvent probe = new BlockBreakEvent(block, owner);
        Bukkit.getServer().getPluginManager().callEvent(probe);
        if (probe.isCancelled()) {
            return false;
        }
        Location loc = block.getLocation();
        if (!breaking.add(loc)) {
            return true; // already cracking this one
        }
        Material original = block.getType();
        int breakerId = BREAKER_ID.getAndIncrement();
        int[] stage = {0};
        long crackPeriod = crackPeriod(block, tool); // pace the crack by the tool's real dig speed (tier + Efficiency)
        ScheduledTask[] holder = new ScheduledTask[1];
        holder[0] = mob.getScheduler().runAtFixedRate(MyPetApi.getPlugin(), task -> {
            if (!toolPresent(tool) || backpackOpen()) {
                // Tool pulled mid-break, or the owner opened the backpack to move it — cancel instantly,
                // no drop, and put the hand down.
                restoreHand(mob);
                endBreak(loc, breakerId, holder[0]);
                return;
            }
            if (block.getType() != original || !mob.isValid()) {
                // Mined out, replaced with a different block, or the pet vanished — abandon it
                // without breaking whatever is there now (which may be a player-placed block).
                endBreak(loc, breakerId, holder[0]);
                return;
            }
            stage[0]++;
            if (stage[0] % 3 == 0) {
                mob.swingMainHand(); // keep the "using the tool" arm-swing going through the break
            }
            sendCrack(loc, breakerId, Math.min(1f, stage[0] / (float) CRACK_STAGES));
            if (stage[0] >= CRACK_STAGES) {
                block.breakNaturally(tool.item(), true);
                if (onBroken != null) {
                    onBroken.run(); // wear the tool now that the block actually dropped
                }
                endBreak(loc, breakerId, holder[0]);
            }
        }, () -> endBreak(loc, breakerId, holder[0]), crackPeriod, crackPeriod);
        if (holder[0] == null) {
            // Scheduler already retired (mob mid-teleport/region change): neither the tick nor
            // the retired callback will run, so release the block now instead of wedging it in
            // `breaking` for the rest of the session (which would deadlock the skill on it).
            breaking.remove(loc);
            return true;
        }
        breakTasks.add(holder[0]);
        return true;
    }

    /** Ends a break: clears the crack overlay, unmarks the block, and cancels/untracks the task. */
    private void endBreak(Location loc, int breakerId, ScheduledTask task) {
        sendCrack(loc, breakerId, 0f);
        breaking.remove(loc);
        if (task != null) {
            breakTasks.remove(task);
            task.cancel();
        }
    }

    /**
     * Ticks between crack stages so the whole animation ≈ the tool's real dig time on {@code block}.
     * Uses vanilla's harvest formula — {@code ceil(hardness × 30 / toolSpeed)} ticks to break — spread
     * across the {@link #CRACK_STAGES} stages, then clamped so the fastest tools still show a full crack
     * and the hardest blocks don't crack for ages.
     */
    private static long crackPeriod(Block block, WorkTool tool) {
        float hardness = block.getType().getHardness();
        ItemStack item = tool == null ? null : tool.item();
        if (hardness <= 0f || item == null) {
            return MIN_CRACK_PERIOD; // instant-mine block, or no concrete tool to measure — quickest crack
        }
        long breakTicks = (long) Math.ceil(hardness * 30.0 / toolSpeed(item));
        long period = Math.round(breakTicks / (double) CRACK_STAGES);
        return Math.max(MIN_CRACK_PERIOD, Math.min(MAX_CRACK_PERIOD, period));
    }

    /**
     * The tool's mining speed: its tier's base speed plus the Efficiency bonus ({@code level² + 1}).
     * {@code resolveBreakTool} only hands back a tool that actually harvests the block, so the tier
     * speed always applies (never the wrong-tool ×1 case).
     */
    private static double toolSpeed(ItemStack tool) {
        double speed = tierSpeed(tool.getType());
        int efficiency = tool.getEnchantmentLevel(Enchantment.EFFICIENCY);
        if (efficiency > 0) {
            speed += efficiency * efficiency + 1;
        }
        return speed;
    }

    /** Vanilla per-tier mining speeds, keyed off the tool material's tier prefix. */
    private static double tierSpeed(Material tool) {
        String name = tool.name();
        if (name.startsWith("WOODEN_")) return 2.0;
        if (name.startsWith("STONE_")) return 4.0;
        if (name.startsWith("IRON_")) return 6.0;
        if (name.startsWith("DIAMOND_")) return 8.0;
        if (name.startsWith("NETHERITE_")) return 9.0;
        if (name.startsWith("GOLDEN_")) return 12.0;
        return 1.0; // hand / unknown material
    }

    /** Sends the block-crack overlay (progress 0..1; 0 clears it) to players near the block. */
    private static void sendCrack(Location loc, int breakerId, float progress) {
        for (Player player : loc.getWorld().getPlayers()) {
            // Hop onto each player's region thread before reading their location / sending the
            // packet — on Folia the crack task runs on the mob's region, not the player's.
            player.getScheduler().run(MyPetApi.getPlugin(), task -> {
                // Re-check the world here, not at enumeration time: the player can leave loc's world
                // between the two, and distanceSquared throws on a cross-world compare.
                if (player.getWorld().equals(loc.getWorld())
                        && player.getLocation().distanceSquared(loc) <= CRACK_VIEW_DISTANCE_SQUARED) {
                    player.sendBlockDamage(loc, progress, breakerId);
                }
            }, null);
        }
    }
}
