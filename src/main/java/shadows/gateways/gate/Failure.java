package shadows.gateways.gate;

import com.google.gson.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import shadows.gateways.entity.GatewayEntity;
import shadows.placebo.json.SerializerBuilder;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Nearly identical to {@link Reward}, however it can be left null without consequences
 */
public interface Failure
{

	public static final Map<String, SerializerBuilder<? extends Failure>.Serializer> SERIALIZERS = new HashMap<>();
	
	/**
	 * Called when the player fails to defeat all enemies before the wave ends.
	 * @param level The level the gateway is in
	 * @param gate The gateway entity
	 * @param summoner The summoning player
	 * @param list When generating items for failure, add them to this list instead of directly to the player or the world.
	 */
	public void generateLoot(ServerLevel level, GatewayEntity gate, Player summoner, Consumer<ItemStack> list);
	
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

	public static Failure read(JsonElement json) {
		JsonObject obj = json.getAsJsonObject();
		String type = GsonHelper.getAsString(obj, "type");
		SerializerBuilder<? extends Failure>.Serializer serializer = SERIALIZERS.get(type);
		if (serializer == null) throw new JsonSyntaxException("Unknown Failure Type: " + type);
		return serializer.read(obj);
	}

	public static Failure read(FriendlyByteBuf buf) {
		String type = buf.readUtf();
		SerializerBuilder<? extends Failure>.Serializer serializer = SERIALIZERS.get(type);
		if (serializer == null) throw new JsonSyntaxException("Unknown Failure Type: " + type);
		return serializer.read(buf);
	}

	public static void initSerializers() {
		SERIALIZERS.put("command", new SerializerBuilder<CommandFailure>("Command Failure").json(CommandFailure::read, CommandFailure::write).net(CommandFailure::read, CommandFailure::write).build(true));
	}

	public static class Serializer implements JsonDeserializer<Failure>, JsonSerializer<Failure> {

		@Override
		public JsonElement serialize(Failure src, Type typeOfSrc, JsonSerializationContext context) {
			return src.write();
		}

		@Override
		public Failure deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			return Failure.read(json);
		}

	}

	/**
	 * Plays a command upon failure
	 */
	public static record CommandFailure(String command) implements Failure
	{

		@Override
		public void generateLoot(ServerLevel level, GatewayEntity gate, Player summoner, Consumer<ItemStack> list) {
			String realCmd = command.replace("<summoner>", summoner.getGameProfile().getName());
			level.getServer().getCommands().performCommand(gate.createCommandSourceStack(), realCmd);
		}

		@Override
		public JsonObject write() {
			JsonObject obj = Failure.super.write();
			obj.addProperty("command", this.command);
			return obj;
		}

		public static CommandFailure read(JsonObject obj) {
			return new CommandFailure(GsonHelper.getAsString(obj, "command"));
		}

		@Override
		public void write(FriendlyByteBuf buf) {
			Failure.super.write(buf);
			buf.writeUtf(this.command);
		}

		public static CommandFailure read(FriendlyByteBuf buf) {
			return new CommandFailure(buf.readUtf());
		}

		@Override
		public String getName() {
			return "command";
		}

		@Override
		public void appendHoverText(Consumer<Component> list) {
			list.accept(new TranslatableComponent("failure.gateways.command", command));
		}
	}

}