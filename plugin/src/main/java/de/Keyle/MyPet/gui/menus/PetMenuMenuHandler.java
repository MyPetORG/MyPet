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

package de.Keyle.MyPet.gui.menus;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.dialog.ConfirmPromptSpec;
import de.Keyle.MyPet.api.dialog.TextPromptSpec;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.entity.PetEquipment;
import de.Keyle.MyPet.api.entity.StoredPet;
import de.Keyle.MyPet.api.event.PetRemoveEvent;
import de.Keyle.MyPet.api.event.PetSelectSkilltreeEvent;
import de.Keyle.MyPet.api.gui.ClickPayload;
import de.Keyle.MyPet.api.gui.ItemAppearance;
import de.Keyle.MyPet.api.gui.MenuDefinition;
import de.Keyle.MyPet.api.gui.MenuId;
import de.Keyle.MyPet.api.gui.MenuIds;
import de.Keyle.MyPet.api.gui.MenuInstance;
import de.Keyle.MyPet.api.gui.MenuHandler;
import de.Keyle.MyPet.api.gui.PaginatedListSection;
import de.Keyle.MyPet.api.gui.Section;
import de.Keyle.MyPet.api.gui.SlotSection;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.skill.skills.Backpack;
import de.Keyle.MyPet.api.skill.skilltree.Skilltree;
import de.Keyle.MyPet.api.util.locale.Locale;
import de.Keyle.MyPet.entity.spawn.VanillaMobSpawner;
import de.Keyle.MyPet.gui.MenuInstanceImpl;
import de.Keyle.MyPet.gui.context.BackpackContext;
import de.Keyle.MyPet.gui.context.ChooseSkilltreeContext;
import de.Keyle.MyPet.gui.context.PetMenuContext;
import de.Keyle.MyPet.gui.context.PetSelectionContext;
import de.Keyle.MyPet.gui.context.PetTradeTargetContext;
import de.Keyle.MyPet.services.EggIconService;
import de.Keyle.MyPet.skill.skills.BackpackImpl;
import de.Keyle.MyPet.skill.skills.BeaconImpl;
import de.Keyle.MyPet.skill.skills.BehaviorImpl;
import de.Keyle.MyPet.skill.skills.PickupImpl;
import de.Keyle.MyPet.api.skill.skilltree.Skill;
import org.bukkit.Material;
import de.Keyle.MyPet.util.NameFilter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Hub menu for {@code /pet}. Each section is a single button that toggles state,
 * pushes another menu, or kicks off a Dialog flow.
 */
public final class PetMenuMenuHandler implements MenuHandler<PetMenuContext> {

    @SuppressWarnings("unchecked")
    @Override public MenuId<PetMenuContext> id() {
        return (MenuId<PetMenuContext>) MenuIds.PET_MENU;
    }

    @Override public TagResolver titlePlaceholders(PetMenuContext context) {
        // Deserialize so a MiniMessage-formatted pet name renders in the title.
        return Placeholder.component("pet_name",
            Util.SANITIZED_MINIMESSAGE.deserialize(context.pet().getPetName()));
    }

    @Override public void onOpen(MenuInstance instance, PetMenuContext context) {
        Pet pet = context.pet();
        instance.setSlotState("stay", pet.isSitting() ? "staying" : "following");
        instance.setSlotState("auto-respawn", pet.getOwner().hasAutoRespawnEnabled() ? "on" : "off");
        if (isSlotVisible(context, "pickup")) {
            PickupImpl pickup = pet.getSkills().get(PickupImpl.class);
            instance.setSlotState("pickup", pickup != null && pickup.isPickupEnabled() ? "on" : "off");
        }
    }

