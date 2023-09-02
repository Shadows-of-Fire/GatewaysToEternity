package dev.shadowsoffire.gateways.gate;

import java.util.Collections;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.shadowsoffire.gateways.Gateways;
import dev.shadowsoffire.gateways.entity.GatewayEntity.GatewaySize;
import dev.shadowsoffire.gateways.gate.SpawnAlgorithms.SpawnAlgorithm;
import dev.shadowsoffire.placebo.json.PSerializer;
import dev.shadowsoffire.placebo.json.PSerializer.PSerializable;
import net.minecraft.network.chat.TextColor;

/**
 * A Gateway is the definition of a Gateway Entity.
 * 
 * @param size      The size of the Gateway. Controls bounding box and pearl texture.
 * @param color     The color of the Gateway. Used for the Gateway, boss bar, name, and pearl.
 * @param waves     The {@linkplain Wave waves} of the Gateway.
 * @param rewards   The {@linkplain Reward completion rewards} if the final wave is defeated.
 * @param failures  The {@linkplain Failure penalties} for failing the gateway.
 * @param spawnAlgo The {@linkplain SpawnAlgorithm spawn algorithm} used for placing wave entities.
 * @param rules     The {@linkplain GateRules rules} of the Gateway.
 */
public record Gateway(GatewaySize size, TextColor color, List<Wave> waves, List<Reward> rewards, List<Failure> failures, SpawnAlgorithm spawnAlgo, GateRules rules,
    BossEventSettings bossEventSettings) implements PSerializable<Gateway> {

    public static Codec<Gateway> CODEC = RecordCodecBuilder.create(inst -> inst
        .group(
            GatewaySize.CODEC.fieldOf("size").forGetter(Gateway::size),
            TextColor.CODEC.fieldOf("color").forGetter(Gateway::color),
            Wave.CODEC.listOf().fieldOf("waves").forGetter(Gateway::waves),
            Reward.CODEC.listOf().optionalFieldOf("rewards", Collections.emptyList()).forGetter(Gateway::rewards),
            Failure.CODEC.listOf().optionalFieldOf("failures", Collections.emptyList()).forGetter(Gateway::failures),
            SpawnAlgorithms.CODEC.optionalFieldOf("spawn_algorithm", SpawnAlgorithms.NAMED_ALGORITHMS.get(Gateways.loc("open_field"))).forGetter(Gateway::spawnAlgo),
            GateRules.CODEC.optionalFieldOf("rules", GateRules.DEFAULT).forGetter(Gateway::rules),
            BossEventSettings.CODEC.optionalFieldOf("boss_event", BossEventSettings.DEFAULT).forGetter(Gateway::bossEventSettings))
        .apply(inst, Gateway::new));

    public static final PSerializer<Gateway> SERIALIZER = PSerializer.fromCodec("Gateway", CODEC);

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
    public PSerializer<? extends Gateway> getSerializer() {
        return SERIALIZER;
    }

}
