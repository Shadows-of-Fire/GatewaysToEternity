package dev.shadowsoffire.gateways.payloads;

import java.util.List;
import java.util.Optional;
import java.util.function.IntFunction;

import dev.shadowsoffire.gateways.Gateways;
import dev.shadowsoffire.gateways.client.ParticleHandler;
import dev.shadowsoffire.gateways.entity.GatewayEntity;
import dev.shadowsoffire.placebo.network.PayloadProvider;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.ByIdMap;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ParticlePayload(int gateId, double x, double y, double z, int color, EffectType effectType) implements CustomPacketPayload {

    public static final Type<ParticlePayload> TYPE = new Type<>(Gateways.loc("particles"));
    public static final StreamCodec<FriendlyByteBuf, ParticlePayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, ParticlePayload::gateId,
        ByteBufCodecs.DOUBLE, ParticlePayload::x,
        ByteBufCodecs.DOUBLE, ParticlePayload::y,
        ByteBufCodecs.DOUBLE, ParticlePayload::z,
        ByteBufCodecs.INT, ParticlePayload::color,
        EffectType.STREAM_CODEC, ParticlePayload::effectType,
        ParticlePayload::new);

    public ParticlePayload(GatewayEntity source, double x, double y, double z, TextColor color, EffectType type) {
        this(source.getId(), x, y, z, color.getValue(), type);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static class Provider implements PayloadProvider<ParticlePayload> {

        @Override
        public Type<ParticlePayload> getType() {
            return TYPE;
        }

        @Override
        public StreamCodec<? super RegistryFriendlyByteBuf, ParticlePayload> getCodec() {
            return CODEC;
        }

        @Override
        public void handleClient(ParticlePayload msg, IPayloadContext ctx) {
            ParticleHandler.handle(msg);
        }

        @Override
        public List<ConnectionProtocol> getSupportedProtocols() {
            return List.of(ConnectionProtocol.PLAY);
        }

        @Override
        public Optional<PacketFlow> getFlow() {
            return Optional.of(PacketFlow.CLIENTBOUND);
        }

        @Override
        public String getVersion() {
            return "1";
        }

    }

    public static enum EffectType {

        /**
         * Spawns a cluster of particles around the given position.
         */
        IDLE,

        /**
         * Spawns a pillar of particles centered on the given position.
         */
        SPAWNED;

        public static final IntFunction<EffectType> BY_ID = ByIdMap.continuous(EffectType::ordinal, values(), ByIdMap.OutOfBoundsStrategy.WRAP);
        public static final StreamCodec<ByteBuf, EffectType> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, EffectType::ordinal);
    }

}
