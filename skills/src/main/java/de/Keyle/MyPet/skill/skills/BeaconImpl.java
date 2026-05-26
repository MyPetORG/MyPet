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
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.gui.IconMenu;
import de.Keyle.MyPet.api.gui.IconMenuItem;
import de.Keyle.MyPet.api.skill.SkillState;
import de.Keyle.MyPet.api.skill.UpgradeComputer;
import de.Keyle.MyPet.api.skill.skills.Beacon;
import de.Keyle.MyPet.api.util.locale.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;

import static org.bukkit.Material.*;

public class BeaconImpl implements Beacon {

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
    protected Pet pet;
    protected boolean active = false;
    protected int hungerDecreaseTimer;
    protected BuffReceiver receiver = BuffReceiver.Owner;
    protected int beaconTimer = 0;
    protected Set<Buff> selectedBuffs = new HashSet<>();
    SkullMeta disabledMeta;
    SkullMeta partyMeta;
    SkullMeta everyoneMeta;
    SkullMeta ownerMeta;

    public BeaconImpl(Pet pet) {
        this.pet = pet;
        hungerDecreaseTimer = Configuration.Skilltree.Skill.Beacon.HUNGER_DECREASE_TIME;

        if (!Configuration.Skilltree.Skill.Beacon.DISABLE_HEAD_TEXTURE) {
        Material headMaterial = PLAYER_HEAD;
            // stone
            disabledMeta = createTexturedSkullMeta(headMaterial,
                    "http://textures.minecraft.net/texture/de9b8aae7f9cc76d625ccb8abc686f30d38f9e6c42533098b9ad577f91c333c");
            // globe
            everyoneMeta = createTexturedSkullMeta(headMaterial,
                    "http://textures.minecraft.net/texture/b1dd4fe4a429abd665dfdb3e21321d6efa6a6b5e7b956db9c5d59c9efab25");
            // beachball
            partyMeta = createTexturedSkullMeta(headMaterial,
                    "http://textures.minecraft.net/texture/5a5ab05ea254c32e3c48f3fdcf9fd9d77d3cba04e6b5ec2e68b3cbdcfac3fd");
            // owner skin
            ownerMeta = (SkullMeta) new ItemStack(headMaterial).getItemMeta();
            ownerMeta.setOwningPlayer(pet.getOwner().getPlayer());
        }

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

    public Pet getPet() {
        return pet;
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
    public void reset() {
        duration.removeAllUpgrades();
        range.removeAllUpgrades();
        selectableBuffs.removeAllUpgrades();
        selectedBuffs.clear();
        buffLevel.values().forEach(UpgradeComputer::removeAllUpgrades);
        beaconTimer = 0;
        hungerDecreaseTimer = Configuration.Skilltree.Skill.Beacon.HUNGER_DECREASE_TIME;
        receiver = BuffReceiver.Owner;
        active = false;
    }

    public boolean activate() {
        final Player owner = pet.getOwner().getPlayer();

        final BeaconImpl beacon = this;
        Component title = Locale.getComponent("Name.Skill.Beacon", pet.getOwner());
        IconMenu menu = new IconMenu(title, new IconMenu.OptionClickEventHandler() {

            Set<Buff> selectedBuffs = new HashSet<>(beacon.selectedBuffs);
            boolean active = beacon.active;
            private BuffReceiver receiver = beacon.receiver;

            @Override
            public void onOptionClick(IconMenu.OptionClickEvent event) {
                event.setWillClose(false);
                event.setWillDestroy(false);

                if (getPet().getStatus() != Pet.PetState.Here) {
                    return;
                }

                IconMenu menu = event.getMenu();

                switch (event.getPosition()) {
                    case 5:
                        event.setWillClose(true);
                        event.setWillDestroy(true);
                        return;
                    case 4:
                        if (active) {
                            menu.getOption(4)
                                    .setMaterial(REDSTONE_BLOCK)
                                    .setTitle(Locale.getFormattedComponent("Message.Skill.Beacon.Effect", pet.getOwner().getLanguage(), Locale.getComponent("Name.Off", pet.getOwner())).color(NamedTextColor.RED))
                                    .addLoreLine(Locale.getComponent("Message.Skill.Beacon.ClickOn", pet.getOwner()));
                            active = false;
                        } else {
                            menu.getOption(4)
                                    .setMaterial(EMERALD_BLOCK)
                                    .setTitle(Locale.getFormattedComponent("Message.Skill.Beacon.Effect", pet.getOwner().getLanguage(), Locale.getComponent("Name.On", pet.getOwner())).color(NamedTextColor.GREEN))
                                    .addLoreLine(Locale.getComponent("Message.Skill.Beacon.ClickOff", pet.getOwner()));
                            active = true;
                        }
                        menu.update();
                        break;
                    case 3:
                        beacon.active = active;
                        beacon.selectedBuffs.clear();
                        beacon.selectedBuffs.addAll(selectedBuffs);
                        beacon.receiver = receiver;
                        event.setWillClose(true);
                        event.setWillDestroy(true);
                        break;
                    case 21:
                        if (receiver != BuffReceiver.Owner) {
                            menu.getOption(21).setMeta(ownerMeta, false, false);
                            if (menu.getOption(22) != null) {
                                menu.getOption(22).setMeta(partyMeta, false, false);
                            }
                            menu.getOption(23).setMeta(disabledMeta, false, false);
                            receiver = BuffReceiver.Owner;
                            menu.update();
                        }
                        break;
                    case 22:
                        if (receiver != BuffReceiver.Party) {
                            menu.getOption(21).setMeta(disabledMeta, false, false);
                            menu.getOption(22).setMeta(partyMeta, false, false);
                            menu.getOption(23).setMeta(disabledMeta, false, false);
                            receiver = BuffReceiver.Party;
                            menu.update();
                        }
                        break;
                    case 23:
                        if (receiver != BuffReceiver.Everyone) {
                            menu.getOption(21).setMeta(disabledMeta, false, false);
                            if (menu.getOption(22) != null) {
                                menu.getOption(22).setMeta(disabledMeta, false, false);
                            }
                            menu.getOption(23).setMeta(everyoneMeta, false, false);
                            receiver = BuffReceiver.Everyone;
                            menu.update();
                        }
                        break;
                    default:
                        Buff selectedBuff = Buff.getBuffAtPosition(event.getPosition());
                        if (selectedBuff != null) {
                            if (selectableBuffs.getValue() > 1) {
                                if (selectedBuffs.contains(selectedBuff)) {
                                    selectedBuffs.remove(selectedBuff);
                                    menu.getOption(selectedBuff.getPosition()).setGlowing(false);
                                    if (selectableBuffs.getValue() > selectedBuffs.size()) {
                                        menu.setOption(13, new IconMenuItem()
                                                .setMaterial(POTION)
                                                .setTitle(Locale.getFormattedComponent("Message.Skill.Beacon.RemainingBuffs", pet.getOwner().getLanguage(), selectableBuffs.getValue() - selectedBuffs.size()).color(NamedTextColor.BLUE))
                                                .setAmount(selectableBuffs.getValue() - selectedBuffs.size()));
                                    } else {
                                        menu.setOption(13, new IconMenuItem()
                                                .setMaterial(GLASS_BOTTLE)
                                                .setTitle(Locale.getFormattedComponent("Message.Skill.Beacon.RemainingBuffs", pet.getOwner().getLanguage(), 0).color(NamedTextColor.GRAY)));
                                    }
                                    menu.update();
                                } else if (selectableBuffs.getValue() > selectedBuffs.size()) {
                                    selectedBuffs.add(selectedBuff);
                                    menu.getOption(selectedBuff.getPosition()).setGlowing(true);
                                    if (selectableBuffs.getValue() > selectedBuffs.size()) {
                                        menu.setOption(13, new IconMenuItem()
                                                .setMaterial(POTION)
                                                .setTitle(Locale.getFormattedComponent("Message.Skill.Beacon.RemainingBuffs", pet.getOwner().getLanguage(), selectableBuffs.getValue() - selectedBuffs.size()).color(NamedTextColor.BLUE))
                                                .setAmount(selectableBuffs.getValue() - selectedBuffs.size()));
                                    } else {
                                        menu.setOption(13, new IconMenuItem()
                                                .setMaterial(GLASS_BOTTLE)
                                                .setTitle(Locale.getFormattedComponent("Message.Skill.Beacon.RemainingBuffs", pet.getOwner().getLanguage(), 0).color(NamedTextColor.GRAY)));
                                    }
                                    menu.update();
                                } else {
                                    break;
                                }

                                if (selectableBuffs.getValue() > selectedBuffs.size()) {
                                    menu.setOption(13, new IconMenuItem()
                                            .setMaterial(POTION)
                                            .setTitle(Locale.getFormattedComponent("Message.Skill.Beacon.RemainingBuffs", pet.getOwner().getLanguage(), selectableBuffs.getValue() - selectedBuffs.size()).color(NamedTextColor.BLUE))
                                            .setAmount(selectableBuffs.getValue() - selectedBuffs.size()));
                                } else {
                                    menu.setOption(13, new IconMenuItem()
                                            .setMaterial(GLASS_BOTTLE)
                                            .setTitle(Locale.getFormattedComponent("Message.Skill.Beacon.RemainingBuffs", pet.getOwner().getLanguage(), 0).color(NamedTextColor.GRAY)));
                                }
                            } else if (!selectedBuffs.contains(selectedBuff)) {
                                if (!selectedBuffs.isEmpty() && menu.getOption(selectedBuff.getPosition()) != null) {
                                    for (Buff buff : selectedBuffs) {
                                        IconMenuItem item = menu.getOption(buff.getPosition());
                                        if (item != null) {
                                            item.setGlowing(false);
                                        }
                                    }
                                    selectedBuffs.clear();
                                }
                                selectedBuffs.add(selectedBuff);
                                menu.getOption(selectedBuff.getPosition()).setGlowing(true);
                                menu.update();
                            }
                        }
                }
            }
        }, MyPetApi.getPlugin());

        if (beacon.active) {
            menu.setOption(4, new IconMenuItem()
                    .setMaterial(EMERALD_BLOCK)
                    .setTitle(Locale.getFormattedComponent("Message.Skill.Beacon.Effect", pet.getOwner().getLanguage(), Locale.getComponent("Name.On", pet.getOwner())).color(NamedTextColor.GREEN))
                    .addLoreLine(Locale.getComponent("Message.Skill.Beacon.ClickOff", pet.getOwner()))
            );
        } else {
            menu.setOption(4, new IconMenuItem()
                    .setMaterial(REDSTONE_BLOCK)
                    .setTitle(Locale.getFormattedComponent("Message.Skill.Beacon.Effect", pet.getOwner().getLanguage(), Locale.getComponent("Name.Off", pet.getOwner())).color(NamedTextColor.RED))
                    .addLoreLine(Locale.getComponent("Message.Skill.Beacon.ClickOn", pet.getOwner()))
            );
        }

        menu.setOption(3, new IconMenuItem()
                .setMaterial(GREEN_STAINED_GLASS_PANE)
                .setTitle(Locale.getComponent("Name.Done", pet.getOwner()).color(NamedTextColor.GREEN)));
        menu.setOption(5, new IconMenuItem()
                .setMaterial(RED_STAINED_GLASS_PANE)
                .setTitle(Locale.getComponent("Name.Cancel", pet.getOwner()).color(NamedTextColor.RED)));

        if (receiver == BuffReceiver.Owner) {
            menu.setOption(21, new IconMenuItem()
                    .setMaterial(PLAYER_HEAD)
                    .setTitle(Locale.getComponent("Name.Owner", pet.getOwner()).color(NamedTextColor.GOLD))
                    .setMeta(ownerMeta, false, false));
        } else {
            menu.setOption(21, new IconMenuItem()
                    .setMaterial(PLAYER_HEAD)
                    .setTitle(Locale.getComponent("Name.Owner", pet.getOwner()).color(NamedTextColor.GOLD))
                    .setMeta(disabledMeta, false, false));
        }
        if (Configuration.Skilltree.Skill.Beacon.PARTY_SUPPORT && MyPetApi.getHookHelper().isInParty(getPet().getOwner().getPlayer())) {
            if (receiver != BuffReceiver.Party) {
                menu.setOption(22, new IconMenuItem()
                        .setMaterial(PLAYER_HEAD)
                        .setTitle(Locale.getComponent("Name.Party", pet.getOwner()).color(NamedTextColor.GOLD))
                        .setMeta(partyMeta, false, false));
            } else {
                menu.setOption(22, new IconMenuItem()
                        .setMaterial(PLAYER_HEAD)
                        .setTitle(Locale.getComponent("Name.Party", pet.getOwner()).color(NamedTextColor.GOLD))
                        .setMeta(disabledMeta, false, false));
            }
        }
        if (receiver == BuffReceiver.Everyone) {
            menu.setOption(23, new IconMenuItem()
                    .setMaterial(PLAYER_HEAD)
                    .setTitle(Locale.getComponent("Name.Everyone", pet.getOwner()).color(NamedTextColor.GOLD))
                    .setMeta(everyoneMeta, false, false));
        } else {
            menu.setOption(23, new IconMenuItem()
                    .setMaterial(PLAYER_HEAD)
                    .setTitle(Locale.getComponent("Name.Everyone", pet.getOwner()).color(NamedTextColor.GOLD))
                    .setMeta(disabledMeta, false, false));
        }

        if (getBuffLevel(Buff.Speed) > 0) {
            menu.setOption(0, new IconMenuItem()
                    .setMaterial(LEATHER_BOOTS)
                    .setAmount(getBuffLevel(Buff.Speed))
                    .setTitle(Component.text().append(Locale.getComponent("Name." + Buff.Speed.getName(), pet.getOwner()).color(NamedTextColor.GOLD)).append(Component.text(" " + Util.decimal2roman(getBuffLevel(Buff.Speed))).color(NamedTextColor.GRAY)).build()));
        }
        if (getBuffLevel(Buff.Haste) > 0) {
            menu.setOption(9, new IconMenuItem()
                    .setMaterial(GOLDEN_PICKAXE)
                    .setAmount(getBuffLevel(Buff.Haste))
                    .setTitle(Component.text().append(Locale.getComponent("Name." + Buff.Haste.getName(), pet.getOwner()).color(NamedTextColor.GOLD)).append(Component.text(" " + Util.decimal2roman(getBuffLevel(Buff.Haste))).color(NamedTextColor.GRAY)).build()));
        }
        if (getBuffLevel(Buff.Strength) > 0) {
            menu.setOption(18, new IconMenuItem()
                    .setMaterial(DIAMOND_SWORD)
                    .setAmount(getBuffLevel(Buff.Strength))
                    .setTitle(Component.text().append(Locale.getComponent("Name." + Buff.Strength.getName(), pet.getOwner()).color(NamedTextColor.GOLD)).append(Component.text(" " + Util.decimal2roman(getBuffLevel(Buff.Strength))).color(NamedTextColor.GRAY)).build()));
        }
        if (getBuffLevel(Buff.JumpBoost) > 0) {
            menu.setOption(1, new IconMenuItem()
                    .setMaterial(FIREWORK_ROCKET)
                    .setAmount(getBuffLevel(Buff.JumpBoost))
                    .setTitle(Component.text().append(Locale.getComponent("Name." + Buff.JumpBoost.getName(), pet.getOwner()).color(NamedTextColor.GOLD)).append(Component.text(" " + Util.decimal2roman(getBuffLevel(Buff.JumpBoost))).color(NamedTextColor.GRAY)).build()));
        }
        if (getBuffLevel(Buff.Regeneration) > 0) {
            menu.setOption(10, new IconMenuItem()
                    .setMaterial(APPLE)
                    .setAmount(getBuffLevel(Buff.Regeneration))
                    .setTitle(Component.text().append(Locale.getComponent("Name." + Buff.Regeneration.getName(), pet.getOwner()).color(NamedTextColor.GOLD)).append(Component.text(" " + Util.decimal2roman(getBuffLevel(Buff.Regeneration))).color(NamedTextColor.GRAY)).build()));
        }
        if (getBuffLevel(Buff.Resistance) > 0) {
            menu.setOption(19, new IconMenuItem()
                    .setMaterial(DIAMOND_CHESTPLATE)
                    .setAmount(getBuffLevel(Buff.Resistance))
                    .setTitle(Component.text().append(Locale.getComponent("Name." + Buff.Resistance.getName(), pet.getOwner()).color(NamedTextColor.GOLD)).append(Component.text(" " + Util.decimal2roman(getBuffLevel(Buff.Resistance))).color(NamedTextColor.GRAY)).build()));
        }
        if (getBuffLevel(Buff.FireResistance) > 0) {
            menu.setOption(7, new IconMenuItem()
                    .setMaterial(LAVA_BUCKET)
                    .setAmount(getBuffLevel(Buff.FireResistance))
                    .setTitle(Component.text().append(Locale.getComponent("Name." + Buff.FireResistance.getName(), pet.getOwner()).color(NamedTextColor.GOLD)).append(Component.text(" " + Util.decimal2roman(getBuffLevel(Buff.FireResistance))).color(NamedTextColor.GRAY)).build()));
        }
        if (getBuffLevel(Buff.WaterBreathing) > 0) {
            menu.setOption(16, new IconMenuItem()
                    .setMaterial(PUFFERFISH)
                    .setAmount(getBuffLevel(Buff.WaterBreathing))
                    .setTitle(Component.text().append(Locale.getComponent("Name." + Buff.WaterBreathing.getName(), pet.getOwner()).color(NamedTextColor.GOLD)).append(Component.text(" " + Util.decimal2roman(getBuffLevel(Buff.WaterBreathing))).color(NamedTextColor.GRAY)).build()));
        }
        if (getBuffLevel(Buff.Invisibility) > 0) {
            menu.setOption(25, new IconMenuItem()
                    .setMaterial(ENDER_EYE)
                    .setAmount(getBuffLevel(Buff.Invisibility))
                    .setTitle(Component.text().append(Locale.getComponent("Name." + Buff.Invisibility.getName(), pet.getOwner()).color(NamedTextColor.GOLD)).append(Component.text(" " + Util.decimal2roman(getBuffLevel(Buff.Invisibility))).color(NamedTextColor.GRAY)).build()));
        }
        if (getBuffLevel(Buff.NightVision) > 0) {
            menu.setOption(8, new IconMenuItem()
                    .setMaterial(TORCH)
                    .setAmount(getBuffLevel(Buff.NightVision))
                    .setTitle(Component.text().append(Locale.getComponent("Name." + Buff.NightVision.getName(), pet.getOwner()).color(NamedTextColor.GOLD)).append(Component.text(" " + Util.decimal2roman(getBuffLevel(Buff.NightVision))).color(NamedTextColor.GRAY)).build()));
        }
        if (getBuffLevel(Buff.Luck) > 0) {
            menu.setOption(17, new IconMenuItem()
                    .setMaterial(DIAMOND)
                    .setAmount(getBuffLevel(Buff.Luck))
                    .setTitle(Component.text().append(Locale.getComponent("Name." + Buff.Luck.getName(), pet.getOwner()).color(NamedTextColor.GOLD)).append(Component.text(" " + Util.decimal2roman(getBuffLevel(Buff.Luck))).color(NamedTextColor.GRAY)).build()));
        }
        if (getBuffLevel(Buff.Absorption) > 0) {
            menu.setOption(26, new IconMenuItem()
                    .setMaterial(SPONGE)
                    .setAmount(getBuffLevel(Buff.Absorption))
                    .setTitle(Component.text().append(Locale.getComponent("Name." + Buff.Absorption.getName(), pet.getOwner()).color(NamedTextColor.GOLD)).append(Component.text(" " + Util.decimal2roman(getBuffLevel(Buff.Absorption))).color(NamedTextColor.GRAY)).build()));
        }

        Iterator<Buff> iterator = selectedBuffs.iterator();
        while (iterator.hasNext()) {
            Buff buff = iterator.next();
            if (buffLevel.containsKey(buff) && getBuffLevel(buff) > 0) {
                menu.getOption(buff.getPosition()).setGlowing(true);
            } else {
                iterator.remove();
            }
        }

        if (selectableBuffs.getValue() > 1) {
            if (selectableBuffs.getValue() > selectedBuffs.size()) {
                menu.setOption(13, new IconMenuItem()
                        .setMaterial(POTION)
                        .setTitle(Locale.getFormattedComponent("Message.Skill.Beacon.RemainingBuffs", pet.getOwner().getLanguage(), selectableBuffs.getValue() - selectedBuffs.size()).color(NamedTextColor.BLUE))
                        .setAmount(selectableBuffs.getValue() - selectedBuffs.size()));
            } else {
                menu.setOption(13, new IconMenuItem()
                        .setMaterial(GLASS_BOTTLE)
                        .setTitle(Locale.getFormattedComponent("Message.Skill.Beacon.RemainingBuffs", pet.getOwner().getLanguage(), 0).color(NamedTextColor.GRAY)));
            }
        }

        menu.open(owner);

        return true;
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

            if (Configuration.HungerSystem.USE_HUNGER_SYSTEM && Configuration.HungerSystem.AFFECT_BEACON_RANGE) {
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

            range = range * range;
            petLocation.getWorld().spawnParticle(Particle.WITCH, petLocation.clone().add(0, 1, 0), 5, 0.2F, 0.2F, 0.2F, 0.1F);

            List<Player> members = null;
            if (Configuration.Skilltree.Skill.Beacon.PARTY_SUPPORT && receiver == BuffReceiver.Party) {
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
            for (Player player : petLocation.getWorld().getPlayers()) {
                if (player.getLocation().distanceSquared(petLocation) > range) {
                    continue;
                } else if (player.getGameMode().name().equals("SPECTATOR")) {
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
                        if (Configuration.Skilltree.Skill.Beacon.PARTY_SUPPORT && members != null) {
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

            if (Configuration.HungerSystem.USE_HUNGER_SYSTEM && Configuration.Skilltree.Skill.Beacon.HUNGER_DECREASE_TIME > 0 && hungerDecreaseTimer-- < 0) {
                pet.decreaseSaturation(1);
                hungerDecreaseTimer = Configuration.Skilltree.Skill.Beacon.HUNGER_DECREASE_TIME;
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

    private static SkullMeta createTexturedSkullMeta(Material headMaterial, String textureUrl) {
        SkullMeta meta = (SkullMeta) new ItemStack(headMaterial).getItemMeta();
        PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID(), null);
        String textureJson = "{\"textures\":{\"SKIN\":{\"url\":\"" + textureUrl + "\"}}}";
        String base64 = Base64.getEncoder().encodeToString(textureJson.getBytes());
        profile.setProperty(new ProfileProperty("textures", base64));
        meta.setPlayerProfile(profile);
        return meta;
    }

    @Override
    public Optional<State> getState() {
        return Optional.of(new State(List.copyOf(selectedBuffs), active, receiver));
    }
}
