package dev.shadowsoffire.gateways.compat;

import dev.shadowsoffire.gateways.GatewayObjects;
import dev.shadowsoffire.gateways.Gateways;
import dev.shadowsoffire.gateways.gate.Gateway;
import dev.shadowsoffire.gateways.item.GatePearlItem;
import dev.shadowsoffire.placebo.dynreg.DynamicHolder;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

@JeiPlugin
public class GatewayJEIPlugin implements IModPlugin {

    @Override
    public void registerItemSubtypes(ISubtypeRegistration reg) {
        reg.registerSubtypeInterpreter(GatewayObjects.GATE_PEARL.value(), new GateOpenerSubtypes());
    }

    @Override
    public void registerRecipes(IRecipeRegistration reg) {
        NonNullList<ItemStack> list = NonNullList.create();
        GatePearlItem.generateGatePearlStacks(list::add);
        reg.addIngredientInfo(list, VanillaTypes.ITEM_STACK, Component.translatable("info.gateways.gate_pearl"), Component.translatable("info.gateways.gate_pearl.2"));
    }

    @Override
    public Identifier getPluginUid() {
        return Gateways.loc("gateways");
    }

    private static class GateOpenerSubtypes implements ISubtypeInterpreter<ItemStack> {

        @Override
        public DynamicHolder<Gateway> getSubtypeData(ItemStack stack, UidContext context) {
            return GatePearlItem.getGate(stack);
        }

    }

}
