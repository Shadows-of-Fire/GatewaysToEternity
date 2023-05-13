package shadows.gateways;

import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mojang.serialization.Codec;

import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.stats.StatFormatter;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.living.LivingConversionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import shadows.gateways.client.GatewayParticleData;
import shadows.gateways.client.GatewayTickableSound;
import shadows.gateways.command.GatewayCommand;
import shadows.gateways.entity.GatewayEntity;
import shadows.gateways.gate.Failure;
import shadows.gateways.gate.GatewayManager;
import shadows.gateways.gate.Reward;
import shadows.gateways.gate.WaveEntity;
import shadows.gateways.item.GatePearlItem;
import shadows.gateways.net.ParticleMessage;
import shadows.gateways.recipe.GatewayRecipeSerializer;
import shadows.placebo.network.MessageHelper;

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
	public static final CreativeModeTab TAB = new CreativeModeTab(MODID) {

		@Override
		public ItemStack makeIcon() {
			return new ItemStack(GatewayObjects.GATE_PEARL.get());
		}

	};

	public Gateways() {
		FMLJavaModLoadingContext.get().getModEventBus().register(this);
		MessageHelper.registerMessage(CHANNEL, 0, new ParticleMessage());
		MinecraftForge.EVENT_BUS.addListener(this::commands);
		MinecraftForge.EVENT_BUS.addListener(this::teleport);
		MinecraftForge.EVENT_BUS.addListener(this::convert);
	}

	@SubscribeEvent
	public void setup(FMLCommonSetupEvent e) {
		GatewayManager.INSTANCE.registerToBus();
		Reward.initSerializers();
		WaveEntity.initSerializers();
		Failure.initSerializers();
	}

	@SubscribeEvent
	public void register(RegisterEvent e) {
		if (e.getForgeRegistry() == (Object) ForgeRegistries.ITEMS) registerItems();
		if (e.getForgeRegistry() == (Object) ForgeRegistries.ENTITY_TYPES) registerEntities();
		if (e.getForgeRegistry() == (Object) ForgeRegistries.RECIPE_SERIALIZERS) registerSerializers();
		if (e.getForgeRegistry() == (Object) ForgeRegistries.SOUND_EVENTS) registerSounds();
		if (e.getForgeRegistry() == (Object) ForgeRegistries.PARTICLE_TYPES) registerParticles();
	}

	public void registerEntities() {
		//Formatter::off
		ForgeRegistries.ENTITY_TYPES.register("gateway", EntityType.Builder
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
		//Formatter::on
	}

	public void registerItems() {
		ForgeRegistries.ITEMS.register("gate_pearl", new GatePearlItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON).tab(TAB)));
		registerStat(GatewayObjects.Stats.STAT_GATES_DEFEATED, StatFormatter.DEFAULT);
	}

	public void registerSerializers() {
		ForgeRegistries.RECIPE_SERIALIZERS.register("gate_recipe", GatewayRecipeSerializer.INSTANCE);
	}

	public void registerSounds() {
		for (String s : new String[] { "gate_warp", "gate_ambient", "gate_start", "gate_end" }) {
			ForgeRegistries.SOUND_EVENTS.register(s, new SoundEvent(Gateways.loc(s)));
		}
	}

	public static ResourceLocation loc(String s) {
		return new ResourceLocation(MODID, s);
	}

	public void registerParticles() {
		ForgeRegistries.PARTICLE_TYPES.register("glow", new ParticleType<GatewayParticleData>(false, GatewayParticleData.DESERIALIZER) {
			@Override
			public Codec<GatewayParticleData> codec() {
				return GatewayParticleData.CODEC;
			}
		});
	}

	public void commands(RegisterCommandsEvent e) {
		GatewayCommand.register(e.getDispatcher());
	}

	private static void registerStat(ResourceLocation id, StatFormatter pFormatter) {
		Registry.register(Registry.CUSTOM_STAT, id, id);
		Stats.CUSTOM.get(id, pFormatter);
	}

	public void teleport(EntityTeleportEvent e) {
		Entity entity = e.getEntity();
		if (entity.getPersistentData().contains("gateways.owner")) {
			UUID id = entity.getPersistentData().getUUID("gateways.owner");
			if (entity.level instanceof ServerLevel sl && sl.getEntity(id) instanceof GatewayEntity gate) {
				if (gate.distanceToSqr(e.getTargetX(), e.getTargetY(), e.getTargetZ()) >= gate.getGateway().getLeashRangeSq()) {
					e.setTargetX(gate.getX() + 0.5 * gate.getBbWidth());
					e.setTargetY(gate.getY() + 0.5 * gate.getBbHeight());
					e.setTargetZ(gate.getZ() + 0.5 * gate.getBbWidth());
				}
			}
		}
	}

	public void convert(LivingConversionEvent.Post e) {
		Entity entity = e.getEntity();
		if (entity.getPersistentData().contains("gateways.owner")) {
			UUID id = entity.getPersistentData().getUUID("gateways.owner");
			if (entity.level instanceof ServerLevel sl && sl.getEntity(id) instanceof GatewayEntity gate) {
				gate.handleConversion(entity, e.getOutcome());
			}
		}
	}

}
