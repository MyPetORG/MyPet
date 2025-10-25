package de.Keyle.MyPet.compat.v1_20_R4.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.animal.CatVariant;
import net.minecraft.world.entity.animal.FrogVariant;
import org.bukkit.entity.Frog;

/**
 * Converts numerical variants into fancy new variants
 */
public class VariantConverter {
    private static enum CatVariants {
        TABBY((CatVariant) BuiltInRegistries.CAT_VARIANT.getOrThrow(CatVariant.TABBY)),
        BLACK((CatVariant) BuiltInRegistries.CAT_VARIANT.getOrThrow(CatVariant.BLACK)),
        RED((CatVariant) BuiltInRegistries.CAT_VARIANT.getOrThrow(CatVariant.RED)),
        SIAMESE((CatVariant) BuiltInRegistries.CAT_VARIANT.getOrThrow(CatVariant.SIAMESE)),
        BRITISH_SHORTHAIR((CatVariant) BuiltInRegistries.CAT_VARIANT.getOrThrow(CatVariant.BRITISH_SHORTHAIR)),
        CALICO((CatVariant) BuiltInRegistries.CAT_VARIANT.getOrThrow(CatVariant.CALICO)),
        PERSIAN((CatVariant) BuiltInRegistries.CAT_VARIANT.getOrThrow(CatVariant.PERSIAN)),
        RAGDOLL((CatVariant) BuiltInRegistries.CAT_VARIANT.getOrThrow(CatVariant.RAGDOLL)),
        WHITE((CatVariant) BuiltInRegistries.CAT_VARIANT.getOrThrow(CatVariant.WHITE)),
        JELLIE((CatVariant) BuiltInRegistries.CAT_VARIANT.getOrThrow(CatVariant.JELLIE)),
        ALL_BLACK((CatVariant) BuiltInRegistries.CAT_VARIANT.getOrThrow(CatVariant.ALL_BLACK));

        CatVariant variant;
        CatVariants(CatVariant cV) {
            this.variant = cV;
        }
    }

    private static enum FrogVariants {
        TEMPERATE(BuiltInRegistries.FROG_VARIANT.getOrThrow(FrogVariant.TEMPERATE), Frog.Variant.TEMPERATE),
        WARM(BuiltInRegistries.FROG_VARIANT.getOrThrow(FrogVariant.WARM), Frog.Variant.WARM),
        COLD(BuiltInRegistries.FROG_VARIANT.getOrThrow(FrogVariant.COLD), Frog.Variant.COLD);

        FrogVariant variant;
        Frog.Variant bukkitVariant;
        FrogVariants(FrogVariant fV, Frog.Variant fV2) {
            this.variant = fV;
            this.bukkitVariant = fV2;
        }
    }

    public static CatVariant convertCatVariant(int varId) {
        return CatVariants.values()[varId].variant;
    }

    public static FrogVariant convertFrogVariant(int varId) {
        return FrogVariants.values()[varId].variant;
    }
    public static Frog.Variant getBukkitFrogVariant(int varId) {
        return FrogVariants.values()[varId].bukkitVariant;
    }
}
