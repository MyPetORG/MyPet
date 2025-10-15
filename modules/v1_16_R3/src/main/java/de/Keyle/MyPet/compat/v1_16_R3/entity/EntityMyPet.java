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

package de.Keyle.MyPet.compat.v1_16_R3.entity;

import com.google.common.base.Preconditions;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.compat.ParticleCompat;
import de.Keyle.MyPet.api.entity.EntitySize;
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
import de.Keyle.MyPet.compat.v1_16_R3.PlatformHelper;
import de.Keyle.MyPet.compat.v1_16_R3.entity.ai.attack.MeleeAttack;
import de.Keyle.MyPet.compat.v1_16_R3.entity.ai.attack.RangedAttack;
import de.Keyle.MyPet.compat.v1_16_R3.entity.ai.movement.Float;
import de.Keyle.MyPet.compat.v1_16_R3.entity.ai.movement.*;
import de.Keyle.MyPet.compat.v1_16_R3.entity.ai.navigation.VanillaNavigation;
import de.Keyle.MyPet.compat.v1_16_R3.entity.ai.target.*;
import de.Keyle.MyPet.compat.v1_16_R3.entity.types.EntityMySeat;
import de.Keyle.MyPet.skill.skills.ControlImpl;
import de.Keyle.MyPet.skill.skills.RideImpl;
import net.minecraft.server.v1_16_R3.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_16_R3.attribute.CraftAttributeMap;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_16_R3.event.CraftEventFactory;
import org.bukkit.craftbukkit.v1_16_R3.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_16_R3.util.CraftChatMessage;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
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
import java.math.RoundingMode;
import java.util.List;

public abstract class EntityMyPet extends EntityInsentient implements MyPetMinecraftEntity {

	protected static final DataWatcherObject<Byte> POTION_PARTICLE_WATCHER = EntityInsentient.ag;

	protected AIGoalSelector petPathfinderSelector, petTargetSelector;
	protected EntityLiving target = null;
	protected int interactCooldown = 0;
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
	protected AttributeMapBase attributeMap;
	protected boolean indirectRiding = false;

	private static final Field jump = ReflectionUtil.getField(EntityLiving.class, "jumping");

