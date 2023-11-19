package dev.shadowsoffire.gateways.gate.endless;

import java.util.Collections;
import java.util.List;

import com.google.common.base.Preconditions;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.shadowsoffire.gateways.gate.Reward;
import dev.shadowsoffire.gateways.gate.WaveEntity;
import dev.shadowsoffire.gateways.gate.WaveModifier;
import dev.shadowsoffire.placebo.codec.PlaceboCodecs;

/**
 * An Endless Modifier is a periodically applied modification to a running Endless Gateway.
 * <p>
 * It can provide more wave entities, more rewards, and additional wave modifiers
 */
public record EndlessModifier(ApplicationMode appMode, List<WaveEntity> entities, List<Reward> rewards, List<WaveModifier> modifiers, int waveTime, int setupTime) {

    public static Codec<EndlessModifier> CODEC = RecordCodecBuilder.create(inst -> inst
        .group(
            ApplicationMode.CODEC.fieldOf("application_mode").forGetter(EndlessModifier::appMode),
            PlaceboCodecs.nullableField(WaveEntity.CODEC.listOf(), "entities", Collections.emptyList()).forGetter(EndlessModifier::entities),
            PlaceboCodecs.nullableField(Reward.CODEC.listOf(), "rewards", Collections.emptyList()).forGetter(EndlessModifier::rewards),
            PlaceboCodecs.nullableField(WaveModifier.CODEC.listOf(), "modifiers", Collections.emptyList()).forGetter(EndlessModifier::modifiers),
            PlaceboCodecs.nullableField(Codec.INT, "max_wave_time", 0).forGetter(EndlessModifier::waveTime),
            PlaceboCodecs.nullableField(Codec.INT, "setup_time", 0).forGetter(EndlessModifier::setupTime))
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

}
