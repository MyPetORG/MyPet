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

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.util.hooks.PluginHookName;
import de.Keyle.MyPet.api.util.hooks.types.LeashHook;
import de.Keyle.MyPet.api.util.hooks.types.PlayerVersusEntityHook;
import de.Keyle.MyPet.api.util.hooks.types.PlayerVersusPlayerHook;
import de.Keyle.MyPet.commands.CommandAdmin;
import de.Keyle.MyPet.commands.admin.CommandOptionNpc;
import de.Keyle.MyPet.util.hooks.citizens.ShopTrait;
import de.Keyle.MyPet.util.hooks.citizens.StorageTrait;
import de.Keyle.MyPet.util.hooks.citizens.WalletTrait;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.TraitInfo;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

@PluginHookName("Citizens")
public class CitizensHook implements PlayerVersusEntityHook, PlayerVersusPlayerHook, LeashHook {

    public static double NPC_STORAGE_COSTS_FIXED = 5;
    public static double NPC_STORAGE_COSTS_FACTOR = 1;

    TraitInfo storageTrait;
    TraitInfo walletTrait;
    TraitInfo shopTrait;

    @Override
    public boolean onEnable() {
        storageTrait = TraitInfo.create(StorageTrait.class).withName("mypet-storage");
        walletTrait = TraitInfo.create(WalletTrait.class).withName("mypet-wallet");
        shopTrait = TraitInfo.create(ShopTrait.class).withName("mypet-shop");

        CitizensAPI.getTraitFactory().registerTrait(storageTrait);
        CitizensAPI.getTraitFactory().registerTrait(walletTrait);
        CitizensAPI.getTraitFactory().registerTrait(shopTrait);

        CommandAdmin.COMMAND_OPTIONS.put("npc", new CommandOptionNpc());

        Plugin npcPlugin = Bukkit.getPluginManager().getPlugin("MyPet-NPC");
        if (npcPlugin != null) {
            MyPetApi.getLogger().warning("MyPet-NPC is included into MyPet now. Please remove the MyPet-NPC plugin!");
            npcPlugin.setNaggable(false);
        }
        return true;
    }

    @Override
    public void onDisable() {
        try {
            if (CitizensAPI.hasImplementation()) {
                CitizensAPI.getTraitFactory().deregisterTrait(storageTrait);
                CitizensAPI.getTraitFactory().deregisterTrait(walletTrait);
                CitizensAPI.getTraitFactory().deregisterTrait(shopTrait);
            }
        } catch (Error ignored) {
        }
    }

    @Override
    public void loadConfig(ConfigurationSection config) {
        config.addDefault("Storage-Trait.Costs.Fixed", NPC_STORAGE_COSTS_FIXED);
        config.addDefault("Storage-Trait.Costs.Factor", NPC_STORAGE_COSTS_FACTOR);

        NPC_STORAGE_COSTS_FACTOR = config.getDouble("Storage-Trait.Costs.Factor", 5.0);
        NPC_STORAGE_COSTS_FIXED = config.getDouble("Storage-Trait.Costs.Fixed", 5.0);
    }

    public boolean canHurt(Player attacker, Entity defender) {
        try {
            if (CitizensAPI.getNPCRegistry().isNPC(defender)) {
                NPC npc = CitizensAPI.getNPCRegistry().getNPC(defender);
                if (npc == null || npc.data() == null) {
                    return true;
                }
                return !npc.data().get("protected", true);
            }
        } catch (Throwable ignored) {
        }
        return true;
    }

    @Override
    public boolean canHurt(Player attacker, Player defender) {
        return canHurt(attacker, (Entity) defender);
    }

    @Override
    public boolean canLeash(Player attacker, Entity defender) {
        try {
            return !CitizensAPI.getNPCRegistry().isNPC(defender);
        } catch (Throwable ignored) {
        }
        return true;
    }
}