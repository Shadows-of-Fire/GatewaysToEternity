package shadows.gateways.entity;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
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
import net.minecraft.nbt.ListNBT;
import net.minecraft.network.IPacket;
import net.minecraft.util.DamageSource;
import net.minecraft.util.WeightedSpawnerEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerBossInfo;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fml.network.NetworkHooks;
import shadows.gateways.GatewaysToEternity;
import shadows.gateways.util.TagBuilder;
import shadows.placebo.util.ReflectionHelper;

public abstract class AbstractGatewayEntity extends Entity {

	protected final ServerBossInfo bossInfo = this.createBossInfo();
	protected final GatewayStats stats = this.createStats();

	protected WeightedSpawnerEntity entity = new WeightedSpawnerEntity(1, TagBuilder.getDefaultTag(EntityType.ZOMBIE));
	protected final Set<LivingEntity> currentWaveEntities = new HashSet<>();
	protected final Set<UUID> unresolvedWaveEntities = new HashSet<>();
	protected int completionXP = 150;
	protected int maxWaveTime = 600;
	protected UUID summonerId;

	protected boolean waveActive = false;
	protected int ticksInactive = 0;
	protected int currentWaveTicks = 0;
	protected int wave = 0;

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

			if (waveActive) {
				float progress = 1F - (this.wave - 1F) / this.stats.maxWaves;
				progress -= (float) this.currentWaveTicks / this.maxWaveTime * (1F / this.stats.maxWaves);
				this.bossInfo.setPercent(progress);
				if (this.currentWaveTicks++ > this.maxWaveTime) {
					this.onWaveTimerElapsed(this.wave, this.currentWaveEntities);
				}
			}

