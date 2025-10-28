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
import de.Keyle.MyPet.api.compat.Compat;
import de.Keyle.MyPet.api.entity.MyPetMinecraftEntity;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.util.inventory.material.MaterialHolder;
import de.keyle.knbt.TagCompound;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.util.locale.Translation;
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

import java.io.*;
import java.nio.file.Files;
import java.util.UUID;

public abstract class PlatformHelper {

    /**
     * @param location the {@link Location} around which players must be to see the effect
     * @param effect   list of effects: https://gist.github.com/riking/5759002
     * @param offsetX  the amount to be randomly offset by in the X axis
     * @param offsetY  the amount to be randomly offset by in the Y axis
     * @param offsetZ  the amount to be randomly offset by in the Z axis
     * @param speed    the speed of the particles
     * @param count    the number of particles
     * @param radius   the radius around the location
     */
    public abstract void playParticleEffect(Location location, String effect, float offsetX, float offsetY, float offsetZ, float speed, int count, int radius, Compat<Object> data);

    public void playParticleEffect(Location location, String effect, float offsetX, float offsetY, float offsetZ, float speed, int count, int radius) {
        playParticleEffect(location, effect, offsetX, offsetY, offsetZ, speed, count, radius, null);
    }

    /**
     * @param location the {@link Location} around which players must be to see the effect
     * @param effect   list of effects: https://gist.github.com/riking/5759002
     * @param offsetX  the amount to be randomly offset by in the X axis
     * @param offsetY  the amount to be randomly offset by in the Y axis
     * @param offsetZ  the amount to be randomly offset by in the Z axis
     * @param speed    the speed of the particles
     * @param count    the number of particles
     * @param radius   the radius around the location
     */
    public abstract void playParticleEffect(Player player, Location location, String effect, float offsetX, float offsetY, float offsetZ, float speed, int count, int radius, Compat<Object> data);

    public void playParticleEffect(Player player, Location location, String effect, float offsetX, float offsetY, float offsetZ, float speed, int count, int radius) {
        playParticleEffect(player, location, effect, offsetX, offsetY, offsetZ, speed, count, radius, null);
    }

    public abstract boolean canSpawn(Location loc, MyPetMinecraftEntity entity);

    public abstract String getPlayerLanguage(Player player);

    public abstract TagCompound entityToTag(Entity entity);

    public abstract void applyTagToEntity(TagCompound tag, Entity entity);

    public String getCommandSenderLanguage(CommandSender sender) {
        String lang = "en";
        if (sender instanceof Player) {
            lang = getPlayerLanguage((Player) sender);
        }
        return lang;
    }

    public void sendMessage(Player player, String Message) {
        if (player != null && player.isOnline()) {
            player.sendMessage(Message);
        }
    }

    public boolean copyResource(Plugin plugin, String ressource, File destination) {
        try {
            InputStream template = plugin.getResource(ressource);
            OutputStream out = Files.newOutputStream(destination.toPath());

            byte[] buf = new byte[1024];
            int len;
            while ((len = template.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            template.close();
            out.close();

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public abstract TagCompound itemStackToCompund(ItemStack itemStack);

    public abstract ItemStack compundToItemStack(TagCompound compound);

    private static BukkitAudiences ADVENTURE_AUDIENCES;
    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private static BukkitAudiences audiences() {
        if (ADVENTURE_AUDIENCES == null) {
            ADVENTURE_AUDIENCES = BukkitAudiences.create(MyPetApi.getPlugin());
        }
        return ADVENTURE_AUDIENCES;
    }

    public void sendMessageActionBar(Player player, Component message) {
        if (player == null || !player.isOnline() || message == null) {
            return;
        }
        audiences().player(player).sendActionBar(message);
    }

    /**
     * Builds the action bar message for pet health updates so it can be reused by all NMS modules.
     * This method only constructs the message; caller is responsible for sending and for any gating (e.g., config flags).
     */
    public Component buildPetHealthActionBar(MyPet myPet, double health, double maxHealth) {
        if (myPet == null) {
            return null;
        }
        double deltaHealth = maxHealth - health;

        NamedTextColor healthColor = NamedTextColor.RED;
        if (health > maxHealth / 3 * 2) {
            healthColor = NamedTextColor.GREEN;
        } else if (health > maxHealth / 3) {
            healthColor = NamedTextColor.YELLOW;
        }
        Component parsed = MINI.deserialize(
                "<petname><reset>: ",
                Placeholder.unparsed("petname", myPet.getPetName()));
        if (health > 0) {
            parsed = parsed.append(MINI.deserialize(
                    "<healthcolor><health><white>/<maxhealth> ",
                    Placeholder.styling("healthcolor", healthColor),
                    Placeholder.unparsed("health", String.format("%1.2f", health)),
                    Placeholder.unparsed("maxhealth", String.format("%1.2f", maxHealth))));
            if (!myPet.getOwner().isHealthBarActive()) {
                parsed = parsed.append(MINI.deserialize(
                        "(<deltahealthcolor><deltahealth><reset>)",
                        Placeholder.parsed("deltahealthcolor", deltaHealth < 0 ? "<green>+" : "<red>-"),
                        Placeholder.unparsed("deltahealth", String.format("%1.2f", deltaHealth))));
            }
        } else {
            parsed = parsed.append(MINI.deserialize(
                    "<dead>",
                    Placeholder.unparsed("dead", Translation.getString("Name.Dead", myPet.getOwner()))));
        }
        return parsed;
    }

    public abstract void addZombieTargetGoal(Zombie zombie);

    public abstract boolean comparePlayerWithEntity(MyPetPlayer player, Object obj);

    public abstract boolean isEquipment(ItemStack itemStack);

    public abstract String getVanillaName(ItemStack itemStack);

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

    public Material getMaterial(MaterialHolder materialHolder) {
        return Material.matchMaterial(materialHolder.getId().toUpperCase());
    }

    public boolean compareBlockPositions(Location a, Location b) {
        return a.getBlockX() == b.getBlockX() &&
                a.getBlockY() == b.getBlockY() &&
                a.getBlockZ() == b.getBlockZ();
    }

    public boolean isSpigot() {
        try {
            Class.forName("org.spigotmc.SpigotConfig");
            return true;
        } catch (Throwable throwable) {
            return false;
        }
    }

    public Entity getEntityByUUID(UUID uuid) {
        return Bukkit.getEntity(uuid);
    }

    public abstract String getLastDamageSource(LivingEntity e);

    public abstract String itemstackToString(ItemStack itemStack);

    public abstract boolean gameruleDoDeathMessages(LivingEntity e);

    public boolean doStackWalking(Class leClass, int oldDepth) {
        return false;
    }
}
