package dev.shadowsoffire.gateways.gate.endless;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.common.base.Preconditions;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.shadowsoffire.gateways.gate.Reward;
import dev.shadowsoffire.gateways.gate.WaveEntity;
import dev.shadowsoffire.gateways.gate.WaveModifier;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;

/**
 * An Endless Modifier is a periodically applied modification to a running Endless Gateway.
 * <p>
 * It can provide more wave entities, more rewards, and additional wave modifiers
 */
public record EndlessModifier(ApplicationMode appMode, List<WaveEntity> entities, List<Reward> rewards, List<WaveModifier> modifiers, int waveTime, int setupTime) {

    public static Codec<EndlessModifier> CODEC = RecordCodecBuilder.create(inst -> inst
        .group(
            ApplicationMode.CODEC.fieldOf("application_mode").forGetter(EndlessModifier::appMode),
            WaveEntity.CODEC.listOf().optionalFieldOf("entities", Collections.emptyList()).forGetter(EndlessModifier::entities),
            Reward.CODEC.listOf().optionalFieldOf("rewards", Collections.emptyList()).forGetter(EndlessModifier::rewards),
            WaveModifier.CODEC.listOf().optionalFieldOf("modifiers", Collections.emptyList()).forGetter(EndlessModifier::modifiers),
            Codec.INT.optionalFieldOf("max_wave_time", 0).forGetter(EndlessModifier::waveTime),
            Codec.INT.optionalFieldOf("setup_time", 0).forGetter(EndlessModifier::setupTime))
        .apply(inst, EndlessModifier::new));

    public EndlessModifier(ApplicationMode appMode, List<WaveEntity> entities, List<Reward> rewards, List<WaveModifier> modifiers, int waveTime, int setupTime) {
        this.appMode = appMode;
        this.entities = entities;
        this.rewards = rewards;
        this.modifiers = modifiers;
        this.waveTime = waveTime;
        this.setupTime = setupTime;
        Preconditions.checkArgument(!this.entities.isEmpty() || !this.rewards.isEmpty() || !this.modifiers.isEmpty(), "An Endless Modifier must provide at least one of entities, rewards, or modifiers.");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private ApplicationMode appMode;
        private List<WaveEntity> entities = new ArrayList<>();
        private List<Reward> rewards = new ArrayList<>();
        private List<WaveModifier> modifiers = new ArrayList<>();
        private int waveTime = 0;
        private int setupTime = 0;

        public Builder applicationMode(ApplicationMode appMode) {
            this.appMode = appMode;
            return this;
        }

        public Builder entity(WaveEntity entity) {
            this.entities.add(entity);
            return this;
        }

        public Builder entities(List<WaveEntity> entities) {
            this.entities = new ArrayList<>(entities);
            return this;
        }

        public Builder reward(Reward reward) {
            this.rewards.add(reward);
            return this;
        }

        public Builder rewards(List<Reward> rewards) {
            this.rewards = new ArrayList<>(rewards);
            return this;
        }

        public Builder modifier(WaveModifier modifier) {
            this.modifiers.add(modifier);
            return this;
        }

        public Builder attribute(Holder<Attribute> attribute, Operation op, float value) {
            return this.modifier(WaveModifier.AttributeModifier.create(attribute, op, value));
        }

        public Builder modifiers(List<WaveModifier> modifiers) {
            this.modifiers = new ArrayList<>(modifiers);
            return this;
        }

        public Builder maxWaveTime(int waveTime) {
            this.waveTime = waveTime;
            return this;
        }

        public Builder setupTime(int setupTime) {
            this.setupTime = setupTime;
            return this;
        }

        public EndlessModifier build() {
            if (appMode == null) {
                throw new IllegalStateException("Application mode must be specified");
            }

            return new EndlessModifier(
                appMode,
                Collections.unmodifiableList(entities),
                Collections.unmodifiableList(rewards),
                Collections.unmodifiableList(modifiers),
                waveTime,
                setupTime);
        }
    }
}
