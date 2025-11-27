/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2025 Keyle
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

package de.Keyle.MyPet.util;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPet.PetState;
import de.Keyle.MyPet.api.player.DonateCheck;
import de.Keyle.MyPet.api.util.ComponentColorizer;
import de.Keyle.MyPet.api.util.ConfigItem;
import de.Keyle.MyPet.api.util.ErrorUtil;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.Keyle.MyPet.skill.skills.BehaviorImpl;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Utility class for building consistent pet information displays.
 * Provides reusable Component builders for common pet info patterns used
 * in CommandInfo, MyPetEntityListener, and other places.
 *
 * This eliminates code duplication and ensures consistent formatting across
 * all pet information displays.
 */
public class PetInfoBuilder {

    private static final String INDENT = "   ";

    /**
     * Builds pet name header component.
     * Format: "<PetName>:" in aqua color
     *
     * @param myPet The pet
     * @return Component with pet name header
     */
    public static Component petNameHeader(MyPet myPet) {
        return Component.text(myPet.getPetName() + ":")
                .color(NamedTextColor.AQUA);
    }

    /**
     * Builds owner line component.
     * Format: "   Owner: <owner name>"
     *
     * @param myPet The pet
     * @param sender CommandSender for translation
     * @return Component with owner information
     */
    public static Component ownerLine(MyPet myPet, CommandSender sender) {
        return Component.text()
                .append(Component.text(INDENT))
                .append(Translation.getComponent("Name.Owner", sender))
                .append(Component.text(": "))
                .append(Component.text(myPet.getOwner().getName()))
                .build();
    }

    /**
     * Builds HP line component with color-coded health display.
     * Green > 66%, Yellow 33-66%, Red < 33%
     * Format: "   HP: <current>/<max>"
     *
     * @param myPet The pet
     * @param sender CommandSender for translation
     * @return Component with HP information
     */
    public static Component hpLine(MyPet myPet, CommandSender sender) {
        Component hpLabel = Component.text()
                .append(Component.text(INDENT))
                .append(Translation.getComponent("Name.HP", sender))
                .append(Component.text(": "))
                .build();

        if (myPet.getStatus() == PetState.Dead) {
            return hpLabel.append(
                    Translation.getComponent("Name.Dead", sender)
                            .color(NamedTextColor.RED)
            );
        }

        double health = myPet.getHealth();
        double maxHealth = myPet.getMaxHealth();

        NamedTextColor healthColor;
        if (health > maxHealth / 3 * 2) {
            healthColor = NamedTextColor.GREEN;
        } else if (health > maxHealth / 3) {
            healthColor = NamedTextColor.YELLOW;
        } else {
            healthColor = NamedTextColor.RED;
        }

        return hpLabel.append(Component.text()
                .append(Component.text(String.format("%1.2f", health)).color(healthColor))
                .append(Component.text("/").color(NamedTextColor.WHITE))
                .append(Component.text(String.format("%1.2f", maxHealth)).color(healthColor))
                .build());
    }

    /**
     * Builds respawn time line component (shown when pet is dead).
     * Format: "   Respawn Time: <seconds>"
     *
     * @param myPet The pet
     * @param sender CommandSender for translation
     * @return Component with respawn time, or null if pet is not dead
     */
    public static Component respawnTimeLine(MyPet myPet, CommandSender sender) {
        if (myPet.getStatus() != PetState.Dead) {
            return null;
        }

        return Component.text()
                .append(Component.text(INDENT))
                .append(Translation.getComponent("Name.Respawntime", sender))
                .append(Component.text(": "))
                .append(Component.text(String.valueOf(myPet.getRespawnTime())))
                .build();
    }

    /**
     * Builds damage line component.
     * Format: "   Damage: <damage>"
     *
     * @param myPet The pet
     * @param sender CommandSender for translation
     * @return Component with damage info, or null if damage is 0
     */
    public static Component damageLine(MyPet myPet, CommandSender sender) {
        if (myPet.getDamage() <= 0) {
            return null;
        }

        return Component.text()
                .append(Component.text(INDENT))
                .append(Translation.getComponent("Name.Damage", sender))
                .append(Component.text(": "))
                .append(Component.text(String.format("%1.2f", myPet.getDamage())))
                .build();
    }

    /**
     * Builds ranged damage line component.
     * Format: "   Ranged Damage: <damage>"
     *
     * @param myPet The pet
     * @param sender CommandSender for translation
     * @return Component with ranged damage info, or null if ranged damage is 0
     */
    public static Component rangedDamageLine(MyPet myPet, CommandSender sender) {
        if (myPet.getRangedDamage() <= 0) {
            return null;
        }

        return Component.text()
                .append(Component.text(INDENT))
                .append(Translation.getComponent("Name.RangedDamage", sender))
                .append(Component.text(": "))
                .append(Component.text(String.format("%1.2f", myPet.getRangedDamage())))
                .build();
    }

