package dev.shadowsoffire.gateways.client;

import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import dev.shadowsoffire.attributeslib.api.AttributeHelper;
import dev.shadowsoffire.gateways.entity.GatewayEntity;
import dev.shadowsoffire.gateways.entity.NormalGatewayEntity;
import dev.shadowsoffire.gateways.gate.Failure;
import dev.shadowsoffire.gateways.gate.Reward;
import dev.shadowsoffire.gateways.gate.Wave;
import dev.shadowsoffire.gateways.gate.WaveEntity;
import dev.shadowsoffire.gateways.gate.WaveModifier;
import dev.shadowsoffire.gateways.gate.normal.NormalGateway;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringUtil;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * Client code specific to {@link NormalGateway}
 */
public class NormalGateClient {

    public static final ResourceLocation BARS = GatewaysClient.BARS;

    public static void appendPearlTooltip(NormalGateway gate, Level level, List<Component> tooltips, TooltipFlag flag) {
        MutableComponent comp;

        int waveIdx = Math.floorMod(GatewaysClient.scrollIdx, gate.getNumWaves());
        Wave wave = gate.getWave(waveIdx);

        if (Screen.hasShiftDown()) {
            comp = Component.translatable("tooltip.gateways.wave", waveIdx + 1, gate.getNumWaves()).withStyle(ChatFormatting.GRAY);
            comp.append(CommonComponents.SPACE);
            comp.append(Component.translatable("tooltip.gateways.scroll").withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY).withUnderlined(false)));
            tooltips.add(comp);
            // tooltips.add(Component.nullToEmpty(null));
            comp = AttributeHelper.list().append(Component.translatable("tooltip.gateways.entities").withStyle(Style.EMPTY.withColor(0x87CEEB)));
            tooltips.add(comp);
            for (WaveEntity entity : wave.entities()) {
                comp = AttributeHelper.list().append(Component.translatable("tooltip.gateways.dot", entity.getDescription()).withStyle(Style.EMPTY.withColor(0x87CEEB)));
                tooltips.add(comp);
            }

            if (!wave.modifiers().isEmpty()) {
                comp = AttributeHelper.list().append(Component.translatable("tooltip.gateways.modifiers").withStyle(ChatFormatting.RED));
                tooltips.add(comp);
                for (WaveModifier modif : wave.modifiers()) {
                    modif.appendHoverText(c -> {
                        tooltips.add(AttributeHelper.list().append(Component.translatable("tooltip.gateways.dot", c.withStyle(ChatFormatting.RED)).withStyle(s -> s.withColor(ChatFormatting.RED))));
                    });
                }
            }

