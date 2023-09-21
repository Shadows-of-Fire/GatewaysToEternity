package dev.shadowsoffire.gateways.gate;

import java.util.List;

import com.mojang.serialization.Codec;

import dev.shadowsoffire.gateways.entity.GatewayEntity;
import dev.shadowsoffire.gateways.gate.SpawnAlgorithms.SpawnAlgorithm;
import dev.shadowsoffire.placebo.codec.CodecProvider;
import dev.shadowsoffire.placebo.codec.PlaceboCodecs;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public interface Gateway extends CodecProvider<Gateway> {

    Size size();

    TextColor color();

    List<Failure> failures();

    SpawnAlgorithm spawnAlgo();

    GateRules rules();

    BossEventSettings bossSettings();

    default double getLeashRangeSq() {
        double leashRange = this.rules().leashRange();
        return leashRange * leashRange;
    }

    GatewayEntity createEntity(Level level, Player summoner);

    void appendPearlTooltip(Level level, List<Component> tooltips, TooltipFlag flag);

    void renderBossBar(GatewayEntity gate, Object gfx, int x, int y, boolean isInWorld);

    public static enum Size {
        SMALL(1F, EntityDimensions.fixed(2F, 2F)),
        MEDIUM(2F, EntityDimensions.fixed(4F, 4F)),
        LARGE(2.5F, EntityDimensions.fixed(5.5F, 5.5F));

        public static final Codec<Size> CODEC = PlaceboCodecs.enumCodec(Size.class);

        private final float scale;
        private final EntityDimensions dims;

        Size(float scale, EntityDimensions dims) {
            this.scale = scale;
            this.dims = dims;
        }

        public float getScale() {
            return this.scale;
        }

        public EntityDimensions getDims() {
            return this.dims;
        }
    }

}
