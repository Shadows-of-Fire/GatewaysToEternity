package dev.shadowsoffire.gateways.gate;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import com.google.common.base.Preconditions;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.shadowsoffire.gateways.Gateways;
import dev.shadowsoffire.placebo.codec.CodecMap;
import dev.shadowsoffire.placebo.codec.CodecProvider;
import dev.shadowsoffire.placebo.codec.PlaceboCodecs;
import dev.shadowsoffire.placebo.json.ChancedEffectInstance;
import dev.shadowsoffire.placebo.json.GearSet;
import dev.shadowsoffire.placebo.json.GearSetRegistry;
import dev.shadowsoffire.placebo.json.NBTAdapter;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Creeper;
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
                PlaceboCodecs.nullableField(GearSetRegistry.INSTANCE.holderCodec(), "gear_set", GearSetRegistry.INSTANCE.emptyHolder()).forGetter(a -> a.gearSet),
                PlaceboCodecs.nullableField(ChancedEffectInstance.CODEC.listOf(), "effects", Collections.emptyList()).forGetter(t -> t.effects),
                PlaceboCodecs.nullableField(Codec.BOOL, "finalize_spawn", true).forGetter(t -> t.finalizeSpawn),
                PlaceboCodecs.nullableField(Codec.intRange(1, 256), "count", 1).forGetter(t -> t.count))
            .apply(inst, StandardWaveEntity::new));

        protected final EntityType<?> type;
        protected final String desc;
        protected final CompoundTag tag;
        protected final DynamicHolder<GearSet> gearSet;
        protected final List<ChancedEffectInstance> effects;
        protected final boolean finalizeSpawn;
        protected final int count;

        public StandardWaveEntity(EntityType<?> type, Optional<String> desc, Optional<CompoundTag> tag, DynamicHolder<GearSet> gearSet, List<ChancedEffectInstance> effects, boolean finalizeSpawn, int count) {
            this.type = type;
            this.desc = desc.orElse(type.getDescriptionId());
            this.tag = tag.orElse(new CompoundTag());
            this.tag.putString("id", EntityType.getKey(type).toString());
            this.gearSet = gearSet;
            this.effects = effects;
            Preconditions.checkArgument(effects.stream().allMatch(i -> i.getChance() == 1));
            this.finalizeSpawn = finalizeSpawn;
            this.count = count;
        }

        @Override
        public LivingEntity createEntity(Level level) {
            Entity ent = EntityType.loadEntityRecursive(this.tag, level, Function.identity());
            if (ent instanceof LivingEntity living) {
                if (this.gearSet.isBound()) {
                    this.gearSet.get().apply(living);
                }

                int duration = living instanceof Creeper ? 6000 : Integer.MAX_VALUE;
                for (ChancedEffectInstance inst : this.effects) {
                    living.addEffect(inst.createInstance(level.random, duration));
                }
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