	public EntityMyPet(World world, MyPet myPet) {
		super(((EntityRegistry) MyPetApi.getEntityRegistry()).getEntityType(myPet.getPetType()), world);

		try {
			this.replaceCraftAttributes();

			this.myPet = myPet;
			this.isMyPet = true;
			this.updateSize();
			this.petPathfinderSelector = new AIGoalSelector(0);
			this.petTargetSelector = new AIGoalSelector(Configuration.Entity.SKIP_TARGET_AI_TICKS);
			this.walkSpeed = MyPetApi.getMyPetInfo().getSpeed(myPet.getPetType());
			this.petNavigation = new VanillaNavigation(this);
			this.sitPathfinder = new Sit(this);
			this.getAttributeInstance(GenericAttributes.MAX_HEALTH).setValue(Integer.MAX_VALUE);
			this.setHealth((float) myPet.getHealth());
			this.updateNameTag();
			this.getAttributeInstance(GenericAttributes.MOVEMENT_SPEED).setValue(walkSpeed);
			this.setPathfinder();
			this.updateVisuals();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void replaceCraftAttributes() {
		Field craftAttributesField = ReflectionUtil.getField(EntityLiving.class, "craftAttributes");
		CraftAttributeMap craftAttributes = new CraftAttributeMap(this.getAttributeMap());
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
		petPathfinderSelector.addGoal("MeleeAttack", new MeleeAttack(this, 0.1F, this.getWidth() + 1.3, 20));
		petPathfinderSelector.addGoal("Control", new Control(this, 0.1F));
		petPathfinderSelector.addGoal("FollowOwner", new FollowOwner(this, Configuration.Entity.MYPET_FOLLOW_START_DISTANCE, 2.0F, 16F));
		petPathfinderSelector.addGoal("LookAtPlayer", new LookAtPlayer(this, 8.0F));
		petPathfinderSelector.addGoal("RandomLookaround", new RandomLookaround(this));
		petTargetSelector.addGoal("OwnerHurtByTarget", new OwnerHurtByTarget(this));
		petTargetSelector.addGoal("HurtByTarget", new HurtByTarget(this));
		petTargetSelector.addGoal("ControlTarget", new ControlTarget(this, 1));
		petTargetSelector.addGoal("AggressiveTarget", new BehaviorAggressiveTarget(this, 16));
		petTargetSelector.addGoal("FarmTarget", new BehaviorFarmTarget(this, 16));
		petTargetSelector.addGoal("DuelTarget", new BehaviorDuelTarget(this, 5));
	}

	@Override
	public AttributeMapBase getAttributeMap() {
		if (attributeMap == null) {
			EntityRegistry entityRegistry = (EntityRegistry) MyPetApi.getEntityRegistry();
			MyPetType type = entityRegistry.getMyPetType(this.getClass());
			EntityTypes<?> types = entityRegistry.entityTypes.get(type);
			AttributeProvider attributeProvider = MyAttributeDefaults.getAttribute(types);
			this.attributeMap = new AttributeMapBase(attributeProvider);
		}
		return attributeMap;
	}

	public static void setupAttributes(EntityMyPet pet, EntityTypes<? extends EntityLiving> types) {
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
		this.setLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getPitch(), loc.getYaw());
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
	public LivingEntity getMyPetTarget() {
		if (target != null) {
			if (target.isAlive()) {
				return (LivingEntity) target.getBukkitEntity();
			}
			target = null;
		}
		return null;
	}

	@Override
	public void setMyPetTarget(LivingEntity entity, TargetPriority priority) {
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
	public void setCustomName(IChatBaseComponent ignored) {
		updateNameTag();
	}

	@Override
	public void updateNameTag() {
		try {
			if (getCustomNameVisible()) {
				String prefix = Configuration.Name.Tag.PREFIX;
				String suffix = Configuration.Name.Tag.SUFFIX;
				prefix = prefix.replace("<owner>", getOwner().getName());
				prefix = prefix.replace("<level>", "" + getMyPet().getExperience().getLevel());
				suffix = suffix.replace("<owner>", getOwner().getName());
				suffix = suffix.replace("<level>", "" + getMyPet().getExperience().getLevel());
				this.setCustomNameVisible(getCustomNameVisible());
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
	public IChatBaseComponent getCustomName() {
		try {
			return CraftChatMessage.fromStringOrNull(myPet.getPetName());
		} catch (Exception e) {
			return super.getCustomName();
		}
	}

	@Override
	public boolean getCustomNameVisible() {
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
		getDataWatcher().set(POTION_PARTICLE_WATCHER, (byte) color.asRGB());
	}

	@Override
	public void hidePotionParticles() {
		int potionEffects = 0;
		if (!effects.isEmpty()) {
			potionEffects = PotionUtil.a(effects.values());
		}
		getDataWatcher().set(POTION_PARTICLE_WATCHER, (byte) potionEffects);
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
			if (entity instanceof EntityPlayer) {
				Player victim = (Player) entity.getBukkitEntity();
				if (!MyPetApi.getHookHelper().canHurt(myPet.getOwner().getPlayer(), victim, true)) {
					if (myPet.hasTarget()) {
						setGoalTarget(null);
					}
					return false;
				}
			}
			damageEntity = entity.damageEntity(DamageSource.mobAttack(this), (float) damage);
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
	public boolean isPersistent() {
		return false;
	}

	@Override
	public CraftMyPet getBukkitEntity() {
		if (this.bukkitEntity == null) {
			this.bukkitEntity = new CraftMyPet(this.world.getServer(), this);
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
	public EnumInteractionResult handlePlayerInteraction(final EntityHuman entityhuman, EnumHand enumhand, final ItemStack itemStack) {
		new BukkitRunnable() {
			@Override
			public void run() {
				EntityPlayer player = ((EntityPlayer) entityhuman);
				if (player.getBukkitEntity().isOnline()) {
					player.updateInventory(entityhuman.defaultContainer);
				}
			}
		}.runTaskLater(MyPetApi.getPlugin(), 5);
		if (itemStack != null && itemStack.getItem() == Items.LEAD) {
			((EntityPlayer) entityhuman).playerConnection.sendPacket(new PacketPlayOutAttachEntity(this, null));
		}

		if (enumhand == EnumHand.OFF_HAND) {
			return EnumInteractionResult.SUCCESS;
		}

		Player owner = this.getOwner().getPlayer();

		if (isMyPet() && myPet.getOwner().equals(entityhuman)) {
			if ((Configuration.Skilltree.Skill.Ride.RIDE_ITEM == null && !canEat(itemStack) && !owner.isSneaking()) ||
					(Configuration.Skilltree.Skill.Ride.RIDE_ITEM != null && Configuration.Skilltree.Skill.Ride.RIDE_ITEM.compare(itemStack))) {
				if (myPet.getSkills().isActive(RideImpl.class) && canMove()) {
					if (Permissions.hasExtended(owner, "MyPet.extended.ride")) {
						((CraftPlayer) owner).getHandle().startRiding(this);
						return EnumInteractionResult.CONSUME;
					} else {
						getOwner().sendMessage(Translation.getString("Message.No.CanUse", myPet.getOwner()), 2000);
					}
				}
			}
			if (Configuration.Skilltree.Skill.CONTROL_ITEM.compare(itemStack)) {
				if (myPet.getSkills().isActive(ControlImpl.class)) {
					return EnumInteractionResult.CONSUME;
				}
			}
			if (itemStack != null) {
				if (itemStack.getItem() == Items.NAME_TAG && itemStack.hasName()) {
					if (Permissions.has(getOwner(), "MyPet.command.name") && Permissions.hasExtended(getOwner(), "MyPet.extended.nametag")) {
						final String name = itemStack.getName().getString();
						getMyPet().setPetName(name);
						EntityMyPet.super.setCustomName(CraftChatMessage.fromStringOrNull("-"));
						myPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Command.Name.New", myPet.getOwner()), name));
						if (!entityhuman.abilities.canInstantlyBuild) {
							itemStack.subtract(1);
						}
						if (itemStack.getCount() <= 0) {
							entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, ItemStack.b);
						}
						new BukkitRunnable() {
							@Override
							public void run() {
								updateNameTag();
							}
						}.runTaskLater(MyPetApi.getPlugin(), 1L);
						return EnumInteractionResult.CONSUME;
					}
				}
				if (canEat(itemStack) && canUseItem()) {
					if (owner != null && !Permissions.hasExtended(owner, "MyPet.extended.feed")) {
						return EnumInteractionResult.CONSUME;
					}
					if (this.petTargetSelector.hasGoal("DuelTarget")) {
						BehaviorDuelTarget duelTarget = (BehaviorDuelTarget) this.petTargetSelector.getGoal("DuelTarget");
						if (duelTarget.getDuelOpponent() != null) {
							return EnumInteractionResult.CONSUME;
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
						if (itemStack != ItemStack.b && !entityhuman.abilities.canInstantlyBuild) {
							itemStack.subtract(1);
							if (itemStack.getCount() <= 0) {
								entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, ItemStack.b);
							}
						}
						MyPetApi.getPlatformHelper().playParticleEffect(myPet.getLocation().get().add(0, getHeadHeight(), 0), ParticleCompat.HEART.get(), 0.5F, 0.5F, 0.5F, 0.5F, 5, 20);

						return EnumInteractionResult.CONSUME;
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
				return owner.performCommand(command) ? EnumInteractionResult.CONSUME : EnumInteractionResult.PASS;
			}
		} else {
			if (itemStack != null) {
				if (itemStack.getItem() == Items.NAME_TAG) {
					if (itemStack.hasName()) {
						EntityMyPet.super.setCustomName(CraftChatMessage.fromStringOrNull("-"));
						new BukkitRunnable() {
							@Override
							public void run() {
								updateNameTag();
							}
						}.runTaskLater(MyPetApi.getPlugin(), 1L);
						return EnumInteractionResult.PASS;
					}
				}
			}
		}
		return EnumInteractionResult.PASS;
	}

	public void onLivingUpdate() {
		if (hasRider) {
			if (!isVehicle()) {
				hasRider = false;
				this.G = 0.5F; // climb height -> halfslab
				Location playerLoc = getOwner().getPlayer().getLocation();
				Location petLoc = getBukkitEntity().getLocation();
				petLoc.setYaw(playerLoc.getYaw());
				petLoc.setPitch(playerLoc.getPitch());
			}
		} else {
			if (isVehicle()) {
				for (Entity e : passengers) {
					Entity ridingEntity = (e instanceof EntityMySeat) ? e.getPassengers().get(0) : e;
					if (ridingEntity instanceof EntityPlayer && getOwner().equals(ridingEntity)) {
						hasRider = true;
						this.G = 1.0F; // climb height -> 1 block
						petTargetSelector.finish();
						petPathfinderSelector.finish();
					} else {
						ridingEntity.stopRiding(); // just the owner can ride a pet
					}
				}
			}
		}
		if (sitPathfinder.isSitting() && sitCounter-- <= 0) {
			MyPetApi.getPlatformHelper().playParticleEffect(getOwner().getPlayer(), this.getBukkitEntity().getLocation().add(0, getHeadHeight() + 1, 0), ParticleCompat.BARRIER.get(), 0F, 0F, 0F, 5F, 1, 32);
			sitCounter = 60;
		}
		Player p = myPet.getOwner().getPlayer();
		if (p != null && p.isOnline() && !p.isDead()) {
			if (p.isSneaking() != isSneaking()) {
				this.setSneaking(!isSneaking());
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
		double health = this.datawatcher.get(HEALTH);
		double maxHealth = myPet.getMaxHealth();
		if (health > maxHealth) {
			setHealth((float) maxHealth);
			health = this.datawatcher.get(HEALTH);
		}
		return (float) health;
	}

	@Override
	public void setHealth(float f) {
		double deltaHealth = this.datawatcher.get(HEALTH);
		double maxHealth = myPet.getMaxHealth();

		boolean silent = this.getAttributeInstance(GenericAttributes.MAX_HEALTH).getValue() != maxHealth;
		this.getAttributeInstance(GenericAttributes.MAX_HEALTH).setValue(maxHealth);

		this.datawatcher.set(HEALTH, MathHelper.a(f, 0.0F, (float) maxHealth));

		double health = this.datawatcher.get(HEALTH);
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
	protected abstract String getDeathSound();

	public void playPetStepSound() {
	}

	public void playStepSound(BlockPosition blockposition, IBlockData blockdata) {
		playPetStepSound();
	}

	private void makeLivingSound() {
		if (getLivingSound() != null) {
			SoundEffect se = IRegistry.SOUND_EVENT.get(new MinecraftKey(getLivingSound()));
			if (se != null) {
				for (int j = 0; j < this.world.getPlayers().size(); ++j) {
					EntityPlayer entityplayer = (EntityPlayer) this.world.getPlayers().get(j);

					float volume = 1f;
					if (MyPetApi.getPlayerManager().isMyPetPlayer(entityplayer.getBukkitEntity())) {
						volume = MyPetApi.getPlayerManager().getMyPetPlayer(entityplayer.getBukkitEntity()).getPetLivingSoundVolume();
					}

					double deltaX = locX() - entityplayer.locX();
					double deltaY = locY() - entityplayer.locY();
					double deltaZ = locZ() - entityplayer.locZ();
					double maxDistance = volume > 1.0F ? (double) (16.0F * volume) : 16.0D;
					if (deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ < maxDistance * maxDistance) {
						entityplayer.playerConnection.sendPacket(new PacketPlayOutNamedSoundEffect(se, SoundCategory.HOSTILE, locX(), locY(), locZ(), volume, getSoundSpeed()));
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

	private void ride(double motionSideways, double motionForward, double motionUpwards, float speedModifier) {
		double locY;
		float f2;
		float speed;
		float swimmSpeed;

		if (this.a(TagsFluid.WATER)) {
			locY = this.locY();
			speed = 0.8F;
			swimmSpeed = 0.02F;

			this.a(swimmSpeed, new Vec3D(motionSideways, motionUpwards, motionForward));
			this.move(EnumMoveType.SELF, this.getMot());
			double motX = this.getMot().x * (double) speed;
			double motY = this.getMot().y * 0.800000011920929D;
			double motZ = this.getMot().z * (double) speed;
			motY -= 0.02D;
			if (this.positionChanged && this.e(this.getMot().x, this.getMot().y + 0.6000000238418579D - this.locY() + locY, this.getMot().z)) {
				motY = 0.30000001192092896D;
			}
			this.setMot(motX, motY, motZ);
		} else if (this.a(TagsFluid.LAVA)) {
			locY = this.locY();
			this.a(0.02F, new Vec3D(motionSideways, motionUpwards, motionForward));
			this.move(EnumMoveType.SELF, this.getMot());
			double motX = this.getMot().x * 0.5D;
			double motY = this.getMot().y * 0.5D;
			double motZ = this.getMot().z * 0.5D;
			motY -= 0.02D;
			if (this.positionChanged && this.e(this.getMot().x, this.getMot().y + 0.6000000238418579D - this.locY() + locY, this.getMot().z)) {
				motY = 0.30000001192092896D;
			}
			this.setMot(motX, motY, motZ);
		} else {
			double minY;
			minY = this.getBoundingBox().minY;

			float friction = 0.91F;
			if (this.onGround) {
				friction = this.world.getType(new BlockPosition(MathHelper.floor(this.locX()), MathHelper.floor(minY) - 1, MathHelper.floor(this.locZ()))).getBlock().getFrictionFactor() * 0.91F;
			}

			speed = speedModifier * (0.16277136F / (friction * friction * friction));

			this.a(speed, new Vec3D(motionSideways, motionUpwards, motionForward));
			friction = 0.91F;
			if (this.onGround) {
				friction = this.world.getType(new BlockPosition(MathHelper.floor(this.locX()), MathHelper.floor(minY) - 1, MathHelper.floor(this.locZ()))).getBlock().getFrictionFactor() * 0.91F;
			}

			double motX = this.getMot().x;
			double motY = this.getMot().y;
			double motZ = this.getMot().z;

			if (this.isClimbing()) {
				swimmSpeed = 0.16F;
				motX = MathHelper.a(motX, -swimmSpeed, swimmSpeed);
				motZ = MathHelper.a(motZ, -swimmSpeed, swimmSpeed);
				this.fallDistance = 0.0F;
				if (motY < -0.16D) {
					motY = -0.16D;
				}
			}

			Vec3D mot = new Vec3D(motX, motY, motZ);

			this.move(EnumMoveType.SELF, mot);
			if (this.positionChanged && this.isClimbing()) {
				motY = 0.2D;
			}

			motY -= 0.08D;

			motY *= 0.9800000190734863D;
			motX *= friction;
			motZ *= friction;

			this.setMot(motX, motY, motZ);
		}
		//           is bird
		this.a(this, false);
	}

	@Override
	public void makeSound(String sound, float volume, float pitch) {
		if (sound != null) {
			SoundEffect se = IRegistry.SOUND_EVENT.get(new MinecraftKey(sound));
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
	protected boolean isDropExperience() {
		return false;
	}

	/**
	 * do NOT drop anything
	 */
	@Override
	protected void dropDeathLoot(DamageSource damagesource, int i, boolean flag) {
	}

	// Obfuscated Methods -------------------------------------------------------------------------------------------

	@Override
	public net.minecraft.server.v1_16_R3.EntitySize a(EntityPose entitypose) {
		EntitySize es = this.getClass().getAnnotation(EntitySize.class);
		if (es != null) {
			float width = es.width();
			float height = java.lang.Float.isNaN(es.height()) ? width : es.height();
			return new net.minecraft.server.v1_16_R3.EntitySize(width, height, false);
		}
		return super.a(entitypose);
	}

	/**
	 * Allows handlePlayerInteraction() to
	 * be fired when a lead is used
	 */
	@Override
	public boolean a(EntityHuman entityhuman) {
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
	public EnumInteractionResult b(EntityHuman entityhuman, EnumHand enumhand) {
		if(checkInteractCooldown()) {
			return EnumInteractionResult.FAIL;
		}

		try {
			ItemStack itemstack = entityhuman.b(enumhand);
			EnumInteractionResult result = handlePlayerInteraction(entityhuman, enumhand, itemstack);
			if (!result.a() && getMyPet().getOwner().equals(entityhuman) && entityhuman.isSneaking()) {
				result = EnumInteractionResult.a(toggleSitting());
			}
			return result;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return EnumInteractionResult.FAIL;
	}
	protected boolean checkInteractCooldown() {
		return (interactCooldown>0);
	}

	/**
	 * -> playStepSound()
	 */
	@Override
	protected void b(BlockPosition blockposition, IBlockData iblockdata) {
		try {
			playStepSound(blockposition, iblockdata);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Returns the sound that is played when the MyPet get hurt
	 * -> getHurtSound()
	 */
	@Override
	protected SoundEffect getSoundHurt(DamageSource damagesource) {
		try {
			return IRegistry.SOUND_EVENT.get(new MinecraftKey(getHurtSound()));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Returns the sound that is played when the MyPet dies
	 * -> getDeathSound()
	 */
	@Override
	protected SoundEffect getSoundDeath() {
		try {
			return IRegistry.SOUND_EVENT.get(new MinecraftKey(getDeathSound()));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Returns the speed of played sounds
	 */
	protected float dn() {
		try {
			return getSoundSpeed();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 1F;
	}

	@Override
	public void movementTick() {
		if (this.jumpDelay > 0) {
			--this.jumpDelay;
		}

		if (this.cj()) {
			this.aV = 0;
			this.c(this.locX(), this.locY(), this.locZ());
		}
		if (this.aV > 0) {
			double newX = this.locX() + (this.bd - this.locX()) / this.aV;
			double newY = this.locY() + (this.be - this.locY()) / this.aV;
			double newZ = this.locZ() + (this.bf - this.locZ()) / this.aV;
			double d3 = MathHelper.g(this.az - (double) this.yaw);
			this.yaw = (float) ((double) this.yaw + d3 / this.aV);
			this.pitch = (float) ((double) this.pitch + (this.aT - (double) this.pitch) / this.aV);
			--this.aV;
			this.setPosition(newX, newY, newZ);
			this.setYawPitch(this.yaw, this.pitch);
		} else {
			this.setMot(this.getMot().a(0.98D));
		}

		Vec3D vec3d = this.getMot();
		double motX = vec3d.x;
		double motY = vec3d.y;
		double motZ = vec3d.z;

		if (Math.abs(vec3d.x) < 0.003D) {
			motX = 0.0D;
		}

		if (Math.abs(vec3d.y) < 0.003D) {
			motY = 0.0D;
		}

		if (Math.abs(vec3d.z) < 0.003D) {
			motZ = 0.0D;
		}

		this.setMot(motX, motY, motZ);

		this.doMyPetTick();

		if (this.jumping) {
			double d7;

			if (this.aQ()) {
				d7 = this.b(TagsFluid.LAVA);
			} else {
				d7 = this.b(TagsFluid.WATER);
			}

			boolean flag = this.isInWater() && d7 > 0.0D;
			double d8 = this.cx();
			if (flag && (!this.onGround || d7 > d8)) {
				this.c(TagsFluid.WATER);
			} else if (this.aQ() && (!this.onGround || d7 > d8)) {
				this.c(TagsFluid.LAVA);
			} else if ((this.onGround || flag && d7 <= d8) && this.jumpDelay == 0) {
				this.jump();
				this.jumpDelay = 10;
			}
		} else {
			this.jumpDelay = 0;
		}

		this.aR *= 0.98F;
		this.aT *= 0.98F;

		// this.n(); //no Elytra flight
		this.g(new Vec3D(this.aR, this.aS, this.aT));
		this.collideNearby();
	}

	@Override
	protected boolean addPassenger(Entity entity) {
		boolean returnVal = false;
		// don't allow anything but the owner to ride this entity
		Ride rideSkill = myPet.getSkills().get(RideImpl.class);
		if (rideSkill != null && entity instanceof EntityPlayer && getOwner().equals(entity)) {
			if (entity.getVehicle() != this) {
				throw new IllegalStateException("Use x.startRiding(y), not y.addPassenger(x)");
			} else {
				Preconditions.checkState(!entity.passengers.contains(this), "Circular entity riding! %s %s", this, entity);
				boolean cancelled = false;
				if (MyPetApi.getPlatformHelper().isSpigot()) {
					cancelled = MountEventWrapper.callEvent(entity.getBukkitEntity(), this.getBukkitEntity());
				}
				if (cancelled) {
					returnVal = false;
				} else {
					returnVal = mountOwner(entity);
				}
			}

			/* this should not matter anymore but I'm leaving it in here in case it is relevant
			if (this instanceof IJumpable) {
				double factor = 1;
				if (Configuration.HungerSystem.USE_HUNGER_SYSTEM && Configuration.HungerSystem.AFFECT_RIDE_SPEED) {
					factor = Math.log10(myPet.getSaturation()) / 2;
				}
				getAttributeInstance(GenericAttributes.MOVEMENT_SPEED).setValue((0.22222F * (1F + (rideSkill.getSpeedIncrease().getValue() / 100F))) * factor);
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
		PlatformHelper platformHelper = (PlatformHelper) MyPetApi.getPlatformHelper();
		AxisAlignedBB bb = entity.getBoundingBox();
		bb = getBBAtPosition(bb, this.locX(), this.locY(), this.locZ());
		if (!platformHelper.canSpawn(getBukkitEntity().getLocation(), bb)) {
			entity.a(this, true);
		} else {
			entity.setPosition(locX(), locY(), locZ());
		}
		return result;
	}

	protected AxisAlignedBB getBBAtPosition(AxisAlignedBB bb, double x, double y, double z) {
		double width = bb.b() / 2;
		double height = bb.c();
		double depth = bb.d() / 2;
		return new AxisAlignedBB(x - width, y, z - depth, x + width, y + height, z + width);
	}

	/**
	 * Entity AI tick method
	 * -> updateAITasks()
	 */
	protected void doMyPetTick() {
		try {
			++this.ticksFarFromPlayer;

			if (isAlive()) {
				getEntitySenses().a(); // sensing

				Player p = getOwner().getPlayer();
				if (p == null || !p.isOnline()) {
					this.die();
					return;
				}

				if(interactCooldown>0) {
					interactCooldown--;
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

			mobTick();

			// controls
			getControllerMove().a(); // move
			getControllerLook().a(); // look
			getControllerJump().b(); // jump
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * MyPets are not persistant so no data needs to be saved
	 */
	@Override
	public boolean d(NBTTagCompound nbttagcompound) {
		return false;
	}

	/**
	 * -> falldamage
	 */
	@Override
	public int e(float f, float f1) {
		if (!this.isFlying) {
			return super.e(f, f1);
		}
		return 0;
	}


	/**
	 * -> travel
	 */
	public void g(Vec3D vec3d) {
		if (!hasRider || !this.isVehicle()) {
			super.g(vec3d);
			return;
		}

		if (this.onGround && this.isFlying) {
			isFlying = false;
			this.fallDistance = 0;
		}

		EntityLiving passenger = null;
		if(!indirectRiding) {
			passenger = (EntityLiving) this.getFirstPassenger();
		} else {
			if(this.getFirstPassenger().getPassengers().isEmpty()) {
				super.g(vec3d);
				return;
			} else {
				passenger = (EntityLiving) this.getFirstPassenger().getPassengers().get(0);
			}
		}

		if (this.a(TagsFluid.WATER)) {
			this.setMot(this.getMot().add(0, 0.4, 0));
		}

		Ride rideSkill = myPet.getSkills().get(RideImpl.class);
		if (rideSkill == null || !rideSkill.getActive().getValue()) {
			passenger.stopRiding();
			return;
		}

		//Rotations are fun
		if (indirectRiding) {
			if (this.getFirstPassenger() instanceof EntityMySeat) {
				EntityMySeat seat = (EntityMySeat) this.getFirstPassenger();
				seat.lastYaw = (seat.yaw = passenger.yaw);
				seat.pitch = passenger.pitch * 0.5F;
				seat.aC = (this.aA = this.yaw);
			}
		}

		//apply pitch & yaw
		this.lastYaw = (this.yaw = passenger.yaw);
		this.pitch = passenger.pitch * 0.5F;
		setYawPitch(this.yaw, this.pitch);
		this.aC = (this.aA = this.yaw);

		// get motion from passenger (player)
		double motionSideways = passenger.aR * 0.5F;
		double motionForward = passenger.aT;

		// backwards is slower
		if (motionForward <= 0.0F) {
			motionForward *= 0.25F;
		}
		// sideways is slower too but not as slow as backwards
		motionSideways *= 0.85F;

		float speed = 0.22222F * (1F + (rideSkill.getSpeedIncrease().getValue() / 100F));
		double jumpHeight = Util.clamp(1 + rideSkill.getJumpHeight().getValue().doubleValue(), 0, 10);
		float ascendSpeed = 0.2f;

		if (Configuration.HungerSystem.USE_HUNGER_SYSTEM && Configuration.HungerSystem.AFFECT_RIDE_SPEED) {
			double factor = Math.log10(myPet.getSaturation()) / 2;
			speed *= factor;
			jumpHeight *= factor;
			ascendSpeed *= factor;
		}

		ride(motionSideways, motionForward, vec3d.y, speed); // apply motion

		// throw player move event
		if (Configuration.Misc.THROW_PLAYER_MOVE_EVENT_WHILE_RIDING) {
			double delta = Math.pow(this.locX() - this.lastX, 2.0D) + Math.pow(this.locY() - this.lastY, 2.0D) + Math.pow(this.locZ() - this.lastZ, 2.0D);
			float deltaAngle = Math.abs(this.yaw - lastYaw) + Math.abs(this.pitch - lastPitch);
			if (delta > 0.00390625D || deltaAngle > 10.0F) {
				Location to = getBukkitEntity().getLocation();
				Location from = new Location(world.getWorld(), this.lastX, this.lastY, this.lastZ, this.lastYaw, this.lastPitch);
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
			this.getBlockJumpFactor(); // TODO why?
			boolean doJump = false;
			if (jump != null) {
				try {
					doJump = jump.getBoolean(passenger);
				} catch (IllegalAccessException ignored) {}
			}

			if (doJump) {
				if (onGround) {
					jumpHeight = new BigDecimal(jumpHeight).setScale(1, RoundingMode.HALF_UP).doubleValue();
					String jumpHeightString = JumpHelper.JUMP_FORMAT.format(jumpHeight);
					Double jumpVelocity = JumpHelper.JUMP_MAP.get(jumpHeightString);
					jumpVelocity = jumpVelocity == null ? 0.44161199999510264 : jumpVelocity;
					this.setMot(this.getMot().x, jumpVelocity, this.getMot().z);
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
						if (this.getMot().y < ascendSpeed) {
							this.setMot(this.getMot().x, ascendSpeed, this.getMot().z);
							this.fallDistance = 0;
							this.isFlying = true;
						}
					}
				}
			} else {
				flyCheckCounter = 0;
			}
		}

		if (Configuration.HungerSystem.USE_HUNGER_SYSTEM && Configuration.Skilltree.Skill.Ride.HUNGER_PER_METER > 0) {
			double dX = locX() - lastX;
			double dY = Math.max(0, locY() - lastY);
			double dZ = locZ() - lastZ;
			if (dX != 0 || dY != 0 || dZ != 0) {
				double distance = Math.sqrt(dX * dX + dY * dY + dZ * dZ);
				if (isFlying && rideSkill.getFlyLimit().getValue().doubleValue() > 0) {
					limitCounter -= distance;
				}
				myPet.decreaseSaturation(Configuration.Skilltree.Skill.Ride.HUNGER_PER_METER * distance);
				double factor = Math.log10(myPet.getSaturation()) / 2;
				getAttributeInstance(GenericAttributes.MOVEMENT_SPEED).setValue((0.22222F * (1F + (rideSkill.getSpeedIncrease().getValue() / 100F))) * factor);
				this.n((float) this.getAttributeInstance(GenericAttributes.MOVEMENT_SPEED).getValue());
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
	protected SoundEffect getSoundAmbient() {
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
	protected void d(DamageSource damagesource) {
		CraftEventFactory.callEntityDeathEvent(this);
	}

	@Override
	public DamageSource dm() {
		if (deathReason != null) {
			return deathReason;
		}
		return super.dm();
	}
	
	@Override
	public void burnFromLava() {
		if(this.getMyPet() instanceof MyPetLavaEntity) {
			return;
		} else {
			super.burnFromLava();
		}
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