			if (waveActive && this.currentWaveEntities.stream().noneMatch(Entity::isAlive)) {
				this.onWaveEnd(this.wave);
				this.bossInfo.setPercent(1F - (float) this.wave / this.stats.maxWaves);
				this.currentWaveEntities.clear();
				this.waveActive = false;
				if (this.wave == this.stats.maxWaves) {
					this.onPortalCompletion();
				}
				this.currentWaveTicks = 0;
			} else if (!waveActive) {
				if (this.ticksInactive++ > this.stats.pauseTime) {
					this.spawnWave();
				}
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
			while (!world.doesNotCollide(optional.get().func_220328_a(x, y, z))) {
				x = j >= 1 ? listnbt.getDouble(0) : blockpos.getX() + (world.rand.nextDouble() - world.rand.nextDouble()) * this.stats.spawnRange + 0.5D;
				y = j >= 2 ? listnbt.getDouble(1) : (double) (blockpos.getY() + world.rand.nextInt(3) - 1);
				z = j >= 3 ? listnbt.getDouble(2) : blockpos.getZ() + (world.rand.nextDouble() - world.rand.nextDouble()) * this.stats.spawnRange + 0.5D;
				if (tries++ >= 4) {
					break;
				}
			}

			final double fx = x, fy = y, fz = z;

			if (world.doesNotCollide(optional.get().func_220328_a(x, y, z))) {
				Entity entity = EntityType.func_220335_a(compoundnbt, world, (p_221408_6_) -> {
					p_221408_6_.setLocationAndAngles(fx, fy, fz, p_221408_6_.rotationYaw, p_221408_6_.rotationPitch);
					return p_221408_6_;
				});

				if (!(entity instanceof LivingEntity)) {
					this.remove();
					GatewaysToEternity.LOGGER.error("Failed to create a living entity from spawn data: " + compoundnbt);
					return;
				}

				modifyEntityForWave(wave + 1, (LivingEntity) entity);

				entity.setLocationAndAngles(entity.getX(), entity.getY(), entity.getZ(), world.rand.nextFloat() * 360.0F, 0.0F);
				if (entity instanceof MobEntity) {
					MobEntity mobentity = (MobEntity) entity;

					if (this.entity.getNbt().size() == 1 && this.entity.getNbt().contains("id", 8) && !ForgeEventFactory.doSpecialSpawn((MobEntity) entity, world, (float) entity.getX(), (float) entity.getY(), (float) entity.getZ(), null, SpawnReason.MOB_SUMMONED)) {
						mobentity.onInitialSpawn(world, world.getDifficultyForLocation(new BlockPos(entity)), SpawnReason.MOB_SUMMONED, (ILivingEntityData) null, (CompoundNBT) null);
					}
				}

				this.spawnEntity(entity);
				this.currentWaveEntities.add((LivingEntity) entity);
			} else {
				this.remove();
			}
		}
		this.wave++;
		this.waveActive = true;
		this.ticksInactive = 0;
		this.onWaveStart(this.wave, this.currentWaveEntities);
	}

	protected void spawnEntity(Entity entity) {
		if (this.world.addEntity(entity)) {
			for (Entity e : entity.getPassengers()) {
				this.spawnEntity(e);
			}

		}
	}

	protected void onPortalCompletion() {
		while (completionXP > 0) {
			int i = ExperienceOrbEntity.getXPSplit(completionXP);
			completionXP -= i;
			this.world.addEntity(new ExperienceOrbEntity(this.world, this.getX(), this.getY(), this.getZ(), i));
		}
		this.remove();
	}

	protected void onWaveStart(int wave, Set<LivingEntity> spawned) {

	}

	static Method dropLoot;

	protected void onWaveEnd(int wave) {
		PlayerEntity p = world.getPlayerByUuid(summonerId);
		if (dropLoot == null) {
			dropLoot = ReflectionHelper.findMethod(LivingEntity.class, "dropLoot", "func_213354_a", DamageSource.class, boolean.class);
		}
		try {
			Entity entity = EntityType.func_220335_a(this.entity.getNbt(), world, (p_221408_6_) -> {
				p_221408_6_.setLocationAndAngles(this.getX(), this.getY(), this.getZ(), p_221408_6_.rotationYaw, p_221408_6_.rotationPitch);
				return p_221408_6_;
			});
			List<ItemEntity> items = new ArrayList<>();
			entity.captureDrops(items);
			for (int i = 0; i < this.currentWaveEntities.size() * wave; i++) {
				dropLoot.invoke(entity, DamageSource.causePlayerDamage(p), true);
			}
			items.forEach(i -> {
				i.setPosition(this.getX(), this.getY() + 1.5, this.getZ());
				i.setVelocity(MathHelper.nextDouble(rand, -0.15, 0.15), 0.4, MathHelper.nextDouble(rand, -0.15, 0.15));
				world.addEntity(i);
			});
		} catch (Exception e) {
			e.printStackTrace();
			this.remove();
		}
	}

	protected abstract void modifyEntityForWave(int wave, LivingEntity entity);

	protected void onWaveTimerElapsed(int wave, Set<LivingEntity> remaining) {
		world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(this.getPosition()).grow(15)).forEach(AbstractGatewayEntity::spawnLightningOn);
		remaining.forEach(Entity::remove);
		this.remove();
	}

	protected abstract ServerBossInfo createBossInfo();

	protected abstract GatewayStats createStats();

	protected abstract GatewaySize getSize();

	@Override
	protected void readAdditional(CompoundNBT tag) {
		this.wave = tag.getByte("wave");
		this.entity = new WeightedSpawnerEntity(tag.getCompound("entity"));
		long[] entities = tag.getLongArray("wave_entities");
		for (int i = 0; i < entities.length; i += 2) {
			unresolvedWaveEntities.add(new UUID(entities[i], entities[i + 1]));
		}
		this.waveActive = tag.getBoolean("active");
		this.ticksInactive = tag.getShort("ticks_inactive");
		this.completionXP = tag.getInt("completion_xp");
		this.summonerId = tag.getUniqueId("summoner");
		this.bossInfo.setName(new TranslationTextComponent(tag.getString("name")));
		this.maxWaveTime = tag.getInt("max_wave_time");
		this.currentWaveTicks = tag.getInt("current_wave_ticks");
		this.bossInfo.setPercent(1F - (float) this.wave / this.stats.maxWaves);
	}

	@Override
	protected void writeAdditional(CompoundNBT tag) {
		tag.putByte("wave", (byte) this.wave);
		tag.put("entity", this.entity.toCompoundTag());
		long[] ids = new long[this.currentWaveEntities.size() * 2];
		int idx = 0;
		for (LivingEntity e : this.currentWaveEntities) {
			UUID id = e.getUniqueID();
			ids[idx++] = id.getMostSignificantBits();
			ids[idx++] = id.getLeastSignificantBits();
		}
		tag.putLongArray("wave_entities", ids);
		tag.putBoolean("active", this.waveActive);
		tag.putShort("ticks_inactive", (short) this.ticksInactive);
		tag.putInt("completion_xp", this.completionXP);
		tag.putUniqueId("summoner", this.summonerId);
		tag.putString("name", this.bossInfo.getName() == null ? "entity.gateways.gateway" : ((TranslationTextComponent) this.bossInfo.getName()).getKey());
		tag.putInt("max_wave_time", this.maxWaveTime);
		tag.putInt("current_wave_ticks", this.currentWaveTicks);
	}

	@Override
	protected void registerData() {
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

	public static void spawnLightningOn(Entity entity) {
		((ServerWorld) entity.world).addLightningBolt(new LightningBoltEntity(entity.world, entity.getX(), entity.getY(), entity.getZ(), false));
	}

	public class GatewayStats {
		protected int maxWaves, entitiesPerWave, spawnRange, pauseTime;

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
