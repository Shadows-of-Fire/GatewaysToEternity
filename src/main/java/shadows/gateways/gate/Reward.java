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

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootContext;
import net.minecraft.loot.LootParameterSets;
import net.minecraft.loot.LootParameters;
import net.minecraft.loot.LootTable;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.registries.ForgeRegistries;
import shadows.gateways.entity.GatewayEntity;
import shadows.gateways.misc.GSerialBuilder;
import shadows.placebo.json.SerializerBuilder;
import shadows.placebo.util.json.ItemAdapter;

/**
 * A Reward is provided when a gateway wave is finished.
 */
public interface Reward {

	/**
	 * 	Method ref to public net.minecraft.world.entity.LivingEntity m_7625_(Lnet/minecraft/world/damagesource/DamageSource;Z)V # dropFromLootTable
	 */
	public static final Method dropFromLootTable = ObfuscationReflectionHelper.findMethod(LivingEntity.class, "func_213354_a", DamageSource.class, boolean.class);
	public static final MethodHandle DROP_LOOT = lootMethodHandle();

	public static final Map<String, SerializerBuilder<? extends Reward>.Serializer> SERIALIZERS = new HashMap<>();

	/**
	 * Called when this reward is to be granted to the player, either on gate or wave completion.
	 * @param level The level the gateway is in
	 * @param gate The gateway entity
	 * @param summoner The summoning player
	 * @param list When generating item rewards, add them to this list instead of directly to the player or the world.
	 */
	public void generateLoot(ServerWorld level, GatewayEntity gate, PlayerEntity summoner, Consumer<ItemStack> list);

	default JsonObject write() {
		JsonObject obj = new JsonObject();
		obj.addProperty("type", getName());
		return obj;
	}

	default void write(PacketBuffer buf) {
		buf.writeUtf(getName());
	}

	public String getName();

	public void appendHoverText(Consumer<ITextComponent> list);

	public static Reward read(JsonElement json) {
		JsonObject obj = json.getAsJsonObject();
		String type = JSONUtils.getAsString(obj, "type");
		SerializerBuilder<? extends Reward>.Serializer serializer = SERIALIZERS.get(type);
		if (serializer == null) throw new JsonSyntaxException("Unknown Reward Type: " + type);
		return serializer.deserialize(obj);
	}

	public static Reward read(PacketBuffer buf) {
		String type = buf.readUtf();
		SerializerBuilder<? extends Reward>.Serializer serializer = SERIALIZERS.get(type);
		if (serializer == null) throw new JsonSyntaxException("Unknown Reward Type: " + type);
		return serializer.deserialize(buf);
	}

	public static void initSerializers() {
		SERIALIZERS.put("stack", new GSerialBuilder<StackReward>("Stack Reward").json(StackReward::read, StackReward::write).net(StackReward::read, StackReward::write).build(true));
		SERIALIZERS.put("stack_list", new GSerialBuilder<StackListReward>("Stack List Reward").json(StackListReward::read, StackListReward::write).net(StackListReward::read, StackListReward::write).build(true));
		SERIALIZERS.put("entity_loot", new GSerialBuilder<EntityLootReward>("Entity Loot Reward").json(EntityLootReward::read, EntityLootReward::write).net(EntityLootReward::read, EntityLootReward::write).build(true));
		SERIALIZERS.put("loot_table", new GSerialBuilder<LootTableReward>("Entity Loot Reward").json(LootTableReward::read, LootTableReward::write).net(LootTableReward::read, LootTableReward::write).build(true));
		SERIALIZERS.put("chanced", new GSerialBuilder<ChancedReward>("Chanced Reward").json(ChancedReward::read, ChancedReward::write).net(ChancedReward::read, ChancedReward::write).build(true));
		SERIALIZERS.put("command", new GSerialBuilder<CommandReward>("Command Reward").json(CommandReward::read, CommandReward::write).net(CommandReward::read, CommandReward::write).build(true));
	}

