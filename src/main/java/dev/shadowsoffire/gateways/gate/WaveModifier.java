package dev.shadowsoffire.gateways.gate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.shadowsoffire.apothic_attributes.ApothicAttributes;
import dev.shadowsoffire.gateways.Gateways;
import dev.shadowsoffire.gateways.entity.GatewayEntity;
import dev.shadowsoffire.placebo.codec.CodecMap;
import dev.shadowsoffire.placebo.codec.CodecProvider;
import dev.shadowsoffire.placebo.dynreg.DynamicHolder;
import dev.shadowsoffire.placebo.json.ChancedEffectInstance;
import dev.shadowsoffire.placebo.json.RandomAttributeModifier;
import dev.shadowsoffire.placebo.systems.gear.GearSet;
import dev.shadowsoffire.placebo.systems.gear.GearSetRegistry;
import dev.shadowsoffire.placebo.util.StepFunction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.storage.loot.LootTable;

public interface WaveModifier extends CodecProvider<WaveModifier> {

    public static final CodecMap<WaveModifier> CODEC = new CodecMap<>("Gateway Wave Modifier");

    /**
     * Applies this modifier to the given entity, which will have been freshly spawned by a Wave.
     * 
     * @param entity   The fresh Wave Entity.
     * @param gate     The GatewayEntity that spawned the wave.
     * @param summoner The Player that summoned the wave, or the closest player.
     */
    void apply(LivingEntity entity, GatewayEntity gate);

    /**
     * Adds this wave modifier to the gate pearl's tooltip.
     */
    public void appendHoverText(TooltipContext ctx, Consumer<MutableComponent> list);

    public static void initCodecs() {
        register("mob_effect", EffectModifier.CODEC);
        register("attribute", AttributeModifier.CODEC);
        register("gear_set", GearSetModifier.CODEC);
        register("loot_table", LootTableModifier.CODEC);
        CODEC.setDefaultCodec(AttributeModifier.CODEC);
    }

    private static void register(String id, Codec<? extends WaveModifier> codec) {
        CODEC.register(Gateways.loc(id), codec);
    }

    /**
     * Wave modifier that applies a mob effect to the wave entities.
     * <p>
     * The effect is applied with infinite duration, unless the entity is a creeper, in which case the duration is reduced to 5 minutes.
     */
    public static record EffectModifier(ChancedEffectInstance effect) implements WaveModifier {

        public static Codec<EffectModifier> CODEC = RecordCodecBuilder.create(inst -> inst
            .group(
                ChancedEffectInstance.CONSTANT_CODEC.fieldOf("effect").forGetter(EffectModifier::effect))
            .apply(inst, EffectModifier::new));

        @Override
        public Codec<? extends WaveModifier> getCodec() {
            return CODEC;
        }

        @Override
        public void apply(LivingEntity entity, GatewayEntity gate) {
            int duration = entity instanceof Creeper ? 6000 : Integer.MAX_VALUE;
            entity.addEffect(effect.createDeterministic(duration));
        }

        @Override
        public void appendHoverText(TooltipContext ctx, Consumer<MutableComponent> list) {
            List<Component> output = new ArrayList<>();
            PotionContents.addPotionTooltip(Arrays.asList(this.effect.createDeterministic(1)), output::add, 1, ctx.tickRate());
            list.accept(Component.literal(output.get(0).getString()));
        }

        public static EffectModifier create(Holder<MobEffect> effect, int amplifier, boolean ambient, boolean visible) {
            return new EffectModifier(new ChancedEffectInstance(1, effect, StepFunction.constant(amplifier), ambient, visible));
        }

        public static EffectModifier create(Holder<MobEffect> effect, int amplifier) {
            return create(effect, amplifier, false, true);
        }
    }

    /**
     * Wave modifier that applies an attribute modifier to the wave entities.
     * <p>
     * The modifier will be ignored if the entity does not have the attribute.
     * <p>
     * Uses generated modifier ids, since wave modifiers are ephemeral (apply-once-remove-never).
     * Each application creates a unique id, so repeated applications stack instead of colliding.
     */
    public static record AttributeModifier(RandomAttributeModifier modifier) implements WaveModifier {

        public static Codec<AttributeModifier> CODEC = RandomAttributeModifier.constantGeneratedCodec(Gateways.loc("wave_modifier")).xmap(AttributeModifier::new, AttributeModifier::modifier);

        @Override
        public Codec<? extends WaveModifier> getCodec() {
            return CODEC;
        }

        @Override
        public void apply(LivingEntity entity, GatewayEntity gate) {
            AttributeInstance inst = entity.getAttribute(this.modifier.attribute());
            if (inst == null) return;
            inst.addPermanentModifier(this.modifier.createDeterministic());
        }

        @Override
        public void appendHoverText(TooltipContext ctx, Consumer<MutableComponent> list) {
            list.accept(modifier.attribute().value().toComponent(modifier.createDeterministic(modifier.modifierId()), ApothicAttributes.getTooltipFlag()));
        }

        public static AttributeModifier create(Holder<Attribute> attribute, Operation op, float value) {
            return new AttributeModifier(RandomAttributeModifier.generated(attribute, op, StepFunction.constant(value), Gateways.loc("wave_modifier")));
        }

    }

    /**
     * Wave modifier that applies a gear set to the wave entities.
     * <p>
     * The applied gear set should be deterministic to a reasonable degree, since it must be translated to a single name.
     */
    public static record GearSetModifier(DynamicHolder<GearSet> set) implements WaveModifier {

        public static Codec<GearSetModifier> CODEC = RecordCodecBuilder.create(inst -> inst
            .group(
                GearSetRegistry.INSTANCE.holderCodec().fieldOf("gear_set").forGetter(GearSetModifier::set))
            .apply(inst, GearSetModifier::new));

        @Override
        public Codec<? extends WaveModifier> getCodec() {
            return CODEC;
        }

        @Override
        public void apply(LivingEntity entity, GatewayEntity gate) {
            this.set.get().apply(entity);
        }

        @Override
        public void appendHoverText(TooltipContext ctx, Consumer<MutableComponent> list) {
            list.accept(Component.translatable("modifier.gateways.gear_set", Component.translatable(this.set.getId().toLanguageKey("gear_set"))));
        }

        public static GearSetModifier create(Identifier set) {
            return new GearSetModifier(GearSetRegistry.INSTANCE.holder(set));
        }

    }

    public static record LootTableModifier(ResourceKey<LootTable> table) implements WaveModifier {

        public static Codec<LootTableModifier> CODEC = RecordCodecBuilder.create(inst -> inst
            .group(
                ResourceKey.codec(Registries.LOOT_TABLE).fieldOf("loot_table").forGetter(LootTableModifier::table))
            .apply(inst, LootTableModifier::new));

        @Override
        public Codec<? extends WaveModifier> getCodec() {
            return CODEC;
        }

        @Override
        public void apply(LivingEntity entity, GatewayEntity gate) {
            if (entity instanceof Mob mob) {
                mob.lootTable = Optional.of(this.table);
            }
        }

        @Override
        public void appendHoverText(TooltipContext ctx, Consumer<MutableComponent> list) {}

        public static LootTableModifier create(ResourceKey<LootTable> table) {
            return new LootTableModifier(table);
        }

        public static LootTableModifier createEmpty() {
            return create(ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace("empty")));
        }
    }
}
