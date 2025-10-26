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

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.party.HeroParty;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.util.hooks.PluginHookName;
import de.Keyle.MyPet.api.util.hooks.types.PartyHook;
import de.Keyle.MyPet.api.util.hooks.types.PlayerVersusPlayerHook;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

@PluginHookName("Heroes")
public class HeroesHook implements PlayerVersusPlayerHook, PartyHook {

    protected Heroes heroes;

    @Override
    public boolean onEnable() {
        heroes = MyPetApi.getPluginHookManager().getPluginInstance(Heroes.class).get();
        return true;
    }

    @Override
    public boolean canHurt(Player attacker, Player defender) {
        try {
            Hero heroAttacker = heroes.getCharacterManager().getHero(attacker);
            Hero heroDefender = heroes.getCharacterManager().getHero(defender);
            int attackerLevel = heroAttacker.getTieredLevel(false);
            int defenderLevel = heroDefender.getTieredLevel(false);

            if (Math.abs(attackerLevel - defenderLevel) > Heroes.properties.pvpLevelRange) {
                return false;
            }
            if ((defenderLevel < Heroes.properties.minPvpLevel) || (attackerLevel < Heroes.properties.minPvpLevel)) {
                return false;
            }
            HeroParty party = heroDefender.getParty();
            if ((party != null) && (party.isNoPvp()) && party.isPartyMember(heroAttacker)) {
                return false;
            }
        } catch (Throwable ignored) {
        }
        return true;
    }

    @Override
    public boolean isInParty(Player player) {
        try {
            Hero heroPlayer = heroes.getCharacterManager().getHero(player);
            return heroPlayer.getParty() != null;
        } catch (Exception ignored) {
        }
        return false;
    }

    @Override
    public List<Player> getPartyMembers(Player player) {
        try {
            Hero heroPlayer = heroes.getCharacterManager().getHero(player);
            if (heroPlayer.getParty() != null) {
                List<Player> members = new ArrayList<>();
                for (Hero hero : heroPlayer.getParty().getMembers()) {
                    if (hero.getPlayer().isOnline()) {
                        members.add(hero.getPlayer());
                    }
                }
                return members;
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}