    @Override
    public void onClick(MenuInstance instance, String sectionId, ClickPayload payload) {
        PetMenuContext ctx = (PetMenuContext) ((MenuInstanceImpl) instance).context();
        Pet pet = ctx.pet();
        Player viewer = ctx.viewer();
        switch (sectionId) {
            case "rename" -> openRenameDialog(instance, ctx);
            case "call" -> {
                if (pet.getBukkitEntity() != null) {
                    pet.removePet(true);
                }
                pet.callToOwner();
                instance.close();
            }
            case "stay" -> {
                pet.setSitting(!pet.isSitting());
                refreshStateSlot(instance, "stay");
            }
            case "auto-respawn" -> {
                MyPetPlayer owner = pet.getOwner();
                owner.setAutoRespawnEnabled(!owner.hasAutoRespawnEnabled());
                refreshStateSlot(instance, "auto-respawn");
            }
            case "send-away" -> {
                pet.sendAway();
                instance.close();
            }
            case "store" -> {
                storePet(viewer, pet);
                instance.close();
            }
            case "switch" -> openSwitchMenu(viewer);
            case "trade" -> {
                MyPetApi.getGuiService().openMenu(
                    viewer,
                    (MenuId<PetTradeTargetContext>) (MenuId<?>) MenuIds.PET_TRADE_TARGET,
                    new PetTradeTargetContext(viewer, pet)
                );
            }
            case "release" -> openReleaseDialog(instance, ctx);
            case "choose-skilltree" -> openChooseSkilltreeMenu(viewer, pet);
            case "skill-menus" -> {
                List<SkillMenuEntry> available = availableSkillMenus(viewer, pet);
                int idx = payload.itemIndex();
                if (idx >= 0 && idx < available.size()) {
                    available.get(idx).open(viewer, pet);
                }
            }
            case "behavior" -> {
                BehaviorImpl behavior = pet.getSkills().get(BehaviorImpl.class);
                if (behavior != null) behavior.activate();
                instance.refreshSection("behavior");
            }
            case "pickup" -> {
                PickupImpl pickup = pet.getSkills().get(PickupImpl.class);
                if (pickup != null) {
                    pickup.setPickupEnabled(!pickup.isPickupEnabled());
                    refreshStateSlot(instance, "pickup");
                }
            }
            case "stop-attacking" -> pet.forgetTarget();
            case "volume" -> openVolumeMenu(viewer);
            default -> {}
        }
    }

    @SuppressWarnings("unchecked")
    private void openVolumeMenu(Player viewer) {
        MyPetApi.getGuiService().openMenu(
            viewer,
            (MenuId<de.Keyle.MyPet.gui.context.PetVolumeContext>) (MenuId<?>) MenuIds.PET_VOLUME,
            new de.Keyle.MyPet.gui.context.PetVolumeContext(viewer)
        );
    }

    @Override
    public boolean isSlotVisible(PetMenuContext context, String sectionId) {
        Player viewer = context.viewer();
        return switch (sectionId) {
            case "rename" -> viewer.hasPermission("MyPet.command.name");
            case "call" -> viewer.hasPermission("MyPet.command.call");
            case "stay" -> viewer.hasPermission("MyPet.command.stay");
            case "send-away" -> viewer.hasPermission("MyPet.command.sendaway");
            case "switch" -> viewer.hasPermission("MyPet.command.switch");
            case "trade" -> viewer.hasPermission("MyPet.command.trade");
            case "release" -> viewer.hasPermission("MyPet.command.release");
            case "choose-skilltree" -> viewer.hasPermission("MyPet.command.chooseskilltree");
            case "store" -> viewer.hasPermission("MyPet.command.store");
            case "behavior" -> viewer.hasPermission("MyPet.command.behavior")
                && context.pet().getSkills().isActive(BehaviorImpl.class);
            case "pickup" -> viewer.hasPermission("MyPet.command.pickup")
                && context.pet().getSkills().isActive(PickupImpl.class);
            default -> true;
        };
    }

