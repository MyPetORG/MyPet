/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2026 Keyle
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
import de.Keyle.MyPet.api.MyPetGlobal;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.entity.Pet.PetState;
import de.Keyle.MyPet.api.entity.StoredPet;
import de.Keyle.MyPet.api.util.ConfigItem;
import de.Keyle.MyPet.api.util.ErrorUtil;
import de.Keyle.MyPet.api.util.locale.Locale;
import de.Keyle.MyPet.skill.skills.BehaviorImpl;
import de.Keyle.MyPet.util.player.ContributorCheck;
import de.Keyle.MyPet.util.player.MyPetPlayerImpl;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Utility class for building consistent pet information displays.
 * Provides reusable Component builders for common pet info patterns used
 * in CommandInfo, PetInfoOnLeashListener, and other places.
 * <p>
 * This eliminates code duplication and ensures consistent formatting across
 * all pet information displays.
 */
public class PetInfoBuilder {

    private static final String INDENT = "   ";

    /**
     * Builds pet name header component.
     * Format: "<PetName>:" in aqua color
     *
     * @param pet The pet
     * @return Component with pet name header
     */
    public static Component petNameHeader(Pet pet) {
        return pet.getDisplayName().append(Component.text(":"));
    }

    /**
     * Builds owner line component.
     * Format: "   Owner: <owner name>"
     *
     * @param pet  The pet
     * @param sender CommandSender for translation
     * @return Component with owner information
     */
    public static Component ownerLine(Pet pet, CommandSender sender) {
        return Component.text()
                .append(Component.text(INDENT))
                .append(Locale.getComponent("Name.Owner", sender))
                .append(Component.text(": "))
                .append(Component.text(pet.getOwner().getName()))
                .build();
    }

