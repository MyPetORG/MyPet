/*
 * This file is part of mypet-v1_8_R3
 *
 * Copyright (C) 2011-2016 Keyle
 * mypet-v1_8_R3 is licensed under the GNU Lesser General Public License.
 *
 * mypet-v1_8_R3 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * mypet-v1_8_R3 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.util.hooks;

import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.util.hooks.EconomyHook;
import de.Keyle.MyPet.api.util.hooks.HookManager;
import de.Keyle.MyPet.api.util.hooks.PluginHookManager;
import de.Keyle.MyPet.util.hooks.arenas.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.List;

public class Hooks extends HookManager {
    public Hooks() {
        enable();
    }

    public void enable() {
        MagicSpellsHook.findPlugin();
        SkillApi.findPlugin();
        MobArena.findPlugin();
        Minigames.findPlugin();
        PvPArena.findPlugin();
        BattleArena.findPlugin();
        SurvivalGames.findPlugin();
        UltimateSurvivalGames.findPlugin();
        MyHungerGames.findPlugin();
        if (PluginHookManager.isPluginUsable("ProtocolLib")) {
            ProtocolLib.findPlugin();
        }
    }

    public static void disable() {
        PvPChecker.reset();
        EconomyHook.reset();
        ProtocolLib.disable();
    }

    @Override
    public boolean canHurt(Player attacker, Player victim, boolean viceversa) {
        return PvPChecker.canHurt(attacker, victim, viceversa);
    }

    @Override
    public boolean canHurt(Player attacker, Player victim) {
        return PvPChecker.canHurt(attacker, victim);
    }

    @Override
    public boolean canHurt(Player attacker, Entity victim) {
        return PvPChecker.canHurt(attacker, victim);
    }

    @Override
    public boolean canUseMyPet(MyPetPlayer player) {
        if (MobArena.isInMobArena(player)) {
            return false;
        }
        if (Minigames.isInMinigame(player)) {
            return false;
        }
        if (BattleArena.isInBattleArena(player)) {
            return false;
        }
        if (PvPArena.isInPvPArena(player)) {
            return false;
        }
        if (MyHungerGames.isInHungerGames(player)) {
            return false;
        }
        if (UltimateSurvivalGames.isInSurvivalGames(player)) {
            return false;
        }
        if (SurvivalGames.isInSurvivalGames(player)) {
            return false;
        }
        return true;
    }

    @Override
    public boolean isInParty(Player player) {
        return PartyManager.isInParty(player);
    }

    @Override
    public List<Player> getPartyMembers(Player player) {
        return PartyManager.getPartyMembers(player);
    }
}
