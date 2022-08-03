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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import shadows.gateways.GatewaysToEternity;
import shadows.gateways.entity.GatewayEntity;

public class GatewayRenderer extends EntityRenderer<GatewayEntity> {

	public static final ResourceLocation TEXTURE = new ResourceLocation(GatewaysToEternity.MODID, "textures/entity/gateway.png");
	public static final int[] FRAMES = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1 };

	public GatewayRenderer(EntityRendererProvider.Context mgr) {
		super(mgr);
	}

	@Override
	public ResourceLocation getTextureLocation(GatewayEntity entity) {
		return TEXTURE;
	}

	@Override
	public void render(GatewayEntity entity, float unknown, float partialTicks, PoseStack matrix, MultiBufferSource buf, int packedLight) {
		matrix.pushPose();
		Player player = Minecraft.getInstance().player;
		Vec3 playerV = player.getEyePosition(partialTicks);
		Vec3 portal = entity.position();

		float scale = 0.35F;
		double yOffset = 1.5;

		matrix.translate(0, yOffset, 0);
		matrix.mulPose(new Quaternion(new Vector3f(0, 1, 0), 90, true));
		matrix.mulPose(new Quaternion(new Vector3f(0, 1, 0), 180F - (float) angleOf(portal, playerV), true));

		float progress = ((entity.tickCount + partialTicks) % 90) / 90F;
		scale += (float) Math.cos(2 * Math.PI * progress) / 6F;

		if (!entity.isWaveActive()) {
			if (entity.getClientTicks() == -1) entity.setClientTicks(entity.tickCount);
		}

		if (entity.getClientTicks() != -1) {
			progress = (entity.tickCount - entity.getClientTicks() + partialTicks) / entity.getCurrentWave().setupTime();
			if (progress >= 1.3F) entity.setClientTicks(-1);
			else {
				if (progress <= 0.45F) {
					float sin = (float) -Math.sin(Math.PI * progress / 0.45F) / 5F;
					scale *= (1 + sin);
				} else {
					float sin = (float) Math.sin(Math.PI * (progress - 0.35F) * (progress - 0.35F));
					float sinSq = sin * sin;
					scale *= (1 + sinSq);
				}
			}
		}

		matrix.scale(scale, scale, 1);

		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.setShaderTexture(0, this.getTextureLocation(entity));
		VertexConsumer builder = buf.getBuffer(RenderType.entityCutout(getTextureLocation(entity)));
		int color = entity.getGateway().getColor().getValue();
		int r = color >> 16 & 255, g = color >> 8 & 255, b = color & 255;
		float frameHeight = 1 / 12F;
		int frame = FRAMES[entity.tickCount % FRAMES.length];
		builder.vertex(matrix.last().pose(), -1, -1, 0).color(r, g, b, 255).uv(1, 1 - frame * frameHeight).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(matrix.last().normal(), 0, 1, 0).endVertex();
		builder.vertex(matrix.last().pose(), -1, 1, 0).color(r, g, b, 255).uv(1, 11F / 12 - frame * frameHeight).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(matrix.last().normal(), 0, 1, 0).endVertex();
		builder.vertex(matrix.last().pose(), 1, 1, 0).color(r, g, b, 255).uv(0, 11F / 12 - frame * frameHeight).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(matrix.last().normal(), 0, 1, 0).endVertex();
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
