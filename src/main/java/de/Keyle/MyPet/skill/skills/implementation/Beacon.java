/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2014 Keyle
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

package de.Keyle.MyPet.skill.skills.implementation;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.api.util.IScheduler;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.skill.skills.ISkillActive;
import de.Keyle.MyPet.skill.skills.ISkillStorage;
import de.Keyle.MyPet.skill.skills.info.BeaconInfo;
import de.Keyle.MyPet.skill.skills.info.ISkillInfo;
import de.Keyle.MyPet.util.BukkitUtil;
import de.Keyle.MyPet.util.Util;
import de.Keyle.MyPet.util.iconmenu.IconMenu;
import de.Keyle.MyPet.util.iconmenu.IconMenuItem;
import de.Keyle.MyPet.util.locale.Locales;
import de.keyle.knbt.*;
import net.minecraft.server.v1_7_R4.EntityHuman;
import net.minecraft.server.v1_7_R4.MobEffect;
import net.minecraft.server.v1_7_R4.MobEffectList;
import org.apache.commons.lang.ArrayUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.bukkit.ChatColor.*;
import static org.bukkit.Material.*;

public class Beacon extends BeaconInfo implements ISkillInstance, IScheduler, ISkillStorage, ISkillActive {
    public static int HUNGER_DECREASE_TIME = 100;
    private static Map<Integer, String> buffNames = new HashMap<Integer, String>();
    private static Map<Integer, MobEffectList> buffEffectLists = new HashMap<Integer, MobEffectList>();
    private static BiMap<Integer, Integer> buffItemPositions = HashBiMap.create();
    private static BiMap<Integer, Integer> buffPositionItems = buffItemPositions.inverse();

    static {
        buffNames.put(1, "Speed");
        buffNames.put(3, "Haste");
        buffNames.put(5, "Strength");
        buffNames.put(8, "JumpBoost");
        buffNames.put(10, "Regeneration");
        buffNames.put(11, "Resistance");
        buffNames.put(12, "FireResistance");
        buffNames.put(13, "WaterBreathing");
        buffNames.put(14, "Invisibility");
        buffNames.put(16, "NightVision");
        buffNames.put(21, "HealthBoost");
        buffNames.put(22, "Absorption");

        buffItemPositions.put(1, 0);
        buffItemPositions.put(3, 9);
        buffItemPositions.put(5, 18);
        buffItemPositions.put(8, 1);
        buffItemPositions.put(10, 10);
        buffItemPositions.put(11, 19);
        buffItemPositions.put(12, 7);
        buffItemPositions.put(13, 16);
        buffItemPositions.put(14, 25);
        buffItemPositions.put(16, 8);
        buffItemPositions.put(21, 17);
        buffItemPositions.put(22, 26);

        buffEffectLists.put(1, MobEffectList.FASTER_MOVEMENT);
        buffEffectLists.put(3, MobEffectList.FASTER_DIG);
        buffEffectLists.put(5, MobEffectList.INCREASE_DAMAGE);
        buffEffectLists.put(8, MobEffectList.JUMP);
        buffEffectLists.put(10, MobEffectList.REGENERATION);
        buffEffectLists.put(11, MobEffectList.RESISTANCE);
        buffEffectLists.put(12, MobEffectList.FIRE_RESISTANCE);
        buffEffectLists.put(13, MobEffectList.WATER_BREATHING);
        buffEffectLists.put(14, MobEffectList.INVISIBILITY);
        buffEffectLists.put(16, MobEffectList.NIGHT_VISION);
        buffEffectLists.put(21, MobEffectList.HEALTH_BOOST);
        buffEffectLists.put(22, MobEffectList.ABSORPTION);
    }

    public enum BeaconReciever {
        Owner, Party, Everyone
    }

    private MyPet myPet;

    private boolean active = false;
    private int hungerDecreaseTimer;
    private BeaconReciever reciever = BeaconReciever.Owner;
    private Map<Integer, Integer> buffLevel = new HashMap<Integer, Integer>();
    private int beaconTimer = 0;
    private List<Integer> selectedBuffs = new ArrayList<Integer>();

    public Beacon(boolean addedByInheritance) {
        super(addedByInheritance);

        reset();
        hungerDecreaseTimer = HUNGER_DECREASE_TIME;
    }

