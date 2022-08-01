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
		this.looping = true;
		this.delay = 0;
		this.x = (float) gateway.getX();
		this.y = (float) gateway.getY();
		this.z = (float) gateway.getZ();
		this.relative = false;
		this.pitch = 0.75F;
	}

	public boolean canStartSilent() {
		return true;
	}

	public void tick() {
		if (!this.gateway.isAlive()) {
			this.stop();
		} else {
			this.volume = 0.25F - (float) (Minecraft.getInstance().player.distanceToSqr(this.gateway) / (18 * 18F)) / 4F;
		}
	}

	public static void startGatewaySound(AbstractGatewayEntity entity) {
		Minecraft.getInstance().getSoundManager().play(new GatewayTickableSound(entity));
	}
}