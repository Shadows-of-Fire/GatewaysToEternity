package shadows.gateways.client;

import java.util.Locale;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.client.particle.IAnimatedSprite;
import net.minecraft.client.particle.IParticleFactory;
import net.minecraft.client.particle.IParticleRenderType;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SpriteTexturedParticle;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.PacketBuffer;
import net.minecraft.particles.IParticleData;
import net.minecraft.particles.ParticleType;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import shadows.gateways.GatewayObjects;

@SuppressWarnings("deprecation")
public class GatewayParticle extends SpriteTexturedParticle {

	public GatewayParticle(GatewayParticle.Data data, World world, double x, double y, double z, double velX, double velY, double velZ) {
		super((ClientWorld) world, x, y, z, velX, velY, velZ);
		this.particleRed = data.red;
		this.particleGreen = data.green;
		this.particleBlue = data.blue;
		this.maxAge = 40; //TODO: Bake age into Data as alpha?
		this.motionX = velX;
		this.motionY = velY;
		this.motionZ = velZ;
	}

	@Override
	public IParticleRenderType getRenderType() {
		return IParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
	}

	public float getScale(float p_217561_1_) {
		return this.particleScale * MathHelper.clamp(((float) this.age + p_217561_1_) / (float) this.maxAge * 32.0F, 0.0F, 1.0F);
	}

	public void tick() {
		this.prevPosX = this.posX;
		this.prevPosY = this.posY;
		this.prevPosZ = this.posZ;
		this.particleAlpha = 1 - (float) this.age / this.maxAge;
		if (this.age++ >= this.maxAge) {
			this.setExpired();
		} else {
			this.move(this.motionX, this.motionY, this.motionZ);
			if (this.posY == this.prevPosY) {
				this.motionX *= 1.1D;
				this.motionZ *= 1.1D;
			}

			this.motionX *= (double) 0.86F;
			this.motionY *= (double) 0.86F;
			this.motionZ *= (double) 0.86F;
			if (this.onGround) {
				this.motionX *= (double) 0.7F;
				this.motionZ *= (double) 0.7F;
			}

		}
	}

	public static class Factory implements IParticleFactory<GatewayParticle.Data> {
		protected final IAnimatedSprite sprites;

		public Factory(IAnimatedSprite sprites) {
			this.sprites = sprites;
		}

		public Particle makeParticle(GatewayParticle.Data data, ClientWorld world, double x, double y, double z, double velX, double velY, double velZ) {
			GatewayParticle particle = new GatewayParticle(data, world, x, y, z, velX, velY, velZ);
			particle.selectSpriteRandomly(this.sprites);
			return particle;
		}
	}

	public static class Data implements IParticleData {

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

		public static final IParticleData.IDeserializer<Data> DESERIALIZER = new IParticleData.IDeserializer<Data>() {
			public Data deserialize(ParticleType<Data> type, StringReader reader) throws CommandSyntaxException {
				reader.expect(' ');
				float f = (float) reader.readDouble();
				reader.expect(' ');
				float f1 = (float) reader.readDouble();
				reader.expect(' ');
				float f2 = (float) reader.readDouble();
				return new Data(f, f1, f2);
			}

			public Data read(ParticleType<Data> type, PacketBuffer buf) {
				return new Data(buf.readFloat(), buf.readFloat(), buf.readFloat());
			}
		};

		@Override
		public void write(PacketBuffer buffer) {
			buffer.writeFloat(this.red);
			buffer.writeFloat(this.green);
			buffer.writeFloat(this.blue);
		}

		@Override
		public String getParameters() {
			return String.format(Locale.ROOT, "%s %.2f %.2f %.2f", Registry.PARTICLE_TYPE.getKey(this.getType()), this.red, this.green, this.blue);
		}
	}

}
