package shadows.gateways.net;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.TextColor;
import net.minecraftforge.network.NetworkEvent.Context;
import shadows.gateways.client.ParticleHandler;
import shadows.gateways.entity.GatewayEntity;
import shadows.placebo.network.MessageHelper;
import shadows.placebo.network.MessageProvider;

public class ParticleMessage implements MessageProvider<ParticleMessage> {

	public int gateId;
	public double x, y, z;
	public int type;
	public int color;

	public ParticleMessage(GatewayEntity source, double x, double y, double z, TextColor color, int type) {
		this(source.getId(), x, y, z, color.getValue(), type);
	}

	public ParticleMessage(int id, double x, double y, double z, int color, int type) {
		this.gateId = id;
		this.x = x;
		this.y = y;
		this.z = z;
		this.color = color;
		this.type = type;
	}

	public ParticleMessage() {
	}

	@Override
	public Class<ParticleMessage> getMsgClass() {
		return ParticleMessage.class;
	}

	@Override
	public ParticleMessage read(FriendlyByteBuf buf) {
		int id = buf.readInt();
		double x = buf.readDouble();
		double y = buf.readDouble();
		double z = buf.readDouble();
		int color = buf.readInt();
		byte type = buf.readByte();
		return new ParticleMessage(id, x, y, z, color, type);
	}

	@Override
	public void write(ParticleMessage msg, FriendlyByteBuf buf) {
		buf.writeInt(msg.gateId);
		buf.writeDouble(msg.x);
		buf.writeDouble(msg.y);
		buf.writeDouble(msg.z);
		buf.writeInt(msg.color);
		buf.writeByte(msg.type);
	}

	@Override
	public void handle(ParticleMessage msg, Supplier<Context> ctx) {
		MessageHelper.handlePacket(() -> () -> {
			ParticleHandler.handle(msg);
		}, ctx);
	}

}