package shadows.gateways.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import shadows.gateways.Gateways;
import shadows.gateways.entity.GatewayEntity;

public class GatewayRenderer extends EntityRenderer<GatewayEntity> {

	public static final ResourceLocation TEXTURE = new ResourceLocation(Gateways.MODID, "textures/entity/gateway.png");

	public GatewayRenderer(EntityRendererProvider.Context mgr) {
		super(mgr);
	}

	@Override
	public ResourceLocation getTextureLocation(GatewayEntity entity) {
		return TEXTURE;
	}

	@Override
	public void render(GatewayEntity gate, float yaw, float partialTicks, PoseStack matrix, MultiBufferSource buf, int packedLight) {
		matrix.pushPose();
		Player player = Minecraft.getInstance().player;
		Vec3 playerV = player.getEyePosition(partialTicks);
		Vec3 portal = gate.position();

		float baseScale = gate.getGateway().getSize().getScale();
		float scale = baseScale;
		double yOffset = gate.getBbHeight() / 2;

		// Rotate to face the player
		matrix.translate(0, yOffset, 0);
		matrix.mulPose(new Quaternion(new Vector3f(0, 1, 0), 90, true));
		matrix.mulPose(new Quaternion(new Vector3f(0, 1, 0), 180F - (float) angleOf(portal, playerV), true));
		matrix.scale(2, 1, 1);

		if (!gate.isWaveActive() && !gate.isLastWave()) {
			float time = gate.getTicksActive() + partialTicks;
			float maxTime = gate.getCurrentWave().setupTime();
			if (time <= maxTime) scale = Mth.lerp(time / maxTime, gate.getClientScale(), baseScale);
		} else {
			float time = gate.getTicksActive() + partialTicks;
			int magic = 10;
			if (time < magic) {
				matrix.scale(Mth.lerp(time / magic, 1, 1.33F), 1, 1);
				matrix.scale(1, Mth.lerp(time / magic, 1, 1.33F), 1);
			} else if (time < 2 * magic) {
				time -= magic;
				matrix.scale(Mth.lerp(time / magic, 1.33F, 1F), 1, 1);
				matrix.scale(1, Mth.lerp(time / magic, 1.33F, 1F), 1);
			} else {
				float progress = ((gate.getTicksActive() + partialTicks - 20) % 80) / 80F;
				scale += (float) Math.sin(2 * Math.PI * progress) * baseScale / 6F;
				gate.setClientScale(scale);
			}
		}

		matrix.scale(scale, scale, 1);

		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.setShaderTexture(0, this.getTextureLocation(gate));
		VertexConsumer builder = buf.getBuffer(RenderType.entityCutout(getTextureLocation(gate)));
		int color = gate.getGateway().getColor().getValue();
		int r = color >> 16 & 255, g = color >> 8 & 255, b = color & 255;
		float frameHeight = 1 / 9F;
		int frame = gate.tickCount % 9;
		builder.vertex(matrix.last().pose(), -1, -1, 0).color(r, g, b, 255).uv(1, 1 - frame * frameHeight).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(matrix.last().normal(), 0, 1, 0).endVertex();
		builder.vertex(matrix.last().pose(), -1, 1, 0).color(r, g, b, 255).uv(1, 8F / 9 - frame * frameHeight).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(matrix.last().normal(), 0, 1, 0).endVertex();
		builder.vertex(matrix.last().pose(), 1, 1, 0).color(r, g, b, 255).uv(0, 8F / 9 - frame * frameHeight).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(matrix.last().normal(), 0, 1, 0).endVertex();
		builder.vertex(matrix.last().pose(), 1, -1, 0).color(r, g, b, 255).uv(0, 1 - frame * frameHeight).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(matrix.last().normal(), 0, 1, 0).endVertex();

		matrix.popPose();
	}

	public static double angleOf(Vec3 p1, Vec3 p2) {
		final double deltaY = p2.z - p1.z;
		final double deltaX = p2.x - p1.x;
		final double result = Math.toDegrees(Math.atan2(deltaY, deltaX));
		return result < 0 ? 360d + result : result;
	}

}
