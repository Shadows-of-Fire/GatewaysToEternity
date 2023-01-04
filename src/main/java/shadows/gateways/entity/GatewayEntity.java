package shadows.gateways.entity;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import com.mojang.authlib.GameProfile;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent.BossBarColor;
import net.minecraft.world.BossEvent.BossBarOverlay;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;
import shadows.gateways.GatewayObjects;
import shadows.gateways.Gateways;
import shadows.gateways.client.ParticleHandler;
import shadows.gateways.event.GateEvent;
import shadows.gateways.gate.Gateway;
import shadows.gateways.gate.GatewayManager;
import shadows.gateways.gate.Wave;
import shadows.gateways.net.ParticleMessage;
import shadows.placebo.network.PacketDistro;

public class GatewayEntity extends Entity implements IEntityAdditionalSpawnData {

	public static final EntityDataAccessor<Boolean> WAVE_ACTIVE = SynchedEntityData.defineId(GatewayEntity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<Integer> TICKS_ACTIVE = SynchedEntityData.defineId(GatewayEntity.class, EntityDataSerializers.INT);
	public static final EntityDataAccessor<Integer> WAVE = SynchedEntityData.defineId(GatewayEntity.class, EntityDataSerializers.INT);
	public static final EntityDataAccessor<Integer> ENEMIES = SynchedEntityData.defineId(GatewayEntity.class, EntityDataSerializers.INT);

	protected Gateway gate;
	protected ServerBossEvent bossEvent;

	protected final Set<LivingEntity> currentWaveEntities = new HashSet<>();
	protected final Set<UUID> unresolvedWaveEntities = new HashSet<>();
	protected UUID summonerId;

	protected float clientScale = 0F;
	protected Queue<ItemStack> undroppedItems = new ArrayDeque<>();
	protected FailureReason failureReason;

	/**
	 * Primary constructor.
	 */
	public GatewayEntity(Level level, Player placer, Gateway gate) {
		super(GatewayObjects.GATEWAY, level);
		this.summonerId = placer.getUUID();
		this.gate = gate;
		this.setCustomName(new TranslatableComponent(gate.getId().toString().replace(':', '.')).withStyle(Style.EMPTY.withColor(gate.getColor())));
		this.bossEvent = this.createBossEvent();
		this.refreshDimensions();
	}

	/**
	 * Client/Load constructor.
	 */
	public GatewayEntity(EntityType<?> type, Level level) {
		super(type, level);
	}

	@Override
	public EntityDimensions getDimensions(Pose pPose) {
		return this.gate.getSize().dims;
	}

	@Override
	public void tick() {
		super.tick();
		if (!level.isClientSide) {
			if (!unresolvedWaveEntities.isEmpty()) {
				for (UUID id : unresolvedWaveEntities) {
					Entity e = ((ServerLevel) level).getEntity(id);
					if (e instanceof LivingEntity) this.currentWaveEntities.add((LivingEntity) e);
				}
				unresolvedWaveEntities.clear();
			}

			if (isWaveActive()) {
				int maxWaveTime = getCurrentWave().maxWaveTime();
				if (this.getTicksActive() > maxWaveTime) {
					this.onFailure(this.currentWaveEntities, FailureReason.TIMER_ELAPSED);
					return;
				}
				this.entityData.set(TICKS_ACTIVE, this.getTicksActive() + 1);
			}

			boolean active = isWaveActive();
			List<LivingEntity> enemies = this.currentWaveEntities.stream().filter(e -> e.getHealth() > 0 && e.getRemovalReason() != RemovalReason.KILLED).toList();
			for (LivingEntity entity : enemies) {
				if (isOutOfRange(entity)) {
					this.onFailure(currentWaveEntities, FailureReason.ENTITY_TOO_FAR);
					return;
				}
				if (entity.tickCount % 20 == 0) this.spawnParticle(this.gate.getColor(), entity.getX(), entity.getY() + entity.getBbHeight() / 2, entity.getZ(), 0);
			}
			this.entityData.set(ENEMIES, enemies.size());
			if (active && enemies.size() == 0) {
				this.onWaveEnd(getCurrentWave());
				this.currentWaveEntities.clear();
				this.entityData.set(WAVE_ACTIVE, false);
				this.entityData.set(TICKS_ACTIVE, 0);
				this.entityData.set(WAVE, Math.min(getWave() + 1, this.gate.getNumWaves()));
			} else if (!active && !isLastWave()) {
				if (this.getTicksActive() > this.getCurrentWave().setupTime()) {
					this.spawnWave();
					return;
				}
				this.entityData.set(TICKS_ACTIVE, this.getTicksActive() + 1);
			}

			if (this.tickCount % 4 == 0 && !undroppedItems.isEmpty()) {
				for (int i = 0; i < this.getDropCount(); i++) {
					spawnItem(undroppedItems.remove());
					if (undroppedItems.isEmpty()) break;
				}
			}

			if (!active && undroppedItems.isEmpty() && isLastWave()) {
				this.completePortal();
			}
		} else {
			if (this.tickCount % 20 == 0) {
				ParticleHandler.spawnIdleParticles(this);
			}
		}
	}

	protected int getDropCount() {
		return 3 + this.undroppedItems.size() / 100;
	}

	public boolean isLastWave() {
		return this.getWave() == this.gate.getNumWaves();
	}

	/**
	 * Returns the current wave, or returns the last wave, if the last wave has been completed.
	 */
	public Wave getCurrentWave() {
		return this.gate.getWave(Math.min(this.gate.getNumWaves() - 1, this.getWave()));
	}

	public void spawnWave() {
		BlockPos blockpos = this.blockPosition();

		List<LivingEntity> spawned = this.gate.getWave(getWave()).spawnWave((ServerLevel) this.level, blockpos, this);
		this.currentWaveEntities.addAll(spawned);

		this.entityData.set(WAVE_ACTIVE, true);
		this.entityData.set(TICKS_ACTIVE, 0);
		this.entityData.set(ENEMIES, this.currentWaveEntities.size());
	}

	protected void completePortal() {
		int completionXp = this.gate.getCompletionXp();
		while (completionXp > 0) {
			int i = 5;
			completionXp -= i;
			this.level.addFreshEntity(new ExperienceOrb(this.level, this.getX(), this.getY(), this.getZ(), i));
		}
		Player player = summonerOrClosest();
		this.gate.getRewards().forEach(r -> {
			r.generateLoot((ServerLevel) this.level, this, player, this::spawnCompletionItem);
		});

		this.bossEvent.setCreateWorldFog(false);
		this.remove(RemovalReason.KILLED);
		this.playSound(GatewayObjects.GATE_END, 1, 1);

		this.level.getNearbyPlayers(TargetingConditions.DEFAULT, null, getBoundingBox().inflate(15)).forEach(p -> p.awardStat(GatewayObjects.Stats.STAT_GATES_DEFEATED));
		MinecraftForge.EVENT_BUS.post(new GateEvent.Completed(this));
	}

	public void onGateCreated() {
		this.playSound(GatewayObjects.GATE_START, 1, 1);
		MinecraftForge.EVENT_BUS.post(new GateEvent.Opened(this));
	}

	/**
	 * Called when a wave is completed.  Responsible for loot spawns.
	 */
	protected void onWaveEnd(Wave wave) {
		Player player = summonerOrClosest();
		undroppedItems.addAll(wave.spawnRewards((ServerLevel) level, this, player));
		MinecraftForge.EVENT_BUS.post(new GateEvent.WaveEnd(this));
	}

	public Player summonerOrClosest() {
		Player player = this.summonerId == null ? null : level.getPlayerByUUID(summonerId);
		if (player == null) {
			player = level.getNearestPlayer(this, 50);
		}
		if (player == null) {
			return summonerId == null ? FakePlayerFactory.getMinecraft((ServerLevel) level) : FakePlayerFactory.get((ServerLevel) level, new GameProfile(summonerId, ""));
		}
		return player;
	}

	/**
	 * Called when a player fails to complete a wave in time, closing the gateway.
	 */
	public void onFailure(Collection<LivingEntity> remaining, FailureReason reason) {
		this.failureReason = reason;
		MinecraftForge.EVENT_BUS.post(new GateEvent.Failed(this));
		Player player = summonerOrClosest();
		if (player != null) player.sendMessage(reason.getMsg(), Util.NIL_UUID);
		spawnLightningOn(this, false);
		remaining.stream().filter(Entity::isAlive).forEach(e -> spawnLightningOn(e, true));
		remaining.forEach(e -> e.remove(RemovalReason.DISCARDED));
		this.getGateway().getFailures().forEach(f -> f.onFailure((ServerLevel) this.level, this, player, reason));
		this.bossEvent.setCreateWorldFog(false);
		this.remove(RemovalReason.DISCARDED);
	}

	protected ServerBossEvent createBossEvent() {
		ServerBossEvent event = new ServerBossEvent(new TextComponent("GATEWAY_ID" + this.getId()), BossBarColor.BLUE, BossBarOverlay.PROGRESS);
		event.setCreateWorldFog(true);
		return event;
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
		tag.putInt("wave", getWave());
		tag.putString("gate", this.gate.getId().toString());
		long[] ids = new long[this.currentWaveEntities.size() * 2];
		int idx = 0;
		for (LivingEntity e : this.currentWaveEntities) {
			UUID id = e.getUUID();
			ids[idx++] = id.getMostSignificantBits();
			ids[idx++] = id.getLeastSignificantBits();
		}
		tag.putLongArray("wave_entities", ids);
		tag.putBoolean("active", isWaveActive());
		tag.putInt("ticks_active", getTicksActive());
		if (this.summonerId != null) tag.putUUID("summoner", this.summonerId);
		ListTag stacks = new ListTag();
		for (ItemStack s : this.undroppedItems) {
			stacks.add(s.serializeNBT());
		}
		tag.put("queued_stacks", stacks);
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
		this.entityData.set(WAVE, tag.getInt("wave"));
		this.gate = GatewayManager.INSTANCE.getOrDefault(new ResourceLocation(tag.getString("gate")), this.gate);
		if (this.gate == null) {
			Gateways.LOGGER.error("Invalid gateway at {} will be removed.", this.position());
			this.remove(RemovalReason.DISCARDED);
		}
		long[] entities = tag.getLongArray("wave_entities");
		for (int i = 0; i < entities.length; i += 2) {
			unresolvedWaveEntities.add(new UUID(entities[i], entities[i + 1]));
		}
		this.entityData.set(WAVE_ACTIVE, tag.getBoolean("active"));
		this.entityData.set(TICKS_ACTIVE, tag.getInt("ticks_active"));
		if (tag.contains("summoner")) this.summonerId = tag.getUUID("summoner");
		ListTag stacks = tag.getList("queued_stacks", Tag.TAG_COMPOUND);
		for (Tag inbt : stacks) {
			undroppedItems.add(ItemStack.of((CompoundTag) inbt));
		}
		this.bossEvent = createBossEvent();
	}

	@Override
	protected void defineSynchedData() {
		this.entityData.define(WAVE_ACTIVE, false);
		this.entityData.define(TICKS_ACTIVE, 0);
		this.entityData.define(WAVE, 0);
		this.entityData.define(ENEMIES, 0);
	}

	@Override
	public Packet<?> getAddEntityPacket() {
		return NetworkHooks.getEntitySpawningPacket(this);
	}

	@Override
	public void startSeenByPlayer(ServerPlayer player) {
		super.startSeenByPlayer(player);
		this.bossEvent.addPlayer(player);
	}

	@Override
	public void stopSeenByPlayer(ServerPlayer player) {
		super.stopSeenByPlayer(player);
		this.bossEvent.removePlayer(player);
	}

	public int getTicksActive() {
		return this.entityData.get(TICKS_ACTIVE);
	}

	public boolean isWaveActive() {
		return this.entityData.get(WAVE_ACTIVE);
	}

	public int getWave() {
		return this.entityData.get(WAVE);
	}

	public int getActiveEnemies() {
		return this.entityData.get(ENEMIES);
	}

	public Gateway getGateway() {
		return gate;
	}

	public ServerBossEvent getBossInfo() {
		return bossEvent;
	}

	public float getClientScale() {
		return this.clientScale;
	}

	public void setClientScale(float clientScale) {
		this.clientScale = clientScale;
	}

	public static void spawnLightningOn(Entity entity, boolean effectOnly) {
		LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(entity.level);
		bolt.setPos(entity.getX(), entity.getY(), entity.getZ());
		bolt.setVisualOnly(effectOnly);
		entity.level.addFreshEntity(bolt);
	}

	public void spawnParticle(TextColor color, double x, double y, double z, int type) {
		PacketDistro.sendToTracking(Gateways.CHANNEL, new ParticleMessage(this, x, y, z, color, type), (ServerLevel) level, new BlockPos((int) x, (int) y, (int) z));
	}

	public void spawnItem(ItemStack stack) {
		ItemEntity i = new ItemEntity(level, 0, 0, 0, stack);
		i.setPos(this.getX() + Mth.nextDouble(random, -0.5, 0.5), this.getY() + 1.5, this.getZ() + Mth.nextDouble(random, -0.5, 0.5));
		i.setDeltaMovement(Mth.nextDouble(random, -0.15, 0.15), 0.4, Mth.nextDouble(random, -0.15, 0.15));
		level.addFreshEntity(i);
		this.level.playSound(null, i.getX(), i.getY(), i.getZ(), GatewayObjects.GATE_WARP, SoundSource.HOSTILE, 0.25F, 2.0F);
	}

	public void spawnCompletionItem(ItemStack stack) {
		ItemEntity i = new ItemEntity(level, 0, 0, 0, stack);
		double variance = 0.05F * this.gate.getSize().getScale();
		i.setPos(this.getX(), this.getY() + this.getBbHeight() / 2, this.getZ());
		i.setDeltaMovement(Mth.nextDouble(random, -variance, variance), this.getBbHeight() / 20F, Mth.nextDouble(random, -variance, variance));
		i.setUnlimitedLifetime();
		level.addFreshEntity(i);
	}

	@Override
	public void writeSpawnData(FriendlyByteBuf buf) {
		buf.writeResourceLocation(this.gate.getId());
	}

	@Override
	public void readSpawnData(FriendlyByteBuf buf) {
		this.gate = GatewayManager.INSTANCE.getValue(buf.readResourceLocation());
		if (this.gate == null) throw new RuntimeException("Invalid gateway received on client!");
	}

	@Override
	protected int getPermissionLevel() {
		return 2;
	}

	/**
	 * @return The failure reason, if the gate has failed, or null otherwise.
	 */
	@Nullable
	public FailureReason getFailureReason() {
		return this.failureReason;
	}

	public boolean isOutOfRange(Entity entity) {
		return entity.distanceToSqr(this) > this.gate.getLeashRangeSq() || entity.getRemovalReason() == RemovalReason.CHANGED_DIMENSION;
	}

	public static enum GatewaySize {
		SMALL(EntityDimensions.scalable(2F, 3F), 1F),
		MEDIUM(EntityDimensions.scalable(4F, 6F), 2F),
		LARGE(EntityDimensions.scalable(6F, 9F), 3F);

		private final EntityDimensions dims;
		private final float scale;

		GatewaySize(EntityDimensions dims, float scale) {
			this.dims = dims;
			this.scale = scale;
		}

		public float getScale() {
			return this.scale;
		}
	}

	public static enum FailureReason {
		SPAWN_FAILED("error.gateways.wave_failed"),
		ENTITY_TOO_FAR("error.gateways.too_far"),
		TIMER_ELAPSED("error.gateways.wave_elapsed");

		private final String langKey;

		FailureReason(String langKey) {
			this.langKey = langKey;
		}

		public Component getMsg() {
			return new TranslatableComponent(this.langKey).withStyle(ChatFormatting.RED, ChatFormatting.UNDERLINE);
		}
	}

}
