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

package de.Keyle.MyPet.entity.types;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.behavior.PetBehavior;
import de.Keyle.MyPet.api.behavior.PetBehaviorHelpers;
import de.Keyle.MyPet.api.config.ConfigKey;
import de.Keyle.MyPet.api.entity.DefaultInfo;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.entity.ShopInfo;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.util.locale.Locale;
import de.Keyle.MyPet.entity.PetImpl;
import de.Keyle.MyPet.entity.PetInfoAccess;
import de.Keyle.MyPet.entity.options.PetCreationOptions;
import de.Keyle.MyPet.entity.options.PetCreationOptions.OptionSpec;
import de.Keyle.MyPet.entity.visual.PetEntitySnapshot;
import org.bukkit.Material;
import org.bukkit.entity.Creeper;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.CreeperPowerEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

@ShopInfo
@DefaultInfo(food = {Material.GUNPOWDER}, flySpeed = 0.5507D)
public class PetCreeper extends PetImpl {

    public static final ConfigKey<Boolean> ALLOW_FLINT_AND_STEEL_EXPLODE = ConfigKey.bool("Creeper", "AllowFlintAndSteelExplode", false);
    public static final ConfigKey<Boolean> ALLOW_NON_OWNER_FLINT_AND_STEEL = ConfigKey.bool("Creeper", "AllowNonOwnerFlintAndSteel", false);
    public static final ConfigKey<Boolean> ALLOW_EXPLOSION_BLOCK_DAMAGE = ConfigKey.bool("Creeper", "AllowExplosionBlockDamage", false);
    public static final ConfigKey<Boolean> ALLOW_EXPLOSION_ENTITY_DAMAGE = ConfigKey.bool("Creeper", "AllowExplosionEntityDamage", false);
    public static final ConfigKey<Boolean> ALLOW_LIGHTNING_POWER = ConfigKey.bool("Creeper", "AllowLightningPower", false);

    /**
     * Right-click with flint-and-steel on a Creeper pet. Gated by
     * {@link #ALLOW_FLINT_AND_STEEL_EXPLODE} and {@link #ALLOW_NON_OWNER_FLINT_AND_STEEL}.
     * Runs at {@code LOW} priority so cancellations land before vanilla's
     * {@code Creeper#mobInteract}.
     */
    public static final PetBehavior<PlayerInteractEntityEvent> FLINT_AND_STEEL_IGNITE =
            PetBehaviorHelpers.onPetInteract("Creeper", (event, pet, mob) -> {
                ItemStack item = event.getPlayer().getInventory().getItem(event.getHand());
                if (item == null || item.getType() != Material.FLINT_AND_STEEL) return;

                if (!ALLOW_FLINT_AND_STEEL_EXPLODE.get()) {
                    event.setCancelled(true);
                    return;
                }
                if (!ALLOW_NON_OWNER_FLINT_AND_STEEL.get()
                        && !isOwner(event.getPlayer().getUniqueId(), pet)) {
                    event.setCancelled(true);
                }
            });

    /**
     * Lightning strike on a Creeper pet. Cancels the {@link CreeperPowerEvent}
     * unless {@link #ALLOW_LIGHTNING_POWER} is on.
     */
    public static final PetBehavior<CreeperPowerEvent> LIGHTNING_POWER_GATE =
            PetBehaviorHelpers.onPetCreeperPower("Creeper", EventPriority.HIGH, true,
                    (event, pet, mob) -> {
                        if (event.getCause() != CreeperPowerEvent.PowerCause.LIGHTNING) return;
                        if (!ALLOW_LIGHTNING_POWER.get()) {
                            event.setCancelled(true);
                        }
                    });

