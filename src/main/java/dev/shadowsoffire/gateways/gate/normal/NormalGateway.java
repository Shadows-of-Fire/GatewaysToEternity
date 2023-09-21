package dev.shadowsoffire.gateways.gate.normal;

import java.util.Collections;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.shadowsoffire.gateways.Gateways;
import dev.shadowsoffire.gateways.client.NormalGateClient;
import dev.shadowsoffire.gateways.entity.GatewayEntity;
import dev.shadowsoffire.gateways.entity.NormalGatewayEntity;
import dev.shadowsoffire.gateways.gate.BossEventSettings;
import dev.shadowsoffire.gateways.gate.Failure;
import dev.shadowsoffire.gateways.gate.GateRules;
import dev.shadowsoffire.gateways.gate.Gateway;
import dev.shadowsoffire.gateways.gate.GatewayRegistry;
import dev.shadowsoffire.gateways.gate.Reward;
import dev.shadowsoffire.gateways.gate.SpawnAlgorithms;
import dev.shadowsoffire.gateways.gate.SpawnAlgorithms.SpawnAlgorithm;
import dev.shadowsoffire.gateways.gate.Wave;
import dev.shadowsoffire.placebo.codec.PlaceboCodecs;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

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
public record NormalGateway(Size size, TextColor color, List<Wave> waves, List<Reward> rewards, List<Failure> failures, SpawnAlgorithm spawnAlgo, GateRules rules,
    BossEventSettings bossSettings) implements Gateway {

    public static Codec<NormalGateway> CODEC = RecordCodecBuilder.create(inst -> inst
        .group(
            Size.CODEC.fieldOf("size").forGetter(NormalGateway::size),
            TextColor.CODEC.fieldOf("color").forGetter(NormalGateway::color),
            Wave.CODEC.listOf().fieldOf("waves").forGetter(NormalGateway::waves),
            PlaceboCodecs.nullableField(Reward.CODEC.listOf(), "rewards", Collections.emptyList()).forGetter(NormalGateway::rewards),
            PlaceboCodecs.nullableField(Failure.CODEC.listOf(), "failures", Collections.emptyList()).forGetter(NormalGateway::failures),
            PlaceboCodecs.nullableField(SpawnAlgorithms.CODEC, "spawn_algorithm", SpawnAlgorithms.NAMED_ALGORITHMS.get(Gateways.loc("open_field"))).forGetter(NormalGateway::spawnAlgo),
            PlaceboCodecs.nullableField(GateRules.CODEC, "rules", GateRules.DEFAULT).forGetter(NormalGateway::rules),
            PlaceboCodecs.nullableField(BossEventSettings.CODEC, "boss_event", BossEventSettings.DEFAULT).forGetter(NormalGateway::bossSettings))
        .apply(inst, NormalGateway::new));

    @Override
    public GatewayEntity createEntity(Level level, Player summoner) {
        return new NormalGatewayEntity(level, summoner, GatewayRegistry.INSTANCE.holder(this));
    }

    @Override
    public void appendPearlTooltip(Level level, List<Component> tooltips, TooltipFlag flag) {
        NormalGateClient.appendPearlTooltip(this, level, tooltips, flag);
    }

    @Override
    public void renderBossBar(GatewayEntity gate, Object gfx, int x, int y, boolean isInWorld) {
        NormalGateClient.renderBossBar(gate, gfx, x, y, isInWorld);
    }

    public int getNumWaves() {
        return this.waves.size();
    }

    public Wave getWave(int n) {
        return this.waves.get(n);
    }

    @Override
    public Codec<NormalGateway> getCodec() {
        return CODEC;
    }

}