    @Override
    public ItemAppearance customizeSlotItem(PetMenuContext context, String sectionId,
                                            ItemAppearance appearance) {
        if (!"info".equals(sectionId)) return appearance;
        EggIconService.Resolved icon = MyPetApi.getServiceManager()
            .getService(EggIconService.class)
            .map(svc -> svc.resolve(context.pet().getPetType()))
            .orElse(null);
        if (icon == null) return appearance;
        return new ItemAppearance(
            icon.material(),
            appearance.title(),
            appearance.lore(),
            icon.glowing() || appearance.glow(),
            appearance.amount(),
            appearance.customModelData(),
            appearance.headSkin(),
            appearance.potionColor()
        );
    }

    @Override
    public TagResolver placeholders(PetMenuContext context, String sectionId, int itemIndex) {
        Pet pet = context.pet();
        TagResolver.Builder b = TagResolver.builder()
            .resolver(Placeholder.component("pet_name", Util.SANITIZED_MINIMESSAGE.deserialize(pet.getPetName())))
            .resolver(Placeholder.component("pet_type",
                Component.translatable("entity.minecraft." + pet.getPetType().getTypeID())));

        // A "Label: value" lore line can't lead with <lang:Key> — MiniMessage makes
        // the trailing ": value" children of the translatable, which the Locale
        // translator drops. Inject the label as a component placeholder (a leaf
        // translatable the renderer resolves in place) so the value survives.
        String labelKey = switch (sectionId) {
            case "stay" -> "Gui.PetMenu.Stay.Label";
            case "auto-respawn" -> "Gui.PetMenu.AutoRespawn.Label";
            case "pickup" -> "Gui.PetMenu.Pickup.Label";
            case "behavior" -> "Gui.PetMenu.Behavior.Label";
            default -> null;
        };
        if (labelKey != null) {
            b.resolver(Placeholder.component("label", Component.translatable(labelKey)));
        }

        if ("info".equals(sectionId)) {
            String locale = Locale.getPlayerLanguage(context.viewer());
            String hunger = Configuration.HungerSystem.USE_HUNGER_SYSTEM
                ? Locale.renderPlain("Name.Hunger", locale) + ": <gold>" + Math.round(pet.getSaturation())
                : "";
            String healthLine = pet.getRespawnTime() > 0
                ? Locale.renderPlain("Name.Respawntime", locale) + ": <gold>" + pet.getRespawnTime() + "sec"
                : Locale.renderPlain("Name.HP", locale) + ": <gold>" + String.format("%1.2f", pet.getHealth());
            int level = pet.getLevel();
            String progression = level > 0
                ? Locale.renderPlain("Name.Level", locale) + ": <gold>" + level
                : Locale.renderPlain("Name.Exp", locale) + ": <gold>" + String.format("%1.2f", pet.getExp());

            // Per-item title built as an uncolored translatable; the JSON colors it.
            // Deserialize the pet name so its MiniMessage formatting renders.
            Component infoTitle = Component.translatable("Gui.PetMenu.Info.Title",
                Util.SANITIZED_MINIMESSAGE.deserialize(pet.getPetName()));

            b.resolver(Placeholder.component("info_title", infoTitle));
            b.resolver(Placeholder.parsed("pet_hunger_line", hunger));
            b.resolver(Placeholder.parsed("pet_health_line", healthLine));
            b.resolver(Placeholder.parsed("pet_progression_line", progression));
            b.resolver(Placeholder.component("info_type_label",
                Component.translatable("Gui.PetMenu.Info.Lore.Type")));
            b.resolver(Placeholder.component("info_skilltree_label",
                Component.translatable("Gui.PetMenu.Info.Lore.Skilltree")));
            b.resolver(Placeholder.unparsed("pet_skilltree",
                pet.getSkilltree() != null ? pet.getSkilltree().getDisplayName() : "-"));
        }
        if ("behavior".equals(sectionId)) {
            BehaviorImpl behavior = pet.getSkills().get(BehaviorImpl.class);
            String mode = behavior != null ? behavior.getBehavior().name() : "Normal";
            b.resolver(Placeholder.unparsed("behavior_mode", mode));
        }
        if ("skill-menus".equals(sectionId) && itemIndex >= 0) {
            List<SkillMenuEntry> available = availableSkillMenus(context.viewer(), pet);
            if (itemIndex < available.size()) {
                SkillMenuEntry entry = available.get(itemIndex);
                b.resolver(Placeholder.component("skill_menu_title",
                    Component.translatable(entry.titleKey)));
                b.resolver(Placeholder.component("skill_menu_lore",
                    Component.translatable(entry.loreKey)));
            }
        }
        return b.build();
    }

