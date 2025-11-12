package de.Keyle.MyPet.compat.v1_20_R3.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.animal.CatVariant;
import net.minecraft.world.entity.animal.FrogVariant;

/**
 * Converts numerical variants into fancy new variants
 */
public class VariantConverter {
    private enum CatVariants {
        TABBY(BuiltInRegistries.CAT_VARIANT.getOrThrow(CatVariant.TABBY)),
        BLACK(BuiltInRegistries.CAT_VARIANT.getOrThrow(CatVariant.BLACK)),
        RED(BuiltInRegistries.CAT_VARIANT.getOrThrow(CatVariant.RED)),
        SIAMESE(BuiltInRegistries.CAT_VARIANT.getOrThrow(CatVariant.SIAMESE)),
        BRITISH_SHORTHAIR(BuiltInRegistries.CAT_VARIANT.getOrThrow(CatVariant.BRITISH_SHORTHAIR)),
        CALICO(BuiltInRegistries.CAT_VARIANT.getOrThrow(CatVariant.CALICO)),
        PERSIAN(BuiltInRegistries.CAT_VARIANT.getOrThrow(CatVariant.PERSIAN)),
        RAGDOLL(BuiltInRegistries.CAT_VARIANT.getOrThrow(CatVariant.RAGDOLL)),
        WHITE(BuiltInRegistries.CAT_VARIANT.getOrThrow(CatVariant.WHITE)),
        JELLIE(BuiltInRegistries.CAT_VARIANT.getOrThrow(CatVariant.JELLIE)),
        ALL_BLACK(BuiltInRegistries.CAT_VARIANT.getOrThrow(CatVariant.ALL_BLACK));

        CatVariant variant;
        CatVariants(CatVariant cV) {
            this.variant = cV;
        }
    }

    private enum FrogVariants {
        TEMPERATE(FrogVariant.TEMPERATE),
        WARM(FrogVariant.WARM),
        COLD(FrogVariant.COLD);

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
