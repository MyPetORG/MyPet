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

package de.Keyle.MyPet.entity.visual;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.util.CompatUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Prevents a Creaking pet from freezing or autonomously attacking players via
 * a shared scoreboard team ({@value #TEAM_NAME}).
 *
 * <p>Every active Creaking pet and every online player is added to a single
 * shared team. {@link org.bukkit.entity.LivingEntity#isAlliedTo} and
 * {@link Mob#canAttack} both resolve team membership as "allied", so inside
 * {@code Creaking#checkCanMove()} every player fails the
 * {@code canAttack(p) && !isAlliedTo(p)} filter — {@code hasPotentialTarget}
 * stays false, the freeze branch is never entered, and the brain's
 * {@code StartAttacking} behaviour can never acquire a player target.
 *
 * <p>MyPet's own combat pipeline is unaffected: {@code PetAggressiveTargetGoal}
 * selects targets via {@code HookHelper.canHurt(...)} (not teams), and
 * {@code PetMeleeAttackGoal#applyPetDamage} applies damage via
 * {@code target.damage(amount, damageSource)} which bypasses team friendly-fire
 * checks. Aggressive mode still attacks non-owner players as designed.
 *
 * <h2>Folia safety</h2>
 *
 * <p>Scoreboard mutations run on {@link Bukkit#getGlobalRegionScheduler()}
 * and operate exclusively on string entries (pet UUID string, player name)
 * captured at entity-region call time — the global-region tasks never
 * dereference live entities or players.
 *
 * <h2>Version isolation</h2>
 *
 * <p>All {@code org.bukkit.entity.Creaking} references are kept inside
 * {@link CreakingHelper}; the outer class short-circuits on pre-1.21.4
 * servers before the inner class is ever referenced, so {@code Creaking.class}
 * is never loaded on versions that don't ship it.
 */
public final class CreakingActivationSuppressor {

    static final String TEAM_NAME = "mypet_creaking_allies";

    private static final Map<UUID, String> registrations = new ConcurrentHashMap<>();
    private static final AtomicInteger activePetCount = new AtomicInteger(0);

    private static final boolean SUPPORTED =
            CompatUtil.minecraftVersionEqualsOrAbove("1.21.4");

    private CreakingActivationSuppressor() {
    }

    public static void startForPet(MyPet pet) {
        if (!SUPPORTED) return;
        Mob mob = pet.getBukkitEntity();
        if (mob == null) return;
        if (!CreakingHelper.isCreaking(mob)) return;

        Plugin plugin = MyPetApi.getPlugin();
        UUID petKey = pet.getUUID();
        String mobUuidEntry = mob.getUniqueId().toString();
        stopForPet(pet);

        registrations.put(petKey, mobUuidEntry);
        activePetCount.incrementAndGet();

        // Scoreboard mutations must run on the global region on Folia.
        Bukkit.getGlobalRegionScheduler().execute(plugin, () -> {
            Team team = ensureTeam();
            if (team == null) return;
            team.addEntry(mobUuidEntry);
            for (Player p : Bukkit.getOnlinePlayers()) {
                team.addEntry(p.getName());
            }
        });
    }

    public static void stopForPet(MyPet pet) {
        UUID petKey = pet.getUUID();
        String mobUuidEntry = registrations.remove(petKey);
        if (mobUuidEntry == null) return;

        if (!SUPPORTED) return;
        activePetCount.decrementAndGet();

        Plugin plugin = MyPetApi.getPlugin();
        Bukkit.getGlobalRegionScheduler().execute(plugin, () -> {
            Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
            Team team = scoreboard.getTeam(TEAM_NAME);
            if (team == null) return;
            team.removeEntry(mobUuidEntry);
            // Player entries are left in place intentionally: they're harmless while
            // no Creaking pets are active (the team only matters inside Creaking's
            // own filter), and re-adding on the next spawn is idempotent.
        });
    }

    /**
     * Adds a freshly-joined player to the allies team if at least one Creaking
     * pet is currently active. Called from {@code CreakingHeartListener}'s
     * {@code PlayerJoinEvent} handler.
     */
    public static void onPlayerJoin(Player player) {
        if (!SUPPORTED) return;
        if (activePetCount.get() <= 0) return;

        String name = player.getName();
        Plugin plugin = MyPetApi.getPlugin();
        Bukkit.getGlobalRegionScheduler().execute(plugin, () -> {
            Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
            Team team = scoreboard.getTeam(TEAM_NAME);
            if (team == null) return;
            team.addEntry(name);
        });
    }

    private static Team ensureTeam() {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = scoreboard.getTeam(TEAM_NAME);
        if (team == null) {
            team = scoreboard.registerNewTeam(TEAM_NAME);
            // Owner->pet damage still needs to work for the dismiss/damage flows.
            team.setAllowFriendlyFire(true);
            team.setCanSeeFriendlyInvisibles(false);
        }
        return team;
    }

    private static final class CreakingHelper {
        static boolean isCreaking(Mob mob) {
            return mob instanceof org.bukkit.entity.Creaking;
        }
    }
}
