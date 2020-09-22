package shadows.gateways.util;

import net.minecraft.util.math.Vec3d;

/**
 * Helper class for creating a point to point line in 3d space.
 */
public class LinearEquation {

	protected Vec3d src;
	protected Vec3d dest;
	protected Vec3d vec;

	public LinearEquation(Vec3d src, Vec3d dest) {
		this.src = src;
		this.dest = dest;
		this.vec = src.subtract(dest);
	}

	public Vec3d step(double step) {
		return new Vec3d(dest.getX() + step * vec.getX(), dest.getY() + step * vec.getY(), dest.getZ() + step * vec.getZ());
	}

	public Vec3d getSrc() {
		return src;
	}

	public Vec3d getDest() {
		return dest;
	}

}