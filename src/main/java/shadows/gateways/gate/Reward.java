package shadows.gateways.gate;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
import com.google.gson.reflect.TypeToken;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.registries.ForgeRegistries;
import shadows.gateways.entity.GatewayEntity;
import shadows.placebo.json.ItemAdapter;
import shadows.placebo.json.PSerializer;

/**
 * A Reward is provided when a gateway wave is finished.
 */
public interface Reward {

	/**
	 * 	Method ref to public net.minecraft.world.entity.LivingEntity m_7625_(Lnet/minecraft/world/damagesource/DamageSource;Z)V # dropFromLootTable
	 */
	public static final Method dropFromLootTable = ObfuscationReflectionHelper.findMethod(LivingEntity.class, "m_7625_", DamageSource.class, boolean.class);
	public static final MethodHandle DROP_LOOT = lootMethodHandle();

	public static final Map<String, PSerializer<? extends Reward>> SERIALIZERS = new HashMap<>();

	/**
	 * Called when this reward is to be granted to the player, either on gate or wave completion.
	 * @param level The level the gateway is in
	 * @param gate The gateway entity
	 * @param summoner The summoning player
	 * @param list When generating item rewards, add them to this list instead of directly to the player or the world.
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

	public static Reward read(JsonObject obj) {
		String type = GsonHelper.getAsString(obj, "type");
		PSerializer<? extends Reward> serializer = SERIALIZERS.get(type);
		if (serializer == null) throw new JsonSyntaxException("Unknown Reward Type: " + type);
		return serializer.read(obj);
	}

	public static Reward read(FriendlyByteBuf buf) {
		String type = buf.readUtf();
		PSerializer<? extends Reward> serializer = SERIALIZERS.get(type);
		if (serializer == null) throw new JsonSyntaxException("Unknown Reward Type: " + type);
		return serializer.read(buf);
	}

	public static void initSerializers() {
		SERIALIZERS.put("stack", PSerializer.autoRegister("Stack Reward", StackReward.class).build(true));
		SERIALIZERS.put("stack_list", PSerializer.autoRegister("Stack List Reward", StackListReward.class).build(true));
		SERIALIZERS.put("entity_loot", PSerializer.autoRegister("Entity Loot Reward", EntityLootReward.class).build(true));
		SERIALIZERS.put("loot_table", PSerializer.autoRegister("Loot Table Reward", LootTableReward.class).build(true));
		SERIALIZERS.put("chanced", PSerializer.autoRegister("Chanced Reward", ChancedReward.class).build(true));
		SERIALIZERS.put("command", PSerializer.autoRegister("Command Reward", CommandReward.class).build(true));
	}

	private static MethodHandle lootMethodHandle() {
		dropFromLootTable.setAccessible(true);
		try {
			return MethodHandles.lookup().unreflect(dropFromLootTable);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	public static class Serializer implements JsonDeserializer<Reward>, JsonSerializer<Reward> {

		@Override
		public JsonElement serialize(Reward src, Type typeOfSrc, JsonSerializationContext context) {
			return src.write();
		}

		@Override
		public Reward deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			return Reward.read(json.getAsJsonObject());
		}

	}

	/**
	 * Provides a single stack as a reward.
	 */
	public static record StackReward(ItemStack stack) implements Reward {

		@Override
		public void generateLoot(ServerLevel level, GatewayEntity gate, Player summoner, Consumer<ItemStack> list) {
			list.accept(stack.copy());
		}

		@Override
		public JsonObject write() {
			JsonObject obj = Reward.super.write();
			obj.add("stack", ItemAdapter.ITEM_READER.toJsonTree(stack));
			return obj;
		}

		public static StackReward read(JsonObject obj) {
			return new StackReward(ItemAdapter.ITEM_READER.fromJson(obj.get("stack"), ItemStack.class));
		}

		@Override
		public void write(FriendlyByteBuf buf) {
			Reward.super.write(buf);
			buf.writeItem(stack);
		}

		public static StackReward read(FriendlyByteBuf buf) {
			return new StackReward(buf.readItem());
		}

		@Override
		public String getName() {
			return "stack";
		}

		@Override
		public void appendHoverText(Consumer<Component> list) {
			list.accept(Component.translatable("reward.gateways.stack", this.stack.getCount(), this.stack.getDisplayName()));
		}
	}

	/**
	 * Provides a list of stacks as a reward.
	 */
	public static record StackListReward(NonNullList<ItemStack> stacks) implements Reward {