    @Override
    public List<?> templateItems(PetMenuContext context, String sectionId) {
        if (!"skill-menus".equals(sectionId)) return List.of();
        return availableSkillMenus(context.viewer(), context.pet());
    }

    /**
     * Compact the skill-menus region to the actual entry count and slide the
     * fixed skill-toggle / action slots left so they sit flush against the
     * dynamic list. Without this, the JSON-declared 5-wide region leaves empty
     * gaps when the pet has fewer than 5 skill menus.
     */
    @Override
    public MenuDefinition transformDefinition(PetMenuContext context, MenuDefinition base) {
        int count = availableSkillMenus(context.viewer(), context.pet()).size();
        Section listSection = base.sections().get("skill-menus");
        if (!(listSection instanceof PaginatedListSection list)) return base;
        int baseWidth = list.width();
        int shift = baseWidth - count;
        if (shift <= 0) return base;

        Map<String, Section> sections = new LinkedHashMap<>(base.sections());
        if (count == 0) {
            sections.remove("skill-menus");
        } else {
            sections.put("skill-menus", new PaginatedListSection(
                list.id(), list.type(), list.col(), list.row(),
                count, list.height(), list.template(),
                list.previousPageSectionId(), list.nextPageSectionId(),
                list.soundOnPageChange(), list.soundOnTemplateClick()
            ));
        }
        // Only the skill-control slots shift with the dynamic list.
        // stop-attacking and volume are general actions; they stay at their
        // fixed positions on the right side of the row.
        for (String id : List.of("behavior", "pickup")) {
            Section s = sections.get(id);
            if (!(s instanceof SlotSection slot)) continue;
            sections.put(id, new SlotSection(
                slot.id(), slot.type(),
                slot.col() - shift, slot.row(),
                slot.item(), slot.states(), slot.defaultState(),
                slot.soundOnClick(), slot.hideAtBoundary()
            ));
        }
        return new MenuDefinition(
            base.menuId(), base.titleMiniMessage(), base.rows(), base.escSupportsBack(),
            base.soundOnOpen(), base.soundOnClose(), base.soundOnBack(), sections
        );
    }

    @Override
    public ItemAppearance customizeTemplateItem(PetMenuContext context, String sectionId,
                                                int itemIndex, ItemAppearance template) {
        if (!"skill-menus".equals(sectionId)) return template;
        List<SkillMenuEntry> available = availableSkillMenus(context.viewer(), context.pet());
        if (itemIndex < 0 || itemIndex >= available.size()) return template;
        SkillMenuEntry entry = available.get(itemIndex);
        return new ItemAppearance(
            entry.material,
            template.title(),
            template.lore(),
            template.glow(),
            template.amount(),
            template.customModelData(),
            template.headSkin(),
            template.potionColor()
        );
    }

    /**
     * Skill-menu entries that the pet currently has unlocked AND the viewer has
     * permission to open. The list seeds the dynamic "skill-menus" paginated
     * section in the hub.
     */
    private static List<SkillMenuEntry> availableSkillMenus(Player viewer, Pet pet) {
        List<SkillMenuEntry> result = new ArrayList<>(SkillMenuEntry.values().length);
        for (SkillMenuEntry entry : SkillMenuEntry.values()) {
            if (entry.isAvailable(viewer, pet)) result.add(entry);
        }
        return result;
    }

