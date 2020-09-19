package shadows.gateways;

import com.digitalfeonix.hydrogel.block.HydroGelBlock;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent.Register;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ObjectHolder;
import shadows.placebo.recipe.RecipeHelper;

@Mod(GatewaysToEternity.MODID)
public class GatewaysToEternity {

	public static final String MODID = "gateways";

	public GatewaysToEternity() {
		FMLJavaModLoadingContext.get().getModEventBus().register(this);
	}

}
