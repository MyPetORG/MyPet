/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2026 Keyle
 * MyPet is licensed under the GNU Lesser General Public License.
 *
 * MyPet is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyPet is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.api.config;

import de.Keyle.MyPet.api.util.ConfigItem;

/**
 * Canonical declarations for every per-pet config key, organized by pet
 * type. Each nested class corresponds to one pet type; each static field
 * is a {@link ConfigKey} whose factory call self-registers with
 * {@link ConfigKeyRegistry}.
 *
 * <p>Read sites — plugin and third-party alike — reference these constants
 * directly:
 *
 * <pre>{@code
 * if (PetConfigKeys.Creeper.ALLOW_LIGHTNING_POWER.get()) {
 *     // ...
 * }
 * }</pre>
 *
 * <p>YAML schema: each constant maps to {@code MyPet.Pets.<PetType>.<Key>}.
 * Defaults baked into the constant — no need for callers to remember them.
 *
 * <p><b>Nested-class loading.</b> {@link #ensureLoaded()} force-loads every
 * nested class via reflection so each one's {@code <clinit>} runs and
 * registers its keys with {@link ConfigKeyRegistry} before
 * {@code ConfigurationLoader} iterates the registry. Call it once during
 * plugin startup before {@link ConfigKeyRegistry#writeDefaults} or
 * {@link ConfigKeyRegistry#loadFromYaml}.
 *
 * <p>Pet types only available on newer Paper versions (CopperGolem,
 * ZombieNautilus, etc.) appear here regardless of runtime support — their
 * YAML rows are written but never read by any pet instance on older runtimes,
 * which is harmless.
 */
public final class PetConfigKeys {

    private PetConfigKeys() {}

    /**
     * Forces class initialization of every nested per-pet class so each
     * static {@link ConfigKey} field fires its factory call and registers
     * with {@link ConfigKeyRegistry}. Idempotent — subsequent calls are
     * no-ops.
     */
    public static void ensureLoaded() {
        // The static block (below) runs on first reference to PetConfigKeys.
        // This method exists purely to trigger that initialization from
        // ConfigurationLoader without depending on any specific nested class.
    }

    static {
        for (Class<?> nested : PetConfigKeys.class.getDeclaredClasses()) {
            try {
                Class.forName(nested.getName(), true, nested.getClassLoader());
            } catch (Throwable ignored) {
                // Best-effort — a failed nested class init only loses that
                // pet's keys; the rest of the registry is unaffected.
            }
        }
    }

    // =====================================================================
    // Per-pet nested classes — alphabetical
    // =====================================================================

    public static final class Allay {
        public static final ConfigKey<Boolean> CAN_FLY = ConfigKey.bool("Allay", "CanFly", true);
    }

    public static final class Armadillo {
        public static final ConfigKey<Boolean> CAN_SHED_SCUTE = ConfigKey.bool("Armadillo", "CanShedScute", true);
        public static final ConfigKey<ConfigItem> GROW_UP_ITEM = ConfigKey.growUpItem("Armadillo", "experience_bottle");
    }

    public static final class Axolotl {
        public static final ConfigKey<Boolean> CAN_SWIM = ConfigKey.bool("Axolotl", "CanSwim", true);
        public static final ConfigKey<Boolean> PREVENT_DRY_OUT = ConfigKey.bool("Axolotl", "PreventDryOut", true);
        public static final ConfigKey<ConfigItem> GROW_UP_ITEM = ConfigKey.growUpItem("Axolotl", "experience_bottle");
    }

    public static final class Bat {
        public static final ConfigKey<Boolean> CAN_FLY = ConfigKey.bool("Bat", "CanFly", true);
    }

    public static final class Bee {
        public static final ConfigKey<Boolean> CAN_FLY = ConfigKey.bool("Bee", "CanFly", true);
        public static final ConfigKey<ConfigItem> GROW_UP_ITEM = ConfigKey.growUpItem("Bee", "experience_bottle");
    }

    public static final class Blaze {
        public static final ConfigKey<Boolean> CAN_FLY = ConfigKey.bool("Blaze", "CanFly", true);
    }

    public static final class Bogged {
        public static final ConfigKey<Boolean> PREVENT_DAYLIGHT_BURN = ConfigKey.bool("Bogged", "PreventDaylightBurn", true);
    }

    public static final class Breeze {
        public static final ConfigKey<Boolean> CAN_FLY = ConfigKey.bool("Breeze", "CanFly", true);
    }

