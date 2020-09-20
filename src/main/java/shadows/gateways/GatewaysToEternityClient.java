package shadows.gateways;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DeferredWorkQueue;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import shadows.gateways.client.GatewayRenderer;

@SuppressWarnings("deprecation")
@EventBusSubscriber(bus = Bus.MOD, value = Dist.CLIENT, modid = GatewaysToEternity.MODID)
public class GatewaysToEternityClient {

	@SubscribeEvent
	public static void setup(FMLClientSetupEvent e) {
		DeferredWorkQueue.runLater(() -> {
			EntityRendererManager mgr = Minecraft.getInstance().getRenderManager();
			mgr.register(GatewaysToEternity.SMALL_GATEWAY, new GatewayRenderer(mgr));
		});
	}

}
