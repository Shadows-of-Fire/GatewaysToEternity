package shadows.gateways.compat;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import shadows.gateways.GatewayObjects;
import shadows.gateways.GatewaysToEternity;

@JeiPlugin
public class GatewayJEIPlugin implements IModPlugin {

	@Override
	public void registerItemSubtypes(ISubtypeRegistration reg) {
		reg.registerSubtypeInterpreter(GatewayObjects.SMALL_GATE_OPENER, new GateOpenerSubtypes());
	}

	@Override
	public ResourceLocation getPluginUid() {
		return new ResourceLocation(GatewaysToEternity.MODID, "gateways");
	}

	private class GateOpenerSubtypes implements ISubtypeInterpreter {

		@Override
		public String apply(ItemStack stack) {
			return ISubtypeInterpreter.NONE;
		}

		@Override
		public String apply(ItemStack stack, UidContext context) {
			if (!stack.hasTag()) return ISubtypeInterpreter.NONE;
			CompoundNBT gateData = stack.getTag().getCompound("gateway_data");
			if (gateData.isEmpty()) return ISubtypeInterpreter.NONE;
			return stack.getItem().getRegistryName() + "@" + gateData.getString("name");
		}

	}

}