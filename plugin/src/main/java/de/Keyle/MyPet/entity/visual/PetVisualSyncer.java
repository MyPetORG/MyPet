package de.Keyle.MyPet.entity.visual;

import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPetBaby;
import de.Keyle.MyPet.entity.types.*;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.*;


/**
 * Writes a {@link MyPet} domain object's state onto its real vanilla Bukkit
 * {@link Mob} via Bukkit API setters. Called both pre-spawn from inside
 * {@code VanillaMobSpawner.configureMob()} (so the initial state lands in the
 * spawn packet) and post-spawn from {@code MyPet#updateVisuals()} (when
 * per-type setters mutate state, e.g. sheep wool colour change via dye).
 */
public final class PetVisualSyncer {

    private PetVisualSyncer() {
    }

    /**
     * Synchronises the MyPet domain state onto the Bukkit mob. Called from
     * {@code VanillaMobSpawner.configureMob} pre-spawn and from
     * {@code plugin/entity/MyPet.updateVisuals} post-spawn.
     */
    public static void sync(MyPet pet, Mob mob) {
        sync(pet, mob, true);
    }

    /**
     * Overload that allows the caller to skip the tameable/owner application.
     * Used by {@code VanillaMobSpawner#releaseToWild} when respawning the pet
     * as a wild vanilla mob — the mob is no longer owned by the player, so
     * {@code setTamed/setOwner} must not be called.
     *
     * @param applyTameable when {@code false}, skip the {@code Tameable} block
     *                      so the synced mob remains un-tamed. The sit pose
     *                      is also suppressed since a released mob should not
     *                      spawn in the sitting animation.
     */
    public static void sync(MyPet pet, Mob mob, boolean applyTameable) {
        if (pet == null || mob == null) return;

        syncUniversal(pet, mob, applyTameable);
        syncType(pet, mob);
    }

    /**
     * Universal sync: baby flag, sitting pose, tamed owner — applicable to
     * any pet type that implements the corresponding Bukkit interface.
     */
    private static void syncUniversal(MyPet pet, Mob mob, boolean applyTameable) {
        if (pet instanceof MyPetBaby baby && mob instanceof Ageable ageable) {
            if (baby.isBaby()) {
                ageable.setBaby();
            } else {
                ageable.setAdult();
            }
        }
        if (mob instanceof Sittable sittable) {
            sittable.setSitting(applyTameable && pet.isSitting());
        }
        if (applyTameable && mob instanceof Tameable tameable) {
            tameable.setTamed(true);
            if (pet.getOwner() != null && pet.getOwner().getPlayer() != null) {
                tameable.setOwner(pet.getOwner().getPlayer());
            }
        }
    }

