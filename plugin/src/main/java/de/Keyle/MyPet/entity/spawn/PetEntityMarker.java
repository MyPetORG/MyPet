package de.Keyle.MyPet.entity.spawn;

import org.bukkit.NamespacedKey;
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
        return entity != null && entity.getPersistentDataContainer().has(KEY, PersistentDataType.BYTE);
    }
}