		@Override
		public void generateLoot(ServerLevel level, GatewayEntity gate, Player summoner, Consumer<ItemStack> list) {
			stacks.forEach(s -> list.accept(s.copy()));
		}

		@Override
		public JsonObject write() {
			JsonObject obj = Reward.super.write();
			obj.add("stacks", ItemAdapter.ITEM_READER.toJsonTree(stacks));
			return obj;
		};

		public static StackListReward read(JsonObject obj) {
			return new StackListReward(ItemAdapter.ITEM_READER.fromJson(obj.get("stacks"), new TypeToken<List<ItemStack>>() {
			}.getType()));
		}

		@Override
		public void write(FriendlyByteBuf buf) {
			Reward.super.write(buf);
			buf.writeVarInt(stacks.size());
			stacks.forEach(buf::writeItem);
		}

		public static StackListReward read(FriendlyByteBuf buf) {
			NonNullList<ItemStack> stacks = NonNullList.withSize(buf.readVarInt(), ItemStack.EMPTY);
			for (int i = 0; i < stacks.size(); i++) {
				stacks.set(i, buf.readItem());
			}
			return new StackListReward(stacks);
		}

		@Override
		public String getName() {
			return "stack_list";
		}

		@Override
		public void appendHoverText(Consumer<Component> list) {
			for (ItemStack stack : this.stacks) {
				list.accept(Component.translatable("reward.gateways.stack", stack.getCount(), stack.getDisplayName()));
			}
		}
	}

	/**
	 * Provides multiple rolls of an entity's loot table as a reward.
	 */
	public static record EntityLootReward(EntityType<?> type, @Nullable CompoundTag nbt, int rolls) implements Reward {

		@Override
		public void generateLoot(ServerLevel level, GatewayEntity gate, Player summoner, Consumer<ItemStack> list) {
			try {
				List<ItemEntity> items = new ArrayList<>();

				Entity entity = type.create(level);
				for (int i = 0; i < rolls; i++) {
					if (nbt != null) entity.load(nbt);
					entity.moveTo(summoner.getX(), summoner.getY(), summoner.getZ(), 0, 0);
					entity.hurt(DamageSource.playerAttack(summoner).bypassMagic().bypassInvul().bypassArmor(), 1);
					entity.captureDrops(items);
					DROP_LOOT.invoke(entity, DamageSource.playerAttack(summoner), true);
					entity.remove(RemovalReason.DISCARDED);
				}

				items.stream().map(ItemEntity::getItem).forEach(list);
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}

		@Override
		public JsonObject write() {
			JsonObject obj = Reward.super.write();
			obj.addProperty("entity", ForgeRegistries.ENTITY_TYPES.getKey(type).toString());
			if (nbt != null) obj.add("nbt", ItemAdapter.ITEM_READER.toJsonTree(nbt));
			obj.addProperty("rolls", rolls);
			return obj;
		}

		public static EntityLootReward read(JsonObject obj) {
			EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(obj.get("entity").getAsString()));
			CompoundTag tag = obj.has("nbt") ? ItemAdapter.ITEM_READER.fromJson(obj.get("nbt"), CompoundTag.class) : null;
			int rolls = GsonHelper.getAsInt(obj, "rolls");
			return new EntityLootReward(type, tag, rolls);
		}

		@Override
		public void write(FriendlyByteBuf buf) {
			Reward.super.write(buf);
			buf.writeRegistryId(ForgeRegistries.ENTITY_TYPES, type);
			buf.writeNbt(nbt == null ? new CompoundTag() : nbt);
			buf.writeVarInt(rolls);
		}

		public static EntityLootReward read(FriendlyByteBuf buf) {
			EntityType<?> type = buf.readRegistryIdSafe(EntityType.class);
			CompoundTag tag = buf.readNbt();
			int rolls = buf.readVarInt();
			return new EntityLootReward(type, tag, rolls);
		}

		@Override
		public String getName() {
			return "entity_loot";
		}

		@Override
		public void appendHoverText(Consumer<Component> list) {
			list.accept(Component.translatable("reward.gateways.entity", rolls, Component.translatable(type.getDescriptionId())));
		}
	}

	/**
	 * Provides a roll of a single loot table as a reward.
	 */
	public static record LootTableReward(ResourceLocation table, int rolls) implements Reward {

