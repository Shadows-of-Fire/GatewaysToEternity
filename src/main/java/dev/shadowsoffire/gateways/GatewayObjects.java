package dev.shadowsoffire.gateways;

import com.mojang.serialization.Codec;

import dev.shadowsoffire.gateways.client.GatewayParticleData;
import dev.shadowsoffire.gateways.client.GatewayTickableSound;
import dev.shadowsoffire.gateways.entity.GatewayEntity;
import dev.shadowsoffire.gateways.item.GatePearlItem;
import dev.shadowsoffire.gateways.recipe.GatewayRecipeSerializer;
import dev.shadowsoffire.placebo.registry.DeferredHelper;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.RegistryObject;

public class GatewayObjects {

    private static final DeferredHelper R = DeferredHelper.create(Gateways.MODID);

    public static final RegistryObject<EntityType<GatewayEntity>> GATEWAY = R.entity("gateway", () -> EntityType.Builder
        .<GatewayEntity>of(GatewayEntity::new, MobCategory.MISC)
        .setTrackingRange(5)
        .setUpdateInterval(20)
        .sized(2F, 3F)
        .setCustomClientFactory((se, w) -> {
            GatewayEntity ent = new GatewayEntity(GatewayObjects.GATEWAY.get(), w);
            GatewayTickableSound.startGatewaySound(ent);
            return ent;
        })
        .build("gateway"));

    public static final RegistryObject<GatePearlItem> GATE_PEARL = R.item("gate_pearl", () -> new GatePearlItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<SoundEvent> GATE_AMBIENT = sound("gate_ambient");
    public static final RegistryObject<SoundEvent> GATE_WARP = sound("gate_warp");
    public static final RegistryObject<SoundEvent> GATE_START = sound("gate_start");
    public static final RegistryObject<SoundEvent> GATE_END = sound("gate_end");

    public static final RegistryObject<ParticleType<GatewayParticleData>> GLOW = R.particle("glow", () -> new ParticleType<>(false, GatewayParticleData.DESERIALIZER){
        @Override
        public Codec<GatewayParticleData> codec() {
            return GatewayParticleData.CODEC;
        }
    });

    public static final RegistryObject<GatewayRecipeSerializer> GATE_RECIPE = R.recipeSerializer("gate_recipe", GatewayRecipeSerializer::new);

    public static final ResourceKey<CreativeModeTab> TAB_KEY = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Gateways.loc("tab"));
    public static final RegistryObject<CreativeModeTab> TAB = R.tab("tab", () -> CreativeModeTab.builder().title(Component.translatable("itemGroup.gateways")).icon(() -> GATE_PEARL.get().getDefaultInstance()).build());

    public static final RegistryObject<ResourceLocation> GATES_DEFEATED = R.custom("gates_defeated", Registries.CUSTOM_STAT, () -> Gateways.loc("gates_defeated"));

    private static RegistryObject<SoundEvent> sound(String name) {
        return R.sound(name, () -> SoundEvent.createVariableRangeEvent(Gateways.loc(name)));
    }

    static void bootstrap() {}
}
