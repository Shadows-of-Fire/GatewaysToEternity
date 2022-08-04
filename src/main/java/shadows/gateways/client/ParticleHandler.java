package shadows.gateways.client;

import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import shadows.gateways.entity.GatewayEntity;
import shadows.gateways.net.ParticleMessage;

public class ParticleHandler {

	public static void handle(ParticleMessage msg) {
		double x = msg.x, y = msg.y, z = msg.z;
		int color = msg.color;
		int type = msg.type;
		Entity src = Minecraft.getInstance().level.getEntity(msg.gateId);
		if (src == null) return;
		if (type == 0) { //Type 0: Entity spawned from portal.  Spawns a cluster of particles around the entity.
			GatewayParticle.Data data = new GatewayParticle.Data(color >> 16 & 255, color >> 8 & 255, color & 255);
			Random rand = src.level.random;
			for (int i = 0; i < 15; i++) {
				double velX = Mth.nextDouble(rand, -0.15, 0.15);
				double velY = Mth.nextDouble(rand, -0.15, 0.15);
				double velZ = Mth.nextDouble(rand, -0.15, 0.15);
				double xOff = Mth.nextDouble(rand, -0.25, 0.25);
				double yOff = Mth.nextDouble(rand, -0.2, 0.2);
				double zOff = Mth.nextDouble(rand, -0.25, 0.25);
				Minecraft.getInstance().particleEngine.createParticle(data, x + xOff, y + yOff, z + zOff, velX, velY, velZ);
			}
		}
		if (type == 1) { //Type 1: Portal idle particles, called every second from the portal itself.
			GatewayParticle.Data data = new GatewayParticle.Data(color >> 16 & 255, color >> 8 & 255, color & 255);
			Random rand = src.level.random;
			for (int i = 0; i < 3; i++) {
				double velX = Mth.nextDouble(rand, -0.05, 0.05);
				double velY = Mth.nextDouble(rand, -0.1, -0.05);
				double velZ = Mth.nextDouble(rand, -0.1, 0.1);
				double xOff = Mth.nextDouble(rand, -0.15, 0.15);
				double yOff = Mth.nextDouble(rand, -0.1, 0.1);
				double zOff = Mth.nextDouble(rand, -0.15, 0.15);
				Minecraft.getInstance().particleEngine.createParticle(data, x + xOff, y + yOff, z + zOff, velX, velY, velZ);
			}

		}
	}

	public static void spawnIdleParticles(GatewayEntity gate) {
		Level level = gate.getLevel();
		Random rand = level.random;
		int color = gate.getGateway().getColor().getValue();
		GatewayParticle.Data data = new GatewayParticle.Data(color >> 16 & 255, color >> 8 & 255, color & 255);

		double x = gate.getX();
		double y = gate.getY() + gate.getBbHeight() / 2;
		double z = gate.getZ();
		float scale = gate.getGateway().getSize().getScale();

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
