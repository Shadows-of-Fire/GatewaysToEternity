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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
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
     * @param level
     * @return The entity, or null if an error occured. Null will end the gate.
     */
    public LivingEntity createEntity(Level level);

    public Component getDescription();

    public AABB getAABB(double x, double y, double z);

    public boolean shouldFinalizeSpawn();

    public static class StandardWaveEntity implements WaveEntity {

        // Formatter::off
        public static Codec<StandardWaveEntity> CODEC = RecordCodecBuilder.create(inst -> inst
            .group(
                ForgeRegistries.ENTITY_TYPES.getCodec().fieldOf("entity").forGetter(t -> t.type),
                NBTAdapter.EITHER_CODEC.optionalFieldOf("nbt").forGetter(t -> Optional.ofNullable(t.tag)))
            .apply(inst, StandardWaveEntity::new));
        // Formatter::on

        protected final EntityType<?> type;
        protected final CompoundTag tag;

        public StandardWaveEntity(EntityType<?> type, Optional<CompoundTag> tag) {
            this.type = type;
            this.tag = tag.orElse(new CompoundTag());
            this.tag.putString("id", EntityType.getKey(type).toString());
        }

        @Override
        public LivingEntity createEntity(Level level) {
            Entity ent = EntityType.loadEntityRecursive(this.tag, level, Function.identity());
            return ent instanceof LivingEntity l ? l : null;
        }

        @Override
        public Component getDescription() {
            return Component.translatable(type.getDescriptionId());
        }

        @Override
        public AABB getAABB(double x, double y, double z) {
            return this.type.getAABB(x, y, z);
        }

        @Override
        public boolean shouldFinalizeSpawn() {
            return this.tag.size() == 1 || this.tag.getBoolean("ForceFinalizeSpawn");
        }

        @Override
        public Codec<? extends WaveEntity> getCodec() {
            return CODEC;
        }

    }

}
