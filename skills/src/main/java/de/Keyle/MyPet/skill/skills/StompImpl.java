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

package de.Keyle.MyPet.skill.skills;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.skill.UpgradeComputer;
import de.Keyle.MyPet.api.skill.skills.Stomp;
import de.Keyle.MyPet.api.util.locale.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

public class StompImpl extends AbstractSkill implements Stomp {

    protected UpgradeComputer<Integer> chance = new UpgradeComputer<>(0);
    protected UpgradeComputer<Number> damage = new UpgradeComputer<>(0);

    public StompImpl(Pet pet) {
        super(pet);
    }

    public boolean isActive() {
        return chance.getValue() > 0 && damage.getValue().doubleValue() > 0;
    }

    @Override
    public void reset() {
        chance.removeAllUpgrades();
        damage.removeAllUpgrades();
    }

    public Component toPrettyComponent(String locale) {
        return Component.text()
                .append(Component.text(chance.getValue()).color(NamedTextColor.GOLD))
                .append(Component.text("% -> "))
                .append(Component.text(damage.getValue().doubleValue()).color(NamedTextColor.GOLD))
                .append(Component.space())
                .append(Locale.getComponent("Name.Damage", locale))
                .asComponent();
    }

    @Override
    public Component[] getUpgradeMessage() {
        return new Component[]{
                upgradeMessage("Message.Skill.Stomp.Upgrade", getChance().getValue(), getChance().getValue().doubleValue())
        };
    }

    public boolean trigger() {
        return ThreadLocalRandom.current().nextDouble() < chance.getValue() / 100.;
    }

    public void apply(LivingEntity target) {
        Location location = target.getLocation();
        location.getWorld().playEffect(location, Effect.STEP_SOUND, Material.BEDROCK);
        location.getWorld().playSound(location, Sound.ENTITY_HOSTILE_BIG_FALL, 0.9F, 0.7F);

        double posX = location.getX();
        double posY = location.getY();
        double posZ = location.getZ();

        Mob petEntity = pet.getBukkitEntity();
        if (petEntity != null) {
            for (Entity e : petEntity.getNearbyEntities(2.5, 2.5, 2.5)) {
                if (e instanceof LivingEntity livingEntity) {

                    if (livingEntity instanceof Player targetPlayer) {
                        if (pet.getOwner().equals(targetPlayer)) {
                            continue;
                        } else if (!MyPetApi.getHookHelper().canHurt(pet.getOwner().getPlayer(), targetPlayer, true)) {
                            continue;
                        }
                    } else if (livingEntity instanceof Tameable tameable) {
                        if (tameable.isTamed() && tameable.getOwner() != null) {
                            AnimalTamer tameableOwner = tameable.getOwner();
                            if (pet.getOwner().equals(tameableOwner)) {
                                continue;
                            } else {
                                if (!MyPetApi.getHookHelper().canHurt(pet.getOwner().getPlayer(), livingEntity)) {
                                    continue;
                                }
                            }
                        }
                    } else {
                        Pet targetPet = MyPetApi.getPetManager().getPetFromEntity(livingEntity);
                        if (targetPet != null && targetPet.getOwner() != null
                                && !MyPetApi.getHookHelper().canHurt(pet.getOwner().getPlayer(), targetPet.getOwner().getPlayer(), true)) {
                            continue;
                        }
                    }
                    if (!MyPetApi.getHookHelper().canHurt(pet.getOwner().getPlayer(), livingEntity)) {
                        continue;
                    }

                    livingEntity.damage(this.damage.getValue().doubleValue(), petEntity);

                    double distancePercent = livingEntity.getLocation().distance(new Location(livingEntity.getWorld(), posX, posY, posZ)) / 2.5;
                    if (distancePercent <= 1.0D) {
                        double distanceX = livingEntity.getLocation().getX() - posX;
                        double distanceY = livingEntity.getLocation().getX() + livingEntity.getEyeHeight() - posY;
                        double distanceZ = livingEntity.getLocation().getX() - posZ;
                        double distance = Math.sqrt(distanceX * distanceX + distanceY * distanceY + distanceZ * distanceZ);
                        if (distance != 0.0D) {
                            double motFactor = (1.0D - distancePercent);
                            final Vector velocity = livingEntity.getVelocity();
                            velocity.multiply(motFactor);
                            livingEntity.getScheduler().runDelayed(MyPetApi.getPlugin(), t -> livingEntity.setVelocity(velocity), null, 1L);
                        }
                    }
                }
            }
        }
    }

    public UpgradeComputer<Integer> getChance() {
        return chance;
    }

    public UpgradeComputer<Number> getDamage() {
        return damage;
    }

}
