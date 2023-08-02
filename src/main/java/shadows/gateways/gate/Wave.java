package shadows.gateways.gate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.shadowsoffire.placebo.json.RandomAttributeModifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeEventFactory;
import shadows.gateways.GatewayObjects;
import shadows.gateways.entity.GatewayEntity;
import shadows.gateways.entity.GatewayEntity.FailureReason;
import shadows.gateways.event.GateEvent;

/**
 * A single wave of a gateway.
 * 
 * @param entities    A list of all entities to be spawned this wave, with optional NBT for additional data.
 * @param modifiers   A list of modifiers that will be applied to all spawned entities.
 * @param rewards     All rewards that will be granted at the end of the wave.
 * @param maxWaveTime The time the player has to complete this wave.
 * @param setupTime   The delay after this wave before the next wave starts. Ignored if this is the last wave.
 */
public record Wave(List<WaveEntity> entities, List<RandomAttributeModifier> modifiers, List<Reward> rewards, int maxWaveTime, int setupTime) {

    // Formatter::off
    public static Codec<Wave> CODEC = RecordCodecBuilder.create(inst -> inst
        .group(
            WaveEntity.CODEC.listOf().fieldOf("entities").forGetter(Wave::entities),
            RandomAttributeModifier.CODEC.listOf().optionalFieldOf("modifiers", Collections.emptyList()).forGetter(Wave::modifiers),
            Reward.CODEC.listOf().optionalFieldOf("rewards", Collections.emptyList()).forGetter(Wave::rewards),
            Codec.INT.fieldOf("max_wave_time").forGetter(Wave::maxWaveTime),
            Codec.INT.fieldOf("setup_time").forGetter(Wave::setupTime))
        .apply(inst, Wave::new));
    // Formatter::on

    public List<LivingEntity> spawnWave(ServerLevel level, Vec3 pos, GatewayEntity gate) {
        List<LivingEntity> spawned = new ArrayList<>();
        for (WaveEntity toSpawn : entities) {
            Vec3 spawnPos = gate.getGateway().getSpawnAlgo().spawn(level, pos, gate, toSpawn);
            LivingEntity entity = toSpawn.createEntity(level);

            if (spawnPos == null || entity == null) {
                gate.onFailure(spawned, FailureReason.SPAWN_FAILED);
                break;
            }

            entity.getPersistentData().putUUID("gateways.owner", gate.getUUID());
            entity.moveTo(spawnPos.x(), spawnPos.y(), spawnPos.z(), level.random.nextFloat() * 360, level.random.nextFloat() * 360);

            entity.getPassengersAndSelf().filter(e -> e instanceof LivingEntity).map(LivingEntity.class::cast).forEach(e -> {
                modifiers.forEach(m -> m.apply(level.random, e));
                e.setHealth(entity.getMaxHealth());
                e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 5, 100, true, false));
            });

            if (entity instanceof Mob mob) {
                if (toSpawn.shouldFinalizeSpawn()) {
                    ForgeEventFactory.onFinalizeSpawn(mob, level, level.getCurrentDifficultyAt(entity.blockPosition()), MobSpawnType.SPAWNER, null, null);
                }
                mob.setTarget(gate.level().getNearestPlayer(gate, 12));
                mob.setPersistenceRequired();
            }

            MinecraftForge.EVENT_BUS.post(new GateEvent.WaveEntitySpawned(gate, entity));
            level.addFreshEntityWithPassengers(entity);
            level.playSound(null, gate.getX(), gate.getY(), gate.getZ(), GatewayObjects.GATE_WARP.get(), SoundSource.HOSTILE, 0.5F, 1);
            spawned.add((LivingEntity) entity);
            gate.spawnParticle(gate.getGateway().getColor(), entity.getX() + entity.getBbWidth() / 2, entity.getY() + entity.getBbHeight() / 2, entity.getZ() + entity.getBbWidth() / 2, 0);

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
