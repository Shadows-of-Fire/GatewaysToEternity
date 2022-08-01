package shadows.gateways.entity;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ILivingEntityData;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.effect.LightningBoltEntity;
import net.minecraft.entity.item.ExperienceOrbEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.WeightedSpawnerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.BossInfo;
import net.minecraft.world.BossInfo.Color;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerBossInfo;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.network.NetworkHooks;
import shadows.gateways.GatewayObjects;
import shadows.gateways.GatewaysToEternity;
import shadows.gateways.net.ParticleMessage;
import shadows.gateways.util.BossColorMap;
import shadows.placebo.util.NetworkUtils;
import shadows.placebo.util.TagBuilder;

public abstract class AbstractGatewayEntity extends Entity implements IEntityAdditionalSpawnData {

	public static final Method DROP_LOOT = ObfuscationReflectionHelper.findMethod(LivingEntity.class, "dropFromLootTable", DamageSource.class, boolean.class);
	public static final DataParameter<Boolean> WAVE_ACTIVE = EntityDataManager.defineId(AbstractGatewayEntity.class, DataSerializers.BOOLEAN);
	public static final DataParameter<Byte> WAVE = EntityDataManager.defineId(AbstractGatewayEntity.class, DataSerializers.BYTE);

	protected final ServerBossInfo bossInfo = this.createBossInfo();
	protected final GatewayStats stats = this.createStats();

	protected WeightedSpawnerEntity entity = new WeightedSpawnerEntity(1, TagBuilder.getDefaultTag(EntityType.ZOMBIE));
	protected final Set<LivingEntity> currentWaveEntities = new HashSet<>();
	protected final Set<UUID> unresolvedWaveEntities = new HashSet<>();
	protected int completionXP = 150;
	protected int maxWaveTime = 600;
	protected UUID summonerId;

	protected int ticksActive = 0;
	protected int ticksInactive = 0;
	protected int clientTickCounter = -1;
	protected Queue<ItemStack> undroppedItems = new ArrayDeque<>();

	/**
	 * Primary constructor.
	 */
	public AbstractGatewayEntity(EntityType<?> type, World world, PlayerEntity placer, ItemStack source) {
		super(type, world);
		CompoundNBT tag = source.getTag().getCompound("gateway_data");
		this.readAdditionalSaveData(tag);
		this.summonerId = placer.getUUID();
	}

