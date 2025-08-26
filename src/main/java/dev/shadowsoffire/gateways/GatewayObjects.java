package dev.shadowsoffire.gateways;

import java.util.function.Supplier;

import com.mojang.serialization.MapCodec;

import dev.shadowsoffire.gateways.advancements.FinishGatewayTrigger;
import dev.shadowsoffire.gateways.client.GatewayParticleData;
import dev.shadowsoffire.gateways.entity.EndlessGatewayEntity;
import dev.shadowsoffire.gateways.entity.NormalGatewayEntity;
import dev.shadowsoffire.gateways.gate.Gateway;
import dev.shadowsoffire.gateways.gate.GatewayRegistry;
import dev.shadowsoffire.gateways.item.GatePearlItem;
import dev.shadowsoffire.gateways.recipe.GatewayRecipeSerializer;
import dev.shadowsoffire.placebo.registry.DeferredHelper;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;

public class GatewayObjects {

    private static final DeferredHelper R = DeferredHelper.create(Gateways.MODID);

    public static final Supplier<EntityType<NormalGatewayEntity>> NORMAL_GATEWAY = R.entity("normal_gateway", () -> EntityType.Builder
        .<NormalGatewayEntity>of(NormalGatewayEntity::new, MobCategory.MISC)
        .setTrackingRange(5)
        .setUpdateInterval(20)
        .sized(2F, 3F)
        .build("gateway"));

    public static final Supplier<EntityType<EndlessGatewayEntity>> ENDLESS_GATEWAY = R.entity("endless_gateway", () -> EntityType.Builder
        .<EndlessGatewayEntity>of(EndlessGatewayEntity::new, MobCategory.MISC)
        .setTrackingRange(5)
        .setUpdateInterval(20)
        .sized(2F, 3F)
        .build("gateway"));

    public static final Holder<Item> GATE_PEARL = R.item("gate_pearl", () -> new GatePearlItem(new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final Holder<SoundEvent> GATE_AMBIENT = sound("gate_ambient");
    public static final Holder<SoundEvent> GATE_WARP = sound("gate_warp");
    public static final Holder<SoundEvent> GATE_START = sound("gate_start");
    public static final Holder<SoundEvent> GATE_END = sound("gate_end");

    public static final Supplier<GatewayRecipeSerializer> GATE_RECIPE = R.recipeSerializer("gate_recipe", GatewayRecipeSerializer::new);

    public static final Supplier<ParticleType<GatewayParticleData>> GLOW = R.particle("glow", () -> new ParticleType<GatewayParticleData>(false){

        @Override
        public MapCodec<GatewayParticleData> codec() {
            return GatewayParticleData.CODEC;
        }

        @Override
        public StreamCodec<? super RegistryFriendlyByteBuf, GatewayParticleData> streamCodec() {
            return GatewayParticleData.STREAM_CODEC;
        }

    });

    public static final Holder<CreativeModeTab> TAB = R.creativeTab("tab", b -> b.title(Component.translatable("itemGroup.gateways")).icon(() -> GATE_PEARL.value().getDefaultInstance()));

    public static final ResourceLocation GATES_DEFEATED = R.custom("gates_defeated", Registries.CUSTOM_STAT, Gateways.loc("gates_defeated"));

    public static final DataComponentType<DynamicHolder<Gateway>> GATEWAY_COMPONENT = R.component("gateway", b -> b.persistent(GatewayRegistry.INSTANCE.holderCodec()).networkSynchronized(GatewayRegistry.INSTANCE.holderStreamCodec()));

    public static final FinishGatewayTrigger FINISH_GATEWAY = R.criteriaTrigger("finish_gateway", new FinishGatewayTrigger());

    private static Holder<SoundEvent> sound(String name) {
        return R.sound(name, () -> SoundEvent.createVariableRangeEvent(Gateways.loc(name)));
    }

    static void bootstrap(IEventBus bus) {
        bus.register(R);
    }
}
