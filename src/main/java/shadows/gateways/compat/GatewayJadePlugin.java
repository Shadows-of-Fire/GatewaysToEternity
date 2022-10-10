package shadows.gateways.compat;

import mcp.mobius.waila.api.IEntityAccessor;
import mcp.mobius.waila.api.IEntityComponentProvider;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.IRegistrar;
import mcp.mobius.waila.api.IWailaPlugin;
import mcp.mobius.waila.api.WailaPlugin;
import net.minecraft.entity.Entity;
import shadows.gateways.entity.GatewayEntity;

@WailaPlugin
public class GatewayJadePlugin implements IWailaPlugin, IEntityComponentProvider {

	@Override
	public void register(IRegistrar arg0) {
		arg0.registerOverrideEntityProvider(this, GatewayEntity.class);
	}

	@Override
	public Entity getOverride(IEntityAccessor accessor, IPluginConfig config) {
		return null;
	}
}
