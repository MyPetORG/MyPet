package de.Keyle.MyPet.compat.v1_21_R2.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.animal.CatVariant;
import net.minecraft.world.entity.animal.FrogVariant;

/**
 * Converts numerical variants into fancy new variants
 */
public class VariantConverter {
    private static enum CatVariants {
        TABBY((CatVariant) BuiltInRegistries.CAT_VARIANT.getOrThrow(CatVariant.TABBY).value()),
        BLACK((CatVariant) BuiltInRegistries.CAT_VARIANT.getOrThrow(CatVariant.BLACK).value()),
        RED((CatVariant) BuiltInRegistries.CAT_VARIANT.getOrThrow(CatVariant.RED).value()),
        SIAMESE((CatVariant) BuiltInRegistries.CAT_VARIANT.getOrThrow(CatVariant.SIAMESE).value()),
        BRITISH_SHORTHAIR((CatVariant) BuiltInRegistries.CAT_VARIANT.getOrThrow(CatVariant.BRITISH_SHORTHAIR).value()),
        CALICO((CatVariant) BuiltInRegistries.CAT_VARIANT.getOrThrow(CatVariant.CALICO).value()),
        PERSIAN((CatVariant) BuiltInRegistries.CAT_VARIANT.getOrThrow(CatVariant.PERSIAN).value()),
        RAGDOLL((CatVariant) BuiltInRegistries.CAT_VARIANT.getOrThrow(CatVariant.RAGDOLL).value()),
        WHITE((CatVariant) BuiltInRegistries.CAT_VARIANT.getOrThrow(CatVariant.WHITE).value()),
        JELLIE((CatVariant) BuiltInRegistries.CAT_VARIANT.getOrThrow(CatVariant.JELLIE).value()),
        ALL_BLACK((CatVariant) BuiltInRegistries.CAT_VARIANT.getOrThrow(CatVariant.ALL_BLACK).value());

        CatVariant variant;
        CatVariants(CatVariant cV) {
            this.variant = cV;
        }
    }

    private static enum FrogVariants {
        TEMPERATE(BuiltInRegistries.FROG_VARIANT.getOrThrow(FrogVariant.TEMPERATE).value()),
        WARM(BuiltInRegistries.FROG_VARIANT.getOrThrow(FrogVariant.WARM).value()),
        COLD(BuiltInRegistries.FROG_VARIANT.getOrThrow(FrogVariant.COLD).value());

        FrogVariant variant;
        FrogVariants(FrogVariant fV) {
            this.variant = fV;
        }
    }

    public static CatVariant convertCatVariant(int varId) {
        return CatVariants.values()[varId].variant;
    }

    public static FrogVariant convertFrogVariant(int varId) {
        return FrogVariants.values()[varId].variant;
    }
}
