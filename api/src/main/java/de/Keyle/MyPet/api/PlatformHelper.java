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
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.util.ErrorUtil;
import de.Keyle.MyPet.api.util.locale.Translation;
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.UUID;

/**
 * Concrete platform helper using pure Bukkit/Paper API.
 */
public class PlatformHelper {

    public boolean canSpawn(Location loc, Class<? extends Mob> mobClass) {
        if (loc == null || loc.getWorld() == null) return false;
        Block at = loc.getBlock();
        if (!at.isPassable()) return false;
        Block below = at.getRelative(BlockFace.DOWN);
        return below.getType().isSolid();
    }

    public String getPlayerLanguage(Player player) {
        String locale = player.getLocale();
        if (locale.isEmpty()) {
            return "en_us";
        }
        return locale;
    }

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

    /**
     * Serializes a Bukkit ItemStack to an adventure-nbt CompoundBinaryTag via
     * Paper's {@code ItemStack#serializeAsBytes()} (vanilla codec format).
     * Returns an empty tag for null/air/empty stacks — Paper's underlying
     * serialization throws on empty items.
     */
    public CompoundBinaryTag itemStackToCompound(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().isAir() || itemStack.getAmount() <= 0) {
            return CompoundBinaryTag.empty();
        }
        try {
            byte[] bytes = itemStack.serializeAsBytes();
            return BinaryTagIO.reader().read(new ByteArrayInputStream(bytes), BinaryTagIO.Compression.GZIP);
        } catch (Throwable e) {
            ErrorUtil.report(e);
            return CompoundBinaryTag.empty();
        }
    }

    /**
     * Deserializes an adventure-nbt CompoundBinaryTag to a Bukkit ItemStack via
     * Paper's {@code ItemStack.deserializeBytes()} (vanilla codec format).
     */
    public ItemStack compoundToItemStack(CompoundBinaryTag compound) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            BinaryTagIO.writer().write(compound, out, BinaryTagIO.Compression.GZIP);
            return ItemStack.deserializeBytes(out.toByteArray());
        } catch (Throwable e) {
            ErrorUtil.report(e);
            return ItemStack.empty();
        }
    }

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
                .append(MyPetApi.getPlugin().getMiniMessage().deserialize("<reset>: "));
        if (health > 0) {
            parsed = parsed.append(MyPetApi.getPlugin().getMiniMessage().deserialize(
                    "<healthcolor><health><white>/<maxhealth> ",
                    Placeholder.styling("healthcolor", healthColor),
                    Placeholder.unparsed("health", String.format("%1.2f", health)),
                    Placeholder.unparsed("maxhealth", String.format("%1.2f", maxHealth))));
            if (!myPet.getOwner().isHealthBarActive()) {
                parsed = parsed.append(MyPetApi.getPlugin().getMiniMessage().deserialize(
                        "(<deltahealthcolor><deltahealth><reset>)",
                        Placeholder.parsed("deltahealthcolor", deltaHealth < 0 ? "<green>+" : "<red>-"),
                        Placeholder.unparsed("deltahealth", String.format("%1.2f", deltaHealth))));
            }
        } else {
            parsed = parsed.append(MyPetApi.getPlugin().getMiniMessage().deserialize(
                    "<dead>",
                    Placeholder.unparsed("dead", Translation.getString("Name.Dead", myPet.getOwner()))));
        }
        return parsed;
    }

    public boolean comparePlayerWithEntity(MyPetPlayer player, Object obj) {
        if (obj instanceof Player p) {
            return p.getUniqueId().equals(player.getUniqueId());
        }
        return false;
    }

    public void doPickupAnimation(Entity entity, Entity target) {
        // NMS sent a ClientboundTakeItemEntityPacket. No pure-Bukkit equivalent.
        // The visual pickup animation is cosmetic; skip for now.
    }

    public Entity getEntity(int id, World world) {
        // NMS entity lookup by network ID — no Bukkit equivalent.
        // Callers should use UUID-based lookup instead.
        return null;
    }

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

    public String getLastDamageSource(LivingEntity e) {
        EntityDamageEvent event = e.getLastDamageCause();
        return event != null ? event.getCause().name() : "GENERIC";
    }

    public String itemstackToString(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().isAir()) return "AIR";
        return itemStack.getType().name();
    }

    public boolean gameruleDoDeathMessages(LivingEntity e) {
        Boolean value = e.getWorld().getGameRuleValue(GameRule.SHOW_DEATH_MESSAGES);
        return value != null && value;
    }

    public boolean doStackWalking(Class<?> leClass, int oldDepth) {
        return false;
    }
}