    /**
     * Builds hunger line component.
     * Format: "   Hunger: <value>"
     *
     * @param myPet The pet
     * @param sender CommandSender for translation
     * @return Component with hunger info, or null if hunger system disabled
     */
    public static Component hungerLine(MyPet myPet, CommandSender sender) {
        if (!Configuration.HungerSystem.USE_HUNGER_SYSTEM) {
            return null;
        }

        return Component.text()
                .append(Component.text(INDENT))
                .append(Translation.getComponent("Name.Hunger", sender))
                .append(Component.text(": "))
                .append(Component.text(String.valueOf(Math.round(myPet.getSaturation()))))
                .build();
    }

    /**
     * Builds food list component with hover events showing pet info.
     * Format: "   Food: <item1>, <item2>, ..." (with hover tooltips)
     *
     * @param myPet The pet
     * @param sender CommandSender for translation
     * @return Component with food list, or null if hunger system disabled or sender is not a player
     */
    public static Component foodLine(MyPet myPet, CommandSender sender) {
        if (!Configuration.HungerSystem.USE_HUNGER_SYSTEM) {
            return null;
        }

        if (sender instanceof Player player) {
            TextComponent.Builder messageBuilder = Component.text()
                    .append(Component.text(INDENT))
                    .append(Translation.getComponent("Name.Food", player))
                    .append(Component.text(": "));

            boolean comma = false;
            for (ConfigItem material : MyPetApi.getMyPetInfo().getFood(myPet.getPetType())) {
                ItemStack is = material.getItem();
                if (is == null || is.getType() == Material.AIR) {
                    continue;
                }
                if (comma) {
                    messageBuilder.append(Component.text(", "));
                }

                ItemMeta meta = is.getItemMeta();

                Component itemComponent;
                if (meta != null && meta.hasDisplayName()) {
                    itemComponent = Component.text(meta.getDisplayName())
                            .hoverEvent(Util.myPetToItemHover(myPet, player.getName()));
                } else {
                    try {
                        itemComponent = Component.translatable(MyPetApi.getPlatformHelper().getVanillaName(is))
                                .color(NamedTextColor.GOLD)
                                .hoverEvent(Util.myPetToItemHover(myPet, player.getName()));
                    } catch (Exception e) {
                        MyPetApi.getLogger().warning("A food item caused an error. If you think this is a bug please report it to the MyPet developer.");
                        MyPetApi.getLogger().warning("" + is);
                        ErrorUtil.reportError("PetInfoBuilder operation failed", e);
                        continue;
                    }
                }
                messageBuilder.append(itemComponent);
                comma = true;
            }
            return messageBuilder.build();
        } else {
            // For console, just show material names without hover
            String foodList = MyPetApi.getMyPetInfo().getFood(myPet.getPetType())
                    .stream()
                    .filter(configItem -> configItem.getItem() != null && configItem.getItem().getType() != Material.AIR)
                    .map(configItem -> configItem.getItem().getType().name())
                    .collect(java.util.stream.Collectors.joining(", "));

            return Component.text()
                    .append(Component.text(INDENT))
                    .append(Translation.getComponent("Name.Food", sender))
                    .append(Component.text(": "))
                    .append(Component.text(foodList))
                    .build();
        }
    }

    /**
     * Builds behavior line component.
     * Format: "   Behavior: <behavior name>"
     *
     * @param myPet The pet
     * @param sender CommandSender for translation
     * @return Component with behavior info, or null if pet doesn't have behavior skill
     */
    public static Component behaviorLine(MyPet myPet, CommandSender sender) {
        if (!myPet.getSkills().has(BehaviorImpl.class)) {
            return null;
        }

        BehaviorImpl behavior = myPet.getSkills().get(BehaviorImpl.class);
        return Component.text()
                .append(Component.text(INDENT))
                .append(Translation.getComponent("Name.Skill.Behavior", sender))
                .append(Component.text(": "))
                .append(Translation.getComponent("Name." + behavior.getBehavior().name(), sender))
                .build();
    }

    /**
     * Builds skilltree line component.
     * Format: "   Skilltree: <skilltree name>"
     *
     * @param myPet The pet
     * @param sender CommandSender for translation
     * @return Component with skilltree info, or null if pet has no skilltree
     */
    public static Component skilltreeLine(MyPet myPet, CommandSender sender) {
        if (myPet.getSkilltree() == null) {
            return null;
        }

        return Component.text()
                .append(Component.text(INDENT))
                .append(Translation.getComponent("Name.Skilltree", sender))
                .append(Component.text(": "))
                .append(ComponentColorizer.parseToComponent(myPet.getSkilltree().getDisplayName()))
                .build();
    }

