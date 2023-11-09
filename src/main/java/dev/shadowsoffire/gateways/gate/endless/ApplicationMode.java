package dev.shadowsoffire.gateways.gate.endless;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.shadowsoffire.gateways.Gateways;
import dev.shadowsoffire.placebo.codec.CodecMap;
import dev.shadowsoffire.placebo.codec.CodecProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public interface ApplicationMode extends CodecProvider<ApplicationMode> {

    public static final CodecMap<ApplicationMode> CODEC = new CodecMap<>("Gateways Endless Application Mode");

    /**
     * Gets the number of times the {@link EndlessModifier}s associated with this application mode will be applied for the current wave.
     * <p>
     * This method is invoked once, at the start of the wave.
     * 
     * @param wave The one-indexed wave number of the wave that is starting.
     * @return The number of times the associated modifier will be applied.
     */
    int getApplicationCount(int wave);

    /**
     * Gets a one-line description that states how often the application mode fires.
     */
    MutableComponent getDescription();

    public static void initSerializers() {
        register("after_wave", AfterWave.CODEC);
        register("after_every_n_waves", AfterEveryNWaves.CODEC);
        register("only_on_wave", OnlyOnWave.CODEC);
        register("only_on_every_n_waves", OnlyOnEveryNWaves.CODEC);
    }

    private static void register(String id, Codec<? extends ApplicationMode> codec) {
        CODEC.register(Gateways.loc(id), codec);
    }

    /**
     * Adds the modifier(s) on the specified wave, remaining for all subsequent waves.
     */
    public static record AfterWave(int wave) implements ApplicationMode {

        public static Codec<AfterWave> CODEC = RecordCodecBuilder.create(inst -> inst
            .group(
                Codec.intRange(1, 1024).fieldOf("wave").forGetter(AfterWave::wave))
            .apply(inst, AfterWave::new));

        @Override
        public int getApplicationCount(int wave) {
            return wave >= this.wave ? 1 : 0;
        }

        @Override
        public MutableComponent getDescription() {
            return Component.translatable("appmode.gateways.after_wave", this.wave);
        }

        @Override
        public Codec<? extends ApplicationMode> getCodec() {
            return CODEC;
        }

    }

    /**
     * Applies the modifier(s) once every N waves, stacking with prior applications, up to M total applications.
     */
    public static record AfterEveryNWaves(int waves, int max) implements ApplicationMode {

        public static Codec<AfterEveryNWaves> CODEC = RecordCodecBuilder.create(inst -> inst
            .group(
                Codec.intRange(1, 1024).fieldOf("waves").forGetter(AfterEveryNWaves::waves),
                Codec.intRange(1, 1024).fieldOf("max").forGetter(AfterEveryNWaves::max))
            .apply(inst, AfterEveryNWaves::new));

        @Override
        public int getApplicationCount(int wave) {
            // Integer division here will cause this to return +1 for every N waves.
            return Math.min(wave / this.waves, this.max);
        }

        @Override
        public MutableComponent getDescription() {
            return Component.translatable("appmode.gateways.after_every_n_waves", this.waves, this.max);
        }

        @Override
        public Codec<? extends ApplicationMode> getCodec() {
            return CODEC;
        }

    }

    /**
     * Applies the modifier only on the specified wave, and not on subsequent waves.
     */
    public static record OnlyOnWave(int wave) implements ApplicationMode {

        public static Codec<OnlyOnWave> CODEC = RecordCodecBuilder.create(inst -> inst
            .group(
                Codec.intRange(1, 1024).fieldOf("wave").forGetter(OnlyOnWave::wave))
            .apply(inst, OnlyOnWave::new));

        @Override
        public int getApplicationCount(int wave) {
            return wave == this.wave ? 1 : 0;
        }

        @Override
        public MutableComponent getDescription() {
            return Component.translatable("appmode.gateways.only_on_wave", this.wave);
        }

        @Override
        public Codec<? extends ApplicationMode> getCodec() {
            return CODEC;
        }

    }

    /**
     * Applies the modifier once every N waves, but not on any others.
     */
    public static record OnlyOnEveryNWaves(int waves) implements ApplicationMode {

        public static Codec<OnlyOnEveryNWaves> CODEC = RecordCodecBuilder.create(inst -> inst
            .group(
                Codec.intRange(1, 1024).fieldOf("waves").forGetter(OnlyOnEveryNWaves::waves))
            .apply(inst, OnlyOnEveryNWaves::new));

        @Override
        public int getApplicationCount(int wave) {
            return wave % waves == 0 ? 1 : 0;
        }

        @Override
        public MutableComponent getDescription() {
            return Component.translatable("appmode.gateways.only_on_every_n_waves", this.waves);
        }

        @Override
        public Codec<? extends ApplicationMode> getCodec() {
            return CODEC;
        }

    }

}