    /**
     * Builds HP line component with color-coded health display.
     * Green > 66%, Yellow 33-66%, Red < 33%
     * Format: "   HP: <current>/<max>"
     *
     * @param pet  The pet
     * @param sender CommandSender for translation
     * @return Component with HP information
     */
    public static Component hpLine(Pet pet, CommandSender sender) {
        Component hpLabel = Component.text()
                .append(Component.text(INDENT))
                .append(Locale.getComponent("Name.HP", sender))
                .append(Component.text(": "))
                .build();

        if (pet.getStatus() == PetState.Dead) {
            return hpLabel.append(
                    Locale.getComponent("Name.Dead", sender)
                            .color(NamedTextColor.RED)
            );
        }

        double health = pet.getHealth();
        double maxHealth = pet.getMaxHealth();

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
     * @param pet  The pet
     * @param sender CommandSender for translation
     * @return Component with respawn time, or null if pet is not dead
     */
    public static Component respawnTimeLine(Pet pet, CommandSender sender) {
        if (pet.getStatus() != PetState.Dead) {
            return null;
        }

        return Component.text()
                .append(Component.text(INDENT))
                .append(Locale.getComponent("Name.Respawntime", sender))
                .append(Component.text(": "))
                .append(Component.text(String.valueOf(pet.getRespawnTime())))
                .build();
    }

    /**
     * Builds damage line component.
     * Format: "   Damage: <damage>"
     *
     * @param pet  The pet
     * @param sender CommandSender for translation
     * @return Component with damage info, or null if damage is 0
     */
    public static Component damageLine(Pet pet, CommandSender sender) {
        if (pet.getDamage() <= 0) {
            return null;
        }

        return Component.text()
                .append(Component.text(INDENT))
                .append(Locale.getComponent("Name.Damage", sender))
                .append(Component.text(": "))
                .append(Component.text(String.format("%1.2f", pet.getDamage())))
                .build();
    }

    /**
     * Builds ranged damage line component.
     * Format: "   Ranged Damage: <damage>"
     *
     * @param pet  The pet
     * @param sender CommandSender for translation
     * @return Component with ranged damage info, or null if ranged damage is 0
     */
    public static Component rangedDamageLine(Pet pet, CommandSender sender) {
        if (pet.getRangedDamage() <= 0) {
            return null;
        }

        return Component.text()
                .append(Component.text(INDENT))
                .append(Locale.getComponent("Name.RangedDamage", sender))
                .append(Component.text(": "))
                .append(Component.text(String.format("%1.2f", pet.getRangedDamage())))
                .build();
    }

    /**
     * Builds hunger line component.
     * Format: "   Hunger: <value>"
     *
     * @param pet  The pet
     * @param sender CommandSender for translation
     * @return Component with hunger info, or null if hunger system disabled
     */
    public static Component hungerLine(Pet pet, CommandSender sender) {
        if (!MyPetGlobal.HungerSystem.USE_HUNGER_SYSTEM.get()) {
            return null;
        }

        return Component.text()
                .append(Component.text(INDENT))
                .append(Locale.getComponent("Name.Hunger", sender))
                .append(Component.text(": "))
                .append(Component.text(String.valueOf(Math.round(pet.getSaturation()))))
                .build();
    }

    /**
     * Builds food list component with hover events showing pet info.
     * Format: "   Food: <item1>, <item2>, ..." (with hover tooltips)
     *
     * @param pet  The pet
     * @param sender CommandSender for translation
     * @return Component with food list, or null if hunger system disabled or sender is not a player
     */
    public static Component foodLine(Pet pet, CommandSender sender) {
        if (!MyPetGlobal.HungerSystem.USE_HUNGER_SYSTEM.get()) {
            return null;
        }

        if (sender instanceof Player player) {
            TextComponent.Builder messageBuilder = Component.text()
                    .append(Component.text(INDENT))
                    .append(Locale.getComponent("Name.Food", player))
                    .append(Component.text(": "));

            boolean comma = false;
            for (ConfigItem material : MyPetApi.getPetInfo().getFood(pet.getPetType())) {
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
                            .hoverEvent(petToItemHover(pet, player.getName()));
                } else {
                    try {
                        itemComponent = Component.translatable(is.translationKey())
                                .color(NamedTextColor.GOLD)
                                .hoverEvent(petToItemHover(pet, player.getName()));
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
            String foodList = MyPetApi.getPetInfo().getFood(pet.getPetType())
                    .stream()
                    .filter(configItem -> configItem.getItem() != null && configItem.getItem().getType() != Material.AIR)
                    .map(configItem -> configItem.getItem().getType().name())
                    .collect(java.util.stream.Collectors.joining(", "));

            return Component.text()
                    .append(Component.text(INDENT))
                    .append(Locale.getComponent("Name.Food", sender))
                    .append(Component.text(": "))
                    .append(Component.text(foodList))
                    .build();
        }
    }

    /**
     * Builds behavior line component.
     * Format: "   Behavior: <behavior name>"
     *
     * @param pet  The pet
     * @param sender CommandSender for translation
     * @return Component with behavior info, or null if pet doesn't have behavior skill
     */
    public static Component behaviorLine(Pet pet, CommandSender sender) {
        if (!pet.getSkills().has(BehaviorImpl.class)) {
            return null;
        }

        BehaviorImpl behavior = pet.getSkills().get(BehaviorImpl.class);
        return Component.text()
                .append(Component.text(INDENT))
                .append(Locale.getComponent("Name.Skill.Behavior", sender))
                .append(Component.text(": "))
                .append(Locale.getComponent("Name." + behavior.getBehavior().name(), sender))
                .build();
    }

    /**
     * Builds skilltree line component.
     * Format: "   Skilltree: <skilltree name>"
     *
     * @param pet  The pet
     * @param sender CommandSender for translation
     * @return Component with skilltree info, or null if pet has no skilltree
     */
    public static Component skilltreeLine(Pet pet, CommandSender sender) {
        if (pet.getSkilltree() == null) {
            return null;
        }

        return Component.text()
                .append(Component.text(INDENT))
                .append(Locale.getComponent("Name.Skilltree", sender))
                .append(Component.text(": "))
                .append(Util.SANITIZED_MINIMESSAGE.deserialize(pet.getSkilltree().getDisplayName()))
                .build();
    }

    /**
     * Builds level line component.
     * Format: "   Level: <level>"
     *
     * @param pet  The pet
     * @param sender CommandSender for translation
     * @return Component with level info
     */
    public static Component levelLine(Pet pet, CommandSender sender) {
        int level = pet.getExperience().getLevel();
        return Component.text()
                .append(Component.text(INDENT))
                .append(Locale.getComponent("Name.Level", sender))
                .append(Component.text(": "))
                .append(Component.text(String.valueOf(level)))
                .build();
    }

    /**
     * Builds experience line component.
     * Format: "   Exp: <current>/<required>"
     *
     * @param pet  The pet
     * @param sender CommandSender for translation
     * @return Component with exp info, or null if pet is at max level
     */
    public static Component expLine(Pet pet, CommandSender sender) {
        int maxLevel = pet.getSkilltree() != null
                ? pet.getSkilltree().getMaxLevel()
                : MyPetGlobal.LevelSystem.Experience.LEVEL_CAP.get();

        if (pet.getExperience().getLevel() >= maxLevel) {
            return null;
        }

        double exp = pet.getExperience().getCurrentExp();
        double reqExp = pet.getExperience().getRequiredExp();

        return Component.text()
                .append(Component.text(INDENT))
                .append(Locale.getComponent("Name.Exp", sender))
                .append(Component.text(": "))
                .append(Component.text(String.format("%1.2f", exp) + "/" + String.format("%1.2f", reqExp)))
                .build();
    }

    /**
     * Builds donation rank line component.
     * Format: "   <icon> <Title> <icon>" in gold
     *
     * @param pet  The pet
     * @param sender CommandSender for translation
     * @return Component with donation rank info, or null if no donation rank
     */
    public static Component donationRankLine(Pet pet, CommandSender sender) {
        ContributorCheck.ContributorRank rank = ((MyPetPlayerImpl) pet.getOwner()).getContributorRank();
        if (rank == ContributorCheck.ContributorRank.None) {
            return null;
        }

        String icon = rank.getDefaultIcon();

        return Component.text()
                .append(Component.text(INDENT))
                .append(Component.text(icon + " ").color(NamedTextColor.GOLD))
                .append(Locale.getComponent("Name.Title." + rank.name(), sender).color(NamedTextColor.GOLD))
                .append(Component.text(" " + icon).color(NamedTextColor.GOLD))
                .build();
    }

    /**
     * Builds complete pet info display with all available information.
     * This is the full info display used by /petinfo command.
     *
     * @param pet  The pet
     * @param sender CommandSender for translation and display
     * @return Component with complete pet info
     */
    public static Component fullPetInfo(Pet pet, CommandSender sender) {
        TextComponent.Builder builder = Component.text();
        boolean hasContent = false;

        // Pet name header
        Component nameHeader = petNameHeader(pet);
        if (nameHeader != null) {
            builder.append(nameHeader).append(Component.newline());
            hasContent = true;
        }

        // Owner (if viewing someone else's pet)
        if (sender != pet.getOwner().getPlayer()) {
            Component owner = ownerLine(pet, sender);
            if (owner != null) {
                builder.append(owner).append(Component.newline());
                hasContent = true;
            }
        }

        // HP
        Component hp = hpLine(pet, sender);
        if (hp != null) {
            builder.append(hp).append(Component.newline());
            hasContent = true;
        }

        // Respawn time (if dead)
        Component respawn = respawnTimeLine(pet, sender);
        if (respawn != null) {
            builder.append(respawn).append(Component.newline());
            hasContent = true;
        }

        // Damage
        Component damage = damageLine(pet, sender);
        if (damage != null) {
            builder.append(damage).append(Component.newline());
            hasContent = true;
        }

        // Ranged damage
        Component rangedDamage = rangedDamageLine(pet, sender);
        if (rangedDamage != null) {
            builder.append(rangedDamage).append(Component.newline());
            hasContent = true;
        }

        // Hunger
        Component hunger = hungerLine(pet, sender);
        if (hunger != null) {
            builder.append(hunger).append(Component.newline());
            hasContent = true;
        }

        // Food
        Component food = foodLine(pet, sender);
        if (food != null) {
            builder.append(food).append(Component.newline());
            hasContent = true;
        }

        // Behavior
        Component behavior = behaviorLine(pet, sender);
        if (behavior != null) {
            builder.append(behavior).append(Component.newline());
            hasContent = true;
        }

        // Skilltree
        Component skilltree = skilltreeLine(pet, sender);
        if (skilltree != null) {
            builder.append(skilltree).append(Component.newline());
            hasContent = true;
        }

        // Level
        Component level = levelLine(pet, sender);
        if (level != null) {
            builder.append(level).append(Component.newline());
            hasContent = true;
        }

        // Experience
        Component exp = expLine(pet, sender);
        if (exp != null) {
            builder.append(exp).append(Component.newline());
            hasContent = true;
        }

        // Donation rank
        Component donation = donationRankLine(pet, sender);
        if (donation != null) {
            builder.append(donation);
            hasContent = true;
        }

        return hasContent ? builder.build() : Component.empty();
    }

    /**
     * Builds a hover event displaying a stored pet's stats: hunger, HP/respawn time,
     * experience, level, pet type, skill tree, and dead status (if applicable).
     */
    public static HoverEvent<Component> petToItemHover(StoredPet pet, String lang) {
        TextComponent.Builder builder = Component.text();

        builder.append(Locale.getComponent("Name.Hunger", lang))
                .append(Component.text(": "))
                .append(Component.text(Math.round(pet.getSaturation())).color(NamedTextColor.GOLD))
                .append(Component.newline());

        if (!MyPetGlobal.Respawn.DISABLE_AUTO_RESPAWN.get()) {
            if (pet.getRespawnTime() > 0) {
                builder.append(Locale.getComponent("Name.Respawntime", lang))
                        .append(Component.text(": "))
                        .append(Component.text(pet.getRespawnTime() + "sec").color(NamedTextColor.GOLD))
                        .append(Component.newline());
            } else {
                builder.append(Locale.getComponent("Name.HP", lang))
                        .append(Component.text(": "))
                        .append(Component.text(String.format("%1.2f", pet.getHealth())).color(NamedTextColor.GOLD))
                        .append(Component.newline());
            }
        } else if (pet.getRespawnTime() <= 0) {
            builder.append(Locale.getComponent("Name.HP", lang))
                    .append(Component.text(": "))
                    .append(Component.text(String.format("%1.2f", pet.getHealth())).color(NamedTextColor.GOLD))
                    .append(Component.newline());
        }

        builder.append(Locale.getComponent("Name.Exp", lang))
                .append(Component.text(": "))
                .append(Component.text(String.format("%1.2f", pet.getExp())).color(NamedTextColor.GOLD))
                .append(Component.newline());

        int level = pet.getLevel();
        if (level > 0) {
            builder.append(Locale.getComponent("Name.Level", lang))
                    .append(Component.text(": "))
                    .append(Component.text(level).color(NamedTextColor.GOLD))
                    .append(Component.newline());
        }

        String entityKey = "entity.minecraft." + pet.getPetType().getBukkitName().toLowerCase();
        builder.append(Locale.getComponent("Name.Type", lang))
                .append(Component.text(": "))
                .append(Component.translatable(entityKey).color(NamedTextColor.GOLD))
                .append(Component.newline());

        builder.append(Locale.getComponent("Name.Skilltree", lang))
                .append(Component.text(": "))
                .append(Util.SANITIZED_MINIMESSAGE.deserialize(pet.getSkilltree() != null ? pet.getSkilltree().getDisplayName() : "-")
                        .color(NamedTextColor.GOLD));

        if (MyPetGlobal.Respawn.DISABLE_AUTO_RESPAWN.get() && pet.getRespawnTime() > 0) {
            builder.append(Component.newline())
                    .append(Locale.getComponent("Name.Dead", lang).color(NamedTextColor.RED));
        }

        return HoverEvent.showText(builder.build());
    }
}
