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
 */
public record Gateway(GatewaySize size, TextColor color, List<Wave> waves, List<Reward> rewards, List<Failure> failures, int completionXp, double spawnRange, double leashRange, SpawnAlgorithm spawnAlgo, boolean playerDamageOnly,
    boolean allowDiscarding, boolean removeMobsOnFailure, boolean requiresNearbyPlayer) implements PSerializable<Gateway> {

    public static Codec<Gateway> CODEC = RecordCodecBuilder.create(inst -> inst
        .group(
            GatewaySize.CODEC.fieldOf("size").forGetter(Gateway::size),
            TextColor.CODEC.fieldOf("color").forGetter(Gateway::color),
            Wave.CODEC.listOf().fieldOf("waves").forGetter(Gateway::waves),
            Reward.CODEC.listOf().optionalFieldOf("rewards", Collections.emptyList()).forGetter(Gateway::rewards),
            Failure.CODEC.listOf().optionalFieldOf("failures", Collections.emptyList()).forGetter(Gateway::failures),
            Codec.INT.fieldOf("completion_xp").forGetter(Gateway::completionXp),
            Codec.DOUBLE.fieldOf("spawn_range").forGetter(Gateway::spawnRange),
            Codec.DOUBLE.optionalFieldOf("leash_range", 24D).forGetter(Gateway::leashRange),
            SpawnAlgorithms.CODEC.optionalFieldOf("spawn_algorithm", SpawnAlgorithms.NAMED_ALGORITHMS.get(Gateways.loc("open_field"))).forGetter(Gateway::spawnAlgo),
            Codec.BOOL.optionalFieldOf("player_damage_only", false).forGetter(Gateway::playerDamageOnly),
            Codec.BOOL.optionalFieldOf("allow_discarding", false).forGetter(Gateway::allowDiscarding),
            Codec.BOOL.optionalFieldOf("remove_mobs_on_failure", true).forGetter(Gateway::removeMobsOnFailure),
            Codec.BOOL.optionalFieldOf("requires_nearby_player", true).forGetter(Gateway::requiresNearbyPlayer))
        .apply(inst, Gateway::new));

    public static final PSerializer<Gateway> SERIALIZER = PSerializer.fromCodec("Gateway", CODEC);

    public int getNumWaves() {
        return waves.size();
    }

    public Wave getWave(int n) {
        return this.waves.get(n);
    }

    public double getLeashRangeSq() {
        return this.leashRange * this.leashRange;
    }

    @Override
    public PSerializer<? extends Gateway> getSerializer() {
        return SERIALIZER;
    }

}
