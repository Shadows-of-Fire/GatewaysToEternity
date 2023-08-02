package shadows.gateways.gate;

import dev.shadowsoffire.placebo.json.PlaceboJsonReloadListener;
import shadows.gateways.Gateways;

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
