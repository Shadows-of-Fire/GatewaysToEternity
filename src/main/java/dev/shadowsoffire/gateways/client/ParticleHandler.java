package dev.shadowsoffire.gateways.client;

import dev.shadowsoffire.gateways.entity.GatewayEntity;
import dev.shadowsoffire.gateways.net.ParticleMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public class ParticleHandler {

    public static void handle(ParticleMessage msg) {
        double x = msg.x, y = msg.y, z = msg.z;
        int color = msg.color;
        ParticleMessage.Type type = msg.type;
        Entity src = Minecraft.getInstance().level.getEntity(msg.gateId);
        if (src == null) return;
        switch (type) {
            case IDLE -> {
                GatewayParticleData data = new GatewayParticleData(color >> 16 & 255, color >> 8 & 255, color & 255);
                RandomSource rand = src.level().random;
                for (int i = 0; i < 6; i++) {
                    double velX = Mth.nextDouble(rand, -0.15, 0.15);
                    double velY = Mth.nextDouble(rand, -0.15, 0.15);
                    double velZ = Mth.nextDouble(rand, -0.15, 0.15);
                    double xOff = Mth.nextDouble(rand, -0.25, 0.25);
                    double yOff = Mth.nextDouble(rand, -0.2, 0.2);
                    double zOff = Mth.nextDouble(rand, -0.25, 0.25);
                    Minecraft.getInstance().particleEngine.createParticle(data, x + xOff, y + yOff, z + zOff, velX, velY, velZ);
                }
            }
            case SPAWNED -> {
                GatewayParticleData data = new GatewayParticleData(color >> 16 & 255, color >> 8 & 255, color & 255);
                RandomSource rand = src.level().random;
                for (int i = 0; i < 25; i++) {
                    double velY = Mth.nextDouble(rand, 0.05, 0.35);
                    double xOff = Mth.nextDouble(rand, -0.15, 0.15);
                    double yOff = Mth.nextDouble(rand, 0.05, 0.15);
                    double zOff = Mth.nextDouble(rand, -0.15, 0.15);
                    Minecraft.getInstance().particleEngine.createParticle(data, x + xOff, y + yOff, z + zOff, 0, velY, 0);
                }
            }
        }
    }

    public static void spawnIdleParticles(GatewayEntity gate) {
        if (!gate.isValid()) return;

        Level level = gate.level();
        RandomSource rand = level.random;
        int color = gate.getGateway().color().getValue();
        GatewayParticleData data = new GatewayParticleData(color >> 16 & 255, color >> 8 & 255, color & 255);

        double x = gate.getX();
        double y = gate.getY() + gate.getBbHeight() / 2;
        double z = gate.getZ();
        float scale = gate.getGateway().size().getScale();

        for (int i = 0; i < 30; i++) {
            float deg = (float) (i * 12 * Math.PI / 180F);
            double velX = Mth.cos(deg) * 0.10 * scale;
            double velY = Mth.nextDouble(rand, -0.2 * scale, -0.15);
            double velZ = Mth.sin(deg) * 0.10 * scale;
            double xOff = velX * rand.nextDouble() * 0.2;
            double yOff = Mth.nextDouble(rand, -0.1, 0.2);
            double zOff = velZ * rand.nextDouble() * 0.2;
            if (rand.nextFloat() < 0.35) Minecraft.getInstance().particleEngine.createParticle(data, x + xOff, y + yOff, z + zOff, velX, velY, velZ);
        }

    }
}
