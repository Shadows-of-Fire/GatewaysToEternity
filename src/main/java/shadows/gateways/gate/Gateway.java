package shadows.gateways.gate;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import shadows.gateways.entity.GatewayEntity.GatewaySize;
import shadows.placebo.json.ItemAdapter;
import shadows.placebo.json.NBTAdapter;
import shadows.placebo.json.PlaceboJsonReloadListener.TypeKeyedBase;
import shadows.placebo.json.RandomAttributeModifier;

public class Gateway extends TypeKeyedBase<Gateway> {
	public static final Gson GSON = new GsonBuilder().setPrettyPrinting().registerTypeHierarchyAdapter(Reward.class, new Reward.Serializer()).registerTypeAdapter(ItemStack.class, ItemAdapter.INSTANCE).registerTypeAdapter(CompoundTag.class, NBTAdapter.INSTANCE).registerTypeAdapter(RandomAttributeModifier.class, new RandomAttributeModifier.Deserializer()).registerTypeAdapter(Wave.class, new Wave.Serializer()).create();

	protected final GatewaySize size;
	protected final TextColor color;
	protected final List<Wave> waves;
	protected final List<Reward> rewards;
	protected final int completionXp;
	protected final double spawnRange;

	Gateway(GatewaySize size, TextColor color, List<Wave> waves, List<Reward> rewards, int completionXp, double spawnRange) {
		this.size = size;
		this.color = color;
		this.waves = waves;
		this.rewards = rewards;
		this.completionXp = completionXp;
		this.spawnRange = spawnRange;
	}

	public GatewaySize getSize() {
		return size;
	}

	public TextColor getColor() {
		return color;
	}

	public List<Wave> getWaves() {
		return waves;
	}

	public int getNumWaves() {
		return waves.size();
	}

	public Wave getWave(int n) {
		return this.waves.get(n);
	}

	public List<Reward> getRewards() {
		return rewards;
	}

	public int getCompletionXp() {
		return this.completionXp;
	}

	public double getSpawnRange() {
		return this.spawnRange;
	}

	public JsonObject write() {
		JsonObject obj = new JsonObject();
		obj.addProperty("size", size.name().toLowerCase(Locale.ROOT));
		obj.addProperty("color", color.serialize());
		obj.add("waves", GSON.toJsonTree(waves));
		obj.add("rewards", GSON.toJsonTree(rewards));
		obj.addProperty("completion_xp", completionXp);
		obj.addProperty("spawn_range", this.spawnRange);
		return obj;
	}

	public static Gateway read(JsonObject obj) {
		GatewaySize size = GatewaySize.valueOf(GsonHelper.getAsString(obj, "size").toUpperCase(Locale.ROOT));
		TextColor color = TextColor.parseColor(GsonHelper.getAsString(obj, "color"));
		List<Wave> waves = GSON.fromJson(obj.get("waves"), new TypeToken<List<Wave>>() {
		}.getType());
		List<Reward> rewards = GSON.fromJson(obj.get("rewards"), new TypeToken<List<Reward>>() {
		}.getType());
		int completionXp = GsonHelper.getAsInt(obj, "completion_xp");
		double spawnRange = GsonHelper.getAsDouble(obj, "spawn_range");
		return new Gateway(size, color, waves, rewards, completionXp, spawnRange);
	}

	public void write(FriendlyByteBuf buf) {
		buf.writeByte(size.ordinal());
		buf.writeUtf(color.serialize());
		buf.writeVarInt(waves.size());
		waves.forEach(w -> w.write(buf));
		buf.writeVarInt(rewards.size());
		rewards.forEach(r -> r.write(buf));
		buf.writeInt(completionXp);
		buf.writeDouble(spawnRange);
	}

	public static Gateway read(FriendlyByteBuf buf) {
		GatewaySize size = GatewaySize.values()[buf.readByte()];
		TextColor color = TextColor.parseColor(buf.readUtf());
		int nWaves = buf.readVarInt();
		List<Wave> waves = new ArrayList<>(nWaves);
		for (int i = 0; i < nWaves; i++) {
			waves.add(Wave.read(buf));
		}
		int nRewards = buf.readVarInt();
		List<Reward> rewards = new ArrayList<>(nRewards);
		for (int i = 0; i < nRewards; i++) {
			rewards.add(Reward.read(buf));
		}
		int completionXp = buf.readInt();
		double spawnRange = buf.readDouble();
		return new Gateway(size, color, waves, rewards, completionXp, spawnRange);
	}
}
