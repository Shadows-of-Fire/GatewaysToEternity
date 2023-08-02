package dev.shadowsoffire.gateways.gate;

import dev.shadowsoffire.gateways.Gateways;
import dev.shadowsoffire.placebo.reload.PlaceboJsonReloadListener;

public class GatewayManager extends PlaceboJsonReloadListener<Gateway> {

    public static final GatewayManager INSTANCE = new GatewayManager();

    private GatewayManager() {
        super(Gateways.LOGGER, "gateways", true, false);
    }

    @Override
    protected void registerBuiltinSerializers() {
        this.registerSerializer(DEFAULT, Gateway.SERIALIZER);
    }

}
