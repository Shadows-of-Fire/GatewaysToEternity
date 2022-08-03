package shadows.gateways.compat;

import mcp.mobius.waila.api.IWailaClientRegistration;
import mcp.mobius.waila.api.IWailaPlugin;
import mcp.mobius.waila.api.WailaPlugin;
import shadows.gateways.GatewayObjects;

@WailaPlugin
public class GatewayJadePlugin implements IWailaPlugin {
	@Override
	public void registerClient(IWailaClientRegistration reg) {
		reg.hideTarget(GatewayObjects.GATEWAY);
	}
}
