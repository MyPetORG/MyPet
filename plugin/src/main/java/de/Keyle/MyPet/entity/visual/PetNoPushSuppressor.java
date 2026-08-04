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
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.util.CompatUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Mob;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adds pet entities to a shared scoreboard team with
 * {@link Team.Option#COLLISION_RULE} set to {@link Team.OptionStatus#NEVER},
 * so the owner can walk through the pet without the pet blocking movement.
 *
 * <p>This replaces the previous {@code LivingEntity#setCollidable(false)}
 * call. In Paper, {@code setCollidable(false)} disables both entity-entity
 * solid collision <em>and</em> projectile hit detection (because Paper's
 * arrow code consults {@code Entity#canBeCollidedWith()} when filtering
 * candidate hit entities) — which silently broke {@code OwnerCanAttackPet}
 * for bows / tridents. The team-based mechanism only affects push physics
 * and leaves projectile collision intact.
 *
 * <p>{@link de.Keyle.MyPet.entity.types.PetCreaking.ActivationSuppressor} owns
 * its own team ({@code mypet_creaking_allies}) for AI-suppression purposes,
 * and a UUID can only belong to one Bukkit team at a time — so Creaking pets
 * are deliberately skipped here. They remain solid for now; cross-team push
 * suppression for Creaking pets is a separate concern.
 *
 * <h2>Folia safety</h2>
 *
 * <p>Scoreboard mutations run on {@link Bukkit#getGlobalRegionScheduler()}
 * and operate exclusively on string entries (pet UUID string) captured at
 * entity-region call time — the global-region tasks never dereference live
 * entities.
 *
 * <p>On Folia itself the whole mechanism is disabled ({@code isFolia()}
 * guard): Folia's {@code CraftScoreboard#registerNewTeam} throws
 * {@link UnsupportedOperationException} ("not supported yet"), so pets
 * stay pushable there. {@link de.Keyle.MyPet.MyPetPlugin} warns once at
 * startup.
 */
public final class PetNoPushSuppressor {

    static final String TEAM_NAME = "mypet_no_push";

    private static final Map<UUID, String> registrations = new ConcurrentHashMap<>();

    private PetNoPushSuppressor() {
    }

    public static void startForPet(Pet pet) {
        // Folia has no registerNewTeam — see class javadoc. Never registering
        // here also makes stopForPet a natural no-op.
        if (CompatUtil.isFolia()) return;
        Mob mob = pet.getBukkitEntity();
        if (mob == null) return;
        // Creaking pets are owned by PetCreaking.ActivationSuppressor's team.
        // Bukkit team membership is exclusive (addEntry removes from prior
        // team), so adding them here would clobber AI suppression.
        if (CreakingHelper.isCreaking(mob)) return;

        Plugin plugin = MyPetApi.getPlugin();
        if (!plugin.isEnabled()) return;
        UUID petKey = pet.getUUID();
        String mobUuidEntry = mob.getUniqueId().toString();
        stopForPet(pet);

        registrations.put(petKey, mobUuidEntry);

        Bukkit.getGlobalRegionScheduler().execute(plugin, () -> {
            Team team = ensureTeam();
            if (team == null) return;
            team.addEntry(mobUuidEntry);
        });
    }

    public static void stopForPet(Pet pet) {
        UUID petKey = pet.getUUID();
        String mobUuidEntry = registrations.remove(petKey);
        if (mobUuidEntry == null) return;

        Plugin plugin = MyPetApi.getPlugin();
        // Skip async cleanup when the plugin is shutting down — Folia rejects
        // scheduling on a disabled plugin. Stale string entries left in the
        // team after a shutdown are harmless: they don't match any live
        // entity, and the next startForPet for the same pet calls
        // stopForPet first (which removes them from `registrations` and
        // re-issues the team mutation).
        if (!plugin.isEnabled()) return;
        Bukkit.getGlobalRegionScheduler().execute(plugin, () -> {
            Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
            Team team = scoreboard.getTeam(TEAM_NAME);
            if (team == null) return;
            team.removeEntry(mobUuidEntry);
        });
    }

    private static Team ensureTeam() {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = scoreboard.getTeam(TEAM_NAME);
        if (team == null) {
            team = scoreboard.registerNewTeam(TEAM_NAME);
            team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
            // Friendly-fire stays on so projectile collision works even if
            // the shooter is ever on the same team as a pet (e.g., admin
            // commands moving a player into the team manually).
            team.setAllowFriendlyFire(true);
        }
        return team;
    }

    /**
     * Isolated 1.21.4+ Creaking class reference so older Bukkit jars without
     * {@code org.bukkit.entity.Creaking} don't fail to load this class.
     */
    private static final class CreakingHelper {
        static boolean isCreaking(Mob mob) {
            try {
                return mob instanceof org.bukkit.entity.Creaking;
            } catch (NoClassDefFoundError e) {
                return false;
            }
        }
    }
}
