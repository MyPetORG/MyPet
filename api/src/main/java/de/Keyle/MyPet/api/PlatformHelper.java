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

package de.Keyle.MyPet.api;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPetMinecraftEntity;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.util.ErrorUtil;
import de.Keyle.MyPet.api.util.locale.Translation;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.UUID;

public abstract class PlatformHelper {

    public abstract boolean canSpawn(Location loc, MyPetMinecraftEntity entity);

    public String getPlayerLanguage(Player player) {
        String locale = player.getLocale();
        if (locale.isEmpty()) {
            return "en_us";
        }
        return locale;
    }

    public abstract CompoundBinaryTag entityToTag(Entity entity);

    public abstract void applyTagToEntity(CompoundBinaryTag tag, Entity entity);

    public String getCommandSenderLanguage(CommandSender sender) {
        if (sender instanceof Player player) {
            return getPlayerLanguage(player);
        }
        return "en";
    }

    public boolean copyResource(Plugin plugin, String ressource, File destination) {
        try (InputStream template = plugin.getResource(ressource);
             OutputStream out = Files.newOutputStream(destination.toPath())) {
            if (template == null) {
                return false;
            }
            template.transferTo(out);
            return true;
        } catch (IOException e) {
            ErrorUtil.report(e);
            return false;
        }
    }

    public abstract CompoundBinaryTag itemStackToCompound(ItemStack itemStack);

    public abstract ItemStack compoundToItemStack(CompoundBinaryTag compound);

    /**
     * Builds the action bar message for pet health updates so it can be reused by all NMS modules.
     * This method only constructs the message; caller is responsible for sending and for any gating (e.g., config flags).
     */
    public Component buildPetHealthActionBar(MyPet myPet, double health, double maxHealth) {
        if (myPet == null) {
            return Component.empty();
        }
        double deltaHealth = maxHealth - health;

        NamedTextColor healthColor = NamedTextColor.RED;
        if (health > maxHealth / 3 * 2) {
            healthColor = NamedTextColor.GREEN;
        } else if (health > maxHealth / 3) {
            healthColor = NamedTextColor.YELLOW;
        }
        Component parsed = myPet.getDisplayName()
                .append(MyPetApi.getPlugin().miniMessage().deserialize("<reset>: "));
        if (health > 0) {
            parsed = parsed.append(MyPetApi.getPlugin().miniMessage().deserialize(
                    "<healthcolor><health><white>/<maxhealth> ",
                    Placeholder.styling("healthcolor", healthColor),
                    Placeholder.unparsed("health", String.format("%1.2f", health)),
                    Placeholder.unparsed("maxhealth", String.format("%1.2f", maxHealth))));
            if (!myPet.getOwner().isHealthBarActive()) {
                parsed = parsed.append(MyPetApi.getPlugin().miniMessage().deserialize(
                        "(<deltahealthcolor><deltahealth><reset>)",
                        Placeholder.parsed("deltahealthcolor", deltaHealth < 0 ? "<green>+" : "<red>-"),
                        Placeholder.unparsed("deltahealth", String.format("%1.2f", deltaHealth))));
            }
        } else {
            parsed = parsed.append(MyPetApi.getPlugin().miniMessage().deserialize(
                    "<dead>",
                    Placeholder.unparsed("dead", Translation.getString("Name.Dead", myPet.getOwner()))));
        }
        return parsed;
    }

    public abstract void addZombieTargetGoal(Zombie zombie);

    public abstract boolean comparePlayerWithEntity(MyPetPlayer player, Object obj);

    public abstract boolean isEquipment(ItemStack itemStack);


    public abstract void doPickupAnimation(Entity entity, Entity target);

    public abstract Entity getEntity(int id, World world);

    public double distanceSquared(Location a, Location b) {
        if (!a.getWorld().equals(b.getWorld())) {
            return Double.MAX_VALUE;
        }
        return a.distanceSquared(b);
    }

    public double distance(Location a, Location b) {
        if (!a.getWorld().equals(b.getWorld())) {
            return Double.MAX_VALUE;
        }
        return Math.sqrt(distanceSquared(a, b));
    }

    public boolean compareBlockPositions(Location a, Location b) {
        return a.getBlockX() == b.getBlockX() &&
                a.getBlockY() == b.getBlockY() &&
                a.getBlockZ() == b.getBlockZ();
    }

    public Entity getEntityByUUID(UUID uuid) {
        return Bukkit.getEntity(uuid);
    }

    public abstract String getLastDamageSource(LivingEntity e);

    public abstract String itemstackToString(ItemStack itemStack);

    public abstract boolean gameruleDoDeathMessages(LivingEntity e);

    public boolean doStackWalking(Class<?> leClass, int oldDepth) {
        return false;
    }
}
