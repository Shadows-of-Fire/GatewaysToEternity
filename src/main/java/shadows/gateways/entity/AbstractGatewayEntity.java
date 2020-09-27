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

	public static final Method DROP_LOOT = ObfuscationReflectionHelper.findMethod(LivingEntity.class, "func_213354_a", DamageSource.class, boolean.class);
	public static final DataParameter<Boolean> WAVE_ACTIVE = EntityDataManager.createKey(AbstractGatewayEntity.class, DataSerializers.BOOLEAN);
	public static final DataParameter<Byte> WAVE = EntityDataManager.createKey(AbstractGatewayEntity.class, DataSerializers.BYTE);

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
		this.readAdditional(tag);
		this.summonerId = placer.getUniqueID();
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
		if (!world.isRemote) {
			if (!unresolvedWaveEntities.isEmpty()) {
				for (UUID id : unresolvedWaveEntities) {
					Entity e = ((ServerWorld) world).getEntityByUuid(id);
					if (e instanceof LivingEntity) this.currentWaveEntities.add((LivingEntity) e);
				}
				unresolvedWaveEntities.clear();
			}

			if (this.ticksExisted % 20 == 0) {
				spawnParticle(this.bossInfo.getColor(), this.getPosX(), this.getPosY() + 1.5F, this.getPosZ(), 1);
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
				this.dataManager.set(WAVE_ACTIVE, false);
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

			if (this.ticksExisted % 4 == 0 && !undroppedItems.isEmpty()) {
				spawnItem(undroppedItems.remove());
			}

			if (!active && undroppedItems.isEmpty() && this.getWave() == this.stats.maxWaves) {
				this.completePortal();
			}
		}

	}

	public void spawnWave() {
		BlockPos blockpos = this.getPosition();

		for (int i = 0; i < this.stats.entitiesPerWave; ++i) {
			CompoundNBT compoundnbt = this.entity.getNbt();
			Optional<EntityType<?>> optional = EntityType.readEntityType(compoundnbt);
			if (!optional.isPresent()) {
				this.remove();
				GatewaysToEternity.LOGGER.error("GatewayEntity - Failed to read the entity type from spawn data: " + compoundnbt);
				return;
			}

			ListNBT listnbt = compoundnbt.getList("Pos", 6);
			int j = listnbt.size();
			int tries = 0;
			double x = j >= 1 ? listnbt.getDouble(0) : blockpos.getX() + (world.rand.nextDouble() - world.rand.nextDouble()) * this.stats.spawnRange + 0.5D;
			double y = j >= 2 ? listnbt.getDouble(1) : (double) (blockpos.getY() + world.rand.nextInt(3) - 1);
			double z = j >= 3 ? listnbt.getDouble(2) : blockpos.getZ() + (world.rand.nextDouble() - world.rand.nextDouble()) * this.stats.spawnRange + 0.5D;
			while (!world.hasNoCollisions(optional.get().getBoundingBoxWithSizeApplied(x, y, z))) {
				x = j >= 1 ? listnbt.getDouble(0) : blockpos.getX() + (world.rand.nextDouble() - world.rand.nextDouble()) * this.stats.spawnRange + 0.5D;
				y = j >= 2 ? listnbt.getDouble(1) : (double) (blockpos.getY() + world.rand.nextInt(3) - 1);
				z = j >= 3 ? listnbt.getDouble(2) : blockpos.getZ() + (world.rand.nextDouble() - world.rand.nextDouble()) * this.stats.spawnRange + 0.5D;
				if (tries++ >= 4) {
					break;
				}
			}

			final double fx = x, fy = y, fz = z;

			if (world.hasNoCollisions(optional.get().getBoundingBoxWithSizeApplied(x, y, z))) {
				Entity entity = EntityType.loadEntityAndExecute(compoundnbt, world, (p_221408_6_) -> {
					p_221408_6_.setLocationAndAngles(fx, fy, fz, p_221408_6_.rotationYaw, p_221408_6_.rotationPitch);
					return p_221408_6_;
				});

				if (!(entity instanceof LivingEntity)) {
					this.remove();
					GatewaysToEternity.LOGGER.error("Failed to create a living entity from spawn data: " + compoundnbt);
					return;
				}

				modifyEntityForWave(getWave() + 1, (LivingEntity) entity);

				entity.setLocationAndAngles(entity.getPosX(), entity.getPosY(), entity.getPosZ(), world.rand.nextFloat() * 360.0F, 0.0F);
				if (entity instanceof MobEntity) {
					MobEntity mobentity = (MobEntity) entity;

					if (this.entity.getNbt().size() == 1 && this.entity.getNbt().contains("id", 8) && !ForgeEventFactory.doSpecialSpawn((MobEntity) entity, world, (float) entity.getPosX(), (float) entity.getPosY(), (float) entity.getPosZ(), null, SpawnReason.MOB_SUMMONED)) {
						mobentity.onInitialSpawn((ServerWorld) world, world.getDifficultyForLocation(entity.getPosition()), SpawnReason.MOB_SUMMONED, (ILivingEntityData) null, (CompoundNBT) null);
					}
				}

				this.spawnEntity(entity);
				this.world.playSound(null, this.getPosX(), this.getPosY(), this.getPosZ(), GatewayObjects.GATE_WARP, SoundCategory.HOSTILE, 0.5F, 1);
				this.currentWaveEntities.add((LivingEntity) entity);
				spawnParticle(this.bossInfo.getColor(), entity.getPosX() + entity.getWidth() / 2, entity.getPosY() + entity.getHeight() / 2, entity.getPosZ() + entity.getWidth() / 2, 0);
			} else {
				this.remove();
			}
		}
		this.dataManager.set(WAVE, (byte) (getWave() + 1));
		this.dataManager.set(WAVE_ACTIVE, true);
		this.ticksInactive = 0;
		this.onWaveStart(getWave(), this.currentWaveEntities);
	}

	protected void spawnEntity(Entity entity) {
		if (this.world.addEntity(entity)) {
			for (Entity e : entity.getPassengers()) {
				this.spawnEntity(e);
			}
		}
	}

	protected void onLastWaveEnd() {
		while (completionXP > 0) {
			int i = 5;
			completionXP -= i;
			this.world.addEntity(new ExperienceOrbEntity(this.world, this.getPosX(), this.getPosY(), this.getPosZ(), i));
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
		PlayerEntity player = world.getPlayerByUuid(summonerId);
		if (player == null) {
			player = world.getClosestPlayer(this, 50);
		}
		try {
			Entity entity = EntityType.loadEntityAndExecute(this.entity.getNbt(), world, (p_221408_6_) -> {
				p_221408_6_.setLocationAndAngles(this.getPosX(), this.getPosY(), this.getPosZ(), p_221408_6_.rotationYaw, p_221408_6_.rotationPitch);
				return p_221408_6_;
			});
			List<ItemEntity> items = new ArrayList<>();
			entity.attackEntityFrom(DamageSource.causePlayerDamage(player).setDamageIsAbsolute().setDamageAllowedInCreativeMode().setDamageBypassesArmor(), 1);
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
			DROP_LOOT.invoke(entity, DamageSource.causePlayerDamage(player), true);
		}
	}

	@Override
	protected void readAdditional(CompoundNBT tag) {
		this.dataManager.set(WAVE, tag.getByte("wave"));
		this.entity = new WeightedSpawnerEntity(tag.getCompound("entity"));
		long[] entities = tag.getLongArray("wave_entities");
		for (int i = 0; i < entities.length; i += 2) {
			unresolvedWaveEntities.add(new UUID(entities[i], entities[i + 1]));
		}
		this.dataManager.set(WAVE_ACTIVE, tag.getBoolean("active"));
		this.ticksInactive = tag.getShort("ticks_inactive");
		this.ticksActive = tag.getInt("ticks_active");
		this.completionXP = tag.getInt("completion_xp");
		if (tag.contains("summoner")) this.summonerId = tag.getUniqueId("summoner");
		this.bossInfo.setName(new TranslationTextComponent(tag.getString("name")));
		this.maxWaveTime = tag.getInt("max_wave_time");
		this.bossInfo.setPercent(1F - (float) getWave() / this.stats.maxWaves);
		ListNBT stacks = tag.getList("queued_stacks", Constants.NBT.TAG_COMPOUND);
		for (INBT inbt : stacks) {
			undroppedItems.add(ItemStack.read((CompoundNBT) inbt));
		}
		this.bossInfo.setColor(Color.byName(tag.getString("color")));
	}

	@Override
	protected void writeAdditional(CompoundNBT tag) {
		tag.putByte("wave", getWave());
		tag.put("entity", this.entity.toCompoundTag());
		long[] ids = new long[this.currentWaveEntities.size() * 2];
		int idx = 0;
		for (LivingEntity e : this.currentWaveEntities) {
			UUID id = e.getUniqueID();
			ids[idx++] = id.getMostSignificantBits();
			ids[idx++] = id.getLeastSignificantBits();
		}
		tag.putLongArray("wave_entities", ids);
		tag.putBoolean("active", isWaveActive());
		tag.putInt("ticks_active", getTicksActive());
		tag.putShort("ticks_inactive", (short) getTicksInactive());
		tag.putInt("completion_xp", this.completionXP);
		tag.putUniqueId("summoner", this.summonerId);
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
	protected void registerData() {
		this.dataManager.register(WAVE_ACTIVE, false);
		this.dataManager.register(WAVE, (byte) 0);
	}

	@Override
	public IPacket<?> createSpawnPacket() {
		return NetworkHooks.getEntitySpawningPacket(this);
	}

	@Override
	public void addTrackingPlayer(ServerPlayerEntity player) {
		super.addTrackingPlayer(player);
		this.bossInfo.addPlayer(player);
	}

	@Override
	public void removeTrackingPlayer(ServerPlayerEntity player) {
		super.removeTrackingPlayer(player);
		this.bossInfo.removePlayer(player);
	}

	public int getTicksActive() {
		return ticksActive;
	}

	public int getTicksInactive() {
		return ticksInactive;
	}

	public boolean isWaveActive() {
		return this.dataManager.get(WAVE_ACTIVE);
	}

	public byte getWave() {
		return this.dataManager.get(WAVE);
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
		LightningBoltEntity bolt = EntityType.LIGHTNING_BOLT.create(entity.world);
		bolt.setPosition(entity.getPosX(), entity.getPosY(), entity.getPosZ());
		bolt.setEffectOnly(effectOnly);
		entity.world.addEntity(bolt);
	}

	public void spawnParticle(Color color, double x, double y, double z, int type) {
		int cInt = BossColorMap.getColor(this.getBossInfo());
		NetworkUtils.sendToTracking(GatewaysToEternity.CHANNEL, new ParticleMessage(this, x, y, z, cInt, type), (ServerWorld) world, new BlockPos((int) x, (int) y, (int) z));
	}

	public void spawnItem(ItemStack stack) {
		ItemEntity i = new ItemEntity(world, 0, 0, 0, stack);
		i.setPosition(this.getPosX() + MathHelper.nextDouble(rand, -0.5, 0.5), this.getPosY() + 1.5, this.getPosZ() + MathHelper.nextDouble(rand, -0.5, 0.5));
		i.setVelocity(MathHelper.nextDouble(rand, -0.15, 0.15), 0.4, MathHelper.nextDouble(rand, -0.15, 0.15));
		world.addEntity(i);
		this.world.playSound(null, i.getPosX(), i.getPosY(), i.getPosZ(), GatewayObjects.GATE_WARP, SoundCategory.HOSTILE, 0.75F, 2.0F);
	}

	@Override
	public void writeSpawnData(PacketBuffer buf) {
		buf.writeString(this.bossInfo.getColor().getName());
	}

	@Override
	public void readSpawnData(PacketBuffer buf) {
		this.bossInfo.setColor(Color.byName(buf.readString()));
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
