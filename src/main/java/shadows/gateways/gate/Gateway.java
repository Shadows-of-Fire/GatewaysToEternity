package shadows.gateways.gate;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.text.Color;
import shadows.gateways.entity.GatewayEntity.GatewaySize;
import shadows.gateways.misc.RandomAttributeModifier;
import shadows.placebo.json.PlaceboJsonReloadListener.TypeKeyedBase;
import shadows.placebo.util.json.ItemAdapter;
import shadows.placebo.util.json.NBTAdapter;

public class Gateway extends TypeKeyedBase<Gateway> {
	public static final Gson GSON = new GsonBuilder().setPrettyPrinting().registerTypeHierarchyAdapter(Reward.class, new Reward.Serializer()).registerTypeAdapter(ItemStack.class, ItemAdapter.INSTANCE).registerTypeAdapter(CompoundNBT.class, NBTAdapter.INSTANCE).registerTypeAdapter(RandomAttributeModifier.class, new RandomAttributeModifier.Deserializer()).registerTypeAdapter(Wave.class, new Wave.Serializer()).create();

	protected final GatewaySize size;
	protected final Color color;
	protected final List<Wave> waves;
	protected final List<Reward> rewards;
	protected final int completionXp;
	protected final double spawnRange;
	protected final double leashRange;

	Gateway(GatewaySize size, Color color, List<Wave> waves, List<Reward> rewards, int completionXp, double spawnRange, double leashRange) {
		this.size = size;
		this.color = color;
		this.waves = waves;
		this.rewards = rewards;
		this.completionXp = completionXp;
		this.spawnRange = spawnRange;
		this.leashRange = leashRange;
	}

	public GatewaySize getSize() {
		return size;
	}

	public Color getColor() {
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
		obj.addProperty("leash_range", this.leashRange);
		return obj;
	}

	public static Gateway read(JsonObject obj) {
		String _size = JSONUtils.getAsString(obj, "size").toUpperCase(Locale.ROOT);
		GatewaySize size;
		try {
			size = GatewaySize.valueOf(_size);
		} catch (IllegalArgumentException ex) {
			throw new JsonParseException("Invalid gateway size " + _size);
		}
		String _color = JSONUtils.getAsString(obj, "color");
		Color color = Color.parseColor(_color);
		if (color == null) { throw new JsonParseException("Invalid gateway color " + _color); }
		List<Wave> waves = GSON.fromJson(obj.get("waves"), new TypeToken<List<Wave>>() {
		}.getType());
		List<Reward> rewards = GSON.fromJson(obj.get("rewards"), new TypeToken<List<Reward>>() {
		}.getType());
		int completionXp = JSONUtils.getAsInt(obj, "completion_xp");
		double spawnRange = JSONUtils.getAsFloat(obj, "spawn_range");
		double leashRange = JSONUtils.getAsFloat(obj, "leash_range", 32);
		return new Gateway(size, color, waves, rewards, completionXp, spawnRange, leashRange);
	}

	public void write(PacketBuffer buf) {
		buf.writeByte(size.ordinal());
		buf.writeUtf(color.serialize());
		buf.writeVarInt(waves.size());
		waves.forEach(w -> w.write(buf));
		buf.writeVarInt(rewards.size());
		rewards.forEach(r -> r.write(buf));
		buf.writeInt(completionXp);
		buf.writeDouble(spawnRange);
		buf.writeDouble(leashRange);
	}

	public static Gateway read(PacketBuffer buf) {
		GatewaySize size = GatewaySize.values()[buf.readByte()];
		Color color = Color.parseColor(buf.readUtf());
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
		double leashRange = buf.readDouble();
		return new Gateway(size, color, waves, rewards, completionXp, spawnRange, leashRange);
	}

	public double getLeashRangeSq() {
		return this.leashRange * this.leashRange;
	}
}
