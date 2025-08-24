package de.Keyle.MyPet.compat.v1_21_R5.util;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.animal.CatVariant;
import net.minecraft.world.entity.animal.CatVariants;
import net.minecraft.world.entity.animal.frog.FrogVariant;
import net.minecraft.world.entity.animal.frog.FrogVariants;
import org.bukkit.craftbukkit.v1_21_R5.CraftRegistry;
import org.bukkit.entity.Frog;

/**
 * Converts numerical variants into fancy new variants
 */
public class VariantConverter {

    public static final Registry<FrogVariant> FROG_REGISTRY = CraftRegistry.getMinecraftRegistry(Registries.FROG_VARIANT);
    public static final Registry<CatVariant> CAT_REGISTRY = CraftRegistry.getMinecraftRegistry(Registries.CAT_VARIANT);

    private static enum ConverterCatVariants {
        TABBY(CAT_REGISTRY.getValue(CatVariants.TABBY)),
        BLACK(CAT_REGISTRY.getValue(CatVariants.BLACK)),
        RED(CAT_REGISTRY.getValue(CatVariants.RED)),
        SIAMESE(CAT_REGISTRY.getValue(CatVariants.SIAMESE)),
        BRITISH_SHORTHAIR(CAT_REGISTRY.getValue(CatVariants.BRITISH_SHORTHAIR)),
        CALICO(CAT_REGISTRY.getValue(CatVariants.CALICO)),
        PERSIAN(CAT_REGISTRY.getValue(CatVariants.PERSIAN)),
        RAGDOLL(CAT_REGISTRY.getValue(CatVariants.RAGDOLL)),
        WHITE(CAT_REGISTRY.getValue(CatVariants.WHITE)),
        JELLIE(CAT_REGISTRY.getValue(CatVariants.JELLIE)),
        ALL_BLACK(CAT_REGISTRY.getValue(CatVariants.ALL_BLACK));

        CatVariant variant;
        ConverterCatVariants(CatVariant cV) {
            this.variant = cV;
        }
    }

    private static enum ConverterFrogVariants {
        TEMPERATE(FROG_REGISTRY.getValue(FrogVariants.TEMPERATE), Frog.Variant.TEMPERATE),
        WARM(FROG_REGISTRY.getValue(FrogVariants.WARM), Frog.Variant.WARM),
        COLD(FROG_REGISTRY.getValue(FrogVariants.COLD), Frog.Variant.COLD);

        FrogVariant variant;
        Frog.Variant bukkitVariant;
        ConverterFrogVariants(FrogVariant fV, Frog.Variant fV2) {
            this.variant = fV;
            this.bukkitVariant = fV2;
        }
    }

    public static CatVariant convertCatVariant(int varId) {
        return ConverterCatVariants.values()[varId].variant;
    }

    public static FrogVariant convertFrogVariant(int varId) {
        return ConverterFrogVariants.values()[varId].variant;
    }
    public static Frog.Variant getBukkitFrogVariant(int varId) {
        return ConverterFrogVariants.values()[varId].bukkitVariant;
    }
}
