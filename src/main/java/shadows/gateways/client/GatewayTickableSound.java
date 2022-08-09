package shadows.gateways.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import shadows.gateways.GatewayObjects;
import shadows.gateways.entity.GatewayEntity;

public class GatewayTickableSound extends AbstractTickableSoundInstance {
	private final GatewayEntity gateway;

	public GatewayTickableSound(GatewayEntity gateway) {
		super(GatewayObjects.GATE_AMBIENT, SoundSource.HOSTILE);
		this.gateway = gateway;
		this.looping = true;
		this.delay = 0;
		this.x = (float) gateway.getX();
		this.y = (float) gateway.getY();
		this.z = (float) gateway.getZ();
		this.relative = true;
		this.pitch = 0.75F;
	}

	public boolean canStartSilent() {
		return true;
	}

	public void tick() {
		if (!this.gateway.isAlive()) {
			this.stop();
		} else {
			this.volume = 0.35F - (float) (Minecraft.getInstance().player.distanceToSqr(this.gateway) / (18 * 18F)) / 4F;
		}
	}

	public static void startGatewaySound(GatewayEntity entity) {
		Minecraft.getInstance().getSoundManager().play(new GatewayTickableSound(entity));
	}
}