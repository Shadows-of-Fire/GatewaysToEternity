package shadows.gateways;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mojang.serialization.Codec;

import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Rarity;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.particles.ParticleType;
import net.minecraft.stats.IStatFormatter;
import net.minecraft.stats.Stats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.registry.Registry;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.RegistryEvent.Register;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import shadows.gateways.client.GatewayParticleData;
import shadows.gateways.client.GatewayTickableSound;
import shadows.gateways.command.GatewayCommand;
import shadows.gateways.entity.GatewayEntity;
import shadows.gateways.gate.GatewayManager;
import shadows.gateways.gate.Reward;
import shadows.gateways.gate.WaveEntity;
import shadows.gateways.item.GatePearlItem;
import shadows.gateways.net.ParticleMessage;
import shadows.gateways.recipe.GatewayRecipeSerializer;
import shadows.placebo.util.NetworkUtils;

@Mod(Gateways.MODID)
public class Gateways {

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
	public static final ItemGroup TAB = new ItemGroup(MODID) {

		@Override
		public ItemStack makeIcon() {
			return new ItemStack(GatewayObjects.GATE_PEARL);
		}

	};

	public Gateways() {
		FMLJavaModLoadingContext.get().getModEventBus().register(this);
		NetworkUtils.registerMessage(CHANNEL, 0, new ParticleMessage());
		MinecraftForge.EVENT_BUS.addListener(this::commands);
		MinecraftForge.EVENT_BUS.addListener(this::starting);
	}

	@SubscribeEvent
	public void setup(FMLCommonSetupEvent e) {
		Reward.initSerializers();
		WaveEntity.initSerializers();
	}

	@SubscribeEvent
	public void registerEntities(Register<EntityType<?>> e) {
		//Formatter::off
		e.getRegistry().register(EntityType.Builder
				.<GatewayEntity>of(GatewayEntity::new, EntityClassification.MISC)
				.setTrackingRange(5)
				.setUpdateInterval(20)
				.sized(2F, 3F)
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
		e.getRegistry().register(new GatePearlItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON).tab(TAB)).setRegistryName("gate_pearl"));
		registerStat(GatewayObjects.Stats.STAT_GATES_DEFEATED, IStatFormatter.DEFAULT);
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
		e.getRegistry().register(new ParticleType<GatewayParticleData>(false, GatewayParticleData.DESERIALIZER) {
			@Override
			public Codec<GatewayParticleData> codec() {
				return GatewayParticleData.CODEC;
			}
		}.setRegistryName("glow"));
	}

	public void commands(RegisterCommandsEvent e) {
		GatewayCommand.register(e.getDispatcher());
	}

	public void starting(AddReloadListenerEvent e) {
		e.addListener(GatewayManager.INSTANCE);
	}

	private static void registerStat(ResourceLocation id, IStatFormatter pFormatter) {
		Registry.register(Registry.CUSTOM_STAT, id, id);
		Stats.CUSTOM.get(id, pFormatter);
	}

}
