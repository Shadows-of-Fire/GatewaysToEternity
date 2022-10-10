package shadows.gateways.gate;

import shadows.gateways.Gateways;
import shadows.placebo.json.PlaceboJsonReloadListener;
import shadows.placebo.json.SerializerBuilder;

public class GatewayManager extends PlaceboJsonReloadListener<Gateway> {

	public static final GatewayManager INSTANCE = new GatewayManager();

	private GatewayManager() {
		super(Gateways.LOGGER, "gateways", true, true);
	}

	@Override
	protected void registerBuiltinSerializers() {
		this.registerSerializer(DEFAULT, new SerializerBuilder<Gateway>("Gateway").withJsonDeserializer(Gateway::read).withJsonSerializer(Gateway::write).withNetworkDeserializer(Gateway::read).withNetworkSerializer(Gateway::write));
	}

}
