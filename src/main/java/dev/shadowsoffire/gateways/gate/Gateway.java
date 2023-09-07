package dev.shadowsoffire.gateways.gate;

import java.util.Collections;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.shadowsoffire.gateways.Gateways;
import dev.shadowsoffire.gateways.entity.GatewayEntity.GatewaySize;
import dev.shadowsoffire.gateways.gate.SpawnAlgorithms.SpawnAlgorithm;
import dev.shadowsoffire.placebo.codec.CodecProvider;
import dev.shadowsoffire.placebo.codec.PlaceboCodecs;
import net.minecraft.network.chat.TextColor;

/**
 * A Gateway is the definition of a Gateway Entity.
 * 
 * @param size      The size of the Gateway. Controls bounding box and pearl texture.
 * @param color     The color of the Gateway. Used for the Gateway, boss bar, name, and pearl.
 * @param waves     The {@linkplain Wave waves} of the Gateway.
 * @param rewards   The {@linkplain Reward completion rewards} if the final wave is defeated. Always displayed.
 * @param failures  The {@linkplain Failure penalties} for failing the gateway.
 * @param spawnAlgo The {@linkplain SpawnAlgorithm spawn algorithm} used for placing wave entities.
 * @param rules     The {@linkplain GateRules rules} of the Gateway.
 */
public record Gateway(GatewaySize size, TextColor color, List<Wave> waves, List<Reward> rewards, List<Failure> failures, SpawnAlgorithm spawnAlgo, GateRules rules,
    BossEventSettings bossEventSettings) implements CodecProvider<Gateway> {

    public static Codec<Gateway> CODEC = RecordCodecBuilder.create(inst -> inst
        .group(
            GatewaySize.CODEC.fieldOf("size").forGetter(Gateway::size),
            TextColor.CODEC.fieldOf("color").forGetter(Gateway::color),
            Wave.CODEC.listOf().fieldOf("waves").forGetter(Gateway::waves),
            PlaceboCodecs.nullableField(Reward.CODEC.listOf(), "rewards", Collections.emptyList()).forGetter(Gateway::rewards),
            PlaceboCodecs.nullableField(Failure.CODEC.listOf(), "failures", Collections.emptyList()).forGetter(Gateway::failures),
            PlaceboCodecs.nullableField(SpawnAlgorithms.CODEC, "spawn_algorithm", SpawnAlgorithms.NAMED_ALGORITHMS.get(Gateways.loc("open_field"))).forGetter(Gateway::spawnAlgo),
            PlaceboCodecs.nullableField(GateRules.CODEC, "rules", GateRules.DEFAULT).forGetter(Gateway::rules),
            PlaceboCodecs.nullableField(BossEventSettings.CODEC, "boss_event", BossEventSettings.DEFAULT).forGetter(Gateway::bossEventSettings))
        .apply(inst, Gateway::new));

    public int getNumWaves() {
        return this.waves.size();
    }

    public Wave getWave(int n) {
        return this.waves.get(n);
    }

    public double getLeashRangeSq() {
        double leashRange = this.rules.leashRange();
        return leashRange * leashRange;
    }

    @Override
    public Codec<Gateway> getCodec() {
        return CODEC;
    }

}
