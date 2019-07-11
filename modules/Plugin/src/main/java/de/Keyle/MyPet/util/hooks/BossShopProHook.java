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

package de.Keyle.MyPet.util.hooks;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.entity.StoredMyPet;
import de.Keyle.MyPet.api.exceptions.MyPetTypeNotFoundException;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.repository.RepositoryCallback;
import de.Keyle.MyPet.api.skill.skilltree.Skilltree;
import de.Keyle.MyPet.api.util.hooks.PluginHook;
import de.Keyle.MyPet.api.util.hooks.PluginHookName;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.Keyle.MyPet.commands.admin.CommandOptionCreate;
import de.Keyle.MyPet.util.shop.ShopMyPet;
import de.keyle.knbt.TagCompound;
import org.black_ixx.bossshop.core.BSBuy;
import org.black_ixx.bossshop.core.rewards.BSRewardType;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

@PluginHookName("BossShopPro")
public class BossShopProHook implements PluginHook {

    @Override
    public boolean onEnable() {
        new MyPetReward().register();
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onDisable() {
        try {
            Field FIELD_TYPES = BSRewardType.class.getDeclaredField("types");
            FIELD_TYPES.setAccessible(true);
            List<BSRewardType> types = (List<BSRewardType>) FIELD_TYPES.get(null);
            types.removeIf(type -> type.getClass().getName().equals("MyPetReward"));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    class MyPetReward extends BSRewardType {

        @Override
        public void enableType() {
        }

        @Override
        public Object createObject(Object o, boolean force_final_state) {
            return o;
        }

        @Override
        public boolean validityCheck(String item_name, Object o) {
            return true;
        }

        @Override
        public boolean canBuy(Player p, BSBuy buy, boolean message_if_no_success, Object reward, ClickType clickType) {
            if (MyPetApi.getPlayerManager().isMyPetPlayer(p)) {
                MyPetPlayer owner = MyPetApi.getPlayerManager().getMyPetPlayer(p);

                if (owner.hasMyPet() && !Permissions.has(owner, "MyPet.shop.storage")) {
                    p.sendMessage(Translation.getString("Message.Command.Trade.Receiver.HasPet", p));
                    return false;
                }
            }
            return true;
        }

        @Override
        public void giveReward(Player p, BSBuy buy, Object reward, ClickType clickType) {
            ShopMyPet pet = parsePet(reward);

            MyPetPlayer petOwner;
            if (MyPetApi.getPlayerManager().isMyPetPlayer(p)) {
                petOwner = MyPetApi.getPlayerManager().getMyPetPlayer(p);
            } else {
                petOwner = MyPetApi.getPlayerManager().registerMyPetPlayer(p);
            }

            pet.setOwner(petOwner);
            final StoredMyPet clonedPet = MyPetApi.getMyPetManager().getInactiveMyPetFromMyPet(pet);

            clonedPet.setOwner(petOwner);
            clonedPet.setWorldGroup(WorldGroup.getGroupByWorld(p.getWorld().getName()).getName());
            clonedPet.setUUID(null);

            MyPetApi.getRepository().addMyPet(clonedPet, new RepositoryCallback<Boolean>() {
                @Override
                public void callback(Boolean value) {
                    if (petOwner.hasMyPet()) {
                        p.sendMessage(Util.formatText(Translation.getString("Message.Shop.SuccessStorage", p), clonedPet.getPetName()));
                    } else {
                        petOwner.setMyPetForWorldGroup(WorldGroup.getGroupByWorld(p.getWorld().getName()), clonedPet.getUUID());
                        MyPetApi.getRepository().updateMyPetPlayer(petOwner, null);
                        MyPet activePet = MyPetApi.getMyPetManager().activateMyPet(clonedPet).get();
                        activePet.createEntity();
                    }
                }
            });
        }

        @Override
        public String getDisplayReward(Player p, BSBuy buy, Object reward, ClickType clickType) {
            ShopMyPet pet = parsePet(reward);
            return pet.getPetName();
        }

        @Override
        public String[] createNames() {
            return new String[]{"mypet"};
        }

        @Override
        public boolean mightNeedShopUpdate() {
            return true;
        }

        protected ShopMyPet parsePet(Object o) {
            ShopMyPet pet = new ShopMyPet("" + o.hashCode());

            String[] optionsArray = null;

            if (o instanceof ArrayList) {
                for (Object os : (ArrayList) o) {
                    if (os instanceof String) {
                        String setting = os.toString();
                        if (setting.toLowerCase().startsWith("name:")) {
                            pet.setPetName(setting.substring(5));
                        } else if (setting.toLowerCase().startsWith("skilltree:")) {
                            String skilltreeName = setting.substring(10);
                            Skilltree skilltree = MyPetApi.getSkilltreeManager().getSkilltree(skilltreeName);
                            pet.setSkilltree(skilltree);
                        } else if (setting.toLowerCase().startsWith("pettype:")) {
                            try {
                                pet.setPetType(MyPetType.byName(setting.substring(8)));
                            } catch (MyPetTypeNotFoundException e) {
                                MyPetApi.getLogger().info("[BossShop] " + setting.substring(9) + " is not a valid pet type!");
                            }
                        } else if (setting.toLowerCase().startsWith("exp:")) {
                            String exp = setting.substring(4);
                            if (Util.isDouble(exp)) {
                                pet.setExp(Double.parseDouble(exp));
                            }
                        }
                    } else if (os instanceof ArrayList) {
                        ArrayList options = (ArrayList) os;
                        if (options.size() > 0) {
                            optionsArray = new String[options.size()];
                            for (int i = 0; i < options.size(); i++) {
                                optionsArray[i] = options.get(i).toString();
                            }
                        }
                    }
                }
            }

            if (optionsArray != null) {
                TagCompound compound = new TagCompound();
                CommandOptionCreate.createInfo(pet.getPetType(), optionsArray, compound);
                pet.setInfo(compound);
            }

            return pet;
        }
    }
}