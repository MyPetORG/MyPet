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

package de.Keyle.MyPet.api.event;

import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.skill.OnHitSkill;
import lombok.Getter;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.NonNull;

/**
 * Fired before a pet's on-hit skill executes against a living target. Dispatched
 * once per on-hit skill per strike — a pet with both Bleed and Fire on a
 * single mêlée swing fires the event twice in sequence.
 *
 * <p>Fires from {@code PetSkillTriggerListener} during the mêlée chain, after
 * {@link PetDamageEvent} (the damage step) and before each
 * {@link OnHitSkill#apply(LivingEntity)} call.
 *
 * <p><b>Cancellable:</b> cancellation suppresses this specific on-hit skill;
 * other on-hit skills in the chain still run. To veto the entire strike,
 * cancel {@link PetDamageEvent} instead — that aborts the damage and the
 * full on-hit chain together.
 *
 * <p><b>Pet state:</b> live pet, with the owner online. Skill is the live
 * {@link OnHitSkill} instance — listeners may call its accessors to read
 * upgrade levels but should not mutate state mid-strike.
 *
 * <p><b>Note:</b> there is no symmetric event for active-skill dispatch.
 * Active skills run via the skill's own {@code activate()} flow without a
 * per-skill event hook today.
 */
public class PetOnHitSkillEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    @Getter
    private final MyPet pet;
    @Getter
    private final OnHitSkill skill;
    @Getter
    private final LivingEntity target;
    private boolean isCancelled = false;

    public PetOnHitSkillEvent(MyPet pet, OnHitSkill skill, LivingEntity target) {
        this.pet = pet;
        this.skill = skill;
        this.target = target;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public MyPetPlayer getOwner() {
        return pet.getOwner();
    }

    public Player getPlayer() {
        return pet.getOwner().getPlayer();
    }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        isCancelled = cancelled;
    }

    @Override
    public @NonNull HandlerList getHandlers() {
        return handlers;
    }
}