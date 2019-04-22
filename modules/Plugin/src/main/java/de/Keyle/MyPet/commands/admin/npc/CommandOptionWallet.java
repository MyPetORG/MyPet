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

package de.Keyle.MyPet.commands.admin.npc;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.commands.CommandOptionTabCompleter;
import de.Keyle.MyPet.api.util.WalletType;
import de.Keyle.MyPet.util.hooks.VaultHook;
import de.Keyle.MyPet.util.hooks.citizens.WalletTrait;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CommandOptionWallet implements CommandOptionTabCompleter {

    private static List<String> walletTypeList = new ArrayList<>();

    static {
        for (WalletType walletType : WalletType.values()) {
            walletTypeList.add(walletType.name());
        }
    }

    @Override
    public boolean onCommandOption(CommandSender sender, String[] args) {
        if (args.length >= 1) {
            NPC selectedNPC = CitizensAPI.getDefaultNPCSelector().getSelected(sender);
            if (selectedNPC == null) {
                sender.sendMessage("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] No NPC seleced!");
                return true;
            }

            if (!selectedNPC.hasTrait(WalletTrait.class)) {
                sender.sendMessage("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] This NPC doesn't has the " + ChatColor.GOLD + "mypet-wallet" + ChatColor.RESET + " trait!");
                return true;
            }

            WalletType newWalletType = WalletType.getByName(args[0]);
            if (newWalletType == null) {
                sender.sendMessage("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] Invalid wallet type!");
                return true;
            }

            WalletTrait trait = selectedNPC.getTrait(WalletTrait.class);

            if (!MyPetApi.getHookHelper().isEconomyEnabled()) {
                if (newWalletType == WalletType.Bank || newWalletType == WalletType.Player) {
                    sender.sendMessage("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] You can not use the \"Player\" and \"Bank\" wallet types without an economy plugin installed!");
                    return true;
                }
            } else {
                if (newWalletType == WalletType.Bank && !((VaultHook) MyPetApi.getHookHelper().getEconomy()).getEconomy().hasBankSupport()) {
                    sender.sendMessage("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] Your economy plugin doesn't has \"Banks\" support!");
                    return true;
                }
            }

            trait.setWalletType(newWalletType);

            if (args.length >= 2) {
                trait.setAccount(args[1]);
            }

            sender.sendMessage("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] wallet trait updated.");
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, String[] strings) {
        if (strings.length == 3) {
            return walletTypeList;
        }
        return Collections.emptyList();
    }
}
