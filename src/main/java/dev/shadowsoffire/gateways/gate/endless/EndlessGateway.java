package dev.shadowsoffire.gateways.gate.endless;

import java.util.Collections;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.shadowsoffire.gateways.Gateways;
import dev.shadowsoffire.gateways.client.EndlessGateClient;
import dev.shadowsoffire.gateways.entity.EndlessGatewayEntity;
import dev.shadowsoffire.gateways.entity.GatewayEntity;
import dev.shadowsoffire.gateways.gate.BossEventSettings;
import dev.shadowsoffire.gateways.gate.Failure;
import dev.shadowsoffire.gateways.gate.GateRules;
import dev.shadowsoffire.gateways.gate.Gateway;
import dev.shadowsoffire.gateways.gate.GatewayRegistry;
import dev.shadowsoffire.gateways.gate.SpawnAlgorithms;
import dev.shadowsoffire.gateways.gate.SpawnAlgorithms.SpawnAlgorithm;
import dev.shadowsoffire.gateways.gate.Wave;
import dev.shadowsoffire.placebo.codec.PlaceboCodecs;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public record EndlessGateway(Size size, TextColor color, Wave baseWave, List<EndlessModifier> modifiers, List<Failure> failures, SpawnAlgorithm spawnAlgo, GateRules rules,
    BossEventSettings bossSettings) implements Gateway {

    public static Codec<EndlessGateway> CODEC = RecordCodecBuilder.create(inst -> inst
        .group(
            Size.CODEC.fieldOf("size").forGetter(EndlessGateway::size),
            TextColor.CODEC.fieldOf("color").forGetter(EndlessGateway::color),
            Wave.CODEC.fieldOf("base_wave").forGetter(EndlessGateway::baseWave),
            EndlessModifier.CODEC.listOf().fieldOf("modifiers").forGetter(EndlessGateway::modifiers),
            PlaceboCodecs.nullableField(Failure.CODEC.listOf(), "failures", Collections.emptyList()).forGetter(EndlessGateway::failures),
            PlaceboCodecs.nullableField(SpawnAlgorithms.CODEC, "spawn_algorithm", SpawnAlgorithms.NAMED_ALGORITHMS.get(Gateways.loc("open_field"))).forGetter(EndlessGateway::spawnAlgo),
            PlaceboCodecs.nullableField(GateRules.CODEC, "rules", GateRules.DEFAULT).forGetter(EndlessGateway::rules),
            PlaceboCodecs.nullableField(BossEventSettings.CODEC, "boss_event", BossEventSettings.DEFAULT).forGetter(EndlessGateway::bossSettings))
        .apply(inst, EndlessGateway::new));

    @Override
    public GatewayEntity createEntity(Level level, Player summoner) {
        return new EndlessGatewayEntity(level, summoner, GatewayRegistry.INSTANCE.holder(this));
    }

    @Override
    public void appendPearlTooltip(Level level, List<Component> tooltips, TooltipFlag flag) {
        EndlessGateClient.appendPearlTooltip(this, level, tooltips, flag);
    }

    @Override
    public void renderBossBar(GatewayEntity gate, Object gfx, int x, int y, boolean isInWorld) {
        EndlessGateClient.renderBossBar(gate, gfx, x, y, isInWorld);
    }

    @Override
    public Codec<? extends Gateway> getCodec() {
        return CODEC;
    }

}