    /**
     * Registry of skill-specific sub-menus exposed in the hub. Adding a new skill
     * with its own GUI is one enum entry plus a {@link #open} dispatch case.
     */
    private enum SkillMenuEntry {
        BACKPACK(BackpackImpl.class, Material.CHEST,
            "Gui.PetMenu.Backpack.Title", "Gui.PetMenu.Backpack.Lore",
            "MyPet.command.inventory", false),
        BEACON(BeaconImpl.class, Material.BEACON,
            "Gui.PetMenu.Beacon.Title", "Gui.PetMenu.Beacon.Lore",
            "MyPet.extended.beacon", true);

        final Class<? extends Skill> skillClass;
        final Material material;
        final String titleKey;
        final String loreKey;
        final String permission;
        final boolean extendedPermission;

        SkillMenuEntry(Class<? extends Skill> skillClass, Material material,
                       String titleKey, String loreKey,
                       String permission, boolean extendedPermission) {
            this.skillClass = skillClass;
            this.material = material;
            this.titleKey = titleKey;
            this.loreKey = loreKey;
            this.permission = permission;
            this.extendedPermission = extendedPermission;
        }

        boolean isAvailable(Player viewer, Pet pet) {
            if (!pet.getSkills().isActive(skillClass)) return false;
            return extendedPermission
                ? Permissions.hasExtended(viewer, permission)
                : viewer.hasPermission(permission);
        }

        @SuppressWarnings("unchecked")
        void open(Player viewer, Pet pet) {
            switch (this) {
                case BACKPACK -> {
                    BackpackImpl bp = pet.getSkills().get(BackpackImpl.class);
                    int rows = bp != null
                        ? Math.max(1, Math.min(6, bp.getRows().getValue().intValue()))
                        : 1;
                    MyPetApi.getGuiService().openMenu(
                        viewer,
                        (MenuId<BackpackContext>) (MenuId<?>) MenuIds.BACKPACK,
                        new BackpackContext(viewer, pet, rows)
                    );
                }
                case BEACON -> {
                    BeaconImpl beacon = pet.getSkills().get(BeaconImpl.class);
                    if (beacon != null) beacon.activate();
                }
            }
        }
    }

    /** Opens a text prompt to rename the pet; reopens the hub on success or cancel. */
    private void openRenameDialog(MenuInstance instance, PetMenuContext ctx) {
        Player viewer = ctx.viewer();
        Pet pet = ctx.pet();
        TextPromptSpec spec = new TextPromptSpec(
            Locale.getComponent("Gui.PetMenu.Rename.Dialog.Title", viewer),
            Locale.getFormattedComponent("Gui.PetMenu.Rename.Dialog.Prompt", viewer, pet.getPetName()),
            // Prefill with the raw name (MiniMessage tags intact) so the player can edit them.
            pet.getPetName(),
            Configuration.Name.MAX_LENGTH
        );
        MyPetApi.getDialogService().promptText(
            viewer, spec,
            newName -> {
                if (newName != null && !newName.isBlank()) {
                    applyRename(viewer, pet, newName);
                }
                reopenHub(viewer, ctx);
            },
            () -> reopenHub(viewer, ctx)
        );
        instance.close();
    }

    /** Mirrors the validation in {@code CommandName.execute} before delegating to {@link Pet#setPetName}. */
    private void applyRename(Player viewer, Pet pet, String name) {
        if (!NameFilter.isClean(name)) {
            viewer.sendMessage(Locale.getComponent("Message.Command.Name.Filter", viewer));
            return;
        }
        if (!Permissions.has(viewer, "MyPet.command.name.color")) {
            name = Util.SANITIZED_MINIMESSAGE.stripTags(name);
        } else {
            Matcher m = MINI_TAG.matcher(name);
            if (m.find()) {
                name = name + "<reset>";
            }
        }
        String stripped = Util.SANITIZED_MINIMESSAGE.stripTags(name);
        if (stripped.length() > Configuration.Name.MAX_LENGTH) {
            viewer.sendMessage(Locale.getFormattedComponent(
                "Message.Command.Name.ToLong", viewer, name, Configuration.Name.MAX_LENGTH));
            return;
        }
        pet.setPetName(name);
        viewer.sendMessage(Locale.getFormattedComponent(
            "Message.Command.Name.New", viewer,
            Permissions.has(viewer, "MyPet.command.name.color") ? name : stripped));
    }