    public static final class Camel {
        public static final ConfigKey<ConfigItem> GROW_UP_ITEM = ConfigKey.growUpItem("Camel", "experience_bottle");
    }

    public static final class CamelHusk {
        public static final ConfigKey<ConfigItem> GROW_UP_ITEM = ConfigKey.growUpItem("CamelHusk", "experience_bottle");
    }

    public static final class Cat {
        public static final ConfigKey<ConfigItem> GROW_UP_ITEM = ConfigKey.growUpItem("Cat", "experience_bottle");
    }

    public static final class Chicken {
        public static final ConfigKey<Boolean> CAN_LAY_EGGS = ConfigKey.bool("Chicken", "CanLayEggs", true);
        public static final ConfigKey<ConfigItem> GROW_UP_ITEM = ConfigKey.growUpItem("Chicken", "experience_bottle");
    }

    public static final class Cod {
        public static final ConfigKey<Boolean> CAN_SWIM = ConfigKey.bool("Cod", "CanSwim", true);
        public static final ConfigKey<Boolean> PREVENT_SUFFOCATION = ConfigKey.bool("Cod", "PreventSuffocation", true);
    }

    public static final class CopperGolem {
        public static final ConfigKey<Boolean> CAN_OXIDIZE = ConfigKey.bool("CopperGolem", "CanOxidize", true);
    }

    public static final class Cow {
        public static final ConfigKey<Boolean> CAN_GIVE_MILK = ConfigKey.bool("Cow", "CanGiveMilk", true);
        public static final ConfigKey<ConfigItem> GROW_UP_ITEM = ConfigKey.growUpItem("Cow", "experience_bottle");
    }

    public static final class Creeper {
        public static final ConfigKey<Boolean> ALLOW_FLINT_AND_STEEL_EXPLODE = ConfigKey.bool("Creeper", "AllowFlintAndSteelExplode", false);
        public static final ConfigKey<Boolean> ALLOW_NON_OWNER_FLINT_AND_STEEL = ConfigKey.bool("Creeper", "AllowNonOwnerFlintAndSteel", false);
        public static final ConfigKey<Boolean> ALLOW_EXPLOSION_BLOCK_DAMAGE = ConfigKey.bool("Creeper", "AllowExplosionBlockDamage", false);
        public static final ConfigKey<Boolean> ALLOW_EXPLOSION_ENTITY_DAMAGE = ConfigKey.bool("Creeper", "AllowExplosionEntityDamage", false);
        public static final ConfigKey<Boolean> ALLOW_LIGHTNING_POWER = ConfigKey.bool("Creeper", "AllowLightningPower", false);
    }

    public static final class Dolphin {
        public static final ConfigKey<Boolean> CAN_SWIM = ConfigKey.bool("Dolphin", "CanSwim", true);
        public static final ConfigKey<Boolean> PREVENT_SUFFOCATION = ConfigKey.bool("Dolphin", "PreventSuffocation", true);
        public static final ConfigKey<ConfigItem> GROW_UP_ITEM = ConfigKey.growUpItem("Dolphin", "experience_bottle");
    }

    public static final class Donkey {
        public static final ConfigKey<ConfigItem> GROW_UP_ITEM = ConfigKey.growUpItem("Donkey", "experience_bottle");
    }

    public static final class Drowned {
        public static final ConfigKey<Boolean> CAN_SWIM = ConfigKey.bool("Drowned", "CanSwim", true);
        public static final ConfigKey<Boolean> PREVENT_DAYLIGHT_BURN = ConfigKey.bool("Drowned", "PreventDaylightBurn", true);
        public static final ConfigKey<ConfigItem> GROW_UP_ITEM = ConfigKey.growUpItem("Drowned", "experience_bottle");
    }

    public static final class ElderGuardian {
        public static final ConfigKey<Boolean> CAN_SWIM = ConfigKey.bool("ElderGuardian", "CanSwim", true);
        public static final ConfigKey<Boolean> PREVENT_SUFFOCATION = ConfigKey.bool("ElderGuardian", "PreventSuffocation", true);
    }

    public static final class EnderDragon {
        public static final ConfigKey<Boolean> CAN_FLY = ConfigKey.bool("EnderDragon", "CanFly", true);
        public static final ConfigKey<Boolean> GRANT_END_ADVANCEMENT_ON_KILL = ConfigKey.bool("EnderDragon", "GrantEndAdvancementOnKill", false);
        public static final ConfigKey<Boolean> ALLOW_BLOCK_DAMAGE = ConfigKey.bool("EnderDragon", "AllowBlockDamage", false);
        public static final ConfigKey<Boolean> ALLOW_PLAYER_CONTACT_DAMAGE = ConfigKey.bool("EnderDragon", "AllowPlayerContactDamage", false);
        public static final ConfigKey<Boolean> ALLOW_ENTITY_CONTACT_DAMAGE = ConfigKey.bool("EnderDragon", "AllowEntityContactDamage", false);
    }

