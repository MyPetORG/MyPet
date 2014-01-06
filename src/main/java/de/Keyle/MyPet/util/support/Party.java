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

package de.Keyle.MyPet.util.support;

import com.ancientshores.AncientRPG.API.ApiManager;
import com.ancientshores.AncientRPG.Party.AncientRPGParty;
import com.gmail.nossr50.api.PartyAPI;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.party.HeroParty;
import org.bukkit.entity.Player;

public class Party {
    public static boolean partyUsable = false;
    public static boolean searchedForPartyPlugins = false;

    public static boolean isPartyUsable() {
        if (!searchedForPartyPlugins) {
            partyUsable = PluginSupportManager.isPluginUsable("Heroes") ||
                    PluginSupportManager.isPluginUsable("mcMMO") ||
                    PluginSupportManager.isPluginUsable("AncientRPG");
        }
        return partyUsable;
    }


    public static boolean isInSameParty(Player player1, Player player2) {
        return isInSamePartyHeroes(player1, player2) || isInSamePartyMcMMO(player1, player2) || isInSamePartyAncientRPG(player1, player2);
    }

    public static boolean isInSamePartyHeroes(Player player1, Player player2) throws NoClassDefFoundError {
        if (PluginSupportManager.isPluginUsable("Heroes")) {
            try {
                Heroes heroes = PluginSupportManager.getPluginInstance(Heroes.class);
                Hero heroAttacker = heroes.getCharacterManager().getHero(player1);
                Hero heroDefender = heroes.getCharacterManager().getHero(player2);
                HeroParty party = heroDefender.getParty();
                if (party == null || !party.isPartyMember(heroAttacker)) {
                    return false;
                }
                return true;
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    public static boolean isInSamePartyMcMMO(Player player1, Player player2) {
        if (PluginSupportManager.isPluginUsable("mcMMO")) {
            try {
                return PartyAPI.inSameParty(player1, player2);
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    public static boolean isInSamePartyAncientRPG(Player player1, Player player2) {
        if (PluginSupportManager.isPluginUsable("AncientRPG")) {
            try {
                ApiManager api = ApiManager.getApiManager();
                AncientRPGParty party = api.getPlayerParty(player1);
                return party != null && party.containsName(player2.getName());
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    public static void reset() {
        searchedForPartyPlugins = false;
        partyUsable = false;
    }
}