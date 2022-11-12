package shadows.gateways.gate;

import java.lang.reflect.Type;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSyntaxException;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion.BlockInteraction;
import net.minecraftforge.registries.ForgeRegistries;
import shadows.gateways.entity.GatewayEntity;
import shadows.gateways.entity.GatewayEntity.FailureReason;
import shadows.placebo.json.ItemAdapter;
import shadows.placebo.json.JsonUtil;
import shadows.placebo.json.PSerializer;

/**
 * A Failure is a negative effect that triggers when a gateway errors for some reason.
 */
public interface Failure {

	public static final Map<String, PSerializer<? extends Failure>> SERIALIZERS = new HashMap<>();

	/**
	 * Called when this failure is to be applied.
	 * @param level The level the gateway is in
	 * @param gate The gateway entity
	 * @param summoner The summoning player
	 * @param reason The reason the failure happened
	 */
	public void onFailure(ServerLevel level, GatewayEntity gate, Player summoner, FailureReason reason);

	default JsonObject write() {
		JsonObject obj = new JsonObject();
		obj.addProperty("type", getName());
		return obj;
	}

	default void write(FriendlyByteBuf buf) {
		buf.writeUtf(getName());
	}

	public String getName();

	public void appendHoverText(Consumer<Component> list);

	public static Failure read(JsonObject obj) {
		String type = GsonHelper.getAsString(obj, "type");
		PSerializer<? extends Failure> serializer = SERIALIZERS.get(type);
		if (serializer == null) throw new JsonSyntaxException("Unknown Failure Type: " + type);
		return serializer.read(obj);
	}

	public static Failure read(FriendlyByteBuf buf) {
		String type = buf.readUtf();
		PSerializer<? extends Failure> serializer = SERIALIZERS.get(type);
		if (serializer == null) throw new JsonSyntaxException("Unknown Failure Type: " + type);
		return serializer.read(buf);
	}

	public static void initSerializers() {
		SERIALIZERS.put("explosion", PSerializer.autoRegister("Explosion Failure", ExplosionFailure.class).build(true));
		SERIALIZERS.put("mob_effect", PSerializer.autoRegister("Mob Effect Failure", MobEffectFailure.class).build(true));
		SERIALIZERS.put("summon", PSerializer.autoRegister("Summon Failure", SummonFailure.class).build(true));
		SERIALIZERS.put("chanced", PSerializer.autoRegister("Chanced Failure", ChancedFailure.class).build(true));
		SERIALIZERS.put("command", PSerializer.autoRegister("Command Failure", CommandFailure.class).build(true));
	}

	public static class Serializer implements JsonDeserializer<Failure>, JsonSerializer<Failure> {

		@Override
		public JsonElement serialize(Failure src, Type typeOfSrc, JsonSerializationContext context) {
			return src.write();
		}

