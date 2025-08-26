package dev.shadowsoffire.gateways.gate;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;

import dev.shadowsoffire.gateways.Gateways;
import dev.shadowsoffire.gateways.entity.GatewayEntity;
import dev.shadowsoffire.placebo.codec.CodecMap;
import dev.shadowsoffire.placebo.codec.CodecProvider;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

public interface WaveEntity extends CodecProvider<WaveEntity> {

    public static final CodecMap<WaveEntity> CODEC = new CodecMap<>("Wave Entity");

    public static void initCodecs() {
        register("standard", StandardWaveEntity.CODEC);
        CODEC.setDefaultCodec(StandardWaveEntity.CODEC);
    }

    private static void register(String id, Codec<? extends WaveEntity> codec) {
        CODEC.register(Gateways.loc(id), codec);
    }

    /**
     * Creates the entity to be spawned in the current wave.
     *
     * @param level The level.
     * @return The entity, or null if an error occured. Null will end the gate.
     */
    @Nullable
    default LivingEntity createEntity(ServerLevel level, GatewayEntity gate) {
        return createEntity(level);
    }

    /**
     * Gets the tooltip form of this wave entity for display in the Gate Pearl's "Waves" section.
     */
    public MutableComponent getDescription();

    /**
     * If the spawned wave entity should have {@link Mob#finalizeSpawn} called.
     */
    public boolean shouldFinalizeSpawn();

    /**
     * The number of times this wave entity should be spawned.
     */
    public int getCount();

    /**
     * @deprecated Use {@link #createEntity(ServerLevel, GatewayEntity)}.
     */
    @Deprecated(forRemoval = true)
    default LivingEntity createEntity(Level level) {
        return null;
    }

}
