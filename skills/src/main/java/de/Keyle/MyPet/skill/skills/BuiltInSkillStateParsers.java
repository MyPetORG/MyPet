package de.Keyle.MyPet.skill.skills;

import de.Keyle.MyPet.api.skill.SkillManager;
import de.Keyle.MyPet.api.skill.skills.Backpack;
import de.Keyle.MyPet.api.skill.skills.Beacon;
import de.Keyle.MyPet.api.skill.skills.Behavior;
import de.Keyle.MyPet.api.skill.skills.Beacon.Buff;
import de.Keyle.MyPet.api.skill.skills.Beacon.BuffReceiver;
import de.Keyle.MyPet.api.skill.skills.Behavior.BehaviorMode;
import de.Keyle.MyPet.api.skill.skills.Pickup;
import de.Keyle.MyPet.api.util.inventory.CustomInventory;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Registers {@link de.Keyle.MyPet.api.skill.SkillStateParser}s for MyPet's
 * four stateful built-in skills (Backpack, Beacon, Behavior, Pickup).
 * Mirrors {@code BuiltInUpgradeParsers.register(SkillManager)} — invoked
 * once during plugin enable, after the built-in skill classes themselves
 * are registered. Stateless skills (Damage, Heal, Sprint, ...) carry no
 * NBT, so no parser is registered for them.
 */
public final class BuiltInSkillStateParsers {

    private BuiltInSkillStateParsers() {
    }

    public static void register(SkillManager skillManager) {
        skillManager.registerStateParser(Backpack.class, Backpack.State.class, compound -> {
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
        });

        skillManager.registerStateParser(Beacon.class, Beacon.State.class, compound -> {
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
            BuffReceiver receiver = BuffReceiver.Owner;
            if (compound.keySet().contains("Receiver")) {
                try {
                    receiver = BuffReceiver.valueOf(compound.getString("Receiver"));
                } catch (IllegalArgumentException ignored) {
                    // fall through to Owner
                }
            }
            return Optional.of(new Beacon.State(List.copyOf(buffs), active, receiver));
        });

        skillManager.registerStateParser(Behavior.class, Behavior.State.class, compound -> {
            if (!compound.keySet().contains("selectedBehavior")) return Optional.empty();
            BehaviorMode mode;
            try {
                mode = BehaviorMode.valueOf(compound.getString("selectedBehavior"));
            } catch (IllegalArgumentException e) {
                mode = BehaviorMode.Normal;
            }
            return Optional.of(new Behavior.State(mode));
        });

        skillManager.registerStateParser(Pickup.class, Pickup.State.class, compound -> {
            if (compound.keySet().isEmpty()) return Optional.empty();
            return Optional.of(new Pickup.State(compound.getBoolean("Active")));
        });
    }
}
