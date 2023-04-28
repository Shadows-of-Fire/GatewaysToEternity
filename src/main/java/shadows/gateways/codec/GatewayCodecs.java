package shadows.gateways.codec;

import com.google.common.collect.BiMap;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

import net.minecraft.resources.ResourceLocation;
import shadows.gateways.Gateways;
import shadows.placebo.codec.PlaceboCodecs.CodecProvider;
import shadows.placebo.codec.PlaceboCodecs.MapBackedCodec;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class GatewayCodecs {

	//TODO: Remove 1.20
	public static <V extends CodecProvider<V>> Codec<V> mapBacked(String name, BiMap<ResourceLocation, Codec<? extends V>> reg) {
		return new NamespaceDefaultedMapBackedCodec<>(name, reg);
	}

	public static class NamespaceDefaultedMapBackedCodec<V extends CodecProvider<V>> extends MapBackedCodec<V> {

		public NamespaceDefaultedMapBackedCodec(String name, BiMap<ResourceLocation, Codec<? extends V>> registry) {
			super(name, registry);
		}

		@Override
		public <T> DataResult<Pair<V, T>> decode(DynamicOps<T> ops, T input) {
			T typeObj = ops.get(input, "type").get().left().get();
			String typeStr = ops.getStringValue(typeObj).getOrThrow(false, Gateways.LOGGER::error);
			ResourceLocation type = typeStr.indexOf(':') == -1 ? Gateways.loc(typeStr) : new ResourceLocation(typeStr);
			Codec codec = this.registry.get(type);

			if (codec == null) {
				return DataResult.error("Failure when parsing a " + name + ". Unrecognized type: " + type);
			}

			return codec.decode(ops, input);
		}

	}

}
