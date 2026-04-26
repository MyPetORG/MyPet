package de.Keyle.MyPet.entity.leashing;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.leashing.LeashFlag;
import de.Keyle.MyPet.api.entity.leashing.LeashFlagManager;

import java.util.List;
import java.util.function.Supplier;

/**
 * Registers MyPet's bundled leash-flag implementations with the active
 * {@link LeashFlagManager}.
 *
 * <p>A leash flag is a precondition (e.g. "pet must be a baby", "pet has low HP", "world
 * is allow-listed") that gates whether a player can leash a particular wild mob into a pet.
 * Each flag is instantiated lazily via the {@link Supplier} table so the same registrar can
 * be re-invoked across reloads without leaking shared state across runs.</p>
 *
 * <p>Invoked once during plugin enable, after the leash-flag manager service has been
 * activated.</p>
 */
public final class BuiltInLeashFlags {

    private static final List<Supplier<LeashFlag>> FLAGS = List.of(
            AdultFlag::new,
            AngryFlag::new,
            BabyFlag::new,
            BelowHpFlag::new,
            CanBreedFlag::new,
            ChanceFlag::new,
            ImpossibleFlag::new,
            LowHpFlag::new,
            ScreamingFlag::new,
            SizeFlag::new,
            TamedFlag::new,
            UserCreatedFlag::new,
            WildFlag::new,
            WorldFlag::new,
            PermissionFlag::new,
            HeartLinkedFlag::new
    );

    private BuiltInLeashFlags() {
    }

    /**
     * Constructs a fresh instance of each built-in leash flag and registers it with the
     * active {@link LeashFlagManager}.
     */
    public static void register() {
        LeashFlagManager manager = MyPetApi.getLeashFlagManager();
        for (Supplier<LeashFlag> flag : FLAGS) {
            manager.registerLeashFlag(flag.get());
        }
    }
}
