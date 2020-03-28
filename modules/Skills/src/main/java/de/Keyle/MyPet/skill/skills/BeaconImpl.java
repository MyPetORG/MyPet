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
import de.Keyle.MyPet.api.compat.ParticleCompat;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.gui.IconMenu;
import de.Keyle.MyPet.api.gui.IconMenuItem;
import de.Keyle.MyPet.api.skill.UpgradeComputer;
import de.Keyle.MyPet.api.skill.skills.Beacon;
import de.Keyle.MyPet.api.util.EnumSelector;
import de.Keyle.MyPet.api.util.inventory.meta.SkullMeta;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.keyle.knbt.*;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

import static org.bukkit.ChatColor.*;
import static org.bukkit.Material.*;

public class BeaconImpl implements Beacon {

    SkullMeta disabledMeta = new SkullMeta();
    SkullMeta partyMeta = new SkullMeta();
    SkullMeta everyoneMeta = new SkullMeta();
    org.bukkit.inventory.meta.SkullMeta ownerMeta;

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
        String title = RESET + Translation.getString("Name.Skill.Beacon", myPet.getOwner());
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
                                    .setTitle(Util.formatText(Translation.getString("Message.Skill.Beacon.Effect", myPet.getOwner()), RED + Translation.getString("Name.Off", myPet.getOwner())))
                                    .setLore(RESET + Translation.getString("Message.Skill.Beacon.ClickOn", myPet.getOwner()));
                            active = false;
                        } else {
                            menu.getOption(4)
                                    .setMaterial(EMERALD_BLOCK)
                                    .setTitle(Util.formatText(Translation.getString("Message.Skill.Beacon.Effect", myPet.getOwner()), GREEN + Translation.getString("Name.On", myPet.getOwner())))
                                    .setLore(RESET + Translation.getString("Message.Skill.Beacon.ClickOff", myPet.getOwner()));
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
                                                .setTitle(BLUE + Util.formatText(Translation.getString("Message.Skill.Beacon.RemainingBuffs", myPet.getOwner()), selectableBuffs.getValue() - selectedBuffs.size()))
                                                .setAmount(selectableBuffs.getValue() - selectedBuffs.size()));
                                    } else {
                                        menu.setOption(13, new IconMenuItem()
                                                .setMaterial(GLASS_BOTTLE)
                                                .setTitle(GRAY + Util.formatText(Translation.getString("Message.Skill.Beacon.RemainingBuffs", myPet.getOwner()), 0)));
                                    }
                                    menu.update();
                                } else if (selectableBuffs.getValue() > selectedBuffs.size()) {
                                    selectedBuffs.add(selectedBuff);
                                    menu.getOption(selectedBuff.getPosition()).setGlowing(true);
                                    if (selectableBuffs.getValue() > selectedBuffs.size()) {
                                        menu.setOption(13, new IconMenuItem()
                                                .setMaterial(POTION)
                                                .setTitle(BLUE + Util.formatText(Translation.getString("Message.Skill.Beacon.RemainingBuffs", myPet.getOwner()), selectableBuffs.getValue() - selectedBuffs.size()))
                                                .setAmount(selectableBuffs.getValue() - selectedBuffs.size()));
                                    } else {
                                        menu.setOption(13, new IconMenuItem()
                                                .setMaterial(GLASS_BOTTLE)
                                                .setTitle(GRAY + Util.formatText(Translation.getString("Message.Skill.Beacon.RemainingBuffs", myPet.getOwner()), 0)));
                                    }
                                    menu.update();
                                } else {
                                    break;
                                }

                                if (selectableBuffs.getValue() > selectedBuffs.size()) {
                                    menu.setOption(13, new IconMenuItem()
                                            .setMaterial(POTION)
                                            .setTitle(BLUE + Util.formatText(Translation.getString("Message.Skill.Beacon.RemainingBuffs", myPet.getOwner()), selectableBuffs.getValue() - selectedBuffs.size()))
                                            .setAmount(selectableBuffs.getValue() - selectedBuffs.size()));
                                } else {
                                    menu.setOption(13, new IconMenuItem()
                                            .setMaterial(GLASS_BOTTLE)
                                            .setTitle(GRAY + Util.formatText(Translation.getString("Message.Skill.Beacon.RemainingBuffs", myPet.getOwner()), 0)));
                                }
                            } else if (!selectedBuffs.contains(selectedBuff)) {
                                if (selectedBuffs.size() != 0 && menu.getOption(selectedBuff.getPosition()) != null) {
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
                    .setTitle(Util.formatText(Translation.getString("Message.Skill.Beacon.Effect", myPet.getOwner()), GREEN + Translation.getString("Name.On", myPet.getOwner())))
                    .addLoreLine(RESET + Translation.getString("Message.Skill.Beacon.ClickOff", myPet.getOwner()))
            );
        } else {
            menu.setOption(4, new IconMenuItem()
                    .setMaterial(REDSTONE_BLOCK)
                    .setTitle(Util.formatText(Translation.getString("Message.Skill.Beacon.Effect", myPet.getOwner()), RED + Translation.getString("Name.Off", myPet.getOwner())))
                    .addLoreLine(RESET + Translation.getString("Message.Skill.Beacon.ClickOn", myPet.getOwner()))
            );
        }

        menu.setOption(3, new IconMenuItem()
                .setMaterial(EnumSelector.find(Material.class, "STAINED_GLASS_PANE", "GREEN_STAINED_GLASS"))
                .setData(5)
                .setTitle(GREEN + Translation.getString("Name.Done", myPet.getOwner())));
        menu.setOption(5, new IconMenuItem()
                .setMaterial(EnumSelector.find(Material.class, "STAINED_GLASS_PANE", "RED_STAINED_GLASS"))
                .setData(14)
                .setTitle(RED + Translation.getString("Name.Cancel", myPet.getOwner())));

        if (receiver == BuffReceiver.Owner) {
            menu.setOption(21, new IconMenuItem()
                    .setMaterial(EnumSelector.find(Material.class, "SKULL_ITEM", "PLAYER_HEAD"))
                    .setData(3)
                    .setTitle(GOLD + Translation.getString("Name.Owner", myPet.getOwner()))
                    .setMeta(ownerMeta, false, false));
        } else {
            menu.setOption(21, new IconMenuItem()
                    .setMaterial(EnumSelector.find(Material.class, "SKULL_ITEM", "PLAYER_HEAD"))
                    .setData(3)
                    .setTitle(GOLD + Translation.getString("Name.Owner", myPet.getOwner()))
                    .setMeta(disabledMeta));
        }
        if (Configuration.Skilltree.Skill.Beacon.PARTY_SUPPORT && MyPetApi.getHookHelper().isInParty(getMyPet().getOwner().getPlayer())) {
            if (receiver != BuffReceiver.Party) {
                menu.setOption(22, new IconMenuItem()
                        .setMaterial(EnumSelector.find(Material.class, "SKULL_ITEM", "PLAYER_HEAD"))
                        .setData(3)
                        .setTitle(GOLD + Translation.getString("Name.Party", myPet.getOwner()))
                        .setMeta(partyMeta));
            } else {
                menu.setOption(22, new IconMenuItem()
                        .setMaterial(EnumSelector.find(Material.class, "SKULL_ITEM", "PLAYER_HEAD"))
                        .setData(3)
                        .setTitle(GOLD + Translation.getString("Name.Party", myPet.getOwner()))
                        .setMeta(disabledMeta));
            }
        }
        if (receiver == BuffReceiver.Everyone) {
            menu.setOption(23, new IconMenuItem()
                    .setMaterial(EnumSelector.find(Material.class, "SKULL_ITEM", "PLAYER_HEAD"))
                    .setData(3)
                    .setTitle(GOLD + Translation.getString("Name.Everyone", myPet.getOwner()))
                    .setMeta(everyoneMeta));
        } else {
            menu.setOption(23, new IconMenuItem()
                    .setMaterial(EnumSelector.find(Material.class, "SKULL_ITEM", "PLAYER_HEAD"))
                    .setData(3)
                    .setTitle(GOLD + Translation.getString("Name.Everyone", myPet.getOwner()))
                    .setMeta(disabledMeta));
        }

        if (getBuffLevel(Buff.Speed) > 0) {
            menu.setOption(0, new IconMenuItem()
                    .setMaterial(LEATHER_BOOTS)
                    .setAmount(getBuffLevel(Buff.Speed))
                    .setTitle(GOLD + Translation.getString("Name." + Buff.Speed.getName(), myPet.getOwner()) + GRAY + " " + Util.decimal2roman(getBuffLevel(Buff.Speed))));
        }
        if (getBuffLevel(Buff.Haste) > 0) {
            menu.setOption(9, new IconMenuItem()
                    .setMaterial(EnumSelector.find(Material.class, "GOLD_PICKAXE", "GOLDEN_PICKAXE"))
                    .setAmount(getBuffLevel(Buff.Haste))
                    .setTitle(GOLD + Translation.getString("Name." + Buff.Haste.getName(), myPet.getOwner()) + GRAY + " " + Util.decimal2roman(getBuffLevel(Buff.Haste))));
        }
        if (getBuffLevel(Buff.Strength) > 0) {
            menu.setOption(18, new IconMenuItem()
                    .setMaterial(DIAMOND_SWORD)
                    .setAmount(getBuffLevel(Buff.Strength))
                    .setTitle(GOLD + Translation.getString("Name." + Buff.Strength.getName(), myPet.getOwner()) + GRAY + " " + Util.decimal2roman(getBuffLevel(Buff.Strength))));
        }
        if (getBuffLevel(Buff.JumpBoost) > 0) {
            menu.setOption(1, new IconMenuItem()
                    .setMaterial(EnumSelector.find(Material.class, "FIREWORK", "FIREWORK_ROCKET"))
                    .setAmount(getBuffLevel(Buff.JumpBoost))
                    .setTitle(GOLD + Translation.getString("Name." + Buff.JumpBoost.getName(), myPet.getOwner()) + GRAY + " " + Util.decimal2roman(getBuffLevel(Buff.JumpBoost))));
        }
        if (getBuffLevel(Buff.Regeneration) > 0) {
            menu.setOption(10, new IconMenuItem()
                    .setMaterial(APPLE)
                    .setAmount(getBuffLevel(Buff.Regeneration))
                    .setTitle(GOLD + Translation.getString("Name." + Buff.Regeneration.getName(), myPet.getOwner()) + GRAY + " " + Util.decimal2roman(getBuffLevel(Buff.Regeneration))));
        }
        if (getBuffLevel(Buff.Resistance) > 0) {
            menu.setOption(19, new IconMenuItem()
                    .setMaterial(DIAMOND_CHESTPLATE)
                    .setAmount(getBuffLevel(Buff.Resistance))
                    .setTitle(GOLD + Translation.getString("Name." + Buff.Resistance.getName(), myPet.getOwner()) + GRAY + " " + Util.decimal2roman(getBuffLevel(Buff.Resistance))));
        }
        if (getBuffLevel(Buff.FireResistance) > 0) {
            menu.setOption(7, new IconMenuItem()
                    .setMaterial(LAVA_BUCKET)
                    .setAmount(getBuffLevel(Buff.FireResistance))
                    .setTitle(GOLD + Translation.getString("Name." + Buff.FireResistance.getName(), myPet.getOwner()) + GRAY + " " + Util.decimal2roman(getBuffLevel(Buff.FireResistance))));
        }
        if (getBuffLevel(Buff.WaterBreathing) > 0) {
            menu.setOption(16, new IconMenuItem()
                    .setMaterial(EnumSelector.find(Material.class, "RAW_FISH", "PUFFERFISH"))
                    .setAmount(getBuffLevel(Buff.WaterBreathing))
                    .setTitle(GOLD + Translation.getString("Name." + Buff.WaterBreathing.getName(), myPet.getOwner()) + GRAY + " " + Util.decimal2roman(getBuffLevel(Buff.WaterBreathing))));
        }
        if (getBuffLevel(Buff.Invisibility) > 0) {
            menu.setOption(25, new IconMenuItem()
                    .setMaterial(EnumSelector.find(Material.class, "EYE_OF_ENDER", "ENDER_EYE"))
                    .setAmount(getBuffLevel(Buff.Invisibility))
                    .setTitle(GOLD + Translation.getString("Name." + Buff.Invisibility.getName(), myPet.getOwner()) + GRAY + " " + Util.decimal2roman(getBuffLevel(Buff.Invisibility))));
        }
        if (getBuffLevel(Buff.NightVision) > 0) {
            menu.setOption(8, new IconMenuItem()
                    .setMaterial(TORCH)
                    .setAmount(getBuffLevel(Buff.NightVision))
                    .setTitle(GOLD + Translation.getString("Name." + Buff.NightVision.getName(), myPet.getOwner()) + GRAY + " " + Util.decimal2roman(getBuffLevel(Buff.NightVision))));
        }
        if (MyPetApi.getCompatUtil().compareWithMinecraftVersion("1.9") >= 0) {
            if (getBuffLevel(Buff.Luck) > 0) {
                menu.setOption(17, new IconMenuItem()
                        .setMaterial(DIAMOND)
                        .setAmount(getBuffLevel(Buff.Luck))
                        .setTitle(GOLD + Translation.getString("Name." + Buff.Luck.getName(), myPet.getOwner()) + GRAY + " " + Util.decimal2roman(getBuffLevel(Buff.Luck))));
            }
        }
        /*
        if (buffLevel.get(21) > 0) {
            menu.setOption(17, new IconMenuItem()
                        .setMaterial(GOLDEN_APPLE)
                        .setAmount(buffLevel.get(21))
                        .setTitle(GOLD + Translation.getString("Name." + buffNames.get(21), myPet.getOwner()) + GRAY + " " + Util.decimal2roman(buffLevel.get(21))));
        }
        */
        if (getBuffLevel(Buff.Absorption) > 0) {
            menu.setOption(26, new IconMenuItem()
                    .setMaterial(SPONGE)
                    .setAmount(getBuffLevel(Buff.Absorption))
                    .setTitle(GOLD + Translation.getString("Name." + Buff.Absorption.getName(), myPet.getOwner()) + GRAY + " " + Util.decimal2roman(getBuffLevel(Buff.Absorption))));
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
                        .setTitle(BLUE + Util.formatText(Translation.getString("Message.Skill.Beacon.RemainingBuffs", myPet.getOwner()), selectableBuffs.getValue() - selectedBuffs.size()))
                        .setAmount(selectableBuffs.getValue() - selectedBuffs.size()));
            } else {
                menu.setOption(13, new IconMenuItem()
                        .setMaterial(GLASS_BOTTLE)
                        .setTitle(GRAY + Util.formatText(Translation.getString("Message.Skill.Beacon.RemainingBuffs", myPet.getOwner()), 0)));
            }
        }

        menu.open(owner);

        return true;
    }

    public String toPrettyString(String locale) {
        String availableBuffs = "";
        for (Buff buff : Buff.values()) {
            if (getBuffLevel(buff) > 0) {
                if (!availableBuffs.equalsIgnoreCase("")) {
                    availableBuffs += ", ";
                }
                availableBuffs += GOLD + Translation.getString("Name." + buff.getName(), locale);
                availableBuffs += GRAY + " " + Util.decimal2roman(getBuffLevel(buff));
                availableBuffs += ChatColor.RESET;
            }
        }
        return availableBuffs;
    }

    @Override
    public String[] getUpgradeMessage() {
        return new String[]{
                Util.formatText(Translation.getString("Message.Skill.Beacon.Upgrade", myPet.getOwner().getLanguage()), myPet.getPetName(), String.format("%1.2f", getRange().getValue().doubleValue()), getDuration().getValue()),
                " " + toPrettyString(myPet.getOwner().getLanguage())
        };
    }

    public void schedule() {
        if (myPet.getStatus() == MyPet.PetState.Here && isActive() && active && selectedBuffs.size() != 0 && --beaconTimer <= 0) {
            beaconTimer = 2;

            double range = this.range.getValue().doubleValue();

            if (Configuration.HungerSystem.USE_HUNGER_SYSTEM && Configuration.HungerSystem.AFFECT_BEACON_RANGE) {
                range *= (Math.log10(myPet.getSaturation()) / 2);
            }

            if (range < 0.7) {
                return;
            }


            if (selectedBuffs.size() == 0) {
                return;
            }
            if (selectedBuffs.size() > selectableBuffs.getValue()) {
                selectedBuffs.clear();
            }

            range = range * range;
            MyPetApi.getPlatformHelper().playParticleEffect(myPet.getLocation().get().add(0, 1, 0), ParticleCompat.SPELL_WITCH.get(), 0.2F, 0.2F, 0.2F, 0.1F, 5, 20);

            List<Player> members = null;
            if (Configuration.Skilltree.Skill.Beacon.PARTY_SUPPORT && receiver == BuffReceiver.Party) {
                members = MyPetApi.getHookHelper().getPartyMembers(getMyPet().getOwner().getPlayer());
            }
            int duration = this.duration.getValue() * 20;

            List<PotionEffect> potionEffects = new ArrayList<>();
            for (Buff buff : selectedBuffs) {
                int amplification = getBuffLevel(buff) - 1;
                PotionEffect effect = new PotionEffect(PotionEffectType.getById(buff.getId()), duration, amplification, true, true);
                potionEffects.add(effect);
            }

            Location myPetLocation = this.myPet.getLocation().get();
            targetLoop:
            for (Player player : myPetLocation.getWorld().getPlayers()) {
                if (MyPetApi.getPlatformHelper().distanceSquared(player.getLocation(), myPetLocation) > range) {
                    continue;
                } else if (player.getGameMode().name().equals("SPECTATOR")) {
                    continue;
                } else if (MyPetApi.getHookHelper().isVanished(player)) {
                    continue;
                }

                switch (receiver) {
                    case Owner:
                        if (!myPet.getOwner().equals(player)) {
                            continue targetLoop;
                        } else {
                            for (PotionEffect effect : potionEffects) {
                                player.addPotionEffect(effect, true);
                            }
                            if (!player.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                                MyPetApi.getPlatformHelper().playParticleEffect(player.getLocation().add(0, 1, 0), ParticleCompat.SPELL_INSTANT.get(), 0.2F, 0.2F, 0.2F, 0.1F, 5, 20);
                            }
                            break targetLoop;
                        }
                    case Everyone:
                        for (PotionEffect effect : potionEffects) {
                            player.addPotionEffect(effect, true);
                        }
                        if (!player.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                            MyPetApi.getPlatformHelper().playParticleEffect(player.getLocation().add(0, 1, 0), ParticleCompat.SPELL_INSTANT.get(), 0.2F, 0.2F, 0.2F, 0.1F, 5, 20);
                        }
                        break;
                    case Party:
                        if (Configuration.Skilltree.Skill.Beacon.PARTY_SUPPORT && members != null) {
                            if (members.contains(player)) {
                                for (PotionEffect effect : potionEffects) {
                                    player.addPotionEffect(effect, true);
                                }
                                if (!player.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                                    MyPetApi.getPlatformHelper().playParticleEffect(player.getLocation().add(0, 1, 0), ParticleCompat.SPELL_INSTANT.get(), 0.2F, 0.2F, 0.2F, 0.1F, 5, 20);
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
    public TagCompound save() {
        TagCompound nbtTagCompound = new TagCompound();
        nbtTagCompound.getCompoundData().put("Buffs", new TagIntArray(selectedBuffs.stream().mapToInt(Buff::getId).toArray()));
        nbtTagCompound.getCompoundData().put("Active", new TagByte(this.active));
        nbtTagCompound.getCompoundData().put("Reciever", new TagString(this.receiver.name()));
        return nbtTagCompound;
    }

    @Override
    public void load(TagCompound compound) {
        if (compound.getCompoundData().containsKey("Buff")) {
            Buff selectedBuff = Buff.getBuffByID(compound.getAs("Buff", TagInt.class).getIntData());
            if (selectedBuff != null) {
                this.selectedBuffs.add(selectedBuff);
            }
        }
        if (compound.getCompoundData().containsKey("Buffs")) {
            int[] selectedBuffs = compound.getAs("Buffs", TagIntArray.class).getIntArrayData();
            if (selectedBuffs.length != 0) {
                for (int selectedBuffId : selectedBuffs) {
                    Buff selectedBuff = Buff.getBuffByID(selectedBuffId);
                    if (selectedBuff != null) {
                        this.selectedBuffs.add(selectedBuff);
                    }
                }
            }
        }
        if (compound.getCompoundData().containsKey("Active")) {
            this.active = compound.getAs("Active", TagByte.class).getBooleanData();
        }
        if (compound.getCompoundData().containsKey("Reciever")) {
            this.receiver = BuffReceiver.valueOf(compound.getAs("Reciever", TagString.class).getStringData());
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