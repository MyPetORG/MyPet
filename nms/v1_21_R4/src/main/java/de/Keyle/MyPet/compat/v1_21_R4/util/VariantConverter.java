package de.Keyle.MyPet.compat.v1_21_R4.util;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.animal.frog.FrogVariant;
import net.minecraft.world.entity.animal.frog.FrogVariants;
import org.bukkit.craftbukkit.v1_21_R4.CraftRegistry;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Frog;
import org.bukkit.entity.Pig;

/**
 * Converts numerical variants into fancy new variants
 */
public class VariantConverter {

    public static final Registry<FrogVariant> FROG_REGISTRY = CraftRegistry.getMinecraftRegistry(Registries.FROG_VARIANT);
    public static final Registry<CatVariant> CAT_REGISTRY = CraftRegistry.getMinecraftRegistry(Registries.CAT_VARIANT);
    public static final Registry<PigVariant> PIG_REGISTRY = CraftRegistry.getMinecraftRegistry(Registries.PIG_VARIANT);
    public static final Registry<ChickenVariant> CHICKEN_REGISTRY = CraftRegistry.getMinecraftRegistry(Registries.CHICKEN_VARIANT);
    public static final Registry<CowVariant> COW_REGISTRY = CraftRegistry.getMinecraftRegistry(Registries.COW_VARIANT);

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

    private static enum ConverterPigVariants {
        TEMPERATE(PIG_REGISTRY.getValue(PigVariants.TEMPERATE), Pig.Variant.TEMPERATE),
        WARM(PIG_REGISTRY.getValue(PigVariants.WARM), Pig.Variant.WARM),
        COLD(PIG_REGISTRY.getValue(PigVariants.COLD), Pig.Variant.COLD);

        PigVariant variant;
        Pig.Variant bukkitVariant;
        ConverterPigVariants(PigVariant pV, Pig.Variant pV2) {
            this.variant = pV;
            this.bukkitVariant = pV2;
        }
    }

    private static enum ConverterChickenVariants {
        TEMPERATE(CHICKEN_REGISTRY.getValue(ChickenVariants.TEMPERATE), org.bukkit.entity.Chicken.Variant.TEMPERATE),
        WARM(CHICKEN_REGISTRY.getValue(ChickenVariants.WARM), org.bukkit.entity.Chicken.Variant.WARM),
        COLD(CHICKEN_REGISTRY.getValue(ChickenVariants.COLD), org.bukkit.entity.Chicken.Variant.COLD);

        ChickenVariant variant;
        org.bukkit.entity.Chicken.Variant bukkitVariant;
        ConverterChickenVariants(ChickenVariant cV, org.bukkit.entity.Chicken.Variant cV2) {
            this.variant = cV;
            this.bukkitVariant = cV2;
        }
    }

    private static enum ConverterCowVariants {
        TEMPERATE(COW_REGISTRY.getValue(CowVariants.TEMPERATE), org.bukkit.entity.Cow.Variant.TEMPERATE),
        WARM(COW_REGISTRY.getValue(CowVariants.WARM), org.bukkit.entity.Cow.Variant.WARM),
        COLD(COW_REGISTRY.getValue(CowVariants.COLD), org.bukkit.entity.Cow.Variant.COLD);

        CowVariant variant;
        org.bukkit.entity.Cow.Variant bukkitVariant;
        ConverterCowVariants(CowVariant cV, Cow.Variant cV2) {
            this.variant = cV;
            this.bukkitVariant = cV2;
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

    public static PigVariant convertPigVariant(String varString) {
        String key = varString == null ? "TEMPERATE" : varString.trim().toUpperCase();
        return ConverterPigVariants.valueOf(key).variant;
    }

    public static Pig.Variant getBukkitPigVariant(String varString) {
        return ConverterPigVariants.valueOf(varString.trim().toUpperCase()).bukkitVariant;
    }

    public static ChickenVariant convertChickenVariant(String varString) {
        String key = varString == null ? "TEMPERATE" : varString.trim().toUpperCase();
        return ConverterChickenVariants.valueOf(key).variant;
    }

    public static Chicken.Variant getBukkitChickenVariant(String varString) {
        return ConverterChickenVariants.valueOf(varString.trim().toUpperCase()).bukkitVariant;
    }

    public static CowVariant convertCowVariant(String varString) {
        String key = varString == null ? "TEMPERATE" : varString.trim().toUpperCase();
        return ConverterCowVariants.valueOf(key).variant;
    }

    public static Cow.Variant getBukkitCowVariant(String varString) {
        return ConverterCowVariants.valueOf(varString.trim().toUpperCase()).bukkitVariant;
    }
}
