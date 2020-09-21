package shadows.gateways;

import net.minecraft.entity.EntityType;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.registries.ObjectHolder;
import shadows.gateways.entity.SmallGatewayEntity;
import shadows.gateways.item.GatewayItem;

@ObjectHolder(GatewaysToEternity.MODID)
public class GatewayObjects {

	public static final EntityType<SmallGatewayEntity> SMALL_GATEWAY = null;
	public static final GatewayItem SMALL_GATE_OPENER = null;
	public static final SoundEvent GATE_AMBIENT = null;
	public static final SoundEvent GATE_WARP = null;

}
