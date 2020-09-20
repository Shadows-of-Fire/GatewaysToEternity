package shadows.gateways;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.Rarity;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraftforge.event.RegistryEvent.Register;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ObjectHolder;
import shadows.gateways.entity.AbstractGatewayEntity;
import shadows.gateways.entity.SmallGatewayEntity;
import shadows.gateways.item.GatewayItem;
import shadows.gateways.recipe.GatewayRecipeSerializer;

@Mod(GatewaysToEternity.MODID)
public class GatewaysToEternity {

	public static final String MODID = "gateways";
	public static final Logger LOGGER = LogManager.getLogger("Gateways to Eternity");

	@ObjectHolder(MODID + ":small_gateway")
	public static final EntityType<SmallGatewayEntity> SMALL_GATEWAY = null;

	@ObjectHolder(MODID + ":small_gate_opener")
	public static final GatewayItem SMALL_GATE_OPENER = null;

	public GatewaysToEternity() {
		FMLJavaModLoadingContext.get().getModEventBus().register(this);
	}

	@SubscribeEvent
	public void registerEntities(Register<EntityType<?>> e) {
		//Formatter::off
		e.getRegistry().register(EntityType.Builder
				.<AbstractGatewayEntity>create(SmallGatewayEntity::new, EntityClassification.MISC)
				.setTrackingRange(5)
				.setUpdateInterval(20)
				.size(2F, 2.5F)
				.setCustomClientFactory((se, w) -> new SmallGatewayEntity(SMALL_GATEWAY, w))
				.build("small_gateway")
				.setRegistryName("small_gateway"));
		//Formatter::on
	}

	@SubscribeEvent
	public void registerItems(Register<Item> e) {
		e.getRegistry().register(new GatewayItem(new Item.Properties().maxStackSize(1).rarity(Rarity.UNCOMMON).group(ItemGroup.MISC), SmallGatewayEntity::new).setRegistryName("small_gate_opener"));
	}

	@SubscribeEvent
	public void registerSerializers(Register<IRecipeSerializer<?>> e) {
		e.getRegistry().register(GatewayRecipeSerializer.INSTANCE.setRegistryName("gate_recipe"));
	}

}
