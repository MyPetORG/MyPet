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
import de.Keyle.MyPet.api.MyPetGlobal;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.entity.Pet.PetState;
import de.Keyle.MyPet.api.event.PetInventoryActionEvent;
import de.Keyle.MyPet.api.event.PetPickupItemEvent;
import de.Keyle.MyPet.api.player.AdminPermissions;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.skill.SkillState;
import de.Keyle.MyPet.api.skill.SkillStateCodec;
import de.Keyle.MyPet.api.skill.SkillStateCodecs;
import de.Keyle.MyPet.api.skill.SkillUpgrades;
import de.Keyle.MyPet.api.skill.UpgradeComputer;
import de.Keyle.MyPet.api.skill.UpgradeParsers;
import de.Keyle.MyPet.api.skill.UpgradeSchema;
import de.Keyle.MyPet.api.skill.skills.Pickup;
import de.Keyle.MyPet.api.util.locale.Locale;
import de.Keyle.MyPet.skill.upgrades.PickupUpgrade;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Item;
import org.bukkit.entity.Mob;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.Optional;

public class PickupImpl extends AbstractSkill implements Pickup {

    public static final SkillUpgrades UPGRADES = SkillUpgrades.of(Pickup.class,
            UpgradeSchema.builder()
                    .number("range").label("Range").cumulative()
                    .bool("exp").label("EXP Pickup")
                    .build(), json -> new PickupUpgrade()
            .setRangeModifier(UpgradeParsers.parseNumber(UpgradeParsers.get(json, "range")))
            .setPickupExpModifier(UpgradeParsers.parseBoolean(UpgradeParsers.get(json, "exp"))));

    public static final SkillStateCodecs STATE_CODEC = SkillStateCodecs.of(Pickup.class, Pickup.State.class,
            new SkillStateCodec<>() {
                @Override
                public CompoundBinaryTag write(Pickup.State state) {
                    return CompoundBinaryTag.builder()
                            .putBoolean("Active", state.active())
                            .build();
                }

                @Override
                public Optional<Pickup.State> read(CompoundBinaryTag compound) {
                    if (compound.keySet().isEmpty()) return Optional.empty();
                    return Optional.of(new Pickup.State(compound.getBoolean("Active")));
                }
            });

    protected UpgradeComputer<Number> range = new UpgradeComputer<>(0);
    protected UpgradeComputer<Boolean> expPickup = new UpgradeComputer<>(false);
    private boolean pickup = false;

    public PickupImpl(Pet pet) {
        super(pet);
    }

    public boolean isActive() {
        return range.getValue().doubleValue() > 0;
    }

    @Override
    public void reset() {
        range.removeAllUpgrades();
        expPickup.removeAllUpgrades();
    }

    public Component toPrettyComponent(String locale) {
        return Component.text()
                .append(Locale.getComponent("Name.Range", locale))
                .append(Component.text(": "))
                .append(Component.text(String.format("%1.2f", range.getValue().doubleValue())).color(NamedTextColor.GOLD))
                .append(Component.space())
                .append(Locale.getComponent("Name.Blocks", locale))
                .asComponent();
    }

    @Override
    public Component[] getUpgradeMessage() {
        return new Component[]{
                upgradeMessage("Message.Skill.Pickup.Upgrade", String.format("%1.2f", getRange().getValue().doubleValue()))
        };
    }

    public boolean activate() {
        if (isActive()) {
            if (pet.getSkills().isActive(BackpackImpl.class)) {
                if (pickup) {
                    pickup = false;
                } else {
                    PetInventoryActionEvent event = new PetInventoryActionEvent(pet, PetInventoryActionEvent.Action.PICKUP);
                    Bukkit.getServer().getPluginManager().callEvent(event);
                    if (!event.isCancelled()) {
                        pickup = true;
                    }
                }

                Component mode = pickup ? Locale.getComponent("Name.Enabled", pet.getOwner()) : Locale.getComponent("Name.Disabled", pet.getOwner());
                pet.getOwner().sendMessage(Locale.getFormattedComponent("Message.Skill.Pickup.StartStop", pet.getOwner(), pet.getDisplayName(), mode));
                return true;
            } else {
                pet.getOwner().sendMessage(Locale.getFormattedComponent("Message.Skill.Pickup.NoInventory", pet.getOwner(), pet.getDisplayName()));
                return false;
            }
        } else {
            pet.getOwner().sendMessage(Locale.getFormattedComponent("Message.No.Skill", pet.getOwner(), pet.getDisplayName(), this.getName(pet.getOwner().getLanguage())));
            return false;
        }
    }

