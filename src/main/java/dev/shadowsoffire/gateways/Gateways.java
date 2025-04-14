package dev.shadowsoffire.gateways;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dev.shadowsoffire.gateways.gate.Failure;
import dev.shadowsoffire.gateways.gate.GatewayRegistry;
import dev.shadowsoffire.gateways.gate.Reward;
import dev.shadowsoffire.gateways.gate.WaveEntity;
import dev.shadowsoffire.gateways.gate.WaveModifier;
import dev.shadowsoffire.gateways.gate.endless.ApplicationMode;
import dev.shadowsoffire.gateways.payloads.ParticlePayload;
import dev.shadowsoffire.placebo.network.PayloadHelper;
import dev.shadowsoffire.placebo.tabs.TabFillingRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.StatFormatter;
import net.minecraft.stats.Stats;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(Gateways.MODID)
public class Gateways {

    public static final String MODID = "gateways";
    public static final Logger LOGGER = LogManager.getLogger("Gateways to Eternity");

    public Gateways(IEventBus bus) {
        bus.register(this);
        PayloadHelper.registerPayload(new ParticlePayload.Provider());
        NeoForge.EVENT_BUS.register(new GatewayEvents());
        GatewayObjects.bootstrap(bus);
    }

    @SubscribeEvent
    public void setup(FMLCommonSetupEvent e) {
        GatewayRegistry.INSTANCE.registerToBus();
        e.enqueueWork(() -> {
            WaveModifier.initSerializers();
            Reward.initSerializers();
            WaveEntity.initSerializers();
            Failure.initSerializers();
            ApplicationMode.initSerializers();
            TabFillingRegistry.register(GatewayObjects.TAB.getKey(), GatewayObjects.GATE_PEARL);
            Stats.CUSTOM.get(GatewayObjects.GATES_DEFEATED, StatFormatter.DEFAULT);
        });
    }

    public static ResourceLocation loc(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    /**
     * Constructs a mutable component with a lang key of the form "type.modid.path", using {@link Gateways#MODID}.
     *
     * @param type The type of language key, "misc", "info", "title", etc...
     * @param path The path of the language key.
     * @param args Translation arguments passed to the created translatable component.
     */
    public static MutableComponent lang(String type, String path, Object... args) {
        return Component.translatable(langKey(type, path), args);
    }

    public static String langKey(String type, String path) {
        return type + "." + MODID + "." + path;
    }

}
