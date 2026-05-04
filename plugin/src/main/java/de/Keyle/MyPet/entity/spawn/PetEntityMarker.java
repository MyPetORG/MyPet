package de.Keyle.MyPet.entity.spawn;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.ComplexEntityPart;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.persistence.PersistentDataType;

/**
 * Manages the PDC marker used to tag real Bukkit mobs as MyPet pets.
 * <p>
 * The marker is a single byte under {@code mypet:pet}. Listeners and hook integrations
 * use this marker as a fast predicate instead of an instanceof check on the (now deleted)
 * {@code MyPetBukkitEntity} interface.
 */
public final class PetEntityMarker {

    public static final NamespacedKey KEY = new NamespacedKey("mypet", "pet");

    private PetEntityMarker() {
    }

    public static void mark(Mob mob) {
        mob.getPersistentDataContainer().set(KEY, PersistentDataType.BYTE, (byte) 1);
    }

    public static boolean isMarked(Entity entity) {
        // Sub-parts of a ComplexLivingEntity (EnderDragon head/neck/body/tail/
        // wings) carry their own entity ID server-side. PlayerInteractEntityEvent
        // and friends fire with the part as the clicked entity, but the PDC
        // marker is set on the parent only — resolve so callers don't need to
        // know about parts.
        if (entity instanceof ComplexEntityPart part) {
            entity = part.getParent();
        }
        return entity != null && entity.getPersistentDataContainer().has(KEY, PersistentDataType.BYTE);
    }
}
