package dev.shadowsoffire.gateways.client;

import javax.annotation.Nullable;

import org.joml.Matrix3x2fStack;

import com.mojang.blaze3d.pipeline.RenderPipeline;

import dev.shadowsoffire.gateways.GatewayObjects;
import dev.shadowsoffire.gateways.Gateways;
import dev.shadowsoffire.gateways.entity.GatewayEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.BossEvent;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RegisterSelectItemModelPropertyEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.common.NeoForge;

@EventBusSubscriber(value = Dist.CLIENT, modid = Gateways.MODID)
public class GatewaysClient {

    public static final Identifier WHITE_PROGRESS = Identifier.withDefaultNamespace("boss_bar/white_progress");
    public static final Identifier WHITE_BACKGROUND = Identifier.withDefaultNamespace("boss_bar/white_background");
    public static final RenderPipeline BLIT_PIPELINE = RenderPipelines.GUI_TEXTURED;

    @Nullable
    public static Rect2i bossBarRect = null;

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent e) {
        NeoForge.EVENT_BUS.addListener(GatewaysClient::bossRenderPre);
        NeoForge.EVENT_BUS.addListener(GatewaysClient::renderPre);
    }

    @SubscribeEvent
    public static void registerTintSources(RegisterColorHandlersEvent.ItemTintSources e) {
        e.register(Gateways.loc("gateway_color"), GatewayColorTintSource.MAP_CODEC);
    }

    @SubscribeEvent
    public static void registerSelectProperties(RegisterSelectItemModelPropertyEvent e) {
        e.register(Gateways.loc("size"), GatewaySizeProperty.TYPE);
    }

    @SubscribeEvent
    public static void eRenders(RegisterRenderers e) {
        e.registerEntityRenderer(GatewayObjects.NORMAL_GATEWAY.get(), GatewayRenderer::new);
        e.registerEntityRenderer(GatewayObjects.ENDLESS_GATEWAY.get(), GatewayRenderer::new);
    }

    @SubscribeEvent
    public static void factories(RegisterParticleProvidersEvent e) {
        e.registerSpriteSet(GatewayObjects.GLOW.get(), GatewayParticle.Provider::new);
    }

    public static void renderPre(RenderFrameEvent.Pre event) {
        bossBarRect = null;
    }

    public static void bossRenderPre(CustomizeGuiOverlayEvent.BossEventProgress event) {
        BossEvent boss = event.getBossEvent();
        String name = boss.getName().getString();
        if (name.startsWith("GATEWAY_ID")) {
            Level level = Minecraft.getInstance().level;
            event.setCanceled(true);
            if (level.getEntity(Integer.valueOf(name.substring(10))) instanceof GatewayEntity gate && gate.isValid()) {
                gate.getGateway().renderBossBar(gate, event.getGuiGraphics(), event.getX(), event.getY(), false);
                event.setIncrement(event.getIncrement() * 2);
            }
        }
        bossBarRect = new Rect2i(event.getX(), 0, 200, event.getY() + event.getIncrement());
    }

    public static void drawReversedDropShadow(GuiGraphicsExtractor gfx, Font font, Component comp, int x, int y) {
        gfx.text(font, comp, x, y, 0xFF000000, false);
        Matrix3x2fStack pose = gfx.pose();
        pose.pushMatrix();
        pose.translate(1, 1);
        int color = comp.getStyle().getColor().getValue();
        int r = ((color >> 16) & 0xFF) / 4;
        int g = ((color >> 8) & 0xFF) / 4;
        int b = ((color) & 0xFF) / 4;
        color = 0xFF << 24 | r << 16 | g << 8 | b;
        gfx.text(font, comp.getString(), x, y, color, false);
        pose.popMatrix();
    }

}
