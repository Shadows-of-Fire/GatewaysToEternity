package shadows.gateways.gate;

import shadows.gateways.Gateways;
import shadows.placebo.json.PlaceboJsonReloadListener;

public class GatewayManager extends PlaceboJsonReloadListener<Gateway> {

	public static final GatewayManager INSTANCE = new GatewayManager();

	private GatewayManager() {
		super(Gateways.LOGGER, "gateways", true, true);
	}

	@Override
	protected void registerBuiltinSerializers() {
		this.registerSerializer(DEFAULT, Gateway.SERIALIZER);
	}

}
