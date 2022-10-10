package shadows.gateways.recipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.ShapedRecipe;
import net.minecraft.util.ResourceLocation;
import shadows.gateways.item.GatePearlItem;

public class GatewayRecipeSerializer extends ShapedRecipe.Serializer {

	public static final GatewayRecipeSerializer INSTANCE = new GatewayRecipeSerializer();

	@Override
	public ShapedRecipe fromJson(ResourceLocation id, JsonObject json) {
		ShapedRecipe recipe = super.fromJson(id, json);
		ItemStack gateway = recipe.getResultItem();
		if (!(gateway.getItem() instanceof GatePearlItem)) {
			throw new JsonSyntaxException("Gateway Recipe output must be a gate opener item.  Provided: " + gateway.getItem().getRegistryName());
		}
		gateway.getOrCreateTag().putString("gateway", json.get("gateway").getAsString());
		return recipe;
	}
}