    private static final Pattern MINI_TAG = Pattern.compile("<[a-zA-Z_]+>");

    /** Opens a yes/no prompt to release the pet; releases on confirm, otherwise reopens the hub. */
    private void openReleaseDialog(MenuInstance instance, PetMenuContext ctx) {
        Player viewer = ctx.viewer();
        Pet pet = ctx.pet();
        ConfirmPromptSpec spec = new ConfirmPromptSpec(
            Locale.getFormattedComponent("Gui.PetMenu.Release.Dialog.Title", viewer, pet.getPetName()),
            Locale.getComponent("Gui.PetMenu.Release.Dialog.Message", viewer),
            Locale.getComponent("Gui.PetMenu.Release.Dialog.Yes", viewer),
            Locale.getComponent("Gui.PetMenu.Release.Dialog.No", viewer)
        );
        MyPetApi.getDialogService().promptConfirm(
            viewer, spec,
            () -> releasePet(viewer, pet),
            () -> reopenHub(viewer, ctx)
        );
        instance.close();
    }

    /**
     * Mirrors the release path from {@code CommandRelease.executeWithName} — fires
     * {@link PetRemoveEvent}, drops backpack contents at the pet's location, optionally
     * converts the entity back to vanilla, drops equipment, deactivates and deletes the pet.
     */
    private void releasePet(Player viewer, Pet pet) {
        if (pet.getStatus() == Pet.PetState.Despawned) {
            viewer.sendMessage(Locale.getFormattedComponent("Message.Call.First", viewer, pet.getDisplayName()));
            return;
        }
        if (pet.getStatus() == Pet.PetState.Dead) {
            viewer.sendMessage(Locale.getFormattedComponent(
                "Message.Spawn.Respawn.In", viewer, pet.getDisplayName(), pet.getRespawnTime()));
            return;
        }

        PetRemoveEvent removeEvent = new PetRemoveEvent(pet, PetRemoveEvent.Source.RELEASE);
        Bukkit.getServer().getPluginManager().callEvent(removeEvent);

        if (pet.getSkills().isActive(Backpack.class)) {
            pet.getSkills().get(Backpack.class).getInventory().dropContentAt(pet.getLocation().get());
        }

        boolean entityConverted = false;
        if (!MyPetApi.getPetInfo().getRemoveAfterRelease(pet.getPetType())) {
            try {
                new VanillaMobSpawner().releaseToWild(pet);
                entityConverted = true;
            } catch (Exception e) {
                MyPetApi.getLogger().log(java.util.logging.Level.SEVERE,
                    "Failed to release pet " + pet.getPetName() + " to wild", e);
                viewer.sendMessage(Component.text(
                    "Failed to release your pet: " + e.getMessage()).color(NamedTextColor.RED));
                return;
            }
        }

        if (pet instanceof PetEquipment && !entityConverted) {
            ((PetEquipment) pet).dropEquipment();
        }

        pet.removePet();
        pet.getOwner().setPetForWorldGroup(WorldGroup.getGroupByWorld(viewer.getWorld().getName()), null);

        viewer.sendMessage(Locale.getFormattedComponent(
            "Message.Command.Release.Success", viewer, pet.getDisplayName()));
        MyPetApi.getPetManager().deactivatePet(pet.getOwner(), false);
        MyPetPlugin.getInstance().getRepository().removePet(pet.getUUID());
    }

    @SuppressWarnings("unchecked")
    private void reopenHub(Player viewer, PetMenuContext ctx) {
        MyPetApi.getGuiService().openMenu(
            viewer,
            (MenuId<PetMenuContext>) (MenuId<?>) MenuIds.PET_MENU,
            ctx
        );
    }

