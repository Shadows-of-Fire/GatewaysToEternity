package shadows.gateways.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.item.ItemModelsProperties;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StringUtils;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.BossInfo;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ParticleFactoryRegisterEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import shadows.gateways.GatewayObjects;
import shadows.gateways.Gateways;
import shadows.gateways.entity.GatewayEntity;
import shadows.gateways.gate.Gateway;
import shadows.gateways.gate.Reward;
import shadows.gateways.gate.WaveEntity;
import shadows.gateways.item.GatePearlItem;
import shadows.gateways.misc.RandomAttributeModifier;

@EventBusSubscriber(bus = Bus.MOD, value = Dist.CLIENT, modid = Gateways.MODID)
public class GatewaysClient {

	@SubscribeEvent
	public static void setup(FMLClientSetupEvent e) {
		e.enqueueWork(() -> {
			Minecraft.getInstance().getItemColors().register((stack, tint) -> {
				Gateway gate = GatePearlItem.getGate(stack);
				if (gate != null) return gate.getColor().getValue();
				return 0xAAAAFF;
			}, GatewayObjects.GATE_PEARL);

			ItemModelsProperties.register(GatewayObjects.GATE_PEARL, new ResourceLocation(Gateways.MODID, "size"), (stack, level, entity) -> {
				Gateway gate = GatePearlItem.getGate(stack);
				if (gate == null) return 2;
				return gate.getSize().ordinal();
			});
			Minecraft.getInstance().getEntityRenderDispatcher().register(GatewayObjects.GATEWAY, new GatewayRenderer(Minecraft.getInstance().getEntityRenderDispatcher()));
		});
		MinecraftForge.EVENT_BUS.addListener(GatewaysClient::bossRenderPre);
		MinecraftForge.EVENT_BUS.addListener(GatewaysClient::tooltip);
	}

	@SubscribeEvent
	public static void factories(ParticleFactoryRegisterEvent e) {
		Minecraft.getInstance().particleEngine.register(GatewayObjects.GLOW, GatewayParticle.Factory::new);
	}

	@SubscribeEvent
	@SuppressWarnings("deprecation")
	public static void stitch(TextureStitchEvent.Pre e) {
		if (e.getMap().location().equals(AtlasTexture.LOCATION_PARTICLES)) {
			e.addSprite(new ResourceLocation(Gateways.MODID, "particle/glow"));
		}
	}

	public static void tooltip(ItemTooltipEvent e) {
		if (e.getItemStack().getItem() == GatewayObjects.GATE_PEARL) {
			Gateway gate = GatePearlItem.getGate(e.getItemStack());
			List<ITextComponent> tooltips = e.getToolTip();
			if (gate == null) {
				tooltips.add(new StringTextComponent("Errored Gate Pearl, file a bug report detailing how you obtained this."));
				return;
			}

			ITextComponent comp = new TranslationTextComponent("tooltip.gateways.max_waves", gate.getNumWaves()).withStyle(TextFormatting.GRAY);
			tooltips.add(comp);

			if (Screen.hasShiftDown()) {
				int wave = 0;
				if (e.getPlayer() != null) {
					wave = (e.getPlayer().tickCount / 50) % gate.getNumWaves();
				}
				comp = new TranslationTextComponent("tooltip.gateways.wave", wave + 1).withStyle(TextFormatting.GREEN, TextFormatting.UNDERLINE);
				tooltips.add(comp);
				tooltips.add(ITextComponent.nullToEmpty(null));
				comp = new TranslationTextComponent("tooltip.gateways.entities").withStyle(TextFormatting.BLUE);
				tooltips.add(comp);
				Map<String, Integer> counts = new HashMap<>();
				for (WaveEntity entity : gate.getWave(wave).entities()) {
					counts.put(entity.getDescription().getString(), counts.getOrDefault(entity.getDescription().getString(), 0) + 1);
				}
				for (Map.Entry<String, Integer> counted : counts.entrySet()) {
					comp = new TranslationTextComponent("tooltip.gateways.list1", counted.getValue(), new TranslationTextComponent(counted.getKey())).withStyle(TextFormatting.BLUE);
					tooltips.add(comp);
				}
				if (!gate.getWave(wave).modifiers().isEmpty()) {
					comp = new TranslationTextComponent("tooltip.gateways.modifiers").withStyle(TextFormatting.RED);
					tooltips.add(comp);
					for (RandomAttributeModifier inst : gate.getWave(wave).modifiers()) {
						comp = toComponent(inst.getAttribute(), inst.genModifier(e.getPlayer().getRandom()));
						comp = new TranslationTextComponent("tooltip.gateways.list2", comp.getString()).withStyle(TextFormatting.RED);
						tooltips.add(comp);
					}
				}
				comp = new TranslationTextComponent("tooltip.gateways.rewards").withStyle(TextFormatting.GOLD);
				tooltips.add(comp);
				for (Reward r : gate.getWave(wave).rewards()) {
					r.appendHoverText(c -> {
						tooltips.add(new TranslationTextComponent("tooltip.gateways.list2", c).withStyle(TextFormatting.GOLD));
					});
				}
			} else {
				comp = new TranslationTextComponent("tooltip.gateways.shift").withStyle(TextFormatting.GRAY);
				tooltips.add(comp);
			}
			if (Screen.hasControlDown()) {
				comp = new TranslationTextComponent("tooltip.gateways.completion").withStyle(TextFormatting.YELLOW, TextFormatting.UNDERLINE);
				tooltips.add(comp);
				tooltips.add(ITextComponent.nullToEmpty(null));
				comp = new TranslationTextComponent("tooltip.gateways.experience", gate.getCompletionXp()).withStyle(TextFormatting.YELLOW);
				tooltips.add(comp);
				for (Reward r : gate.getRewards()) {
					r.appendHoverText(c -> {
						tooltips.add(new TranslationTextComponent("tooltip.gateways.list3", c).withStyle(TextFormatting.YELLOW));
					});
				}
			} else {
				comp = new TranslationTextComponent("tooltip.gateways.ctrl").withStyle(TextFormatting.GRAY);
				tooltips.add(comp);
			}

		}
	}

