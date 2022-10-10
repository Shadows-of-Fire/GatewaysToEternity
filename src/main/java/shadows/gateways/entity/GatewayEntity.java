package shadows.gateways.entity;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.mojang.authlib.GameProfile;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPredicate;
import net.minecraft.entity.EntitySize;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Pose;
import net.minecraft.entity.effect.LightningBoltEntity;
import net.minecraft.entity.item.ExperienceOrbEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.Color;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.BossInfo;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerBossInfo;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.network.NetworkHooks;
import shadows.gateways.GatewayObjects;
import shadows.gateways.Gateways;
import shadows.gateways.client.ParticleHandler;
import shadows.gateways.gate.Gateway;
import shadows.gateways.gate.GatewayManager;
import shadows.gateways.gate.Wave;
import shadows.gateways.net.ParticleMessage;
import shadows.placebo.util.NetworkUtils;

public class GatewayEntity extends Entity implements IEntityAdditionalSpawnData {

	public static final DataParameter<Boolean> WAVE_ACTIVE = EntityDataManager.defineId(GatewayEntity.class, DataSerializers.BOOLEAN);
	public static final DataParameter<Integer> TICKS_ACTIVE = EntityDataManager.defineId(GatewayEntity.class, DataSerializers.INT);
	public static final DataParameter<Integer> WAVE = EntityDataManager.defineId(GatewayEntity.class, DataSerializers.INT);
	public static final DataParameter<Integer> ENEMIES = EntityDataManager.defineId(GatewayEntity.class, DataSerializers.INT);

	protected Gateway gate;
	protected ServerBossInfo bossEvent;

	protected final Set<LivingEntity> currentWaveEntities = new HashSet<>();
	protected final Set<UUID> unresolvedWaveEntities = new HashSet<>();
	protected UUID summonerId;

	protected float clientScale = 0F;
	protected Queue<ItemStack> undroppedItems = new ArrayDeque<>();

	/**
	 * Primary constructor.
	 */
	public GatewayEntity(World level, PlayerEntity placer, Gateway gate) {
		super(GatewayObjects.GATEWAY, level);
		this.summonerId = placer.getUUID();
		this.gate = gate;
		this.setCustomName(new TranslationTextComponent(gate.getId().toString().replace(':', '.')).withStyle(Style.EMPTY.withColor(gate.getColor())));
		this.bossEvent = this.createBossEvent();
		this.refreshDimensions();
	}

	/**
	 * Client/Load constructor.
	 */
	public GatewayEntity(EntityType<?> type, World level) {
		super(type, level);
	}

	@Override
	public EntitySize getDimensions(Pose pPose) {
		return this.gate.getSize().dims;
	}

