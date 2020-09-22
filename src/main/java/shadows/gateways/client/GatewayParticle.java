package shadows.gateways.client;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.client.particle.IAnimatedSprite;
import net.minecraft.client.particle.IParticleFactory;
import net.minecraft.client.particle.IParticleRenderType;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SpriteTexturedParticle;
import net.minecraft.network.PacketBuffer;
import net.minecraft.particles.IParticleData;
import net.minecraft.particles.ParticleType;
import net.minecraft.particles.RedstoneParticleData;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import shadows.gateways.GatewayObjects;

public class GatewayParticle extends SpriteTexturedParticle {

	public GatewayParticle(RedstoneParticleData data, World world, double x, double y, double z, double velX, double velY, double velZ) {
		super(world, x, y, z, velX, velY, velZ);
		this.particleRed = data.getRed();
		this.particleGreen = data.getGreen();
		this.particleBlue = data.getBlue();
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

	public static class Factory implements IParticleFactory<RedstoneParticleData> {
		protected final IAnimatedSprite sprites;

		public Factory(IAnimatedSprite sprites) {
			this.sprites = sprites;
		}

		public Particle makeParticle(RedstoneParticleData data, World world, double x, double y, double z, double velX, double velY, double velZ) {
			GatewayParticle particle = new GatewayParticle(data, world, x, y, z, velX, velY, velZ);
			particle.selectSpriteRandomly(this.sprites);
			return particle;
		}
	}

	public static class Data extends RedstoneParticleData {

		public Data(float r, float g, float b, float a) {
			super(r, g, b, a);
		}

		public Data(int r, int g, int b, int a) {
			super(r / 255F, g / 255F, b / 255F, a / 255F);
		}

		@Override
		public ParticleType<RedstoneParticleData> getType() {
			return GatewayObjects.GLOW;
		}

		public static final IParticleData.IDeserializer<RedstoneParticleData> DESERIALIZER = new IParticleData.IDeserializer<RedstoneParticleData>() {
			public RedstoneParticleData deserialize(ParticleType<RedstoneParticleData> type, StringReader reader) throws CommandSyntaxException {
				reader.expect(' ');
				float f = (float) reader.readDouble();
				reader.expect(' ');
				float f1 = (float) reader.readDouble();
				reader.expect(' ');
				float f2 = (float) reader.readDouble();
				reader.expect(' ');
				float f3 = (float) reader.readDouble();
				return new Data(f, f1, f2, f3);
			}

			public RedstoneParticleData read(ParticleType<RedstoneParticleData> type, PacketBuffer buf) {
				return new Data(buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat());
			}
		};
	}

}
