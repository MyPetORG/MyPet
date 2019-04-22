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

package de.Keyle.MyPet.util.hooks.citizens;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.util.WalletType;
import de.Keyle.MyPet.util.hooks.VaultHook;
import net.citizensnpcs.api.exception.NPCLoadException;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.trait.trait.Owner;
import net.citizensnpcs.api.util.DataKey;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

public class WalletTrait extends Trait {

    private double credit = 0.0D;
    private WalletType type = WalletType.Private;
    private String account = "";

    public WalletTrait() {
        super("mypet-wallet");
    }

    public void load(DataKey key) throws NPCLoadException {
        String type = key.getString("type", "Private");
        if (type.equalsIgnoreCase("Owner")) {
            type = "Player";
        }
        WalletType wt = WalletType.getByName(type);
        if (wt != null) {
            this.type = wt;
        } else {
            this.type = WalletType.Private;
        }
        account = key.getString("accountName", key.getString("account", ""));
        credit = key.getDouble("privateWallet", key.getDouble("credit", 0D));
    }

    public void save(DataKey key) {
        if (key.getString("walletTypeName") != null && !key.getString("walletTypeName").isEmpty()) {
            key.removeKey("walletTypeName");
        }
        if (key.getString("accountName") != null && !key.getString("accountName").isEmpty()) {
            key.removeKey("accountName");
        }
        if (key.getString("privateWallet") != null && !key.getString("privateWallet").isEmpty()) {
            key.removeKey("privateWallet");
        }

        key.setString("type", this.type.name());
        key.setString("account", this.account);
        key.setDouble("credit", this.credit);
    }

    public void setWalletType(WalletType newType) {
        type = newType;
    }

    public void setAccount(String accountName) {
        this.account = accountName;
    }

    public boolean deposit(double amount) {
        if (amount <= 0.0D) {
            return false;
        }
        switch (type) {
            case Private:
                this.credit += amount;
                return true;
            case Player:
                if (!MyPetApi.getHookHelper().isEconomyEnabled()) {
                    MyPetApi.getPlugin().getLogger().info(ChatColor.RED + "The MyPet-Wallet trait needs an economy plugin to use the \"Owner\" wallet type! (NPC: " + this.getNPC().getId() + ")");
                    return false;
                }
                return ((VaultHook) MyPetApi.getHookHelper().getEconomy()).getEconomy().depositPlayer(Bukkit.getOfflinePlayer(this.npc.getTrait(Owner.class).getOwnerId()), amount).transactionSuccess();
            case Bank:
                if (!MyPetApi.getHookHelper().isEconomyEnabled()) {
                    MyPetApi.getPlugin().getLogger().info(ChatColor.RED + "The MyPet-Wallet trait needs an economy plugin to use the \"Bank\" wallet type! (NPC: " + this.getNPC().getId() + ")");
                    return false;
                }
                return ((VaultHook) MyPetApi.getHookHelper().getEconomy()).getEconomy().isBankOwner(account, Bukkit.getOfflinePlayer(this.npc.getTrait(Owner.class).getOwnerId())).transactionSuccess() && ((VaultHook) MyPetApi.getHookHelper().getEconomy()).getEconomy().bankDeposit(account, amount).transactionSuccess();
            case None:
                return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "MyPetWalletTrait{type: " + type + ", credit: " + String.format("%1.4f", credit) + ", account: " + account + "}";
    }
}
