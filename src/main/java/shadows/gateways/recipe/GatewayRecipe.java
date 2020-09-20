package shadows.gateways.recipe;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapedRecipe;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;

public class GatewayRecipe extends ShapedRecipe {

	public GatewayRecipe(ResourceLocation id, String group, int width, int height, NonNullList<Ingredient> ingredients, ItemStack output) {
		super(id, group, width, height, ingredients, output);
	}

	@Override
	public IRecipeSerializer<?> getSerializer() {
		return GatewayRecipeSerializer.INSTANCE;
	}

}
