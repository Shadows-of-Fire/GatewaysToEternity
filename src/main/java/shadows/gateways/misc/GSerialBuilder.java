package shadows.gateways.misc;

import shadows.placebo.json.SerializerBuilder;

public class GSerialBuilder<V> extends SerializerBuilder<V> {

	public GSerialBuilder(String name) {
		super(name);
	}

	public GSerialBuilder<V> json(JsonDeserializer<V> jds, JsonSerializer<V> js) {
		return (GSerialBuilder<V>) this.withJsonDeserializer(jds).withJsonSerializer(js);
	}

	public GSerialBuilder<V> net(NetDeserializer<V> jds, NetSerializer<V> js) {
		return (GSerialBuilder<V>) this.withNetworkDeserializer(jds).withNetworkSerializer(js);
	}

}
