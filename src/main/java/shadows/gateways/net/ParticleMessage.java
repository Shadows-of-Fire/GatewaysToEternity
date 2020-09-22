package shadows.gateways.net;

import java.util.function.Supplier;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent.Context;
import shadows.gateways.client.ParticleHandler;
import shadows.gateways.entity.AbstractGatewayEntity;
import shadows.placebo.util.NetworkUtils;
import shadows.placebo.util.NetworkUtils.MessageProvider;

public class ParticleMessage extends MessageProvider<ParticleMessage> {

	public int gateId;
	public double x, y, z;
	public int type;
	public int color;

	public ParticleMessage(AbstractGatewayEntity source, double x, double y, double z, int color, int type) {
		this(source.getEntityId(), x, y, z, color, type);
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
	public ParticleMessage read(PacketBuffer buf) {
		int id = buf.readInt();
		double x = buf.readDouble();
		double y = buf.readDouble();
		double z = buf.readDouble();
		int color = buf.readInt();
		byte type = buf.readByte();
		return new ParticleMessage(id, x, y, z, color, type);
	}

	@Override
	public void write(ParticleMessage msg, PacketBuffer buf) {
		buf.writeInt(msg.gateId);
		buf.writeDouble(msg.x);
		buf.writeDouble(msg.y);
		buf.writeDouble(msg.z);
		buf.writeInt(msg.color);
		buf.writeByte(msg.type);
	}

	@Override
	public void handle(ParticleMessage msg, Supplier<Context> ctx) {
		NetworkUtils.handlePacket(() -> () -> {
			ParticleHandler.handle(msg);
		}, ctx.get());
	}

}