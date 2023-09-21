package dev.shadowsoffire.gateways.gate;

import dev.shadowsoffire.gateways.Gateways;
import dev.shadowsoffire.gateways.gate.endless.EndlessGateway;
import dev.shadowsoffire.gateways.gate.normal.NormalGateway;
import dev.shadowsoffire.placebo.reload.DynamicRegistry;

public class GatewayRegistry extends DynamicRegistry<Gateway> {

    public static final GatewayRegistry INSTANCE = new GatewayRegistry();

    private GatewayRegistry() {
        super(Gateways.LOGGER, "gateways", true, true);
    }

    @Override
    protected void registerBuiltinCodecs() {
        this.registerDefaultCodec(Gateways.loc("normal"), NormalGateway.CODEC);
        this.registerCodec(Gateways.loc("endless"), EndlessGateway.CODEC);
    }

}
