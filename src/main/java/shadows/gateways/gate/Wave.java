package shadows.gateways.gate;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.registries.ForgeRegistries;
import shadows.gateways.GatewayObjects;
import shadows.gateways.Gateways;
import shadows.gateways.entity.GatewayEntity;
import shadows.placebo.json.ItemAdapter;
import shadows.placebo.json.JsonUtil;
import shadows.placebo.json.RandomAttributeModifier;
import shadows.placebo.util.RandomRange;

/**
 * A single wave of a gateway.
 * @param entities A list of all entities to be spawned this wave, with optional NBT for additional data.
 * @param modifiers A list of modifiers that will be applied to all spawned entities.
 * @param rewards All rewards that will be granted at the end of the wave.
 * @param maxWaveTime The time the player has to complete this wave.
 * @param setupTime The delay after this wave before the next wave starts.  Ignored if this is the last wave.
 */
public record Wave(List<Pair<EntityType<?>, @Nullable CompoundTag>> entities, List<RandomAttributeModifier> modifiers, List<Reward> rewards, int maxWaveTime, int setupTime) {

	public List<LivingEntity> spawnWave(ServerLevel level, BlockPos pos, GatewayEntity gate) {
		List<LivingEntity> spawned = new ArrayList<>();
		for (Pair<EntityType<?>, CompoundTag> toSpawn : entities) {
			EntityType<?> type = toSpawn.getKey();
			CompoundTag tag = toSpawn.getValue();
			double spawnRange = gate.getGateway().getSpawnRange();

			int tries = 0;
			double x = pos.getX() + (-1 + 2 * level.random.nextDouble()) * spawnRange;
			double y = pos.getY() + level.random.nextInt(3) - 1;
			double z = pos.getZ() + (-1 + 2 * level.random.nextDouble()) * spawnRange;
			while (!level.noCollision(type.getAABB(x, y, z)) && tries++ < 7) {
				x = pos.getX() + (level.random.nextDouble() - level.random.nextDouble()) * spawnRange + 0.5D;
				y = pos.getY() + level.random.nextInt(3 * (int) gate.getGateway().getSize().getScale()) - 1;
				z = pos.getZ() + (level.random.nextDouble() - level.random.nextDouble()) * spawnRange + 0.5D;
			}

			final double fx = x, fy = y, fz = z;

			if (level.noCollision(type.getAABB(x, y, z))) {
				Entity entity = type.create(level);

				if (!(entity instanceof LivingEntity)) {
					Gateways.LOGGER.error("Gate {} failed to create a living entity during wave {}!", gate.getName().getString(), gate.getWave());
					continue;
				}
				LivingEntity living = (LivingEntity) entity;

				if (tag != null) entity.load(tag);
				entity.moveTo(fx, fy, fz, level.random.nextFloat() * 360, level.random.nextFloat() * 360);

				modifiers.forEach(m -> m.apply(level.random, living));
				living.setHealth(living.getMaxHealth());
				living.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 5, 100, true, false));

				if (entity instanceof Mob mob) {
					if ((tag == null || tag.isEmpty() || tag.getBoolean("ForceFinalizeSpawn")) && !ForgeEventFactory.doSpecialSpawn((Mob) entity, (LevelAccessor) level, (float) entity.getX(), (float) entity.getY(), (float) entity.getZ(), null, MobSpawnType.NATURAL)) {
						mob.finalizeSpawn(level, level.getCurrentDifficultyAt(entity.blockPosition()), MobSpawnType.NATURAL, null, null);
					}
					mob.setTarget(gate.getLevel().getNearestPlayer(gate, 12));
				}

				level.addFreshEntityWithPassengers(entity);
				level.playSound(null, gate.getX(), gate.getY(), gate.getZ(), GatewayObjects.GATE_WARP, SoundSource.HOSTILE, 0.5F, 1);
				spawned.add((LivingEntity) entity);
				gate.spawnParticle(gate.getGateway().getColor(), entity.getX() + entity.getBbWidth() / 2, entity.getY() + entity.getBbHeight() / 2, entity.getZ() + entity.getBbWidth() / 2, 0);
			} else {
				gate.onFailure(spawned, new TranslatableComponent("error.gateways.wave_failed").withStyle(ChatFormatting.RED));
				break;
			}
		}

		return spawned;
	}

	public List<ItemStack> spawnRewards(ServerLevel level, GatewayEntity gate, Player summoner) {
		List<ItemStack> stacks = new ArrayList<>();
		this.rewards.forEach(r -> r.generateLoot(level, gate, summoner, stacks::add));
		return stacks;
	}

	public JsonObject write() {
		JsonObject obj = new JsonObject();
		JsonArray arr = new JsonArray();
		for (Pair<EntityType<?>, CompoundTag> entity : entities) {
			JsonObject entityData = new JsonObject();
			entityData.addProperty("entity", entity.getKey().getRegistryName().toString());
			if (entity.getValue() != null) entityData.add("nbt", ItemAdapter.ITEM_READER.toJsonTree(entity.getValue()));
			arr.add(entityData);
		}
		obj.add("entities", arr);
		obj.add("modifiers", Gateway.GSON.toJsonTree(modifiers));
		obj.add("rewards", Gateway.GSON.toJsonTree(rewards));
		obj.addProperty("max_wave_time", maxWaveTime);
		obj.addProperty("setup_time", setupTime);
		return obj;
	}

	public static Wave read(JsonObject obj) {
		JsonArray entities = obj.get("entities").getAsJsonArray();
		List<Pair<EntityType<?>, CompoundTag>> entityList = new ArrayList<>();
		for (JsonElement e : entities) {
			JsonObject entity = e.getAsJsonObject();
			EntityType<?> type = JsonUtil.getRegistryObject(entity, "entity", ForgeRegistries.ENTITIES);
			CompoundTag nbt = entity.has("nbt") ? ItemAdapter.ITEM_READER.fromJson(entity.get("nbt"), CompoundTag.class) : null;
			entityList.add(Pair.of(type, nbt));
		}
		List<RandomAttributeModifier> modifiers = Gateway.GSON.fromJson(obj.get("modifiers"), new TypeToken<List<RandomAttributeModifier>>() {
		}.getType());
		if (modifiers == null) modifiers = Collections.emptyList();
		List<Reward> rewards = Gateway.GSON.fromJson(obj.get("rewards"), new TypeToken<List<Reward>>() {
		}.getType());
		if (rewards == null) rewards = Collections.emptyList();
		int maxWaveTime = GsonHelper.getAsInt(obj, "max_wave_time");
		int recoveryTime = GsonHelper.getAsInt(obj, "setup_time");
		return new Wave(entityList, modifiers, rewards, maxWaveTime, recoveryTime);
	}

	public void write(FriendlyByteBuf buf) {
		buf.writeVarInt(entities.size());
		for (Pair<EntityType<?>, CompoundTag> entity : entities) {
			buf.writeRegistryId(entity.getKey());
		}
		buf.writeVarInt(modifiers.size());
		modifiers.forEach(m -> {
			buf.writeRegistryId(m.getAttribute());
			buf.writeByte(m.getOp().ordinal());
			buf.writeFloat((float) m.getValue().min());
		});
		buf.writeVarInt(rewards.size());
		rewards.forEach(r -> r.write(buf));
		buf.writeInt(maxWaveTime);
		buf.writeInt(setupTime);
	}

	public static Wave read(FriendlyByteBuf buf) {
		int size = buf.readVarInt();
		List<Pair<EntityType<?>, CompoundTag>> entities = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			entities.add(Pair.of(buf.readRegistryIdSafe(EntityType.class), null));
		}
		size = buf.readVarInt();
		List<RandomAttributeModifier> modifiers = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			Attribute attrib = buf.readRegistryIdSafe(Attribute.class);
			Operation op = Operation.values()[buf.readByte()];
			float value = buf.readFloat();
			modifiers.add(new RandomAttributeModifier(attrib, op, new RandomRange(value, value)));
		}
		size = buf.readVarInt();
		List<Reward> rewards = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			rewards.add(Reward.read(buf));
		}
		int maxWaveTime = buf.readInt();
		int recoveryTime = buf.readInt();
		return new Wave(entities, modifiers, rewards, maxWaveTime, recoveryTime);
	}

	public static class Serializer implements JsonDeserializer<Wave>, JsonSerializer<Wave> {

		@Override
		public JsonElement serialize(Wave src, Type typeOfSrc, JsonSerializationContext context) {
			return src.write();
		}

		@Override
		public Wave deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			return Wave.read(json.getAsJsonObject());
		}

	}

}