	public static final ResourceLocation BARS = new ResourceLocation("textures/gui/bars.png");

	@SuppressWarnings("deprecation")
	public static void bossRenderPre(RenderGameOverlayEvent.BossInfo event) {
		BossInfo boss = event.getBossInfo();
		String name = boss.getName().getString();
		if (name.startsWith("GATEWAY_ID")) {
			World level = Minecraft.getInstance().level;
			event.setCanceled(true);
			Entity entity = level.getEntity(Integer.valueOf(name.substring(10)));
			if (entity instanceof GatewayEntity) {
				GatewayEntity gate = (GatewayEntity) entity;
				int color = gate.getGateway().getColor().getValue();
				int r = color >> 16 & 255, g = color >> 8 & 255, b = color & 255;

				Minecraft.getInstance().getTextureManager().bind(BARS);
				RenderSystem.color4f(r / 255F, g / 255F, b / 255F, 1.0F);
				MatrixStack stack = event.getMatrixStack();

				int wave = gate.getWave() + 1;
				int maxWave = gate.getGateway().getNumWaves();
				int enemies = gate.getActiveEnemies();
				int maxEnemies = gate.getCurrentWave().entities().size();

				int x = event.getX();
				int y = event.getY();
				int y2 = y + event.getIncrement();
				Screen.blit(stack, x, y, 200, 0, 6 * 5 * 2, 182, 5, 256, 256);
				Screen.blit(stack, x, y2, 200, 0, 6 * 5 * 2, 182, 5, 256, 256);

				float waveProgress = 1F / maxWave;
				float progress = waveProgress * (maxWave - wave + 1);
				if (gate.isWaveActive()) progress -= waveProgress * ((float) (maxEnemies - enemies) / maxEnemies);

				int i = (int) (progress * 183.0F);
				if (i > 0) Screen.blit(stack, x, y, 200, 0, 6 * 5 * 2 + 5, i, 5, 256, 256);

				float maxTime = gate.getCurrentWave().maxWaveTime();
				if (gate.isWaveActive()) {
					i = (int) ((maxTime - gate.getTicksActive()) / maxTime * 183.0F);
					if (i > 0) Screen.blit(stack, x, y2, 200, 0, 6 * 5 * 2 + 5, i, 5, 256, 256);
				} else {
					maxTime = gate.getCurrentWave().setupTime();
					i = (int) (gate.getTicksActive() / maxTime * 183.0F);
					if (i > 0) Screen.blit(stack, x, y2, 200, 0, 6 * 5 * 2 + 5, i, 5, 256, 256);
				}
				RenderSystem.color4f(1, 1, 1, 1);
				FontRenderer font = Minecraft.getInstance().font;

				int width = Minecraft.getInstance().getWindow().getGuiScaledWidth();
				ITextComponent component = new StringTextComponent(gate.getCustomName().getString()).withStyle(TextFormatting.GOLD);
				int strWidth = font.width(component);
				int textX = width / 2 - strWidth / 2;
				int textY = y - 9;
				font.drawShadow(stack, component, textX, textY, 16777215);
				event.setIncrement(event.getIncrement() * 2);
				textY = y2 - 9;

				int time = (int) maxTime - gate.getTicksActive();
				String str = I18n.get("boss.gateways.wave", wave, maxWave, StringUtils.formatTickDuration(time), enemies);
				if (!gate.isWaveActive()) {
					if (gate.isLastWave()) {
						str = I18n.get("boss.gateways.done");
					} else str = I18n.get("boss.gateways.starting", wave, StringUtils.formatTickDuration(time));
				}
				component = new StringTextComponent(str).withStyle(TextFormatting.GREEN);
				strWidth = font.width(component);
				textX = width / 2 - strWidth / 2;
				font.drawShadow(stack, component, textX, textY, 16777215);
			}
		}
	}

	/**
	 * Converts an Attribute Modifier to the standard form tooltip component.
	 */
	public static ITextComponent toComponent(Attribute attr, AttributeModifier modif) {
		double amt = modif.getAmount();

		if (modif.getOperation() == Operation.ADDITION) {
			if (attr == Attributes.KNOCKBACK_RESISTANCE) amt *= 10.0D;
		} else {
			amt *= 100.0D;
		}

		int code = modif.getOperation().ordinal();

		if (amt > 0.0D) {
			return new TranslationTextComponent("attribute.modifier.plus." + code, ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(amt), new TranslationTextComponent(attr.getDescriptionId())).withStyle(TextFormatting.BLUE);
		} else {
			amt *= -1.0D;
			return new TranslationTextComponent("attribute.modifier.take." + code, ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(amt), new TranslationTextComponent(attr.getDescriptionId())).withStyle(TextFormatting.RED);
		}
	}

}