    /**
     * Per-type visual sync. Each branch is wrapped in a try/catch so that an
     * unexpected API shape on a single type (e.g. a Bukkit enum renamed
     * between Minecraft versions) doesn't crash the whole sync pass.
     */
    private static void syncType(MyPet pet, Mob mob) {
        // ─── Tier 1: simple flags / sizes ───

        if (pet instanceof MyPig pig && mob instanceof Pig bukkitPig) {
            try {
                bukkitPig.setSaddle(pig.getSaddle() != null);
            } catch (Throwable ignored) {}
        }
        if (pet instanceof MySnowGolem sg && mob instanceof Snowman snowman) {
            try {
                snowman.setDerp(sg.isSheared());
            } catch (Throwable ignored) {}
        }
        if (pet instanceof MySlime slimePet && mob instanceof Slime slime) {
            try {
                slime.setSize(Math.max(1, slimePet.getSize()));
            } catch (Throwable ignored) {}
        }
        if (pet instanceof MyMagmaCube mc && mob instanceof MagmaCube magmaCube) {
            try {
                magmaCube.setSize(Math.max(1, mc.getSize()));
            } catch (Throwable ignored) {}
        }
        if (pet instanceof MyStrider strider && mob instanceof Strider bukkitStrider) {
            try {
                bukkitStrider.setSaddle(strider.getSaddle() != null);
            } catch (Throwable ignored) {}
        }
        if (pet instanceof MyPufferfish pf && mob instanceof PufferFish pufferFish) {
            try {
                pufferFish.setPuffState(pf.getPuffState());
            } catch (Throwable ignored) {}
        }
        if (pet instanceof MyCreeper cp && mob instanceof Creeper creeper) {
            try {
                creeper.setPowered(cp.isPowered());
            } catch (Throwable ignored) {}
        }
        if (pet instanceof MyPhantom ph && mob instanceof Phantom phantom) {
            try {
                phantom.setSize(Math.max(1, ph.getSize()));
            } catch (Throwable ignored) {}
        }
        if (pet instanceof MyBee beePet && mob instanceof Bee bee) {
            try {
                bee.setHasNectar(beePet.isHasNectar());
                bee.setHasStung(beePet.isHasStung());
                bee.setAnger(beePet.isAngry() ? 400 : 0);
            } catch (Throwable ignored) {}
        }

        // ─── Tier 2: colours / variants ───

        if (pet instanceof MySheep sheepPet && mob instanceof Sheep sheep) {
            try {
                sheep.setColor(sheepPet.getColor());
                sheep.setSheared(sheepPet.isSheared());
            } catch (Throwable ignored) {}
        }
        if (pet instanceof MyWolf wolfPet && mob instanceof Wolf wolf) {
            try {
                wolf.setCollarColor(wolfPet.getCollarColor());
                wolf.setAngry(wolfPet.isAngry());
                wolf.setTamed(wolfPet.isTamed());
                // Wolf variant API: interface-based Keyed type since 1.21; resolve via registry.
                try {
                    NamespacedKey variantKey = NamespacedKey.minecraft(
                            wolfPet.getVariant().toLowerCase());
                    Wolf.Variant variant = Registry.WOLF_VARIANT.get(variantKey);
                    if (variant != null) {
                        wolf.setVariant(variant);
                    }
                } catch (Throwable ignored) {}
            } catch (Throwable ignored) {}
        }
        if (pet instanceof MyCat catPet && mob instanceof Cat cat) {
            try {
                cat.setCatType(catPet.getCatType());
                cat.setCollarColor(catPet.getCollarColor());
                cat.setTamed(catPet.isTamed());
            } catch (Throwable ignored) {}
        }
        if (pet instanceof MyParrot parrotPet && mob instanceof Parrot parrot) {
            try {
                // Use the drift-safe name-based resolver on MyParrot rather
                // than looking up Parrot.Variant by ordinal.
                Parrot.Variant v = parrotPet.resolveBukkitVariant();
                if (v != null) {
                    parrot.setVariant(v);
                }
            } catch (Throwable ignored) {}
        }
        if (pet instanceof MyRabbit rabbitPet && mob instanceof Rabbit rabbit) {
            try {
                Rabbit.Type t = rabbitPet.getVariant();
                if (t != null) {
                    rabbit.setRabbitType(t);
                }
            } catch (Throwable ignored) {}
        }
        if (pet instanceof MyAxolotl ax && mob instanceof Axolotl axolotl) {
            try {
                Axolotl.Variant v = ax.resolveBukkitVariant();
                if (v != null) {
                    axolotl.setVariant(v);
                }
            } catch (Throwable ignored) {}
        }
        if (pet instanceof MyFrog frogPet && mob instanceof Frog frog) {
            try {
                Frog.Variant v = frogPet.resolveBukkitVariant();
                if (v != null) {
                    frog.setVariant(v);
                }
            } catch (Throwable ignored) {}
        }
        if (pet instanceof MyTropicalFish tfPet && mob instanceof TropicalFish tf) {
            try {
                // TropicalFish pattern-colour encoding is a 32-bit int; delegate
                // to the stored variant as the pattern index for now.
                TropicalFish.Pattern[] values = TropicalFish.Pattern.values();
                int idx = tfPet.getVariant() % values.length;
                if (idx >= 0) {
                    tf.setPattern(values[idx]);
                }
            } catch (Throwable ignored) {}
        }
        if (pet instanceof MyMooshroom mushroomPet && mob instanceof MushroomCow mushroomCow) {
            try {
                MushroomCow.Variant v = mushroomPet.getType();
                if (v != null) {
                    mushroomCow.setVariant(v);
                }
            } catch (Throwable ignored) {}
        }

        // ─── Tier 3: complex state ───

        if (pet instanceof MyHorse horsePet && mob instanceof Horse horse) {
            try {
                Horse.Color color = horsePet.resolveColor();
                if (color != null) {
                    horse.setColor(color);
                }
                Horse.Style style = horsePet.resolveStyle();
                if (style != null) {
                    horse.setStyle(style);
                }
                if (horsePet.getSaddle() != null) {
                    horse.getInventory().setSaddle(horsePet.getSaddle());
                }
                if (horsePet.getArmor() != null) {
                    horse.getInventory().setArmor(horsePet.getArmor());
                }
            } catch (Throwable ignored) {}
        }
        if (pet instanceof MyLlama llamaPet && mob instanceof Llama llama) {
            try {
                Llama.Color color = llamaPet.resolveColor();
                if (color != null) {
                    llama.setColor(color);
                }
                llama.setCarryingChest(llamaPet.getChest() != null);
                if (llamaPet.getDecor() != null) {
                    llama.getInventory().setDecor(llamaPet.getDecor());
                }
            } catch (Throwable ignored) {}
        }
        if (pet instanceof MyTraderLlama tlPet && mob instanceof TraderLlama traderLlama) {
            try {
                Llama.Color color = tlPet.resolveColor();
                if (color != null) {
                    traderLlama.setColor(color);
                }
                traderLlama.setCarryingChest(tlPet.getChest() != null);
                if (tlPet.getDecor() != null) {
                    traderLlama.getInventory().setDecor(tlPet.getDecor());
                }
            } catch (Throwable ignored) {}
        }
        if (pet instanceof MyDonkey donkeyPet && mob instanceof Donkey donkey) {
            try {
                donkey.setCarryingChest(donkeyPet.getChest() != null);
                if (donkeyPet.getSaddle() != null) {
                    donkey.getInventory().setSaddle(donkeyPet.getSaddle());
                }
            } catch (Throwable ignored) {}
        }
        if (pet instanceof MyMule mulePet && mob instanceof Mule mule) {
            try {
                mule.setCarryingChest(mulePet.getChest() != null);
                if (mulePet.getSaddle() != null) {
                    mule.getInventory().setSaddle(mulePet.getSaddle());
                }
            } catch (Throwable ignored) {}
        }
        if (pet instanceof MyCamel camelPet && mob instanceof Camel camel) {
            try {
                if (camelPet.getSaddle() != null) {
                    camel.getInventory().setSaddle(camelPet.getSaddle());
                }
            } catch (Throwable ignored) {}
        }
        if (pet instanceof MySkeletonHorse shPet && mob instanceof SkeletonHorse sh) {
            try {
                if (shPet.getSaddle() != null) {
                    sh.getInventory().setSaddle(shPet.getSaddle());
                }
            } catch (Throwable ignored) {}
        }
        if (pet instanceof MyZombieHorse zhPet && mob instanceof ZombieHorse zh) {
            try {
                if (zhPet.getSaddle() != null) {
                    zh.getInventory().setSaddle(zhPet.getSaddle());
                }
            } catch (Throwable ignored) {}
        }
        if (pet instanceof MyVillager villagerPet && mob instanceof Villager villager) {
            try {
                // Drift-safe: look profession up by NamespacedKey, not ordinal.
                try {
                    Villager.Profession prof = Registry.VILLAGER_PROFESSION.get(
                            NamespacedKey.minecraft(villagerPet.getProfessionKey()));
                    if (prof != null) {
                        villager.setProfession(prof);
                    }
                } catch (Throwable ignored) {}
                // Villager.Type is now stored directly — no intermediate mapping needed.
                try {
                    Villager.Type bukkitType = villagerPet.getType();
                    if (bukkitType != null) {
                        villager.setVillagerType(bukkitType);
                    }
                } catch (Throwable ignored) {}
                villager.setVillagerLevel(Math.max(1, Math.min(5, villagerPet.getLevel())));
            } catch (Throwable ignored) {}
        }
        if (pet instanceof MyZombieVillager zvPet && mob instanceof ZombieVillager zv) {
            try {
                try {
                    Villager.Profession prof = Registry.VILLAGER_PROFESSION.get(
                            NamespacedKey.minecraft(zvPet.getProfessionKey()));
                    if (prof != null) {
                        zv.setVillagerProfession(prof);
                    }
                } catch (Throwable ignored) {}
                try {
                    Villager.Type bukkitType = zvPet.getType();
                    if (bukkitType != null) {
                        zv.setVillagerType(bukkitType);
                    }
                } catch (Throwable ignored) {}
            } catch (Throwable ignored) {}
        }
        if (pet instanceof MyPanda pandaPet && mob instanceof Panda panda) {
            try {
                panda.setMainGene(pandaPet.getMainGene());
                panda.setHiddenGene(pandaPet.getHiddenGene());
            } catch (Throwable ignored) {}
        }
        if (pet instanceof MyFox foxPet && mob instanceof Fox fox) {
            try {
                fox.setFoxType(foxPet.getFoxType());
            } catch (Throwable ignored) {}
        }
        if (pet instanceof MyGoat goatPet && mob instanceof Goat goat) {
            try {
                goat.setScreaming(goatPet.isScreaming());
                goat.setLeftHorn(goatPet.hasLeftHorn());
                goat.setRightHorn(goatPet.hasRightHorn());
            } catch (Throwable ignored) {}
        }
        if (pet instanceof MyEnderman enPet && mob instanceof Enderman enderman) {
            try {
                if (enPet.hasBlock() && enPet.getBlock() != null) {
                    enderman.setCarriedBlock(enPet.getBlock().getType().createBlockData());
                }
                if (enPet.isScreaming()) {
                    enderman.setScreaming(true);
                }
            } catch (Throwable ignored) {}
        }
        // ─── Tier 3 types with no straightforward Bukkit setter — deferred ───
        // MyCopperGolem (oxidation state — no Bukkit API in 1.21.x)
        // MyWarden (heartAttack — no setter)
        // MyIronGolem (flower — no Bukkit API)
        // MyVex (glowing — vex glow driven by vanilla internally)
        // MyChicken/MyCow variants (1.21.4+ only, skipping until stage F)
        // MyPiglin/MyPiglinBrute/MyHoglin (shakeImmune — internal AI flag)
    }
}
