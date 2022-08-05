package shadows.gateways.client;

import java.util.Locale;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import shadows.gateways.GatewayObjects;

@SuppressWarnings("deprecation")
public class GatewayParticleData implements ParticleOptions {

	public final float red, green, blue;

	public GatewayParticleData(float r, float g, float b) {
		this.red = r;
		this.green = g;
		this.blue = b;
	}

	public GatewayParticleData(int r, int g, int b) {
		this(r / 255F, g / 255F, b / 255F);
	}

	@Override
	public ParticleType<GatewayParticleData> getType() {
		return GatewayObjects.GLOW;
	}

	public static final Codec<GatewayParticleData> CODEC = RecordCodecBuilder.create(builder -> {
		return builder.group(Codec.FLOAT.fieldOf("r").forGetter((data) -> {
			return data.red;
		}), Codec.FLOAT.fieldOf("g").forGetter((data) -> {
			return data.green;
		}), Codec.FLOAT.fieldOf("b").forGetter((data) -> {
			return data.blue;
		})).apply(builder, GatewayParticleData::new);
	});

	public static final ParticleOptions.Deserializer<GatewayParticleData> DESERIALIZER = new ParticleOptions.Deserializer<GatewayParticleData>() {
		public GatewayParticleData fromCommand(ParticleType<GatewayParticleData> type, StringReader reader) throws CommandSyntaxException {
			reader.expect(' ');
			float f = (float) reader.readDouble();
			reader.expect(' ');
			float f1 = (float) reader.readDouble();
			reader.expect(' ');
			float f2 = (float) reader.readDouble();
			return new GatewayParticleData(f, f1, f2);
		}

		public GatewayParticleData fromNetwork(ParticleType<GatewayParticleData> type, FriendlyByteBuf buf) {
			return new GatewayParticleData(buf.readFloat(), buf.readFloat(), buf.readFloat());
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