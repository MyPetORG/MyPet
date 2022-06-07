/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2020 Keyle
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

package de.Keyle.MyPet.compat.v1_19_R1.entity;

import com.google.common.base.Preconditions;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.compat.ParticleCompat;
import de.Keyle.MyPet.api.entity.*;
import de.Keyle.MyPet.api.entity.ai.AIGoalSelector;
import de.Keyle.MyPet.api.entity.ai.navigation.AbstractNavigation;
import de.Keyle.MyPet.api.entity.ai.target.TargetPriority;
import de.Keyle.MyPet.api.event.MyPetFeedEvent;
import de.Keyle.MyPet.api.event.MyPetInventoryActionEvent;
import de.Keyle.MyPet.api.event.MyPetSitEvent;
import de.Keyle.MyPet.api.player.DonateCheck;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.skill.skills.Ride;
import de.Keyle.MyPet.api.util.ConfigItem;
import de.Keyle.MyPet.api.util.ReflectionUtil;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.Keyle.MyPet.compat.v1_19_R1.PlatformHelper;
import de.Keyle.MyPet.compat.v1_19_R1.entity.ai.attack.MeleeAttack;
import de.Keyle.MyPet.compat.v1_19_R1.entity.ai.attack.RangedAttack;
import de.Keyle.MyPet.compat.v1_19_R1.entity.ai.movement.Float;
import de.Keyle.MyPet.compat.v1_19_R1.entity.ai.movement.*;
import de.Keyle.MyPet.compat.v1_19_R1.entity.ai.navigation.VanillaNavigation;
import de.Keyle.MyPet.compat.v1_19_R1.entity.ai.target.*;
import de.Keyle.MyPet.compat.v1_19_R1.entity.types.EntityMyDolphin;
import de.Keyle.MyPet.compat.v1_19_R1.entity.types.EntityMySeat;
import de.Keyle.MyPet.skill.skills.ControlImpl;
import de.Keyle.MyPet.skill.skills.RideImpl;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_19_R1.attribute.CraftAttributeMap;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_19_R1.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_19_R1.util.CraftChatMessage;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public abstract class EntityMyPet extends Mob implements MyPetMinecraftEntity {

	protected static final EntityDataAccessor<Byte> POTION_PARTICLE_WATCHER = Mob.DATA_SHARED_FLAGS_ID;

	protected AIGoalSelector petPathfinderSelector, petTargetSelector;
	protected LivingEntity target = null;
	protected TargetPriority targetPriority = TargetPriority.None;
	protected double walkSpeed = 0.3F;
	protected boolean hasRider = false;
	protected boolean isMyPet = false;
	protected boolean isFlying = false;
	protected boolean canFly = true;
	protected boolean isInvisible = false;
	protected MyPet myPet;
	protected int jumpDelay = 0;
	protected int idleSoundTimer = 0;
	protected int flyCheckCounter = 0;
	protected int sitCounter = 0;
	protected AbstractNavigation petNavigation;
	protected Sit sitPathfinder;
	protected int donatorParticleCounter = 0;
	protected float limitCounter = 0;
	protected DamageSource deathReason = null;
	protected CraftMyPet bukkitEntity = null;
	protected AttributeMap attributeMap;
	protected boolean indirectRiding = false;

	private static final Field jump = ReflectionUtil.getField(LivingEntity.class, "bn");	//Jumping-Field

	public EntityMyPet(Level world, MyPet myPet) {
		super(((EntityRegistry) MyPetApi.getEntityRegistry()).getEntityType(myPet.getPetType()), world);

		try {
			this.replaceCraftAttributes();

			this.myPet = myPet;
			this.isMyPet = true;
			this.refreshDimensions();
			this.petPathfinderSelector = new AIGoalSelector(0);
			this.petTargetSelector = new AIGoalSelector(Configuration.Entity.SKIP_TARGET_AI_TICKS);
			this.walkSpeed = MyPetApi.getMyPetInfo().getSpeed(myPet.getPetType());
			this.navigation = this.setSpecialNav();
			this.petNavigation = new VanillaNavigation(this);
			this.sitPathfinder = new Sit(this);
			this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(Integer.MAX_VALUE);
			this.setHealth((float) myPet.getHealth());
			this.updateNameTag();
			this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(walkSpeed);
			this.setPathfinder();
			this.updateVisuals();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void replaceCraftAttributes() {
		Field craftAttributesField = ReflectionUtil.getField(LivingEntity.class, "craftAttributes");
		CraftAttributeMap craftAttributes = new CraftAttributeMap(this.getAttributes());
		ReflectionUtil.setFinalFieldValue(craftAttributesField, this, craftAttributes);
}

	protected void initAttributes() {
	}

	@Override
	public boolean isMyPet() {
		return isMyPet;
	}

	@Override
	public void setPathfinder() {
		petPathfinderSelector.addGoal("Float", new Float(this));
		petPathfinderSelector.addGoal("Sit", sitPathfinder);
		petPathfinderSelector.addGoal("Sprint", new Sprint(this, 0.25F));
		petPathfinderSelector.addGoal("RangedTarget", new RangedAttack(this, -0.1F, 12.0F));
		petPathfinderSelector.addGoal("MeleeAttack", new MeleeAttack(this, 0.1F, this.getBbWidth() + 1.3, 20));
		petPathfinderSelector.addGoal("Control", new Control(this, 0.1F));
		petPathfinderSelector.addGoal("FollowOwner", new FollowOwner(this, Configuration.Entity.MYPET_FOLLOW_START_DISTANCE, 2.0F, 16F));
		petPathfinderSelector.addGoal("LookAtPlayer", new LookAtPlayer(this, 8.0F));
		petPathfinderSelector.addGoal("RandomLockaround", new RandomLookaround(this));
		petTargetSelector.addGoal("OwnerHurtByTarget", new OwnerHurtByTarget(this));
		petTargetSelector.addGoal("HurtByTarget", new HurtByTarget(this));
		petTargetSelector.addGoal("ControlTarget", new ControlTarget(this, 1));
		petTargetSelector.addGoal("AggressiveTarget", new BehaviorAggressiveTarget(this, 16));
		petTargetSelector.addGoal("FarmTarget", new BehaviorFarmTarget(this, 16));
		petTargetSelector.addGoal("DuelTarget", new BehaviorDuelTarget(this, 5));
	}

	@Override
	public AttributeMap getAttributes() {
		if (attributeMap == null) {
			EntityRegistry entityRegistry = (EntityRegistry) MyPetApi.getEntityRegistry();
			MyPetType type = entityRegistry.getMyPetType(this.getClass());
			EntityType<?> types = entityRegistry.entityTypes.get(type);
			AttributeSupplier attributeProvider = MyAttributeDefaults.getAttribute(types);
			this.attributeMap = new AttributeMap(attributeProvider);
		}
		return attributeMap;
	}

	public static void setupAttributes(EntityMyPet pet, EntityType<? extends LivingEntity> types) {
		pet.initAttributes();
	}

	@Override
	public AbstractNavigation getPetNavigation() {
		return petNavigation;
	}

	@Override
	public MyPet getMyPet() {
		return myPet;
	}

	@Override
	public boolean hasRider() {
		return isVehicle();
	}

	@Override
	public void setLocation(Location loc) {
		this.setPos(loc.getX(), loc.getY(), loc.getZ());
		this.setRot(loc.getPitch(), loc.getYaw());
	}

	@Override
	public AIGoalSelector getPathfinder() {
		return petPathfinderSelector;
	}

	@Override
	public AIGoalSelector getTargetSelector() {
		return petTargetSelector;
	}

	@Override
	public boolean hasTarget() {
		if (target != null) {
			if (target.isAlive()) {
				return true;
			}
			target = null;
		}
		return false;
	}

	@Override
	public TargetPriority getTargetPriority() {
		return targetPriority;
	}

	@Override
	public org.bukkit.entity.LivingEntity getMyPetTarget() {
		if (target != null) {
			if (target.isAlive()) {
				return (org.bukkit.entity.LivingEntity) target.getBukkitEntity();
			}
			target = null;
		}
		return null;
	}

	@Override
	public void setMyPetTarget(org.bukkit.entity.LivingEntity entity, TargetPriority priority) {
		if (entity == null || entity.isDead() || entity instanceof ArmorStand || !(entity instanceof CraftLivingEntity)) {
			forgetTarget();
			return;
		}
		if (!MyPetApi.getHookHelper().canHurt(myPet.getOwner().getPlayer(), entity)) {
			forgetTarget();
			return;
		}
		if (priority.getPriority() > getTargetPriority().getPriority()) {
			target = ((CraftLivingEntity) entity).getHandle();
		}
	}

	@Override
	public void forgetTarget() {
		target = null;
		targetPriority = TargetPriority.None;
	}

	@Override
	public void setCustomName(Component ignored) {
		updateNameTag();
	}

	@Override
	public void updateNameTag() {
		try {
			if (isCustomNameVisible()) {
				String prefix = Configuration.Name.Tag.PREFIX;
				String suffix = Configuration.Name.Tag.SUFFIX;
				prefix = prefix.replace("<owner>", getOwner().getName());
				prefix = prefix.replace("<level>", "" + getMyPet().getExperience().getLevel());
				suffix = suffix.replace("<owner>", getOwner().getName());
				suffix = suffix.replace("<level>", "" + getMyPet().getExperience().getLevel());
				this.setCustomNameVisible(isCustomNameVisible());
				String name = myPet.getPetName();
				if (!Permissions.has(getOwner(), "MyPet.command.name.color")) {
					name = ChatColor.stripColor(name);
				}
				super.setCustomName(CraftChatMessage.fromStringOrNull(Util.cutString(prefix + name + suffix, 64)));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public Component getCustomName() {
		try {
			return CraftChatMessage.fromStringOrNull(myPet.getPetName());
		} catch (Exception e) {
			return super.getCustomName();
		}
	}

	@Override
	public boolean isCustomNameVisible() {
		return Configuration.Name.Tag.SHOW;
	}

	@Override
	public void setCustomNameVisible(boolean ignored) {
		super.setCustomNameVisible(Configuration.Name.Tag.SHOW);
	}

	public boolean canMove() {
		return !sitPathfinder.isSitting();
	}

	@Override
	public double getWalkSpeed() {
		return walkSpeed;
	}

	public boolean canEat(ItemStack itemstack) {
		List<ConfigItem> foodList = MyPetApi.getMyPetInfo().getFood(myPet.getPetType());
		for (ConfigItem foodItem : foodList) {
			if (foodItem.compare(itemstack)) {
				return true;
			}
		}
		return false;
	}

	public boolean canEquip() {
		return Permissions.hasExtended(getOwner().getPlayer(), "MyPet.extended.equip") && canUseItem();
	}

	public boolean canUseItem() {
		MyPetInventoryActionEvent event = new MyPetInventoryActionEvent(myPet, MyPetInventoryActionEvent.Action.Use);
		Bukkit.getServer().getPluginManager().callEvent(event);
		return !event.isCancelled();
	}

	public boolean playIdleSound() {
		if (getLivingSound() != null) {
			if (idleSoundTimer-- <= 0) {
				idleSoundTimer = 5;
				return true;
			}
		}
		return false;
	}

	@Override
	public void showPotionParticles(Color color) {
		getEntityData().set(POTION_PARTICLE_WATCHER, (byte) color.asRGB());
	}

	@Override
	public void hidePotionParticles() {
		int potionEffects = 0;
		if (!getActiveEffects().isEmpty()) {
			potionEffects = PotionUtils.getColor(getActiveEffects());
		}
		getEntityData().set(POTION_PARTICLE_WATCHER, (byte) potionEffects);
	}

	@Override
	public MyPetPlayer getOwner() {
		return myPet.getOwner();
	}

	/**
	 * Is called when a MyPet attemps to do damge to another entity
	 */
	public boolean attack(Entity entity) {
		boolean damageEntity = false;
		try {
			double damage = isMyPet() ? myPet.getDamage() : 0;
			if (entity instanceof ServerPlayer) {
				Player victim = (Player) entity.getBukkitEntity();
				if (!MyPetApi.getHookHelper().canHurt(myPet.getOwner().getPlayer(), victim, true)) {
					if (myPet.hasTarget()) {
						setTarget(null);
					}
					return false;
				}
			}
			damageEntity = entity.hurt(DamageSource.mobAttack(this), (float) damage);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return damageEntity;
	}

	@Override
	public void updateVisuals() {
	}

	public boolean toggleSitting() {
		MyPetSitEvent sitEvent = new MyPetSitEvent(getMyPet(), isSitting() ? MyPetSitEvent.Action.Follow : MyPetSitEvent.Action.Stay);
		Bukkit.getPluginManager().callEvent(sitEvent);
		if (!sitEvent.isCancelled()) {
			this.sitPathfinder.toggleSitting();
			if (isSitting()) {
				getOwner().sendMessage(Util.formatText(Translation.getString("Message.Sit.Stay", myPet.getOwner()), getMyPet().getPetName()));
			} else {
				getOwner().sendMessage(Util.formatText(Translation.getString("Message.Sit.Follow", myPet.getOwner()), getMyPet().getPetName()));
			}
			sitCounter = 0;
		}
		return !sitEvent.isCancelled();
	}

	@Override
	public void setSitting(boolean sitting) {
		if (isSitting() != sitting) {
			MyPetSitEvent sitEvent = new MyPetSitEvent(getMyPet(), sitting ? MyPetSitEvent.Action.Follow : MyPetSitEvent.Action.Stay);
			Bukkit.getPluginManager().callEvent(sitEvent);
			if (!sitEvent.isCancelled()) {
				this.sitPathfinder.toggleSitting();
				sitCounter = 0;
			}
		}
	}

	@Override
	public boolean isSitting() {
		return this.sitPathfinder.isSitting();
	}

	@Override
	public boolean isPersistenceRequired() {
		return false;
	}

	@Override
	public CraftMyPet getBukkitEntity() {
		if (this.bukkitEntity == null) {
			this.bukkitEntity = new CraftMyPet(this.level.getCraftServer(), this);
		}
		return this.bukkitEntity;
	}

	// Obfuscated Method handler ------------------------------------------------------------------------------------

	/**
	 * Is called when player rightclicks this MyPet
	 * return:
	 * true: there was a reaction on rightclick
	 * false: no reaction on rightclick
	 */
	public InteractionResult handlePlayerInteraction(final net.minecraft.world.entity.player.Player entityhuman, InteractionHand enumhand, final ItemStack itemStack) {
		new BukkitRunnable() {
			@Override
			public void run() {
				ServerPlayer player = ((ServerPlayer) entityhuman);
				if (player.getBukkitEntity().isOnline()) {
					player.initMenu(entityhuman.containerMenu);
				}
			}
		}.runTaskLater(MyPetApi.getPlugin(), 5);
		if (itemStack != null && itemStack.getItem() == Items.LEAD) {
			((ServerPlayer) entityhuman).connection.send(new ClientboundSetEntityLinkPacket(this, null));
		}

		if (enumhand == InteractionHand.OFF_HAND) {
			return InteractionResult.SUCCESS;
		}

		Player owner = this.getOwner().getPlayer();

		if (isMyPet() && myPet.getOwner().equals(entityhuman)) {
			if ((Configuration.Skilltree.Skill.Ride.RIDE_ITEM == null && !canEat(itemStack) && !owner.isSneaking()) ||
					(Configuration.Skilltree.Skill.Ride.RIDE_ITEM != null && Configuration.Skilltree.Skill.Ride.RIDE_ITEM.compare(itemStack))) {
				if (myPet.getSkills().isActive(RideImpl.class) && canMove()) {
					if (Permissions.hasExtended(owner, "MyPet.extended.ride")) {
						((CraftPlayer) owner).getHandle().startRiding(this);
						return InteractionResult.CONSUME;
					} else {
						getOwner().sendMessage(Translation.getString("Message.No.CanUse", myPet.getOwner()), 2000);
					}
				}
			}
			if (Configuration.Skilltree.Skill.CONTROL_ITEM.compare(itemStack)) {
				if (myPet.getSkills().isActive(ControlImpl.class)) {
					return InteractionResult.CONSUME;
				}
			}
			if (itemStack != null) {
				if (itemStack.getItem() == Items.NAME_TAG && itemStack.hasCustomHoverName()) {
					if (Permissions.has(getOwner(), "MyPet.command.name") && Permissions.hasExtended(getOwner(), "MyPet.extended.nametag")) {
						final String name = itemStack.getHoverName().getString();
						getMyPet().setPetName(name);
						EntityMyPet.super.setCustomName(CraftChatMessage.fromStringOrNull("-"));
						myPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Command.Name.New", myPet.getOwner()), name));
						if (!entityhuman.getAbilities().instabuild) {
							itemStack.shrink(1);
						}
						if (itemStack.getCount() <= 0) {
							entityhuman.getInventory().setItem(entityhuman.getInventory().selected, ItemStack.EMPTY);
						}
						new BukkitRunnable() {
							@Override
							public void run() {
								updateNameTag();
							}
						}.runTaskLater(MyPetApi.getPlugin(), 1L);
						return InteractionResult.CONSUME;
					}
				}
				if (canEat(itemStack) && canUseItem()) {
					if (owner != null && !Permissions.hasExtended(owner, "MyPet.extended.feed")) {
						return InteractionResult.CONSUME;
					}
					if (this.petTargetSelector.hasGoal("DuelTarget")) {
						BehaviorDuelTarget duelTarget = (BehaviorDuelTarget) this.petTargetSelector.getGoal("DuelTarget");
						if (duelTarget.getDuelOpponent() != null) {
							return InteractionResult.CONSUME;
						}
					}
					boolean used = false;
					double saturation = Configuration.HungerSystem.HUNGER_SYSTEM_SATURATION_PER_FEED;
					if (saturation > 0) {
						if (myPet.getSaturation() < 100) {
							MyPetFeedEvent feedEvent = new MyPetFeedEvent(getMyPet(), CraftItemStack.asCraftMirror(itemStack), saturation, MyPetFeedEvent.Result.Eat);
							Bukkit.getPluginManager().callEvent(feedEvent);
							if (!feedEvent.isCancelled()) {
								saturation = feedEvent.getSaturation();
								double missingSaturation = 100 - myPet.getSaturation();
								myPet.setSaturation(myPet.getSaturation() + saturation);
								saturation = Math.max(0, saturation - missingSaturation);
								used = true;
							}
						}
					}
					if (saturation > 0) {
						if (getHealth() < myPet.getMaxHealth()) {
							MyPetFeedEvent feedEvent = new MyPetFeedEvent(getMyPet(), CraftItemStack.asCraftMirror(itemStack), saturation, MyPetFeedEvent.Result.Heal);
							Bukkit.getPluginManager().callEvent(feedEvent);
							if (!feedEvent.isCancelled()) {
								saturation = feedEvent.getSaturation();
								double missingHealth = myPet.getMaxHealth() - getHealth();
								this.heal((float) Math.min(saturation, missingHealth), RegainReason.EATING);
								used = true;
							}
						}
					}

					if (used) {
						if (itemStack != ItemStack.EMPTY && !entityhuman.getAbilities().instabuild) {
							itemStack.shrink(1);
							if (itemStack.getCount() <= 0) {
								entityhuman.getInventory().setItem(entityhuman.getInventory().selected, ItemStack.EMPTY);
							}
						}
						MyPetApi.getPlatformHelper().playParticleEffect(myPet.getLocation().get().add(0, getEyeHeight(), 0), ParticleCompat.HEART.get(), 0.5F, 0.5F, 0.5F, 0.5F, 5, 20);

						return InteractionResult.CONSUME;
					}
				}
			}
			if (!owner.isSneaking() && !Configuration.Misc.RIGHT_CLICK_COMMAND.isEmpty()) {
				String command = Configuration.Misc.RIGHT_CLICK_COMMAND;
				command = command.replaceAll("%pet_name%", myPet.getPetName());
				command = command.replaceAll("%pet_owner%", myPet.getOwner().getName());
				command = command.replaceAll("%pet_level%", "" + myPet.getExperience().getLevel());
				command = command.replaceAll("%pet_status%", "" + myPet.getStatus().name());
				command = command.replaceAll("%pet_type%", myPet.getPetType().name());
				command = command.replaceAll("%pet_uuid%", myPet.getUUID().toString());
				command = command.replaceAll("%pet_world_group%", myPet.getWorldGroup());
				command = command.replaceAll("%pet_skilltree_name%", myPet.getSkilltree() != null ? myPet.getSkilltree().getName() : "");
				return owner.performCommand(command) ? InteractionResult.CONSUME : InteractionResult.PASS;
			}
		} else {
			if (itemStack != null) {
				if (itemStack.getItem() == Items.NAME_TAG) {
					if (itemStack.hasCustomHoverName()) {
						EntityMyPet.super.setCustomName(CraftChatMessage.fromStringOrNull("-"));
						new BukkitRunnable() {
							@Override
							public void run() {
								updateNameTag();
							}
						}.runTaskLater(MyPetApi.getPlugin(), 1L);
						return InteractionResult.PASS;
					}
				}
			}
		}
		return InteractionResult.PASS;
	}

	public void onLivingUpdate() {
		if (hasRider) {
			if (!isVehicle()) {
				hasRider = false;
				this.maxUpStep = 0.5F; // climb height -> halfslab
				Location playerLoc = getOwner().getPlayer().getLocation();
				Location petLoc = getBukkitEntity().getLocation();
				petLoc.setYaw(playerLoc.getYaw());
				petLoc.setPitch(playerLoc.getPitch());
				getOwner().getPlayer().teleport(petLoc);
			}
		} else {
			if (isVehicle()) {
				for (Entity e : this.getPassengers()) {
					Entity ridingEntity = (e instanceof EntityMySeat) ? e.getFirstPassenger() : e;
					if (ridingEntity instanceof ServerPlayer && getOwner().equals(ridingEntity)) {
						hasRider = true;
						this.maxUpStep = 1.0F; // climb height -> 1 block
						petTargetSelector.finish();
						petPathfinderSelector.finish();
					} else {
						ridingEntity.stopRiding(); // just the owner can ride a pet
					}
				}
			}
		}
		if (sitPathfinder.isSitting() && sitCounter-- <= 0) {
			MyPetApi.getPlatformHelper().playParticleEffect(getOwner().getPlayer(), this.getBukkitEntity().getLocation().add(0, getEyeHeight() + 1, 0), ParticleCompat.BARRIER.get(), 0F, 0F, 0F, 5F, 1, 32, ParticleCompat.BARRIER_BLOCK_DATA);
			sitCounter = 60;
		}
		Player p = myPet.getOwner().getPlayer();
		if (p != null && p.isOnline() && !p.isDead()) {
			if (p.isSneaking() != isShiftKeyDown()) {
				this.setShiftKeyDown(!isShiftKeyDown());
			}
			if (Configuration.Misc.INVISIBLE_LIKE_OWNER) {
				if (!isInvisible && p.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
					isInvisible = true;
					getBukkitEntity().addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 1, false));
				} else if (isInvisible && !p.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
					getBukkitEntity().removePotionEffect(PotionEffectType.INVISIBILITY);
					isInvisible = false;
				}
			}
			if (!this.isInvisible() && getOwner().getDonationRank() != DonateCheck.DonationRank.None && donatorParticleCounter-- <= 0) {
				donatorParticleCounter = 20 + getRandom().nextInt(10);
				MyPetApi.getPlatformHelper().playParticleEffect(this.getBukkitEntity().getLocation().add(0, 1, 0), ParticleCompat.VILLAGER_HAPPY.get(), 0.4F, 0.4F, 0.4F, 0.4F, 5, 10);
			}
		}
	}

	@Override
	public float getHealth() {
		double health = super.getHealth();
		double maxHealth = myPet.getMaxHealth();
		if (health > maxHealth) {
			setHealth((float) maxHealth);
			health = super.getHealth();
		}
		return (float) health;
	}

	@Override
	public void setHealth(float f) {
		double deltaHealth = super.getHealth();
		double maxHealth = myPet.getMaxHealth();

		boolean silent = this.getAttribute(Attributes.MAX_HEALTH).getValue() != maxHealth;
		this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(maxHealth);

		super.setHealth(Mth.clamp(f, 0.0F, (float) maxHealth));

		double health = super.getHealth();
		if (deltaHealth > maxHealth) {
			deltaHealth = 0;
		} else {
			deltaHealth = health - deltaHealth;
		}

		if (!silent && !Configuration.Misc.DISABLE_ALL_ACTIONBAR_MESSAGES) {
			String msg = myPet.getPetName() + ChatColor.RESET + ": ";
			if (health > maxHealth / 3 * 2) {
				msg += ChatColor.GREEN;
			} else if (health > maxHealth / 3) {
				msg += ChatColor.YELLOW;
			} else {
				msg += ChatColor.RED;
			}
			if (health > 0) {
				msg += String.format("%1.2f", health) + ChatColor.WHITE + "/" + String.format("%1.2f", maxHealth);

				if (!myPet.getOwner().isHealthBarActive()) {
					if (deltaHealth > 0) {
						msg += " (" + ChatColor.GREEN + "+" + String.format("%1.2f", deltaHealth) + ChatColor.RESET + ")";
					} else if (deltaHealth < 0) {
						msg += " (" + ChatColor.RED + String.format("%1.2f", deltaHealth) + ChatColor.RESET + ")";
					}
				}
			} else {
				msg += Translation.getString("Name.Dead", getOwner());
			}

			MyPetApi.getPlatformHelper().sendMessageActionBar(getOwner().getPlayer(), msg);
		}
	}

	@Override
	public void die(DamageSource damagesource) {
		this.deathReason = damagesource;
		super.die(damagesource);
	}

	/**
	 * Returns the speed of played sounds
	 * The faster the higher the sound will be
	 */
	public float getSoundSpeed() {
		float pitchAddition = 0;
		if (getMyPet() instanceof MyPetBaby) {
			if (((MyPetBaby) getMyPet()).isBaby()) {
				pitchAddition += 0.5F;
			}
		}
		return (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1 + pitchAddition;
	}

	/**
	 * Returns the default sound of the MyPet
	 */
	protected abstract String getLivingSound();

	/**
	 * Returns the sound that is played when the MyPet get hurt
	 */
	protected abstract String getHurtSound();

	/**
	 * Returns the sound that is played when the MyPet dies
	 */
	protected abstract String getMyPetDeathSound();

	public void playPetStepSound() {
	}

	public void playMyPetStepSound(BlockPos blockposition, BlockState blockdata) {
		playPetStepSound();
	}

	private void makeLivingSound() {
		if (getLivingSound() != null) {
			SoundEvent se = Registry.SOUND_EVENT.get(new ResourceLocation(getLivingSound()));
			if (se != null) {
				for (int j = 0; j < this.level.players().size(); ++j) {
					ServerPlayer entityplayer = (ServerPlayer) this.level.players().get(j);

					float volume = 1f;
					if (MyPetApi.getPlayerManager().isMyPetPlayer(entityplayer.getBukkitEntity())) {
						volume = MyPetApi.getPlayerManager().getMyPetPlayer(entityplayer.getBukkitEntity()).getPetLivingSoundVolume();
					}

					double deltaX = getX() - entityplayer.getX();
					double deltaY = getY() - entityplayer.getY();
					double deltaZ = getZ() - entityplayer.getZ();
					double maxDistance = volume > 1.0F ? (double) (16.0F * volume) : 16.0D;
					if (deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ < maxDistance * maxDistance) {
						entityplayer.connection.send(new ClientboundSoundPacket(se, SoundSource.HOSTILE, getX(), getY(), getZ(), volume, getSoundSpeed(), 1)); //TODO: check if this works.
					}
				}
			} else {
				MyPetApi.getLogger().warning("Sound \"" + getLivingSound() + "\" not found. Please report this to the developer.");
			}
		}
	}


	/**
	 * returns the first passenger
	 */
	public Entity getFirstPassenger() {
		return this.getPassengers().isEmpty() ? null : this.getPassengers().get(0);
	}

	protected void ride(double motionSideways, double motionForward, double motionUpwards, float speedModifier) {
		double locY;
		float speed;
		float swimmSpeed;

		if (this.isEyeInFluid(FluidTags.WATER)) {
			locY = this.getY();
			speed = 0.8F;
			swimmSpeed = 0.02F;

			this.moveRelative(swimmSpeed, new Vec3(motionSideways, motionUpwards, motionForward));
			this.move(MoverType.SELF, this.getDeltaMovement());
			double motX = this.getDeltaMovement().x() * (double) speed;
			double motY = this.getDeltaMovement().y() * 0.800000011920929D;
			double motZ = this.getDeltaMovement().z() * (double) speed;
			motY -= 0.02D;
			if (this.horizontalCollision && this.isFree(this.getDeltaMovement().x(), this.getDeltaMovement().y() + 0.6000000238418579D - this.getY() + locY, this.getDeltaMovement().z())) {
				motY = 0.30000001192092896D;
			}
			this.setDeltaMovement(motX, motY, motZ);
		} else if (this.isEyeInFluid(FluidTags.LAVA)) {
			locY = this.getY();
			this.moveRelative(0.02F, new Vec3(motionSideways, motionUpwards, motionForward));
			this.move(MoverType.SELF, this.getDeltaMovement());
			double motX = this.getDeltaMovement().x() * 0.5D;
			double motY = this.getDeltaMovement().y() * 0.5D;
			double motZ = this.getDeltaMovement().z() * 0.5D;
			motY -= 0.02D;
			if (this.horizontalCollision && this.isFree(this.getDeltaMovement().x(), this.getDeltaMovement().y() + 0.6000000238418579D - this.getY() + locY, this.getDeltaMovement().z())) {
				motY = 0.30000001192092896D;
			}
			this.setDeltaMovement(motX, motY, motZ);
		} else {
			double minY;
			minY = this.getBoundingBox().minY;

			float friction = 0.91F;
			if (this.onGround) {
				friction = this.level.getBlockState(new BlockPos(Mth.floor(this.getX()), Mth.floor(minY) - 1, Mth.floor(this.getZ()))).getBlock().getFriction() * 0.91F;
			}

			speed = speedModifier * (0.16277136F / (friction * friction * friction));

			this.moveRelative(speed, new Vec3(motionSideways, motionUpwards, motionForward));

			double motX = this.getDeltaMovement().x();
			double motY = this.getDeltaMovement().y();
			double motZ = this.getDeltaMovement().z();

			if (this.onClimbable()) {
				swimmSpeed = 0.16F;
				motX = Mth.clamp(motX, -swimmSpeed, swimmSpeed);
				motZ = Mth.clamp(motZ, -swimmSpeed, swimmSpeed);
				this.flyDist = 0.0F;
				if (motY < -0.16D) {
					motY = -0.16D;
				}
			}

			Vec3 mot = new Vec3(motX, motY, motZ);

			this.move(MoverType.SELF, mot);
			if (this.horizontalCollision && this.onClimbable()) {
				motY = 0.2D;
			}

			motY -= 0.08D;

			motY *= 0.9800000190734863D;
			motX *= friction;
			motZ *= friction;

			this.setDeltaMovement(motX, motY, motZ);
		}
		//           is bird
		this.startRiding(this, false);
	}

	@Override
	public void makeSound(String sound, float volume, float pitch) {
		if (sound != null) {
			SoundEvent se = Registry.SOUND_EVENT.get(new ResourceLocation(sound));
			if (se != null) {
				this.playSound(se, volume, pitch);
			} else {
				MyPetApi.getLogger().warning("Sound \"" + sound + "\" not found. Please report this to the developer.");
			}
		}
	}

	/**
	 * do NOT drop anything
	 */
	@Override
	public boolean shouldDropExperience() {
		return false;
	}

	/**
	 * do NOT drop anything
	 */
	@Override
	protected void dropCustomDeathLoot(DamageSource damagesource, int i, boolean flag) {
	}

	// Obfuscated Methods -------------------------------------------------------------------------------------------

	@Override
	public EntityDimensions getDimensions(Pose entitypose) {
		EntitySize es = this.getClass().getAnnotation(EntitySize.class);
		if (es != null) {
			float width = es.width();
			float height = java.lang.Float.isNaN(es.height()) ? width : es.height();
			return new EntityDimensions(width, height, false);
		}
		return super.getDimensions(entitypose);
	}

	/**
	 * Allows handlePlayerInteraction() to
	 * be fired when a lead is used
	 */
	@Override
	public boolean canBeLeashed(net.minecraft.world.entity.player.Player entityhuman) {
		return false;
	}

	/**
	 * Is called when player rightclicks this MyPet
	 * return:
	 * true: there was a reaction on rightclick
	 * false: no reaction on rightclick
	 * -> handlePlayerInteraction(EntityHuman)
	 */
	@Override
	public InteractionResult mobInteract(net.minecraft.world.entity.player.Player entityhuman, InteractionHand enumhand) {
		try {
			ItemStack itemstack = entityhuman.getItemInHand(enumhand);
			InteractionResult result = handlePlayerInteraction(entityhuman, enumhand, itemstack);
			if (!result.consumesAction() && getMyPet().getOwner().equals(entityhuman) && entityhuman.isShiftKeyDown()) {
				result = InteractionResult.sidedSuccess(toggleSitting());
			}
			return result;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return InteractionResult.PASS;
	}

	/**
	 * -> playStepSound()
	 */
	@Override
	protected void playStepSound(BlockPos blockposition, BlockState iblockdata) {
		try {
			playMyPetStepSound(blockposition, iblockdata);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Returns the sound that is played when the MyPet get hurt
	 * -> getHurtSound()
	 */
	@Override
	protected SoundEvent getHurtSound(DamageSource damagesource) {
		try {
			return Registry.SOUND_EVENT.get(new ResourceLocation(getHurtSound()));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Returns the sound that is played when the MyPet dies
	 */
	@Override
	protected SoundEvent getDeathSound() {
		try {
			return Registry.SOUND_EVENT.get(new ResourceLocation(getMyPetDeathSound()));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Returns the speed of played sounds
	 */
	protected float sp() {
		try {
			return getSoundSpeed();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 1F;
	}

	@Override
	public void aiStep() {
		if (this.jumpDelay > 0) {
			--this.jumpDelay;
		}

		if (this.isAlwaysTicking()) {
			this.lerpX = 0;
			this.syncPacketPositionCodec(this.getX(), this.getY(), this.getZ());
		}
		if (this.lerpX > 0) {
			double newX = this.getX() + (this.useItemRemaining - this.getX()) / this.lerpX;
			double newY = this.getY() + (this.fallFlyTicks - this.getY()) / this.lerpX;
			double newZ = this.getZ() + (this.autoSpinAttackTicks - this.getZ()) / this.lerpX;
			double d3 = Mth.frac(this.rotA - (double) this.getYRot());
			this.setYRot((float) ((double) this.getYRot() + d3 / this.lerpX));
			this.setXRot((float) ((double) this.getXRot() + (this.animationPosition - (double) this.getXRot()) / this.lerpX));
			--this.lerpX;
			this.setPos(newX, newY, newZ);
			this.setRot(this.getYRot(), this.getXRot());
		} else {
			this.setDeltaMovement(this.getDeltaMovement().scale(0.98D));
		}

		Vec3 vec3d = this.getDeltaMovement();
		double motX = vec3d.x();
		double motY = vec3d.y();
		double motZ = vec3d.z();

		if (Math.abs(vec3d.x()) < 0.003D) {
			motX = 0.0D;
		}

		if (Math.abs(vec3d.y()) < 0.003D) {
			motY = 0.0D;
		}

		if (Math.abs(vec3d.z()) < 0.003D) {
			motZ = 0.0D;
		}

		this.setDeltaMovement(motX, motY, motZ);

		this.doMyPetTick();

		if (this.jumping) {
			double d7;

			if (this.isInLava()) {
				d7 = this.getFluidHeight(FluidTags.LAVA);
			} else {
				d7 = this.getFluidHeight(FluidTags.WATER);
			}

			boolean flag = this.isInWater() && d7 > 0.0D;
			double d8 = this.getFluidJumpThreshold();
			if (flag && (!this.onGround || d7 > d8)) {
				this.jumpInLiquid(FluidTags.WATER);
			} else if (this.isInLava() && (!this.onGround || d7 > d8)) {
				this.jumpInLiquid(FluidTags.LAVA);
			} else if ((this.onGround || flag && d7 <= d8) && this.jumpDelay == 0) {
				this.jumpFromGround();
				this.jumpDelay = 10;
			}
		} else {
			this.jumpDelay = 0;
		}

		this.xxa *= 0.98F;
		this.zza *= 0.98F;

		// this.n(); //no Elytra flight
		this.travel(new Vec3(this.xxa, this.yya, this.zza));
		this.pushEntities();
	}

	@Override
	protected boolean addPassenger(Entity entity) {
		boolean returnVal = false;
		// don't allow anything but the owner to ride this entity
		Ride rideSkill = myPet.getSkills().get(RideImpl.class);
		if (rideSkill != null && entity instanceof ServerPlayer && getOwner().equals(entity)) {
			if (entity.getVehicle() != this) {
				throw new IllegalStateException("Use x.startRiding(y), not y.addPassenger(x)");
			} else {
				Preconditions.checkState(!entity.getPassengers().contains(this), "Circular entity riding! %s %s", this, entity);
				boolean cancelled = false;
				if (MyPetApi.getPlatformHelper().isSpigot()) {
					cancelled = MountEventWrapper.callEvent(entity.getBukkitEntity(), this.getBukkitEntity());
				}
				if (cancelled) {
					returnVal = false;
				} else {
					if(this.getMoveControl() instanceof MyPetAquaticMoveControl) {
						this.switchMovement(new MoveControl(this));
					}
					returnVal = mountOwner(entity);
				}
			}

			/* this should not matter anymore but I'm leaving it in here in case it is relevant
			if (this instanceof EntityMyAbstractHorse) {
				double factor = 1;
				if (Configuration.HungerSystem.USE_HUNGER_SYSTEM && Configuration.HungerSystem.AFFECT_RIDE_SPEED) {
					factor = Math.log10(myPet.getSaturation()) / 2;
				}
				getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue((0.22222F * (1F + (rideSkill.getSpeedIncrease().getValue() / 100F))) * factor);
			} */
		}

		if(rideSkill != null && entity instanceof EntityMySeat) {
			if (entity.getVehicle() != this) {
				throw new IllegalStateException("Use x.startRiding(y), not y.addPassenger(x)");
			} else {
				Preconditions.checkState(!entity.getPassengers().contains(this), "Circular entity riding! %s %s", this, entity);
				boolean cancelled = false;
				if (MyPetApi.getPlatformHelper().isSpigot()) {
					cancelled = MountEventWrapper.callEvent(entity.getBukkitEntity(), this.getBukkitEntity());
				}
				if (cancelled) {
					returnVal = false;
				} else {
					returnVal = super.addPassenger(entity);
				}
			}
		}
		return returnVal;
	}

	private static class MountEventWrapper {

		public static boolean callEvent(final CraftEntity player, final CraftMyPet pet) {
			Event event = new org.spigotmc.event.entity.EntityMountEvent(player, pet);
			Bukkit.getPluginManager().callEvent(event);
			return ((Cancellable) event).isCancelled();
		}
	}

	/**
	 * -> unmount(Entity)
	 */
	@Override
	protected boolean removePassenger(Entity entity) {
		boolean result = super.removePassenger(entity);

		if(this.isInWaterOrBubble() && this instanceof EntityMyAquaticPet
				&& !(this.getMoveControl() instanceof MyPetAquaticMoveControl)) {
			this.switchMovement(new MyPetAquaticMoveControl(this));
		}

		PlatformHelper platformHelper = (PlatformHelper) MyPetApi.getPlatformHelper();
		AABB bb = entity.getBoundingBox();
		bb = getBBAtPosition(bb, this.getX(), this.getY(), this.getZ());
		if (!platformHelper.canSpawn(getBukkitEntity().getLocation(), this)) {
			entity.startRiding(this, true);
		} else {
			entity.setPos(getX(), getY(), getZ());
		}
		return result;
	}

	protected AABB getBBAtPosition(AABB bb, double x, double y, double z) {
		double width = bb.getXsize() / 2;
		double height = bb.getYsize();
		double depth = bb.getZsize() / 2;
		return new AABB(x - width, y, z - depth, x + width, y + height, z + width);
	}

	/**
	 * Entity AI tick method
	 * -> updateAITasks()
	 */
	protected void doMyPetTick() {
		try {
			++this.noActionTime;

			if (isAlive()) {
				getSensing().tick(); // sensing

				Player p = getOwner().getPlayer();
				if (p == null || !p.isOnline()) {
					this.discard();
					return;
				}

				if (!hasRider()) {
					petTargetSelector.tick(); // target selector
					petPathfinderSelector.tick(); // pathfinder selector
					petNavigation.tick(); // navigation
				}

				Ride rideSkill = myPet.getSkills().get(RideImpl.class);
				if (this.onGround && rideSkill.getFlyLimit().getValue().doubleValue() > 0) {
					limitCounter += rideSkill.getFlyRegenRate().getValue().doubleValue();
					if (limitCounter > rideSkill.getFlyLimit().getValue().doubleValue()) {
						limitCounter = rideSkill.getFlyLimit().getValue().floatValue();
					}
				}
			}

			customServerAiStep();

			// controls
			getMoveControl().tick(); // move
			getLookControl().tick(); // look
			getJumpControl().tick(); // jump
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * MyPets are not persistant so no data needs to be saved
	 */
	@Override
	public boolean saveAsPassenger(CompoundTag nbttagcompound) {
		return false;
	}

	/**
	 * -> falldamage
	 */
	@Override
	public int calculateFallDamage(float f, float f1) {
		if (!this.isFlying) {
			return super.calculateFallDamage(f, f1);
		}
		return 0;
	}


	/**
	 * -> travel
	 */
	@Override
	public void travel(Vec3 vec3d) {
		if (!hasRider || !this.isVehicle()) {
			super.travel(vec3d);
			return;
		}

		if (this.onGround && this.isFlying) {
			isFlying = false;
			this.flyDist = 0;
		}

		LivingEntity passenger = null;
		if(!indirectRiding) {
			passenger = (LivingEntity) this.getFirstPassenger();
		} else {
			passenger = (LivingEntity) this.getFirstPassenger().getFirstPassenger();
		}
		if(passenger == null) {
			super.travel(vec3d);
			return;
		}

		if (this.isEyeInFluid(FluidTags.WATER) && !this.rideableUnderWater()) {
			this.setDeltaMovement(this.getDeltaMovement().add(0, 0.4, 0));
		}

		Ride rideSkill = myPet.getSkills().get(RideImpl.class);
		if (rideSkill == null || !rideSkill.getActive().getValue()) {
			passenger.stopRiding();
			return;
		}

		//Rotations are fun
		if (indirectRiding) {
			if (this.getFirstPassenger() instanceof EntityMySeat seat) {
				seat.yRotO = this.getYRot();
				seat.setYRot(passenger.getYRot());
				seat.setXRot(passenger.getXRot() * 0.5F);
				seat.yHeadRot = (this.yBodyRot = this.getYRot());
			}
		}

		//apply pitch & yaw
		this.yRotO = this.getYRot();
		this.setYRot(passenger.getYRot());
		this.setXRot(passenger.getXRot() * 0.5F);
		this.setRot(this.getYRot(), this.getXRot());
		this.yHeadRot = (this.yBodyRot = this.getYRot());

		// get motion from passenger (player)
		double motionSideways = passenger.xxa;
		double motionForward = passenger.zza * 2;

		// backwards is slower
		if (motionForward <= 0.0F) {
			motionForward *= 0.5F;
		}

		float speed = 0.22222F * (1F + (rideSkill.getSpeedIncrease().getValue() / 100F));
		double jumpHeight = Util.clamp(1 + rideSkill.getJumpHeight().getValue().doubleValue(), 0, 10);
		float ascendSpeed = 0.2f;

		if (Configuration.HungerSystem.USE_HUNGER_SYSTEM && Configuration.HungerSystem.AFFECT_RIDE_SPEED) {
			double factor = Math.log10(myPet.getSaturation()) / 2;
			speed *= factor;
			jumpHeight *= factor;
			ascendSpeed *= factor;
		}

		ride(motionSideways, motionForward, vec3d.y(), speed); // apply motion

		// throw player move event
		if (Configuration.Misc.THROW_PLAYER_MOVE_EVENT_WHILE_RIDING) {
			double delta = Math.pow(this.getX() - this.xo, 2.0D) + Math.pow(this.getY() - this.yo, 2.0D) + Math.pow(this.getZ() - this.zo, 2.0D);
			float deltaAngle = Math.abs(this.getYRot() - yRotO) + Math.abs(this.getXRot() - xRotO);
			if (delta > 0.00390625D || deltaAngle > 10.0F) {
				Location to = getBukkitEntity().getLocation();
				Location from = new Location(level.getWorld(), this.xo, this.yo, this.zo, this.yRotO, this.xRotO);
				if (from.getX() != Double.MAX_VALUE) {
					Location oldTo = to.clone();
					PlayerMoveEvent event = new PlayerMoveEvent((Player) passenger.getBukkitEntity(), from, to);
					Bukkit.getPluginManager().callEvent(event);
					if (event.isCancelled()) {
						passenger.getBukkitEntity().teleport(from);
						return;
					}
					if ((!oldTo.equals(event.getTo())) && (!event.isCancelled())) {
						passenger.getBukkitEntity().teleport(event.getTo(), PlayerTeleportEvent.TeleportCause.UNKNOWN);
						return;
					}
				}
			}
		}

		if (jump != null && this.isVehicle()) {
			boolean doJump = false;
			if (jump != null) {
				try {
					doJump = jump.getBoolean(passenger);
				} catch (IllegalAccessException ignored) {
				}
			}

			if (doJump) {
				if (onGround) {
					jumpHeight = new BigDecimal(jumpHeight).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue();
					String jumpHeightString = JumpHelper.JUMP_FORMAT.format(jumpHeight);
					Double jumpVelocity = JumpHelper.JUMP_MAP.get(jumpHeightString);
					jumpVelocity = jumpVelocity == null ? 0.44161199999510264 : jumpVelocity;
					this.setDeltaMovement(this.getDeltaMovement().x(), jumpVelocity, this.getDeltaMovement().z());
				} else if (rideSkill.getCanFly().getValue()) {
					if (limitCounter <= 0 && rideSkill.getFlyLimit().getValue().doubleValue() > 0) {
						canFly = false;
					} else if (flyCheckCounter-- <= 0) {
						canFly = MyPetApi.getHookHelper().canMyPetFlyAt(getBukkitEntity().getLocation());
						if (canFly && !Permissions.hasExtended(getOwner().getPlayer(), "MyPet.extended.ride.fly")) {
							canFly = false;
						}
						flyCheckCounter = 5;
					}
					if (canFly) {
						if (this.getDeltaMovement().y() < ascendSpeed) {
							this.setDeltaMovement(this.getDeltaMovement().x(), ascendSpeed, this.getDeltaMovement().z());
							this.flyDist = 0;
							this.isFlying = true;
						}
					}
				} else if (this.rideableUnderWater() && this.isInWaterOrBubble()) {
					if (this.getDeltaMovement().y() < ascendSpeed) {
						this.setDeltaMovement(this.getDeltaMovement().x(), ascendSpeed, this.getDeltaMovement().z());
					}
				} else if (this instanceof EntityMyDolphin
						&& this.level.getBlockState(this.getBlockPosBelowThatAffectsMyMovement()).getMaterial().isLiquid()
						&& ((EntityMyDolphin)this).canDolphinjump) {
					this.setDeltaMovement(this.getDeltaMovement().x(), ascendSpeed*4, this.getDeltaMovement().z());
					((EntityMyDolphin)this).canDolphinjump = false;
				}
			} else {
				flyCheckCounter = 0;
			}
		}

		if (Configuration.HungerSystem.USE_HUNGER_SYSTEM && Configuration.Skilltree.Skill.Ride.HUNGER_PER_METER > 0) {
			double dX = getX() - xo;
			double dY = Math.max(0, getY() - yo);
			double dZ = getZ() - zo;
			if (dX != 0 || dY != 0 || dZ != 0) {
				double distance = Math.sqrt(dX * dX + dY * dY + dZ * dZ);
				if (isFlying && rideSkill.getFlyLimit().getValue().doubleValue() > 0) {
					limitCounter -= distance;
				}
				myPet.decreaseSaturation(Configuration.Skilltree.Skill.Ride.HUNGER_PER_METER * distance);
				double factor = Math.log10(myPet.getSaturation()) / 2;
				getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue((0.22222F * (1F + (rideSkill.getSpeedIncrease().getValue() / 100F))) * factor);
				this.setSpeed((float) this.getAttribute(Attributes.MOVEMENT_SPEED).getValue());
			}
		}
	}

	/**
	 * -> onLivingUpdate()
	 */
	@Override
	public void tick() {
		super.tick();
		try {
			onLivingUpdate();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Returns the default sound of the MyPet
	 * -> getLivingSound()
	 */
	@Override
	protected SoundEvent getAmbientSound() {
		try {
			if (getLivingSound() != null && playIdleSound()) {
				makeLivingSound();
			}
			return null;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public void lavaHurt() {
		super.lavaHurt();
	}

	@Override
	public DamageSource getLastDamageSource() {
		if (deathReason != null) {
			return deathReason;
		}
		return super.getLastDamageSource();
	}

	@Override
	public UUID getUniqueID() {
		return this.uuid;
	}

	public boolean floatsInLava() {	//Some entities do - now they can
		return false;
	}

	/**
	 * Used for abnormal floating-behaviour
	 *
	 * @return true if it handled the floating - false if normal floating can be used
	 */

	public boolean specialFloat() { //Some entities do strange stuff in Water/Lava - this enables that
		return false;
	}

	public void switchMovement(MoveControl mvcontrol) {	//This is for switching between Movesets
		this.moveControl = mvcontrol;
	}

	protected PathNavigation setSpecialNav() { //Some Pets have special PathNavigations
		return this.navigation;
	}

	public boolean mountOwner(Entity owner) {
		ejectPassengers();
		if (owner != null) {
			if (!indirectRiding) {
				return super.addPassenger(owner);
			} else {
				return EntityMySeat.mountToPet(owner, this);
			}
		}
		return false;
	}
}
