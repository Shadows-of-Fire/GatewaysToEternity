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
import net.minecraft.entity.item.ExperienceOrbEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.network.IPacket;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.DamageSource;
import net.minecraft.util.WeightedSpawnerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.BossInfo;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerBossInfo;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fml.network.NetworkHooks;
import shadows.gateways.GatewaysToEternity;
import shadows.gateways.util.TagBuilder;
import shadows.placebo.util.ReflectionHelper;

public class GatewayEntity extends Entity {

	protected final ServerBossInfo bossInfo = new ServerBossInfo(this.getDefaultName(), BossInfo.Color.BLUE, BossInfo.Overlay.PROGRESS);;

	protected int maxWaves = 6;
	protected int wave = 0;
	protected int entitiesPerWave = 3;
	protected int spawnRange = 5;
	protected WeightedSpawnerEntity entity = new WeightedSpawnerEntity(1, TagBuilder.getDefaultTag(EntityType.ZOMBIE));
	protected final Set<LivingEntity> currentWaveEntities = new HashSet<>();
	protected final Set<UUID> unresolvedWaveEntities = new HashSet<>();
	protected boolean waveActive = false;
	protected int ticksInactive = 0;
	protected int completionXP = 150;
	protected int wavePauseTime = 140;
	protected UUID summonerId;

	/**
	 * Primary constructor.
	 */
	public GatewayEntity(World world, PlayerEntity placer, ItemStack source) {
		super(GatewaysToEternity.GATEWAY_ENTITY, world);
		CompoundNBT tag = source.getTag().getCompound("gateway_data");
		this.readAdditional(tag);
		this.summonerId = placer.getUniqueID();
	}

	/**
	 * Client/Load constructor.
	 */
	public GatewayEntity(EntityType<?> type, World world) {
		super(type, world);
	}

	@Override
	protected void registerData() {
	}

	@Override
	protected void readAdditional(CompoundNBT tag) {
		this.maxWaves = tag.getByte("max_waves");
		this.wave = tag.getByte("wave");
		this.entitiesPerWave = tag.getByte("entities_per_wave");
		this.spawnRange = tag.getByte("spawn_range");
		this.entity = new WeightedSpawnerEntity(tag.getCompound("entity"));
		long[] entities = tag.getLongArray("wave_entities");
		for (int i = 0; i < entities.length; i += 2) {
			unresolvedWaveEntities.add(new UUID(entities[i], entities[i + 1]));
		}
		this.waveActive = tag.getBoolean("active");
		this.ticksInactive = tag.getShort("ticks_inactive");
		this.completionXP = tag.getInt("completion_xp");
		this.wavePauseTime = tag.getShort("wave_pause_time");
		this.summonerId = tag.getUniqueId("summoner");
		this.bossInfo.setName(new TranslationTextComponent(tag.getString("name")));
	}

	@Override
	protected void writeAdditional(CompoundNBT tag) {
		tag.putByte("max_waves", (byte) this.maxWaves);
		tag.putByte("wave", (byte) this.wave);
		tag.putByte("entities_per_wave", (byte) this.entitiesPerWave);
		tag.putByte("spawn_range", (byte) this.spawnRange);
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
		tag.putShort("wave_pause_time", (short) this.wavePauseTime);
		tag.putUniqueId("summoner", this.summonerId);
		tag.putString("name", this.bossInfo.getName() == null ? "entity.gateways.gateway" : ((TranslationTextComponent) this.bossInfo.getName()).getKey());
	}

	@Override
	public IPacket<?> createSpawnPacket() {
		return NetworkHooks.getEntitySpawningPacket(this);
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
				float progress = 1F - (this.wave - 1F) / this.maxWaves;
				progress -= ((this.entitiesPerWave - (float) this.currentWaveEntities.stream().filter(Entity::isAlive).count()) / this.entitiesPerWave) * (1F / this.maxWaves);
				this.bossInfo.setPercent(progress);
			}
			if (waveActive && this.currentWaveEntities.stream().noneMatch(Entity::isAlive)) {
				this.onWaveEnd(this.wave);
				this.currentWaveEntities.clear();
				this.waveActive = false;
				if (this.wave == this.maxWaves) {
					this.onPortalCompletion();
				}
			} else if (!waveActive) {
				if (this.ticksInactive++ > this.wavePauseTime) {
					this.spawnWave();
				}
			}
		}

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

	public void spawnWave() {
		BlockPos blockpos = this.getPosition();

		for (int i = 0; i < this.entitiesPerWave; ++i) {
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
			double x = j >= 1 ? listnbt.getDouble(0) : blockpos.getX() + (world.rand.nextDouble() - world.rand.nextDouble()) * this.spawnRange + 0.5D;
			double y = j >= 2 ? listnbt.getDouble(1) : (double) (blockpos.getY() + world.rand.nextInt(3) - 1);
			double z = j >= 3 ? listnbt.getDouble(2) : blockpos.getZ() + (world.rand.nextDouble() - world.rand.nextDouble()) * this.spawnRange + 0.5D;
			while (!world.doesNotCollide(optional.get().func_220328_a(x, y, z))) {
				x = j >= 1 ? listnbt.getDouble(0) : blockpos.getX() + (world.rand.nextDouble() - world.rand.nextDouble()) * this.spawnRange + 0.5D;
				y = j >= 2 ? listnbt.getDouble(1) : (double) (blockpos.getY() + world.rand.nextInt(3) - 1);
				z = j >= 3 ? listnbt.getDouble(2) : blockpos.getZ() + (world.rand.nextDouble() - world.rand.nextDouble()) * this.spawnRange + 0.5D;
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

	Method dropLoot;

	protected void onWaveEnd(int wave) {
		PlayerEntity p = (PlayerEntity) world.getPlayerByUuid(summonerId);
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

	protected void modifyEntityForWave(int wave, LivingEntity entity) {
		entity.addPotionEffect(new EffectInstance(Effects.FIRE_RESISTANCE, 20 * 60 * 10));
	}

}
