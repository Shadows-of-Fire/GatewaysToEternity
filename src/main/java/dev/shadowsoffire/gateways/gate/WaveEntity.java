package dev.shadowsoffire.gateways.gate;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.shadowsoffire.gateways.Gateways;
import dev.shadowsoffire.placebo.codec.CodecMap;
import dev.shadowsoffire.placebo.codec.CodecProvider;
import dev.shadowsoffire.placebo.codec.PlaceboCodecs;
import dev.shadowsoffire.placebo.json.NBTAdapter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

public interface WaveEntity extends CodecProvider<WaveEntity> {

    public static final CodecMap<WaveEntity> CODEC = new CodecMap<>("Wave Entity");

    public static void initSerializers() {
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
                PlaceboCodecs.nullableField(Codec.STRING, "desc").forGetter(t -> Optional.of(t.desc)),
                PlaceboCodecs.nullableField(NBTAdapter.EITHER_CODEC, "nbt").forGetter(t -> Optional.of(t.tag)),
                PlaceboCodecs.nullableField(WaveModifier.CODEC.listOf(), "modifiers", Collections.emptyList()).forGetter(t -> t.modifiers),
                PlaceboCodecs.nullableField(Codec.BOOL, "finalize_spawn", true).forGetter(t -> t.finalizeSpawn),
                PlaceboCodecs.nullableField(Codec.intRange(1, 256), "count", 1).forGetter(t -> t.count))
            .apply(inst, StandardWaveEntity::new));

        protected final EntityType<?> type;
        protected final String desc;
        protected final CompoundTag tag;
        protected final List<WaveModifier> modifiers;
        protected final boolean finalizeSpawn;
        protected final int count;

        public StandardWaveEntity(EntityType<?> type, Optional<String> desc, Optional<CompoundTag> tag, List<WaveModifier> modifiers, boolean finalizeSpawn, int count) {
            this.type = type;
            this.desc = desc.orElse(type.getDescriptionId());
            this.tag = tag.orElse(new CompoundTag());
            this.tag.putString("id", EntityType.getKey(type).toString());
            this.modifiers = modifiers;
            this.finalizeSpawn = finalizeSpawn;
            this.count = count;
        }

        @Override
        public LivingEntity createEntity(Level level) {
            Entity ent = EntityType.loadEntityRecursive(this.tag, level, Function.identity());
            if (ent instanceof LivingEntity living) {
                this.modifiers.forEach(m -> m.apply(living));
                return living;
            }
            return null;
        }

        @Override
        public MutableComponent getDescription() {
            return Component.translatable("tooltip.gateways.with_count", getCount(), Component.translatable(this.desc));
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
