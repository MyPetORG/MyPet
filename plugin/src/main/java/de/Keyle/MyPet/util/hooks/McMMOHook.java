/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2019 Keyle
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

package de.Keyle.MyPet.util.hooks;

import com.gmail.nossr50.api.PartyAPI;
import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.util.player.UserManager;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.entity.leashing.LeashFlag;
import de.Keyle.MyPet.api.entity.leashing.LeashFlagName;
import de.Keyle.MyPet.api.util.ReflectionUtil;
import de.Keyle.MyPet.api.util.configuration.settings.Setting;
import de.Keyle.MyPet.api.util.configuration.settings.Settings;
import de.Keyle.MyPet.api.util.hooks.PluginHookName;
import de.Keyle.MyPet.api.util.hooks.types.PartyHook;
import de.Keyle.MyPet.api.util.hooks.types.PlayerVersusPlayerHook;
import de.Keyle.MyPet.api.util.locale.Translation;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@PluginHookName("mcMMO")
public class McMMOHook implements PlayerVersusPlayerHook, PartyHook {

    private static Method METHOD_SkillType_values;
    private static Method METHOD_SkillType_getName;
    private static Method METHOD_McMMOPlayer_getSkillLevel;

    @Override
    public boolean onEnable() {
        boolean isV2;
        try {
            Class.forName("com.gmail.nossr50.datatypes.skills.PrimarySkillType");
            isV2 = true;
        } catch (ClassNotFoundException e) {
            isV2 = false;
        }
        Class skillTypeClass;
        if (isV2) {
            skillTypeClass = ReflectionUtil.getClass("com.gmail.nossr50.datatypes.skills.PrimarySkillType");
        } else {
            skillTypeClass = ReflectionUtil.getClass("com.gmail.nossr50.datatypes.skills.SkillType");
        }
        METHOD_SkillType_values = ReflectionUtil.getMethod(skillTypeClass, "values");
        METHOD_SkillType_getName = ReflectionUtil.getMethod(skillTypeClass, "getName");
        METHOD_McMMOPlayer_getSkillLevel = ReflectionUtil.getMethod(McMMOPlayer.class, "getSkillLevel", skillTypeClass);
        MyPetApi.getLeashFlagManager().registerLeashFlag(new JobLevelFlag());
        return true;
    }

    @Override
    public void onDisable() {
        MyPetApi.getLeashFlagManager().removeFlag("mcMMO");
    }

    @Override
    public boolean canHurt(Player attacker, Player defender) {
        try {
            return !PartyAPI.inSameParty(attacker, defender);
        } catch (Throwable ignored) {
        }
        return true;
    }

    @Override
    public boolean isInParty(Player player) {
        try {
            return PartyAPI.inParty(player);
        } catch (Throwable ignored) {
        }
        return false;
    }

    @Override
    public List<Player> getPartyMembers(Player player) {
        try {
            if (PartyAPI.inParty(player)) {
                String partyName = PartyAPI.getPartyName(player);
                return PartyAPI.getOnlineMembers(partyName);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    @LeashFlagName("mcMMO")
    class JobLevelFlag implements LeashFlag {

        @Override
        public boolean check(Player player, LivingEntity entity, double damage, Settings settings) {
            try {
                for (Object skillType : (Object[]) METHOD_SkillType_values.invoke(null)) {
                    String skillName = METHOD_SkillType_getName.invoke(skillType).toString().toLowerCase();
                    if (settings.map().containsKey(skillName)) {
                        Setting setting = settings.map().get(skillName);
                        if (Util.isInt(setting.getValue())) {
                            int requiredLevel = Integer.parseInt(setting.getValue());
                            McMMOPlayer mmoPlayer = UserManager.getPlayer(player);
                            int skillLevel = (int) METHOD_McMMOPlayer_getSkillLevel.invoke(mmoPlayer, skillType);
                            if (skillLevel < requiredLevel) {
                                return false;
                            }
                        }
                    }
                }
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
            return true;
        }

        @Override
        public String getMissingMessage(Player player, LivingEntity entity, double damage, Settings settings) {
            try {
                List<String> skills = new ArrayList<>();
                for (Object skillType : (Object[]) METHOD_SkillType_values.invoke(null)) {
                    String skillName = METHOD_SkillType_getName.invoke(skillType).toString();
                    if (settings.map().containsKey(skillName.toLowerCase())) {
                        Setting setting = settings.map().get(skillName.toLowerCase());
                        if (Util.isInt(setting.getValue())) {
                            int requiredLevel = Integer.parseInt(setting.getValue());
                            skills.add(skillName + ": " + Translation.getString("Name.Level", player) + " " + requiredLevel);
                        }
                    }
                }
                return "mcMMO: " + String.join(", ", skills);
            } catch (IllegalAccessException | InvocationTargetException e) {
                return null;
            }
        }
    }
}