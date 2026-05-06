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

package de.Keyle.MyPet.util.hooks;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.util.hooks.PluginHook;
import de.Keyle.MyPet.api.util.hooks.PluginHookName;
import de.Keyle.MyPet.skill.skills.BehaviorImpl;
import de.Keyle.MyPet.util.player.ContributorCheck.ContributorRank;
import de.Keyle.MyPet.util.player.MyPetPlayerImpl;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@PluginHookName("PlaceholderAPI")
public class PlaceholderApiHook implements PluginHook {

    Map<String, PlaceHolder<?>> placeHolders = new HashMap<>();
    PlaceholderExpansion myPetExpansion;

    private static ContributorRank rankOf(MyPetPlayer player) {
        return ((MyPetPlayerImpl) player).getContributorRank();
    }

    @Override
    public boolean onEnable() {
        boolean loaded = registerParentPlaceHolder();
        if (loaded) {
            registerPlaceholder();
        }
        return loaded;
    }

    @Override
    public void onDisable() {
        myPetExpansion = null;
    }

    public void registerPlaceholder() {
        placeHolders.put("name", new PlaceHolder<>(Pet.class) {
            @Override
            public String getValue(Pet pet) {
                return Util.SANITIZED_MINIMESSAGE.stripTags(pet.getPetName());
            }
        });

        placeHolders.put("level", new PlaceHolder<>(Pet.class) {
            @Override
            public String getValue(Pet pet) {
                return "" + pet.getExperience().getLevel();
            }
        });

        placeHolders.put("exp", new PlaceHolder<>(Pet.class) {
            @Override
            public String getValue(Pet pet) {
                return String.format("%.2f", pet.getExp());
            }
        });

        placeHolders.put("exp_long", new PlaceHolder<>(Pet.class) {
            @Override
            public String getValue(Pet pet) {
                return "" + pet.getExp();
            }
        });

        placeHolders.put("type", new PlaceHolder<>(Pet.class) {
            @Override
            public String getValue(Pet pet) {
                return pet.getPetType().name();
            }
        });

        placeHolders.put("status", new PlaceHolder<>(Pet.class) {
            @Override
            public String getValue(Pet pet) {
                return pet.getStatus().name();
            }
        });

        placeHolders.put("health", new PlaceHolder<>(Pet.class) {
            @Override
            public String getValue(Pet pet) {
                return String.format("%.2f", pet.getHealth());
            }
        });

        placeHolders.put("health_long", new PlaceHolder<>(Pet.class) {
            @Override
            public String getValue(Pet pet) {
                return "" + pet.getHealth();
            }
        });

        placeHolders.put("health_max", new PlaceHolder<>(Pet.class) {
            @Override
            public String getValue(Pet pet) {
                return String.format("%.2f", pet.getMaxHealth());
            }
        });

        placeHolders.put("health_max_long", new PlaceHolder<>(Pet.class) {
            @Override
            public String getValue(Pet pet) {
                return "" + pet.getMaxHealth();
            }
        });

        placeHolders.put("respawn_time", new PlaceHolder<>(Pet.class) {
            @Override
            public String getValue(Pet pet) {
                return "" + pet.getRespawnTime();
            }
        });

        placeHolders.put("saturation", new PlaceHolder<>(Pet.class) {
            @Override
            public String getValue(Pet pet) {
                return String.format("%.2f", pet.getSaturation());
            }
        });

        placeHolders.put("saturation_long", new PlaceHolder<>(Pet.class) {
            @Override
            public String getValue(Pet pet) {
                return "" + pet.getSaturation();
            }
        });

        placeHolders.put("petfood", new PlaceHolder<>(Pet.class) {
            @Override
            public String getValue(Pet pet) {
                String foodString;
                foodString = MyPetApi.getPetInfo().getFood(pet.getPetType())
                        .stream()
                        .filter(configItem -> configItem.getItem() != null && configItem.getItem().getType() != Material.AIR)
                        .map(configItem -> configItem.getItem().getType().name())
                        .collect(Collectors.joining(", "));
                return foodString;
            }
        });

        placeHolders.put("uuid", new PlaceHolder<>(Pet.class) {
            @Override
            public String getValue(Pet pet) {
                return pet.getUUID().toString();
            }
        });

        placeHolders.put("behavior", new PlaceHolder<>(Pet.class) {
            @Override
            public String getValue(Pet pet) {
                return pet.getSkills().has(BehaviorImpl.class) ? pet.getSkills().get(BehaviorImpl.class).getBehavior().name() : "Normal";
            }
        });

        placeHolders.put("skilltree_display", new PlaceHolder<>(Pet.class) {
            @Override
            public String getValue(Pet pet) {
                return pet.getSkilltree() != null ? Util.SANITIZED_MINIMESSAGE.stripTags(pet.getSkilltree().getDisplayName()) : "";
            }
        });

        placeHolders.put("skilltree_name", new PlaceHolder<>(Pet.class) {
            @Override
            public String getValue(Pet pet) {
                return pet.getSkilltree() != null ? pet.getSkilltree().getName() : "";
            }
        });

        placeHolders.put("world_group", new PlaceHolder<>(Pet.class) {
            @Override
            public String getValue(Pet pet) {
                return pet.getWorldGroup();
            }
        });

        placeHolders.put("player_is_premium", new PlaceHolder<>(MyPetPlayer.class) {
            @Override
            public String getValue(MyPetPlayer player) {
                return rankOf(player) == ContributorRank.Premium ? "yes" : "no";
            }
        });

        placeHolders.put("player_is_donator", new PlaceHolder<>(MyPetPlayer.class) {
            @Override
            public String getValue(MyPetPlayer player) {
                return rankOf(player) == ContributorRank.Donator ? "yes" : "no";
            }
        });

        placeHolders.put("player_is_creator", new PlaceHolder<>(MyPetPlayer.class) {
            @Override
            public String getValue(MyPetPlayer player) {
                return rankOf(player) == ContributorRank.Creator ? "yes" : "no";
            }
        });

        placeHolders.put("player_is_developer", new PlaceHolder<>(MyPetPlayer.class) {
            @Override
            public String getValue(MyPetPlayer player) {
                return rankOf(player) == ContributorRank.Developer ? "yes" : "no";
            }
        });

        placeHolders.put("player_is_translator", new PlaceHolder<>(MyPetPlayer.class) {
            @Override
            public String getValue(MyPetPlayer player) {
                return rankOf(player) == ContributorRank.Translator ? "yes" : "no";
            }
        });

        placeHolders.put("player_is_none", new PlaceHolder<>(MyPetPlayer.class) {
            @Override
            public String getValue(MyPetPlayer player) {
                return rankOf(player) == ContributorRank.None ? "yes" : "no";
            }
        });

        placeHolders.put("player_particle_rank", new PlaceHolder<>(MyPetPlayer.class) {
            @Override
            public String getValue(MyPetPlayer player) {
                return rankOf(player).name();
            }
        });

        placeHolders.put("player_language", new PlaceHolder<>(MyPetPlayer.class) {
            @Override
            public String getValue(MyPetPlayer player) {
                return player.getLanguage();
            }
        });

        placeHolders.put("has_pet", new PlaceHolder<>(Player.class) {
            @Override
            public String getValue(Player player) {
                return MyPetApi.getPlayerManager().isMyPetPlayer(player) && MyPetApi.getPetManager().hasActivePet(player) ? "yes" : "no";
            }
        });

        placeHolders.put("idle_volume", new PlaceHolder<>(MyPetPlayer.class) {
            @Override
            public String getValue(MyPetPlayer player) {
                return Math.round(player.getPetLivingSoundVolume() * 100f) + "%";
            }
        });
    }

