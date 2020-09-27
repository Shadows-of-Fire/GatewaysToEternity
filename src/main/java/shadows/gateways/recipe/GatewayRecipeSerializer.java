package shadows.gateways.recipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.ShapedRecipe;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.WeightedSpawnerEntity;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.registries.ForgeRegistries;
import shadows.gateways.item.GatewayItem;
import shadows.placebo.util.TagBuilder;

public class GatewayRecipeSerializer extends ShapedRecipe.Serializer {

	public static final GatewayRecipeSerializer INSTANCE = new GatewayRecipeSerializer();

	@Override
	public GatewayRecipe read(ResourceLocation id, JsonObject json) {
		ShapedRecipe recipe = super.read(id, json);
		ItemStack gateway = recipe.getRecipeOutput();
		if (!(gateway.getItem() instanceof GatewayItem)) {
			throw new JsonSyntaxException("Gateway Recipe output must be a gate opener item.  Provided: " + gateway.getItem().getRegistryName());
		}
		JsonObject gatewayData = json.get("result").getAsJsonObject().get("gateway").getAsJsonObject();
		CompoundNBT tag = toNBT(gatewayData);
		gateway.getOrCreateTag().put("gateway_data", tag);
		TranslationTextComponent name = new TranslationTextComponent("gateways.gate_opener", new TranslationTextComponent(tag.getString("name")));
		gateway.getTag().putString("opener_name", ITextComponent.Serializer.toJson(name));
		return new GatewayRecipe(id, recipe.getGroup(), recipe.getWidth(), recipe.getHeight(), recipe.getIngredients(), gateway);
	}

	public static CompoundNBT toNBT(JsonObject data) {
		String name = data.has("name") ? data.get("name").getAsString() : "";
		ResourceLocation entity = new ResourceLocation(data.get("entity").getAsString());
		int completionXP = data.has("completion_xp") ? data.get("completion_xp").getAsInt() : 150;
		int maxWaveTime = data.has("max_wave_time") ? data.get("max_wave_time").getAsInt() : 600;
		String color = data.has("color") ? data.get("color").getAsString() : "blue";
		CompoundNBT tag = new CompoundNBT();
		tag.putString("name", name);
		WeightedSpawnerEntity ws = new WeightedSpawnerEntity(1, TagBuilder.getDefaultTag(ForgeRegistries.ENTITIES.getValue(entity)));
		tag.put("entity", ws.toCompoundTag());
		tag.putInt("completion_xp", completionXP);
		tag.putInt("max_wave_time", maxWaveTime);
		tag.putString("color", color);
		return tag;
	}

}
