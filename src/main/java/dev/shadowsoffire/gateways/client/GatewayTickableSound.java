package dev.shadowsoffire.gateways.client;

import dev.shadowsoffire.gateways.GatewayObjects;
import dev.shadowsoffire.gateways.entity.GatewayEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;

public class GatewayTickableSound extends AbstractTickableSoundInstance {
    private final GatewayEntity gateway;

    public GatewayTickableSound(GatewayEntity gateway) {
        super(gateway.getGateway().soundtrack().value(), SoundSource.HOSTILE, gateway.level().getRandom());
        this.gateway = gateway;
        this.looping = true;
        this.delay = 0;
        this.x = (float) gateway.getX();
        this.y = (float) gateway.getY();
        this.z = (float) gateway.getZ();

        // Magic special casing for the default sound. TODO: Data-driven sound properties.
        if (gateway.getGateway().soundtrack() == GatewayObjects.GATE_AMBIENT) {
            this.pitch = 0.75F;
            this.attenuation = Attenuation.LINEAR;
        }
        else {
            this.attenuation = Attenuation.NONE;
        }
    }

    @Override
    public void tick() {
        if (!this.gateway.isAlive()) {
            this.stop();
        }
        else {
            this.volume = (1 - (float) (Minecraft.getInstance().player.distanceTo(this.gateway) / this.gateway.getGateway().rules().leashRange())) * 0.33F;
        }
    }

    public static void startGatewaySound(GatewayEntity entity) {
        Minecraft.getInstance().getSoundManager().play(new GatewayTickableSound(entity));
    }
}