	@Override
	public void tick() {
		super.tick();
		if (!level.isClientSide) {
			if (!unresolvedWaveEntities.isEmpty()) {
				for (UUID id : unresolvedWaveEntities) {
					Entity e = ((ServerWorld) level).getEntity(id);
					if (e instanceof LivingEntity) this.currentWaveEntities.add((LivingEntity) e);
				}
				unresolvedWaveEntities.clear();
			}

			if (isWaveActive()) {
				int maxWaveTime = getCurrentWave().maxWaveTime();
				if (this.getTicksActive() > maxWaveTime) {
					this.onFailure(this.currentWaveEntities, new TranslationTextComponent("error.gateways.wave_elapsed").withStyle(TextFormatting.RED, TextFormatting.UNDERLINE));
					return;
				}
				this.entityData.set(TICKS_ACTIVE, this.getTicksActive() + 1);
			}

			boolean active = isWaveActive();
			List<LivingEntity> enemies = this.currentWaveEntities.stream().filter(Entity::isAlive).collect(Collectors.toList());
			for (LivingEntity entity : enemies) {
				if (entity.distanceToSqr(this) > this.gate.getLeashRangeSq()) {
					this.onFailure(currentWaveEntities, new TranslationTextComponent("error.gateways.too_far").withStyle(TextFormatting.RED));
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
				for (int i = 0; i < 3; i++) {
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

		List<LivingEntity> spawned = this.gate.getWave(getWave()).spawnWave((ServerWorld) this.level, blockpos, this);
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
			this.level.addFreshEntity(new ExperienceOrbEntity(this.level, this.getX(), this.getY(), this.getZ(), i));
		}
		PlayerEntity player = summonerOrClosest();
		this.gate.getRewards().forEach(r -> {
			r.generateLoot((ServerWorld) this.level, this, player, this::spawnCompletionItem);
		});

		this.remove();
		this.playSound(GatewayObjects.GATE_END, 1, 1);

		this.level.getNearbyPlayers(EntityPredicate.DEFAULT, null, getBoundingBox().inflate(15)).forEach(p -> p.awardStat(GatewayObjects.Stats.STAT_GATES_DEFEATED));
	}

	public void onGateCreated() {
		this.playSound(GatewayObjects.GATE_START, 1, 1);
	}

	/**
	 * Called when a wave is completed.  Responsible for loot spawns.
	 */
	protected void onWaveEnd(Wave wave) {
		PlayerEntity player = summonerOrClosest();
		undroppedItems.addAll(wave.spawnRewards((ServerWorld) level, this, player));
	}

	public PlayerEntity summonerOrClosest() {
		PlayerEntity player = this.summonerId == null ? null : level.getPlayerByUUID(summonerId);
		if (player == null) {
			player = level.getNearestPlayer(this, 50);
		}
		if (player == null) {
			return summonerId == null ? FakePlayerFactory.getMinecraft((ServerWorld) level) : FakePlayerFactory.get((ServerWorld) level, new GameProfile(summonerId, ""));
		}
		return player;
	}

	/**
	 * Called when a player fails to complete a wave in time, closing the gateway.
	 */
	public void onFailure(Collection<LivingEntity> remaining, ITextComponent message) {
		PlayerEntity player = summonerOrClosest();
		if (player != null) player.sendMessage(message, Util.NIL_UUID);
		spawnLightningOn(this, false);
		remaining.stream().filter(Entity::isAlive).forEach(e -> spawnLightningOn(e, true));
		remaining.forEach(e -> e.remove());
		this.remove();
	}

	protected ServerBossInfo createBossEvent() {
		ServerBossInfo event = new ServerBossInfo(new StringTextComponent("GATEWAY_ID" + this.getId()), BossInfo.Color.BLUE, BossInfo.Overlay.PROGRESS);
		event.setCreateWorldFog(true);
		return event;
	}

	@Override
	protected void addAdditionalSaveData(CompoundNBT tag) {
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
		ListNBT stacks = new ListNBT();
		for (ItemStack s : this.undroppedItems) {
			stacks.add(s.serializeNBT());
		}
		tag.put("queued_stacks", stacks);
	}

	@Override
	protected void readAdditionalSaveData(CompoundNBT tag) {
		this.entityData.set(WAVE, tag.getInt("wave"));
		this.gate = GatewayManager.INSTANCE.getOrDefault(new ResourceLocation(tag.getString("gate")), this.gate);
		if (this.gate == null) {
			Gateways.LOGGER.error("Invalid gateway at {} will be removed.", this.position());
			this.remove();
		}
		long[] entities = tag.getLongArray("wave_entities");
		for (int i = 0; i < entities.length; i += 2) {
			unresolvedWaveEntities.add(new UUID(entities[i], entities[i + 1]));
		}
		this.entityData.set(WAVE_ACTIVE, tag.getBoolean("active"));
		this.entityData.set(TICKS_ACTIVE, tag.getInt("ticks_active"));
		if (tag.contains("summoner")) this.summonerId = tag.getUUID("summoner");
		ListNBT stacks = tag.getList("queued_stacks", Constants.NBT.TAG_COMPOUND);
		for (INBT inbt : stacks) {
			undroppedItems.add(ItemStack.of((CompoundNBT) inbt));
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
	public IPacket<?> getAddEntityPacket() {
		return NetworkHooks.getEntitySpawningPacket(this);
	}

	@Override
	public void startSeenByPlayer(ServerPlayerEntity player) {
		super.startSeenByPlayer(player);
		this.bossEvent.addPlayer(player);
	}

	@Override
	public void stopSeenByPlayer(ServerPlayerEntity player) {
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

	public ServerBossInfo getBossInfo() {
		return bossEvent;
	}

	public float getClientScale() {
		return this.clientScale;
	}

	public void setClientScale(float clientScale) {
		this.clientScale = clientScale;
	}

	public static void spawnLightningOn(Entity entity, boolean effectOnly) {
		LightningBoltEntity bolt = EntityType.LIGHTNING_BOLT.create(entity.level);
		bolt.setPos(entity.getX(), entity.getY(), entity.getZ());
		bolt.setVisualOnly(effectOnly);
		entity.level.addFreshEntity(bolt);
	}

	public void spawnParticle(Color color, double x, double y, double z, int type) {
		NetworkUtils.sendToTracking(Gateways.CHANNEL, new ParticleMessage(this, x, y, z, color, type), (ServerWorld) level, new BlockPos((int) x, (int) y, (int) z));
	}

	public void spawnItem(ItemStack stack) {
		ItemEntity i = new ItemEntity(level, 0, 0, 0, stack);
		i.setPos(this.getX() + MathHelper.nextDouble(random, -0.5, 0.5), this.getY() + 1.5, this.getZ() + MathHelper.nextDouble(random, -0.5, 0.5));
		i.setDeltaMovement(MathHelper.nextDouble(random, -0.15, 0.15), 0.4, MathHelper.nextDouble(random, -0.15, 0.15));
		level.addFreshEntity(i);
		this.level.playSound(null, i.getX(), i.getY(), i.getZ(), GatewayObjects.GATE_WARP, SoundCategory.HOSTILE, 0.25F, 2.0F);
	}

	public void spawnCompletionItem(ItemStack stack) {
		ItemEntity i = new ItemEntity(level, 0, 0, 0, stack);
		double variance = 0.05F * this.gate.getSize().getScale();
		i.setPos(this.getX(), this.getY() + this.getBbHeight() / 2, this.getZ());
		i.setDeltaMovement(MathHelper.nextDouble(random, -variance, variance), this.getBbHeight() / 20F, MathHelper.nextDouble(random, -variance, variance));
		i.setExtendedLifetime();
		level.addFreshEntity(i);
	}

	@Override
	public void writeSpawnData(PacketBuffer buf) {
		buf.writeResourceLocation(this.gate.getId());
	}

	@Override
	public void readSpawnData(PacketBuffer buf) {
		this.gate = GatewayManager.INSTANCE.getValue(buf.readResourceLocation());
		if (this.gate == null) throw new RuntimeException("Invalid gateway received on client!");
	}

	@Override
	protected int getPermissionLevel() {
		return 2;
	}

	public static enum GatewaySize {
		SMALL(EntitySize.scalable(2F, 3F), 1F),
		MEDIUM(EntitySize.scalable(4F, 6F), 2F),
		LARGE(EntitySize.scalable(6F, 9F), 3F);

		private final EntitySize dims;
		private final float scale;

		GatewaySize(EntitySize dims, float scale) {
			this.dims = dims;
			this.scale = scale;
		}

		public float getScale() {
			return this.scale;
		}
	}

}
