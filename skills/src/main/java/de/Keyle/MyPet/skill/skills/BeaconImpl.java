/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2020 Keyle
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
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.gui.IconMenu;
import de.Keyle.MyPet.api.gui.IconMenuItem;
import de.Keyle.MyPet.api.skill.UpgradeComputer;
import de.Keyle.MyPet.api.skill.skills.Beacon;
import de.Keyle.MyPet.api.util.EnumSelector;
import de.Keyle.MyPet.api.util.inventory.meta.SkullMeta;
import de.Keyle.MyPet.api.util.locale.Translation;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.IntArrayBinaryTag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

import static org.bukkit.Material.*;

public class BeaconImpl implements Beacon {

    protected UpgradeComputer<Integer> duration = new UpgradeComputer<>(0);
    protected UpgradeComputer<Number> range = new UpgradeComputer<>(0);
    protected UpgradeComputer<Integer> selectableBuffs = new UpgradeComputer<>(0);
    protected Map<Buff, UpgradeComputer> buffLevel = new HashMap<>();
    protected MyPet myPet;
    protected boolean active = false;
    protected int hungerDecreaseTimer;
    protected BuffReceiver receiver = BuffReceiver.Owner;
    protected int beaconTimer = 0;
    protected Set<Buff> selectedBuffs = new HashSet<>();
    SkullMeta disabledMeta = new SkullMeta();
    SkullMeta partyMeta = new SkullMeta();
    SkullMeta everyoneMeta = new SkullMeta();
    org.bukkit.inventory.meta.SkullMeta ownerMeta;

    public BeaconImpl(MyPet myPet) {
        this.myPet = myPet;
        hungerDecreaseTimer = Configuration.Skilltree.Skill.Beacon.HUNGER_DECREASE_TIME;

        if (!Configuration.Skilltree.Skill.Beacon.DISABLE_HEAD_TEXTURE) {
            // stone
            disabledMeta.setOwner("NeverUsed0000001");
            disabledMeta.setTexture("http://textures.minecraft.net/texture/de9b8aae7f9cc76d625ccb8abc686f30d38f9e6c42533098b9ad577f91c333c");
            // globe
            everyoneMeta.setOwner("NeverUsed0000002");
            everyoneMeta.setTexture("http://textures.minecraft.net/texture/b1dd4fe4a429abd665dfdb3e21321d6efa6a6b5e7b956db9c5d59c9efab25");
            // beachball
            partyMeta.setOwner("NeverUsed0000003");
            partyMeta.setTexture("http://textures.minecraft.net/texture/5a5ab05ea254c32e3c48f3fdcf9fd9d77d3cba04e6b5ec2e68b3cbdcfac3fd");
            // owner skin
            ownerMeta = (org.bukkit.inventory.meta.SkullMeta) new ItemStack(EnumSelector.find(Material.class, "SKULL_ITEM", "PLAYER_HEAD")).getItemMeta();
            ownerMeta.setOwner(myPet.getOwner().getName());
        }

        for (Buff buff : Buff.values()) {
            UpgradeComputer upgradeComputer;
            if (buff.hasMoreThanOneLevel()) {
                upgradeComputer = new UpgradeComputer<>(0);
            } else {
                upgradeComputer = new UpgradeComputer<>(false);
            }
            buffLevel.put(buff, upgradeComputer);
            UpgradeComputer.UpgradeCallback callback = (newValue, reason) -> {
                if (reason == UpgradeComputer.CallbackReason.Remove) {
                    if (upgradeComputer.getValue() instanceof Boolean) {
                        if (!((Boolean) newValue)) {
                            selectedBuffs.remove(buff);
                        }
                    } else if (upgradeComputer.getValue() instanceof Integer) {
                        if ((Integer) newValue == 0) {
                            selectedBuffs.remove(buff);
                        }
                    }
                }
            };
            //noinspection unchecked
            upgradeComputer.addCallback(callback);
        }
    }

    public MyPet getMyPet() {
        return myPet;
    }