    public void setMyPet(MyPet myPet) {
        this.myPet = myPet;
    }

    public MyPet getMyPet() {
        return myPet;
    }

    public boolean isActive() {
        if (selectableBuffs == 0) {
            return false;
        }
        for (int amp : buffLevel.values()) {
            if (amp > 0) {
                return duration > 0 && range > 0;
            }
        }
        return false;
    }

    public boolean activate() {
        Player owner = myPet.getOwner().getPlayer();

        final Beacon beacon = this;

        IconMenu menu = new IconMenu(Util.cutString("Beacon - " + myPet.getPetName(), 32), 27, new IconMenu.OptionClickEventHandler() {
            List<Integer> selectedBuffs = beacon.selectedBuffs;
            boolean active = beacon.active;
            private BeaconReciever reciever = beacon.reciever;

            @Override
            public void onOptionClick(IconMenu.OptionClickEvent event) {
                event.setWillClose(false);
                event.setWillDestroy(false);

                IconMenu menu = event.getMenu();

                switch (event.getPosition()) {
                    case 5:
                        event.setWillClose(true);
                        event.setWillDestroy(true);
                        return;
                    case 4:
                        if (active) {
                            menu.getOption(4).setMaterial(REDSTONE_BLOCK).setTitle("Buffs: " + RED + "off").setLore(RESET + "Click to turn them " + GREEN + "on");
                            active = false;
                        } else {
                            menu.getOption(4).setMaterial(EMERALD_BLOCK).setTitle("Buffs: " + GREEN + "on").setLore(RESET + "Click to turn them " + RED + "off");
                            active = true;
                        }
                        menu.update();
                        break;
                    case 3:
                        beacon.active = active;
                        beacon.selectedBuffs = selectedBuffs;
                        beacon.reciever = reciever;
                        event.setWillClose(true);
                        event.setWillDestroy(true);
                        break;
                    case 21:
                        if (reciever != BeaconReciever.Owner) {
                            menu.getOption(21).setGlowing(true);
                            //menu.getOption(22).setGlowing(false);
                            menu.getOption(23).setGlowing(false);
                            reciever = BeaconReciever.Owner;
                            menu.update();
                        }
                        break;
                    case 22:
                        if (reciever != BeaconReciever.Party) {
                            menu.getOption(21).setGlowing(false);
                            menu.getOption(22).setGlowing(true);
                            menu.getOption(23).setGlowing(false);
                            reciever = BeaconReciever.Party;
                            menu.update();
                        }
                        break;
                    case 23:
                        if (reciever != BeaconReciever.Everyone) {
                            menu.getOption(21).setGlowing(false);
                            //menu.getOption(22).setGlowing(false);
                            menu.getOption(23).setGlowing(true);
                            reciever = BeaconReciever.Everyone;
                            menu.update();
                        }
                        break;
                    default:
                        if (buffPositionItems.containsKey(event.getPosition())) {
                            int selectedBuff = buffPositionItems.get(event.getPosition());

                            if (selectableBuffs > 1) {
                                if (selectedBuffs.indexOf(selectedBuff) != -1) {
                                    selectedBuffs.remove(selectedBuffs.indexOf(selectedBuff));
                                    menu.getOption(buffItemPositions.get(selectedBuff)).setGlowing(false);
                                    menu.update();
                                } else if (selectableBuffs > selectedBuffs.size()) {
                                    selectedBuffs.add(selectedBuff);
                                    menu.getOption(buffItemPositions.get(selectedBuff)).setGlowing(true);
                                    menu.update();
                                } else {
                                    break;
                                }

                                if (selectableBuffs > selectedBuffs.size()) {
                                    menu.setOption(13, new IconMenuItem().setMaterial(POTION).setTitle(BLUE + Util.formatText(Locales.getString("Message.Skill.Beacon.RemainingBuffs", myPet.getOwner().getLanguage()), selectableBuffs - selectedBuffs.size())).setAmount(selectableBuffs - selectedBuffs.size()));
                                } else {
                                    menu.setOption(13, new IconMenuItem().setMaterial(GLASS_BOTTLE).setTitle(GRAY + Util.formatText(Locales.getString("Message.Skill.Beacon.RemainingBuffs", myPet.getOwner().getLanguage()), 0)));
                                }
                            } else {
                                if (!selectedBuffs.contains(selectedBuff)) {
                                    if (selectedBuffs.size() != 0 && menu.getOption(buffItemPositions.get(selectedBuff)) != null) {
                                        menu.getOption(buffItemPositions.get(selectedBuffs.get(0))).setGlowing(false);
                                        selectedBuffs.clear();
                                    }
                                    selectedBuffs.add(selectedBuff);
                                    menu.getOption(buffItemPositions.get(selectedBuff)).setGlowing(true);
                                    menu.update();
                                }
                            }
                        }
                }
            }
        }, MyPetPlugin.getPlugin());

        if (beacon.active) {
            menu.setOption(4, new IconMenuItem().setMaterial(EMERALD_BLOCK).setTitle(Util.formatText(Locales.getString("Message.Skill.Beacon.Effect", myPet.getOwner().getLanguage()), GREEN + Locales.getString("Name.On", myPet.getOwner().getLanguage()))).addLoreLine(RESET + Locales.getString("Message.Skill.Beacon.ClickOff", myPet.getOwner().getLanguage())));
        } else {
            menu.setOption(4, new IconMenuItem().setMaterial(REDSTONE_BLOCK).setTitle(Util.formatText(Locales.getString("Message.Skill.Beacon.Effect", myPet.getOwner().getLanguage()), RED + Locales.getString("Name.Off", myPet.getOwner().getLanguage()))).addLoreLine(RESET + Locales.getString("Message.Skill.Beacon.ClickOn", myPet.getOwner().getLanguage())));
        }

        menu.setOption(3, new IconMenuItem().setMaterial(STAINED_GLASS_PANE).setData(5).setTitle(GREEN + Locales.getString("Name.Done", myPet.getOwner().getLanguage())));
        menu.setOption(5, new IconMenuItem().setMaterial(STAINED_GLASS_PANE).setData(14).setTitle(RED + Locales.getString("Name.Cancel", myPet.getOwner().getLanguage())));

        menu.setOption(21, new IconMenuItem().setMaterial(SKULL_ITEM).setData(3).setTitle(GOLD + Locales.getString("Name.Owner", myPet.getOwner().getLanguage())).setGlowing(reciever == BeaconReciever.Owner));
        // Will be implemented later
        // menu.setOption(22, new IconMenuItem().setMaterial(SKULL_ITEM).setData(1).setTitle(GOLD + Locales.getString("Name.Party", myPet.getOwner().getLanguage())).setGlowing(reciever == BeaconReciever.Party));
        menu.setOption(23, new IconMenuItem().setMaterial(SKULL_ITEM).setData(2).setTitle(GOLD + Locales.getString("Name.Everyone", myPet.getOwner().getLanguage())).setGlowing(reciever == BeaconReciever.Everyone));

        if (buffLevel.get(1) > 0) {
            menu.setOption(0, new IconMenuItem().setMaterial(LEATHER_BOOTS).setAmount(buffLevel.get(1)).setTitle(GOLD + Locales.getString("Name." + buffNames.get(1), myPet.getOwner().getLanguage()) + GRAY + " " + Util.decimal2roman(buffLevel.get(1))));
        }
        if (buffLevel.get(3) > 0) {
            menu.setOption(9, new IconMenuItem().setMaterial(GOLD_PICKAXE).setAmount(buffLevel.get(3)).setTitle(GOLD + Locales.getString("Name." + buffNames.get(3), myPet.getOwner().getLanguage()) + GRAY + " " + Util.decimal2roman(buffLevel.get(3))));
        }
        if (buffLevel.get(5) > 0) {
            menu.setOption(18, new IconMenuItem().setMaterial(DIAMOND_SWORD).setAmount(buffLevel.get(5)).setTitle(GOLD + Locales.getString("Name." + buffNames.get(5), myPet.getOwner().getLanguage()) + GRAY + " " + Util.decimal2roman(buffLevel.get(5))));
        }
        if (buffLevel.get(8) > 0) {
            menu.setOption(1, new IconMenuItem().setMaterial(FIREWORK).setAmount(buffLevel.get(8)).setTitle(GOLD + Locales.getString("Name." + buffNames.get(8), myPet.getOwner().getLanguage()) + GRAY + " " + Util.decimal2roman(buffLevel.get(8))));
        }
        if (buffLevel.get(10) > 0) {
            menu.setOption(10, new IconMenuItem().setMaterial(APPLE).setAmount(buffLevel.get(10)).setTitle(GOLD + Locales.getString("Name." + buffNames.get(10), myPet.getOwner().getLanguage()) + GRAY + " " + Util.decimal2roman(buffLevel.get(10))));
        }
        if (buffLevel.get(11) > 0) {
            menu.setOption(19, new IconMenuItem().setMaterial(DIAMOND_CHESTPLATE).setAmount(buffLevel.get(11)).setTitle(GOLD + Locales.getString("Name." + buffNames.get(11), myPet.getOwner().getLanguage()) + GRAY + " " + Util.decimal2roman(buffLevel.get(11))));
        }
        if (buffLevel.get(12) > 0) {
            menu.setOption(7, new IconMenuItem().setMaterial(LAVA_BUCKET).setAmount(buffLevel.get(12)).setTitle(GOLD + Locales.getString("Name." + buffNames.get(12), myPet.getOwner().getLanguage()) + GRAY + " " + Util.decimal2roman(buffLevel.get(12))));
        }
        if (buffLevel.get(13) > 0) {
            menu.setOption(16, new IconMenuItem().setMaterial(RAW_FISH).setAmount(buffLevel.get(13)).setTitle(GOLD + Locales.getString("Name." + buffNames.get(13), myPet.getOwner().getLanguage()) + GRAY + " " + Util.decimal2roman(buffLevel.get(13))));
        }
        if (buffLevel.get(14) > 0) {
            menu.setOption(25, new IconMenuItem().setMaterial(EYE_OF_ENDER).setAmount(buffLevel.get(14)).setTitle(GOLD + Locales.getString("Name." + buffNames.get(14), myPet.getOwner().getLanguage()) + GRAY + " " + Util.decimal2roman(buffLevel.get(14))));
        }
        if (buffLevel.get(16) > 0) {
            menu.setOption(8, new IconMenuItem().setMaterial(TORCH).setAmount(buffLevel.get(16)).setTitle(GOLD + Locales.getString("Name." + buffNames.get(16), myPet.getOwner().getLanguage()) + GRAY + " " + Util.decimal2roman(buffLevel.get(16))));
        }
        if (buffLevel.get(21) > 0) {
            menu.setOption(17, new IconMenuItem().setMaterial(GOLDEN_APPLE).setAmount(buffLevel.get(21)).setTitle(GOLD + Locales.getString("Name." + buffNames.get(21), myPet.getOwner().getLanguage()) + GRAY + " " + Util.decimal2roman(buffLevel.get(21))));
        }
        if (buffLevel.get(22) > 0) {
            menu.setOption(26, new IconMenuItem().setMaterial(SPONGE).setAmount(buffLevel.get(22)).setTitle(GOLD + Locales.getString("Name." + buffNames.get(22), myPet.getOwner().getLanguage()) + GRAY + " " + Util.decimal2roman(buffLevel.get(22))));
        }

        for (int buff : selectedBuffs) {
            if (buffLevel.get(buff) > 0) {
                menu.getOption(buffItemPositions.get(buff)).setGlowing(true);
            } else {
                selectedBuffs.remove(buff);
            }
        }

        if (selectableBuffs > 1) {
            if (selectableBuffs > selectedBuffs.size()) {
                menu.setOption(13, new IconMenuItem().setMaterial(POTION).setTitle(BLUE + Util.formatText(Locales.getString("Message.Skill.Beacon.RemainingBuffs", myPet.getOwner().getLanguage()), selectableBuffs - selectedBuffs.size())).setAmount(selectableBuffs - selectedBuffs.size()));
            } else {
                menu.setOption(13, new IconMenuItem().setMaterial(GLASS_BOTTLE).setTitle(GRAY + Util.formatText(Locales.getString("Message.Skill.Beacon.RemainingBuffs", myPet.getOwner().getLanguage()), 0)));
            }
        }

        menu.open(owner);

        return true;
    }