		@Override
		public Failure deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			return Failure.read(json.getAsJsonObject());
		}

	}

	/**
	 * Provides a single stack as a reward.
	 */
	public static record ExplosionFailure(float strength, boolean fire, boolean blockDamage) implements Failure {

		@Override
		public void onFailure(ServerLevel level, GatewayEntity gate, Player summoner, FailureReason reason) {
			level.explode(gate, gate.getX(), gate.getY(), gate.getZ(), strength, fire, blockDamage ? BlockInteraction.DESTROY : BlockInteraction.NONE);
		}

		@Override
		public JsonObject write() {
			JsonObject obj = Failure.super.write();
			obj.addProperty("strength", this.strength);
			obj.addProperty("fire", this.fire);
			obj.addProperty("block_damage", this.blockDamage);
			return obj;
		}

		public static ExplosionFailure read(JsonObject obj) {
			return new ExplosionFailure(GsonHelper.getAsFloat(obj, "strength"), GsonHelper.getAsBoolean(obj, "fire", true), GsonHelper.getAsBoolean(obj, "block_damage", true));
		}

		@Override
		public void write(FriendlyByteBuf buf) {
			Failure.super.write(buf);
			buf.writeFloat(this.strength);
			buf.writeBoolean(this.fire);
			buf.writeBoolean(this.blockDamage);
		}

		public static ExplosionFailure read(FriendlyByteBuf buf) {
			return new ExplosionFailure(buf.readFloat(), buf.readBoolean(), buf.readBoolean());
		}

		@Override
		public String getName() {
			return "explosion";
		}

		@Override
		public void appendHoverText(Consumer<Component> list) {
			list.accept(Component.translatable("failure.gateways.explosion", this.strength, this.fire, this.blockDamage));
		}
	}

	/**
	 * Provides a list of stacks as a reward.
	 */
	public static record MobEffectFailure(MobEffect effect, int duration, int amplifier) implements Failure {

		@Override
		public void onFailure(ServerLevel level, GatewayEntity gate, Player summoner, FailureReason reason) {
			level.getNearbyPlayers(TargetingConditions.forNonCombat(), null, gate.getBoundingBox().inflate(gate.getGateway().leashRange)).forEach(p -> {
				p.addEffect(new MobEffectInstance(effect, duration, amplifier));
			});
		}

		@Override
		public JsonObject write() {
			JsonObject obj = Failure.super.write();
			obj.addProperty("effect", ForgeRegistries.MOB_EFFECTS.getKey(effect).toString());
			obj.addProperty("duration", duration);
			obj.addProperty("amplifier", amplifier);
			return obj;
		};

		public static MobEffectFailure read(JsonObject obj) {
			return new MobEffectFailure(JsonUtil.getRegistryObject(obj, "effect", ForgeRegistries.MOB_EFFECTS), GsonHelper.getAsInt(obj, "duration"), GsonHelper.getAsInt(obj, "amplifier", 0));
		}

		@Override
		public void write(FriendlyByteBuf buf) {
			Failure.super.write(buf);
			buf.writeRegistryId(ForgeRegistries.MOB_EFFECTS, this.effect);
			buf.writeInt(this.duration);
			buf.writeInt(this.amplifier);
		}

		public static MobEffectFailure read(FriendlyByteBuf buf) {
			return new MobEffectFailure(buf.readRegistryIdSafe(MobEffect.class), buf.readInt(), buf.readInt());
		}

		@Override
		public String getName() {
			return "mob_effect";
		}

		@Override
		public void appendHoverText(Consumer<Component> list) {
			list.accept(Component.translatable("failure.gateways.mob_effect", toComponent(new MobEffectInstance(effect, duration, amplifier))));
		}

		private static Component toComponent(MobEffectInstance mobeffectinstance) {
			MutableComponent mutablecomponent = Component.translatable(mobeffectinstance.getDescriptionId());
			MobEffect mobeffect = mobeffectinstance.getEffect();

			if (mobeffectinstance.getAmplifier() > 0) {
				mutablecomponent = Component.translatable("potion.withAmplifier", mutablecomponent, Component.translatable("potion.potency." + mobeffectinstance.getAmplifier()));
			}

			if (mobeffectinstance.getDuration() > 20) {
				mutablecomponent = Component.translatable("potion.withDuration", mutablecomponent, MobEffectUtil.formatDuration(mobeffectinstance, 1));
			}

			return mutablecomponent.withStyle(mobeffect.getCategory().getTooltipFormatting());
		}
	}

	/**
	 * Provides multiple rolls of an entity's loot table as a reward.
	 */
	public static record SummonFailure(EntityType<?> type, @Nullable CompoundTag nbt, int count) implements Failure {

		@Override
		public void onFailure(ServerLevel level, GatewayEntity gate, Player summoner, FailureReason reason) {
			for (int i = 0; i < count; i++) {
				Entity entity = type.create(level);
				if (nbt != null) entity.load(nbt);
				entity.moveTo(gate.getX(), gate.getY(), gate.getZ(), 0, 0);
				level.addFreshEntity(entity);
			}
		}

		@Override
		public JsonObject write() {
			JsonObject obj = Failure.super.write();
			obj.addProperty("entity", ForgeRegistries.ENTITY_TYPES.getKey(type).toString());
			if (nbt != null) obj.add("nbt", ItemAdapter.ITEM_READER.toJsonTree(nbt));
			obj.addProperty("count", count);
			return obj;
		}

		public static SummonFailure read(JsonObject obj) {
			EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(obj.get("entity").getAsString()));
			CompoundTag tag = obj.has("nbt") ? ItemAdapter.ITEM_READER.fromJson(obj.get("nbt"), CompoundTag.class) : null;
			int count = GsonHelper.getAsInt(obj, "count");
			return new SummonFailure(type, tag, count);
		}

		@Override
		public void write(FriendlyByteBuf buf) {
			Failure.super.write(buf);
			buf.writeRegistryId(ForgeRegistries.ENTITY_TYPES, type);
			buf.writeNbt(nbt == null ? new CompoundTag() : nbt);
			buf.writeVarInt(count);
		}

		public static SummonFailure read(FriendlyByteBuf buf) {
			EntityType<?> type = buf.readRegistryIdSafe(EntityType.class);
			CompoundTag tag = buf.readNbt();
			int count = buf.readVarInt();
			return new SummonFailure(type, tag, count);
		}

		@Override
		public String getName() {
			return "summon";
		}

		@Override
		public void appendHoverText(Consumer<Component> list) {
			list.accept(Component.translatable("failure.gateways.summon", count, Component.translatable(type.getDescriptionId())));
		}
	}

	/**
	 * Wraps a reward with a random chance applied to it.
	 */
	public static record ChancedFailure(Failure failure, float chance) implements Failure {

		@Override
		public void onFailure(ServerLevel level, GatewayEntity gate, Player summoner, FailureReason reason) {
			if (level.random.nextFloat() < chance) failure.onFailure(level, gate, summoner, reason);
		}

		@Override
		public JsonObject write() {
			JsonObject obj = Failure.super.write();
			obj.addProperty("chance", chance);
			obj.add("failure", failure.write());
			return obj;
		}

		public static ChancedFailure read(JsonObject obj) {
			float chance = GsonHelper.getAsFloat(obj, "chance");
			Failure reward = Failure.read(GsonHelper.getAsJsonObject(obj, "failure"));
			return new ChancedFailure(reward, chance);
		}

		@Override
		public void write(FriendlyByteBuf buf) {
			Failure.super.write(buf);
			buf.writeFloat(chance);
			failure.write(buf);
		}

		public static ChancedFailure read(FriendlyByteBuf buf) {
			float chance = buf.readFloat();
			Failure reward = Failure.read(buf);
			return new ChancedFailure(reward, chance);
		}

		@Override
		public String getName() {
			return "chanced";
		}

		static DecimalFormat fmt = new DecimalFormat("##.##%");

		@Override
		public void appendHoverText(Consumer<Component> list) {
			this.failure.appendHoverText(c -> {
				list.accept(Component.translatable("failure.gateways.chance", fmt.format(chance * 100), c));
			});
		}
	}

	/**
	 * Provides a roll of a single loot table as a reward.
	 */
	public static record CommandFailure(String command, String desc) implements Failure {

		@Override
		public void onFailure(ServerLevel level, GatewayEntity gate, Player summoner, FailureReason reason) {
			String realCmd = command.replace("<summoner>", summoner.getGameProfile().getName());
			level.getServer().getCommands().performPrefixedCommand(gate.createCommandSourceStack(), realCmd);
		}

		@Override
		public JsonObject write() {
			JsonObject obj = Failure.super.write();
			obj.addProperty("command", this.command);
			return obj;
		}

		public static CommandFailure read(JsonObject obj) {
			return new CommandFailure(GsonHelper.getAsString(obj, "command"), GsonHelper.getAsString(obj, "desc"));
		}

		@Override
		public void write(FriendlyByteBuf buf) {
			Failure.super.write(buf);
			buf.writeUtf(this.desc);
		}

		public static CommandFailure read(FriendlyByteBuf buf) {
			return new CommandFailure("", buf.readUtf());
		}

		@Override
		public String getName() {
			return "command";
		}

		@Override
		public void appendHoverText(Consumer<Component> list) {
			list.accept(Component.translatable(desc));
		}
	}

}