    public boolean isActive() {
        if (selectableBuffs.getValue() == 0 || range.getValue().doubleValue() == 0) {
            return false;
        }
        for (UpgradeComputer amp : buffLevel.values()) {
            if (amp.getValue() instanceof Boolean) {
                if ((Boolean) amp.getValue()) {
                    return duration.getValue() > 0;
                }
            } else if (amp.getValue() instanceof Integer) {
                if ((Integer) amp.getValue() > 0) {
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
        final Player owner = myPet.getOwner().getPlayer();

        final BeaconImpl beacon = this;
        Component title = Translation.getComponent("Name.Skill.Beacon", myPet.getOwner());
        IconMenu menu = new IconMenu(title, new IconMenu.OptionClickEventHandler() {

            Set<Buff> selectedBuffs = new HashSet<>(beacon.selectedBuffs);
            boolean active = beacon.active;
            private BuffReceiver receiver = beacon.receiver;

            @Override
            public void onOptionClick(IconMenu.OptionClickEvent event) {
                event.setWillClose(false);
                event.setWillDestroy(false);

                if (getMyPet().getStatus() != MyPet.PetState.Here) {
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
                                    .setTitle(Translation.getFormattedComponent("Message.Skill.Beacon.Effect", myPet.getOwner().getLanguage(), Translation.getComponent("Name.Off", myPet.getOwner())).color(NamedTextColor.RED))
                                    .addLoreLine(Translation.getComponent("Message.Skill.Beacon.ClickOn", myPet.getOwner()));
                            active = false;
                        } else {
                            menu.getOption(4)
                                    .setMaterial(EMERALD_BLOCK)
                                    .setTitle(Translation.getFormattedComponent("Message.Skill.Beacon.Effect", myPet.getOwner().getLanguage(), Translation.getComponent("Name.On", myPet.getOwner())).color(NamedTextColor.GREEN))
                                    .addLoreLine(Translation.getComponent("Message.Skill.Beacon.ClickOff", myPet.getOwner()));
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
                                menu.getOption(22).setMeta(partyMeta);
                            }
                            menu.getOption(23).setMeta(disabledMeta);
                            receiver = BuffReceiver.Owner;
                            menu.update();
                        }
                        break;
                    case 22:
                        if (receiver != BuffReceiver.Party) {
                            menu.getOption(21).setMeta(disabledMeta);
                            menu.getOption(22).setMeta(partyMeta);
                            menu.getOption(23).setMeta(disabledMeta);
                            receiver = BuffReceiver.Party;
                            menu.update();
                        }
                        break;
                    case 23:
                        if (receiver != BuffReceiver.Everyone) {
                            menu.getOption(21).setMeta(disabledMeta);
                            if (menu.getOption(22) != null) {
                                menu.getOption(22).setMeta(disabledMeta);
                            }
                            menu.getOption(23).setMeta(everyoneMeta);
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
                                                .setTitle(Translation.getFormattedComponent("Message.Skill.Beacon.RemainingBuffs", myPet.getOwner().getLanguage(), selectableBuffs.getValue() - selectedBuffs.size()).color(NamedTextColor.BLUE))
                                                .setAmount(selectableBuffs.getValue() - selectedBuffs.size()));
                                    } else {
                                        menu.setOption(13, new IconMenuItem()
                                                .setMaterial(GLASS_BOTTLE)
                                                .setTitle(Translation.getFormattedComponent("Message.Skill.Beacon.RemainingBuffs", myPet.getOwner().getLanguage(), 0).color(NamedTextColor.GRAY)));
                                    }
                                    menu.update();
                                } else if (selectableBuffs.getValue() > selectedBuffs.size()) {
                                    selectedBuffs.add(selectedBuff);
                                    menu.getOption(selectedBuff.getPosition()).setGlowing(true);
                                    if (selectableBuffs.getValue() > selectedBuffs.size()) {
                                        menu.setOption(13, new IconMenuItem()
                                                .setMaterial(POTION)
                                                .setTitle(Translation.getFormattedComponent("Message.Skill.Beacon.RemainingBuffs", myPet.getOwner().getLanguage(), selectableBuffs.getValue() - selectedBuffs.size()).color(NamedTextColor.BLUE))
                                                .setAmount(selectableBuffs.getValue() - selectedBuffs.size()));
                                    } else {
                                        menu.setOption(13, new IconMenuItem()
                                                .setMaterial(GLASS_BOTTLE)
                                                .setTitle(Translation.getFormattedComponent("Message.Skill.Beacon.RemainingBuffs", myPet.getOwner().getLanguage(), 0).color(NamedTextColor.GRAY)));
                                    }
                                    menu.update();
                                } else {
                                    break;
                                }

                                if (selectableBuffs.getValue() > selectedBuffs.size()) {
                                    menu.setOption(13, new IconMenuItem()
                                            .setMaterial(POTION)
                                            .setTitle(Translation.getFormattedComponent("Message.Skill.Beacon.RemainingBuffs", myPet.getOwner().getLanguage(), selectableBuffs.getValue() - selectedBuffs.size()).color(NamedTextColor.BLUE))
                                            .setAmount(selectableBuffs.getValue() - selectedBuffs.size()));
                                } else {
                                    menu.setOption(13, new IconMenuItem()
                                            .setMaterial(GLASS_BOTTLE)
                                            .setTitle(Translation.getFormattedComponent("Message.Skill.Beacon.RemainingBuffs", myPet.getOwner().getLanguage(), 0).color(NamedTextColor.GRAY)));
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
                    .setTitle(Translation.getFormattedComponent("Message.Skill.Beacon.Effect", myPet.getOwner().getLanguage(), Translation.getComponent("Name.On", myPet.getOwner())).color(NamedTextColor.GREEN))
                    .addLoreLine(Translation.getComponent("Message.Skill.Beacon.ClickOff", myPet.getOwner()))
            );
        } else {
            menu.setOption(4, new IconMenuItem()
                    .setMaterial(REDSTONE_BLOCK)
                    .setTitle(Translation.getFormattedComponent("Message.Skill.Beacon.Effect", myPet.getOwner().getLanguage(), Translation.getComponent("Name.Off", myPet.getOwner())).color(NamedTextColor.RED))
                    .addLoreLine(Translation.getComponent("Message.Skill.Beacon.ClickOn", myPet.getOwner()))
            );
        }

        menu.setOption(3, new IconMenuItem()
                .setMaterial(EnumSelector.find(Material.class, "STAINED_GLASS_PANE", "GREEN_STAINED_GLASS"))
                .setData(5)
                .setTitle(Translation.getComponent("Name.Done", myPet.getOwner()).color(NamedTextColor.GREEN)));
        menu.setOption(5, new IconMenuItem()
                .setMaterial(EnumSelector.find(Material.class, "STAINED_GLASS_PANE", "RED_STAINED_GLASS"))
                .setData(14)
                .setTitle(Translation.getComponent("Name.Cancel", myPet.getOwner()).color(NamedTextColor.RED)));

        if (receiver == BuffReceiver.Owner) {
            menu.setOption(21, new IconMenuItem()
                    .setMaterial(EnumSelector.find(Material.class, "SKULL_ITEM", "PLAYER_HEAD"))
                    .setData(3)
                    .setTitle(Translation.getComponent("Name.Owner", myPet.getOwner()).color(NamedTextColor.GOLD))
                    .setMeta(ownerMeta, false, false));
        } else {
            menu.setOption(21, new IconMenuItem()
                    .setMaterial(EnumSelector.find(Material.class, "SKULL_ITEM", "PLAYER_HEAD"))
                    .setData(3)
                    .setTitle(Translation.getComponent("Name.Owner", myPet.getOwner()).color(NamedTextColor.GOLD))
                    .setMeta(disabledMeta));
        }
        if (Configuration.Skilltree.Skill.Beacon.PARTY_SUPPORT && MyPetApi.getHookHelper().isInParty(getMyPet().getOwner().getPlayer())) {
            if (receiver != BuffReceiver.Party) {
                menu.setOption(22, new IconMenuItem()
                        .setMaterial(EnumSelector.find(Material.class, "SKULL_ITEM", "PLAYER_HEAD"))
                        .setData(3)
                        .setTitle(Translation.getComponent("Name.Party", myPet.getOwner()).color(NamedTextColor.GOLD))
                        .setMeta(partyMeta));
            } else {
                menu.setOption(22, new IconMenuItem()
                        .setMaterial(EnumSelector.find(Material.class, "SKULL_ITEM", "PLAYER_HEAD"))
                        .setData(3)
                        .setTitle(Translation.getComponent("Name.Party", myPet.getOwner()).color(NamedTextColor.GOLD))
                        .setMeta(disabledMeta));
            }
        }
        if (receiver == BuffReceiver.Everyone) {
            menu.setOption(23, new IconMenuItem()
                    .setMaterial(EnumSelector.find(Material.class, "SKULL_ITEM", "PLAYER_HEAD"))
                    .setData(3)
                    .setTitle(Translation.getComponent("Name.Everyone", myPet.getOwner()).color(NamedTextColor.GOLD))
                    .setMeta(everyoneMeta));
        } else {
            menu.setOption(23, new IconMenuItem()
                    .setMaterial(EnumSelector.find(Material.class, "SKULL_ITEM", "PLAYER_HEAD"))
                    .setData(3)
                    .setTitle(Translation.getComponent("Name.Everyone", myPet.getOwner()).color(NamedTextColor.GOLD))
                    .setMeta(disabledMeta));
        }

        if (getBuffLevel(Buff.Speed) > 0) {
            menu.setOption(0, new IconMenuItem()
                    .setMaterial(LEATHER_BOOTS)
                    .setAmount(getBuffLevel(Buff.Speed))
                    .setTitle(Component.text().append(Translation.getComponent("Name." + Buff.Speed.getName(), myPet.getOwner()).color(NamedTextColor.GOLD)).append(Component.text(" " + Util.decimal2roman(getBuffLevel(Buff.Speed))).color(NamedTextColor.GRAY)).build()));
        }
        if (getBuffLevel(Buff.Haste) > 0) {
            menu.setOption(9, new IconMenuItem()
                    .setMaterial(EnumSelector.find(Material.class, "GOLD_PICKAXE", "GOLDEN_PICKAXE"))
                    .setAmount(getBuffLevel(Buff.Haste))
                    .setTitle(Component.text().append(Translation.getComponent("Name." + Buff.Haste.getName(), myPet.getOwner()).color(NamedTextColor.GOLD)).append(Component.text(" " + Util.decimal2roman(getBuffLevel(Buff.Haste))).color(NamedTextColor.GRAY)).build()));
        }
        if (getBuffLevel(Buff.Strength) > 0) {
            menu.setOption(18, new IconMenuItem()
                    .setMaterial(DIAMOND_SWORD)
                    .setAmount(getBuffLevel(Buff.Strength))
                    .setTitle(Component.text().append(Translation.getComponent("Name." + Buff.Strength.getName(), myPet.getOwner()).color(NamedTextColor.GOLD)).append(Component.text(" " + Util.decimal2roman(getBuffLevel(Buff.Strength))).color(NamedTextColor.GRAY)).build()));
        }
        if (getBuffLevel(Buff.JumpBoost) > 0) {
            menu.setOption(1, new IconMenuItem()
                    .setMaterial(EnumSelector.find(Material.class, "FIREWORK", "FIREWORK_ROCKET"))
                    .setAmount(getBuffLevel(Buff.JumpBoost))
                    .setTitle(Component.text().append(Translation.getComponent("Name." + Buff.JumpBoost.getName(), myPet.getOwner()).color(NamedTextColor.GOLD)).append(Component.text(" " + Util.decimal2roman(getBuffLevel(Buff.JumpBoost))).color(NamedTextColor.GRAY)).build()));
        }
        if (getBuffLevel(Buff.Regeneration) > 0) {
            menu.setOption(10, new IconMenuItem()
                    .setMaterial(APPLE)
                    .setAmount(getBuffLevel(Buff.Regeneration))
                    .setTitle(Component.text().append(Translation.getComponent("Name." + Buff.Regeneration.getName(), myPet.getOwner()).color(NamedTextColor.GOLD)).append(Component.text(" " + Util.decimal2roman(getBuffLevel(Buff.Regeneration))).color(NamedTextColor.GRAY)).build()));
        }
        if (getBuffLevel(Buff.Resistance) > 0) {
            menu.setOption(19, new IconMenuItem()
                    .setMaterial(DIAMOND_CHESTPLATE)
                    .setAmount(getBuffLevel(Buff.Resistance))
                    .setTitle(Component.text().append(Translation.getComponent("Name." + Buff.Resistance.getName(), myPet.getOwner()).color(NamedTextColor.GOLD)).append(Component.text(" " + Util.decimal2roman(getBuffLevel(Buff.Resistance))).color(NamedTextColor.GRAY)).build()));
        }
        if (getBuffLevel(Buff.FireResistance) > 0) {
            menu.setOption(7, new IconMenuItem()
                    .setMaterial(LAVA_BUCKET)
                    .setAmount(getBuffLevel(Buff.FireResistance))
                    .setTitle(Component.text().append(Translation.getComponent("Name." + Buff.FireResistance.getName(), myPet.getOwner()).color(NamedTextColor.GOLD)).append(Component.text(" " + Util.decimal2roman(getBuffLevel(Buff.FireResistance))).color(NamedTextColor.GRAY)).build()));
        }
        if (getBuffLevel(Buff.WaterBreathing) > 0) {
            menu.setOption(16, new IconMenuItem()
                    .setMaterial(EnumSelector.find(Material.class, "RAW_FISH", "PUFFERFISH"))
                    .setAmount(getBuffLevel(Buff.WaterBreathing))
                    .setTitle(Component.text().append(Translation.getComponent("Name." + Buff.WaterBreathing.getName(), myPet.getOwner()).color(NamedTextColor.GOLD)).append(Component.text(" " + Util.decimal2roman(getBuffLevel(Buff.WaterBreathing))).color(NamedTextColor.GRAY)).build()));
        }
        if (getBuffLevel(Buff.Invisibility) > 0) {
            menu.setOption(25, new IconMenuItem()
                    .setMaterial(EnumSelector.find(Material.class, "EYE_OF_ENDER", "ENDER_EYE"))
                    .setAmount(getBuffLevel(Buff.Invisibility))
                    .setTitle(Component.text().append(Translation.getComponent("Name." + Buff.Invisibility.getName(), myPet.getOwner()).color(NamedTextColor.GOLD)).append(Component.text(" " + Util.decimal2roman(getBuffLevel(Buff.Invisibility))).color(NamedTextColor.GRAY)).build()));
        }
        if (getBuffLevel(Buff.NightVision) > 0) {
            menu.setOption(8, new IconMenuItem()
                    .setMaterial(TORCH)
                    .setAmount(getBuffLevel(Buff.NightVision))
                    .setTitle(Component.text().append(Translation.getComponent("Name." + Buff.NightVision.getName(), myPet.getOwner()).color(NamedTextColor.GOLD)).append(Component.text(" " + Util.decimal2roman(getBuffLevel(Buff.NightVision))).color(NamedTextColor.GRAY)).build()));
        }
        if (MyPetApi.getCompatUtil().compareWithMinecraftVersion("1.9") >= 0) {
            if (getBuffLevel(Buff.Luck) > 0) {
                menu.setOption(17, new IconMenuItem()
                        .setMaterial(DIAMOND)
                        .setAmount(getBuffLevel(Buff.Luck))
                        .setTitle(Component.text().append(Translation.getComponent("Name." + Buff.Luck.getName(), myPet.getOwner()).color(NamedTextColor.GOLD)).append(Component.text(" " + Util.decimal2roman(getBuffLevel(Buff.Luck))).color(NamedTextColor.GRAY)).build()));
            }
        }
        if (getBuffLevel(Buff.Absorption) > 0) {
            menu.setOption(26, new IconMenuItem()
                    .setMaterial(SPONGE)
                    .setAmount(getBuffLevel(Buff.Absorption))
                    .setTitle(Component.text().append(Translation.getComponent("Name." + Buff.Absorption.getName(), myPet.getOwner()).color(NamedTextColor.GOLD)).append(Component.text(" " + Util.decimal2roman(getBuffLevel(Buff.Absorption))).color(NamedTextColor.GRAY)).build()));
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
                        .setTitle(Translation.getFormattedComponent("Message.Skill.Beacon.RemainingBuffs", myPet.getOwner().getLanguage(), selectableBuffs.getValue() - selectedBuffs.size()).color(NamedTextColor.BLUE))
                        .setAmount(selectableBuffs.getValue() - selectedBuffs.size()));
            } else {
                menu.setOption(13, new IconMenuItem()
                        .setMaterial(GLASS_BOTTLE)
                        .setTitle(Translation.getFormattedComponent("Message.Skill.Beacon.RemainingBuffs", myPet.getOwner().getLanguage(), 0).color(NamedTextColor.GRAY)));
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
                        .append(Translation.getComponent("Name." + buff.getName(), locale).color(net.kyori.adventure.text.format.NamedTextColor.GOLD))
                        .append(Component.text(" " + Util.decimal2roman(getBuffLevel(buff))).color(net.kyori.adventure.text.format.NamedTextColor.GRAY));
                first = false;
            }
        }
        return result;
    }

