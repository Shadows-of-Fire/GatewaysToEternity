package dev.shadowsoffire.gateways.net;

import java.util.Optional;
import java.util.function.Supplier;

import dev.shadowsoffire.gateways.client.ParticleHandler;
import dev.shadowsoffire.gateways.entity.GatewayEntity;
import dev.shadowsoffire.placebo.network.MessageHelper;
import dev.shadowsoffire.placebo.network.MessageProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.TextColor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent.Context;

public class ParticleMessage {

    public int gateId;
    public double x, y, z;
    public Type type;
    public int color;

    public ParticleMessage(GatewayEntity source, double x, double y, double z, TextColor color, Type type) {
        this(source.getId(), x, y, z, color.getValue(), type);
    }

    public ParticleMessage(int id, double x, double y, double z, int color, Type type) {
        this.gateId = id;
        this.x = x;
        this.y = y;
        this.z = z;
        this.color = color;
        this.type = type;
    }

    public static enum Type {

        /**
         * Spawns a cluster of particles around the given position.
         */
        IDLE,

        /**
         * Spawns a pillar of particles centered on the given position.
         */
        SPAWNED;
    }

    public static class Provider implements MessageProvider<ParticleMessage> {

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
            Type type = Type.values()[buf.readByte()];
            return new ParticleMessage(id, x, y, z, color, type);
        }

        @Override
        public void write(ParticleMessage msg, FriendlyByteBuf buf) {
            buf.writeInt(msg.gateId);
            buf.writeDouble(msg.x);
            buf.writeDouble(msg.y);
            buf.writeDouble(msg.z);
            buf.writeInt(msg.color);
            buf.writeByte(msg.type.ordinal());
        }

        @Override
        public void handle(ParticleMessage msg, Supplier<Context> ctx) {
            MessageHelper.handlePacket(() -> {
                ParticleHandler.handle(msg);
            }, ctx);
        }

        @Override
        public Optional<NetworkDirection> getNetworkDirection() {
            return Optional.of(NetworkDirection.PLAY_TO_CLIENT);
        }

    }

}
