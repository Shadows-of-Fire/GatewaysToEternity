package shadows.gateways.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.Quaternion;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Vector3f;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import shadows.gateways.GatewaysToEternity;
import shadows.gateways.entity.AbstractGatewayEntity;
import shadows.gateways.util.BossColorMap;

public class GatewayRenderer extends EntityRenderer<AbstractGatewayEntity> {

	public static final ResourceLocation TEXTURE = new ResourceLocation(GatewaysToEternity.MODID, "textures/entity/temp_gray.png");

	public GatewayRenderer(EntityRendererManager mgr) {
		super(mgr);
	}

	@Override
	public ResourceLocation getEntityTexture(AbstractGatewayEntity entity) {
		return TEXTURE;
	}

	@Override
	public void render(AbstractGatewayEntity entity, float unknown, float partialTicks, MatrixStack matrix, IRenderTypeBuffer buf, int packedLight) {
		matrix.push();
		PlayerEntity player = Minecraft.getInstance().player;
		Vec3d playerV = player.getEyePosition(partialTicks);
		Vec3d portal = entity.getPositionVec();

		float scale = 0.35F;
		double yOffset = 1.5;

		matrix.translate(0, yOffset, 0);
		matrix.multiply(new Quaternion(new Vector3f(0, 1, 0), 90, true));
		matrix.multiply(new Quaternion(new Vector3f(0, 1, 0), 180F - (float) angleOf(portal, playerV), true));

		float progress = ((entity.ticksExisted + partialTicks) % 90) / 90F;
		scale += (float) Math.cos(2 * Math.PI * progress) / 6F;

		if (!entity.isWaveActive()) {
			if (entity.getClientTicks() == -1) entity.setClientTicks(entity.ticksExisted);
		}

		if (entity.getClientTicks() != -1) {
			progress = (entity.ticksExisted - entity.getClientTicks() + partialTicks) / entity.getStats().pauseTime;
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

		this.renderManager.textureManager.bindTexture(this.getEntityTexture(entity));
		IVertexBuilder builder = buf.getBuffer(RenderType.getEntityTranslucent(getEntityTexture(entity)));
		int color = BossColorMap.getColor(entity.getBossInfo());
		int r = color >> 16 & 255, g = color >> 8 & 255, b = color & 255;
		builder.vertex(matrix.peek().getModel(), -1, -1, 0).color(r, g, b, 255).texture(1, 1).overlay(OverlayTexture.DEFAULT_UV).light(packedLight).normal(matrix.peek().getNormal(), 0, 1, 0).endVertex();
		builder.vertex(matrix.peek().getModel(), -1, 1, 0).color(r, g, b, 255).texture(1, 0).overlay(OverlayTexture.DEFAULT_UV).light(packedLight).normal(matrix.peek().getNormal(), 0, 1, 0).endVertex();
		builder.vertex(matrix.peek().getModel(), 1, 1, 0).color(r, g, b, 255).texture(0, 0).overlay(OverlayTexture.DEFAULT_UV).light(packedLight).normal(matrix.peek().getNormal(), 0, 1, 0).endVertex();
		builder.vertex(matrix.peek().getModel(), 1, -1, 0).color(r, g, b, 255).texture(0, 1).overlay(OverlayTexture.DEFAULT_UV).light(packedLight).normal(matrix.peek().getNormal(), 0, 1, 0).endVertex();

		matrix.pop();
	}

	public static double angleOf(Vec3d p1, Vec3d p2) {
		final double deltaY = p2.z - p1.z;
		final double deltaX = p2.x - p1.x;
		final double result = Math.toDegrees(Math.atan2(deltaY, deltaX));
		return result < 0 ? 360d + result : result;
	}

}
