package shadows.gateways.client;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.particle.IAnimatedSprite;
import net.minecraft.client.particle.IParticleFactory;
import net.minecraft.client.particle.IParticleRenderType;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SpriteTexturedParticle;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

@SuppressWarnings("deprecation")
public class GatewayParticle extends SpriteTexturedParticle {

	static final IParticleRenderType RENDER_TYPE = new IParticleRenderType() {
		public void begin(BufferBuilder bufferBuilder, TextureManager textureManager) {
			RenderSystem.enableAlphaTest();
			RenderSystem.depthMask(false);
			RenderSystem.enableBlend();
			GlStateManager._disableCull();
			RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE);
			RenderSystem.alphaFunc(GL11.GL_GREATER, 0.003921569F);
			textureManager.bind(AtlasTexture.LOCATION_PARTICLES);
			bufferBuilder.begin(7, DefaultVertexFormats.PARTICLE);
		}

		public void end(Tessellator tesselator) {
			tesselator.end();
		}

		public String toString() {
			return "PARTICLE_SHEET_TRANSLUCENT";
		}
	};

	public GatewayParticle(GatewayParticleData data, World world, double x, double y, double z, double velX, double velY, double velZ) {
		super((ClientWorld) world, x, y, z, velX, velY, velZ);
		this.rCol = data.red;
		this.gCol = data.green;
		this.bCol = data.blue;
		this.lifetime = 40;
		this.xd = velX;
		this.yd = velY;
		this.zd = velZ;
	}

	@Override
	public IParticleRenderType getRenderType() {
		return RENDER_TYPE;
	}

	public float getQuadSize(float p_217561_1_) {
		return 0.75F * this.quadSize * MathHelper.clamp(((float) this.age + p_217561_1_) / (float) this.lifetime * 32.0F, 0.0F, 1.0F);
	}

	public void tick() {
		this.xo = this.x;
		this.yo = this.y;
		this.zo = this.z;
		this.alpha = 1 - (float) this.age / this.lifetime;
		if (this.age++ >= this.lifetime) {
			this.remove();
		} else {
			this.move(this.xd, this.yd, this.zd);
			if (this.y == this.yo) {
				this.xd *= 1.1D;
				this.zd *= 1.1D;
			}

			this.xd *= (double) 0.86F;
			this.yd *= (double) 0.86F;
			this.zd *= (double) 0.86F;
			if (this.onGround) {
				this.xd *= (double) 0.7F;
				this.zd *= (double) 0.7F;
			}

		}
	}

	public static class Factory implements IParticleFactory<GatewayParticleData> {
		protected final IAnimatedSprite sprites;

		public Factory(IAnimatedSprite sprites) {
			this.sprites = sprites;
		}

		public Particle createParticle(GatewayParticleData data, ClientWorld world, double x, double y, double z, double velX, double velY, double velZ) {
			GatewayParticle particle = new GatewayParticle(data, world, x, y, z, velX, velY, velZ);
			particle.pickSprite(this.sprites);
			return particle;
		}
	}

}
