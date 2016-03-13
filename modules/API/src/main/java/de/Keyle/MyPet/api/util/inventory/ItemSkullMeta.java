/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2016 Keyle
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

package de.Keyle.MyPet.api.util.inventory;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class ItemSkullMeta implements SkullMeta {
    @Override
    public String getOwner() {
        return null;
    }

    @Override
    public boolean hasOwner() {
        return false;
    }

    @Override
    public boolean setOwner(String owner) {
        return false;
    }

    @Override
    public boolean hasDisplayName() {
        return false;
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public void setDisplayName(String name) {

    }

    @Override
    public boolean hasLore() {
        return false;
    }

    @Override
    public List<String> getLore() {
        return null;
    }

    @Override
    public void setLore(List<String> lore) {

    }

    @Override
    public boolean hasEnchants() {
        return false;
    }

    @Override
    public boolean hasEnchant(Enchantment ench) {
        return false;
    }

    @Override
    public int getEnchantLevel(Enchantment ench) {
        return 0;
    }

    @Override
    public Map<Enchantment, Integer> getEnchants() {
        return null;
    }

    @Override
    public boolean addEnchant(Enchantment ench, int level, boolean ignoreLevelRestriction) {
        return false;
    }

    @Override
    public boolean removeEnchant(Enchantment ench) {
        return false;
    }

    @Override
    public boolean hasConflictingEnchant(Enchantment ench) {
        return false;
    }

    @Override
    public void addItemFlags(ItemFlag... itemFlags) {

    }

    @Override
    public void removeItemFlags(ItemFlag... itemFlags) {

    }

    @Override
    public Set<ItemFlag> getItemFlags() {
        return null;
    }

    @Override
    public boolean hasItemFlag(ItemFlag flag) {
        return false;
    }

    @Override
    public SkullMeta clone() {
        return null;
    }

    @Override
    public Map<String, Object> serialize() {
        return null;
    }
}
