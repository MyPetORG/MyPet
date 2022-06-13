package de.Keyle.MyPet.compat.v1_19_R1.util;

import net.minecraft.world.entity.animal.CatVariant;
import net.minecraft.world.entity.animal.FrogVariant;
import org.bukkit.Bukkit;

/**
 * Converts numerical variants into fancy new variants
 */
public class VariantConverter {
    private static enum CatVariants {
        TABBY(CatVariant.TABBY),
        BLACK(CatVariant.BLACK),
        RED(CatVariant.RED),
        SIAMESE(CatVariant.SIAMESE),
        BRITISH_SHORTHAIR(CatVariant.BRITISH_SHORTHAIR),
        CALICO(CatVariant.CALICO),
        PERSIAN(CatVariant.PERSIAN),
        RAGDOLL(CatVariant.RAGDOLL),
        WHITE(CatVariant.WHITE),
        JELLIE(CatVariant.JELLIE),
        ALL_BLACK(CatVariant.ALL_BLACK);

        CatVariant variant;
        CatVariants(CatVariant cV) {
            this.variant = cV;
        }
    }

    private static enum FrogVariants {
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
