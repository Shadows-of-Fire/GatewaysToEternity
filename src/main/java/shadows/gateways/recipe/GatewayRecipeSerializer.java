package shadows.gateways.recipe;

import com.google.gson.JsonObject;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.ShapedRecipe;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.WeightedSpawnerEntity;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.registries.ForgeRegistries;
import shadows.gateways.util.TagBuilder;

public class GatewayRecipeSerializer extends ShapedRecipe.Serializer {

	public static final GatewayRecipeSerializer INSTANCE = new GatewayRecipeSerializer();

	public GatewayRecipe read(ResourceLocation id, JsonObject json) {
		ShapedRecipe recipe = super.read(id, json);
		ItemStack gateway = recipe.getRecipeOutput();
		JsonObject gatewayData = json.get("result").getAsJsonObject().get("gateway").getAsJsonObject();
		CompoundNBT tag = toNBT(gatewayData);
		gateway.getOrCreateTag().put("gateway_data", tag);
		TranslationTextComponent name = new TranslationTextComponent("gateways.gate_opener", new TranslationTextComponent(tag.getString("name")));
		gateway.getTag().putString("opener_name", ITextComponent.Serializer.toJson(name));
		return new GatewayRecipe(id, recipe.getGroup(), recipe.getWidth(), recipe.getHeight(), recipe.getIngredients(), gateway);
	}

	public static CompoundNBT toNBT(JsonObject data) {
		String name = data.has("name") ? data.get("name").getAsString() : "";
		byte maxWaves = data.has("max_waves") ? data.get("max_waves").getAsByte() : 0b101;
		byte entitiesPerWave = data.has("entities_per_wave") ? data.get("entities_per_wave").getAsByte() : 0b11;
		byte spawnRange = data.has("spawn_range") ? data.get("spawn_range").getAsByte() : 0b101;
		ResourceLocation entity = new ResourceLocation(data.get("entity").getAsString());
		int completionXP = data.has("completion_xp") ? data.get("completion_xp").getAsInt() : 150;
		short wavePauseTime = data.has("wave_pause_time") ? data.get("wave_pause_time").getAsShort() : (short) 140;
		CompoundNBT tag = new CompoundNBT();
		tag.putString("name", name);
		tag.putByte("max_waves", maxWaves);
		tag.putByte("entities_per_wave", entitiesPerWave);
		tag.putByte("spawn_range", spawnRange);
		WeightedSpawnerEntity ws = new WeightedSpawnerEntity(1, TagBuilder.getDefaultTag(ForgeRegistries.ENTITIES.getValue(entity)));
		tag.put("entity", ws.toCompoundTag());
		tag.putInt("completion_xp", completionXP);
		tag.putShort("wave_pause_time", wavePauseTime);
		return tag;
	}

}