    @Override
    public Component[] getUpgradeMessage() {
        return new Component[]{
                Translation.getFormattedComponent("Message.Skill.Beacon.Upgrade", myPet.getOwner().getLanguage(), myPet.getDisplayName(), String.format("%1.2f", getRange().getValue().doubleValue()), getDuration().getValue()),
                Component.text(" ").append(toPrettyComponent(myPet.getOwner().getLanguage()))
        };
    }

    public void schedule() {
        if (myPet.getStatus() == MyPet.PetState.Here && isActive() && active && !selectedBuffs.isEmpty() && --beaconTimer <= 0) {
            beaconTimer = 2;

            // Safety check - pet could despawn between status check and location retrieval
            if (!this.myPet.getLocation().isPresent()) {
                return;
            }
            Location myPetLocation = this.myPet.getLocation().get();

            // Check if beacon is allowed at pet's location
            if (!MyPetApi.getHookHelper().isBeaconAllowed(myPetLocation)) {
                return;
            }

            double range = this.range.getValue().doubleValue();

            if (Configuration.HungerSystem.USE_HUNGER_SYSTEM && Configuration.HungerSystem.AFFECT_BEACON_RANGE) {
                range *= (Math.log10(myPet.getSaturation()) / 2);
            }

            // Apply region range multiplier
            range *= MyPetApi.getHookHelper().getBeaconRangeMultiplier(myPetLocation);

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
            myPetLocation.getWorld().spawnParticle(Particle.SPELL_WITCH, myPetLocation.clone().add(0, 1, 0), 5, 0.2F, 0.2F, 0.2F, 0.1F);

            List<Player> members = null;
            if (Configuration.Skilltree.Skill.Beacon.PARTY_SUPPORT && receiver == BuffReceiver.Party) {
                members = MyPetApi.getHookHelper().getPartyMembers(getMyPet().getOwner().getPlayer());
            }

            // Apply region duration multiplier
            int duration = (int) (this.duration.getValue() * 20 * MyPetApi.getHookHelper().getBeaconDurationMultiplier(myPetLocation));

            // Get region amplifier modifier
            int amplifierMod = MyPetApi.getHookHelper().getBeaconAmplifierModifier(myPetLocation);

            List<PotionEffect> potionEffects = new ArrayList<>();
            for (Buff buff : selectedBuffs) {
                int amplification = Math.max(0, getBuffLevel(buff) - 1 + amplifierMod);
                PotionEffect effect = new PotionEffect(PotionEffectType.getById(buff.getId()), duration, amplification, true, true);
                potionEffects.add(effect);
            }

            targetLoop:
            for (Player player : myPetLocation.getWorld().getPlayers()) {
                if (MyPetApi.getPlatformHelper().distanceSquared(player.getLocation(), myPetLocation) > range) {
                    continue;
                } else if (player.getGameMode().name().equals("SPECTATOR")) {
                    continue;
                } else if (MyPetApi.getHookHelper().isVanished(player)) {
                    continue;
                }

                boolean isOwner = myPet.getOwner().getPlayer().equals(player);

                // Check self-deny for owner
                if (isOwner && !MyPetApi.getHookHelper().isBeaconSelfAllowed(player.getLocation())) {
                    continue;
                }

                // Check share-deny for non-owners (both pet location and target location)
                if (!isOwner) {
                    if (!MyPetApi.getHookHelper().isBeaconShareAllowed(myPetLocation) ||
                        !MyPetApi.getHookHelper().isBeaconShareAllowed(player.getLocation())) {
                        continue;
                    }
                }

                switch (receiver) {
                    case Owner:
                        if (!myPet.getOwner().equals(player)) {
                            continue;
                        } else {
                            for (PotionEffect effect : potionEffects) {
                                player.addPotionEffect(effect, true);
                            }
                            if (!player.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                                player.getWorld().spawnParticle(Particle.SPELL_INSTANT, player.getLocation().add(0, 1, 0), 5, 0.2F, 0.2F, 0.2F, 0.1F);
                            }
                            break targetLoop;
                        }
                    case Everyone:
                        for (PotionEffect effect : potionEffects) {
                            player.addPotionEffect(effect, true);
                        }
                        if (!player.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                            player.getWorld().spawnParticle(Particle.SPELL_INSTANT, player.getLocation().add(0, 1, 0), 5, 0.2F, 0.2F, 0.2F, 0.1F);
                        }
                        break;
                    case Party:
                        if (Configuration.Skilltree.Skill.Beacon.PARTY_SUPPORT && members != null) {
                            if (members.contains(player)) {
                                for (PotionEffect effect : potionEffects) {
                                    player.addPotionEffect(effect, true);
                                }
                                if (!player.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                                    player.getWorld().spawnParticle(Particle.SPELL_INSTANT, player.getLocation().add(0, 1, 0), 5, 0.2F, 0.2F, 0.2F, 0.1F);
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
                myPet.decreaseSaturation(1);
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

    @Override
    public UpgradeComputer getBuff(Buff buff) {
        return this.buffLevel.get(buff);
    }

    public int getBuffLevel(Buff buff) {
        UpgradeComputer buffLevel = this.buffLevel.get(buff);
        if (buffLevel.getValue() instanceof Boolean) {
            return (Boolean) buffLevel.getValue() ? 1 : 0;
        } else if (buffLevel.getValue() instanceof Integer) {
            return (Integer) buffLevel.getValue();
        }
        return 0;
    }

    @Override
    public CompoundBinaryTag save() {
        return CompoundBinaryTag.builder()
                .put("Buffs", IntArrayBinaryTag.intArrayBinaryTag(selectedBuffs.stream().mapToInt(Buff::getId).toArray()))
                .putBoolean("Active", this.active)
                .putString("Receiver", this.receiver.name())
                .build();
    }

    @Override
    public void load(CompoundBinaryTag compound) {
        if (compound.keySet().contains("Buff")) {
            Buff selectedBuff = Buff.getBuffByID(compound.getInt("Buff"));
            if (selectedBuff != null) {
                this.selectedBuffs.add(selectedBuff);
            }
        }
        if (compound.keySet().contains("Buffs")) {
            int[] selectedBuffs = compound.getIntArray("Buffs");
            for (int selectedBuffId : selectedBuffs) {
                Buff selectedBuff = Buff.getBuffByID(selectedBuffId);
                if (selectedBuff != null) {
                    this.selectedBuffs.add(selectedBuff);
                }
            }
        }
        if (compound.keySet().contains("Active")) {
            this.active = compound.getBoolean("Active");
        }
        if (compound.keySet().contains("Receiver")) {
            this.receiver = BuffReceiver.valueOf(compound.getString("Receiver"));
        }
    }

    @Override
    public String toString() {
        return "BeaconImpl{" +
                "duration=" + duration +
                ", range=" + range +
                ", selectableBuffs=" + selectableBuffs +
                ", active=" + active +
                ", receiver=" + receiver +
                ", buffLevel=" + buffLevel +
                ", selectedBuffs=" + selectedBuffs +
                '}';
    }
}