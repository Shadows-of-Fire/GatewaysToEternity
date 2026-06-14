package dev.shadowsoffire.gateways.gate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.shadowsoffire.gateways.GatewayObjects;
import dev.shadowsoffire.gateways.Gateways;
import dev.shadowsoffire.gateways.entity.GatewayEntity;
import dev.shadowsoffire.gateways.entity.GatewayEntity.FailureReason;
import dev.shadowsoffire.gateways.event.GateEvent;
import dev.shadowsoffire.gateways.payloads.ParticlePayload;
import net.minecraft.core.Holder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.DropChances;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.EventHooks;

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
            WaveModifier.CODEC.listOf().optionalFieldOf("modifiers", Collections.emptyList()).forGetter(Wave::modifiers),
            Reward.CODEC.listOf().optionalFieldOf("rewards", Collections.emptyList()).forGetter(Wave::rewards),
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
    @Nullable
    public static LivingEntity spawnWaveEntity(ServerLevel level, Vec3 pos, GatewayEntity gate, Wave wave, WaveEntity waveEntity) {
        LivingEntity entity = waveEntity.createEntity(level, gate);
        if (entity == null) {
            Gateways.logSpawnDebug(gate, waveEntity, "Entity creation returned null");
            return null;
        }

        Vec3 spawnPos = gate.getGateway().spawnAlgo().spawn(level, pos, gate, entity);
        if (spawnPos == null) {
            Gateways.logSpawnDebug(gate, waveEntity, "Spawn algorithm returned null position");
            return null;
        }

        entity.getPersistentData().store("gateways.owner", UUIDUtil.CODEC, gate.getUUID());
        entity.snapTo(spawnPos.x(), spawnPos.y(), spawnPos.z(), level.getRandom().nextFloat() * 360, level.getRandom().nextFloat() * 360);

        entity.getPassengersAndSelf().filter(e -> e instanceof LivingEntity).map(LivingEntity.class::cast).forEach(e -> {
            wave.modifiers.forEach(m -> m.apply(e, gate));
            e.setHealth(e.getMaxHealth());
            e.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 5, 100, true, false));
        });

        GateRules rules = gate.getGateway().rules();

        if (entity instanceof Mob mob) {
            if (waveEntity.shouldFinalizeSpawn()) {
                EventHooks.finalizeMobSpawn(mob, level, level.getCurrentDifficultyAt(entity.blockPosition()), EntitySpawnReason.SPAWNER, null);
            }
            Player summoner = gate.summonerOrClosest();
            if (!(summoner instanceof FakePlayer)) {
                mob.setTarget(summoner);
            }
            mob.setPersistenceRequired();

            // Override the drop chances to the rules-specified default if they are unchanged from the default of 0.085F
            if (rules.defaultDropChance() >= 0) {
                for (EquipmentSlot slot : EquipmentSlot.values()) {
                    if (mob.getDropChances().byEquipment(slot) == DropChances.DEFAULT_EQUIPMENT_DROP_CHANCE) {
                        mob.setDropChance(slot, rules.defaultDropChance());
                    }
                }
            }
        }

        if (rules.followRangeBoost() > 0) {
            AttributeInstance attr = entity.getAttribute(Attributes.FOLLOW_RANGE);
            if (attr != null) {
                attr.addPermanentModifier(new AttributeModifier(Gateways.loc("follow_range_boost"), rules.followRangeBoost(), Operation.ADD_VALUE));
            }
        }

        NeoForge.EVENT_BUS.post(new GateEvent.WaveEntitySpawned(gate, entity));
        level.addFreshEntityWithPassengers(entity);
        level.playSound(null, gate.getX(), gate.getY(), gate.getZ(), GatewayObjects.GATE_WARP, SoundSource.HOSTILE, 0.5F, 1);
        gate.spawnParticle(entity.getX(), entity.getY(), entity.getZ(), ParticlePayload.EffectType.SPAWNED);
        return entity;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final List<WaveEntity> entities = new ArrayList<>();
        private final List<WaveModifier> modifiers = new ArrayList<>();
        private final List<Reward> rewards = new ArrayList<>();
        private int maxWaveTime;
        private int setupTime;

        /**
         * Adds an entity to this wave.
         * 
         * @param entity The entity to add
         * @return This builder for chaining
         */
        public Builder entity(WaveEntity entity) {
            this.entities.add(entity);
            return this;
        }

        /**
         * Adds multiple entities to this wave.
         * 
         * @param entities The entities to add
         * @return This builder for chaining
         */
        public Builder entities(List<WaveEntity> entities) {
            this.entities.addAll(entities);
            return this;
        }

        /**
         * Adds a modifier to this wave.
         * 
         * @param modifier The modifier to add
         * @return This builder for chaining
         */
        public Builder modifier(WaveModifier modifier) {
            this.modifiers.add(modifier);
            return this;
        }

        public Builder attribute(Holder<Attribute> attribute, Operation op, float value) {
            return this.modifier(WaveModifier.AttributeModifier.create(attribute, op, value, Gateways.loc("wave_modifier_" + this.modifiers.size())));
        }

        /**
         * Adds multiple modifiers to this wave.
         * 
         * @param modifiers The modifiers to add
         * @return This builder for chaining
         */
        public Builder modifiers(List<WaveModifier> modifiers) {
            this.modifiers.addAll(modifiers);
            return this;
        }

        /**
         * Adds a reward to this wave.
         * 
         * @param reward The reward to add
         * @return This builder for chaining
         */
        public Builder reward(Reward reward) {
            this.rewards.add(reward);
            return this;
        }

        /**
         * Adds multiple rewards to this wave.
         * 
         * @param rewards The rewards to add
         * @return This builder for chaining
         */
        public Builder rewards(List<Reward> rewards) {
            this.rewards.addAll(rewards);
            return this;
        }

        /**
         * Sets the maximum time allowed to complete this wave.
         * 
         * @param maxWaveTime The maximum time in ticks
         * @return This builder for chaining
         */
        public Builder maxWaveTime(int maxWaveTime) {
            this.maxWaveTime = maxWaveTime;
            return this;
        }

        /**
         * Sets the setup time before the next wave starts.
         * 
         * @param setupTime The setup time in ticks
         * @return This builder for chaining
         */
        public Builder setupTime(int setupTime) {
            this.setupTime = setupTime;
            return this;
        }

        /**
         * Builds a new Wave with the configured parameters.
         * 
         * @return A new Wave instance
         * @throws IllegalStateException if required parameters are missing
         */
        public Wave build() {
            if (entities.isEmpty()) {
                throw new IllegalStateException("Wave must have at least one entity");
            }

            if (maxWaveTime <= 0) {
                throw new IllegalStateException("Maximum wave time must be positive");
            }

            if (setupTime < 0) {
                throw new IllegalStateException("Setup time cannot be negative");
            }

            return new Wave(
                Collections.unmodifiableList(new ArrayList<>(entities)),
                Collections.unmodifiableList(new ArrayList<>(modifiers)),
                Collections.unmodifiableList(new ArrayList<>(rewards)),
                maxWaveTime,
                setupTime);
        }
    }

}
