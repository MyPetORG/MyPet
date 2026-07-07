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

package de.Keyle.MyPet.entity.model;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.entity.PetType;
import de.Keyle.MyPet.api.lifecycle.PetLifecycleHook;
import de.Keyle.MyPet.api.util.hooks.types.PetModelHook;
import de.Keyle.MyPet.api.util.hooks.types.PetModelSourceHook;
import de.Keyle.MyPet.entity.types.ModelPet;
import org.bukkit.Location;
import org.bukkit.entity.Mob;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves a pet to its active model (renderer hook + model id) and drives the
 * global attach-on-spawn / detach-on-despawn lifecycle. Side-effect-free resolve
 * so it can be queried both at visual-sync time and at lifecycle time.
 */
public final class PetModelService {

    private PetModelService() {}

    public record Resolved(PetModelHook hook, String modelId) {}

    /** Provider name, model id, and optional nametag height for a single pet type. */
    public record ModelConfig(String provider, String modelId, Double nameHeight) {}

    private static volatile boolean initialised = false;

    /**
     * Ticks to wait after spawn before removing a model from a type that has no configured
     * override. The delay lets a renderer plugin finish re-applying its model from the
     * entity PDC (which it does asynchronously) so the cleanup actually catches it.
     */
    private static final long MODEL_CLEANUP_DELAY_TICKS = 3L;

    /**
     * Extra {@link #reconcileAfterRevival} attempts (each after {@link #MODEL_CLEANUP_DELAY_TICKS})
     * granted to a source-driven pet whose model hasn't arrived yet. A third-party source may apply
     * its model on its own schedule, later than the first cleanup pass — retry before tearing the
     * nametag/spawn-animation down rather than giving up on the first miss.
     */
    private static final int MODEL_REVIVAL_MAX_RETRIES = 5;

    /** Fallback removal/hand-off delay (ticks) when a renderer can't report an animation's length. */
    private static final long DEFAULT_ANIM_FALLBACK_TICKS = 20L;

    /** Per-type model configuration, keyed by lower-case pet type name. */
    private static final Map<String, ModelConfig> MODELS = new ConcurrentHashMap<>();
    /**
     * Per-type animation-name overrides ({@code Model.Animations.<event>}), keyed by lower-case
     * type name. Applies to rendered AND source-driven types (source models from third-party
     * packs may not follow the canonical names). Empty map for types with no overrides.
     */
    private static final Map<String, Map<PetModelAnimation, String>> ANIMATION_OVERRIDES = new ConcurrentHashMap<>();
    /**
     * Pet UUIDs whose host was freshly summoned (not snapshot-restored), so the spawn
     * animation plays once on a genuine summon and not on every chunk-reload respawn.
     * Set by {@code VanillaMobSpawner}; consumed when the model is confirmed present.
     */
    private static final Set<UUID> FRESH_SPAWNS = ConcurrentHashMap.newKeySet();
    /** Fallback offset added above the host mob's height when a type has no configured name height. */
    private static final double DEFAULT_NAME_OFFSET = 0.5;

    /** Records a pet type's model configuration. Called by the config loader during (re)load. */
    public static void registerModel(String typeName, ModelConfig cfg) {
        MODELS.put(typeName.toLowerCase(Locale.ROOT), cfg);
    }

    /** Clears all registered model configurations. Call before a (re)load. */
    public static void clearModels() {
        MODELS.clear();
    }

    /** Read-only view of all currently registered model configurations. */
    public static Collection<ModelConfig> configs() {
        return MODELS.values();
    }

    /** Records a pet type's per-event animation-name overrides. Ignored when empty. */
    public static void registerAnimationOverrides(String typeName, Map<PetModelAnimation, String> overrides) {
        if (overrides != null && !overrides.isEmpty()) {
            ANIMATION_OVERRIDES.put(typeName.toLowerCase(Locale.ROOT), Map.copyOf(overrides));
        }
    }

    /** Clears all animation-name overrides. Call before a (re)load. */
    public static void clearAnimationOverrides() {
        ANIMATION_OVERRIDES.clear();
    }

    /** Marks a pet as freshly summoned (not snapshot-restored), gating its one-shot spawn animation. */
    public static void markFreshSpawn(Pet pet) {
        if (pet != null) {
            FRESH_SPAWNS.add(pet.getUUID());
        }
    }

    private static boolean consumeFreshSpawn(Pet pet) {
        return pet != null && FRESH_SPAWNS.remove(pet.getUUID());
    }

