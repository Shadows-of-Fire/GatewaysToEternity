package dev.shadowsoffire.gateways.gate;

import dev.shadowsoffire.gateways.Gateways;
import dev.shadowsoffire.gateways.gate.endless.EndlessGateway;
import dev.shadowsoffire.gateways.gate.normal.NormalGateway;
import dev.shadowsoffire.placebo.dynreg.DynamicRegistry;
import dev.shadowsoffire.placebo.dynreg.RegistrySerializer;
import dev.shadowsoffire.placebo.dynreg.SubtypedSerializer;

public class GatewayRegistry extends DynamicRegistry<Gateway> {

    /**
     * Public serializer so external mods (e.g. Apotheosis compat) can register additional Gateway subtypes.
     */
    public static final SubtypedSerializer<Gateway> SERIALIZER = RegistrySerializer.<Gateway>subtypedSynced("gateways")
        .registerDefault(Gateways.loc("normal"), NormalGateway.CODEC)
        .register(Gateways.loc("endless"), EndlessGateway.CODEC);

    public static final GatewayRegistry INSTANCE = new GatewayRegistry();

    private GatewayRegistry() {
        super(Gateways.LOGGER, Gateways.loc("gateways"), SERIALIZER);
    }

}
