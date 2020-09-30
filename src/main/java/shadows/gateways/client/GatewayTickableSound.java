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
		this.x = (float) gateway.getPosX();
		this.y = (float) gateway.getPosY();
		this.z = (float) gateway.getPosZ();
		this.global = false;
		this.pitch = 0.75F;
	}

	public boolean canBeSilent() {
		return true;
	}

	public void tick() {
		if (!this.gateway.isAlive()) {
			this.finishPlaying();
		} else {
			this.volume = 0.25F - (float) (Minecraft.getInstance().player.getDistanceSq(this.gateway) / (18 * 18F)) / 4F;
		}
	}

	public static void startGatewaySound(AbstractGatewayEntity entity) {
		Minecraft.getInstance().getSoundHandler().play(new GatewayTickableSound(entity));
	}
}