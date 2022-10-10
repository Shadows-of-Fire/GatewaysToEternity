package shadows.gateways.gate;

import java.util.function.Function;

import javax.annotation.Nullable;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;
import shadows.placebo.json.PlaceboJsonReloadListener;
import shadows.placebo.json.SerializerBuilder;
import shadows.placebo.util.json.ItemAdapter;

public interface WaveEntity {

	public static BiMap<ResourceLocation, SerializerBuilder<WaveEntity>.Serializer> SERIALIZERS = HashBiMap.create();

	/**
	 * Creates the entity to be spawned in the current wave.
	 * @param level
	 * @return The entity, or null if an error occured.  Null will end the gate.
	 */
	public LivingEntity createEntity(World level);

	public ITextComponent getDescription();

	public AxisAlignedBB getAABB(double x, double y, double z);

	public boolean shouldFinalizeSpawn();

	public SerializerBuilder<WaveEntity>.Serializer getSerializer();

	public static class StandardWaveEntity implements WaveEntity {

		static final SerializerBuilder<StandardWaveEntity>.Serializer SERIALIZER = new SerializerBuilder<StandardWaveEntity>("Std Wave Entity").withJsonDeserializer(StandardWaveEntity::read).withJsonSerializer(StandardWaveEntity::write).withNetworkDeserializer(StandardWaveEntity::read).withNetworkSerializer(StandardWaveEntity::write).build(true);

		protected final EntityType<?> type;
		protected final CompoundNBT tag;

		public StandardWaveEntity(EntityType<?> type, @Nullable CompoundNBT tag) {
			this.type = type;
			this.tag = tag == null ? new CompoundNBT() : tag;
			this.tag.putString("id", type.getRegistryName().toString());
		}

		@Override
		public LivingEntity createEntity(World level) {
			Entity ent = EntityType.loadEntityRecursive(this.tag, level, Function.identity());
			return ent instanceof LivingEntity ? (LivingEntity) ent : null;
		}

		@Override
		public ITextComponent getDescription() {
			return new TranslationTextComponent(type.getDescriptionId());
		}

		@Override
		public AxisAlignedBB getAABB(double x, double y, double z) {
			return this.type.getAABB(x, y, z);
		}

		@Override
		public boolean shouldFinalizeSpawn() {
			return this.tag.size() == 1 || this.tag.getBoolean("ForceFinalizeSpawn");
		}

		@Override
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public SerializerBuilder<WaveEntity>.Serializer getSerializer() {
			return (SerializerBuilder.Serializer) SERIALIZER;
		}

		public JsonObject write() {
			JsonObject entityData = new JsonObject();
			entityData.addProperty("entity", type.getRegistryName().toString());
			if (tag != null) entityData.add("nbt", ItemAdapter.ITEM_READER.toJsonTree(tag));
			return entityData;
		}

		public static StandardWaveEntity read(JsonObject obj) {
			EntityType<?> type = getRegistryObject(obj, "entity", ForgeRegistries.ENTITIES);
			CompoundNBT nbt = obj.has("nbt") ? ItemAdapter.ITEM_READER.fromJson(obj.get("nbt"), CompoundNBT.class) : null;
			return new StandardWaveEntity(type, nbt);
		}

		public void write(PacketBuffer buf) {
			buf.writeRegistryId(type);
		}

		public static StandardWaveEntity read(PacketBuffer buf) {
			return new StandardWaveEntity(buf.readRegistryIdSafe(EntityType.class), null);
		}

	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void initSerializers() {
		SERIALIZERS.put(PlaceboJsonReloadListener.DEFAULT, (SerializerBuilder.Serializer) StandardWaveEntity.SERIALIZER);
	}

	public static <T extends IForgeRegistryEntry<T>> T getRegistryObject(JsonObject parent, String name, IForgeRegistry<T> registry) {
		String key = JSONUtils.getAsString(parent, name);
		T regObj = registry.getValue(new ResourceLocation(key));
		if (regObj == null) throw new JsonSyntaxException("Failed to parse " + registry.getRegistryName() + " object with key " + key);
		return regObj;
	}

}
