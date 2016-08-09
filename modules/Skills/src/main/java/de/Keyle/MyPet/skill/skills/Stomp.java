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

package de.Keyle.MyPet.skill.skills;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPetBukkitEntity;
import de.Keyle.MyPet.api.skill.ActiveSkill;
import de.Keyle.MyPet.api.skill.SkillInfo;
import de.Keyle.MyPet.api.skill.SkillInstance;
import de.Keyle.MyPet.api.skill.skills.StompInfo;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.keyle.knbt.TagDouble;
import de.keyle.knbt.TagInt;
import de.keyle.knbt.TagString;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Random;

public class Stomp extends StompInfo implements SkillInstance, ActiveSkill {
    private static Random random = new Random();
    private MyPet myPet;

    public Stomp(boolean addedByInheritance) {
        super(addedByInheritance);
    }

    public void setMyPet(MyPet myPet) {
        this.myPet = myPet;
    }

    public MyPet getMyPet() {
        return myPet;
    }

    public boolean isActive() {
        return chance > 0 && damage > 0;
    }

    public void upgrade(SkillInfo upgrade, boolean quiet) {
        if (upgrade instanceof StompInfo) {
            if (upgrade.getProperties().getCompoundData().containsKey("chance")) {
                if (!upgrade.getProperties().getCompoundData().containsKey("addset_chance") || upgrade.getProperties().getAs("addset_chance", TagString.class).getStringData().equals("add")) {
                    chance += upgrade.getProperties().getAs("chance", TagInt.class).getIntData();
                } else {
                    chance = upgrade.getProperties().getAs("chance", TagInt.class).getIntData();
                }
                chance = Math.min(chance, 100);
                if (!quiet) {
                    myPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Skill.Stomp.Upgrade", myPet.getOwner().getLanguage()), myPet.getPetName(), chance));
                }
            }
            if (upgrade.getProperties().getCompoundData().containsKey("damage")) {
                if (!upgrade.getProperties().getCompoundData().containsKey("addset_damage") || upgrade.getProperties().getAs("addset_damage", TagString.class).getStringData().equals("add")) {
                    damage += upgrade.getProperties().getAs("damage", TagDouble.class).getDoubleData();
                } else {
                    damage = upgrade.getProperties().getAs("damage", TagDouble.class).getDoubleData();
                }
            }
            if (!quiet) {
                myPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Skill.Stomp.Upgrade", myPet.getOwner().getLanguage()), myPet.getPetName(), chance, damage));
            }
        }
    }

    public String getFormattedValue() {
        return "" + ChatColor.GOLD + chance + ChatColor.RESET + "% -> " + ChatColor.GOLD + damage + ChatColor.RESET + " " + Translation.getString("Name.Damage", myPet.getOwner());
    }

    public void reset() {
        chance = 0;
    }

    public boolean activate() {
        return random.nextDouble() < chance / 100.;
    }

    public void stomp(Location location) {
        location.getWorld().playEffect(location, Effect.STEP_SOUND, Material.BEDROCK);
        if (MyPetApi.getCompatUtil().compareWithMinecraftVersion("1.9") >= 0) {
            location.getWorld().playSound(location, Sound.ENTITY_HOSTILE_BIG_FALL, 0.9F, 0.7F);
        } else {
            location.getWorld().playSound(location, Sound.valueOf("FALL_BIG"), 0.9F, 0.7F);
        }

        double posX = location.getX();
        double posY = location.getY();
        double posZ = location.getZ();

        for (Entity e : myPet.getEntity().get().getNearbyEntities(2.5, 2.5, 2.5)) {
            if (e instanceof LivingEntity) {
                final LivingEntity livingEntity = (LivingEntity) e;

                if (livingEntity instanceof Player) {
                    Player targetPlayer = (Player) livingEntity;
                    if (myPet.getOwner().equals(targetPlayer)) {
                        continue;
                    } else if (!MyPetApi.getHookHelper().canHurt(myPet.getOwner().getPlayer(), targetPlayer, true)) {
                        continue;
                    }
                } else if (livingEntity instanceof Tameable) {
                    Tameable tameable = (Tameable) livingEntity;
                    if (tameable.isTamed() && tameable.getOwner() != null) {
                        AnimalTamer tameableOwner = tameable.getOwner();
                        if (myPet.getOwner().equals(tameableOwner)) {
                            continue;
                        } else {
                            if (!MyPetApi.getHookHelper().canHurt(myPet.getOwner().getPlayer(), livingEntity)) {
                                continue;
                            }
                        }
                    }
                } else if (livingEntity instanceof MyPetBukkitEntity) {
                    MyPet targetMyPet = ((MyPetBukkitEntity) livingEntity).getMyPet();
                    if (!MyPetApi.getHookHelper().canHurt(myPet.getOwner().getPlayer(), targetMyPet.getOwner().getPlayer(), true)) {
                        continue;
                    }
                }
                if (!MyPetApi.getHookHelper().canHurt(myPet.getOwner().getPlayer(), livingEntity)) {
                    continue;
                }

                ((LivingEntity) e).damage(this.damage, myPet.getEntity().get());

                double distancePercent = MyPetApi.getPlatformHelper().distance(livingEntity.getLocation(), new Location(livingEntity.getWorld(), posX, posY, posZ)) / 2.5;
                if (distancePercent <= 1.0D) {
                    double distanceX = livingEntity.getLocation().getX() - posX;
                    double distanceY = livingEntity.getLocation().getX() + livingEntity.getEyeHeight() - posY;
                    double distanceZ = livingEntity.getLocation().getX() - posZ;
                    double distance = Math.sqrt(distanceX * distanceX + distanceY * distanceY + distanceZ * distanceZ);
                    if (distance != 0.0D) {
                        double motFactor = (1.0D - distancePercent);
                        final Vector velocity = livingEntity.getVelocity();
                        velocity.multiply(motFactor);
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                livingEntity.setVelocity(velocity);
                            }
                        }.runTaskLater(MyPetApi.getPlugin(), 0);
                    }
                }
            }
        }
    }

    @Override
    public SkillInstance cloneSkill() {
        Stomp newSkill = new Stomp(this.isAddedByInheritance());
        newSkill.setProperties(getProperties());
        return newSkill;
    }
}