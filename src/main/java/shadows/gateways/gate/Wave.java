package shadows.gateways.gate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.event.ForgeEventFactory;
import shadows.gateways.GatewayObjects;
import shadows.gateways.Gateways;
import shadows.gateways.entity.GatewayEntity;
import shadows.gateways.entity.GatewayEntity.FailureReason;
import shadows.placebo.json.RandomAttributeModifier;

/**
 * A single wave of a gateway.
 * @param entities A list of all entities to be spawned this wave, with optional NBT for additional data.
 * @param modifiers A list of modifiers that will be applied to all spawned entities.
 * @param rewards All rewards that will be granted at the end of the wave.
 * @param maxWaveTime The time the player has to complete this wave.
 * @param setupTime The delay after this wave before the next wave starts.  Ignored if this is the last wave.
 */
public record Wave(List<WaveEntity> entities, List<RandomAttributeModifier> modifiers, List<Reward> rewards, int maxWaveTime, int setupTime) {

	//Formatter::off
	public static Codec<Wave> CODEC = RecordCodecBuilder.create(inst -> inst
		.group(
			WaveEntity.CODEC.listOf().fieldOf("entities").forGetter(Wave::entities),
			RandomAttributeModifier.CODEC.listOf().optionalFieldOf("modifiers", Collections.emptyList()).forGetter(Wave::modifiers),
			Reward.CODEC.listOf().optionalFieldOf("rewards", Collections.emptyList()).forGetter(Wave::rewards),
			Codec.INT.fieldOf("max_wave_time").forGetter(Wave::maxWaveTime),
			Codec.INT.fieldOf("setup_time").forGetter(Wave::setupTime))
			.apply(inst, Wave::new)
		);
	//Formatter::on

	public List<LivingEntity> spawnWave(ServerLevel level, BlockPos pos, GatewayEntity gate) {
		List<LivingEntity> spawned = new ArrayList<>();
		for (WaveEntity toSpawn : entities) {

			double spawnRange = gate.getGateway().getSpawnRange();

			int tries = 0;
			double x = pos.getX() + (-1 + 2 * level.random.nextDouble()) * spawnRange;
			double y = pos.getY() + level.random.nextInt(3) - 1;
			double z = pos.getZ() + (-1 + 2 * level.random.nextDouble()) * spawnRange;
			while (!level.noCollision(toSpawn.getAABB(x, y, z)) && tries++ < 7) {
				x = pos.getX() + (level.random.nextDouble() - level.random.nextDouble()) * spawnRange + 0.5D;
				y = pos.getY() + level.random.nextInt(3 * (int) gate.getGateway().getSize().getScale()) + 1;
				z = pos.getZ() + (level.random.nextDouble() - level.random.nextDouble()) * spawnRange + 0.5D;
			}

			while (level.getBlockState(new BlockPos(x, y - 1, z)).isAir() && y > level.getMinBuildHeight()) {
				y--;
			}

			while (!level.noCollision(toSpawn.getAABB(x, y, z))) {
				y++;
			}

			if (gate.distanceToSqr(x, y, z) > gate.getGateway().getLeashRangeSq()) {
				gate.onFailure(spawned, FailureReason.SPAWN_FAILED);
				break;
			}

			final double fx = x, fy = y, fz = z;

			if (level.noCollision(toSpawn.getAABB(fx, fy, fz))) {
				LivingEntity entity = toSpawn.createEntity(level);

				if (entity == null) {
					Gateways.LOGGER.error("Gate {} failed to create a living entity during wave {}!", gate.getName().getString(), gate.getWave());
					continue;
				}

				entity.getPersistentData().putUUID("gateways.owner", gate.getUUID());
				entity.moveTo(fx, fy, fz, level.random.nextFloat() * 360, level.random.nextFloat() * 360);

				entity.getPassengersAndSelf().filter(e -> e instanceof LivingEntity).map(LivingEntity.class::cast).forEach(e -> {
					modifiers.forEach(m -> m.apply(level.random, e));
					e.setHealth(entity.getMaxHealth());
					e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 5, 100, true, false));
				});

				if (entity instanceof Mob mob) {
					if (toSpawn.shouldFinalizeSpawn() && !ForgeEventFactory.doSpecialSpawn((Mob) entity, (LevelAccessor) level, (float) entity.getX(), (float) entity.getY(), (float) entity.getZ(), null, MobSpawnType.SPAWNER)) {
						mob.finalizeSpawn(level, level.getCurrentDifficultyAt(entity.blockPosition()), MobSpawnType.SPAWNER, null, null);
					}
					mob.setTarget(gate.getLevel().getNearestPlayer(gate, 12));
					mob.setPersistenceRequired();
				}

				level.addFreshEntityWithPassengers(entity);
				level.playSound(null, gate.getX(), gate.getY(), gate.getZ(), GatewayObjects.GATE_WARP.get(), SoundSource.HOSTILE, 0.5F, 1);
				spawned.add((LivingEntity) entity);
				gate.spawnParticle(gate.getGateway().getColor(), entity.getX() + entity.getBbWidth() / 2, entity.getY() + entity.getBbHeight() / 2, entity.getZ() + entity.getBbWidth() / 2, 0);
			} else {
				gate.onFailure(spawned, FailureReason.SPAWN_FAILED);
				break;
			}
		}

		return spawned;
	}

	public List<ItemStack> spawnRewards(ServerLevel level, GatewayEntity gate, Player summoner) {
		List<ItemStack> stacks = new ArrayList<>();
		this.rewards.forEach(r -> r.generateLoot(level, gate, summoner, s -> {
			if (!s.isEmpty()) stacks.add(s);
		}));
		return stacks;
	}

}
