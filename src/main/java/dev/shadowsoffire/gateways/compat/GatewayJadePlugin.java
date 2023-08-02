package dev.shadowsoffire.gateways.compat;

import dev.shadowsoffire.gateways.GatewayObjects;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class GatewayJadePlugin implements IWailaPlugin {
	@Override
	public void registerClient(IWailaClientRegistration reg) {
		reg.hideTarget(GatewayObjects.GATEWAY.get());
	}
}
