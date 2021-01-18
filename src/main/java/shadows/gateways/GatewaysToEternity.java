package shadows.gateways;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mojang.serialization.Codec;

import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.Rarity;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.particles.ParticleType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent.Register;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import shadows.gateways.client.GatewayParticle;
import shadows.gateways.client.GatewayTickableSound;
import shadows.gateways.entity.AbstractGatewayEntity;
import shadows.gateways.entity.SmallGatewayEntity;
import shadows.gateways.item.GateOpenerItem;
import shadows.gateways.net.ParticleMessage;
import shadows.gateways.recipe.GatewayRecipeSerializer;
import shadows.placebo.util.NetworkUtils;

@Mod(GatewaysToEternity.MODID)
public class GatewaysToEternity {

	public static final String MODID = "gateways";
	public static final Logger LOGGER = LogManager.getLogger("Gateways to Eternity");
	//Formatter::off
    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(MODID, "channel"))
            .clientAcceptedVersions(s->true)
            .serverAcceptedVersions(s->true)
            .networkProtocolVersion(() -> "1.0.0")
            .simpleChannel();
    //Formatter::on

	public GatewaysToEternity() {
		FMLJavaModLoadingContext.get().getModEventBus().register(this);
		NetworkUtils.registerMessage(CHANNEL, 0, new ParticleMessage());
	}

	@SubscribeEvent
	public void registerEntities(Register<EntityType<?>> e) {
		//Formatter::off
		e.getRegistry().register(EntityType.Builder
				.<AbstractGatewayEntity>create(SmallGatewayEntity::new, EntityClassification.MISC)
				.setTrackingRange(5)
				.setUpdateInterval(20)
				.size(2F, 2.5F)
				.setCustomClientFactory((se, w) -> {
					AbstractGatewayEntity ent = new SmallGatewayEntity(GatewayObjects.SMALL_GATEWAY, w);
					GatewayTickableSound.startGatewaySound(ent);
					return ent;
				})
				.build("small_gateway")
				.setRegistryName("small_gateway"));
		//Formatter::on
	}

	@SubscribeEvent
	public void registerItems(Register<Item> e) {
		e.getRegistry().register(new GateOpenerItem(new Item.Properties().maxStackSize(1).rarity(Rarity.UNCOMMON).group(ItemGroup.MISC), SmallGatewayEntity::new).setRegistryName("small_gate_opener"));
	}

	@SubscribeEvent
	public void registerSerializers(Register<IRecipeSerializer<?>> e) {
		e.getRegistry().register(GatewayRecipeSerializer.INSTANCE.setRegistryName("gate_recipe"));
	}

	@SubscribeEvent
	public void registerSounds(Register<SoundEvent> e) {
		//Formatter::off
		e.getRegistry().registerAll(
				new SoundEvent(new ResourceLocation(MODID, "gate_warp")).setRegistryName("gate_warp"),
				new SoundEvent(new ResourceLocation(MODID, "gate_ambient")).setRegistryName("gate_ambient"),
				new SoundEvent(new ResourceLocation(MODID, "gate_start")).setRegistryName("gate_start"),
				new SoundEvent(new ResourceLocation(MODID, "gate_end")).setRegistryName("gate_end")
		);
		//Formatter::on
	}

	@SubscribeEvent
	public void registerParticles(Register<ParticleType<?>> e) {
		e.getRegistry().register(new ParticleType<GatewayParticle.Data>(false, GatewayParticle.Data.DESERIALIZER) {
			@Override
			public Codec<GatewayParticle.Data> func_230522_e_() {
				return GatewayParticle.Data.CODEC;
			}
		}.setRegistryName("glow"));
	}

}