    public void upgrade(ISkillInfo upgrade, boolean quiet) {
        if (upgrade instanceof BeaconInfo) {

            TagCompound TagCompound = upgrade.getProperties();

            if (TagCompound.getCompoundData().containsKey("buff_speed_boost_enable")) {
                if (TagCompound.getAs("buff_speed_boost_enable", TagByte.class).getBooleanData()) {
                    if (TagCompound.getCompoundData().containsKey("buff_speed_boost_level")) {
                        buffLevel.put(1, TagCompound.getAs("buff_speed_boost_level", TagInt.class).getIntData());
                    }
                } else {
                    buffLevel.put(1, 0);
                }
            }
            if (TagCompound.getCompoundData().containsKey("buff_haste_enable")) {
                if (TagCompound.getAs("buff_haste_enable", TagByte.class).getBooleanData()) {
                    if (TagCompound.getCompoundData().containsKey("buff_haste_level")) {
                        buffLevel.put(3, TagCompound.getAs("buff_haste_level", TagInt.class).getIntData());
                    }
                } else {
                    buffLevel.put(3, 0);
                }
            }
            if (TagCompound.getCompoundData().containsKey("buff_strength_enable")) {
                if (TagCompound.getAs("buff_strength_enable", TagByte.class).getBooleanData()) {
                    if (TagCompound.getCompoundData().containsKey("buff_strength_level")) {
                        buffLevel.put(5, TagCompound.getAs("buff_strength_level", TagInt.class).getIntData());
                    }
                } else {
                    buffLevel.put(5, 0);
                }
            }
            if (TagCompound.getCompoundData().containsKey("buff_jump_boost_enable")) {
                if (TagCompound.getAs("buff_jump_boost_enable", TagByte.class).getBooleanData()) {
                    if (TagCompound.getCompoundData().containsKey("buff_jump_boost_level")) {
                        buffLevel.put(8, TagCompound.getAs("buff_jump_boost_level", TagInt.class).getIntData());
                    }
                } else {
                    buffLevel.put(8, 0);
                }
            }
            if (TagCompound.getCompoundData().containsKey("buff_regeneration_enable")) {
                if (TagCompound.getAs("buff_regeneration_enable", TagByte.class).getBooleanData()) {
                    if (TagCompound.getCompoundData().containsKey("buff_regeneration_level")) {
                        buffLevel.put(10, TagCompound.getAs("buff_regeneration_level", TagInt.class).getIntData());
                    }
                } else {
                    buffLevel.put(10, 0);
                }
            }
            if (TagCompound.getCompoundData().containsKey("buff_resistance_enable")) {
                if (TagCompound.getAs("buff_resistance_enable", TagByte.class).getBooleanData()) {
                    if (TagCompound.getCompoundData().containsKey("buff_resistance_level")) {
                        buffLevel.put(11, TagCompound.getAs("buff_resistance_level", TagInt.class).getIntData());
                    }
                } else {
                    buffLevel.put(11, 0);
                }
            }
            if (TagCompound.getCompoundData().containsKey("buff_fire_resistance_enable")) {
                buffLevel.put(12, (int) TagCompound.getAs("buff_fire_resistance_enable", TagByte.class).getByteData());
            }
            if (TagCompound.getCompoundData().containsKey("buff_water_breathing_enable")) {
                buffLevel.put(13, (int) TagCompound.getAs("buff_water_breathing_enable", TagByte.class).getByteData());
            }
            if (TagCompound.getCompoundData().containsKey("buff_invisibility_enable")) {
                buffLevel.put(14, (int) TagCompound.getAs("buff_invisibility_enable", TagByte.class).getByteData());
            }
            if (TagCompound.getCompoundData().containsKey("buff_night_vision_enable")) {
                buffLevel.put(16, (int) TagCompound.getAs("buff_night_vision_enable", TagByte.class).getByteData());
            }
            if (TagCompound.getCompoundData().containsKey("buff_health_boost_enable")) {
                if (TagCompound.getAs("buff_health_boost_enable", TagByte.class).getBooleanData()) {
                    if (TagCompound.getCompoundData().containsKey("buff_health_boost_level")) {
                        buffLevel.put(21, TagCompound.getAs("buff_health_boost_level", TagInt.class).getIntData());
                    }
                } else {
                    buffLevel.put(21, 0);
                }
            }
            if (TagCompound.getCompoundData().containsKey("buff_absorption_enable")) {
                if (TagCompound.getAs("buff_absorption_enable", TagByte.class).getBooleanData()) {
                    if (TagCompound.getCompoundData().containsKey("buff_absorption_level")) {
                        buffLevel.put(22, TagCompound.getAs("buff_absorption_level", TagInt.class).getIntData());
                    }
                } else {
                    buffLevel.put(22, 0);
                }
            }

            if (upgrade.getProperties().getCompoundData().containsKey("duration")) {
                if (!upgrade.getProperties().getCompoundData().containsKey("addset_duration") || upgrade.getProperties().getAs("addset_duration", TagString.class).getStringData().equals("add")) {
                    duration += upgrade.getProperties().getAs("duration", TagInt.class).getIntData();
                } else {
                    duration = upgrade.getProperties().getAs("duration", TagInt.class).getIntData();
                }
            }
            if (upgrade.getProperties().getCompoundData().containsKey("range")) {
                if (!upgrade.getProperties().getCompoundData().containsKey("addset_range") || upgrade.getProperties().getAs("addset_range", TagString.class).getStringData().equals("add")) {
                    range += upgrade.getProperties().getAs("range", TagDouble.class).getDoubleData();
                } else {
                    range = upgrade.getProperties().getAs("range", TagDouble.class).getDoubleData();
                }
            }
            if (upgrade.getProperties().getCompoundData().containsKey("selection_count")) {
                if (upgrade.getProperties().getCompoundData().containsKey("addset_selection_count") && upgrade.getProperties().getAs("addset_selection_count", TagString.class).getStringData().equals("add")) {
                    selectableBuffs += upgrade.getProperties().getAs("selection_count", TagInt.class).getIntData();
                } else {
                    selectableBuffs = upgrade.getProperties().getAs("selection_count", TagInt.class).getIntData();
                }
                selectableBuffs = selectableBuffs > 12 ? 12 : selectableBuffs;
            }

            if (!quiet) {
                myPet.sendMessageToOwner(Util.formatText(Locales.getString("Message.Skill.Beacon.Upgrade", myPet.getOwner().getLanguage()), myPet.getPetName(), String.format("%1.2f", range), duration));
                myPet.sendMessageToOwner(" " + getFormattedValue());
            }
        }
    }