	/**
	 * Client/Load constructor.
	 */
	public AbstractGatewayEntity(EntityType<?> type, World world) {
		super(type, world);
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

			if (this.tickCount % 20 == 0) {
				spawnParticle(this.bossInfo.getColor(), this.getX(), this.getY() + 1.5F, this.getZ(), 1);
			}

			if (isWaveActive()) {
				float progress = 1F - (getWave() - 1F) / this.stats.maxWaves;
				progress -= (float) this.ticksActive / this.maxWaveTime * (1F / this.stats.maxWaves);
				this.bossInfo.setPercent(progress);
				if (this.ticksActive++ > this.maxWaveTime) {
					this.onWaveTimerElapsed(getWave(), this.currentWaveEntities);
					return;
				}
			}

			boolean active = isWaveActive();
			if (active && this.currentWaveEntities.stream().noneMatch(Entity::isAlive)) {
				byte wave = getWave();
				this.onWaveEnd(wave);
				this.bossInfo.setPercent(1F - (float) wave / this.stats.maxWaves);
				this.currentWaveEntities.clear();
				this.entityData.set(WAVE_ACTIVE, false);
				if (wave == this.stats.maxWaves) {
					this.onLastWaveEnd();
				}
				this.ticksActive = 0;
			} else if (!active) {
				if (this.ticksInactive++ > this.stats.pauseTime && this.getWave() != this.stats.maxWaves) {
					this.spawnWave();
					return;
				}
			}

			if (this.tickCount % 4 == 0 && !undroppedItems.isEmpty()) {
				spawnItem(undroppedItems.remove());
			}

			if (!active && undroppedItems.isEmpty() && this.getWave() == this.stats.maxWaves) {
				this.completePortal();
			}
		}
	}

	public void spawnWave() {
		BlockPos blockpos = this.blockPosition();

		for (int i = 0; i < this.stats.entitiesPerWave; ++i) {
			CompoundNBT compoundnbt = this.entity.getTag();
			Optional<EntityType<?>> optional = EntityType.by(compoundnbt);
			if (!optional.isPresent()) {
				this.remove();
				GatewaysToEternity.LOGGER.error("GatewayEntity - Failed to read the entity type from spawn data: " + compoundnbt);
				return;
			}

			ListNBT listnbt = compoundnbt.getList("Pos", 6);
			int j = listnbt.size();
			int tries = 0;
			double x = j >= 1 ? listnbt.getDouble(0) : blockpos.getX() + (level.random.nextDouble() - level.random.nextDouble()) * this.stats.spawnRange + 0.5D;
			double y = j >= 2 ? listnbt.getDouble(1) : (double) (blockpos.getY() + level.random.nextInt(3) - 1);
			double z = j >= 3 ? listnbt.getDouble(2) : blockpos.getZ() + (level.random.nextDouble() - level.random.nextDouble()) * this.stats.spawnRange + 0.5D;
			while (!level.noCollision(optional.get().getAABB(x, y, z))) {
				x = j >= 1 ? listnbt.getDouble(0) : blockpos.getX() + (level.random.nextDouble() - level.random.nextDouble()) * this.stats.spawnRange + 0.5D;
				y = j >= 2 ? listnbt.getDouble(1) : (double) (blockpos.getY() + level.random.nextInt(3) - 1);
				z = j >= 3 ? listnbt.getDouble(2) : blockpos.getZ() + (level.random.nextDouble() - level.random.nextDouble()) * this.stats.spawnRange + 0.5D;
				if (tries++ >= 4) {
					break;
				}
			}

			final double fx = x, fy = y, fz = z;

			if (level.noCollision(optional.get().getAABB(x, y, z))) {
				Entity entity = EntityType.loadEntityRecursive(compoundnbt, level, (p_221408_6_) -> {
					p_221408_6_.moveTo(fx, fy, fz, p_221408_6_.yRot, p_221408_6_.xRot);
					return p_221408_6_;
				});

				if (!(entity instanceof LivingEntity)) {
					this.remove();
					GatewaysToEternity.LOGGER.error("Failed to create a living entity from spawn data: " + compoundnbt);
					return;
				}

				modifyEntityForWave(getWave() + 1, (LivingEntity) entity);

				entity.moveTo(entity.getX(), entity.getY(), entity.getZ(), level.random.nextFloat() * 360.0F, 0.0F);
				if (entity instanceof MobEntity) {
					MobEntity mobentity = (MobEntity) entity;

					if (this.entity.getTag().size() == 1 && this.entity.getTag().contains("id", 8) && !ForgeEventFactory.doSpecialSpawn((MobEntity) entity, level, (float) entity.getX(), (float) entity.getY(), (float) entity.getZ(), null, SpawnReason.NATURAL)) {
						mobentity.finalizeSpawn((ServerWorld) level, level.getCurrentDifficultyAt(entity.blockPosition()), SpawnReason.NATURAL, (ILivingEntityData) null, (CompoundNBT) null);
					}
				}

				this.spawnEntity(entity);
				this.level.playSound(null, this.getX(), this.getY(), this.getZ(), GatewayObjects.GATE_WARP, SoundCategory.HOSTILE, 0.5F, 1);
				this.currentWaveEntities.add((LivingEntity) entity);
				spawnParticle(this.bossInfo.getColor(), entity.getX() + entity.getBbWidth() / 2, entity.getY() + entity.getBbHeight() / 2, entity.getZ() + entity.getBbWidth() / 2, 0);
			} else {
				this.remove();
			}
		}
		this.entityData.set(WAVE, (byte) (getWave() + 1));
		this.entityData.set(WAVE_ACTIVE, true);
		this.ticksInactive = 0;
		this.onWaveStart(getWave(), this.currentWaveEntities);
	}

	protected void spawnEntity(Entity entity) {
		if (this.level.addFreshEntity(entity)) {
			for (Entity e : entity.getPassengers()) {
				this.spawnEntity(e);
			}
		}
	}

	protected void onLastWaveEnd() {
		while (completionXP > 0) {
			int i = 5;
			completionXP -= i;
			this.level.addFreshEntity(new ExperienceOrbEntity(this.level, this.getX(), this.getY(), this.getZ(), i));
		}
	}

	protected void onWaveStart(int wave, Set<LivingEntity> spawned) {

	}

	protected void completePortal() {
		this.remove();
		this.playSound(GatewayObjects.GATE_END, 1, 1);
	}

	public void onGateCreated() {
		this.playSound(GatewayObjects.GATE_START, 1, 1);
	}

	protected void onWaveEnd(int wave) {
		PlayerEntity player = this.summonerId == null ? null : level.getPlayerByUUID(summonerId);
		if (player == null) {
			player = level.getNearestPlayer(this, 50);
		}
		try {
			Entity entity = EntityType.loadEntityRecursive(this.entity.getTag(), level, (p_221408_6_) -> {
				p_221408_6_.moveTo(this.getX(), this.getY(), this.getZ(), p_221408_6_.yRot, p_221408_6_.xRot);
				return p_221408_6_;
			});
			List<ItemEntity> items = new ArrayList<>();
			entity.hurt(DamageSource.playerAttack(player).bypassMagic().bypassInvul().bypassArmor(), 1);
			entity.captureDrops(items);
			this.dropBonusLoot(player, (LivingEntity) entity);
			items.stream().map(ItemEntity::getItem).forEach(undroppedItems::add);
		} catch (Exception e) {
			e.printStackTrace();
			this.remove();
		}
	}

	protected abstract void modifyEntityForWave(int wave, LivingEntity entity);

	protected void onWaveTimerElapsed(int wave, Set<LivingEntity> remaining) {
		spawnLightningOn(this, false);
		remaining.stream().filter(Entity::isAlive).forEach(e -> spawnLightningOn(e, true));
		remaining.forEach(Entity::remove);
		this.remove();
	}

	protected abstract ServerBossInfo createBossInfo();

	protected abstract GatewayStats createStats();

	protected abstract GatewaySize getSize();

	protected void dropBonusLoot(PlayerEntity player, LivingEntity entity) throws Exception {
		for (int i = 0; i < this.stats.entitiesPerWave * getWave(); i++) {
			DROP_LOOT.invoke(entity, DamageSource.playerAttack(player), true);
		}
	}

	@Override
	protected void readAdditionalSaveData(CompoundNBT tag) {
		this.entityData.set(WAVE, tag.getByte("wave"));
		this.entity = new WeightedSpawnerEntity(tag.getCompound("entity"));
		long[] entities = tag.getLongArray("wave_entities");
		for (int i = 0; i < entities.length; i += 2) {
			unresolvedWaveEntities.add(new UUID(entities[i], entities[i + 1]));
		}
		this.entityData.set(WAVE_ACTIVE, tag.getBoolean("active"));
		this.ticksInactive = tag.getShort("ticks_inactive");
		this.ticksActive = tag.getInt("ticks_active");
		this.completionXP = tag.getInt("completion_xp");
		if (tag.contains("summoner")) this.summonerId = tag.getUUID("summoner");
		this.bossInfo.setName(new TranslationTextComponent(tag.getString("name")));
		this.maxWaveTime = tag.getInt("max_wave_time");
		this.bossInfo.setPercent(1F - (float) getWave() / this.stats.maxWaves);
		ListNBT stacks = tag.getList("queued_stacks", Constants.NBT.TAG_COMPOUND);
		for (INBT inbt : stacks) {
			undroppedItems.add(ItemStack.of((CompoundNBT) inbt));
		}
		this.bossInfo.setColor(Color.byName(tag.getString("color")));
	}

	@Override
	protected void addAdditionalSaveData(CompoundNBT tag) {
		tag.putByte("wave", getWave());
		tag.put("entity", this.entity.save());
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
		tag.putShort("ticks_inactive", (short) getTicksInactive());
		tag.putInt("completion_xp", this.completionXP);
		if (this.summonerId != null) tag.putUUID("summoner", this.summonerId);
		tag.putString("name", this.bossInfo.getName() == null ? "entity.gateways.gateway" : ((TranslationTextComponent) this.bossInfo.getName()).getKey());
		tag.putInt("max_wave_time", this.maxWaveTime);
		ListNBT stacks = new ListNBT();
		for (ItemStack s : this.undroppedItems) {
			stacks.add(s.serializeNBT());
		}
		tag.put("queued_stacks", stacks);
		tag.putString("color", bossInfo.getColor().getName());
	}

	@Override
	protected void defineSynchedData() {
		this.entityData.define(WAVE_ACTIVE, false);
		this.entityData.define(WAVE, (byte) 0);
	}

	@Override
	public IPacket<?> getAddEntityPacket() {
		return NetworkHooks.getEntitySpawningPacket(this);
	}

	@Override
	public void startSeenByPlayer(ServerPlayerEntity player) {
		super.startSeenByPlayer(player);
		this.bossInfo.addPlayer(player);
	}

	@Override
	public void stopSeenByPlayer(ServerPlayerEntity player) {
		super.stopSeenByPlayer(player);
		this.bossInfo.removePlayer(player);
	}

	public int getTicksActive() {
		return ticksActive;
	}

	public int getTicksInactive() {
		return ticksInactive;
	}

	public boolean isWaveActive() {
		return this.entityData.get(WAVE_ACTIVE);
	}

	public byte getWave() {
		return this.entityData.get(WAVE);
	}

	public GatewayStats getStats() {
		return stats;
	}

	public BossInfo getBossInfo() {
		return bossInfo;
	}

	public int getClientTicks() {
		return this.clientTickCounter;
	}

	public void setClientTicks(int ticks) {
		this.clientTickCounter = ticks;
	}

	public static void spawnLightningOn(Entity entity, boolean effectOnly) {
		LightningBoltEntity bolt = EntityType.LIGHTNING_BOLT.create(entity.level);
		bolt.setPos(entity.getX(), entity.getY(), entity.getZ());
		bolt.setVisualOnly(effectOnly);
		entity.level.addFreshEntity(bolt);
	}

	public void spawnParticle(Color color, double x, double y, double z, int type) {
		int cInt = BossColorMap.getColor(this.getBossInfo());
		NetworkUtils.sendToTracking(GatewaysToEternity.CHANNEL, new ParticleMessage(this, x, y, z, cInt, type), (ServerWorld) level, new BlockPos((int) x, (int) y, (int) z));
	}

	public void spawnItem(ItemStack stack) {
		ItemEntity i = new ItemEntity(level, 0, 0, 0, stack);
		i.setPos(this.getX() + MathHelper.nextDouble(random, -0.5, 0.5), this.getY() + 1.5, this.getZ() + MathHelper.nextDouble(random, -0.5, 0.5));
		i.setDeltaMovement(MathHelper.nextDouble(random, -0.15, 0.15), 0.4, MathHelper.nextDouble(random, -0.15, 0.15));
		level.addFreshEntity(i);
		this.level.playSound(null, i.getX(), i.getY(), i.getZ(), GatewayObjects.GATE_WARP, SoundCategory.HOSTILE, 0.75F, 2.0F);
	}

	@Override
	public void writeSpawnData(PacketBuffer buf) {
		buf.writeUtf(this.bossInfo.getColor().getName());
	}

	@Override
	public void readSpawnData(PacketBuffer buf) {
		this.bossInfo.setColor(Color.byName(buf.readUtf()));
	}

	public class GatewayStats {
		public final int maxWaves, entitiesPerWave, spawnRange, pauseTime;

		public GatewayStats(int maxWaves, int entitiesPerWave, int spawnRange, int pauseTime) {
			this.maxWaves = maxWaves;
			this.entitiesPerWave = entitiesPerWave;
			this.spawnRange = spawnRange;
			this.pauseTime = pauseTime;
		}
	}

	public enum GatewaySize {
		SMALL,
		MEDIUM,
		LARGE;
	}

}
