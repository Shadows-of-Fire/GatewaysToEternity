package dev.shadowsoffire.gateways.gate.endless;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.shadowsoffire.gateways.GatewayObjects;
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
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public record EndlessGateway(Size size, TextColor color, Wave baseWave, List<EndlessModifier> modifiers, List<Failure> failures, SpawnAlgorithm spawnAlgo, GateRules rules,
    BossEventSettings bossSettings, Holder<SoundEvent> soundtrack) implements Gateway {

    public static final Codec<EndlessGateway> CODEC = RecordCodecBuilder.create(inst -> inst
        .group(
            Size.CODEC.fieldOf("size").forGetter(EndlessGateway::size),
            TextColor.CODEC.fieldOf("color").forGetter(EndlessGateway::color),
            Wave.CODEC.fieldOf("base_wave").forGetter(EndlessGateway::baseWave),
            EndlessModifier.CODEC.listOf().fieldOf("modifiers").forGetter(EndlessGateway::modifiers),
            Failure.CODEC.listOf().optionalFieldOf("failures", Collections.emptyList()).forGetter(EndlessGateway::failures),
            SpawnAlgorithms.CODEC.optionalFieldOf("spawn_algorithm", SpawnAlgorithms.OPEN_FIELD).forGetter(EndlessGateway::spawnAlgo),
            GateRules.CODEC.optionalFieldOf("rules", GateRules.DEFAULT).forGetter(EndlessGateway::rules),
            BossEventSettings.CODEC.optionalFieldOf("boss_event", BossEventSettings.DEFAULT).forGetter(EndlessGateway::bossSettings),
            BuiltInRegistries.SOUND_EVENT.holderByNameCodec().optionalFieldOf("soundtrack", GatewayObjects.GATE_AMBIENT).forGetter(EndlessGateway::soundtrack))
        .apply(inst, EndlessGateway::new));

    @Deprecated // back-compat ctor
    public EndlessGateway(Size size, TextColor color, Wave baseWave, List<EndlessModifier> modifiers, List<Failure> failures, SpawnAlgorithm spawnAlgo, GateRules rules,
        BossEventSettings bossSettings) {
        this(size, color, baseWave, modifiers, failures, spawnAlgo, rules, bossSettings, GatewayObjects.GATE_AMBIENT);
    }

    @Override
    public GatewayEntity createEntity(Level level, Player summoner) {
        return new EndlessGatewayEntity(level, summoner, GatewayRegistry.INSTANCE.holder(this));
    }

    @Override
    public void appendPearlTooltip(TooltipContext ctx, Consumer<Component> tooltips, TooltipFlag flag) {
        EndlessGateClient.appendPearlTooltip(this, ctx, tooltips, flag);
    }

    @Override
    public void renderBossBar(GatewayEntity gate, Object gfx, int x, int y, boolean isInWorld) {
        EndlessGateClient.renderBossBar(gate, gfx, x, y, isInWorld);
    }

    @Override
    public Codec<? extends Gateway> getCodec() {
        return CODEC;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private EndlessGateway.Size size;
        private TextColor color;
        private Wave baseWave;
        private List<EndlessModifier> modifiers = new ArrayList<>();
        private List<Failure> failures = Collections.emptyList();
        private SpawnAlgorithm spawnAlgo = SpawnAlgorithms.OPEN_FIELD;
        private GateRules rules = GateRules.DEFAULT;
        private BossEventSettings bossSettings = BossEventSettings.DEFAULT;
        private Holder<SoundEvent> soundtrack = GatewayObjects.GATE_AMBIENT;

        public Builder size(Gateway.Size size) {
            this.size = size;
            return this;
        }

        public Builder color(TextColor color) {
            this.color = color;
            return this;
        }

        public Builder color(int rgb) {
            this.color = TextColor.fromRgb(rgb);
            return this;
        }

        public Builder baseWave(Wave baseWave) {
            this.baseWave = baseWave;
            return this;
        }

        public Builder baseWave(UnaryOperator<Wave.Builder> config) {
            this.baseWave = config.apply(Wave.builder()).build();
            return this;
        }

        public Builder modifier(EndlessModifier modifier) {
            this.modifiers.add(modifier);
            return this;
        }

        public Builder modifier(UnaryOperator<EndlessModifier.Builder> config) {
            this.modifiers.add(config.apply(EndlessModifier.builder()).build());
            return this;
        }

        public Builder failure(Failure failure) {
            this.failures.add(failure);
            return this;
        }

        public Builder failures(List<Failure> failures) {
            this.failures.addAll(failures);
            return this;
        }

        public Builder spawnAlgo(SpawnAlgorithm spawnAlgo) {
            this.spawnAlgo = spawnAlgo;
            return this;
        }

        public Builder rules(GateRules rules) {
            this.rules = rules;
            return this;
        }

        public Builder rules(UnaryOperator<GateRules.Builder> config) {
            return this.rules(config.apply(GateRules.builder()).build());
        }

        public Builder bossSettings(BossEventSettings bossSettings) {
            this.bossSettings = bossSettings;
            return this;
        }

        public Builder soundtrack(Holder<SoundEvent> soundtrack) {
            this.soundtrack = soundtrack;
            return this;
        }

        public EndlessGateway build() {
            if (size == null) {
                throw new IllegalStateException("Size must be specified");
            }
            if (color == null) {
                throw new IllegalStateException("Color must be specified");
            }
            if (baseWave == null) {
                throw new IllegalStateException("Base wave must be specified");
            }

            return new EndlessGateway(size, color, baseWave, modifiers, failures, spawnAlgo, rules, bossSettings, soundtrack);
        }
    }

}
