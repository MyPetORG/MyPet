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

import de.Keyle.MyPet.MyPetApi;
import com.google.gson.JsonObject;
import de.Keyle.MyPet.api.MyPetGlobal;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.gui.ClickPayload;
import de.Keyle.MyPet.api.gui.ItemAppearance;
import de.Keyle.MyPet.api.gui.MenuHandler;
import de.Keyle.MyPet.api.gui.MenuId;
import de.Keyle.MyPet.api.gui.MenuIds;
import de.Keyle.MyPet.api.gui.MenuInstance;
import de.Keyle.MyPet.api.skill.SkillState;
import de.Keyle.MyPet.api.skill.SkillStateCodec;
import de.Keyle.MyPet.api.skill.SkillStateCodecs;
import de.Keyle.MyPet.api.skill.SkillUpgrades;
import de.Keyle.MyPet.api.skill.UpgradeComputer;
import de.Keyle.MyPet.api.skill.UpgradeParsers;
import de.Keyle.MyPet.api.skill.UpgradeSchema;
import de.Keyle.MyPet.api.skill.skills.Beacon;
import de.Keyle.MyPet.api.util.locale.Locale;
import de.Keyle.MyPet.skill.upgrades.BeaconUpgrade;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.kyori.adventure.nbt.StringBinaryTag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class BeaconImpl extends AbstractSkill implements Beacon {

    public static final SkillUpgrades UPGRADES = SkillUpgrades.of(Beacon.class,
            UpgradeSchema.builder()
                    .number("range").label("Range (blocks)").cumulative()
                    .integer("duration").label("Duration (s)").cumulative()
                    .integer("count").label("Count (simultaneous buffs)").cumulative()
                    .group("buffs", g -> g
                            .integer("absorption").label("Absorption").cumulative()
                            .bool("fireresistance").label("Fire Resistance")
                            .integer("haste").label("Haste").cumulative()
                            .bool("luck").label("Luck")
                            .bool("nightvision").label("Night Vision")
                            .integer("resistance").label("Resistance").cumulative()
                            .integer("speed").label("Speed").cumulative()
                            .integer("strength").label("Strength").cumulative()
                            .bool("waterbreathing").label("Water Breathing")
                            .integer("regeneration").label("Regeneration").cumulative()
                            .bool("invisibility").label("Invisibility")
                            .integer("jumpboost").label("Jump Boost").cumulative())
                    .label("Buffs")
                    .build(), json -> {
        JsonObject buffs = (JsonObject) UpgradeParsers.get(json, "buffs");
        return new BeaconUpgrade()
                .setRangeModifier(UpgradeParsers.parseNumber(UpgradeParsers.get(json, "range")))
                .setDurationModifier(UpgradeParsers.parseInteger(UpgradeParsers.get(json, "duration")))
                .setNumberOfBuffsModifier(UpgradeParsers.parseInteger(UpgradeParsers.get(json, "count")))
                .setAbsorptionModifier(UpgradeParsers.parseInteger(UpgradeParsers.get(buffs, "absorption")))
                .setFireResistanceModifier(UpgradeParsers.parseBoolean(UpgradeParsers.get(buffs, "fireresistance")))
                .setHasteModifier(UpgradeParsers.parseInteger(UpgradeParsers.get(buffs, "haste")))
                .setLuckModifier(UpgradeParsers.parseBoolean(UpgradeParsers.get(buffs, "luck")))
                .setNightVisionModifier(UpgradeParsers.parseBoolean(UpgradeParsers.get(buffs, "nightvision")))
                .setResistanceModifier(UpgradeParsers.parseInteger(UpgradeParsers.get(buffs, "resistance")))
                .setSpeedModifier(UpgradeParsers.parseInteger(UpgradeParsers.get(buffs, "speed")))
                .setStrengthModifier(UpgradeParsers.parseInteger(UpgradeParsers.get(buffs, "strength")))
                .setWaterBreathingModifier(UpgradeParsers.parseBoolean(UpgradeParsers.get(buffs, "waterbreathing")))
                .setRegenerationModifier(UpgradeParsers.parseInteger(UpgradeParsers.get(buffs, "regeneration")))
                .setInvisibilityModifier(UpgradeParsers.parseBoolean(UpgradeParsers.get(buffs, "invisibility")))
                .setJumpBoostModifier(UpgradeParsers.parseInteger(UpgradeParsers.get(buffs, "jumpboost")));
    });

    public static final SkillStateCodecs STATE_CODEC = SkillStateCodecs.of(Beacon.class, Beacon.State.class,
            new SkillStateCodec<>() {
                @Override
                public CompoundBinaryTag write(Beacon.State state) {
                    ListBinaryTag.Builder<StringBinaryTag> buffsBuilder = ListBinaryTag.builder(BinaryTagTypes.STRING);
                    for (Buff buff : state.buffs()) {
                        buffsBuilder.add(StringBinaryTag.stringBinaryTag(buff.getName()));
                    }
                    return CompoundBinaryTag.builder()
                            .put("Buffs", buffsBuilder.build())
                            .putBoolean("Active", state.active())
                            .putString("Receiver", state.receiver().name())
                            .build();
                }

                @Override
                public Optional<Beacon.State> read(CompoundBinaryTag compound) {
                    if (compound.keySet().isEmpty()) return Optional.empty();
                    List<Buff> buffs = new ArrayList<>();
                    if (compound.keySet().contains("Buffs")) {
                        ListBinaryTag list = compound.getList("Buffs", BinaryTagTypes.STRING);
                        for (int i = 0; i < list.size(); i++) {
                            Buff b = Buff.getByName(list.getString(i));
                            if (b != null) buffs.add(b);
                        }
                    }
                    boolean active = compound.keySet().contains("Active") && compound.getBoolean("Active");
                    // Receiver is enum.valueOf — tolerate stale or unknown names by
                    // falling back to Owner. The pre-codec live-skill load lacked
                    // this catch and would propagate IllegalArgumentException up
                    // through pet activation; consolidating to one codec lets the
                    // lenient (parser) behavior win for both contexts.
                    BuffReceiver receiver = BuffReceiver.Owner;
                    if (compound.keySet().contains("Receiver")) {
                        try {
                            receiver = BuffReceiver.valueOf(compound.getString("Receiver"));
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                    return Optional.of(new Beacon.State(List.copyOf(buffs), active, receiver));
                }
            });

    /**
     * GUI handler for the Beacon buff-selection menu. The buff buttons are rendered as a
     * single {@code paginated-list} section ({@code "buffs"}) that the handler populates
     * dynamically from {@link #getAvailableBuffs()}. The receiver and toggle slots are
     * stateful per pet; the confirm slot persists state and closes.
     */
    public static final class BeaconMenuHandler implements MenuHandler<Beacon.MenuContext> {

        @SuppressWarnings("unchecked")
        @Override public MenuId<Beacon.MenuContext> id() {
            return (MenuId<Beacon.MenuContext>) MenuIds.BEACON;
        }

        @Override
        public void onOpen(MenuInstance instance, Beacon.MenuContext context) {
            BeaconImpl beacon = beaconSkill(context);
            if (beacon == null) { instance.close(); return; }

            // Drop stale selections from a previous skilltree configuration so schedule()
            // doesn't wipe everything when size > current limit.
            beacon.pruneUnavailableBuffs();

            instance.setSlotState("receiver", beacon.getReceiverModeStateName());
            instance.setSlotState("toggle",   beacon.isEnabled() ? "on" : "off");
        }

        @Override
        public void onClick(MenuInstance instance, String sectionId, ClickPayload payload) {
            Beacon.MenuContext ctx = (Beacon.MenuContext) instance.context();
            BeaconImpl beacon = beaconSkill(ctx);
            if (beacon == null) { instance.close(); return; }

            if ("buffs".equals(sectionId)) {
                List<Buff> available = beacon.getAvailableBuffs();
                int idx = payload.itemIndex();
                if (idx < 0 || idx >= available.size()) return;
                Buff clicked = available.get(idx);
                int limit = beacon.getBuffLimit();

                if (beacon.isBuffEnabled(clicked)) {
                    beacon.setBuffEnabled(clicked, false);
                } else if (limit <= 1) {
                    // Single-buff mode: clear all available buffs first.
                    for (Buff other : available) {
                        if (beacon.isBuffEnabled(other)) beacon.setBuffEnabled(other, false);
                    }
                    beacon.setBuffEnabled(clicked, true);
                } else if (beacon.getSelectedBuffCount() < limit) {
                    beacon.setBuffEnabled(clicked, true);
                }
                // else: at limit — silently ignored.
                instance.refreshSection("buffs");
            } else if ("receiver".equals(sectionId)) {
                String next = beacon.cycleReceiverMode();
                instance.setSlotState("receiver", next);
            } else if ("toggle".equals(sectionId)) {
                boolean now = !beacon.isEnabled();
                beacon.setActive(now);
                instance.setSlotState("toggle", now ? "on" : "off");
            } else if ("confirm".equals(sectionId)) {
                beacon.persist();
                instance.close();
            }
        }

        @Override
        public List<?> templateItems(Beacon.MenuContext context, String sectionId) {
            if (!"buffs".equals(sectionId)) return List.of();
            BeaconImpl beacon = beaconSkill(context);
            if (beacon == null) return List.of();
            return beacon.getAvailableBuffs();
        }

        @Override
        public TagResolver placeholders(Beacon.MenuContext context, String sectionId, int itemIndex) {
            // The receiver slot's title leads with a label, so supply it as a leaf
            // translatable; a leading <lang:Key> would swallow the trailing value.
            TagResolver receiverLabel = Placeholder.component("receiver_label",
                Component.translatable("Gui.Beacon.Receiver.Label"));
            if (!"buffs".equals(sectionId) || itemIndex < 0) return receiverLabel;
            BeaconImpl beacon = beaconSkill(context);
            if (beacon == null) return receiverLabel;
            List<Buff> available = beacon.getAvailableBuffs();
            if (itemIndex >= available.size()) return receiverLabel;
            Buff buff = available.get(itemIndex);
            // Vanilla effect translation key resolves client-side against the player's
            // selected language. PotionEffectType implements Translatable on Paper 1.20+.
            return TagResolver.builder()
                .resolver(receiverLabel)
                .resolver(Placeholder.component("buff_name",
                    Component.translatable(buff.getPotionEffectType().translationKey())))
                .build();
        }

        @Override
        public ItemAppearance customizeTemplateItem(Beacon.MenuContext context, String sectionId,
                                                    int itemIndex, ItemAppearance template) {
            if (!"buffs".equals(sectionId)) return template;
            BeaconImpl beacon = beaconSkill(context);
            if (beacon == null) return template;
            List<Buff> available = beacon.getAvailableBuffs();
            if (itemIndex < 0 || itemIndex >= available.size()) return template;
            Buff buff = available.get(itemIndex);
            boolean selected = beacon.isBuffEnabled(buff);
            return new ItemAppearance(
                template.material(),
                template.title(),
                template.lore(),
                selected,
                template.amount(),
                template.customModelData(),
                template.headSkin(),
                buff.getPotionEffectType().getColor()
            );
        }

        private static BeaconImpl beaconSkill(Beacon.MenuContext ctx) {
            return ctx.pet().getSkills().get(BeaconImpl.class);
        }
    }

    private static final Set<Buff> BOOLEAN_BUFFS = EnumSet.of(
            Buff.FireResistance, Buff.WaterBreathing, Buff.Invisibility, Buff.NightVision, Buff.Luck);

    // INSTANT_EFFECT requires a Particle.Spell data object on Paper 1.21.9+,
    // but the class doesn't exist on 1.20.5-1.21.8. Declared as Object so
    // class loading of BeaconImpl doesn't fail on older versions.
    private static final Object INSTANT_EFFECT_DATA = createInstantEffectData();

    private static Object createInstantEffectData() {
        try {
            Class<?> spellClass = Class.forName("org.bukkit.Particle$Spell");
            return spellClass.getConstructor(Color.class, float.class)
                    .newInstance(Color.WHITE, 1.0F);
        } catch (Throwable t) {
            return null;
        }
    }

    private static void spawnBeaconParticle(Player player) {
        Location loc = player.getLocation().add(0, 1, 0);
        if (INSTANT_EFFECT_DATA != null) {
            player.getWorld().spawnParticle(Particle.INSTANT_EFFECT, loc, 5, 0.2F, 0.2F, 0.2F, 0.1F, INSTANT_EFFECT_DATA);
        } else {
            player.getWorld().spawnParticle(Particle.INSTANT_EFFECT, loc, 5, 0.2F, 0.2F, 0.2F, 0.1F);
        }
    }


    protected UpgradeComputer<Integer> duration = new UpgradeComputer<>(0);
    protected UpgradeComputer<Number> range = new UpgradeComputer<>(0);
    protected UpgradeComputer<Integer> selectableBuffs = new UpgradeComputer<>(0);
    protected Map<Buff, UpgradeComputer<?>> buffLevel = new HashMap<>();
    protected boolean active = false;
    protected int hungerDecreaseTimer;
    protected BuffReceiver receiver = BuffReceiver.Owner;
    protected int beaconTimer = 0;
    protected Set<Buff> selectedBuffs = new HashSet<>();

    public BeaconImpl(Pet pet) {
        super(pet);
        hungerDecreaseTimer = MyPetGlobal.Skilltree.Skill.Beacon.HUNGER_DECREASE_TIME.get();

        for (Buff buff : Buff.values()) {
            if (BOOLEAN_BUFFS.contains(buff)) {
                UpgradeComputer<Boolean> boolComputer = new UpgradeComputer<>(false);
                boolComputer.addCallback((newValue, reason) -> {
                    if (reason == UpgradeComputer.CallbackReason.Remove && !newValue) {
                        selectedBuffs.remove(buff);
                    }
                });
                buffLevel.put(buff, boolComputer);
            } else {
                UpgradeComputer<Integer> intComputer = new UpgradeComputer<>(0);
                intComputer.addCallback((newValue, reason) -> {
                    if (reason == UpgradeComputer.CallbackReason.Remove && newValue == 0) {
                        selectedBuffs.remove(buff);
                    }
                });
                buffLevel.put(buff, intComputer);
            }
        }
    }

    public boolean isActive() {
        if (selectableBuffs.getValue() == 0 || range.getValue().doubleValue() == 0) {
            return false;
        }
        for (UpgradeComputer<?> amp : buffLevel.values()) {
            if (amp.getValue() instanceof Boolean b) {
                if (b) {
                    return duration.getValue() > 0;
                }
            } else if (amp.getValue() instanceof Integer i) {
                if (i > 0) {
                    return duration.getValue() > 0;
                }
            }
        }
        return false;
    }

    @Override
    public boolean activate() {
        Player player = pet.getOwner().getPlayer();
        if (player == null) {
            return false;
        }

        @SuppressWarnings("unchecked")
        MenuId<Beacon.MenuContext> id = (MenuId<Beacon.MenuContext>) (MenuId<?>) MenuIds.BEACON;
        MyPetApi.getGuiService().openMenu(player, id, new Beacon.MenuContext(player, pet));
        return true;
    }

    @Override
    public void reset() {
        duration.removeAllUpgrades();
        range.removeAllUpgrades();
        selectableBuffs.removeAllUpgrades();
        selectedBuffs.clear();
        buffLevel.values().forEach(UpgradeComputer::removeAllUpgrades);
        beaconTimer = 0;
        hungerDecreaseTimer = MyPetGlobal.Skilltree.Skill.Beacon.HUNGER_DECREASE_TIME.get();
        receiver = BuffReceiver.Owner;
        active = false;
    }

    // -----------------------------------------------------------------------
    // Menu-handler API
    // -----------------------------------------------------------------------

    /** Returns whether the given buff is currently selected by the player. */
    public boolean isBuffEnabled(Buff buff) {
        return selectedBuffs.contains(buff);
    }

    /** Sets whether the given buff is selected. */
    public void setBuffEnabled(Buff buff, boolean v) {
        if (v) {
            selectedBuffs.add(buff);
        } else {
            selectedBuffs.remove(buff);
        }
    }

    /** Sets whether the beacon effect is active. */
    public void setActive(boolean v) {
        this.active = v;
    }

    /**
     * Returns the player's toggle preference for the beacon (independent of
     * {@link #isActive()}, which is a skill-capability check based on upgrades).
     * Use this in the menu when reading/writing the on/off state of the toggle button.
     */
    public boolean isEnabled() {
        return this.active;
    }

    /** The maximum number of buffs the player may have selected at once (from upgrades). */
    public int getBuffLimit() {
        return selectableBuffs.getValue();
    }

    /** The number of buffs currently selected. */
    public int getSelectedBuffCount() {
        return selectedBuffs.size();
    }

    /**
     * True if the skilltree has granted access to {@code buff} — its upgrade
     * computer is registered and resolves to a non-zero level (or {@code true}
     * for boolean buffs).
     */
    public boolean isBuffAvailable(Buff buff) {
        UpgradeComputer<?> computer = buffLevel.get(buff);
        if (computer == null) return false;
        Object value = computer.getValue();
        if (value instanceof Boolean b) return b;
        if (value instanceof Number n) return n.intValue() > 0;
        return false;
    }

    /**
     * Returns the menu-ordered list of buffs the skilltree currently grants.
     * Order follows the {@link Buff} enum declaration so the menu layout is
     * driven entirely by the canonical buff list.
     */
    public List<Buff> getAvailableBuffs() {
        List<Buff> out = new ArrayList<>();
        for (Buff buff : Buff.values()) {
            if (isBuffAvailable(buff)) out.add(buff);
        }
        return out;
    }

    /**
     * Prune {@link #selectedBuffs} of any entries that are no longer available
     * (e.g. due to skilltree changes) or that exceed the current
     * {@link #getBuffLimit()}. Idempotent. Returns the number of buffs removed.
     *
     * <p>Without this, the {@link #schedule()} method's defensive
     * {@code selectedBuffs.clear()} (triggered when {@code size > limit}) would
     * wipe the entire selection on the next tick, preventing any effects from
     * being applied.
     */
    public int pruneUnavailableBuffs() {
        int removed = 0;
        Iterator<Buff> it = selectedBuffs.iterator();
        while (it.hasNext()) {
            if (!isBuffAvailable(it.next())) {
                it.remove();
                removed++;
            }
        }
        int limit = getBuffLimit();
        while (selectedBuffs.size() > limit && !selectedBuffs.isEmpty()) {
            Buff first = selectedBuffs.iterator().next();
            selectedBuffs.remove(first);
            removed++;
        }
        return removed;
    }

    /** Returns the receiver mode state name in lowercase (e.g. {@code "owner"}, {@code "party"}, {@code "everyone"}). */
    public String getReceiverModeStateName() {
        return receiver.name().toLowerCase();
    }

    /** Cycles the receiver mode to the next value and returns the new state name. */
    public String cycleReceiverMode() {
        BuffReceiver[] values = BuffReceiver.values();
        receiver = values[(receiver.ordinal() + 1) % values.length];
        return getReceiverModeStateName();
    }

    /** Confirms the current in-memory state as persisted. Mutations via the setter methods are already live; this is a no-op. */
    public void persist() {
        // Mutations via isBuffEnabled/setBuffEnabled, setActive, cycleReceiverMode
        // are applied directly to the live fields; nothing additional to commit.
    }

    public Component toPrettyComponent(String locale) {
        Component result = Component.empty();
        boolean first = true;
        for (Buff buff : Buff.values()) {
            if (getBuffLevel(buff) > 0) {
                if (!first) {
                    result = result.append(Component.text(", "));
                }
                result = result
                        .append(Locale.getComponent("Name." + buff.getName(), locale).color(net.kyori.adventure.text.format.NamedTextColor.GOLD))
                        .append(Component.text(" " + Util.decimal2roman(getBuffLevel(buff))).color(net.kyori.adventure.text.format.NamedTextColor.GRAY));
                first = false;
            }
        }
        return result;
    }

    @Override
    public Component[] getUpgradeMessage() {
        return new Component[]{
                Locale.getFormattedComponent("Message.Skill.Beacon.Upgrade", pet.getOwner().getLanguage(), pet.getDisplayName(), String.format("%1.2f", getRange().getValue().doubleValue()), getDuration().getValue()),
                Component.text(" ").append(toPrettyComponent(pet.getOwner().getLanguage()))
        };
    }

    public void schedule() {
        if (pet.getStatus() == Pet.PetState.Here && isActive() && active && !selectedBuffs.isEmpty() && --beaconTimer <= 0) {
            beaconTimer = 2;

            // Safety check - pet could despawn between status check and location retrieval
            if (!this.pet.getLocation().isPresent()) {
                return;
            }
            Location petLocation = this.pet.getLocation().get();

            // Check if beacon is allowed at pet's location
            if (!MyPetApi.getHookHelper().isBeaconAllowed(petLocation)) {
                return;
            }

            double range = this.range.getValue().doubleValue();

            if (MyPetGlobal.HungerSystem.USE_HUNGER_SYSTEM.get() && MyPetGlobal.HungerSystem.AFFECT_BEACON_RANGE.get()) {
                range *= (Math.log10(pet.getSaturation()) / 2);
            }

            // Apply region range multiplier
            range *= MyPetApi.getHookHelper().getBeaconRangeMultiplier(petLocation);

            if (range < 0.7) {
                return;
            }

            if (selectedBuffs.isEmpty()) {
                return;
            }
            if (selectedBuffs.size() > selectableBuffs.getValue()) {
                selectedBuffs.clear();
            }

            double rangeSquared = range * range;
            petLocation.getWorld().spawnParticle(Particle.WITCH, petLocation.clone().add(0, 1, 0), 5, 0.2F, 0.2F, 0.2F, 0.1F);

            List<Player> members = null;
            if (MyPetGlobal.Skilltree.Skill.Beacon.PARTY_SUPPORT.get() && receiver == BuffReceiver.Party) {
                members = MyPetApi.getHookHelper().getPartyMembers(getPet().getOwner().getPlayer());
            }

            // Apply region duration multiplier
            int duration = (int) (this.duration.getValue() * 20 * MyPetApi.getHookHelper().getBeaconDurationMultiplier(petLocation));

            // Get region amplifier modifier
            int amplifierMod = MyPetApi.getHookHelper().getBeaconAmplifierModifier(petLocation);

            List<PotionEffect> potionEffects = new ArrayList<>();
            for (Buff buff : selectedBuffs) {
                int amplification = Math.max(0, getBuffLevel(buff) - 1 + amplifierMod);
                PotionEffect effect = new PotionEffect(buff.getPotionEffectType(), duration, amplification, true, true);
                potionEffects.add(effect);
            }

            targetLoop:
            for (Player player : petLocation.getNearbyPlayers(range)) {
                // getNearbyPlayers scans a bounding box; keep the spherical filter.
                if (player.getLocation().distanceSquared(petLocation) > rangeSquared) {
                    continue;
                } else if (player.getGameMode() == GameMode.SPECTATOR) {
                    continue;
                } else if (MyPetApi.getHookHelper().isVanished(player)) {
                    continue;
                }

                boolean isOwner = pet.getOwner().getPlayer().equals(player);

                // Check self-deny for owner
                if (isOwner && !MyPetApi.getHookHelper().isBeaconSelfAllowed(player.getLocation())) {
                    continue;
                }

                // Check share-deny for non-owners (both pet location and target location)
                if (!isOwner) {
                    if (!MyPetApi.getHookHelper().isBeaconShareAllowed(petLocation) ||
                        !MyPetApi.getHookHelper().isBeaconShareAllowed(player.getLocation())) {
                        continue;
                    }
                }

                switch (receiver) {
                    case Owner:
                        if (!pet.getOwner().equals(player)) {
                            continue;
                        } else {
                            for (PotionEffect effect : potionEffects) {
                                player.addPotionEffect(effect, true);
                            }
                            if (!player.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                                spawnBeaconParticle(player);
                            }
                            break targetLoop;
                        }
                    case Everyone:
                        for (PotionEffect effect : potionEffects) {
                            player.addPotionEffect(effect, true);
                        }
                        if (!player.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                            spawnBeaconParticle(player);
                        }
                        break;
                    case Party:
                        if (MyPetGlobal.Skilltree.Skill.Beacon.PARTY_SUPPORT.get() && members != null) {
                            if (members.contains(player)) {
                                for (PotionEffect effect : potionEffects) {
                                    player.addPotionEffect(effect, true);
                                }
                                if (!player.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                                    spawnBeaconParticle(player);
                                }
                            }
                            break;
                        } else {
                            receiver = BuffReceiver.Owner;
                            break targetLoop;
                        }
                }
            }

            if (MyPetGlobal.HungerSystem.USE_HUNGER_SYSTEM.get() && MyPetGlobal.Skilltree.Skill.Beacon.HUNGER_DECREASE_TIME.get() > 0 && hungerDecreaseTimer-- < 0) {
                pet.decreaseSaturation(1);
                hungerDecreaseTimer = MyPetGlobal.Skilltree.Skill.Beacon.HUNGER_DECREASE_TIME.get();
            }
        }
    }

    public UpgradeComputer<Integer> getDuration() {
        return duration;
    }

    @Override
    public UpgradeComputer<Integer> getNumberOfBuffs() {
        return selectableBuffs;
    }

    @Override
    public UpgradeComputer<Number> getRange() {
        return range;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> UpgradeComputer<T> getBuff(Buff buff) {
        return (UpgradeComputer<T>) this.buffLevel.get(buff);
    }

    public int getBuffLevel(Buff buff) {
        UpgradeComputer<?> buffLevel = this.buffLevel.get(buff);
        if (buffLevel.getValue() instanceof Boolean b) {
            return b ? 1 : 0;
        } else if (buffLevel.getValue() instanceof Integer i) {
            return i;
        }
        return 0;
    }

    @Override
    public void applyState(SkillState state) {
        if (state instanceof State s) {
            selectedBuffs.clear();
            selectedBuffs.addAll(s.buffs());
            this.active = s.active();
            this.receiver = s.receiver();
        }
    }

    @Override
    public Optional<State> getState() {
        return Optional.of(new State(List.copyOf(selectedBuffs), active, receiver));
    }
}