    /**
     * Strips terrain/yield damage when {@link #ALLOW_EXPLOSION_BLOCK_DAMAGE}
     * is off, scrubs ignite state to prevent re-explode loops on respawn,
     * captures snapshot, then routes the pet through the death pipeline.
     *
     * <p>Vanilla {@code Creeper#explodeCreeper} calls {@code discard()}
     * directly, skipping {@code die()} and {@code EntityDeathEvent}. Without
     * this routing the pet would be removed without ever being marked
     * {@link PetState#Dead}, so the spawn pipeline would immediately respawn
     * it — an instant-respawn loop.
     */
    public static final PetBehavior<EntityExplodeEvent> EXPLODE_DEATH_ROUTING =
            PetBehaviorHelpers.onPetExplodes("Creeper", EventPriority.HIGH, false,
                    (event, pet, mob) -> {
                        if (!(mob instanceof Creeper creeper)) return;

                        if (!ALLOW_EXPLOSION_BLOCK_DAMAGE.get()) {
                            event.blockList().clear();
                            event.setYield(0f);
                        }

                        // Scrub live ignite state before snapshot. Vanilla writes
                        // `ignited` and a near-zero `Fuse` into NBT during
                        // explodeCreeper, and Creeper#tick re-triggers the fuse on
                        // respawn if those persist — the pet would explode again
                        // the moment it comes back. Mutating the doomed entity is
                        // safe because it's about to be discarded.
                        creeper.setIgnited(false);
                        creeper.setFuseTicks(creeper.getMaxFuseTicks());
                        try {
                            PetInfoAccess.write(pet, PetEntitySnapshot.capture(creeper));
                        } catch (Throwable t) {
                            MyPetApi.getLogger().warning(
                                    "Failed to capture EntitySnapshot for Creeper pet "
                                            + pet.getUUID() + " on explode — pet will respawn"
                                            + " with default live-entity state. " + t.getMessage());
                        }
                        pet.setRespawnTime(
                                (Configuration.Respawn.TIME_FIXED
                                        + MyPetApi.getPetInfo().getCustomRespawnTimeFixed(pet.getPetType()))
                                        + (pet.getExperience().getLevel()
                                        * (Configuration.Respawn.TIME_FACTOR
                                        + MyPetApi.getPetInfo().getCustomRespawnTimeFactor(pet.getPetType())))
                        );
                        pet.setStatus(PetState.Dead);
                        if (pet.getOwner() != null && pet.getOwner().getPlayer() != null) {
                            pet.getOwner().sendMessage(Locale.getFormattedComponent(
                                    "Message.Spawn.Respawn.In",
                                    pet.getOwner().getPlayer(),
                                    pet.getDisplayName(),
                                    pet.getRespawnTime()));
                        }
                    });

    /**
     * Cancels the per-victim splash damage that vanilla applies to nearby
     * entities during a Creeper-pet explosion. The block-damage and
     * entity-damage paths are independent in modern Paper —
     * {@link EntityExplodeEvent} cancellation only suppresses block damage,
     * so the entity-damage flag has its own dedicated handler.
     */
    public static final PetBehavior<EntityDamageByEntityEvent> EXPLOSION_SPLASH_DAMAGE_GATE =
            PetBehaviorHelpers.onPetDamages("Creeper", EventPriority.HIGH, true,
                    (event, pet, mob) -> {
                        if (ALLOW_EXPLOSION_ENTITY_DAMAGE.get()) return;
                        if (event.getCause() != DamageCause.ENTITY_EXPLOSION) return;
                        event.setCancelled(true);
                    });

    public static final List<OptionSpec> CREATION_SPECS = PetCreationOptions.specs(
            () -> OptionSpec.ofFlag("powered", Creeper.class, c -> c.setPowered(true))
    );

    public PetCreeper(MyPetPlayer petOwner) {
        super(petOwner);
    }

    private static boolean isOwner(UUID playerId, Pet pet) {
        return pet.getOwner() != null
                && pet.getOwner().getPlayer() != null
                && pet.getOwner().getPlayer().getUniqueId().equals(playerId);
    }
}