	static MethodHandle lootMethodHandle() {
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
			return Reward.read(json);
		}

	}

	/**
	 * Provides a single stack as a reward.
	 */
	public static class StackReward implements Reward {

		private final ItemStack stack;

		public StackReward(ItemStack stack) {
			this.stack = stack;
		}

		@Override
		public void generateLoot(ServerWorld level, GatewayEntity gate, PlayerEntity summoner, Consumer<ItemStack> list) {
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
		public void write(PacketBuffer buf) {
			Reward.super.write(buf);
			buf.writeItem(stack);
		}

		public static StackReward read(PacketBuffer buf) {
			return new StackReward(buf.readItem());
		}

		@Override
		public String getName() {
			return "stack";
		}

		@Override
		public void appendHoverText(Consumer<ITextComponent> list) {
			list.accept(new TranslationTextComponent("reward.gateways.stack", this.stack.getCount(), this.stack.getDisplayName()));
		}
	}

	/**
	 * Provides a list of stacks as a reward.
	 */
	public static class StackListReward implements Reward {

		private final NonNullList<ItemStack> stacks;

		public StackListReward(NonNullList<ItemStack> stacks) {
			this.stacks = stacks;
		}

		@Override
		public void generateLoot(ServerWorld level, GatewayEntity gate, PlayerEntity summoner, Consumer<ItemStack> list) {
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
		public void write(PacketBuffer buf) {
			Reward.super.write(buf);
			buf.writeVarInt(stacks.size());
			stacks.forEach(buf::writeItem);
		}

		public static StackListReward read(PacketBuffer buf) {
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
		public void appendHoverText(Consumer<ITextComponent> list) {
			for (ItemStack stack : this.stacks) {
				list.accept(new TranslationTextComponent("reward.gateways.stack", stack.getCount(), stack.getDisplayName()));
			}
		}
	}

	/**
	 * Provides multiple rolls of an entity's loot table as a reward.
	 */
	public static class EntityLootReward implements Reward {

		private final EntityType<?> type;
		private final @Nullable CompoundNBT nbt;
		private final int rolls;

		public EntityLootReward(EntityType<?> type, @Nullable CompoundNBT nbt, int rolls) {
			this.type = type;
			this.nbt = nbt;
			this.rolls = rolls;
		}

		@Override
		public void generateLoot(ServerWorld level, GatewayEntity gate, PlayerEntity summoner, Consumer<ItemStack> list) {
			try {
				List<ItemEntity> items = new ArrayList<>();

				Entity entity = type.create(level);
				for (int i = 0; i < rolls; i++) {
					if (nbt != null) entity.load(nbt);
					entity.moveTo(summoner.getX(), summoner.getY(), summoner.getZ(), 0, 0);
					entity.hurt(DamageSource.playerAttack(summoner).bypassMagic().bypassInvul().bypassArmor(), 1);
					entity.captureDrops(items);
					DROP_LOOT.invoke(entity, DamageSource.playerAttack(summoner), true);
					entity.remove(false);
				}

				items.stream().map(ItemEntity::getItem).forEach(list);
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}

		@Override
		public JsonObject write() {
			JsonObject obj = Reward.super.write();
			obj.addProperty("entity", type.getRegistryName().toString());
			if (nbt != null) obj.add("nbt", ItemAdapter.ITEM_READER.toJsonTree(nbt));
			obj.addProperty("rolls", rolls);
			return obj;
		}

		public static EntityLootReward read(JsonObject obj) {
			EntityType<?> type = ForgeRegistries.ENTITIES.getValue(new ResourceLocation(JSONUtils.getAsString(obj, "entity")));
			CompoundNBT tag = obj.has("nbt") ? ItemAdapter.ITEM_READER.fromJson(obj.get("nbt"), CompoundNBT.class) : null;
			int rolls = JSONUtils.getAsInt(obj, "rolls");
			return new EntityLootReward(type, tag, rolls);
		}

		@Override
		public void write(PacketBuffer buf) {
			Reward.super.write(buf);
			buf.writeRegistryId(type);
			buf.writeNbt(nbt == null ? new CompoundNBT() : nbt);
			buf.writeVarInt(rolls);
		}

		public static EntityLootReward read(PacketBuffer buf) {
			EntityType<?> type = buf.readRegistryIdSafe(EntityType.class);
			CompoundNBT tag = buf.readNbt();
			int rolls = buf.readVarInt();
			return new EntityLootReward(type, tag, rolls);
		}

		@Override
		public String getName() {
			return "entity_loot";
		}

		@Override
		public void appendHoverText(Consumer<ITextComponent> list) {
			list.accept(new TranslationTextComponent("reward.gateways.entity", rolls, new TranslationTextComponent(type.getDescriptionId())));
		}
	}

	/**
	 * Provides a roll of a single loot table as a reward.
	 */
	public static class LootTableReward implements Reward {

		private final ResourceLocation table;
		private final int rolls;

		public LootTableReward(ResourceLocation table, int rolls) {
			this.table = table;
			this.rolls = rolls;
		}

		@Override
		public void generateLoot(ServerWorld level, GatewayEntity gate, PlayerEntity summoner, Consumer<ItemStack> list) {
			LootTable realTable = level.getServer().getLootTables().get(table);
			for (int i = 0; i < rolls; i++) {
				LootContext.Builder ctx = new LootContext.Builder(level).withParameter(LootParameters.ORIGIN, gate.getPosition(1)).withOptionalRandomSeed(gate.tickCount + i);
				ctx.withLuck(summoner.getLuck()).withParameter(LootParameters.THIS_ENTITY, summoner).withParameter(LootParameters.TOOL, summoner.getMainHandItem());
				realTable.getRandomItems(ctx.create(LootParameterSets.CHEST)).forEach(list);
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
			return new LootTableReward(new ResourceLocation(JSONUtils.getAsString(obj, "loot_table")), JSONUtils.getAsInt(obj, "rolls"));
		}

		@Override
		public void write(PacketBuffer buf) {
			Reward.super.write(buf);
			buf.writeResourceLocation(table);
			buf.writeInt(rolls);
		}

		public static LootTableReward read(PacketBuffer buf) {
			return new LootTableReward(buf.readResourceLocation(), buf.readInt());
		}

		@Override
		public String getName() {
			return "loot_table";
		}

		@Override
		public void appendHoverText(Consumer<ITextComponent> list) {
			list.accept(new TranslationTextComponent("reward.gateways.loot_table", rolls, table));
		}
	}

	/**
	 * Wraps a reward with a random chance applied to it.
	 */
	public static class ChancedReward implements Reward {

		private final Reward reward;
		private final float chance;

		public ChancedReward(Reward reward, float chance) {
			this.reward = reward;
			this.chance = chance;
		}

		@Override
		public void generateLoot(ServerWorld level, GatewayEntity gate, PlayerEntity summoner, Consumer<ItemStack> list) {
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
			float chance = JSONUtils.getAsFloat(obj, "chance");
			Reward reward = Reward.read(JSONUtils.getAsJsonObject(obj, "reward"));
			return new ChancedReward(reward, chance);
		}

		@Override
		public void write(PacketBuffer buf) {
			Reward.super.write(buf);
			buf.writeFloat(chance);
			reward.write(buf);
		}

		public static ChancedReward read(PacketBuffer buf) {
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
		public void appendHoverText(Consumer<ITextComponent> list) {
			this.reward.appendHoverText(c -> {
				list.accept(new TranslationTextComponent("reward.gateways.chance", fmt.format(chance * 100), c));
			});
		}
	}

	/**
	 * Provides a roll of a single loot table as a reward.
	 */
	public static class CommandReward implements Reward {

		private final String command;

		CommandReward(String command) {
			this.command = command;
		}

		@Override
		public void generateLoot(ServerWorld level, GatewayEntity gate, PlayerEntity summoner, Consumer<ItemStack> list) {
			String realCmd = command.replace("<summoner>", summoner.getGameProfile().getName());
			level.getServer().getCommands().performCommand(gate.createCommandSourceStack(), realCmd);
		}

		@Override
		public JsonObject write() {
			JsonObject obj = Reward.super.write();
			obj.addProperty("command", this.command);
			return obj;
		}

		public static CommandReward read(JsonObject obj) {
			return new CommandReward(JSONUtils.getAsString(obj, "command"));
		}

		@Override
		public void write(PacketBuffer buf) {
			Reward.super.write(buf);
			buf.writeUtf(this.command);
		}

		public static CommandReward read(PacketBuffer buf) {
			return new CommandReward(buf.readUtf());
		}

		@Override
		public String getName() {
			return "command";
		}

		@Override
		public void appendHoverText(Consumer<ITextComponent> list) {
			list.accept(new TranslationTextComponent("reward.gateways.command", command));
		}
	}

}