    /** Flips a multi-state slot's value and refreshes it. */
    private void refreshStateSlot(MenuInstance instance, String slotId) {
        PetMenuContext ctx = (PetMenuContext) ((MenuInstanceImpl) instance).context();
        Pet pet = ctx.pet();
        String state = switch (slotId) {
            case "stay" -> pet.isSitting() ? "staying" : "following";
            case "auto-respawn" -> pet.getOwner().hasAutoRespawnEnabled() ? "on" : "off";
            case "pickup" -> {
                PickupImpl pickup = pet.getSkills().get(PickupImpl.class);
                yield pickup != null && pickup.isPickupEnabled() ? "on" : "off";
            }
            default -> null;
        };
        if (state != null) instance.setSlotState(slotId, state);
    }

    /** Opens the existing pet-selection menu (Task 11: switch). */
    @SuppressWarnings("unchecked")
    private void openSwitchMenu(Player viewer) {
        if (!MyPetApi.getPlayerManager().isMyPetPlayer(viewer)) return;
        final MyPetPlayer owner = MyPetApi.getPlayerManager().getMyPetPlayer(viewer);

        MyPetPlugin.getInstance().getRepository().getPets(owner)
            .thenAccept(pets -> viewer.getScheduler().run(MyPetApi.getPlugin(), task -> {
                String worldGroup = WorldGroup.getGroupByWorld(viewer.getWorld().getName()).getName();
                final UUID activePetUUID = owner.hasPet() ? owner.getPet().getUUID() : null;
                List<StoredPet> selectablePets = pets.stream()
                    .filter(p -> !p.getWorldGroup().isEmpty() && p.getWorldGroup().equals(worldGroup))
                    .filter(p -> activePetUUID == null || !activePetUUID.equals(p.getUUID()))
                    .collect(Collectors.toCollection(ArrayList::new));

                MyPetApi.getGuiService().openMenu(
                    viewer,
                    (MenuId<PetSelectionContext>) (MenuId<?>) MenuIds.PET_SELECTION,
                    new PetSelectionContext(
                        viewer,
                        () -> CompletableFuture.completedFuture(selectablePets),
                        storedPet -> MyPetApi.getPetManager().activatePet(storedPet)
                            .ifPresent(active -> {
                                if (!owner.isOnline()) return;
                                WorldGroup wg = WorldGroup.getGroupByWorld(viewer.getWorld().getName());
                                owner.setPetForWorldGroup(wg, active.getUUID());
                                active.createEntity();
                                // Return the player to the hub with the newly active pet.
                                MyPetApi.getGuiService().openMenu(
                                    viewer,
                                    (MenuId<PetMenuContext>) (MenuId<?>) MenuIds.PET_MENU,
                                    new PetMenuContext(viewer, active)
                                );
                            }))
                );
            }, null));
    }

    /** Opens the existing backpack menu (Task 11: backpack). */
    @SuppressWarnings("unchecked")
    private void openBackpackMenu(Player viewer, Pet pet) {
        BackpackImpl bp = pet.getSkills().get(BackpackImpl.class);
        if (bp == null) return;
        if (!bp.activate()) return;
        int rows = Math.max(1, Math.min(6, bp.getRows().getValue().intValue()));
        MyPetApi.getGuiService().openMenu(
            viewer,
            (MenuId<BackpackContext>) (MenuId<?>) MenuIds.BACKPACK,
            new BackpackContext(viewer, pet, rows)
        );
    }

