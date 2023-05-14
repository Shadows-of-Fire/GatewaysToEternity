package shadows.gateways.gate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import shadows.gateways.entity.GatewayEntity.GatewaySize;
import shadows.gateways.gate.SpawnAlgorithms.SpawnAlgorithm;
import shadows.placebo.json.ItemAdapter;
import shadows.placebo.json.NBTAdapter;
import shadows.placebo.json.PlaceboJsonReloadListener.TypeKeyedBase;
import shadows.placebo.json.RandomAttributeModifier;

public class Gateway extends TypeKeyedBase<Gateway> {

	public static final Gson GSON = new GsonBuilder().setPrettyPrinting().registerTypeHierarchyAdapter(Failure.class, new Failure.Serializer()).registerTypeHierarchyAdapter(Reward.class, new Reward.Serializer()).registerTypeAdapter(ItemStack.class, ItemAdapter.INSTANCE).registerTypeAdapter(CompoundTag.class, NBTAdapter.INSTANCE).registerTypeAdapter(RandomAttributeModifier.class, new RandomAttributeModifier.Deserializer()).registerTypeAdapter(Wave.class, new Wave.Serializer()).create();

	protected final GatewaySize size;
	protected final TextColor color;
	protected final List<Wave> waves;
	protected final List<Reward> rewards;
	protected final List<Failure> failures;
	protected final int completionXp;
	protected final double spawnRange;
	protected final double leashRange;
	protected final SpawnAlgorithm spawnAlgo;
	protected final boolean playerDamageOnly;
	protected final boolean allowDiscarding;

	public Gateway(GatewaySize size, TextColor color, List<Wave> waves, List<Reward> rewards, List<Failure> failures, int completionXp, double spawnRange, double leashRange, SpawnAlgorithm spawnAlgo, boolean onlyPlayerDamage, boolean allowDiscarding) {
		this.size = size;
		this.color = color;
		this.waves = waves;
		this.rewards = rewards;
		this.failures = failures;
		this.completionXp = completionXp;
		this.spawnRange = spawnRange;
		this.leashRange = leashRange;
		this.spawnAlgo = spawnAlgo;
		this.playerDamageOnly = onlyPlayerDamage;
		this.allowDiscarding = allowDiscarding;
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

	public List<Failure> getFailures() {
		return failures;
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
		String _size = GsonHelper.getAsString(obj, "size").toUpperCase(Locale.ROOT);
		GatewaySize size;
		try {
			size = GatewaySize.valueOf(_size);
		} catch (IllegalArgumentException ex) {
			throw new JsonParseException("Invalid gateway size " + _size);
		}
		String _color = GsonHelper.getAsString(obj, "color");
		TextColor color = TextColor.parseColor(_color);
		if (color == null) { throw new JsonParseException("Invalid gateway color " + _color); }
		List<Wave> waves = GSON.fromJson(obj.get("waves"), new TypeToken<List<Wave>>() {
		}.getType());
		List<Reward> rewards = GSON.fromJson(obj.get("rewards"), new TypeToken<List<Reward>>() {
		}.getType());
		List<Failure> failures = GSON.fromJson(obj.get("failures"), new TypeToken<List<Failure>>() {
		}.getType());
		if (failures == null) failures = Collections.emptyList();
		int completionXp = GsonHelper.getAsInt(obj, "completion_xp");
		double spawnRange = GsonHelper.getAsDouble(obj, "spawn_range");
		double leashRange = GsonHelper.getAsDouble(obj, "leash_range", 32);
		SpawnAlgorithm spawnAlgo = SpawnAlgorithms.NAMED_ALGORITHMS.get(new ResourceLocation(GsonHelper.getAsString(obj, "spawn_algorithm", "gateways:open_field")));
		Objects.requireNonNull(spawnAlgo);
		boolean playerDmgOnly = GsonHelper.getAsBoolean(obj, "player_damage_only", false);
		boolean allowDiscarding = GsonHelper.getAsBoolean(obj, "allow_discarding", false);
		return new Gateway(size, color, waves, rewards, failures, completionXp, spawnRange, leashRange, spawnAlgo, playerDmgOnly, allowDiscarding);
	}

	public void write(FriendlyByteBuf buf) {
		buf.writeByte(size.ordinal());
		buf.writeUtf(color.serialize());
		buf.writeVarInt(waves.size());
		waves.forEach(w -> w.write(buf));
		buf.writeVarInt(rewards.size());
		rewards.forEach(r -> r.write(buf));
		buf.writeVarInt(failures.size());
		failures.forEach(r -> r.write(buf));
		buf.writeInt(completionXp);
		buf.writeDouble(spawnRange);
		buf.writeDouble(leashRange);
		buf.writeResourceLocation(SpawnAlgorithms.NAMED_ALGORITHMS.inverse().get(this.spawnAlgo));
		buf.writeByte(this.playerDamageOnly ? 1 : 0);
		buf.writeByte(this.allowDiscarding ? 1 : 0);
	}

	public SpawnAlgorithm getSpawnAlgo() {
		return this.spawnAlgo;
	}

	public boolean playerDamageOnly() {
		return this.playerDamageOnly;
	}

	public boolean allowsDiscarding() {
		return this.allowDiscarding;
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
		int nFailures = buf.readVarInt();
		List<Failure> failures = new ArrayList<>(nFailures);
		for (int i = 0; i < nFailures; i++) {
			failures.add(Failure.read(buf));
		}
		int completionXp = buf.readInt();
		double spawnRange = buf.readDouble();
		double leashRange = buf.readDouble();
		SpawnAlgorithm spawnAlgo = SpawnAlgorithms.NAMED_ALGORITHMS.get(buf.readResourceLocation());
		boolean playerDmgOnly = buf.readByte() == 1;
		boolean allowDiscarding = buf.readByte() == 1;
		return new Gateway(size, color, waves, rewards, failures, completionXp, spawnRange, leashRange, spawnAlgo, playerDmgOnly, allowDiscarding);
	}

	public double getLeashRangeSq() {
		return this.leashRange * this.leashRange;
	}
}
