package shadows.gateways.gate;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
import shadows.placebo.json.PlaceboJsonReloadListener;
import shadows.placebo.json.RandomAttributeModifier;
import shadows.placebo.util.StepFunction;

/**
 * A single wave of a gateway.
 * @param entities A list of all entities to be spawned this wave, with optional NBT for additional data.
 * @param modifiers A list of modifiers that will be applied to all spawned entities.
 * @param rewards All rewards that will be granted at the end of the wave.
 * @param maxWaveTime The time the player has to complete this wave.
 * @param setupTime The delay after this wave before the next wave starts.  Ignored if this is the last wave.
 */
public record Wave(List<WaveEntity> entities, List<RandomAttributeModifier> modifiers, List<Reward> rewards, int maxWaveTime, int setupTime) {

	public List<LivingEntity> spawnWave(ServerLevel level, BlockPos pos, GatewayEntity gate) {
		List<LivingEntity> spawned = new ArrayList<>();
		for (WaveEntity toSpawn : entities) {

			double spawnRange = gate.getGateway().getSpawnRange();

			int tries = 0;
			double x = pos.getX() + (-1 + 2 * level.random.nextDouble()) * spawnRange;
			double y = pos.getY() + level.random.nextInt(3) - 1;
			double z = pos.getZ() + (-1 + 2 * level.random.nextDouble()) * spawnRange;
			while (!level.noCollision(toSpawn.getAABB(x, y, z)) && tries++ < 7) {
				x = pos.getX() + (level.random.nextDouble() - level.random.nextDouble()) * spawnRange + 0.5D;
				y = pos.getY() + level.random.nextInt(3 * (int) gate.getGateway().getSize().getScale()) - 1;
				z = pos.getZ() + (level.random.nextDouble() - level.random.nextDouble()) * spawnRange + 0.5D;
			}

			final double fx = x, fy = y, fz = z;

			if (level.noCollision(toSpawn.getAABB(fx, fy, fz))) {
				LivingEntity entity = toSpawn.createEntity(level);

				if (entity == null) {
					Gateways.LOGGER.error("Gate {} failed to create a living entity during wave {}!", gate.getName().getString(), gate.getWave());
					continue;
				}

				entity.moveTo(fx, fy, fz, level.random.nextFloat() * 360, level.random.nextFloat() * 360);

				entity.getPassengersAndSelf().filter(e -> e instanceof LivingEntity).map(LivingEntity.class::cast).forEach(e -> {
					modifiers.forEach(m -> m.apply(level.random, e));
					e.setHealth(entity.getMaxHealth());
					e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 5, 100, true, false));
				});

				if (entity instanceof Mob mob) {
					if (toSpawn.shouldFinalizeSpawn() && !ForgeEventFactory.doSpecialSpawn((Mob) entity, (LevelAccessor) level, (float) entity.getX(), (float) entity.getY(), (float) entity.getZ(), null, MobSpawnType.SPAWNER)) {
						mob.finalizeSpawn(level, level.getCurrentDifficultyAt(entity.blockPosition()), MobSpawnType.SPAWNER, null, null);
					}
					mob.setTarget(gate.getLevel().getNearestPlayer(gate, 12));
				}

				level.addFreshEntityWithPassengers(entity);
				level.playSound(null, gate.getX(), gate.getY(), gate.getZ(), GatewayObjects.GATE_WARP.get(), SoundSource.HOSTILE, 0.5F, 1);
				spawned.add((LivingEntity) entity);
				gate.spawnParticle(gate.getGateway().getColor(), entity.getX() + entity.getBbWidth() / 2, entity.getY() + entity.getBbHeight() / 2, entity.getZ() + entity.getBbWidth() / 2, 0);
			} else {
				gate.onFailure(spawned, Component.translatable("error.gateways.wave_failed").withStyle(ChatFormatting.RED));
				break;
			}
		}

		return spawned;
	}

	public List<ItemStack> spawnRewards(ServerLevel level, GatewayEntity gate, Player summoner) {
		List<ItemStack> stacks = new ArrayList<>();
		this.rewards.forEach(r -> r.generateLoot(level, gate, summoner, s -> {
			if (!s.isEmpty()) stacks.add(s);
		}));
		return stacks;
	}

	public JsonObject write() {
		JsonObject obj = new JsonObject();
		JsonArray arr = new JsonArray();
		for (WaveEntity entity : entities) {
			var s = entity.getSerializer();
			ResourceLocation id = WaveEntity.SERIALIZERS.inverse().get(s);
			JsonObject entityData = s.write(entity).getAsJsonObject();
			entityData.addProperty("type", id.toString());
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
		List<WaveEntity> entityList = new ArrayList<>();
		for (JsonElement e : entities) {
			JsonObject entity = e.getAsJsonObject();
			ResourceLocation id = entity.has("type") ? new ResourceLocation(entity.get("type").getAsString()) : PlaceboJsonReloadListener.DEFAULT;
			var s = WaveEntity.SERIALIZERS.get(id);
			entityList.add(s.read(entity));
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
		for (WaveEntity entity : entities) {
			var s = entity.getSerializer();
			ResourceLocation id = WaveEntity.SERIALIZERS.inverse().get(s);
			buf.writeResourceLocation(id);
			s.write(entity, buf);
		}
		buf.writeVarInt(modifiers.size());
		modifiers.forEach(m -> {
			buf.writeRegistryId(ForgeRegistries.ATTRIBUTES, m.getAttribute());
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
		List<WaveEntity> entities = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			ResourceLocation id = buf.readResourceLocation();
			var s = WaveEntity.SERIALIZERS.get(id);
			entities.add(s.read(buf));
		}
		size = buf.readVarInt();
		List<RandomAttributeModifier> modifiers = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			Attribute attrib = buf.readRegistryIdSafe(Attribute.class);
			Operation op = Operation.values()[buf.readByte()];
			float value = buf.readFloat();
			modifiers.add(new RandomAttributeModifier(attrib, op, new StepFunction(value, 1, 0)));
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
