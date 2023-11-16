package dev.shadowsoffire.gateways.gate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.shadowsoffire.gateways.GatewayObjects;
import dev.shadowsoffire.gateways.entity.GatewayEntity;
import dev.shadowsoffire.gateways.entity.GatewayEntity.FailureReason;
import dev.shadowsoffire.gateways.event.GateEvent;
import dev.shadowsoffire.gateways.net.ParticleMessage;
import dev.shadowsoffire.placebo.codec.PlaceboCodecs;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeEventFactory;

/**
 * A single wave of a gateway.
 *
 * @param entities    A list of all entities to be spawned this wave, with optional NBT for additional data.
 * @param modifiers   A list of modifiers that will be applied to all spawned entities.
 * @param rewards     All rewards that will be granted at the end of the wave.
 * @param maxWaveTime The time the player has to complete this wave.
 * @param setupTime   The delay after this wave before the next wave starts. Ignored if this is the last wave.
 */
public record Wave(List<WaveEntity> entities, List<WaveModifier> modifiers, List<Reward> rewards, int maxWaveTime, int setupTime) {

    public static Codec<Wave> CODEC = RecordCodecBuilder.create(inst -> inst
        .group(
            WaveEntity.CODEC.listOf().fieldOf("entities").forGetter(Wave::entities),
            PlaceboCodecs.nullableField(WaveModifier.CODEC.listOf(), "modifiers", Collections.emptyList()).forGetter(Wave::modifiers),
            PlaceboCodecs.nullableField(Reward.CODEC.listOf(), "rewards", Collections.emptyList()).forGetter(Wave::rewards),
            Codec.INT.fieldOf("max_wave_time").forGetter(Wave::maxWaveTime),
            Codec.INT.fieldOf("setup_time").forGetter(Wave::setupTime))
        .apply(inst, Wave::new));

    public List<LivingEntity> spawnWave(ServerLevel level, Vec3 pos, GatewayEntity gate) {
        List<LivingEntity> spawned = new ArrayList<>();
        for (WaveEntity toSpawn : this.entities) {
            for (int i = 0; i < toSpawn.getCount(); i++) {
                LivingEntity entity = spawnWaveEntity(level, pos, gate, this, toSpawn);
                if (entity == null) {
                    gate.onFailure(spawned, FailureReason.SPAWN_FAILED);
                    break;
                }
                spawned.add(entity);
            }
        }

        return spawned;
    }

    public List<ItemStack> spawnRewards(ServerLevel level, GatewayEntity gate, Player summoner) {
        List<ItemStack> stacks = new ArrayList<>();
        this.rewards.forEach(r -> r.generateLoot(level, gate, summoner, s -> {
            if (!s.isEmpty()) {
                while (s.getCount() > 4) {
                    ItemStack copy = s.copy();
                    copy.setCount(4);
                    stacks.add(copy);
                    s.shrink(4);
                }
                if (!s.isEmpty()) stacks.add(s);
            }
        }));
        return stacks;
    }

    /**
     * Attempts to spawn a wave entity, placing it in the world and processing all usual triggers.
     * 
     * @param level      The level the gateway is in.
     * @param pos        The position of the gateway.
     * @param gate       The controlling gateway.
     * @param wave       The current wave of the gateway.
     * @param waveEntity The wave entity being spawned.
     * @return The freshly spawned entity, or null if the spawn failed.
     */
    public static LivingEntity spawnWaveEntity(ServerLevel level, Vec3 pos, GatewayEntity gate, Wave wave, WaveEntity waveEntity) {
        LivingEntity entity = waveEntity.createEntity(level);
        if (entity == null) return null;

        Vec3 spawnPos = gate.getGateway().spawnAlgo().spawn(level, pos, gate, entity);
        if (spawnPos == null) return null;

        entity.getPersistentData().putUUID("gateways.owner", gate.getUUID());
        entity.moveTo(spawnPos.x(), spawnPos.y(), spawnPos.z(), level.random.nextFloat() * 360, level.random.nextFloat() * 360);

        entity.getPassengersAndSelf().filter(e -> e instanceof LivingEntity).map(LivingEntity.class::cast).forEach(e -> {
            wave.modifiers.forEach(m -> m.apply(e));
            e.setHealth(entity.getMaxHealth());
            e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 5, 100, true, false));
        });

        GateRules rules = gate.getGateway().rules();

        if (entity instanceof Mob mob) {
            if (waveEntity.shouldFinalizeSpawn()) {
                ForgeEventFactory.onFinalizeSpawn(mob, level, level.getCurrentDifficultyAt(entity.blockPosition()), MobSpawnType.SPAWNER, null, null);
            }
            mob.setTarget(gate.summonerOrClosest());
            mob.setPersistenceRequired();

            // Override the drop chances to the rules-specified default if they are unchanged from the default of 0.085F
            if (rules.defaultDropChance() >= 0) {
                for (int i = 0; i < 2; i++) {
                    if (mob.handDropChances[i] == 0.085F) {
                        mob.handDropChances[i] = rules.defaultDropChance();
                    }
                }
                for (int i = 0; i < 4; i++) {
                    if (mob.armorDropChances[i] == 0.085F) {
                        mob.armorDropChances[i] = rules.defaultDropChance();
                    }
                }
            }
        }

        if (rules.followRangeBoost() > 0) {
            AttributeInstance attr = entity.getAttribute(Attributes.FOLLOW_RANGE);
            if (attr != null) {
                attr.addPermanentModifier(new AttributeModifier("Gateway Follow Range Boost", rules.followRangeBoost(), Operation.ADDITION));
            }
        }

        MinecraftForge.EVENT_BUS.post(new GateEvent.WaveEntitySpawned(gate, entity));
        level.addFreshEntityWithPassengers(entity);
        level.playSound(null, gate.getX(), gate.getY(), gate.getZ(), GatewayObjects.GATE_WARP.get(), SoundSource.HOSTILE, 0.5F, 1);
        gate.spawnParticle(entity.getX(), entity.getY(), entity.getZ(), ParticleMessage.Type.SPAWNED);
        return entity;
    }

}