    public static final class Fox {
        public static final ConfigKey<ConfigItem> GROW_UP_ITEM = ConfigKey.growUpItem("Fox", "experience_bottle");
    }

    public static final class Frog {
        public static final ConfigKey<Boolean> CAN_SWIM = ConfigKey.bool("Frog", "CanSwim", true);
    }

    public static final class Ghast {
        public static final ConfigKey<Boolean> CAN_FLY = ConfigKey.bool("Ghast", "CanFly", true);
    }

    public static final class GlowSquid {
        public static final ConfigKey<Boolean> CAN_SWIM = ConfigKey.bool("GlowSquid", "CanSwim", true);
        public static final ConfigKey<Boolean> PREVENT_SUFFOCATION = ConfigKey.bool("GlowSquid", "PreventSuffocation", true);
        public static final ConfigKey<ConfigItem> GROW_UP_ITEM = ConfigKey.growUpItem("GlowSquid", "experience_bottle");
    }

    public static final class Goat {
        public static final ConfigKey<Boolean> CAN_DROP_HORN = ConfigKey.bool("Goat", "CanDropHorn", true);
        public static final ConfigKey<Boolean> CAN_GIVE_MILK = ConfigKey.bool("Goat", "CanGiveMilk", true);
        public static final ConfigKey<ConfigItem> GROW_UP_ITEM = ConfigKey.growUpItem("Goat", "experience_bottle");
    }

    public static final class Guardian {
        public static final ConfigKey<Boolean> CAN_SWIM = ConfigKey.bool("Guardian", "CanSwim", true);
        public static final ConfigKey<Boolean> PREVENT_SUFFOCATION = ConfigKey.bool("Guardian", "PreventSuffocation", true);
    }

    public static final class HappyGhast {
        public static final ConfigKey<Boolean> CAN_FLY = ConfigKey.bool("HappyGhast", "CanFly", true);
        public static final ConfigKey<ConfigItem> GROW_UP_ITEM = ConfigKey.growUpItem("HappyGhast", "experience_bottle");
    }

    public static final class Hoglin {
        public static final ConfigKey<Boolean> ALLOW_ZOMBIFICATION = ConfigKey.bool("Hoglin", "AllowZombification", false);
        public static final ConfigKey<ConfigItem> GROW_UP_ITEM = ConfigKey.growUpItem("Hoglin", "experience_bottle");
    }

    public static final class Horse {
        public static final ConfigKey<ConfigItem> GROW_UP_ITEM = ConfigKey.growUpItem("Horse", "bread");
    }

    public static final class Husk {
        public static final ConfigKey<ConfigItem> GROW_UP_ITEM = ConfigKey.growUpItem("Husk", "experience_bottle");
    }

    public static final class IronGolem {
        public static final ConfigKey<Boolean> CAN_TOSS_UP = ConfigKey.bool("IronGolem", "CanTossUp", true);
    }

    public static final class Llama {
        public static final ConfigKey<ConfigItem> GROW_UP_ITEM = ConfigKey.growUpItem("Llama", "experience_bottle");
    }

    public static final class MagmaCube {
        public static final ConfigKey<Boolean> CAN_HURT_PLAYERS_ON_CONTACT = ConfigKey.bool("MagmaCube", "CanHurtPlayersOnContact", false);
    }

    public static final class Mooshroom {
        public static final ConfigKey<Boolean> CAN_GIVE_STEW = ConfigKey.bool("Mooshroom", "CanGiveStew", false);
        public static final ConfigKey<Boolean> ALLOW_LIGHTNING_VARIANT_FLIP = ConfigKey.bool("Mooshroom", "AllowLightningVariantFlip", false);
        public static final ConfigKey<ConfigItem> GROW_UP_ITEM = ConfigKey.growUpItem("Mooshroom", "experience_bottle");
    }

    public static final class Mule {
        public static final ConfigKey<ConfigItem> GROW_UP_ITEM = ConfigKey.growUpItem("Mule", "experience_bottle");
    }

    public static final class Nautilus {
        public static final ConfigKey<Boolean> CAN_SWIM = ConfigKey.bool("Nautilus", "CanSwim", true);
    }

