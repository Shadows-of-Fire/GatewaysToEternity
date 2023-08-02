package shadows.gateways.recipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraftforge.registries.ForgeRegistries;
import shadows.gateways.item.GatePearlItem;

public class GatewayRecipeSerializer extends ShapedRecipe.Serializer {

    @Override
    public ShapedRecipe fromJson(ResourceLocation id, JsonObject json) {
        ShapedRecipe recipe = super.fromJson(id, json);
        ItemStack gateway = recipe.getResultItem(null);
        if (!(gateway.getItem() instanceof GatePearlItem)) {
            throw new JsonSyntaxException("Gateway Recipe output must be a gate opener item.  Provided: " + ForgeRegistries.ITEMS.getKey(gateway.getItem()));
        }
        gateway.getOrCreateTag().putString("gateway", json.get("gateway").getAsString());
        return recipe;
    }
}
