package shadows.gateways.client;

import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import shadows.gateways.net.ParticleMessage;

public class ParticleHandler {

	public static void handle(ParticleMessage msg) {
		double x = msg.x, y = msg.y, z = msg.z;
		int color = msg.color;
		int type = msg.type;
		Entity src = Minecraft.getInstance().world.getEntityByID(msg.gateId);
		if (src == null) return;
		if (type == 0) { //Type 0: Entity spawned from portal.  Spawns a cluster of particles around the entity.
			GatewayParticle.Data data = new GatewayParticle.Data(color >> 16 & 255, color >> 8 & 255, color & 255, 1);
			Random rand = src.world.rand;
			for (int i = 0; i < 15; i++) {
				double velX = MathHelper.nextDouble(rand, -0.15, 0.15);
				double velY = MathHelper.nextDouble(rand, -0.15, 0.15);
				double velZ = MathHelper.nextDouble(rand, -0.15, 0.15);
				double xOff = MathHelper.nextDouble(rand, -0.25, 0.25);
				double yOff = MathHelper.nextDouble(rand, -0.2, 0.2);
				double zOff = MathHelper.nextDouble(rand, -0.25, 0.25);
				Minecraft.getInstance().particles.addParticle(data, x + xOff, y + yOff, z + zOff, velX, velY, velZ);
			}
		}
		if (type == 1) { //Type 1: Portal idle particles, called every second from the portal itself.
			GatewayParticle.Data data = new GatewayParticle.Data(color >> 16 & 255, color >> 8 & 255, color & 255, 1);
			Random rand = src.world.rand;
			for (int i = 0; i < 3; i++) {
				double velX = MathHelper.nextDouble(rand, -0.05, 0.05);
				double velY = MathHelper.nextDouble(rand, -0.1, -0.05);
				double velZ = MathHelper.nextDouble(rand, -0.1, 0.1);
				double xOff = MathHelper.nextDouble(rand, -0.15, 0.15);
				double yOff = MathHelper.nextDouble(rand, -0.1, 0.1);
				double zOff = MathHelper.nextDouble(rand, -0.15, 0.15);
				Minecraft.getInstance().particles.addParticle(data, x + xOff, y + yOff, z + zOff, velX, velY, velZ);
			}

		}
	}
}