    public static final class Ocelot {
        public static final ConfigKey<ConfigItem> GROW_UP_ITEM = ConfigKey.growUpItem("Ocelot", "experience_bottle");
    }

    public static final class Panda {
        public static final ConfigKey<Boolean> CAN_DROP_SLIMEBALL = ConfigKey.bool("Panda", "CanDropSlimeball", true);
        public static final ConfigKey<ConfigItem> GROW_UP_ITEM = ConfigKey.growUpItem("Panda", "experience_bottle");
    }

    public static final class Parrot {
        public static final ConfigKey<Boolean> CAN_FLY = ConfigKey.bool("Parrot", "CanFly", true);
    }

    public static final class Phantom {
        public static final ConfigKey<Boolean> CAN_FLY = ConfigKey.bool("Phantom", "CanFly", true);
        public static final ConfigKey<Boolean> PREVENT_DAYLIGHT_BURN = ConfigKey.bool("Phantom", "PreventDaylightBurn", true);
    }

    public static final class Pig {
        public static final ConfigKey<Boolean> ALLOW_LIGHTNING_CONVERSION = ConfigKey.bool("Pig", "AllowLightningConversion", false);
        public static final ConfigKey<ConfigItem> GROW_UP_ITEM = ConfigKey.growUpItem("Pig", "experience_bottle");
    }

    public static final class Piglin {
        public static final ConfigKey<Boolean> ALLOW_ZOMBIFICATION = ConfigKey.bool("Piglin", "AllowZombification", false);
        public static final ConfigKey<ConfigItem> GROW_UP_ITEM = ConfigKey.growUpItem("Piglin", "experience_bottle");
    }

    public static final class PiglinBrute {
        public static final ConfigKey<Boolean> ALLOW_ZOMBIFICATION = ConfigKey.bool("PiglinBrute", "AllowZombification", false);
    }

    public static final class PolarBear {
        public static final ConfigKey<ConfigItem> GROW_UP_ITEM = ConfigKey.growUpItem("PolarBear", "experience_bottle");
    }

    public static final class Pufferfish {
        public static final ConfigKey<Boolean> CAN_SWIM = ConfigKey.bool("Pufferfish", "CanSwim", true);
        public static final ConfigKey<Boolean> PREVENT_SUFFOCATION = ConfigKey.bool("Pufferfish", "PreventSuffocation", true);
    }

    public static final class Rabbit {
        public static final ConfigKey<ConfigItem> GROW_UP_ITEM = ConfigKey.growUpItem("Rabbit", "experience_bottle");
    }

    public static final class Salmon {
        public static final ConfigKey<Boolean> CAN_SWIM = ConfigKey.bool("Salmon", "CanSwim", true);
        public static final ConfigKey<Boolean> PREVENT_SUFFOCATION = ConfigKey.bool("Salmon", "PreventSuffocation", true);
    }

    public static final class Sheep {
        public static final ConfigKey<Boolean> CAN_BE_SHEARED = ConfigKey.bool("Sheep", "CanBeSheared", true);
        public static final ConfigKey<Boolean> CAN_REGROW_WOOL = ConfigKey.bool("Sheep", "CanRegrowWool", true);
        public static final ConfigKey<ConfigItem> GROW_UP_ITEM = ConfigKey.growUpItem("Sheep", "experience_bottle");
    }

    public static final class Skeleton {
        public static final ConfigKey<Boolean> PREVENT_DAYLIGHT_BURN = ConfigKey.bool("Skeleton", "PreventDaylightBurn", true);
    }

    public static final class SkeletonHorse {
        public static final ConfigKey<ConfigItem> GROW_UP_ITEM = ConfigKey.growUpItem("SkeletonHorse", "experience_bottle");
    }

    public static final class Slime {
        public static final ConfigKey<Boolean> CAN_HURT_PLAYERS_ON_CONTACT = ConfigKey.bool("Slime", "CanHurtPlayersOnContact", false);
    }

    public static final class Sniffer {
        public static final ConfigKey<Boolean> CAN_DIG_SEEDS = ConfigKey.bool("Sniffer", "CanDigSeeds", true);
        public static final ConfigKey<ConfigItem> GROW_UP_ITEM = ConfigKey.growUpItem("Sniffer", "experience_bottle");
    }

    public static final class SnowGolem {
        public static final ConfigKey<Boolean> DISABLE_SNOW_TRACK = ConfigKey.bool("SnowGolem", "DisableSnowTrack", true);
    }

