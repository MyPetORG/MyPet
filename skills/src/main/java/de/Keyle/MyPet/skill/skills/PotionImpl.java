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

package de.Keyle.MyPet.skill.skills;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.entity.Pet.PetState;
import de.Keyle.MyPet.api.skill.SkillUpgrades;
import de.Keyle.MyPet.api.skill.UpgradeComputer;
import de.Keyle.MyPet.api.skill.UpgradeParsers;
import de.Keyle.MyPet.api.skill.UpgradeSchema;
import de.Keyle.MyPet.api.skill.skills.Behavior;
import de.Keyle.MyPet.api.skill.skills.Potion;
import de.Keyle.MyPet.api.util.locale.Locale;
import de.Keyle.MyPet.skill.upgrades.PotionUpgrade;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionEffectTypeCategory;
import org.bukkit.potion.PotionType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PotionImpl extends AbstractSkill implements Potion {

    public static final SkillUpgrades UPGRADES = SkillUpgrades.of(Potion.class,
            UpgradeSchema.builder()
                    .list("potions", row -> row
                            .enumValues("type", potionCatalog()).label("Potion")
                            .integer("cooldown").label("Cooldown").suffix(" s"))
                    .bool("materialize").label("Materialize (no stock)")
                    .build(), PotionImpl::parseUpgrade);

    /** Throw range in blocks (owner for support, nearest hostile for offense). */
    private static final double THROW_RANGE = 16;
    private static final double MAX_THROW_RANGE_SQUARED = THROW_RANGE * THROW_RANGE;
    private static final int DEFAULT_COOLDOWN = 8;

    /** PDC tags on a thrown potion so {@code PetPotionListener} can shield the owner from harmful splashes. */
    private static final NamespacedKey POTION_OWNER_KEY = new NamespacedKey("mypet", "potion_owner");
    private static final NamespacedKey POTION_HARMFUL_KEY = new NamespacedKey("mypet", "potion_harmful");

    private static final Attribute MAX_HEALTH = resolveMaxHealth();

    /** The permitted potions (skilltree-granted); rebuilt on every skilltree change. */
    private final List<Potion.Entry> arsenal = new ArrayList<>();
    /** Conjure permitted potions without backpack stock. */
    private final UpgradeComputer<Boolean> materialize = new UpgradeComputer<>(false);
    /** Seconds left before each potion type may be thrown again (this list ticks once per second). */
    private final Map<PotionType, Integer> cooldownRemaining = new HashMap<>();
    /** Throttle for the "out of potions" hint so it fires once per depletion. */
    private boolean warnedNoStock = false;

    public PotionImpl(Pet pet) {
        super(pet);
    }

    @Override
    public boolean isActive() {
        return !arsenal.isEmpty();
    }

    @Override
    public void reset() {
        arsenal.clear();
        materialize.removeAllUpgrades();
        cooldownRemaining.clear();
        warnedNoStock = false;
    }

    @Override
    public void addEntry(Entry entry) {
        arsenal.add(entry);
    }

    @Override
    public void removeEntry(Entry entry) {
        arsenal.remove(entry);
    }

    @Override
    public List<Entry> getArsenal() {
        return arsenal;
    }

    @Override
    public UpgradeComputer<Boolean> getMaterialize() {
        return materialize;
    }

    @Override
    public Component toPrettyComponent(String locale) {
        Component result = Component.empty();
        boolean first = true;
        for (Entry entry : arsenal) {
            if (!first) {
                result = result.append(Component.text(", "));
            }
            result = result
                    .append(Component.text(prettyName(entry.type())).color(NamedTextColor.GOLD))
                    .append(Component.text(" (" + entry.cooldown() + "s)").color(NamedTextColor.GRAY));
            first = false;
        }
        if (materialize.getValue()) {
            result = result.append(Component.space())
                    .append(Locale.getComponent("Name.Skill.Potion.Materialize", locale).color(NamedTextColor.AQUA));
        }
        return result;
    }

    @Override
    public Component[] getUpgradeMessage() {
        return new Component[]{
                upgradeMessage("Message.Skill.Potion.Upgrade", arsenal.size()),
                Component.text(" ").append(toPrettyComponent(pet.getOwner().getLanguage()))
        };
    }

    @Override
    public void schedule() {
        if (pet.getStatus() != PetState.Here || !isActive()) {
            return;
        }
        tickCooldowns();

        Mob mob = pet.getBukkitEntity();
        Player owner = pet.getOwner() != null ? pet.getOwner().getPlayer() : null;
        if (mob == null || mob.isDead() || owner == null || owner.isDead() || !Bukkit.isOwnedByCurrentRegion(mob)) {
            return;
        }
        // Owner-targeted (beneficial) throws read and aim at the owner, so they need the owner near and in
        // the pet's region. Enemy-targeted (harmful) throws don't — the pet may be off chasing a mob far
        // from the owner — so we don't gate the whole skill on owner distance any more.
        boolean ownerReachable = owner.isOnline()
                && Bukkit.isOwnedByCurrentRegion(owner)
                && owner.getGameMode() != GameMode.SPECTATOR
                && owner.getWorld() == mob.getWorld()
                && owner.getLocation().distanceSquared(mob.getLocation()) <= MAX_THROW_RANGE_SQUARED;

        ThrowPlan plan = chooseThrow(mob, owner, ownerReachable);
        if (plan == null) {
            return;
        }
        if (!mob.hasLineOfSight(plan.target)) {
            return; // a wall between them would just shatter the potion for nothing
        }
        PotionType throwType = plan.type;
        if (!materialize.getValue()) {
            int slot = findStockSlot(plan.type);
            if (slot < 0) {
                warnNoStock(owner, plan.type);
                return;
            }
            throwType = stockedType(slot); // throw the actual bottle the owner stocked (its own strength)
            consumeSlot(slot);
        }
        warnedNoStock = false;
        throwPotion(mob, owner, plan.target, throwType, plan.harmful);
        cooldownRemaining.put(plan.type, Math.max(1, plan.cooldown));
    }

    /** A decided throw: which potion, at whom, whether it's a debuff, and its cooldown to stamp. */
    private record ThrowPlan(PotionType type, LivingEntity target, boolean harmful, int cooldown) {}

    /** Counts every arsenal cooldown down by one second (this runs once per second). */
    private void tickCooldowns() {
        cooldownRemaining.replaceAll((type, left) -> left > 0 ? left - 1 : 0);
    }

    /**
     * Picks the most urgent throwable potion this cycle: rescue (heal a hurt owner) &gt; offense &gt;
     * owner buff. A candidate must be off cooldown, contextually useful, and — unless Materialize is
     * granted — actually stocked in the Backpack. Beneficial potions also need the owner reachable.
     */
    private ThrowPlan chooseThrow(Mob mob, Player owner, boolean ownerReachable) {
        LivingEntity enemy = resolveEnemy(mob);
        boolean combat = enemy != null || pet.hasTarget();
        ThrowPlan best = null;
        int bestScore = -1;
        boolean wantedButUnstocked = false;

        for (Entry entry : arsenal) {
            PotionType type = entry.type();
            if (cooldownRemaining.getOrDefault(type, 0) > 0) {
                continue;
            }
            List<PotionEffect> effects = type.getPotionEffects();
            if (effects.isEmpty()) {
                continue;
            }
            boolean harmful = isHarmful(effects);
            boolean instant = allInstant(effects);
            LivingEntity target;
            int score;
            if (harmful) {
                if (enemy == null) {
                    continue; // nothing to debuff (or Friendly)
                }
                if (!instant && !lacksAny(enemy, effects)) {
                    continue; // already fully debuffed
                }
                target = enemy;
                score = instant ? 80 : 70;
            } else {
                if (!ownerReachable || !beneficialUseful(owner, effects, combat)) {
                    continue; // owner unreachable, or nothing here the owner actually needs right now
                }
                target = owner;
                score = instant ? 100 : 40; // an emergency heal outranks a routine buff
            }
            if (score <= bestScore) {
                continue;
            }
            if (!materialize.getValue() && findStockSlot(type) < 0) {
                wantedButUnstocked = true; // it would have thrown, but there's no matching bottle
                continue;
            }
            best = new ThrowPlan(type, target, harmful, entry.cooldown());
            bestScore = score;
        }
        if (best == null && wantedButUnstocked) {
            warnNoStock(owner, null);
        }
        return best;
    }

    /**
     * The mob a harmful potion should target, honouring the pet's behaviour: never in Friendly; the
     * pet's own target when it has a live one in range; and for the proactive modes (Aggressive/Raid)
     * the nearest hostile in range when it doesn't.
     */
    private LivingEntity resolveEnemy(Mob mob) {
        Behavior.BehaviorMode mode = behaviorMode();
        if (mode == Behavior.BehaviorMode.Friendly) {
            return null;
        }
        LivingEntity target = validEnemy(mob, pet.getPetTarget());
        if (target != null) {
            return target;
        }
        return mode == Behavior.BehaviorMode.Aggressive || mode == Behavior.BehaviorMode.Raid
                ? nearestHostile(mob) : null;
    }

    private Behavior.BehaviorMode behaviorMode() {
        return pet.getSkills().isActive(BehaviorImpl.class)
                ? pet.getSkills().get(BehaviorImpl.class).getBehavior() : Behavior.BehaviorMode.Normal;
    }

    private static LivingEntity validEnemy(Mob mob, LivingEntity candidate) {
        if (candidate == null || candidate.isDead() || candidate.getWorld() != mob.getWorld()) {
            return null;
        }
        return mob.getLocation().distanceSquared(candidate.getLocation()) <= MAX_THROW_RANGE_SQUARED ? candidate : null;
    }

    private static LivingEntity nearestHostile(Mob mob) {
        LivingEntity nearest = null;
        double best = Double.MAX_VALUE;
        for (Entity entity : mob.getNearbyEntities(THROW_RANGE, THROW_RANGE, THROW_RANGE)) {
            if (entity instanceof Monster monster && !monster.isDead()) {
                double distance = mob.getLocation().distanceSquared(monster.getLocation());
                if (distance <= MAX_THROW_RANGE_SQUARED && distance < best) {
                    best = distance;
                    nearest = monster;
                }
            }
        }
        return nearest;
    }

    /**
     * Whether a beneficial potion is worth throwing at the owner right now — each effect only when the
     * situation calls for it (Fire Resistance while burning, Night Vision at night, Strength in combat,
     * a heal when hurt, …), and never a lasting effect the owner already has.
     */
    private boolean beneficialUseful(Player owner, List<PotionEffect> effects, boolean combat) {
        for (PotionEffect effect : effects) {
            PotionEffectType type = effect.getType();
            if (!type.isInstant() && owner.hasPotionEffect(type)) {
                continue; // already buffed — don't refresh early
            }
            if (contextNeeds(owner, type, combat)) {
                return true;
            }
        }
        return false;
    }

    private static boolean contextNeeds(Player owner, PotionEffectType type, boolean combat) {
        return switch (type.getKey().getKey()) {
            case "instant_health", "regeneration", "absorption" -> ownerHurt(owner);
            case "fire_resistance" -> owner.getFireTicks() > 0;
            case "water_breathing", "conduit_power" -> owner.getRemainingAir() < owner.getMaximumAir();
            case "night_vision" -> isNight(owner.getWorld());
            case "slow_falling", "levitation" -> owner.getVelocity().getY() < -0.5;
            default -> combat; // Strength, Speed, Resistance, Jump, Haste, … — only during a fight
        };
    }

    private static boolean isNight(World world) {
        long time = world.getTime();
        return time >= 13000 && time < 23000;
    }

    /** Harmful only when no effect is beneficial and at least one is harmful (so Turtle Master stays a buff). */
    private static boolean isHarmful(List<PotionEffect> effects) {
        boolean anyHarmful = false;
        for (PotionEffect effect : effects) {
            PotionEffectTypeCategory category = effect.getType().getCategory();
            if (category == PotionEffectTypeCategory.BENEFICIAL) {
                return false;
            }
            if (category == PotionEffectTypeCategory.HARMFUL) {
                anyHarmful = true;
            }
        }
        return anyHarmful;
    }

    private static boolean allInstant(List<PotionEffect> effects) {
        return effects.stream().allMatch(effect -> effect.getType().isInstant());
    }

    /** True if the entity is missing at least one of the potion's lasting effects (so a throw isn't wasted). */
    private static boolean lacksAny(LivingEntity entity, List<PotionEffect> effects) {
        return effects.stream().anyMatch(effect -> !entity.hasPotionEffect(effect.getType()));
    }

    private static boolean ownerHurt(Player owner) {
        AttributeInstance maxHealth = MAX_HEALTH != null ? owner.getAttribute(MAX_HEALTH) : null;
        double max = maxHealth != null ? maxHealth.getValue() : 20.0;
        return owner.getHealth() < max - 1.0;
    }

    // --- Backpack stock ----------------------------------------------------------------------

    /** Backpack slot of a splash potion whose effect family matches {@code granted}, or -1 (needs Backpack active). */
    private int findStockSlot(PotionType granted) {
        BackpackImpl backpack = activeBackpack();
        if (backpack == null) {
            return -1;
        }
        ItemStack[] contents = backpack.rawContents();
        for (int i = 0; i < contents.length; i++) {
            if (isSplashOfFamily(contents[i], granted)) {
                return i;
            }
        }
        return -1;
    }

    /** The base potion type of the splash bottle in {@code slot} (what actually gets thrown). */
    private PotionType stockedType(int slot) {
        ItemStack item = activeBackpack().rawContents()[slot];
        return ((PotionMeta) item.getItemMeta()).getBasePotionType();
    }

    /** Removes one bottle from {@code slot}. */
    private void consumeSlot(int slot) {
        ItemStack[] contents = activeBackpack().rawContents();
        ItemStack item = contents[slot];
        if (item == null) {
            return;
        }
        if (item.getAmount() <= 1) {
            contents[slot] = null;
        } else {
            item.setAmount(item.getAmount() - 1);
        }
    }

    private BackpackImpl activeBackpack() {
        return pet.getSkills().isActive(BackpackImpl.class) ? pet.getSkills().get(BackpackImpl.class) : null;
    }

    /** A splash potion whose effect family matches {@code granted} (so Healing II satisfies a Healing grant). */
    private static boolean isSplashOfFamily(ItemStack item, PotionType granted) {
        if (item == null || item.getType() != Material.SPLASH_POTION) {
            return false;
        }
        if (!(item.getItemMeta() instanceof PotionMeta meta) || !meta.hasBasePotionType()) {
            return false;
        }
        return sameFamily(meta.getBasePotionType(), granted);
    }

    /** Two potion types are the same family if they share their primary effect (I / II / long variants). */
    private static boolean sameFamily(PotionType a, PotionType b) {
        if (a == b) {
            return true;
        }
        PotionEffectType primary = primaryEffect(a);
        return primary != null && primary == primaryEffect(b);
    }

    private static PotionEffectType primaryEffect(PotionType type) {
        List<PotionEffect> effects = type.getPotionEffects();
        return effects.isEmpty() ? null : effects.get(0).getType();
    }

    // --- Throwing ----------------------------------------------------------------------------

    private void throwPotion(Mob mob, Player owner, LivingEntity target, PotionType type, boolean harmful) {
        ItemStack potionItem = new ItemStack(Material.SPLASH_POTION);
        PotionMeta meta = (PotionMeta) potionItem.getItemMeta();
        meta.setBasePotionType(type); // the chosen variant carries its own effects, amplifier, and duration
        potionItem.setItemMeta(meta);

        // Witch-style lob: aim a little above the target's feet and add lift with distance so the arc lands.
        Location eye = mob.getEyeLocation();
        Vector direction = target.getLocation().add(0, 0.8, 0).subtract(eye).toVector();
        double horizontal = Math.sqrt(direction.getX() * direction.getX() + direction.getZ() * direction.getZ());
        direction.setY(direction.getY() + horizontal * 0.2);
        if (direction.lengthSquared() < 0.01) {
            direction = new Vector(0, 0.1, 0);
        }
        String ownerId = owner.getUniqueId().toString();
        mob.launchProjectile(ThrownPotion.class, direction.normalize().multiply(0.75), potion -> {
            potion.setItem(potionItem);
            PersistentDataContainer pdc = potion.getPersistentDataContainer();
            pdc.set(POTION_OWNER_KEY, PersistentDataType.STRING, ownerId);
            pdc.set(POTION_HARMFUL_KEY, PersistentDataType.BYTE, (byte) (harmful ? 1 : 0));
        });
        mob.swingMainHand();
    }

    private void warnNoStock(Player owner, PotionType type) {
        if (warnedNoStock) {
            return;
        }
        warnedNoStock = true;
        Component potion = type != null ? Component.text(prettyName(type)) : Component.text("a potion");
        owner.sendMessage(Locale.getFormattedComponent(
                "Message.Skill.Potion.NoStock", pet.getOwner(), pet.getDisplayName(), potion));
    }

    // --- Upgrade parsing ---------------------------------------------------------------------

    private static PotionUpgrade parseUpgrade(JsonObject json) {
        PotionUpgrade upgrade = new PotionUpgrade();
        JsonElement potions = UpgradeParsers.get(json, "potions");
        if (potions != null && potions.isJsonArray()) {
            for (JsonElement element : potions.getAsJsonArray()) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject row = element.getAsJsonObject();
                JsonElement typeElement = UpgradeParsers.get(row, "type");
                if (typeElement == null || !typeElement.isJsonPrimitive()) {
                    continue;
                }
                PotionType type = resolvePotion(typeElement.getAsString());
                if (type == null) {
                    continue; // unknown/removed potion — skip rather than crash the whole tree
                }
                int cooldown = parsePlainInt(UpgradeParsers.get(row, "cooldown"), DEFAULT_COOLDOWN);
                upgrade.addEntry(new Potion.Entry(type, Math.max(1, cooldown)));
            }
        }
        upgrade.setMaterializeModifier(UpgradeParsers.parseBoolean(UpgradeParsers.get(json, "materialize")));
        return upgrade;
    }

    private static PotionType resolvePotion(String key) {
        NamespacedKey namespaced = NamespacedKey.fromString(key);
        return namespaced == null ? null : Registry.POTION.get(namespaced);
    }

    private static int parsePlainInt(JsonElement element, int fallback) {
        if (element instanceof JsonPrimitive primitive) {
            try {
                if (primitive.isNumber()) {
                    return primitive.getAsInt();
                }
                if (primitive.isString()) {
                    return Integer.parseInt(primitive.getAsString().trim());
                }
            } catch (NumberFormatException ignored) {
                // fall through to the default
            }
        }
        return fallback;
    }

    /** Every base potion type the server knows that actually has an effect — the editor's choices. */
    private static List<String> potionCatalog() {
        List<String> catalog = new ArrayList<>();
        try {
            for (PotionType type : Registry.POTION) {
                if (!type.getPotionEffects().isEmpty()) {
                    catalog.add(type.getKey().toString());
                }
            }
            catalog.sort(null);
        } catch (RuntimeException ignored) {
            // registry not ready at class-load — the editor just shows an empty picker, no crash
        }
        return catalog;
    }

    private static String prettyName(PotionType type) {
        return type.getKey().getKey().replace('_', ' ');
    }

    private static Attribute resolveMaxHealth() {
        Attribute attribute = Registry.ATTRIBUTE.get(NamespacedKey.minecraft("max_health"));
        if (attribute == null) {
            attribute = Registry.ATTRIBUTE.get(NamespacedKey.minecraft("generic.max_health"));
        }
        return attribute;
    }
}
