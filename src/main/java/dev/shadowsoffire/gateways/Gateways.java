package dev.shadowsoffire.gateways;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dev.shadowsoffire.gateways.data.GatewayProvider;
import dev.shadowsoffire.gateways.gate.Failure;
import dev.shadowsoffire.gateways.gate.GatewayRegistry;
import dev.shadowsoffire.gateways.gate.Reward;
import dev.shadowsoffire.gateways.gate.WaveEntity;
import dev.shadowsoffire.gateways.gate.WaveModifier;
import dev.shadowsoffire.gateways.gate.endless.ApplicationMode;
import dev.shadowsoffire.gateways.payloads.ParticlePayload;
import dev.shadowsoffire.placebo.datagen.DataGenBuilder;
import dev.shadowsoffire.placebo.network.PayloadHelper;
import dev.shadowsoffire.placebo.tabs.TabFillingRegistry;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.data.DataProvider;
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
import net.neoforged.neoforge.data.event.GatherDataEvent;

@Mod(Gateways.MODID)
public class Gateways {

    public static final String MODID = "gateways";
    public static final Logger LOGGER = LogManager.getLogger("Gateways to Eternity");

    public Gateways(IEventBus bus) {
        bus.register(this);
        PayloadHelper.registerPayload(new ParticlePayload.Provider());
        NeoForge.EVENT_BUS.register(new GatewayEvents());
        GatewayObjects.bootstrap(bus);
        WaveModifier.initCodecs();
        Reward.initCodecs();
        WaveEntity.initCodecs();
        Failure.initCodecs();
        ApplicationMode.initCodecs();
    }

    @SubscribeEvent
    public void setup(FMLCommonSetupEvent e) {
        GatewayRegistry.INSTANCE.registerToBus();
        e.enqueueWork(() -> {
            TabFillingRegistry.register(GatewayObjects.TAB.getKey(), GatewayObjects.GATE_PEARL);
            Stats.CUSTOM.get(GatewayObjects.GATES_DEFEATED, StatFormatter.DEFAULT);
        });
    }

    @SubscribeEvent
    public void data(GatherDataEvent e) {
        DataProvider.INDENT_WIDTH.set(4);
        DataGenBuilder.create(MODID)
            .provider(GatewayProvider::new)
            .build(e);

        setupDatagenFieldOrder();
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

    public static void setupDatagenFieldOrder() {
        Object2IntOpenHashMap<String> map = (Object2IntOpenHashMap<String>) DataProvider.FIXED_ORDER_FIELDS;
        // Try to keep the gateway data in a consistent order.

        // Normal Gateway Fields
        map.put("size", 10);
        map.put("color", 20);
        map.put("waves", 30);
        map.put("rewards", 40);
        map.put("failures", 50);
        map.put("spawn_algorithm", 60);
        map.put("rules", 70);
        map.put("boss_event", 80);

        // Endless Gateway Fields
        map.put("base_wave", 30);
        map.put("modifiers", 35);

        // Wave Fields
        map.put("max_wave_time", 5);
        map.put("setup_time", 8);
        map.put("entities", 10);
    }
}
