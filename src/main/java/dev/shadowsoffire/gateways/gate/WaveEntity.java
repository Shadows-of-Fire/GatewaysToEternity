package dev.shadowsoffire.gateways.gate;

import java.util.Optional;
import java.util.function.Function;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.shadowsoffire.gateways.Gateways;
import dev.shadowsoffire.placebo.codec.PlaceboCodecs;
import dev.shadowsoffire.placebo.codec.PlaceboCodecs.CodecProvider;
import dev.shadowsoffire.placebo.json.NBTAdapter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

public interface WaveEntity extends CodecProvider<WaveEntity> {

    public static final BiMap<ResourceLocation, Codec<? extends WaveEntity>> CODECS = HashBiMap.create();

    public static final Codec<WaveEntity> CODEC = PlaceboCodecs.mapBackedDefaulted("Wave Entity", CODECS, StandardWaveEntity.CODEC);

    public static void initSerializers() {
        register("default", StandardWaveEntity.CODEC);
    }

    private static void register(String id, Codec<? extends WaveEntity> codec) {
        CODECS.put(Gateways.loc(id), codec);
    }

    /**
     * Creates the entity to be spawned in the current wave.
     *
     * @param level The level.
     * @return The entity, or null if an error occured. Null will end the gate.
     */
    public LivingEntity createEntity(Level level);

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

    public static class StandardWaveEntity implements WaveEntity {

        public static Codec<StandardWaveEntity> CODEC = RecordCodecBuilder.create(inst -> inst
            .group(
                ForgeRegistries.ENTITY_TYPES.getCodec().fieldOf("entity").forGetter(t -> t.type),
                Codec.STRING.optionalFieldOf("desc").forGetter(t -> Optional.of(t.desc)),
                NBTAdapter.EITHER_CODEC.optionalFieldOf("nbt").forGetter(t -> Optional.of(t.tag)),
                Codec.BOOL.optionalFieldOf("finalize_spawn", true).forGetter(t -> t.finalizeSpawn),
                Codec.intRange(1, 256).optionalFieldOf("count", 1).forGetter(t -> t.count))
            .apply(inst, StandardWaveEntity::new));

        protected final EntityType<?> type;
        protected final String desc;
        protected final CompoundTag tag;
        protected final boolean finalizeSpawn;
        protected final int count;

        public StandardWaveEntity(EntityType<?> type, Optional<String> desc, Optional<CompoundTag> tag, boolean finalizeSpawn, int count) {
            this.type = type;
            this.desc = desc.orElse(type.getDescriptionId());
            this.tag = tag.orElse(new CompoundTag());
            this.tag.putString("id", EntityType.getKey(type).toString());
            this.finalizeSpawn = finalizeSpawn;
            this.count = count;
        }

        @Override
        public LivingEntity createEntity(Level level) {
            Entity ent = EntityType.loadEntityRecursive(this.tag, level, Function.identity());
            return ent instanceof LivingEntity l ? l : null;
        }

        @Override
        public MutableComponent getDescription() {
            return Component.translatable("tooltip.gateways.list1", getCount(), Component.translatable(this.desc));
        }

        @Override
        public boolean shouldFinalizeSpawn() {
            return finalizeSpawn;
        }

        @Override
        public int getCount() {
            return this.count;
        }

        @Override
        public Codec<? extends WaveEntity> getCodec() {
            return CODEC;
        }

    }

}
