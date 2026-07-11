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
import de.Keyle.MyPet.api.MyPetGlobal;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.gui.MenuId;
import de.Keyle.MyPet.api.gui.MenuIds;
import de.Keyle.MyPet.api.skill.SkillState;
import de.Keyle.MyPet.api.skill.UpgradeComputer;
import de.Keyle.MyPet.api.skill.skills.Beacon;
import de.Keyle.MyPet.api.util.locale.Locale;
import net.kyori.adventure.text.Component;
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

        try {
            Class<?> contextType = Class.forName("de.Keyle.MyPet.gui.context.BeaconContext");
            Object context = contextType.getConstructor(Player.class, Pet.class).newInstance(player, pet);
            @SuppressWarnings("unchecked")
            MenuId<Object> id = (MenuId<Object>) (MenuId<?>) MenuIds.BEACON;
            MyPetApi.getGuiService().openMenu(player, id, context);
        } catch (Exception e) {
            return false;
        }
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