    public String getFormattedValue() {
        String availableBuffs = "";
        for (int primaryBuffId : buffLevel.keySet()) {
            if (primaryBuffId != 0 && buffLevel.get(primaryBuffId) > 0) {
                if (!availableBuffs.equalsIgnoreCase("")) {
                    availableBuffs += ", ";
                }
                availableBuffs += GOLD + Locales.getString("Name." + buffNames.get(primaryBuffId), myPet.getOwner().getLanguage());
                availableBuffs += GRAY + " " + Util.decimal2roman(buffLevel.get(primaryBuffId));
                availableBuffs += ChatColor.RESET;
            }
        }
        return availableBuffs;
    }

    public void reset() {
        range = 0;
        duration = 0;
        selectedBuffs.clear();
        active = false;

        buffLevel.put(1, 0);
        buffLevel.put(3, 0);
        buffLevel.put(5, 0);
        buffLevel.put(8, 0);
        buffLevel.put(10, 0);
        buffLevel.put(11, 0);
        buffLevel.put(12, 0);
        buffLevel.put(13, 0);
        buffLevel.put(14, 0);
        buffLevel.put(16, 0);
        buffLevel.put(21, 0);
        buffLevel.put(22, 0);
    }

    public void schedule() {
        if (myPet.getStatus() == MyPet.PetState.Here && isActive() && active && selectedBuffs.size() != 0 && --beaconTimer <= 0) {
            beaconTimer = 2;

            double range = this.range * myPet.getHungerValue() / 100.;

            if (range < 0.7) {
                active = false;
                selectedBuffs.clear();
            }

            if (selectedBuffs.size() > selectableBuffs) {
                int usableBuff = 0;
                for (int buff : selectedBuffs) {
                    if (buffLevel.get(buff) > 0) {
                        usableBuff = buff;
                    }
                }
                selectedBuffs.clear();
                if (usableBuff != 0) {
                    selectedBuffs.add(usableBuff);
                }
            }

            BukkitUtil.playParticleEffect(myPet.getLocation().add(0, 1, 0), "witchMagic", 0.2F, 0.2F, 0.2F, 0.1F, 5, 20);

            targetLoop:
            for (Object entityObj : this.myPet.getCraftPet().getHandle().world.a(EntityHuman.class, myPet.getCraftPet().getHandle().boundingBox.grow(range, range, range))) {
                EntityHuman entityHuman = (EntityHuman) entityObj;

                if (!entityHuman.getBukkitEntity().equals(Bukkit.getPlayer(entityHuman.getName()))) {
                    continue;
                }
                switch (reciever) {
                    case Owner:
                        if (!myPet.getOwner().equals(entityHuman)) {
                            continue targetLoop;
                        } else {
                            int amplification;
                            for (int buff : selectedBuffs) {
                                amplification = buffLevel.get(buff) - 1;
                                if (entityHuman.hasEffect(buff)) {
                                    MobEffect effect = entityHuman.getEffect(buffEffectLists.get(buff));
                                    effect.a(new MobEffect(buff, duration * 20, amplification, true));
                                    entityHuman.updateEffects = true;
                                } else {
                                    entityHuman.addEffect(new MobEffect(buff, duration * 20, amplification, true));
                                }
                            }
                            BukkitUtil.playParticleEffect(entityHuman.getBukkitEntity().getLocation().add(0, 1, 0), "instantSpell", 0.2F, 0.2F, 0.2F, 0.1F, 5, 20);
                            break targetLoop;
                        }
                    case Everyone:
                        int amplification;
                        for (int buff : selectedBuffs) {
                            amplification = buffLevel.get(buff) - 1;
                            if (entityHuman.hasEffect(buff)) {
                                MobEffect effect = entityHuman.getEffect(buffEffectLists.get(buff));
                                effect.a(new MobEffect(buff, duration * 20, amplification, true));
                                entityHuman.updateEffects = true;
                            } else {
                                entityHuman.addEffect(new MobEffect(buff, duration * 20, amplification, true));
                            }
                        }
                        BukkitUtil.playParticleEffect(entityHuman.getBukkitEntity().getLocation().add(0, 1, 0), "instantSpell", 0.2F, 0.2F, 0.2F, 0.1F, 5, 20);
                        break;
                }
            }

            if (HUNGER_DECREASE_TIME > 0 && hungerDecreaseTimer-- < 0) {
                myPet.setHungerValue(myPet.getHungerValue() - 1);
                hungerDecreaseTimer = HUNGER_DECREASE_TIME;
            }
        }
    }

