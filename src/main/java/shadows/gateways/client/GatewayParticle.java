package shadows.gateways.client;

import java.util.Locale;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import shadows.gateways.GatewayObjects;

@SuppressWarnings("deprecation")
public class GatewayParticle extends TextureSheetParticle {

	static final ParticleRenderType RENDER_TYPE = new ParticleRenderType() {
		public void begin(BufferBuilder bufferBuilder, TextureManager textureManager) {
			//RenderSystem.enableAlphaTest();
			RenderSystem.depthMask(false);
			RenderSystem.enableBlend();
			GlStateManager._disableCull();
			RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE);
			//RenderSystem.alphaFunc(GL11.GL_GREATER, 0.003921569F);
			RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
			bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
		}

		public void end(Tesselator tesselator) {
			tesselator.end();
		}

		public String toString() {
			return "GatewayParticleType";
		}
	};

	public GatewayParticle(GatewayParticle.Data data, Level world, double x, double y, double z, double velX, double velY, double velZ) {
		super((ClientLevel) world, x, y, z, velX, velY, velZ);
		this.rCol = data.red;
		this.gCol = data.green;
		this.bCol = data.blue;
		this.lifetime = 40;
		this.xd = velX;
		this.yd = velY;
		this.zd = velZ;
	}

	@Override
	public void render(VertexConsumer pBuffer, Camera pRenderInfo, float pPartialTicks) {
		RENDER_TYPE.begin((BufferBuilder) pBuffer, null);
		super.render(pBuffer, pRenderInfo, pPartialTicks);
		RENDER_TYPE.end(Tesselator.getInstance());
	}

	@Override
	public ParticleRenderType getRenderType() {
		return ParticleRenderType.CUSTOM;
	}

	public float getQuadSize(float p_217561_1_) {
		return 0.75F * this.quadSize * Mth.clamp(((float) this.age + p_217561_1_) / (float) this.lifetime * 32.0F, 0.0F, 1.0F);
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

	public static class Factory implements ParticleProvider<GatewayParticle.Data> {
		protected final SpriteSet sprites;

		public Factory(SpriteSet sprites) {
			this.sprites = sprites;
		}

		public Particle createParticle(GatewayParticle.Data data, ClientLevel world, double x, double y, double z, double velX, double velY, double velZ) {
			GatewayParticle particle = new GatewayParticle(data, world, x, y, z, velX, velY, velZ);
			particle.pickSprite(this.sprites);
			return particle;
		}
	}

	public static class Data implements ParticleOptions {

		public final float red, green, blue;

		public Data(float r, float g, float b) {
			this.red = r;
			this.green = g;
			this.blue = b;
		}

		public Data(int r, int g, int b) {
			this(r / 255F, g / 255F, b / 255F);
		}

		@Override
		public ParticleType<Data> getType() {
			return GatewayObjects.GLOW;
		}

		public static final Codec<Data> CODEC = RecordCodecBuilder.create(builder -> {
			return builder.group(Codec.FLOAT.fieldOf("r").forGetter((data) -> {
				return data.red;
			}), Codec.FLOAT.fieldOf("g").forGetter((data) -> {
				return data.green;
			}), Codec.FLOAT.fieldOf("b").forGetter((data) -> {
				return data.blue;
			})).apply(builder, Data::new);
		});

		public static final ParticleOptions.Deserializer<Data> DESERIALIZER = new ParticleOptions.Deserializer<Data>() {
			public Data fromCommand(ParticleType<Data> type, StringReader reader) throws CommandSyntaxException {
				reader.expect(' ');
				float f = (float) reader.readDouble();
				reader.expect(' ');
				float f1 = (float) reader.readDouble();
				reader.expect(' ');
				float f2 = (float) reader.readDouble();
				return new Data(f, f1, f2);
			}

			public Data fromNetwork(ParticleType<Data> type, FriendlyByteBuf buf) {
				return new Data(buf.readFloat(), buf.readFloat(), buf.readFloat());
			}
		};

		@Override
		public void writeToNetwork(FriendlyByteBuf buffer) {
			buffer.writeFloat(this.red);
			buffer.writeFloat(this.green);
			buffer.writeFloat(this.blue);
		}

		@Override
		public String writeToString() {
			return String.format(Locale.ROOT, "%s %.2f %.2f %.2f", Registry.PARTICLE_TYPE.getKey(this.getType()), this.red, this.green, this.blue);
		}
	}

}