		@Override
		public void generateLoot(ServerLevel level, GatewayEntity gate, Player summoner, Consumer<ItemStack> list) {
			LootTable realTable = level.getServer().getLootTables().get(table);
			for (int i = 0; i < rolls; i++) {
				LootContext.Builder ctx = new LootContext.Builder(level).withParameter(LootContextParams.ORIGIN, gate.getPosition(1)).withOptionalRandomSeed(gate.tickCount + i);
				ctx.withLuck(summoner.getLuck()).withParameter(LootContextParams.THIS_ENTITY, summoner).withParameter(LootContextParams.TOOL, summoner.getMainHandItem());
				realTable.getRandomItems(ctx.create(LootContextParamSets.CHEST)).forEach(list);
			}
		}

		@Override
		public JsonObject write() {
			JsonObject obj = Reward.super.write();
			obj.addProperty("loot_table", table.toString());
			obj.addProperty("rolls", rolls);
			return obj;
		}

		public static LootTableReward read(JsonObject obj) {
			return new LootTableReward(new ResourceLocation(GsonHelper.getAsString(obj, "loot_table")), GsonHelper.getAsInt(obj, "rolls"));
		}

		@Override
		public void write(FriendlyByteBuf buf) {
			Reward.super.write(buf);
			buf.writeResourceLocation(table);
			buf.writeInt(rolls);
		}

		public static LootTableReward read(FriendlyByteBuf buf) {
			return new LootTableReward(buf.readResourceLocation(), buf.readInt());
		}

		@Override
		public String getName() {
			return "loot_table";
		}

		@Override
		public void appendHoverText(Consumer<Component> list) {
			list.accept(Component.translatable("reward.gateways.loot_table", rolls, table));
		}
	}

	/**
	 * Wraps a reward with a random chance applied to it.
	 */
	public static record ChancedReward(Reward reward, float chance) implements Reward {

		@Override
		public void generateLoot(ServerLevel level, GatewayEntity gate, Player summoner, Consumer<ItemStack> list) {
			if (level.random.nextFloat() < chance) reward.generateLoot(level, gate, summoner, list);
		}

		@Override
		public JsonObject write() {
			JsonObject obj = Reward.super.write();
			obj.addProperty("chance", chance);
			obj.add("reward", reward.write());
			return obj;
		}

		public static ChancedReward read(JsonObject obj) {
			float chance = GsonHelper.getAsFloat(obj, "chance");
			Reward reward = Reward.read(GsonHelper.getAsJsonObject(obj, "reward"));
			return new ChancedReward(reward, chance);
		}

		@Override
		public void write(FriendlyByteBuf buf) {
			Reward.super.write(buf);
			buf.writeFloat(chance);
			reward.write(buf);
		}

		public static ChancedReward read(FriendlyByteBuf buf) {
			float chance = buf.readFloat();
			Reward reward = Reward.read(buf);
			return new ChancedReward(reward, chance);
		}

		@Override
		public String getName() {
			return "chanced";
		}

		static DecimalFormat fmt = new DecimalFormat("##.##%");

		@Override
		public void appendHoverText(Consumer<Component> list) {
			this.reward.appendHoverText(c -> {
				list.accept(Component.translatable("reward.gateways.chance", fmt.format(chance * 100), c));
			});
		}
	}

	/**
	 * Provides a roll of a single loot table as a reward.
	 */
	public static record CommandReward(String command) implements Reward {

		@Override
		public void generateLoot(ServerLevel level, GatewayEntity gate, Player summoner, Consumer<ItemStack> list) {
			String realCmd = command.replace("<summoner>", summoner.getGameProfile().getName());
			level.getServer().getCommands().performPrefixedCommand(gate.createCommandSourceStack(), realCmd);
		}

		@Override
		public JsonObject write() {
			JsonObject obj = Reward.super.write();
			obj.addProperty("command", this.command);
			return obj;
		}

		public static CommandReward read(JsonObject obj) {
			return new CommandReward(GsonHelper.getAsString(obj, "command"));
		}

		@Override
		public void write(FriendlyByteBuf buf) {
			Reward.super.write(buf);
			buf.writeUtf(this.command);
		}

		public static CommandReward read(FriendlyByteBuf buf) {
			return new CommandReward(buf.readUtf());
		}

		@Override
		public String getName() {
			return "command";
		}

		@Override
		public void appendHoverText(Consumer<Component> list) {
			list.accept(Component.translatable("reward.gateways.command", command));
		}
	}

}