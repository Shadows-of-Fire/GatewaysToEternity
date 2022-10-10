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

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.ForgeEventFactory;
import shadows.gateways.GatewayObjects;
import shadows.gateways.Gateways;
import shadows.gateways.entity.GatewayEntity;
import shadows.gateways.misc.RandomAttributeModifier;
import shadows.gateways.misc.StepFunction;
import shadows.placebo.json.PlaceboJsonReloadListener;
import shadows.placebo.json.SerializerBuilder;

/**
 * A single wave of a gateway.
 * @param entities A list of all entities to be spawned this wave, with optional NBT for additional data.
 * @param modifiers A list of modifiers that will be applied to all spawned entities.
 * @param rewards All rewards that will be granted at the end of the wave.
 * @param maxWaveTime The time the player has to complete this wave.
 * @param setupTime The delay after this wave before the next wave starts.  Ignored if this is the last wave.
 */
public class Wave {

	private final List<WaveEntity> entities;
	private final List<RandomAttributeModifier> modifiers;
	private final List<Reward> rewards;
	private final int maxWaveTime;
	private final int setupTime;

	public Wave(List<WaveEntity> entities, List<RandomAttributeModifier> modifiers, List<Reward> rewards, int maxWaveTime, int setupTime) {
		this.entities = entities;
		this.modifiers = modifiers;
		this.rewards = rewards;
		this.maxWaveTime = maxWaveTime;
		this.setupTime = setupTime;
	}

	public List<LivingEntity> spawnWave(ServerWorld level, BlockPos pos, GatewayEntity gate) {
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

				entity.getSelfAndPassengers().filter(e -> e instanceof LivingEntity).map(LivingEntity.class::cast).forEach(e -> {
					modifiers.forEach(m -> m.apply(level.random, e));
					e.setHealth(entity.getMaxHealth());
					e.addEffect(new EffectInstance(Effects.DAMAGE_RESISTANCE, 5, 100, true, false));
				});

				if (entity instanceof MobEntity) {
					MobEntity mob = (MobEntity) entity;
					if (toSpawn.shouldFinalizeSpawn() && !ForgeEventFactory.doSpecialSpawn((MobEntity) entity, (World) level, (float) entity.getX(), (float) entity.getY(), (float) entity.getZ(), null, SpawnReason.SPAWNER)) {
						mob.finalizeSpawn(level, level.getCurrentDifficultyAt(entity.blockPosition()), SpawnReason.SPAWNER, null, null);
					}
					mob.setTarget(gate.level.getNearestPlayer(gate, 12));
				}

				level.addFreshEntityWithPassengers(entity);
				level.playSound(null, gate.getX(), gate.getY(), gate.getZ(), GatewayObjects.GATE_WARP, SoundCategory.HOSTILE, 0.5F, 1);
				spawned.add((LivingEntity) entity);
				gate.spawnParticle(gate.getGateway().getColor(), entity.getX() + entity.getBbWidth() / 2, entity.getY() + entity.getBbHeight() / 2, entity.getZ() + entity.getBbWidth() / 2, 0);
			} else {
				gate.onFailure(spawned, new TranslationTextComponent("error.gateways.wave_failed").withStyle(TextFormatting.RED));
				break;
			}
		}

		return spawned;
	}

	public List<ItemStack> spawnRewards(ServerWorld level, GatewayEntity gate, PlayerEntity summoner) {
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
			SerializerBuilder<WaveEntity>.Serializer s = entity.getSerializer();
			ResourceLocation id = WaveEntity.SERIALIZERS.inverse().get(s);
			JsonObject entityData = s.serialize(entity);
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
			SerializerBuilder<WaveEntity>.Serializer s = WaveEntity.SERIALIZERS.get(id);
			entityList.add(s.deserialize(entity));
		}
		List<RandomAttributeModifier> modifiers = Gateway.GSON.fromJson(obj.get("modifiers"), new TypeToken<List<RandomAttributeModifier>>() {
		}.getType());
		if (modifiers == null) modifiers = Collections.emptyList();
		List<Reward> rewards = Gateway.GSON.fromJson(obj.get("rewards"), new TypeToken<List<Reward>>() {
		}.getType());
		if (rewards == null) rewards = Collections.emptyList();
		int maxWaveTime = JSONUtils.getAsInt(obj, "max_wave_time");
		int recoveryTime = JSONUtils.getAsInt(obj, "setup_time");
		return new Wave(entityList, modifiers, rewards, maxWaveTime, recoveryTime);
	}

	public void write(PacketBuffer buf) {
		buf.writeVarInt(entities.size());
		for (WaveEntity entity : entities) {
			SerializerBuilder<WaveEntity>.Serializer s = entity.getSerializer();
			ResourceLocation id = WaveEntity.SERIALIZERS.inverse().get(s);
			buf.writeResourceLocation(id);
			s.serialize(entity, buf);
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

	public static Wave read(PacketBuffer buf) {
		int size = buf.readVarInt();
		List<WaveEntity> entities = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			ResourceLocation id = buf.readResourceLocation();
			SerializerBuilder<WaveEntity>.Serializer s = WaveEntity.SERIALIZERS.get(id);
			entities.add(s.deserialize(buf));
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

	public int maxWaveTime() {
		return this.maxWaveTime;
	}

	public int setupTime() {
		return this.setupTime;
	}

	public List<WaveEntity> entities() {
		return this.entities;
	}

	public List<RandomAttributeModifier> modifiers() {
		return this.modifiers;
	}

	public List<Reward> rewards() {
		return this.rewards;
	}

}
