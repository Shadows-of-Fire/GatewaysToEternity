package shadows.gateways.gate;

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
import net.minecraft.network.chat.TranslatableComponent;
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
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.registries.ForgeRegistries;
import shadows.gateways.entity.GatewayEntity;
import shadows.placebo.json.ItemAdapter;
import shadows.placebo.json.SerializerBuilder;

/**
 * A Reward is provided when a gateway wave is finished.
 */
public interface Reward {

	public static final Method DROP_LOOT = ObfuscationReflectionHelper.findMethod(LivingEntity.class, "dropFromLootTable", DamageSource.class, boolean.class);
	public static final Map<String, SerializerBuilder<? extends Reward>.Serializer> SERIALIZERS = new HashMap<>();

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

	public static Reward read(JsonElement json) {
		JsonObject obj = json.getAsJsonObject();
		String type = GsonHelper.getAsString(obj, "type");
		SerializerBuilder<? extends Reward>.Serializer serializer = SERIALIZERS.get(type);
		if (serializer == null) throw new JsonSyntaxException("Unknown Reward Type: " + type);
		return serializer.deserialize(obj);
	}

	public static Reward read(FriendlyByteBuf buf) {
		String type = buf.readUtf();
		SerializerBuilder<? extends Reward>.Serializer serializer = SERIALIZERS.get(type);
		if (serializer == null) throw new JsonSyntaxException("Unknown Reward Type: " + type);
		return serializer.deserialize(buf);
	}

	public static void initSerializers() {
		SERIALIZERS.put("stack", new SerializerBuilder<StackReward>("Stack Reward").json(StackReward::read, StackReward::write).net(StackReward::read, StackReward::write).build(true));
		SERIALIZERS.put("stack_list", new SerializerBuilder<StackListReward>("Stack List Reward").json(StackListReward::read, StackListReward::write).net(StackListReward::read, StackListReward::write).build(true));
		SERIALIZERS.put("entity_loot", new SerializerBuilder<EntityLootReward>("Entity Loot Reward").json(EntityLootReward::read, EntityLootReward::write).net(EntityLootReward::read, EntityLootReward::write).build(true));
		SERIALIZERS.put("loot_table", new SerializerBuilder<LootTableReward>("Entity Loot Reward").json(LootTableReward::read, LootTableReward::write).net(LootTableReward::read, LootTableReward::write).build(true));
		SERIALIZERS.put("chanced", new SerializerBuilder<ChancedReward>("Chanced Reward").json(ChancedReward::read, ChancedReward::write).net(ChancedReward::read, ChancedReward::write).build(true));
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
			list.accept(new TranslatableComponent("%sx %s", this.stack.getCount(), this.stack.getDisplayName()));
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
				list.accept(new TranslatableComponent("%sx %s", stack.getCount(), stack.getDisplayName()));
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
				Entity entity = type.create(level);
				if (entity == null) return;
				if (nbt != null) entity.load(nbt);
				entity.moveTo(summoner.getX(), summoner.getY(), summoner.getZ(), 0, 0);
				List<ItemEntity> items = new ArrayList<>();
				entity.hurt(DamageSource.playerAttack(summoner).bypassMagic().bypassInvul().bypassArmor(), 1);
				entity.captureDrops(items);

				for (int i = 0; i < rolls; i++) {
					DROP_LOOT.invoke(entity, DamageSource.playerAttack(summoner), true);
				}

				items.stream().map(ItemEntity::getItem).forEach(list);
				entity.remove(RemovalReason.DISCARDED);
				System.out.println("Spawning " + rolls + " entity loot drops!");
			} catch (Exception e) {
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
			EntityType<?> type = ForgeRegistries.ENTITIES.getValue(new ResourceLocation(obj.get("entity").getAsString()));
			CompoundTag tag = obj.has("nbt") ? ItemAdapter.ITEM_READER.fromJson(obj.get("nbt"), CompoundTag.class) : null;
			int rolls = GsonHelper.getAsInt(obj, "rolls");
			return new EntityLootReward(type, tag, rolls);
		}

		@Override
		public void write(FriendlyByteBuf buf) {
			Reward.super.write(buf);
			buf.writeRegistryId(type);
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
			list.accept(new TranslatableComponent("%sx %s Loot", rolls, new TranslatableComponent(type.getDescriptionId())));
		}
	}

	/**
	 * Provides a roll of a single loot table as a reward.
	 */
	public static record LootTableReward(ResourceLocation table) implements Reward {

		@Override
		public void generateLoot(ServerLevel level, GatewayEntity gate, Player summoner, Consumer<ItemStack> list) {
			LootContext.Builder ctx = new LootContext.Builder(level).withParameter(LootContextParams.ORIGIN, gate.getPosition(1)).withOptionalRandomSeed(gate.tickCount);
			ctx.withLuck(summoner.getLuck()).withParameter(LootContextParams.THIS_ENTITY, summoner);
			level.getServer().getLootTables().get(table).getRandomItems(ctx.create(LootContextParamSets.CHEST)).forEach(list);
		}

		@Override
		public JsonObject write() {
			JsonObject obj = Reward.super.write();
			obj.addProperty("loot_table", table.toString());
			return obj;
		}

		public static LootTableReward read(JsonObject obj) {
			return new LootTableReward(new ResourceLocation(GsonHelper.getAsString(obj, "loot_table")));
		}

		@Override
		public void write(FriendlyByteBuf buf) {
			Reward.super.write(buf);
			buf.writeResourceLocation(table);
		}

		public static LootTableReward read(FriendlyByteBuf buf) {
			return new LootTableReward(buf.readResourceLocation());
		}

		@Override
		public String getName() {
			return "loot_table";
		}

		@Override
		public void appendHoverText(Consumer<Component> list) {
			list.accept(new TranslatableComponent("Loot Table: %s", table));
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
				list.accept(new TranslatableComponent("%s Chance of %s", fmt.format(chance), c));
			});
		}
	}

}