/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2018 Keyle
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
import com.gmail.nossr50.datatypes.skills.SkillType;
import com.gmail.nossr50.util.player.UserManager;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.entity.leashing.LeashFlag;
import de.Keyle.MyPet.api.entity.leashing.LeashFlagName;
import de.Keyle.MyPet.api.entity.leashing.LeashFlagSetting;
import de.Keyle.MyPet.api.entity.leashing.LeashFlagSettings;
import de.Keyle.MyPet.api.util.hooks.PluginHookName;
import de.Keyle.MyPet.api.util.hooks.types.PartyHook;
import de.Keyle.MyPet.api.util.hooks.types.PlayerVersusPlayerHook;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.List;

@PluginHookName("mcMMO")
public class McMMOHook implements PlayerVersusPlayerHook, PartyHook {

    @Override
    public boolean onEnable() {
        if (Configuration.Hooks.USE_McMMO) {
            MyPetApi.getLeashFlagManager().registerLeashFlag(new JobLevelFlag());
            return true;
        }
        return false;
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
        public boolean check(Player player, LivingEntity entity, double damage, LeashFlagSettings settings) {
            for (SkillType skillType : SkillType.values()) {
                if (settings.map().containsKey(skillType.getName().toLowerCase())) {
                    LeashFlagSetting setting = settings.map().get(skillType.getName().toLowerCase());
                    if (Util.isInt(setting.getValue())) {
                        int requiredLevel = Integer.parseInt(setting.getValue());
                        McMMOPlayer mmoPlayer = UserManager.getPlayer(player);
                        if (mmoPlayer.getSkillLevel(skillType) < requiredLevel) {
                            return false;
                        }
                    }
                }
            }

            return true;
        }
    }
}