    public static final class Squid {
        public static final ConfigKey<Boolean> CAN_SWIM = ConfigKey.bool("Squid", "CanSwim", true);
        public static final ConfigKey<Boolean> PREVENT_SUFFOCATION = ConfigKey.bool("Squid", "PreventSuffocation", true);
        public static final ConfigKey<ConfigItem> GROW_UP_ITEM = ConfigKey.growUpItem("Squid", "experience_bottle");
    }

    public static final class Stray {
        public static final ConfigKey<Boolean> PREVENT_DAYLIGHT_BURN = ConfigKey.bool("Stray", "PreventDaylightBurn", true);
    }

    public static final class Strider {
        public static final ConfigKey<ConfigItem> GROW_UP_ITEM = ConfigKey.growUpItem("Strider", "experience_bottle");
    }

    public static final class Tadpole {
        public static final ConfigKey<Boolean> CAN_SWIM = ConfigKey.bool("Tadpole", "CanSwim", true);
        public static final ConfigKey<Boolean> PREVENT_SUFFOCATION = ConfigKey.bool("Tadpole", "PreventSuffocation", true);
    }

    public static final class TraderLlama {
        public static final ConfigKey<ConfigItem> GROW_UP_ITEM = ConfigKey.growUpItem("TraderLlama", "experience_bottle");
    }

    public static final class TropicalFish {
        public static final ConfigKey<Boolean> CAN_SWIM = ConfigKey.bool("TropicalFish", "CanSwim", true);
        public static final ConfigKey<Boolean> PREVENT_SUFFOCATION = ConfigKey.bool("TropicalFish", "PreventSuffocation", true);
    }

    public static final class Turtle {
        public static final ConfigKey<Boolean> CAN_SWIM = ConfigKey.bool("Turtle", "CanSwim", true);
        public static final ConfigKey<ConfigItem> GROW_UP_ITEM = ConfigKey.growUpItem("Turtle", "experience_bottle");
    }

    public static final class Vex {
        public static final ConfigKey<Boolean> CAN_FLY = ConfigKey.bool("Vex", "CanFly", true);
    }

    public static final class Villager {
        public static final ConfigKey<Boolean> ALLOW_LIGHTNING_CONVERSION = ConfigKey.bool("Villager", "AllowLightningConversion", false);
        public static final ConfigKey<ConfigItem> GROW_UP_ITEM = ConfigKey.growUpItem("Villager", "experience_bottle");
    }

    public static final class Wither {
        public static final ConfigKey<Boolean> CAN_FLY = ConfigKey.bool("Wither", "CanFly", true);
        public static final ConfigKey<ConfigItem> GROW_UP_ITEM = ConfigKey.growUpItem("Wither", "experience_bottle");
    }

    public static final class Wolf {
        public static final ConfigKey<ConfigItem> GROW_UP_ITEM = ConfigKey.growUpItem("Wolf", "experience_bottle");
    }

    public static final class Zoglin {
        public static final ConfigKey<ConfigItem> GROW_UP_ITEM = ConfigKey.growUpItem("Zoglin", "experience_bottle");
    }

    public static final class Zombie {
        public static final ConfigKey<Boolean> PREVENT_DAYLIGHT_BURN = ConfigKey.bool("Zombie", "PreventDaylightBurn", true);
        public static final ConfigKey<ConfigItem> GROW_UP_ITEM = ConfigKey.growUpItem("Zombie", "experience_bottle");
    }

    public static final class ZombieHorse {
        public static final ConfigKey<ConfigItem> GROW_UP_ITEM = ConfigKey.growUpItem("ZombieHorse", "experience_bottle");
    }

    public static final class ZombieNautilus {
        public static final ConfigKey<Boolean> CAN_SWIM = ConfigKey.bool("ZombieNautilus", "CanSwim", true);
        public static final ConfigKey<Boolean> PREVENT_DAYLIGHT_BURN = ConfigKey.bool("ZombieNautilus", "PreventDaylightBurn", true);
    }

    public static final class ZombieVillager {
        public static final ConfigKey<Boolean> PREVENT_DAYLIGHT_BURN = ConfigKey.bool("ZombieVillager", "PreventDaylightBurn", true);
        public static final ConfigKey<ConfigItem> GROW_UP_ITEM = ConfigKey.growUpItem("ZombieVillager", "experience_bottle");
    }

    public static final class ZombifiedPiglin {
        public static final ConfigKey<ConfigItem> GROW_UP_ITEM = ConfigKey.growUpItem("ZombifiedPiglin", "experience_bottle");
    }
}