    public int getDuration() {
        return duration;
    }

    @Override
    public ISkillInstance cloneSkill() {
        Beacon newSkill = new Beacon(this.isAddedByInheritance());
        newSkill.setProperties(getProperties());
        return newSkill;
    }

    @Override
    public TagCompound save() {
        TagCompound nbtTagCompound = new TagCompound();
        nbtTagCompound.getCompoundData().put("Buffs", new TagIntArray(ArrayUtils.toPrimitive(selectedBuffs.toArray(new Integer[selectedBuffs.size()]))));
        nbtTagCompound.getCompoundData().put("Active", new TagByte(this.active));
        nbtTagCompound.getCompoundData().put("Reciever", new TagString(this.reciever.name()));
        return nbtTagCompound;
    }

    @Override
    public void load(TagCompound compound) {
        if (compound.getCompoundData().containsKey("Buff")) {
            int oldSelectedBuff = compound.getAs("Buff", TagInt.class).getIntData();
            if (oldSelectedBuff != 0) {
                this.selectedBuffs.add(oldSelectedBuff);
            }
        }
        if (compound.getCompoundData().containsKey("Buffs")) {
            int[] selectedBuffs = compound.getAs("Buffs", TagIntArray.class).getIntArrayData();
            if (selectedBuffs.length != 0) {
                for (int selectedBuff : selectedBuffs) {
                    this.selectedBuffs.add(selectedBuff);
                }
            }
        }
        if (compound.getCompoundData().containsKey("Active")) {
            this.active = compound.getAs("Active", TagByte.class).getBooleanData();
        }
        if (compound.getCompoundData().containsKey("Reciever")) {
            this.reciever = BeaconReciever.valueOf(compound.getAs("Reciever", TagString.class).getStringData());
        }
    }
}