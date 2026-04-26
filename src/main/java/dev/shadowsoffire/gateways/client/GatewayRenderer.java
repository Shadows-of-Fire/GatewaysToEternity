package dev.shadowsoffire.gateways.client;

import org.joml.Quaternionf;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.shadowsoffire.gateways.Gateways;
import dev.shadowsoffire.gateways.entity.EndlessGatewayEntity;
import dev.shadowsoffire.gateways.entity.GatewayEntity;
import dev.shadowsoffire.gateways.entity.NormalGatewayEntity;
import dev.shadowsoffire.gateways.gate.WaveEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.data.AtlasIds;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class GatewayRenderer extends EntityRenderer<GatewayEntity, GatewayRenderState> {

    public static final Identifier TEXTURE = Gateways.loc("textures/entity/gateway.png");

    public GatewayRenderer(EntityRendererProvider.Context mgr) {
        super(mgr);
    }

    @Override
    public GatewayRenderState createRenderState() {
        return new GatewayRenderState();
    }

    @Override
    public void extractRenderState(GatewayEntity entity, GatewayRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.valid = entity.isValid();
        state.waveActive = entity.isWaveActive();
        state.completed = entity.isCompleted();
        state.ticksActive = entity.getTicksActive();
        state.tickCount = entity.tickCount;
        state.bbHeight = entity.getBbHeight();
        state.setupTime = entity.getSetupTime();
        state.partialTick = partialTicks;

        if (entity.isValid()) {
            state.baseScale = entity.getGateway().size().getScale();
            // Compute the pulsing scale deterministically from ticksActive so we don't need an entity reference.
            // The sine wave runs from tick 20 onward; before that, scale tracks the opening zoom.
            int ticks = entity.getTicksActive();
            if (entity.isWaveActive() || !entity.isCompleted()) {
                if (ticks >= 20) {
                    float progress = ((ticks + partialTicks - 20) % 80) / 80F;
                    state.clientScale = state.baseScale + (float) Math.sin(2 * Math.PI * progress) * state.baseScale / 6F;
                }
                else {
                    state.clientScale = state.baseScale;
                }
            }
            else {
                state.clientScale = state.baseScale;
            }
            state.color = entity.getGateway().color().getValue();
            state.drawAsName = entity.getGateway().bossSettings().drawAsName();
            state.customName = entity.getCustomName();

            if (state.drawAsName) {
                extractBossBarState(entity, state);
            }
        }
    }

    private void extractBossBarState(GatewayEntity entity, GatewayRenderState state) {
        int wave = entity.getWave() + 1;
        float maxTime = entity.getMaxWaveTime();
        float tps = entity.level().tickRateManager().tickrate();

        if (entity instanceof NormalGatewayEntity normal) {
            state.isEndless = false;
            int maxWave = normal.getGateway().getNumWaves();
            int enemies = normal.getActiveEnemies();
            int maxEnemies = normal.getCurrentWave().entities().stream().mapToInt(WaveEntity::getCount).sum();

            float waveProgress = 1F / maxWave;
            float progress = waveProgress * (maxWave - wave + 1);
            if (entity.isWaveActive()) progress -= waveProgress * ((float) (maxEnemies - enemies) / maxEnemies);
            state.topBarProgress = progress;

            float effectiveMaxTime = maxTime;
            if (entity.isWaveActive()) {
                state.bottomBarProgress = (maxTime - entity.getTicksActive()) / maxTime;
            }
            else {
                effectiveMaxTime = entity.getSetupTime();
                state.bottomBarProgress = entity.getTicksActive() / effectiveMaxTime;
            }

            int displayTime = (int) effectiveMaxTime - entity.getTicksActive();
            String str = I18n.get("boss.gateways.wave", wave, maxWave, StringUtil.formatTickDuration(displayTime, tps), enemies);
            if (!entity.isWaveActive()) {
                if (normal.isLastWave()) str = I18n.get("boss.gateways.done");
                else str = I18n.get("boss.gateways.starting", wave, StringUtil.formatTickDuration(displayTime, tps));
            }
            state.topStatusText = Component.literal(str).withStyle(ChatFormatting.GREEN);
            state.bottomStatusText = null;
        }
        else if (entity instanceof EndlessGatewayEntity endless) {
            state.isEndless = true;
            int enemies = endless.getActiveEnemies();
            int maxEnemies = endless.getMaxEnemies();
            int modifiers = endless.getModifiersApplied();

            float effectiveMaxTime = maxTime;
            if (entity.isWaveActive()) {
                state.topBarProgress = (float) enemies / maxEnemies;
                state.bottomBarProgress = (maxTime - entity.getTicksActive()) / maxTime;
            }
            else {
                effectiveMaxTime = entity.getSetupTime();
                state.topBarProgress = entity.getTicksActive() / effectiveMaxTime;
                state.bottomBarProgress = state.topBarProgress;
            }

            int displayTime = (int) effectiveMaxTime - entity.getTicksActive();
            String str = I18n.get("boss.gateways.endless.top", wave, '\u221E', StringUtil.formatTickDuration(displayTime, tps));
            String str2 = I18n.get("boss.gateways.endless.bot", enemies, maxEnemies, modifiers);
            if (!entity.isWaveActive()) {
                str = I18n.get("boss.gateways.starting", wave, StringUtil.formatTickDuration(displayTime, tps));
                str2 = I18n.get("boss.gateways.endless.incoming", maxEnemies);
            }
            state.topStatusText = Component.literal(str).withStyle(ChatFormatting.GREEN);
            state.bottomStatusText = Component.literal(str2).withStyle(ChatFormatting.GREEN);
        }
    }

    @Override
    public void submit(GatewayRenderState state, PoseStack matrix, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.valid) return;

        float partialTicks = state.partialTick;
        float baseScale = state.baseScale;
        float scale = baseScale;

        Player player = Minecraft.getInstance().player;
        Vec3 playerV = player.getEyePosition(partialTicks);
        Vec3 portal = new Vec3(state.x, state.y, state.z);
        double yOffset = state.bbHeight / 2;

        matrix.pushPose();
        matrix.translate(0, yOffset, 0);
        matrix.mulPose(new Quaternionf().rotationAxis(Mth.DEG_TO_RAD * 90, 0, 1, 0));
        matrix.mulPose(new Quaternionf().rotationAxis(Mth.DEG_TO_RAD * (180F - (float) angleOf(portal, playerV)), 0, 1, 0));
        matrix.scale(2, 1, 1);

        if (!state.waveActive && state.completed) {
            float time = state.ticksActive + partialTicks;
            float maxTime = state.setupTime;
            if (time <= maxTime) scale = Mth.lerp(time / maxTime, state.clientScale, baseScale);
        }
        else {
            float time = state.ticksActive + partialTicks;
            int magic = 10;
            if (time < magic) {
                matrix.scale(Mth.lerp(time / magic, 1, 1.33F), 1, 1);
                matrix.scale(1, Mth.lerp(time / magic, 1, 1.33F), 1);
            }
            else if (time < 2 * magic) {
                time -= magic;
                matrix.scale(Mth.lerp(time / magic, 1.33F, 1F), 1, 1);
                matrix.scale(1, Mth.lerp(time / magic, 1.33F, 1F), 1);
            }
            else {
                float progress = (state.ticksActive + partialTicks - 20) % 80 / 80F;
                scale += (float) Math.sin(2 * Math.PI * progress) * baseScale / 6F;
            }
        }

        matrix.scale(scale, scale, 1);

        int color = state.color;
        int r = color >> 16 & 255, g = color >> 8 & 255, b = color & 255;
        float frameHeight = 1 / 9F;
        int frame = state.tickCount % 9;
        int lightCoords = state.lightCoords;

        collector.submitCustomGeometry(matrix, RenderTypes.entityCutout(TEXTURE), (pose, builder) -> {
            builder.addVertex(pose.pose(), -1, -1, 0).setColor(r, g, b, 255).setUv(1, 1 - frame * frameHeight).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightCoords).setNormal(pose, 0, 1, 0);
            builder.addVertex(pose.pose(), -1, 1, 0).setColor(r, g, b, 255).setUv(1, 8F / 9 - frame * frameHeight).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightCoords).setNormal(pose, 0, 1, 0);
            builder.addVertex(pose.pose(), 1, 1, 0).setColor(r, g, b, 255).setUv(0, 8F / 9 - frame * frameHeight).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightCoords).setNormal(pose, 0, 1, 0);
            builder.addVertex(pose.pose(), 1, -1, 0).setColor(r, g, b, 255).setUv(0, 1 - frame * frameHeight).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightCoords).setNormal(pose, 0, 1, 0);
        });
        matrix.popPose();

        if (state.drawAsName && state.customName != null) {
            submitInWorldBossBar(state, matrix, collector, playerV, portal, lightCoords);
        }

        super.submit(state, matrix, collector, camera);
    }

    private void submitInWorldBossBar(GatewayRenderState state, PoseStack matrix, SubmitNodeCollector collector, Vec3 playerV, Vec3 portal, int lightCoords) {
        Font font = Minecraft.getInstance().font;
        int barColor = 0xFF000000 | state.color;
        float barWidth = 182;
        float barHeight = 5;
        float barX = -barWidth / 2;
        int lineHeight = font.lineHeight;

        TextureAtlas guiAtlas = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.GUI);
        TextureAtlasSprite bgSprite = guiAtlas.getSprite(GatewaysClient.WHITE_BACKGROUND);
        TextureAtlasSprite progressSprite = guiAtlas.getSprite(GatewaysClient.WHITE_PROGRESS);

        matrix.pushPose();
        matrix.translate(0.0F, state.bbHeight + 1, 0.0F);
        matrix.mulPose(new Quaternionf().rotationAxis(Mth.DEG_TO_RAD * 90, 0, 1, 0));
        matrix.mulPose(new Quaternionf().rotationAxis(Mth.DEG_TO_RAD * (180F - (float) angleOf(portal, playerV)), 0, 1, 0));
        matrix.scale(-0.02F, -0.02F, 0.02F);

        Component title = Component.literal(state.customName.getString()).withStyle(
            state.isEndless ? s -> s.withColor(ChatFormatting.GOLD).applyFormat(ChatFormatting.UNDERLINE) : s -> s.withColor(ChatFormatting.GOLD));

        float cursorY;

        if (state.isEndless) {
            cursorY = 0;
            submitReversedShadowText(matrix, collector, font, title, cursorY, lightCoords);

            cursorY += lineHeight + 6;
            if (state.topStatusText != null) {
                submitReversedShadowText(matrix, collector, font, state.topStatusText, cursorY, lightCoords);
            }

            cursorY += lineHeight;
            submitSpriteBar(matrix, collector, bgSprite, progressSprite, barX, cursorY, barWidth, barHeight, barColor, state.topBarProgress, lightCoords);

            cursorY += barHeight + 2;
            if (state.bottomStatusText != null) {
                submitReversedShadowText(matrix, collector, font, state.bottomStatusText, cursorY, lightCoords);
            }

            cursorY += lineHeight;
            submitSpriteBar(matrix, collector, bgSprite, progressSprite, barX, cursorY, barWidth, barHeight, barColor, state.bottomBarProgress, lightCoords);
        }
        else {
            cursorY = 0;
            submitReversedShadowText(matrix, collector, font, title, cursorY, lightCoords);

            cursorY += lineHeight + 1;
            submitSpriteBar(matrix, collector, bgSprite, progressSprite, barX, cursorY, barWidth, barHeight, barColor, state.topBarProgress, lightCoords);

            cursorY += barHeight + 2;
            if (state.topStatusText != null) {
                submitReversedShadowText(matrix, collector, font, state.topStatusText, cursorY, lightCoords);
            }

            cursorY += lineHeight;
            submitSpriteBar(matrix, collector, bgSprite, progressSprite, barX, cursorY, barWidth, barHeight, barColor, state.bottomBarProgress, lightCoords);
        }

        matrix.popPose();
    }

    private static void submitReversedShadowText(PoseStack matrix, SubmitNodeCollector collector, Font font, Component text, float y, int lightCoords) {
        float x = -font.width(text) / 2.0F;
        int textColor = text.getStyle().getColor() != null ? ARGB.opaque(text.getStyle().getColor().getValue()) : 0xFFFFFFFF;
        int shadowR = ((textColor >> 16) & 0xFF) / 4;
        int shadowG = ((textColor >> 8) & 0xFF) / 4;
        int shadowB = (textColor & 0xFF) / 4;
        int shadowColor = 0xFF000000 | shadowR << 16 | shadowG << 8 | shadowB;

        // Shadow behind: render the plain string (no styled FormattedCharSequence) so the color parameter is used directly.
        FormattedCharSequence shadowSeq = FormattedCharSequence.forward(text.getString(), Style.EMPTY);
        matrix.pushPose();
        matrix.translate(1, 1, 0.03F);
        collector.submitText(matrix, x, y, shadowSeq, false, Font.DisplayMode.NORMAL, lightCoords, shadowColor, 0, 0);
        matrix.popPose();

        // Main text in front with original styled formatting.
        collector.submitText(matrix, x, y, text.getVisualOrderText(), false, Font.DisplayMode.NORMAL, lightCoords, textColor, 0, 0);
    }

    private static void submitSpriteBar(PoseStack matrix, SubmitNodeCollector collector, TextureAtlasSprite bgSprite, TextureAtlasSprite fgSprite,
            float x, float y, float width, float height, int tintColor, float progress, int lightCoords) {
        // Background bar (pushed behind via positive Z in this flipped coordinate space)
        matrix.pushPose();
        matrix.translate(0, 0, 0.01F);
        submitSpriteQuad(matrix, collector, bgSprite, x, y, width, height, tintColor, 1.0F, lightCoords);
        matrix.popPose();

        // Progress bar (at z=0, in front of background)
        if (progress > 0) {
            float clampedProgress = Mth.clamp(progress, 0, 1);
            submitSpriteQuad(matrix, collector, fgSprite, x, y, width, height, tintColor, clampedProgress, lightCoords);
        }
    }

    private static void submitSpriteQuad(PoseStack matrix, SubmitNodeCollector collector, TextureAtlasSprite sprite,
            float x, float y, float width, float height, int color, float widthFraction, int lightCoords) {
        float drawWidth = width * widthFraction;
        float u0 = sprite.getU0();
        float u1 = Mth.lerp(widthFraction, sprite.getU0(), sprite.getU1());
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();
        int r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF, a = (color >> 24) & 0xFF;

        RenderType renderType = RenderTypes.entityCutout(sprite.atlasLocation());

        collector.submitCustomGeometry(matrix, renderType, (pose, buffer) -> {
            buffer.addVertex(pose.pose(), x, y, 0).setColor(r, g, b, a).setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightCoords).setNormal(pose, 0, 0, -1);
            buffer.addVertex(pose.pose(), x, y + height, 0).setColor(r, g, b, a).setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightCoords).setNormal(pose, 0, 0, -1);
            buffer.addVertex(pose.pose(), x + drawWidth, y + height, 0).setColor(r, g, b, a).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightCoords).setNormal(pose, 0, 0, -1);
            buffer.addVertex(pose.pose(), x + drawWidth, y, 0).setColor(r, g, b, a).setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightCoords).setNormal(pose, 0, 0, -1);
        });
    }

    public static double angleOf(Vec3 p1, Vec3 p2) {
        final double deltaY = p2.z - p1.z;
        final double deltaX = p2.x - p1.x;
        final double result = Math.toDegrees(Math.atan2(deltaY, deltaX));
        return result < 0 ? 360d + result : result;
    }

}
