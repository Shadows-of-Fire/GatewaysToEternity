package shadows.gateways;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mojang.serialization.Codec;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.RegistryEvent.Register;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import shadows.gateways.client.GatewayParticle;
import shadows.gateways.client.GatewayTickableSound;
import shadows.gateways.command.GatewayCommand;
import shadows.gateways.entity.GatewayEntity;
import shadows.gateways.gate.GatewayManager;
import shadows.gateways.gate.Reward;
import shadows.gateways.item.GateOpenerItem;
import shadows.gateways.net.ParticleMessage;
import shadows.gateways.recipe.GatewayRecipeSerializer;
import shadows.placebo.network.MessageHelper;

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
		MessageHelper.registerMessage(CHANNEL, 0, new ParticleMessage());
		MinecraftForge.EVENT_BUS.addListener(this::commands);
	}

	@SubscribeEvent
	public void setup(FMLCommonSetupEvent e) {
		GatewayManager.INSTANCE.registerToBus();
		Reward.initSerializers();
	}

	@SubscribeEvent
	public void registerEntities(Register<EntityType<?>> e) {
		//Formatter::off
		e.getRegistry().register(EntityType.Builder
				.<GatewayEntity>of(GatewayEntity::new, MobCategory.MISC)
				.setTrackingRange(5)
				.setUpdateInterval(20)
				.sized(2F, 2.5F)
				.setCustomClientFactory((se, w) -> {
					GatewayEntity ent = new GatewayEntity(GatewayObjects.GATEWAY, w);
					GatewayTickableSound.startGatewaySound(ent);
					return ent;
				})
				.build("gateway")
				.setRegistryName("gateway"));
		//Formatter::on
	}

	@SubscribeEvent
	public void registerItems(Register<Item> e) {
		e.getRegistry().register(new GateOpenerItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON).tab(CreativeModeTab.TAB_MISC), GatewayEntity::new).setRegistryName("gate_opener"));
	}

	@SubscribeEvent
	public void registerSerializers(Register<RecipeSerializer<?>> e) {
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
			public Codec<GatewayParticle.Data> codec() {
				return GatewayParticle.Data.CODEC;
			}
		}.setRegistryName("glow"));
	}

	public void commands(RegisterCommandsEvent e) {
		GatewayCommand.register(e.getDispatcher());
	}

}
