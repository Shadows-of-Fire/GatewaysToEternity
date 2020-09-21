package shadows.gateways.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.TickableSound;
import net.minecraft.util.SoundCategory;
import shadows.gateways.GatewayObjects;
import shadows.gateways.entity.AbstractGatewayEntity;

public class GatewayTickableSound extends TickableSound {
	private final AbstractGatewayEntity gateway;

	public GatewayTickableSound(AbstractGatewayEntity gateway) {
		super(GatewayObjects.GATE_AMBIENT, SoundCategory.HOSTILE);
		this.gateway = gateway;
		this.repeat = true;
		this.repeatDelay = 0;
		this.x = (float) gateway.getX();
		this.y = (float) gateway.getY();
		this.z = (float) gateway.getZ();
		this.global = false;
		this.pitch = 0.75F;
	}

	public boolean canBeSilent() {
		return true;
	}

	public void tick() {
		if (!this.gateway.isAlive()) {
			this.donePlaying = true;
		} else {
			this.volume = 1 - (float) Minecraft.getInstance().player.getDistanceSq(this.gateway) / (18 * 18F);
		}
	}

	public static void startGatewaySound(AbstractGatewayEntity entity) {
		Minecraft.getInstance().getSoundHandler().play(new GatewayTickableSound(entity));
	}
}