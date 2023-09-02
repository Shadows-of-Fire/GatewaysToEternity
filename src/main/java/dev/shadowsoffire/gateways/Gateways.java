package dev.shadowsoffire.gateways;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dev.shadowsoffire.gateways.gate.Failure;
import dev.shadowsoffire.gateways.gate.GatewayRegistry;
import dev.shadowsoffire.gateways.gate.Reward;
import dev.shadowsoffire.gateways.gate.WaveEntity;
import dev.shadowsoffire.gateways.net.ParticleMessage;
import dev.shadowsoffire.placebo.network.MessageHelper;
import dev.shadowsoffire.placebo.tabs.TabFillingRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.StatFormatter;
import net.minecraft.stats.Stats;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

@Mod(Gateways.MODID)
public class Gateways {

    public static final String MODID = "gateways";
    public static final Logger LOGGER = LogManager.getLogger("Gateways to Eternity");

    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
        .named(new ResourceLocation(MODID, "channel"))
        .clientAcceptedVersions(s -> true)
        .serverAcceptedVersions(s -> true)
        .networkProtocolVersion(() -> "1.0.0")
        .simpleChannel();

    public Gateways() {
        FMLJavaModLoadingContext.get().getModEventBus().register(this);
        MessageHelper.registerMessage(CHANNEL, 0, new ParticleMessage.Provider());
        MinecraftForge.EVENT_BUS.register(new GatewayEvents());
        GatewayObjects.bootstrap();
    }

    @SubscribeEvent
    public void setup(FMLCommonSetupEvent e) {
        GatewayRegistry.INSTANCE.registerToBus();
        e.enqueueWork(() -> {
            Reward.initSerializers();
            WaveEntity.initSerializers();
            Failure.initSerializers();
            TabFillingRegistry.register(GatewayObjects.TAB_KEY, GatewayObjects.GATE_PEARL);
            Stats.CUSTOM.get(GatewayObjects.GATES_DEFEATED.get(), StatFormatter.DEFAULT);
        });
    }

    public static ResourceLocation loc(String s) {
        return new ResourceLocation(MODID, s);
    }

}
