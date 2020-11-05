package shadows.gateways;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.item.crafting.RecipeManager;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.world.BossInfo;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ParticleFactoryRegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import shadows.gateways.client.GatewayParticle;
import shadows.gateways.client.GatewayRenderer;
import shadows.gateways.util.BossColorMap;

@EventBusSubscriber(bus = Bus.MOD, value = Dist.CLIENT, modid = GatewaysToEternity.MODID)
public class GatewaysToEternityClient {

	@SubscribeEvent
	public static void setup(FMLClientSetupEvent e) {
		e.enqueueWork(() -> {
			EntityRendererManager mgr = Minecraft.getInstance().getRenderManager();
			mgr.register(GatewayObjects.SMALL_GATEWAY, new GatewayRenderer(mgr));
			Minecraft.getInstance().getItemColors().register((stack, tint) -> {
				if (stack.hasTag() && stack.getTag().contains("gateway_data")) {
					CompoundNBT data = stack.getOrCreateChildTag("gateway_data");
					if (data.contains("color")) {
						BossInfo.Color color = BossInfo.Color.byName(data.getString("color"));
						return BossColorMap.getColor(color);
					}
				}
				return 0xFFFFFF;
			}, GatewayObjects.SMALL_GATE_OPENER);
		});
	}

	@SubscribeEvent
	public static void factories(ParticleFactoryRegisterEvent e) {
		Minecraft.getInstance().particles.registerFactory(GatewayObjects.GLOW, GatewayParticle.Factory::new);
	}

	public static RecipeManager getClientRecipeManager() {
		ClientPlayerEntity player = Minecraft.getInstance().player;
		if (player == null || player.connection == null) return null;
		return player.connection.getRecipeManager();
	}

}
