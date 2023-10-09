package dev.shadowsoffire.gateways.client;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.shadowsoffire.gateways.GatewayObjects;
import dev.shadowsoffire.gateways.Gateways;
import dev.shadowsoffire.gateways.entity.GatewayEntity;
import dev.shadowsoffire.gateways.gate.Gateway;
import dev.shadowsoffire.gateways.item.GatePearlItem;
import dev.shadowsoffire.placebo.PlaceboClient;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(bus = Bus.MOD, value = Dist.CLIENT, modid = Gateways.MODID)
public class GatewaysClient {

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent e) {
        e.enqueueWork(() -> {
            ItemProperties.register(GatewayObjects.GATE_PEARL.get(), new ResourceLocation(Gateways.MODID, "size"), (stack, level, entity, seed) -> {
                DynamicHolder<Gateway> gate = GatePearlItem.getGate(stack);
                if (gate.isBound()) return gate.get().size().ordinal();
                return 2;
            });
        });
        MinecraftForge.EVENT_BUS.addListener(GatewaysClient::bossRenderPre);
        MinecraftForge.EVENT_BUS.addListener(GatewaysClient::tooltip);
        MinecraftForge.EVENT_BUS.addListener(GatewaysClient::scroll);
        MinecraftForge.EVENT_BUS.addListener(GatewaysClient::scroll2);
    }

    @SubscribeEvent
    public static void colors(RegisterColorHandlersEvent.Item e) {
        e.register((stack, tint) -> {
            DynamicHolder<Gateway> gate = GatePearlItem.getGate(stack);
            if (gate.isBound()) return gate.get().color().getValue();
            return 0xAAAAFF;
        }, GatewayObjects.GATE_PEARL.get());
    }

    @SubscribeEvent
    public static void eRenders(RegisterRenderers e) {
        e.registerEntityRenderer(GatewayObjects.NORMAL_GATEWAY.get(), GatewayRenderer::new);
        e.registerEntityRenderer(GatewayObjects.ENDLESS_GATEWAY.get(), GatewayRenderer::new);
    }

    @SubscribeEvent
    public static void factories(RegisterParticleProvidersEvent e) {
        e.registerSprite(GatewayObjects.GLOW.get(), GatewayParticle::new);
    }

    static int scrollIdx = 0;
    private static ItemStack currentTooltipItem = ItemStack.EMPTY;
    private static long tooltipTick = 0;

    public static void scroll(ScreenEvent.MouseScrolled.Pre e) {
        if (currentTooltipItem.getItem() == GatewayObjects.GATE_PEARL.get() && tooltipTick == PlaceboClient.ticks && Screen.hasShiftDown()) {
            scrollIdx += e.getScrollDelta() < 0 ? 1 : -1;
            e.setCanceled(true);
        }
    }

    public static void scroll2(InputEvent.MouseScrollingEvent e) {
        if (currentTooltipItem.getItem() == GatewayObjects.GATE_PEARL.get() && tooltipTick == PlaceboClient.ticks && Screen.hasShiftDown()) {
            scrollIdx += e.getScrollDelta() < 0 ? 1 : -1;
            e.setCanceled(true);
        }
    }

    static RandomSource rand = RandomSource.create();

    public static void tooltip(ItemTooltipEvent e) {
        currentTooltipItem = e.getItemStack();
        tooltipTick = PlaceboClient.ticks;
    }

    public static final ResourceLocation BARS = new ResourceLocation("textures/gui/bars.png");

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
    }

    /**
     * When rendering the string in-world, the rotation causes the drop shadow to be rendered behind the original text.
     */
    public static void drawReversedDropShadow(GuiGraphics gfx, Font font, Component comp, int x, int y) {
        gfx.drawString(font, comp, x, y, 0, false);
        PoseStack pose = gfx.pose();
        pose.pushPose();
        pose.translate(1, 1, 0.03);
        int color = comp.getStyle().getColor().getValue();
        int r = ((color >> 16) & 0xFF) / 4;
        int g = ((color >> 8) & 0xFF) / 4;
        int b = ((color) & 0xFF) / 4;
        color = 0xFF << 24 | r << 16 | g << 8 | b;
        gfx.drawString(font, comp.getString(), x, y, color, false);
        pose.popPose();
    }

}
