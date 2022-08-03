package shadows.gateways.entity;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;
import shadows.gateways.GatewayObjects;
import shadows.gateways.GatewaysToEternity;
import shadows.gateways.client.ParticleHandler;
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
	protected ServerBossEvent bossInfo;

	protected final Set<LivingEntity> currentWaveEntities = new HashSet<>();
	protected final Set<UUID> unresolvedWaveEntities = new HashSet<>();
	protected UUID summonerId;

	protected int clientTickCounter = -1;
	protected Queue<ItemStack> undroppedItems = new ArrayDeque<>();

	/**
	 * Primary constructor.
	 */
	public GatewayEntity(Level level, Player placer, Gateway gate) {
		super(GatewayObjects.GATEWAY, level);
		this.summonerId = placer.getUUID();
		this.gate = gate;
		this.setCustomName(new TranslatableComponent(gate.getId().toString().replace(':', '.')).withStyle(Style.EMPTY.withColor(gate.getColor())));
		this.bossInfo = this.createBossInfo();
	}

	/**
	 * Client/Load constructor.
	 */
	public GatewayEntity(EntityType<?> type, Level level) {
		super(type, level);
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
					this.onFailure(getWave(), this.currentWaveEntities);
					return;
				}
				this.entityData.set(TICKS_ACTIVE, this.getTicksActive() + 1);
			}

			boolean active = isWaveActive();
			List<LivingEntity> enemies = this.currentWaveEntities.stream().filter(Entity::isAlive).toList();
			for (LivingEntity entity : enemies) {
				if (entity.distanceToSqr(this) > 32 * 32) {
					this.onFailure(getWave(), currentWaveEntities);
					return;
				}
			}
			this.entityData.set(ENEMIES, enemies.size());
			if (active && enemies.size() == 0) {
				this.onWaveEnd(getCurrentWave());
				this.currentWaveEntities.clear();
				this.entityData.set(WAVE_ACTIVE, false);
				if (isLastWave()) {
					this.onLastWaveEnd();
				}
				this.entityData.set(TICKS_ACTIVE, 0);
			} else if (!active) {
				if (this.getTicksActive() > this.getCurrentWave().setupTime() && !isLastWave()) {
					this.spawnWave();
					return;
				}
				this.entityData.set(TICKS_ACTIVE, this.getTicksActive() + 1);
			}

			if (this.tickCount % 4 == 0 && !undroppedItems.isEmpty()) {
				spawnItem(undroppedItems.remove());
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

	public boolean isLastWave() {
		return this.getWave() == this.gate.getNumWaves() - 1;
	}

	public Wave getCurrentWave() {
		return this.gate.getWave(this.getWave());
	}

	public void spawnWave() {
		BlockPos blockpos = this.blockPosition();

		List<LivingEntity> spawned = this.gate.getWave(getWave()).spawnWave((ServerLevel) this.level, blockpos, this);
		this.currentWaveEntities.addAll(spawned);

		this.entityData.set(WAVE_ACTIVE, true);
		this.entityData.set(TICKS_ACTIVE, 0);
		this.entityData.set(ENEMIES, this.currentWaveEntities.size());
	}

	protected void onLastWaveEnd() {
		int completionXp = this.gate.getCompletionXp();
		while (completionXp > 0) {
			int i = 5;
			completionXp -= i;
			this.level.addFreshEntity(new ExperienceOrb(this.level, this.getX(), this.getY(), this.getZ(), i));
		}
	}

	protected void completePortal() {
		this.remove(RemovalReason.KILLED);
		this.playSound(GatewayObjects.GATE_END, 1, 1);
	}

	public void onGateCreated() {
		this.playSound(GatewayObjects.GATE_START, 1, 1);
	}

	/**
	 * Called when a wave is completed.  Responsible for loot spawns.
	 */
	protected void onWaveEnd(Wave wave) {
		Player player = this.summonerId == null ? null : level.getPlayerByUUID(summonerId);
		if (player == null) {
			player = level.getNearestPlayer(this, 50);
		}
		undroppedItems.addAll(wave.spawnRewards((ServerLevel) level, this, player));
		this.entityData.set(WAVE, Math.min(getWave() + 1, this.gate.getNumWaves() - 1));
	}

	/**
	 * Called when a player fails to complete a wave in time, closing the gateway.
	 */
	protected void onFailure(int wave, Set<LivingEntity> remaining) {
		spawnLightningOn(this, false);
		remaining.stream().filter(Entity::isAlive).forEach(e -> spawnLightningOn(e, true));
		remaining.forEach(e -> e.remove(RemovalReason.DISCARDED));
		this.remove(RemovalReason.DISCARDED);
	}

	protected ServerBossEvent createBossInfo() {
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
			GatewaysToEternity.LOGGER.error("Invalid gateway at {} will be removed.", this.position());
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
		this.bossInfo = createBossInfo();
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
		this.bossInfo.addPlayer(player);
	}

	@Override
	public void stopSeenByPlayer(ServerPlayer player) {
		super.stopSeenByPlayer(player);
		this.bossInfo.removePlayer(player);
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
		return bossInfo;
	}

	public int getClientTicks() {
		return this.clientTickCounter;
	}

	public void setClientTicks(int ticks) {
		this.clientTickCounter = ticks;
	}

	public static void spawnLightningOn(Entity entity, boolean effectOnly) {
		LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(entity.level);
		bolt.setPos(entity.getX(), entity.getY(), entity.getZ());
		bolt.setVisualOnly(effectOnly);
		entity.level.addFreshEntity(bolt);
	}

	public void spawnParticle(TextColor color, double x, double y, double z, int type) {
		PacketDistro.sendToTracking(GatewaysToEternity.CHANNEL, new ParticleMessage(this, x, y, z, color, type), (ServerLevel) level, new BlockPos((int) x, (int) y, (int) z));
	}

	public void spawnItem(ItemStack stack) {
		ItemEntity i = new ItemEntity(level, 0, 0, 0, stack);
		i.setPos(this.getX() + Mth.nextDouble(random, -0.5, 0.5), this.getY() + 1.5, this.getZ() + Mth.nextDouble(random, -0.5, 0.5));
		i.setDeltaMovement(Mth.nextDouble(random, -0.15, 0.15), 0.4, Mth.nextDouble(random, -0.15, 0.15));
		level.addFreshEntity(i);
		this.level.playSound(null, i.getX(), i.getY(), i.getZ(), GatewayObjects.GATE_WARP, SoundSource.HOSTILE, 0.25F, 2.0F);
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

	public static enum GatewaySize {
		SMALL,
		MEDIUM,
		LARGE;
	}

}
