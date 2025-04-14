package dev.shadowsoffire.gateways.gate.normal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.UnaryOperator;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

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
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item.TooltipContext;
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
            Reward.CODEC.listOf().optionalFieldOf("rewards", Collections.emptyList()).forGetter(NormalGateway::rewards),
            Failure.CODEC.listOf().optionalFieldOf("failures", Collections.emptyList()).forGetter(NormalGateway::failures),
            SpawnAlgorithms.CODEC.optionalFieldOf("spawn_algorithm", SpawnAlgorithms.OPEN_FIELD).forGetter(NormalGateway::spawnAlgo),
            GateRules.CODEC.optionalFieldOf("rules", GateRules.DEFAULT).forGetter(NormalGateway::rules),
            BossEventSettings.CODEC.optionalFieldOf("boss_event", BossEventSettings.DEFAULT).forGetter(NormalGateway::bossSettings))
        .apply(inst, NormalGateway::new));

    @Override
    public GatewayEntity createEntity(Level level, Player summoner) {
        return new NormalGatewayEntity(level, summoner, GatewayRegistry.INSTANCE.holder(this));
    }

    @Override
    public void appendPearlTooltip(TooltipContext ctx, List<Component> tooltips, TooltipFlag flag) {
        NormalGateClient.appendPearlTooltip(this, ctx, tooltips, flag);
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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private Size size;
        private TextColor color;
        private final List<Wave> waves = new ArrayList<>();
        private final List<Reward> rewards = new ArrayList<>();
        private final List<Failure> failures = new ArrayList<>();
        private SpawnAlgorithm spawnAlgo = SpawnAlgorithms.OPEN_FIELD;
        private GateRules rules = GateRules.DEFAULT;
        private BossEventSettings bossSettings = BossEventSettings.DEFAULT;

        public Builder size(Size size) {
            this.size = size;
            return this;
        }

        public Builder color(TextColor color) {
            this.color = color;
            return this;
        }

        public Builder color(int color) {
            return this.color(TextColor.fromRgb(color));
        }

        /**
         * Adds a wave to this gateway.
         * 
         * @param wave The wave to add
         * @return This builder for chaining
         */
        public Builder wave(Wave wave) {
            this.waves.add(wave);
            return this;
        }

        /**
         * Adds a wave to this gateway.
         * 
         * @param config A unary operator to create the wave.
         * @return This builder for chaining
         */
        public Builder wave(UnaryOperator<Wave.Builder> config) {
            return this.wave(config.apply(new Wave.Builder()).build());
        }

        /**
         * Adds multiple waves to this gateway.
         * 
         * @param waves The waves to add
         * @return This builder for chaining
         */
        public Builder waves(List<Wave> waves) {
            this.waves.addAll(waves);
            return this;
        }

        /**
         * Adds a reward to this gateway.
         * 
         * @param reward The reward to add
         * @return This builder for chaining
         */
        public Builder keyReward(Reward reward) {
            this.rewards.add(reward);
            return this;
        }

        /**
         * Adds multiple rewards to this gateway.
         * 
         * @param rewards The rewards to add
         * @return This builder for chaining
         */
        public Builder keyRewards(List<Reward> rewards) {
            this.rewards.addAll(rewards);
            return this;
        }

        /**
         * Adds a failure condition to this gateway.
         * 
         * @param failure The failure to add
         * @return This builder for chaining
         */
        public Builder failure(Failure failure) {
            this.failures.add(failure);
            return this;
        }

        /**
         * Adds multiple failure conditions to this gateway.
         * 
         * @param failures The failures to add
         * @return This builder for chaining
         */
        public Builder failures(List<Failure> failures) {
            this.failures.addAll(failures);
            return this;
        }

        /**
         * Sets the spawn algorithm for this gateway.
         * 
         * @param spawnAlgo The spawn algorithm to use
         * @return This builder for chaining
         */
        public Builder spawnAlgorithm(SpawnAlgorithm spawnAlgo) {
            this.spawnAlgo = spawnAlgo;
            return this;
        }

        /**
         * Sets the rules for this gateway.
         * 
         * @param rules The rules to use
         * @return This builder for chaining
         */
        public Builder rules(GateRules rules) {
            this.rules = rules;
            return this;
        }

        /**
         * Sets the boss event settings for this gateway.
         * 
         * @param bossSettings The boss event settings to use
         * @return This builder for chaining
         */
        public Builder bossSettings(BossEventSettings bossSettings) {
            this.bossSettings = bossSettings;
            return this;
        }

        /**
         * Builds a new NormalGateway with the configured parameters.
         * 
         * @return A new NormalGateway instance
         * @throws IllegalStateException if required parameters are missing
         */
        public NormalGateway build() {
            if (size == null) {
                throw new IllegalStateException("Gateway size must be specified");
            }

            if (color == null) {
                throw new IllegalStateException("Gateway color must be specified");
            }

            if (waves.isEmpty()) {
                throw new IllegalStateException("Gateway must have at least one wave");
            }

            return new NormalGateway(
                size,
                color,
                Collections.unmodifiableList(new ArrayList<>(waves)),
                Collections.unmodifiableList(new ArrayList<>(rewards)),
                Collections.unmodifiableList(new ArrayList<>(failures)),
                spawnAlgo,
                rules,
                bossSettings);
        }
    }

}
