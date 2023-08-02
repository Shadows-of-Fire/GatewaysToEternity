package shadows.gateways.compat;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTab.TabVisibility;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import shadows.gateways.GatewayObjects;
import shadows.gateways.Gateways;

@JeiPlugin
public class GatewayJEIPlugin implements IModPlugin {

    @Override
    public void registerItemSubtypes(ISubtypeRegistration reg) {
        reg.registerSubtypeInterpreter(GatewayObjects.GATE_PEARL.get(), new GateOpenerSubtypes());
    }

    @Override
    public void registerRecipes(IRecipeRegistration reg) {
        Dummy d = new Dummy();
        GatewayObjects.GATE_PEARL.get().fillItemCategory(CreativeModeTabs.searchTab(), d);
        reg.addIngredientInfo(d.list, VanillaTypes.ITEM_STACK, Component.translatable("info.gateways.gate_pearl"), Component.translatable("info.gateways.gate_pearl.2"));
    }

    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(Gateways.MODID, "gateways");
    }

    private class GateOpenerSubtypes implements IIngredientSubtypeInterpreter<ItemStack> {

        @Override
        public String apply(ItemStack stack, UidContext context) {
            if (stack.hasTag() && stack.getTag().contains("gateway")) {
                return stack.getTag().getString("gateway");
            }
            return ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();
        }

    }

    private class Dummy implements CreativeModeTab.Output {

        NonNullList<ItemStack> list = NonNullList.create();

        @Override
        public void accept(ItemStack pStack, TabVisibility pTabVisibility) {
            list.add(pStack);
        }

    }

}
