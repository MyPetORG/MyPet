package de.Keyle.MyPet.api.entity;

import org.bukkit.Material;

import java.util.Set;

/**
 * Marker for pet types whose underlying vanilla mob has a periodic AI-tick
 * item drop (chicken eggs, armadillo scutes, sniffer seeds, goat horns,
 * panda baby slimeballs, ...). In v4 pets are real vanilla mobs, so these
 * drops happen for free on every tame pet. Implementers expose two pieces of
 * metadata that {@code PetDropListener} uses to decide whether to cancel the
 * drop event:
 *
 * <ol>
 *   <li>{@link #naturalDropMaterials()} — the set of vanilla {@link Material}s
 *       this pet may periodically drop. Most pets drop a single material;
 *       sniffers drop two (torchflower seed + pitcher pod).</li>
 *   <li>{@link #isNaturalDropSuppressed()} — reads the implementer's own
 *       per-pet config flag (e.g. {@code Configuration.MyPet.Chicken.CAN_LAY_EGGS}).
 *       Returns {@code true} when the admin has disabled the drop.</li>
 * </ol>
 *
 * <p>Per-pet flag names stay semantically rich ({@code CanLayEggs},
 * {@code CanShedScute}, {@code CanDigSeeds}) because each implementer reads
 * its own {@code Configuration.MyPet.<Type>.*} field. The listener never
 * names them.
 */
public interface MyPetNaturalDrop extends MyPet {

    Set<Material> naturalDropMaterials();

    boolean isNaturalDropSuppressed();
}