    /** Opens the existing choose-skilltree menu (Task 11: choose-skilltree). */
    @SuppressWarnings("unchecked")
    private void openChooseSkilltreeMenu(Player viewer, Pet pet) {
        if (Configuration.Skilltree.AUTOMATIC_SKILLTREE_ASSIGNMENT && !pet.getOwner().isMyPetAdmin()) {
            pet.autoAssignSkilltree();
            viewer.sendMessage(Locale.getComponent(
                "Message.Command.ChooseSkilltree.AutomaticSkilltreeAssignment", pet.getOwner()));
            return;
        }
        List<Skilltree> available = new ArrayList<>();
        for (Skilltree st : MyPetApi.getSkilltreeManager().getOrderedSkilltrees()) {
            if (st.getMobTypes().contains(pet.getPetType()) && st.checkRequirements(pet)) {
                available.add(st);
            }
        }
        if (available.isEmpty()) {
            viewer.sendMessage(Locale.getFormattedComponent(
                "Message.Command.ChooseSkilltree.NoneAvailable", viewer, pet.getDisplayName()));
            return;
        }
        MyPetApi.getGuiService().openMenu(
            viewer,
            (MenuId<ChooseSkilltreeContext>) (MenuId<?>) MenuIds.CHOOSE_SKILLTREE,
            new ChooseSkilltreeContext(viewer, pet, available, chosen -> {
                if (pet.getSkilltree() != null
                    && Configuration.Skilltree.CHOOSE_SKILLTREE_ONLY_ONCE
                    && !pet.getOwner().isMyPetAdmin()) {
                    viewer.sendMessage(Locale.getFormattedComponent(
                        "Message.Command.ChooseSkilltree.OnlyOnce", pet.getOwner(), pet.getDisplayName()));
                    return;
                }
                pet.setSkilltree(chosen, PetSelectSkilltreeEvent.Source.PLAYER_COMMAND);
            })
        );
    }

    /**
     * Stores the active pet (Task 11: store). Mirrors the validation and deactivation
     * path from {@code CommandStore.execute}.
     */
    private void storePet(Player viewer, Pet pet) {
        if (WorldGroup.getGroupByWorld(viewer.getWorld()).isDisabled()) {
            viewer.sendMessage(Locale.getComponent("Message.No.AllowedHere", viewer));
            return;
        }
        if (!viewer.hasPermission("MyPet.command.store")) {
            viewer.sendMessage(Locale.getComponent("Message.No.Allowed", viewer));
            return;
        }
        if (!MyPetApi.getPlayerManager().isMyPetPlayer(viewer)) return;
        final MyPetPlayer owner = MyPetApi.getPlayerManager().getMyPetPlayer(viewer);
        final int maxPetCount = computeMaxPetCount(viewer);
        if (maxPetCount == 0) {
            viewer.sendMessage(Locale.getComponent("Message.No.Allowed", viewer));
            return;
        }
        if (!owner.hasPet()) {
            viewer.sendMessage(Locale.getComponent("Message.Command.Switch.NoPet", viewer));
            return;
        }
        MyPetPlugin.getInstance().getRepository().getPets(owner)
            .thenAccept(pets -> viewer.getScheduler().run(MyPetApi.getPlugin(), task -> {
                if (!owner.hasPet()) {
                    viewer.sendMessage(Locale.getComponent("Message.Command.Switch.NoPet", viewer));
                    return;
                }
                String worldGroup = pet.getWorldGroup();
                int inactiveCount = (int) pets.stream()
                    .filter(p -> p.getWorldGroup().equals(worldGroup))
                    .count() - 1;
                if (inactiveCount >= maxPetCount) {
                    viewer.sendMessage(Locale.getFormattedComponent(
                        "Message.Command.Switch.Limit", viewer, maxPetCount));
                    return;
                }
                if (MyPetApi.getPetManager().deactivatePet(owner, true)) {
                    owner.setPetForWorldGroup(worldGroup, null);
                    viewer.sendMessage(Locale.getFormattedComponent(
                        "Message.Command.Switch.Success", viewer, pet.getDisplayName()));
                }
            }, null));
    }

    private static int computeMaxPetCount(Player viewer) {
        int max = 0;
        if (viewer.hasPermission("MyPet.admin")) {
            max = Configuration.Misc.MAX_STORED_PET_COUNT;
        } else {
            for (int i = Configuration.Misc.MAX_STORED_PET_COUNT; i > 0; i--) {
                if (viewer.hasPermission("MyPet.petstorage.limit." + i)) {
                    max = i;
                    break;
                }
            }
        }
        return max;
    }
}