    public boolean registerParentPlaceHolder() {
        myPetExpansion = new PlaceholderExpansion() {
            @Override
            public boolean persist() {
                return true;
            }

            @SuppressWarnings("deprecation")
            @Override
            public @NotNull String getAuthor() {
                return MyPetApi.getPlugin().getDescription().getAuthors().toString();
            }

            @Override
            public @NotNull String getIdentifier() {
                return "mypet";
            }

            @Override
            public String getRequiredPlugin() {
                return "MyPet";
            }

            @Override
            public @NotNull String getVersion() {
                return "1.0.4";
            }

            @Override
            @SuppressWarnings("unchecked")
            public String onPlaceholderRequest(Player p, @NotNull String identifier) {
                if (p == null) {
                    return null;
                }
                PlaceHolder<?> placeHolder = placeHolders.get(identifier);
                if (placeHolder == null) {
                    return null;
                }

                if (placeHolder.getHolderClass() == Player.class) {
                    return ((PlaceHolder<Player>) placeHolder).getValue(p);
                }
                if (placeHolder.getHolderClass() == MyPetPlayer.class) {
                    if (MyPetApi.getPlayerManager().isMyPetPlayer(p)) {
                        return ((PlaceHolder<MyPetPlayer>) placeHolder).getValue(MyPetApi.getPlayerManager().getMyPetPlayer(p));
                    }
                }
                if (placeHolder.getHolderClass() == Pet.class) {
                    if (MyPetApi.getPlayerManager().isMyPetPlayer(p)) {
                        MyPetPlayer petPlayer = MyPetApi.getPlayerManager().getMyPetPlayer(p);
                        if (petPlayer.hasPet()) {
                            return ((PlaceHolder<Pet>) placeHolder).getValue(petPlayer.getPet());
                        }
                    }
                }
                return "";
            }
        };

        return myPetExpansion.register();
    }

    abstract static class PlaceHolder<T> {

        Class<T> clazz;

        public PlaceHolder(Class<T> clazz) {
            this.clazz = clazz;
        }

        public abstract String getValue(T holder);

        public Class<T> getHolderClass() {
            return clazz;
        }
    }
}