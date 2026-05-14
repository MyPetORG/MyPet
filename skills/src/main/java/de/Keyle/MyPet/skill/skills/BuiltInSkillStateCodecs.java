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

import de.Keyle.MyPet.api.skill.SkillManager;
import de.Keyle.MyPet.api.skill.SkillStateCodec;
import de.Keyle.MyPet.api.skill.skills.Backpack;
import de.Keyle.MyPet.api.skill.skills.Beacon;
import de.Keyle.MyPet.api.skill.skills.Beacon.Buff;
import de.Keyle.MyPet.api.skill.skills.Beacon.BuffReceiver;
import de.Keyle.MyPet.api.skill.skills.Behavior;
import de.Keyle.MyPet.api.skill.skills.Behavior.BehaviorMode;
import de.Keyle.MyPet.api.skill.skills.Pickup;
import de.Keyle.MyPet.api.util.inventory.CustomInventory;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.kyori.adventure.nbt.StringBinaryTag;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Registers {@link SkillStateCodec}s for MyPet's four stateful built-in skills
 * (Backpack, Beacon, Behavior, Pickup). Each codec is the single source of
 * truth for its skill's NBT keys — both directions of the persistence
 * round-trip live in one place, so a typo or type mismatch is caught at
 * compile time instead of silently breaking saved state.
 *
 * <p>Stateless skills (Damage, Heal, Sprint, ...) carry no NBT, so no codec is
 * registered for them.
 */
public final class BuiltInSkillStateCodecs {

    private BuiltInSkillStateCodecs() {
    }

    public static void register(SkillManager skillManager) {
        skillManager.registerCodec(Backpack.class, Backpack.State.class, new SkillStateCodec<>() {
            @Override
            public CompoundBinaryTag write(Backpack.State state) {
                return state.inventory().save(CompoundBinaryTag.empty());
            }

            @Override
            public Optional<Backpack.State> read(CompoundBinaryTag compound) {
                if (compound.keySet().isEmpty()) return Optional.empty();
                ListBinaryTag items = compound.getList("Items", BinaryTagTypes.COMPOUND);
                int maxSlot = -1;
                for (int i = 0; i < items.size(); i++) {
                    CompoundBinaryTag item = items.getCompound(i);
                    if (item.keySet().contains("Slot")) {
                        int slot = item.getByte("Slot") & 0xff;
                        if (slot > maxSlot) maxSlot = slot;
                    }
                }
                int size = Math.min(54, Math.max(9, ((maxSlot + 9) / 9) * 9));
                CustomInventory inv = new CustomInventory();
                inv.setSize(size);
                inv.load(compound);
                return Optional.of(new Backpack.State(inv));
            }
        });

        skillManager.registerCodec(Beacon.class, Beacon.State.class, new SkillStateCodec<>() {
            @Override
            public CompoundBinaryTag write(Beacon.State state) {
                ListBinaryTag.Builder<StringBinaryTag> buffsBuilder = ListBinaryTag.builder(BinaryTagTypes.STRING);
                for (Buff buff : state.buffs()) {
                    buffsBuilder.add(StringBinaryTag.stringBinaryTag(buff.getName()));
                }
                return CompoundBinaryTag.builder()
                        .put("Buffs", buffsBuilder.build())
                        .putBoolean("Active", state.active())
                        .putString("Receiver", state.receiver().name())
                        .build();
            }

            @Override
            public Optional<Beacon.State> read(CompoundBinaryTag compound) {
                if (compound.keySet().isEmpty()) return Optional.empty();
                List<Buff> buffs = new ArrayList<>();
                if (compound.keySet().contains("Buffs")) {
                    ListBinaryTag list = compound.getList("Buffs", BinaryTagTypes.STRING);
                    for (int i = 0; i < list.size(); i++) {
                        Buff b = Buff.getByName(list.getString(i));
                        if (b != null) buffs.add(b);
                    }
                }
                boolean active = compound.keySet().contains("Active") && compound.getBoolean("Active");
                // Receiver is enum.valueOf — tolerate stale or unknown names by
                // falling back to Owner. The pre-codec live-skill load lacked
                // this catch and would propagate IllegalArgumentException up
                // through pet activation; consolidating to one codec lets the
                // lenient (parser) behavior win for both contexts.
                BuffReceiver receiver = BuffReceiver.Owner;
                if (compound.keySet().contains("Receiver")) {
                    try {
                        receiver = BuffReceiver.valueOf(compound.getString("Receiver"));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                return Optional.of(new Beacon.State(List.copyOf(buffs), active, receiver));
            }
        });

        skillManager.registerCodec(Behavior.class, Behavior.State.class, new SkillStateCodec<>() {
            @Override
            public CompoundBinaryTag write(Behavior.State state) {
                return CompoundBinaryTag.builder()
                        .putString("selectedBehavior", state.mode().name())
                        .build();
            }

            @Override
            public Optional<Behavior.State> read(CompoundBinaryTag compound) {
                if (!compound.keySet().contains("selectedBehavior")) return Optional.empty();
                BehaviorMode mode;
                try {
                    mode = BehaviorMode.valueOf(compound.getString("selectedBehavior"));
                } catch (IllegalArgumentException e) {
                    mode = BehaviorMode.Normal;
                }
                return Optional.of(new Behavior.State(mode));
            }
        });

        skillManager.registerCodec(Pickup.class, Pickup.State.class, new SkillStateCodec<>() {
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
    }
}
