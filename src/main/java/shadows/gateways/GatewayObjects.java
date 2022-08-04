package shadows.gateways;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ObjectHolder;
import shadows.gateways.client.GatewayParticle;
import shadows.gateways.entity.GatewayEntity;
import shadows.gateways.item.GatePearlItem;

@ObjectHolder(Gateways.MODID)
public class GatewayObjects {

	public static final EntityType<GatewayEntity> GATEWAY = null;
	public static final GatePearlItem GATE_PEARL = null;
	public static final SoundEvent GATE_AMBIENT = null;
	public static final SoundEvent GATE_WARP = null;
	public static final SoundEvent GATE_START = null;
	public static final ParticleType<GatewayParticle.Data> GLOW = null;
	public static final SoundEvent GATE_END = null;

}