    /**
     * Builds level line component.
     * Format: "   Level: <level>"
     *
     * @param myPet The pet
     * @param sender CommandSender for translation
     * @return Component with level info
     */
    public static Component levelLine(MyPet myPet, CommandSender sender) {
        int level = myPet.getExperience().getLevel();
        return Component.text()
                .append(Component.text(INDENT))
                .append(Translation.getComponent("Name.Level", sender))
                .append(Component.text(": "))
                .append(Component.text(String.valueOf(level)))
                .build();
    }

    /**
     * Builds experience line component.
     * Format: "   Exp: <current>/<required>"
     *
     * @param myPet The pet
     * @param sender CommandSender for translation
     * @return Component with exp info, or null if pet is at max level
     */
    public static Component expLine(MyPet myPet, CommandSender sender) {
        int maxLevel = myPet.getSkilltree() != null
                ? myPet.getSkilltree().getMaxLevel()
                : Configuration.LevelSystem.Experience.LEVEL_CAP;

        if (myPet.getExperience().getLevel() >= maxLevel) {
            return null;
        }

        double exp = myPet.getExperience().getCurrentExp();
        double reqExp = myPet.getExperience().getRequiredExp();

        return Component.text()
                .append(Component.text(INDENT))
                .append(Translation.getComponent("Name.Exp", sender))
                .append(Component.text(": "))
                .append(Component.text(String.format("%1.2f", exp) + "/" + String.format("%1.2f", reqExp)))
                .build();
    }

    /**
     * Builds donation rank line component.
     * Format: "   <icon> <Title> <icon>" in gold
     *
     * @param myPet The pet
     * @param sender CommandSender for translation
     * @return Component with donation rank info, or null if no donation rank
     */
    public static Component donationRankLine(MyPet myPet, CommandSender sender) {
        if (myPet.getOwner().getDonationRank() == DonateCheck.DonationRank.None) {
            return null;
        }

        DonateCheck.DonationRank rank = myPet.getOwner().getDonationRank();
        String icon = rank.getDefaultIcon();

        return Component.text()
                .append(Component.text(INDENT))
                .append(Component.text(icon + " ").color(NamedTextColor.GOLD))
                .append(Translation.getComponent("Name.Title." + rank.name(), sender).color(NamedTextColor.GOLD))
                .append(Component.text(" " + icon).color(NamedTextColor.GOLD))
                .build();
    }

    /**
     * Builds complete pet info display with all available information.
     * This is the full info display used by /petinfo command.
     *
     * @param myPet The pet
     * @param sender CommandSender for translation and display
     * @return Component with complete pet info
     */
    public static Component fullPetInfo(MyPet myPet, CommandSender sender) {
        TextComponent.Builder builder = Component.text();
        boolean hasContent = false;

        // Pet name header
        Component nameHeader = petNameHeader(myPet);
        if (nameHeader != null) {
            builder.append(nameHeader).append(Component.newline());
            hasContent = true;
        }

        // Owner (if viewing someone else's pet)
        if (sender != myPet.getOwner().getPlayer()) {
            Component owner = ownerLine(myPet, sender);
            if (owner != null) {
                builder.append(owner).append(Component.newline());
                hasContent = true;
            }
        }

        // HP
        Component hp = hpLine(myPet, sender);
        if (hp != null) {
            builder.append(hp).append(Component.newline());
            hasContent = true;
        }

        // Respawn time (if dead)
        Component respawn = respawnTimeLine(myPet, sender);
        if (respawn != null) {
            builder.append(respawn).append(Component.newline());
            hasContent = true;
        }

        // Damage
        Component damage = damageLine(myPet, sender);
        if (damage != null) {
            builder.append(damage).append(Component.newline());
            hasContent = true;
        }

        // Ranged damage
        Component rangedDamage = rangedDamageLine(myPet, sender);
        if (rangedDamage != null) {
            builder.append(rangedDamage).append(Component.newline());
            hasContent = true;
        }

        // Hunger
        Component hunger = hungerLine(myPet, sender);
        if (hunger != null) {
            builder.append(hunger).append(Component.newline());
            hasContent = true;
        }

        // Food
        Component food = foodLine(myPet, sender);
        if (food != null) {
            builder.append(food).append(Component.newline());
            hasContent = true;
        }

        // Behavior
        Component behavior = behaviorLine(myPet, sender);
        if (behavior != null) {
            builder.append(behavior).append(Component.newline());
            hasContent = true;
        }

        // Skilltree
        Component skilltree = skilltreeLine(myPet, sender);
        if (skilltree != null) {
            builder.append(skilltree).append(Component.newline());
            hasContent = true;
        }

        // Level
        Component level = levelLine(myPet, sender);
        if (level != null) {
            builder.append(level).append(Component.newline());
            hasContent = true;
        }

        // Experience
        Component exp = expLine(myPet, sender);
        if (exp != null) {
            builder.append(exp).append(Component.newline());
            hasContent = true;
        }

        // Donation rank
        Component donation = donationRankLine(myPet, sender);
        if (donation != null) {
            builder.append(donation);
            hasContent = true;
        }

        return hasContent ? builder.build() : Component.empty();
    }
}
