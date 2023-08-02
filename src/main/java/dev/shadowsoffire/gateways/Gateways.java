package dev.shadowsoffire.gateways;

import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dev.shadowsoffire.gateways.command.GatewayCommand;
import dev.shadowsoffire.gateways.entity.GatewayEntity;
import dev.shadowsoffire.gateways.gate.Failure;
import dev.shadowsoffire.gateways.gate.GatewayManager;
import dev.shadowsoffire.gateways.gate.Reward;
import dev.shadowsoffire.gateways.gate.WaveEntity;
import dev.shadowsoffire.gateways.net.ParticleMessage;
import dev.shadowsoffire.placebo.network.MessageHelper;
import dev.shadowsoffire.placebo.tabs.TabFillingRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.StatFormatter;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.living.LivingConversionEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
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
    // Formatter::off
    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
        .named(new ResourceLocation(MODID, "channel"))
        .clientAcceptedVersions(s -> true)
        .serverAcceptedVersions(s -> true)
        .networkProtocolVersion(() -> "1.0.0")
        .simpleChannel();
    // Formatter::on

    public Gateways() {
        FMLJavaModLoadingContext.get().getModEventBus().register(this);
        MessageHelper.registerMessage(CHANNEL, 0, new ParticleMessage());
        MinecraftForge.EVENT_BUS.addListener(this::commands);
        MinecraftForge.EVENT_BUS.addListener(this::teleport);
        MinecraftForge.EVENT_BUS.addListener(this::convert);
        MinecraftForge.EVENT_BUS.addListener(this::hurt);
        GatewayObjects.bootstrap();
    }

    @SubscribeEvent
    public void setup(FMLCommonSetupEvent e) {
        GatewayManager.INSTANCE.registerToBus();
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

    public void commands(RegisterCommandsEvent e) {
        GatewayCommand.register(e.getDispatcher());
    }

    public void teleport(EntityTeleportEvent e) {
        Entity entity = e.getEntity();
        if (entity.getPersistentData().contains("gateways.owner")) {
            UUID id = entity.getPersistentData().getUUID("gateways.owner");
            if (entity.level() instanceof ServerLevel sl && sl.getEntity(id) instanceof GatewayEntity gate) {
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
            if (entity.level() instanceof ServerLevel sl && sl.getEntity(id) instanceof GatewayEntity gate) {
                gate.handleConversion(entity, e.getOutcome());
            }
        }
    }

    public void hurt(LivingHurtEvent e) {
        Entity entity = e.getEntity();
        if (entity.getPersistentData().contains("gateways.owner")) {
            UUID id = entity.getPersistentData().getUUID("gateways.owner");
            if (entity.level() instanceof ServerLevel sl && sl.getEntity(id) instanceof GatewayEntity gate) {
                boolean isPlayerDamage = e.getSource().getEntity() instanceof Player p && !(p instanceof FakePlayer);
                if (!isPlayerDamage && gate.getGateway().playerDamageOnly()) e.setCanceled(true);
            }
        }
    }

}
