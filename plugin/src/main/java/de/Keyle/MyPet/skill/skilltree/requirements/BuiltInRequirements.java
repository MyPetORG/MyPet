package de.Keyle.MyPet.skill.skilltree.requirements;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.skill.skilltree.SkilltreeManager;
import de.Keyle.MyPet.api.skill.skilltree.requirements.Requirement;

import java.util.List;
import java.util.function.Supplier;

/**
 * Registers MyPet's bundled skilltree-requirement implementations with the active
 * {@link SkilltreeManager}.
 *
 * <p>A requirement gates which skilltrees a pet may select (e.g. "owner has permission X",
 * "pet has reached level Y", "another skilltree is/isn't already selected"). Each requirement
 * type is registered as a fresh instance so that per-tree configuration parsed at load time
 * doesn't bleed between runs.</p>
 *
 * <p>Invoked once during plugin enable, after the skilltree manager service has been activated
 * and before skilltree files are loaded from disk.</p>
 */
public final class BuiltInRequirements {

    private static final List<Supplier<Requirement>> REQUIREMENTS = List.of(
            NoSkilltreeRequirement::new,
            PermissionRequirement::new,
            PetLevelRequirement::new,
            SkilltreeRequirement::new
    );

    private BuiltInRequirements() {
    }

    /**
     * Constructs a fresh instance of each built-in requirement and registers it with the
     * active {@link SkilltreeManager}.
     */
    public static void register() {
        SkilltreeManager manager = MyPetApi.getSkilltreeManager();
        for (Supplier<Requirement> req : REQUIREMENTS) {
            manager.registerRequirement(req.get());
        }
    }
}
