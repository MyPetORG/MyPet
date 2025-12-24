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

package de.Keyle.MyPet.util.shop;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.MyPetBaby;
import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.entity.ShopInfo;
import de.Keyle.MyPet.api.util.ErrorUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Generates the pet-shops.yml configuration file dynamically based on
 * {@link ShopInfo} annotations on pet type interfaces.
 * <p>
 * This utility class scans all {@link MyPetType} values for the {@code @ShopInfo}
 * annotation and creates a default shop configuration with two shops:
 * <ul>
 *   <li><b>all</b> - Contains all annotated pet types</li>
 *   <li><b>babies</b> - Contains baby variants for pets implementing {@link MyPetBaby}</li>
 * </ul>
 * <p>
 * The generated file is only created if it doesn't already exist, allowing server
 * administrators to customize the configuration without it being overwritten.
 *
 * @see ShopInfo
 * @see ShopManager
 */
public class ShopConfigGenerator {

    /** YAML header comment explaining the configuration file purpose and linking to documentation. */
    private static final String YAML_HEADER = """
            #####################################################################
            #             This is the shop configuration of MyPet               #
            #               You can find more info on the wiki:                 #
            #  https://wiki.mypet-plugin.de/setup/configurations/pet-shops.yml  #
            #####################################################################
            """;

    /**
     * Generates the pet-shops.yml file if it doesn't exist.
     * Creates two shops: "all" (all pets with @ShopInfo) and "babies" (baby variants).
     *
     * @param shopConfigFile the target file to generate
     */
    public static void generateIfMissing(File shopConfigFile) {
        if (shopConfigFile.exists()) {
            return;
        }

        YamlConfiguration config = new YamlConfiguration();
        config.options().header(YAML_HEADER);

        // Create shop headers
        ConfigurationSection allShop = config.createSection("Shops.all");
        allShop.set("Name", "All Pets");
        allShop.set("Default", true);
        allShop.set("Balance.Type", "Private");
        allShop.set("Position", 0);
        allShop.set("Icon.Material", "chest");
        allShop.set("Icon.Glowing", false);

        ConfigurationSection babiesShop = config.createSection("Shops.babies");
        babiesShop.set("Name", "Baby Shop");
        babiesShop.set("Balance.Type", "Private");
        babiesShop.set("Position", 1);
        babiesShop.set("Icon.Material", "egg");
        babiesShop.set("Icon.Glowing", false);

        ConfigurationSection allPets = allShop.createSection("Pets");
        ConfigurationSection babyPets = babiesShop.createSection("Pets");

        for (MyPetType petType : MyPetType.values()) {
            if (!petType.checkMinecraftVersion()) {
                continue;
            }

            Class<?> petClass = petType.getMyPetClass();
            if (petClass == null) {
                continue;
            }

            ShopInfo shopInfo = petClass.getAnnotation(ShopInfo.class);
            if (shopInfo == null) {
                continue;
            }

            String key = petType.name().toLowerCase();
            String displayName = getDisplayName(shopInfo, petType);
            String description = getDescription(shopInfo, petType);
            List<String> options = new ArrayList<>(Arrays.asList(shopInfo.options()));

            // Add to "all" shop
            ConfigurationSection petSection = allPets.createSection(key);
            petSection.set("Name", displayName);
            petSection.set("Description", List.of(description));
            petSection.set("PetType", petType.name());
            petSection.set("EXP", shopInfo.exp());
            petSection.set("Price", shopInfo.price());
            if (!options.isEmpty()) {
                petSection.set("Options", options);
            }

            // Add baby variant if pet supports it
            if (MyPetBaby.class.isAssignableFrom(petClass)) {
                String babyKey = "baby_" + key;
                double babyPrice = shopInfo.babyPrice() < 0 ? shopInfo.price() * 2 : shopInfo.babyPrice();
                String babyDescription = getBabyDescription(shopInfo, petType);

                List<String> babyOptions = new ArrayList<>(options);
                if (!babyOptions.contains("baby")) {
                    babyOptions.add("baby");
                }

                ConfigurationSection babySection = babyPets.createSection(babyKey);
                babySection.set("Name", "Baby " + displayName);
                babySection.set("Description", List.of(babyDescription));
                babySection.set("PetType", petType.name());
                babySection.set("EXP", shopInfo.exp());
                babySection.set("Price", babyPrice);
                babySection.set("Options", babyOptions);
            }
        }

        try {
            config.save(shopConfigFile);
            MyPetApi.getMyPetLogger().info("Generated default pet-shops.yml configuration");
        } catch (IOException e) {
            ErrorUtil.reportError("Failed to generate pet-shops.yml", e);
        }
    }

    /**
     * Resolves the display name for a pet type.
     * <p>
     * If {@link ShopInfo#displayName()} is specified, that value is used.
     * Otherwise, the pet type's enum name is converted from CamelCase to
     * a human-readable format (e.g., "ZombieVillager" becomes "Zombie Villager").
     *
     * @param shopInfo the shop info annotation containing optional display name
     * @param petType  the pet type to get the display name for
     * @return the resolved display name
     */
    private static String getDisplayName(ShopInfo shopInfo, MyPetType petType) {
        if (!shopInfo.displayName().isEmpty()) {
            return shopInfo.displayName();
        }
        // Convert CamelCase to "Camel Case"
        return petType.name().replaceAll("([a-z])([A-Z])", "$1 $2");
    }

    /**
     * Generates the shop description for a pet type.
     * <p>
     * If {@link ShopInfo#description()} is specified, that value is used.
     * Otherwise, generates a default description like {@code "<green>It's a Zombie!"}.
     * Uses "an" instead of "a" when the name starts with a vowel.
     *
     * @param shopInfo the shop info annotation containing optional description
     * @param petType  the pet type to generate description for
     * @return the description string with MiniMessage color formatting
     */
    private static String getDescription(ShopInfo shopInfo, MyPetType petType) {
        if (!shopInfo.description().isEmpty()) {
            return shopInfo.description();
        }
        String name = getDisplayName(shopInfo, petType);
        String article = startsWithVowel(name) ? "an" : "a";
        return "<green>It's " + article + " " + name + "!";
    }

    /**
     * Generates the shop description for a baby variant of a pet type.
     * <p>
     * Always generates a description like {@code "<green>It's a baby Zombie!"}.
     * Uses "an" instead of "a" when the name starts with a vowel.
     *
     * @param shopInfo the shop info annotation (used to resolve display name)
     * @param petType  the pet type to generate baby description for
     * @return the baby description string with MiniMessage color formatting
     */
    private static String getBabyDescription(ShopInfo shopInfo, MyPetType petType) {
        String name = getDisplayName(shopInfo, petType);
        String article = startsWithVowel(name) ? "an" : "a";
        return "<green>It's " + article + " baby " + name + "!";
    }

    /**
     * Checks if a string starts with a vowel (a, e, i, o, u).
     * <p>
     * Used to determine whether to use "a" or "an" as the article
     * in generated descriptions.
     *
     * @param s the string to check
     * @return {@code true} if the string starts with a vowel, {@code false} otherwise
     */
    private static boolean startsWithVowel(String s) {
        if (s == null || s.isEmpty()) {
            return false;
        }
        char first = Character.toLowerCase(s.charAt(0));
        return first == 'a' || first == 'e' || first == 'i' || first == 'o' || first == 'u';
    }
}
