package shadows.gateways;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.RegistryObject;
import shadows.gateways.client.GatewayParticleData;
import shadows.gateways.entity.GatewayEntity;
import shadows.gateways.item.GatePearlItem;
import shadows.placebo.util.RegObjHelper;

public class GatewayObjects {

	private static final RegObjHelper R = new RegObjHelper(Gateways.MODID);

	public static final RegistryObject<EntityType<GatewayEntity>> GATEWAY = R.entity("gateway");

	public static final RegistryObject<GatePearlItem> GATE_PEARL = R.item("gate_pearl");

	public static final RegistryObject<SoundEvent> GATE_AMBIENT = R.sound("gate_ambient");
	public static final RegistryObject<SoundEvent> GATE_WARP = R.sound("gate_warp");
	public static final RegistryObject<SoundEvent> GATE_START = R.sound("gate_start");
	public static final RegistryObject<SoundEvent> GATE_END = R.sound("gate_end");

	public static final RegistryObject<ParticleType<GatewayParticleData>> GLOW = R.particle("glow");

	public static class Stats {
		public static final ResourceLocation STAT_GATES_DEFEATED = new ResourceLocation(Gateways.MODID, "gates_defeated");
	}
}
