package dev.shadowsoffire.gateways.gate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.shadowsoffire.placebo.codec.PlaceboCodecs;
import net.minecraft.server.level.ServerBossEvent;

/**
 * Boss event settings for a Gateway. Controls how the {@link ServerBossEvent} is managed.
 * <p>
 * Currently you can only choose between a boss bar or an above-gateway nameplate. Fog is only supported with boss bars.
 */
public record BossEventSettings(Mode mode, boolean fog) {

    public static final BossEventSettings DEFAULT = new BossEventSettings(Mode.BOSS_BAR, true);

    public static Codec<BossEventSettings> CODEC = RecordCodecBuilder.create(inst -> inst
        .group(
            Mode.CODEC.optionalFieldOf("mode", Mode.BOSS_BAR).forGetter(BossEventSettings::mode),
            Codec.BOOL.optionalFieldOf("fog", true).forGetter(BossEventSettings::fog))
        .apply(inst, BossEventSettings::new));

    public boolean drawAsName() {
        return this.mode == Mode.NAME_PLATE;
    }

    public boolean drawAsBar() {
        return this.mode == Mode.BOSS_BAR;
    }

    public static enum Mode {
        BOSS_BAR,
        NAME_PLATE;

        public static final Codec<Mode> CODEC = PlaceboCodecs.enumCodec(Mode.class);
    }

}
