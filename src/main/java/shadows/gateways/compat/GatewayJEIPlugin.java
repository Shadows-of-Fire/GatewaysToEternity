package shadows.gateways.compat;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import shadows.gateways.GatewayObjects;
import shadows.gateways.GatewaysToEternity;

@JeiPlugin
public class GatewayJEIPlugin implements IModPlugin {

	@Override
	public void registerItemSubtypes(ISubtypeRegistration reg) {
		reg.registerSubtypeInterpreter(GatewayObjects.GATE_OPENER, new GateOpenerSubtypes());
	}

	@Override
	public ResourceLocation getPluginUid() {
		return new ResourceLocation(GatewaysToEternity.MODID, "gateways");
	}

	private class GateOpenerSubtypes implements IIngredientSubtypeInterpreter<ItemStack> {

		@Override
		public String apply(ItemStack stack, UidContext context) {
			if (stack.hasTag() && stack.getTag().contains("gateway")) { return stack.getTag().getString("gateway"); }
			return stack.getItem().getRegistryName().toString();
		}

	}

}