package dev.shadowsoffire.gateways.gate;

import dev.shadowsoffire.gateways.Gateways;
import dev.shadowsoffire.placebo.reload.DynamicRegistry;

public class GatewayRegistry extends DynamicRegistry<Gateway> {

    public static final GatewayRegistry INSTANCE = new GatewayRegistry();

    private GatewayRegistry() {
        super(Gateways.LOGGER, "gateways", true, false);
    }

    @Override
    protected void registerBuiltinSerializers() {
        this.registerSerializer(DEFAULT, Gateway.SERIALIZER);
    }

}
