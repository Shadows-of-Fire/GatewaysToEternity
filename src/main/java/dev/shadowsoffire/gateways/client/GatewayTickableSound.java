package dev.shadowsoffire.gateways.client;

import dev.shadowsoffire.gateways.GatewayObjects;
import dev.shadowsoffire.gateways.entity.GatewayEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;

public class GatewayTickableSound extends AbstractTickableSoundInstance {
    private final GatewayEntity gateway;

    public GatewayTickableSound(GatewayEntity gateway) {
        super(GatewayObjects.GATE_AMBIENT.get(), SoundSource.HOSTILE, gateway.level().getRandom());
        this.gateway = gateway;
        this.looping = true;
        this.delay = 0;
        this.x = (float) gateway.getX();
        this.y = (float) gateway.getY();
        this.z = (float) gateway.getZ();
        this.relative = true;
        this.pitch = 0.75F;
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        if (!this.gateway.isAlive()) {
            this.stop();
        }
        else {
            this.volume = 0.35F - (float) (Minecraft.getInstance().player.distanceToSqr(this.gateway) / (18 * 18F)) / 4F;
        }
    }

    public static void startGatewaySound(GatewayEntity entity) {
        Minecraft.getInstance().getSoundManager().play(new GatewayTickableSound(entity));
    }
}
