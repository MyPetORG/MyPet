/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2014 Keyle
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

package de.Keyle.MyPet.skill.skills.implementation;

import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.skill.skills.ISkillActive;
import de.Keyle.MyPet.skill.skills.info.ISkillInfo;
import de.Keyle.MyPet.skill.skills.info.StompInfo;
import de.Keyle.MyPet.util.Util;
import de.Keyle.MyPet.util.hooks.PvPChecker;
import de.Keyle.MyPet.util.locale.Locales;
import de.keyle.knbt.TagDouble;
import de.keyle.knbt.TagInt;
import de.keyle.knbt.TagString;
import net.minecraft.server.v1_8_R1.*;
import net.minecraft.server.v1_8_R1.World;
import org.bukkit.*;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R1.entity.CraftLivingEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Random;

public class Stomp extends StompInfo implements ISkillInstance, ISkillActive {
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

    public void upgrade(ISkillInfo upgrade, boolean quiet) {
        if (upgrade instanceof StompInfo) {
            if (upgrade.getProperties().getCompoundData().containsKey("chance")) {
                if (!upgrade.getProperties().getCompoundData().containsKey("addset_chance") || upgrade.getProperties().getAs("addset_chance", TagString.class).getStringData().equals("add")) {
                    chance += upgrade.getProperties().getAs("chance", TagInt.class).getIntData();
                } else {
                    chance = upgrade.getProperties().getAs("chance", TagInt.class).getIntData();
                }
                chance = Math.min(chance, 100);
                if (!quiet) {
                    myPet.sendMessageToOwner(Util.formatText(Locales.getString("Message.Skill.Stomp.Upgrade", myPet.getOwner().getLanguage()), myPet.getPetName(), chance));
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
                myPet.sendMessageToOwner(Util.formatText(Locales.getString("Message.Skill.Stomp.Upgrade", myPet.getOwner().getLanguage()), myPet.getPetName(), chance, damage));
            }
        }
    }

    public String getFormattedValue() {
        return "" + ChatColor.GOLD + chance + ChatColor.RESET + "% -> " + ChatColor.GOLD + damage + ChatColor.RESET + " " + Locales.getString("Name.Damage", myPet.getOwner());
    }

    public void reset() {
        chance = 0;
    }

    public boolean activate() {
        return random.nextDouble() < chance / 100.;
    }

    public void stomp(Location location) {
        location.getWorld().playEffect(location, Effect.STEP_SOUND, Material.BEDROCK);
        location.getWorld().playSound(location, Sound.FALL_BIG, 0.9F, 0.7F);

        World w = ((CraftWorld) location.getWorld()).getHandle();
        Vec3D vec3d = new Vec3D(location.getX(), location.getY(), location.getZ());
        double posX = location.getX();
        double posY = location.getY();
        double posZ = location.getZ();

        for (Entity e : myPet.getCraftPet().getNearbyEntities(2.5, 2.5, 2.5)) {
            if (e instanceof CraftLivingEntity) {
                EntityLiving livingEntity = ((CraftLivingEntity) e).getHandle();

                if (livingEntity != myPet.getCraftPet().getHandle()) {
                    if (livingEntity instanceof EntityPlayer) {
                        Player targetPlayer = (Player) livingEntity.getBukkitEntity();
                        if (myPet.getOwner().equals(targetPlayer)) {
                            continue;
                        } else if (!PvPChecker.canHurt(myPet.getOwner().getPlayer(), targetPlayer)) {
                            continue;
                        }
                    } else if (livingEntity instanceof EntityTameableAnimal) {
                        EntityTameableAnimal tameable = (EntityTameableAnimal) livingEntity;
                        if (tameable.isTamed() && tameable.getOwner() != null) {
                            Player tameableOwner = (Player) tameable.getOwner().getBukkitEntity();
                            if (myPet.getOwner().equals(tameableOwner)) {
                                continue;
                            } else if (!PvPChecker.canHurt(myPet.getOwner().getPlayer(), tameableOwner)) {
                                continue;
                            }
                        }
                    } else if (livingEntity instanceof EntityMyPet) {
                        MyPet targetMyPet = ((EntityMyPet) livingEntity).getMyPet();
                        if (!PvPChecker.canHurt(myPet.getOwner().getPlayer(), targetMyPet.getOwner().getPlayer())) {
                            continue;
                        }
                    }
                    if (!PvPChecker.canHurtCitizens(livingEntity.getBukkitEntity())) {
                        continue;
                    }
                } else {
                    continue;
                }

                ((CraftLivingEntity) e).damage(this.damage, myPet.getCraftPet());

                double distancePercent = livingEntity.f(posX, posY, posZ) / 2.5;
                if (distancePercent <= 1.0D) {
                    double distanceX = livingEntity.locX - posX;
                    double distanceY = livingEntity.locY + livingEntity.getHeadHeight() - posY;
                    double distanceZ = livingEntity.locZ - posZ;
                    double distance = MathHelper.sqrt(distanceX * distanceX + distanceY * distanceY + distanceZ * distanceZ);
                    if (distance != 0.0D) {
                        distanceX /= distance;
                        distanceY /= distance;
                        distanceZ /= distance;
                        double d = w.a(vec3d, livingEntity.getBoundingBox());
                        double motFactor = (1.0D - distancePercent) * d;
                        livingEntity.motX += distanceX * motFactor;
                        livingEntity.motY += distanceY * motFactor;
                        livingEntity.motZ += distanceZ * motFactor;
                    }
                }
            }
        }
    }

    @Override
    public ISkillInstance cloneSkill() {
        Stomp newSkill = new Stomp(this.isAddedByInheritance());
        newSkill.setProperties(getProperties());
        return newSkill;
    }
}