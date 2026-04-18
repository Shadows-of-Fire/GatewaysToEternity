package dev.shadowsoffire.gateways.compat;

import dev.shadowsoffire.gateways.GatewayObjects;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class GatewayJadePlugin implements IWailaPlugin {

    @Override
    @SuppressWarnings("deprecation")
    public void register(IWailaCommonRegistration reg) {
        reg.entityTypeOperations().hide(GatewayObjects.NORMAL_GATEWAY.get().builtInRegistryHolder().key());
        reg.entityTypeOperations().hide(GatewayObjects.ENDLESS_GATEWAY.get().builtInRegistryHolder().key());
    }

    @Override
    public void registerClient(IWailaClientRegistration reg) {}

}