    /**
     * Whether the pet's type is source-driven: its configured provider is served by a
     * {@link PetModelSourceHook} (the model rides in from an adopted creature) and by NO
     * {@link PetModelHook} renderer. Derived live from the hook registry, so it needs no
     * parse-time flag and is always resolved against fully-loaded hooks by spawn time.
     */
    public static boolean isSourceDriven(Pet pet) {
        if (pet == null) {
            return false;
        }
        ModelConfig cfg = MODELS.get(pet.getPetType().name().toLowerCase(Locale.ROOT));
        if (cfg == null || cfg.provider() == null) {
            return false;
        }
        for (PetModelHook hook : MyPetApi.getServiceManager().getServices(PetModelHook.class)) {
            if (hook.getServiceName().equalsIgnoreCase(cfg.provider())) {
                return false; // a renderer draws it -> rendered, not source-driven
            }
        }
        for (PetModelSourceHook src : MyPetApi.getServiceManager().getServices(PetModelSourceHook.class)) {
            if (src.getServiceName().equalsIgnoreCase(cfg.provider())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the configured model for the pet's type by looking it up in the registry
     * and matching the provider to an active {@link PetModelHook} by service name.
     * Returns empty when the type has no registered config or its provider is not installed.
     */
    public static Optional<Resolved> resolve(Pet pet) {
        if (pet == null) {
            return Optional.empty();
        }
        ModelConfig cfg = MODELS.get(pet.getPetType().name().toLowerCase(Locale.ROOT));
        if (cfg == null) {
            return Optional.empty();
        }
        for (PetModelHook hook : MyPetApi.getServiceManager().getServices(PetModelHook.class)) {
            if (hook.getServiceName().equalsIgnoreCase(cfg.provider())) {
                return Optional.of(new Resolved(hook, cfg.modelId()));
            }
        }
        return Optional.empty(); // provider not installed → graceful no-op
    }

    public static boolean hasModel(Pet pet) {
        return resolve(pet).isPresent();
    }

    /** The configured model id for a pet type, or null when the type has no Model block. */
    public static String modelIdOf(Pet pet) {
        if (pet == null) {
            return null;
        }
        ModelConfig cfg = MODELS.get(pet.getPetType().name().toLowerCase(Locale.ROOT));
        return cfg == null ? null : cfg.modelId();
    }

    /**
     * The custom-creature ({@link ModelPet}) type whose {@code Model.Provider} AND {@code Model.Id}
     * both match, rendered or source-driven. Lets any mob wearing a pet's model — a rendered
     * creature left in the wild on release, a {@code /meg summon} mob, or a wild MythicMob — resolve
     * back to its pet type without the type name having to equal the id. Matching on the provider as
     * well as the id keeps two types that share an id string across different providers (e.g. a
     * ModelEngine {@code capybara} and a BetterModel {@code capybara}) from cross-resolving.
     * Deterministic (lowest type name) on the unlikely chance more than one survives
     * {@code CustomPetLoader}'s per-(provider,id) dedup.
     */
    public static PetType typeForModel(String provider, String modelId) {
        if (provider == null || modelId == null) {
            return null;
        }
        PetType best = null;
        for (Map.Entry<String, ModelConfig> e : MODELS.entrySet()) {
            ModelConfig cfg = e.getValue();
            if (cfg.provider() == null || cfg.modelId() == null
                    || !cfg.provider().equalsIgnoreCase(provider) || !cfg.modelId().equalsIgnoreCase(modelId)) {
                continue;
            }
            PetType t = PetType.byNameOrNull(e.getKey());
            if (t != null && t.getPetClass() == ModelPet.class
                    && (best == null || t.name().compareToIgnoreCase(best.name()) < 0)) {
                best = t;
            }
        }
        return best;
    }

    /**
     * The renderer hook currently drawing the pet's model: the configured provider for a
     * rendered type, otherwise the first hook reporting a model on the host (covers
     * source-driven pets, whose model rode in from an adopted creature). Empty if none.
     */
    private static Optional<PetModelHook> activeHook(Pet pet) {
        Optional<Resolved> r = resolve(pet);
        if (r.isPresent()) {
            return Optional.of(r.get().hook());
        }
        for (PetModelHook hook : MyPetApi.getServiceManager().getServices(PetModelHook.class)) {
            try {
                if (!hook.currentModels(pet).isEmpty()) {
                    return Optional.of(hook);
                }
            } catch (Throwable ignored) {
                // one provider failing must not block the others
            }
        }
        return Optional.empty();
    }

    /** Whether a renderer is currently drawing a model on the pet's live host. */
    public static boolean hasActiveModel(Pet pet) {
        return pet != null && pet.getBukkitEntity() != null && activeHook(pet).isPresent();
    }

    /** Resolved animation name for an event: the pet type's override, else the canonical default. */
    private static String animationName(Pet pet, PetModelAnimation event) {
        Map<PetModelAnimation, String> overrides = ANIMATION_OVERRIDES.get(pet.getPetType().name().toLowerCase(Locale.ROOT));
        if (overrides != null) {
            String name = overrides.get(event);
            if (name != null) {
                return name;
            }
        }
        return event.defaultName();
    }

    /**
     * Plays a discrete animation on the pet's active renderer, across all model paths.
     * Best-effort: a no-op when no provider/model is present or the animation is unknown.
     */
    public static void playAnimation(Pet pet, PetModelAnimation event) {
        if (pet == null || pet.getBukkitEntity() == null) {
            return;
        }
        activeHook(pet).ifPresent(hook -> {
            try {
                hook.playAnimation(pet, animationName(pet, event), event.loops());
            } catch (Throwable ignored) {
                // best-effort: a bad provider must not break the trigger site
            }
        });
    }

    /**
     * Plays the despawn animation on the pet's active renderer and returns how many ticks
     * to wait before removing the host (the animation's length, or {@link #DEFAULT_ANIM_FALLBACK_TICKS}
     * when the renderer can't report it). Empty when the pet has no active model.
     */
    public static OptionalLong playDespawn(Pet pet) {
        if (pet == null || pet.getBukkitEntity() == null) {
            return OptionalLong.empty();
        }
        Optional<PetModelHook> hook = activeHook(pet);
        if (hook.isEmpty()) {
            return OptionalLong.empty();
        }
        PetModelHook h = hook.get();
        String name = animationName(pet, PetModelAnimation.DESPAWN);
        try {
            h.playAnimation(pet, name, PetModelAnimation.DESPAWN.loops());
        } catch (Throwable ignored) {
            // best-effort
        }
        double ticks;
        try {
            ticks = h.animationLength(pet, name).orElse(DEFAULT_ANIM_FALLBACK_TICKS);
        } catch (Throwable t) {
            ticks = DEFAULT_ANIM_FALLBACK_TICKS;
        }
        return OptionalLong.of(Math.max(1L, (long) Math.ceil(ticks)));
    }

    /**
     * On release of a RENDERED custom creature (one with a {@code Model.Provider}/{@code Id},
     * no source plugin to reconstruct it), leave a wild modeled mob in the world instead of
     * nothing: spawn the pet type's host mob and render the configured model on it. Returns
     * false — caller despawns as before — when the type has no rendered model (source-driven
     * types are handled by {@code spawnSource}), the provider is absent, or host/world are
     * unavailable. Config-based, so it works after the pet's live entity is already gone.
     */
    public static boolean releaseAsModeledWild(Pet pet, Location loc) {
        if (pet == null || loc == null || loc.getWorld() == null) {
            return false;
        }
        Optional<Resolved> r = resolve(pet);
        if (r.isEmpty()) {
            return false;
        }
        Class<? extends Mob> host = pet.getPetType().getBukkitEntityClass();
        if (host == null) {
            return false;
        }
        try {
            Mob wild = loc.getWorld().spawn(loc, host, CreatureSpawnEvent.SpawnReason.CUSTOM, m -> {});
            r.get().hook().renderOn(wild, r.get().modelId());
            return true;
        } catch (Throwable t) {
            MyPetApi.getLogger().warning("release: failed to leave a modeled wild mob for "
                    + pet.getPetType().name() + ": " + t.getMessage());
            return false;
        }
    }

    /**
     * Drives the sit animation state machine: on sit-down play {@code SIT}, then hand off to
     * the looping {@code SIT_LOOP} after the transition's length (re-checked, so a quick
     * sit/unsit toggle doesn't leave a stuck loop); on stand-up STOP the seated loop
     * (playing {@code UNSIT} alone only covers the loop, it doesn't remove it) and play
     * {@code UNSIT}. No-op without an active model.
     */
    public static void onSit(Pet pet, boolean sitting) {
        if (pet == null) {
            return;
        }
        Mob mob = pet.getBukkitEntity();
        if (mob == null) {
            return;
        }
        Optional<PetModelHook> hook = activeHook(pet);
        if (hook.isEmpty()) {
            return;
        }
        PetModelHook h = hook.get();
        if (!sitting) {
            // Stop the looping seated animation first — a loop is not removed by playing another
            // animation, so without this the model snaps back into sit_loop after unsit finishes.
            // Stop both SIT_LOOP and SIT (some rigs author "sit" itself as the held loop).
            try {
                h.stopAnimation(pet, animationName(pet, PetModelAnimation.SIT_LOOP));
                h.stopAnimation(pet, animationName(pet, PetModelAnimation.SIT));
            } catch (Throwable ignored) {
            }
            try {
                h.playAnimation(pet, animationName(pet, PetModelAnimation.UNSIT), PetModelAnimation.UNSIT.loops());
            } catch (Throwable ignored) {
            }
            return;
        }
        String sit = animationName(pet, PetModelAnimation.SIT);
        try {
            h.playAnimation(pet, sit, PetModelAnimation.SIT.loops());
        } catch (Throwable ignored) {
        }
        long delay;
        try {
            delay = Math.max(1L, (long) Math.ceil(h.animationLength(pet, sit).orElse(DEFAULT_ANIM_FALLBACK_TICKS)));
        } catch (Throwable t) {
            delay = DEFAULT_ANIM_FALLBACK_TICKS;
        }
        mob.getScheduler().runDelayed(MyPetApi.getPlugin(), task -> {
            if (pet.isSitting() && pet.getBukkitEntity() != null) {
                playAnimation(pet, PetModelAnimation.SIT_LOOP);
            }
        }, null, delay);
    }

    /**
     * Height (blocks above the host) at which to float this pet's model nametag: the type's
     * configured value if set, otherwise the host mob's height plus a small offset.
     */
    public static double nameHeight(Pet pet) {
        ModelConfig cfg = MODELS.get(pet.getPetType().name().toLowerCase(Locale.ROOT));
        if (cfg != null && cfg.nameHeight() != null) {
            return cfg.nameHeight();
        }
        Mob mob = pet.getBukkitEntity();
        return (mob != null ? mob.getHeight() : 1.0) + DEFAULT_NAME_OFFSET;
    }

    /**
     * Removes any model this pet may carry, from EVERY active renderer hook.
     * Not gated on current config (the config may have just changed), so a model
     * is always cleaned even after its mapping was removed. Each hook's detach is
     * a safe no-op when it holds no model for the pet.
     */
    public static void detachAll(Pet pet) {
        if (pet == null) {
            return;
        }
        for (PetModelHook hook : MyPetApi.getServiceManager().getServices(PetModelHook.class)) {
            try {
                hook.detach(pet);
            } catch (Throwable ignored) {
                // one provider failing must not block the others
            }
        }
    }

    /**
     * Brings the pet's rendered model in line with current config: clear any model
     * it currently carries (including one revived from a persisted entity snapshot),
     * then attach the configured model if one is mapped. Idempotent — safe to call
     * on every spawn and on config reload.
     */
    public static void reconcile(Pet pet) {
        if (pet == null || pet.getBukkitEntity() == null) {
            return;
        }
        detachAll(pet);
        resolve(pet).ifPresent(r -> r.hook().attach(pet, r.modelId()));
    }

    /**
     * Re-applies models to every currently-active pet. Call after a config reload
     * so model mapping changes take effect on already-spawned pets immediately
     * (mirrors how {@code /mypet reload skilltrees} re-applies to live pets).
     */
    public static void reapplyAll() {
        for (Pet pet : MyPetApi.getPetManager().getAllActivePets()) {
            Mob mob = pet.getBukkitEntity();
            if (mob == null) {
                continue;
            }
            // Dispatch each reconcile onto the pet's own region thread (Folia-safe): a reload can
            // touch pets owned by other regions, and one thrown detach/attach must not abort the rest.
            mob.getScheduler().run(MyPetApi.getPlugin(), task -> {
                try {
                    reconcile(pet);
                } catch (Throwable t) {
                    MyPetApi.getLogger().warning("reapply model failed for a pet: " + t.getMessage());
                }
            }, null);
        }
    }

    /**
     * Spawn-time model application. If the pet's type has a configured model, attach it
     * (covers fresh spawns; for a pet whose model rode along in its persisted snapshot the
     * attach is idempotent). If the type has NO configured model, a model may still have
     * been revived from the snapshot's entity PDC by the renderer plugin (BetterModel and
     * ModelEngine re-apply asynchronously), so a short-delayed {@link #detachAll} removes it
     * — after the revival window — so a type whose override was removed ends up bare.
     */
    private static void onSpawn(Pet pet) {
        // Attach immediately if configured so fresh spawns show their model promptly (idempotent
        // for a pet whose model rode along in its snapshot), and float the pet's name above it.
        resolve(pet).ifPresent(r -> {
            r.hook().attach(pet, r.modelId());
            PetModelNameTag.startForPet(pet);
        });
        // A source-driven type has no Model block to attach (its model rides in from the
        // source's snapshot/PDC), but it should still float a name above the host.
        if (resolve(pet).isEmpty() && isSourceDriven(pet)) {
            PetModelNameTag.startForPet(pet);
        }
        // Then reconcile after the renderer's async PDC-revival window: a model revived from
        // the snapshot that no longer matches config (changed or removed) is corrected.
        Mob mob = pet.getBukkitEntity();
        if (mob == null) {
            return;
        }
        mob.getScheduler().runDelayed(MyPetApi.getPlugin(),
                task -> reconcileAfterRevival(pet, MODEL_REVIVAL_MAX_RETRIES), null, MODEL_CLEANUP_DELAY_TICKS);
    }

    /**
     * Run a few ticks after spawn — once renderer plugins have re-applied any model from the
     * entity PDC — to make the rendered model match config exactly. Removes a model the type
     * shouldn't have, and replaces a stale/wrong model (e.g. after a model id was changed)
     * with the configured one. Leaves the common "already correct" case untouched, so it
     * never flashes.
     */
    private static void reconcileAfterRevival(Pet pet, int attemptsLeft) {
        Optional<Resolved> want = resolve(pet);
        Set<String> have = currentModels(pet);
        if (want.isEmpty()) {
            // A source-driven type with a model present keeps it: the model rode in from the
            // adopted source creature, not a Model block, so cleanup must not strip it.
            boolean keep = isSourceDriven(pet) && !have.isEmpty();
            if (!keep) {
                // Source-driven pet whose model hasn't shown up yet: the source may apply it on its
                // own (later) schedule, so retry rather than tearing down the nametag/spawn animation.
                if (isSourceDriven(pet) && have.isEmpty() && attemptsLeft > 0) {
                    Mob mob = pet.getBukkitEntity();
                    if (mob != null) {
                        mob.getScheduler().runDelayed(MyPetApi.getPlugin(),
                                task -> reconcileAfterRevival(pet, attemptsLeft - 1), null, MODEL_CLEANUP_DELAY_TICKS);
                        return;
                    }
                }
                if (!have.isEmpty()) {
                    detachAll(pet);
                }
                PetModelNameTag.stopForPet(pet); // type has no model -> remove its floating name too
                consumeFreshSpawn(pet);          // no model to animate -> clear the fresh flag
                return;
            }
            // keep: source-driven model confirmed present -> fall through to the spawn animation
        } else {
            String wanted = want.get().modelId();
            boolean exactlyWanted = have.size() == 1 && have.iterator().next().equalsIgnoreCase(wanted);
            if (!exactlyWanted) {
                detachAll(pet);
                want.get().hook().attach(pet, wanted);
            }
            // rendered model now confirmed present -> fall through to the spawn animation
        }
        // A model is confirmed on the host. Play the spawn animation once, on a genuine
        // fresh summon only (not chunk-reload/relog snapshot restores).
        if (consumeFreshSpawn(pet)) {
            playAnimation(pet, PetModelAnimation.SPAWN);
        }
    }

    /** Union of the model ids every active renderer hook currently renders on the pet's host. */
    public static Set<String> currentModels(Pet pet) {
        if (pet == null) {
            return Set.of();
        }
        Set<String> all = new HashSet<>();
        for (PetModelHook hook : MyPetApi.getServiceManager().getServices(PetModelHook.class)) {
            try {
                all.addAll(hook.currentModels(pet));
            } catch (Throwable ignored) {
                // one provider failing must not block the others
            }
        }
        return all;
    }

    /**
     * Registers the single global spawn/despawn lifecycle hook. The model itself is
     * intentionally left untouched on despawn: it stays on the host so it disappears
     * together with the entity (no mid-despawn flash) and rides along in the persisted
     * snapshot for a seamless respawn. Despawn only stops the floating nametag and drops
     * any fresh-spawn marker still pending, so it can't outlive a host that's removed
     * before {@link #reconcileAfterRevival} gets to consume it. Idempotent.
     */
    public static synchronized void init() {
        if (initialised) {
            return;
        }
        PetLifecycleHook.global(PetModelService::onSpawn, PetModelService::onDespawn);
        initialised = true;
    }

    private static void onDespawn(Pet pet) {
        PetModelNameTag.stopForPet(pet);
        consumeFreshSpawn(pet); // drop a pending fresh-spawn marker so it can't leak past this pet's despawn
    }
}
