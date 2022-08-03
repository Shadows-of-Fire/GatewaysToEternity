package shadows.gateways;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringUtil;
import net.minecraft.world.BossEvent;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers;
import net.minecraftforge.client.event.ParticleFactoryRegisterEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import shadows.gateways.client.GatewayParticle;
import shadows.gateways.client.GatewayRenderer;
import shadows.gateways.entity.GatewayEntity;
import shadows.gateways.gate.Gateway;
import shadows.gateways.item.GateOpenerItem;

@EventBusSubscriber(bus = Bus.MOD, value = Dist.CLIENT, modid = GatewaysToEternity.MODID)
public class GatewaysToEternityClient {

	@SubscribeEvent
	public static void setup(FMLClientSetupEvent e) {
		e.enqueueWork(() -> {
			Minecraft.getInstance().getItemColors().register((stack, tint) -> {
				Gateway gate = GateOpenerItem.getGate(stack);
				if (gate != null) return gate.getColor().getValue();
				return 0xFFFFFF;
			}, GatewayObjects.GATE_OPENER);
		});
		MinecraftForge.EVENT_BUS.addListener(GatewaysToEternityClient::bossRenderPre);
	}

	@SubscribeEvent
	public static void eRenders(RegisterRenderers e) {
		e.registerEntityRenderer(GatewayObjects.GATEWAY, GatewayRenderer::new);
	}

	@SubscribeEvent
	public static void factories(ParticleFactoryRegisterEvent e) {
		Minecraft.getInstance().particleEngine.register(GatewayObjects.GLOW, GatewayParticle.Factory::new);
	}

	@SubscribeEvent
	@SuppressWarnings("deprecation")
	public static void stitch(TextureStitchEvent.Pre e) {
		if (e.getAtlas().location().equals(TextureAtlas.LOCATION_PARTICLES)) {
			e.addSprite(new ResourceLocation(GatewaysToEternity.MODID, "particle/glow"));
		}
	}

	public static final ResourceLocation BARS = new ResourceLocation("textures/gui/bars.png");

	public static void bossRenderPre(RenderGameOverlayEvent.BossInfo event) {
		BossEvent boss = event.getBossEvent();
		String name = boss.getName().getString();
		if (name.startsWith("GATEWAY_ID")) {
			Level level = Minecraft.getInstance().level;
			event.setCanceled(true);
			if (level.getEntity(Integer.valueOf(name.substring(10))) instanceof GatewayEntity gate) {
				int color = gate.getGateway().getColor().getValue();
				int r = color >> 16 & 255, g = color >> 8 & 255, b = color & 255;
				RenderSystem.setShaderColor(r / 255F, g / 255F, b / 255F, 1.0F);
				RenderSystem.setShaderTexture(0, BARS);
				PoseStack stack = event.getMatrixStack();

				int wave = gate.getWave() + 1;
				int maxWave = gate.getGateway().getNumWaves();
				int enemies = gate.getActiveEnemies();
				int maxEnemies = gate.getCurrentWave().entities().size();

				int x = event.getX();
				int y = event.getY();
				int y2 = y + event.getIncrement();
				Gui.blit(stack, x, y, 200, 0, 6 * 5 * 2, 182, 5, 256, 256);
				Gui.blit(stack, x, y2, 200, 0, 6 * 5 * 2, 182, 5, 256, 256);

				float waveProgress = 1F / maxWave;
				float progress = waveProgress * (maxWave - wave + 1);
				if (gate.isWaveActive()) progress -= waveProgress * ((float) (maxEnemies - enemies) / maxEnemies);

				int i = (int) (progress * 183.0F);
				if (i > 0) Gui.blit(stack, x, y, 200, 0, 6 * 5 * 2 + 5, i, 5, 256, 256);

				float maxTime = gate.getCurrentWave().maxWaveTime();
				if (gate.isWaveActive()) {
					i = (int) ((maxTime - gate.getTicksActive()) / maxTime * 183.0F);
					if (i > 0) Gui.blit(stack, x, y2, 200, 0, 6 * 5 * 2 + 5, i, 5, 256, 256);
				} else {
					maxTime = gate.getCurrentWave().setupTime();
					i = (int) (gate.getTicksActive() / maxTime * 183.0F);
					if (i > 0) Gui.blit(stack, x, y2, 200, 0, 6 * 5 * 2 + 5, i, 5, 256, 256);
				}
				RenderSystem.setShaderColor(1, 1, 1, 1);
				Font font = Minecraft.getInstance().font;

				int width = Minecraft.getInstance().getWindow().getGuiScaledWidth();
				Component component = new TextComponent(gate.getCustomName().getString()).withStyle(ChatFormatting.GOLD);
				int strWidth = font.width(component);
				int textX = width / 2 - strWidth / 2;
				int textY = y - 9;
				font.drawShadow(stack, component, textX, textY, 16777215);
				event.setIncrement(event.getIncrement() * 2);
				textY = y2 - 9;

				int time = (int) maxTime - gate.getTicksActive();
				String str = String.format("Wave: %d/%d | Time: %s | Enemies: %d", wave, maxWave, StringUtil.formatTickDuration(time), enemies);
				if (!gate.isWaveActive()) {
					str = String.format("Wave %d starting in %s", wave, StringUtil.formatTickDuration(time));
				}
				component = new TextComponent(str).withStyle(ChatFormatting.GREEN);
				strWidth = font.width(component);
				textX = width / 2 - strWidth / 2;
				font.drawShadow(stack, component, textX, textY, 16777215);
			}
		}
	}

}
