package dev.shadowsoffire.gateways.gate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.shadowsoffire.gateways.Gateways;
import dev.shadowsoffire.gateways.entity.GatewayEntity;
import dev.shadowsoffire.placebo.json.NBTAdapter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityProcessor;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

public record StandardWaveEntity(EntityType<?> type, Optional<String> desc, Optional<CompoundTag> tag, List<WaveModifier> modifiers, boolean finalizeSpawn, int count) implements WaveEntity {

    public static Codec<StandardWaveEntity> CODEC = RecordCodecBuilder.create(inst -> inst
        .group(
            BuiltInRegistries.ENTITY_TYPE.byNameCodec().fieldOf("entity").forGetter(StandardWaveEntity::type),
            Codec.STRING.optionalFieldOf("desc").forGetter(StandardWaveEntity::desc),
            NBTAdapter.EITHER_CODEC.optionalFieldOf("nbt").forGetter(StandardWaveEntity::tag),
            WaveModifier.CODEC.listOf().optionalFieldOf("modifiers", Collections.emptyList()).forGetter(StandardWaveEntity::modifiers),
            Codec.BOOL.optionalFieldOf("finalize_spawn", true).forGetter(StandardWaveEntity::finalizeSpawn),
            Codec.intRange(1, 256).optionalFieldOf("count", 1).forGetter(StandardWaveEntity::count))
        .apply(inst, StandardWaveEntity::new));

    @Override
    public LivingEntity createEntity(ServerLevel level, GatewayEntity gate) {
        CompoundTag data = tag.orElse(new CompoundTag());
        data.putString("id", EntityType.getKey(type).toString());
        Entity ent = EntityType.loadEntityRecursive(data, level, EntitySpawnReason.SPAWNER, EntityProcessor.NOP);
        if (ent == null) {
            Gateways.logSpawnDebug(gate, this, "Entity deserialization returned null");
            return null;
        }

        if (ent instanceof LivingEntity living) {
            this.modifiers.forEach(m -> m.apply(living, gate));
            return living;
        }

        Gateways.logSpawnDebug(gate, this, "Deserialized entity is not a LivingEntity");
        return null;
    }

    @Override
    public MutableComponent getDescription() {
        String descKey = this.desc.orElse(this.type.getDescriptionId());
        return Component.translatable("tooltip.gateways.with_count", getCount(), Component.translatable(descKey));
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

    public static Builder builder(EntityType<?> type) {
        return new Builder(type);
    }

    public static class Builder {

        private final EntityType<?> type;
        private Optional<String> desc = Optional.empty();
        private Optional<CompoundTag> tag = Optional.empty();
        private final List<WaveModifier> modifiers = new ArrayList<>();
        private boolean finalizeSpawn = true;
        private int count = 1;

        /**
         * Creates a new builder with the required entity type.
         * 
         * @param type The type of entity to spawn
         * @return A new builder instance
         */
        public Builder(EntityType<?> type) {
            this.type = type;
        }

        /**
         * Sets an optional description for this entity.
         * 
         * @param desc The description string, typically a translation key
         * @return This builder for chaining
         */
        public Builder desc(String desc) {
            this.desc = Optional.of(desc);
            return this;
        }

        /**
         * Sets the NBT data for the entity.
         * 
         * @param tag The NBT data
         * @return This builder for chaining
         */
        public Builder nbt(UnaryOperator<CompoundTag> config) {
            CompoundTag tag = this.tag.orElse(new CompoundTag());
            this.tag = Optional.ofNullable(config.apply(tag));
            return this;
        }

        /**
         * Adds a modifier to this wave entity.
         * 
         * @param modifier The modifier to add
         * @return This builder for chaining
         */
        public Builder addModifier(WaveModifier modifier) {
            this.modifiers.add(modifier);
            return this;
        }

        /**
         * Adds multiple modifiers to this wave entity.
         * 
         * @param modifiers The modifiers to add
         * @return This builder for chaining
         */
        public Builder addModifiers(List<WaveModifier> modifiers) {
            this.modifiers.addAll(modifiers);
            return this;
        }

        /**
         * Sets whether to finalize the entity's spawn.
         * 
         * @param finalizeSpawn Whether to finalize the spawn
         * @return This builder for chaining
         */
        public Builder finalizeSpawn(boolean finalizeSpawn) {
            this.finalizeSpawn = finalizeSpawn;
            return this;
        }

        /**
         * Sets the number of entities to spawn.
         * 
         * @param count The number of entities
         * @return This builder for chaining
         */
        public Builder count(int count) {
            if (count < 1 || count > 256) {
                throw new IllegalArgumentException("Entity count must be between 1 and 256");
            }
            this.count = count;
            return this;
        }

        /**
         * Builds a new StandardWaveEntity with the configured parameters.
         * 
         * @return A new StandardWaveEntity instance
         * @throws IllegalStateException if required parameters are missing
         */
        public StandardWaveEntity build() {
            if (type == null) {
                throw new IllegalStateException("Entity type must be specified");
            }

            return new StandardWaveEntity(
                type,
                desc,
                tag,
                Collections.unmodifiableList(new ArrayList<>(modifiers)),
                finalizeSpawn,
                count);
        }
    }

}
