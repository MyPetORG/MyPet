package de.Keyle.MyPet.skill.skills;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.skill.skilltree.Skill;

import java.util.List;

/**
 * Registers MyPet's bundled skill implementations with {@link de.Keyle.MyPet.api.skill.SkillManager}.
 *
 * <p>Each entry in {@link #SKILLS} is a concrete skill class that participates in the skill
 * resolution pipeline used by skilltrees. The order of registration is preserved on disk
 * and reflects the canonical "core skill" ordering — adding a new built-in skill should
 * append to {@link #SKILLS} rather than insert mid-list, so existing skilltree files
 * continue to resolve in the same order.</p>
 *
 * <p>Invoked once during plugin enable, after the skill manager is initialized but before
 * skilltrees are loaded from disk.</p>
 */
public final class BuiltInSkills {

    private static final List<Class<? extends Skill>> SKILLS = List.of(
            BackpackImpl.class,
            HealImpl.class,
            PickupImpl.class,
            BehaviorImpl.class,
            DamageImpl.class,
            ControlImpl.class,
            LifeImpl.class,
            PoisonImpl.class,
            RideImpl.class,
            ThornsImpl.class,
            FireImpl.class,
            BeaconImpl.class,
            WitherImpl.class,
            LightningImpl.class,
            SlowImpl.class,
            KnockbackImpl.class,
            RangedImpl.class,
            SprintImpl.class,
            StompImpl.class,
            ShieldImpl.class,
            BleedImpl.class
    );

    private BuiltInSkills() {
    }

    /**
     * Registers every built-in skill class with the active {@link de.Keyle.MyPet.api.skill.SkillManager}.
     * Intended to be called exactly once per plugin enable; the manager logs (but does not throw)
     * on duplicate registration.
     */
    public static void register() {
        for (Class<? extends Skill> skill : SKILLS) {
            MyPetApi.getSkillManager().registerSkill(skill);
        }
    }
}
