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

package de.Keyle.MyPet.util.hooks;

import com.github.sirblobman.combatlogx.api.ICombatLogX;
import com.github.sirblobman.combatlogx.api.object.TagReason;
import com.github.sirblobman.combatlogx.api.object.TagType;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.util.service.Load;
import de.Keyle.MyPet.api.util.service.RequiresPlugin;
import de.Keyle.MyPet.api.util.service.ServiceContainer;
import de.Keyle.MyPet.api.util.service.ServiceName;
import de.Keyle.MyPet.entity.ai.attack.PetRangedAttackGoal;
import de.Keyle.MyPet.entity.spawn.PetEntityMarker;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import static de.Keyle.MyPet.MyPetApi.getPetManager;

@ServiceName("CombatLogX")
@RequiresPlugin("CombatLogX")
@Load(Load.State.Hooks)
public class CombatLogXHook implements ServiceContainer {

    public static boolean IGNORE_PLUGIN_SETTINGS = false;

    private ICombatLogX combatLogX;

    @Override
    public boolean onEnable() {
        try {
            combatLogX = (ICombatLogX) Bukkit.getPluginManager().getPlugin("CombatLogX");
        } catch (Throwable e) {
            return false;
        }
        if (combatLogX == null) {
            return false;
        }
        Bukkit.getPluginManager().registerEvents(this, MyPetApi.getPlugin());
        return true;
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);
    }

    @Override
    public void loadConfig(ConfigurationSection config) {
        config.addDefault("MyPet.Hooks.CombatLogX.Ignore-Plugin-Settings", IGNORE_PLUGIN_SETTINGS);

        IGNORE_PLUGIN_SETTINGS = config.getBoolean("Ignore-Plugin-Settings", false);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void on(EntityDamageByEntityEvent e) {
        Entity damaged = e.getEntity();
        Entity damager = e.getDamager();

        // Resolve Pet-fired projectiles back to the shooting pet (so the
        // pet-link check below can then resolve it to the owner Player).
        // Replaces the legacy `instanceof CraftMyPetProjectile` check —
        // identification is now via the PDC owner tag that
        // PetRangedAttackGoal writes at launch time.
        if (damager instanceof Projectile projectile && (combatLogX.getConfiguration().isLinkProjectiles() || IGNORE_PLUGIN_SETTINGS)) {
            Pet sourcePet = PetRangedAttackGoal.getSourcePet(projectile);
            if (sourcePet != null) {
                Mob shooterEntity = sourcePet.getBukkitEntity();
                if (shooterEntity != null) {
                    damager = shooterEntity;
                }
            }
        }

        if ((PetEntityMarker.isMarked(damager)) && (combatLogX.getConfiguration().isLinkPets() || IGNORE_PLUGIN_SETTINGS)) {
            damager = getPetManager().getPetFromEntity(damager).getOwner().getPlayer();
        } else {
            return;
        }

        if (damager != null && damaged instanceof LivingEntity) {
            if (damaged instanceof Player p) {
                LivingEntity enemy = (LivingEntity) damager;
                TagReason reason = TagReason.ATTACKED;
                combatLogX.getCombatManager().tag(p, enemy, TagType.PLAYER, reason);
            }

            Player p = (Player) damager;
            LivingEntity enemy = (LivingEntity) damaged;
            TagType type = damaged instanceof Player ? TagType.PLAYER : TagType.MOB;
            TagReason reason = TagReason.ATTACKER;
            combatLogX.getCombatManager().tag(p, enemy, type, reason);
        }
    }
}