    public void schedule() {
        // Fire the per-second USE heartbeat unconditionally so third-party listeners
        // still observe it while pickup is toggled off; only the pickup work is gated.
        PetInventoryActionEvent event = new PetInventoryActionEvent(pet, PetInventoryActionEvent.Action.USE);
        Bukkit.getServer().getPluginManager().callEvent(event);
        if (!pickup) {
            return;
        }
        if (event.isCancelled() || !Permissions.hasExtended(pet.getOwner().getPlayer(), "MyPet.extended.pickup")) {
            pickup = false;
            pet.getOwner().sendMessage(Locale.getFormattedComponent("Message.Skill.Pickup.StartStop", pet.getOwner().getPlayer(), pet.getDisplayName(), Locale.getComponent("Name.Disabled", pet.getOwner())));
            return;
        }
        if (pet.getOwner().getPlayer().getGameMode() == GameMode.CREATIVE && !MyPetGlobal.Skilltree.Skill.Backpack.OPEN_IN_CREATIVE.get() && !Permissions.has(pet.getOwner().getPlayer(), AdminPermissions.BYPASS_CREATIVE)) {
            pet.getOwner().sendMessage(Locale.getComponent("Message.Skill.Pickup.Creative", pet.getOwner()));
            pickup = false;
            return;
        }
        if (isActive() && pet.getStatus() == PetState.Here && pet.getSkills().isActive(BackpackImpl.class)) {
            Mob petEntity = pet.getBukkitEntity();
            if (petEntity != null) {
                double range = this.range.getValue().doubleValue();
                for (Entity entity : petEntity.getNearbyEntities(range, range, range)) {
                    if (!entity.isDead()) {
                        if (entity instanceof Item itemEntity) {
                            ItemStack itemStack = itemEntity.getItemStack();

                            if (itemEntity.getPickupDelay() <= 0 && itemStack.getAmount() > 0) {
                                PetPickupItemEvent petPickupEvent = new PetPickupItemEvent(pet, itemEntity);
                                Bukkit.getServer().getPluginManager().callEvent(petPickupEvent);

                                if (petPickupEvent.isCancelled()) {
                                    continue;
                                }

                                EntityPickupItemEvent entityPickupEvent = new EntityPickupItemEvent(pet.getOwner().getPlayer(), itemEntity, 0);
                                Bukkit.getServer().getPluginManager().callEvent(entityPickupEvent);

                                if (entityPickupEvent.isCancelled()) {
                                    continue;
                                }

                                itemStack = itemEntity.getItemStack();

                                int itemAmount = pet.getSkills().get(BackpackImpl.class).addItem(itemStack);
                                if (itemAmount == 0) {
                                    animatePickup(petEntity, itemEntity); // magnet the item into the pet, then remove it
                                } else {
                                    itemStack.setAmount(itemAmount);
                                    itemEntity.setItemStack(itemStack);
                                }
                            }
                        } else if (expPickup.getValue() && entity instanceof ExperienceOrb expEntity) {
                            pet.getOwner().getPlayer().giveExp(expEntity.getExperience());
                            expEntity.setExperience(0);
                            expEntity.remove();
                        }
                    }
                }
            }
        }
    }

    /** Ticks the collected item spends flying toward the pet before it vanishes. */
    private static final long PICKUP_FLY_TICKS = 8L;

    /**
     * Plays the classic pickup animation: the already-collected item entity magnets toward the pet
     * for a few ticks, then vanishes with the pickup sound. The item is kept unpickable during the
     * flight so nothing else grabs it, and the retired callback still removes it if the entity dies.
     */
    private void animatePickup(Mob pet, Item item) {
        item.setPickupDelay(Short.MAX_VALUE);
        int[] ticks = {0};
        ScheduledTask task = item.getScheduler().runAtFixedRate(MyPetApi.getPlugin(), t -> {
            if (!item.isValid()) {
                t.cancel();
                return;
            }
            if (!pet.isValid() || ticks[0]++ >= PICKUP_FLY_TICKS) {
                finishPickup(pet, item);
                t.cancel();
                return;
            }
            Vector delta = pet.getLocation().add(0, pet.getHeight() * 0.4, 0).subtract(item.getLocation()).toVector();
            if (delta.lengthSquared() < 0.25) {
                finishPickup(pet, item);
                t.cancel();
                return;
            }
            item.setVelocity(delta.multiply(0.6));
        }, () -> finishPickup(pet, item), 1L, 1L);
        if (task == null) {
            finishPickup(pet, item); // scheduler already retired (mob mid-teleport) — just collect it
        }
    }

    private static void finishPickup(Mob pet, Item item) {
        if (pet.isValid()) {
            pet.getWorld().playSound(pet.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.2F, 1.0F);
        }
        item.remove();
    }

    @Override
    public void applyState(SkillState state) {
        if (state instanceof State picked) {
            pickup = picked.active();
        }
    }

    public UpgradeComputer<Number> getRange() {
        return range;
    }

    public UpgradeComputer<Boolean> getExpPickup() {
        return expPickup;
    }

    @Override
    public boolean isPickupEnabled() {
        return pickup;
    }

    @Override
    public void setPickupEnabled(boolean enabled) {
        pickup = enabled;
    }

    @Override
    public Optional<State> getState() {
        return Optional.of(new State(pickup));
    }

}