            comp = AttributeHelper.list().append(Component.translatable("tooltip.gateways.rewards").withStyle(s -> s.withColor(ChatFormatting.GOLD)));
            tooltips.add(comp);
            for (Reward r : wave.rewards()) {
                r.appendHoverText(c -> {
                    tooltips.add(AttributeHelper.list().append(Component.translatable("tooltip.gateways.dot", c).withStyle(s -> s.withColor(ChatFormatting.GOLD))));
                });
            }
        }
        else {
            comp = Component.translatable("tooltip.gateways.num_wave" + (gate.getNumWaves() == 1 ? "" : "s"), gate.getNumWaves()).withStyle(ChatFormatting.GRAY);
            comp.append(CommonComponents.SPACE);
            comp.append(Component.translatable("tooltip.gateways.shift").withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY)));
            tooltips.add(comp);
        }

        List<Failure> failures = gate.failures();
        if (!failures.isEmpty()) {
            if (Screen.hasControlDown()) {
                comp = Component.translatable("tooltip.gateways.failures").withStyle(Style.EMPTY.withColor(ChatFormatting.RED));
                tooltips.add(comp);
                for (Failure f : failures) {
                    f.appendHoverText(c -> {
                        tooltips.add(AttributeHelper.list().append(c.withStyle(Style.EMPTY.withColor(ChatFormatting.RED))));
                    });
                }
            }
            else {
                comp = Component.translatable("tooltip.gateways.num_failure" + (failures.size() == 1 ? "" : "s"), failures.size()).withStyle(Style.EMPTY.withColor(ChatFormatting.RED));
                comp.append(CommonComponents.SPACE);
                comp.append(Component.translatable("tooltip.gateways.ctrl").withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY)));
                tooltips.add(comp);
            }
        }

        List<MutableComponent> deviations = gate.rules().buildDeviations();
        if (!deviations.isEmpty()) {
            if (Screen.hasAltDown()) {
                comp = Component.translatable("tooltip.gateways.rules", deviations.size()).withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GREEN));
                tooltips.add(comp);
                deviations.forEach(c -> {
                    tooltips.add(AttributeHelper.list().append(c.withStyle(ChatFormatting.DARK_GREEN)));
                });
            }
            else {
                comp = Component.translatable("tooltip.gateways.num_rule" + (deviations.size() == 1 ? "" : "s"), deviations.size()).withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GREEN));
                comp.append(CommonComponents.SPACE);
                comp.append(Component.translatable("tooltip.gateways.alt").withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY)));
                tooltips.add(comp);
            }
        }

        List<Reward> rewards = gate.rewards();
        if (!rewards.isEmpty()) {
            comp = Component.translatable("tooltip.gateways.key_rewards").withStyle(Style.EMPTY.withColor(0x33AA20));
            tooltips.add(comp);
            for (Reward r : rewards) {
                r.appendHoverText(c -> {
                    tooltips.add(AttributeHelper.list().append(c.withStyle(Style.EMPTY.withColor(0x33AA20))));
                });
            }
        }
    }

    public static void renderBossBar(GatewayEntity gateEntity, Object guiGfx, int x, int y, boolean isInWorld) {
        NormalGatewayEntity gate = (NormalGatewayEntity) gateEntity;
        GuiGraphics gfx = (GuiGraphics) guiGfx;
        PoseStack pose = gfx.pose();
        int color = gate.getGateway().color().getValue();
        int r = color >> 16 & 255, g = color >> 8 & 255, b = color & 255;
        RenderSystem.setShaderColor(r / 255F, g / 255F, b / 255F, 1.0F);

        int wave = gate.getWave() + 1;
        int maxWave = gate.getGateway().getNumWaves();
        int enemies = gate.getActiveEnemies();
        int maxEnemies = gate.getCurrentWave().entities().stream().mapToInt(WaveEntity::getCount).sum();
        int y2 = y + 10 + Minecraft.getInstance().font.lineHeight;

        pose.pushPose();
        pose.translate(0, 0, 0.01);
        gfx.blit(BARS, x, y, 0, 6 * 5 * 2, 182, 5, 256, 256);
        gfx.blit(BARS, x, y2, 0, 6 * 5 * 2, 182, 5, 256, 256);
        pose.popPose();

        float waveProgress = 1F / maxWave;
        float progress = waveProgress * (maxWave - wave + 1);
        if (gate.isWaveActive()) progress -= waveProgress * ((float) (maxEnemies - enemies) / maxEnemies);

        int i = (int) (progress * 183.0F);
        if (i > 0) gfx.blit(BARS, x, y, 0, 6 * 5 * 2 + 5, i, 5, 256, 256);

        float maxTime = gate.getMaxWaveTime();
        if (gate.isWaveActive()) {
            i = (int) ((maxTime - gate.getTicksActive()) / maxTime * 183.0F);
            if (i > 0) gfx.blit(BARS, x, y2, 0, 6 * 5 * 2 + 5, i, 5, 256, 256);
        }
        else {
            maxTime = gate.getSetupTime();
            i = (int) (gate.getTicksActive() / maxTime * 183.0F);
            if (i > 0) gfx.blit(BARS, x, y2, 0, 6 * 5 * 2 + 5, i, 5, 256, 256);
        }

        RenderSystem.setShaderColor(1, 1, 1, 1);
        Font font = Minecraft.getInstance().font;

        Component component = Component.literal(gate.getCustomName().getString()).withStyle(ChatFormatting.GOLD);
        int strWidth = font.width(component);
        int textX = x + 182 / 2 - strWidth / 2;
        int textY = y - 9;
        if (isInWorld) {
            GatewaysClient.drawReversedDropShadow(gfx, font, component, textX, textY);
        }
        else {
            gfx.drawString(font, component, textX, textY, 16777215, true);
        }
        textY = y2 - 9;

        int time = (int) maxTime - gate.getTicksActive();
        String str = I18n.get("boss.gateways.wave", wave, maxWave, StringUtil.formatTickDuration(time), enemies);
        if (!gate.isWaveActive()) {
            if (gate.isLastWave()) {
                str = I18n.get("boss.gateways.done");
            }
            else str = I18n.get("boss.gateways.starting", wave, StringUtil.formatTickDuration(time));
        }
        component = Component.literal(str).withStyle(ChatFormatting.GREEN);
        strWidth = font.width(component);
        textX = x + 182 / 2 - strWidth / 2;
        if (isInWorld) {
            GatewaysClient.drawReversedDropShadow(gfx, font, component, textX, textY);
        }
        else {
            gfx.drawString(font, component, textX, textY, 16777215, true);
        }
    }

}
