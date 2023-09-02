package dev.shadowsoffire.gateways.gate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.shadowsoffire.placebo.codec.PlaceboCodecs;

public record BossEventSettings(Mode mode, boolean fog) {

    public static final BossEventSettings DEFAULT = new BossEventSettings(Mode.BOSS_BAR, true);

    public static Codec<BossEventSettings> CODEC = RecordCodecBuilder.create(inst -> inst
        .group(
            Mode.CODEC.fieldOf("mode").forGetter(BossEventSettings::mode),
            Codec.BOOL.fieldOf("fog").forGetter(BossEventSettings::fog))
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
