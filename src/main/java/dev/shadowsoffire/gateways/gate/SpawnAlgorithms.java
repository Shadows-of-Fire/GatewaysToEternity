package dev.shadowsoffire.gateways.gate;

import javax.annotation.Nullable;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.serialization.Codec;

import dev.shadowsoffire.gateways.Gateways;
import dev.shadowsoffire.gateways.entity.GatewayEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SpawnAlgorithms {

    public interface SpawnAlgorithm {

        /**
         * Locates a viable spawn position for a wave entity.
         *
         * @param level   The level the entity will be spawned in.
         * @param pos     The block position of the Gateway entity.
         * @param gate    The controlling Gateway entity.
         * @param toSpawn The wave entity being spawned.
         * @return The spawn position, or null, if a suitable spawn location could not be found.
         */
        @Nullable
        Vec3 spawn(ServerLevel level, Vec3 pos, GatewayEntity gate, Entity toSpawn);
    }

    public static final SpawnAlgorithm OPEN_FIELD = SpawnAlgorithms::openField;
    public static final SpawnAlgorithm INWARD_SPIRAL = SpawnAlgorithms::inwardSpiral;

    private static final BiMap<ResourceLocation, SpawnAlgorithm> NAMED_ALGORITHMS = HashBiMap.create();

    static {
        register(Gateways.loc("open_field"), OPEN_FIELD);
        register(Gateways.loc("inward_spiral"), INWARD_SPIRAL);
    }

    public static final Codec<SpawnAlgorithm> CODEC = ResourceLocation.CODEC.xmap(NAMED_ALGORITHMS::get, NAMED_ALGORITHMS.inverse()::get);
    public static final int MAX_SPAWN_TRIES = 15;

    /**
     * The Open Field Algorithm selects random spawn positions within the spawn radius, and places entities on the ground.<br>
     * This algorithm will likely fail if the working area is not mostly empty.<br>
     */
    @Nullable
    private static Vec3 openField(ServerLevel level, Vec3 pos, GatewayEntity gate, Entity toSpawn) {
        double spawnRange = gate.getBbWidth() / 2 + gate.getGateway().rules().spawnRange();

        for (int i = 0; i < MAX_SPAWN_TRIES; i++) {
            // Select a position
            double x = pos.x() + (level.random.nextDouble() - level.random.nextDouble()) * spawnRange + 0.5D;
            double y = pos.y() + level.random.nextInt(3 * (int) gate.getGateway().size().getScale()) + 1;
            double z = pos.z() + (level.random.nextDouble() - level.random.nextDouble()) * spawnRange + 0.5D;

            // Find the floor
            while (level.getBlockState(BlockPos.containing(x, y - 1, z)).isAir() && y > level.getMinBuildHeight()) {
                y--;
            }

            // Move up until we actually fit, to account for uneven floors with open space above them.
            while (!noBlockCollision(level, getAABB(toSpawn, x, y, z))) {
                y++;
            }

            // Skip spots that are outside the range
            if (gate.distanceToSqr(x, y, z) > gate.getGateway().getLeashRangeSq()) continue;

            if (noBlockCollision(level, getAABB(toSpawn, x, y, z))) return new Vec3(x, y, z);
        }

        return null;
    }

    /**
     * The Inward Spiral Algorithm selects random spawn positions within the spawn radius, but reduces the spawn radius each attempt.<br>
     * On the final attempt, the wave entity will attempt to spawn exactly on the position of the Gateway itself.<br>
     * Spawned entities will still be placed on the ground.<br>
     * This algorithm will work in most scenarios, but may enable non-ideal cheese mechanisms such as dropping all wave entities into a mob grinder.
     */
    @Nullable
    private static Vec3 inwardSpiral(ServerLevel level, Vec3 pos, GatewayEntity gate, Entity toSpawn) {
        double spawnRange = gate.getBbWidth() / 2 + gate.getGateway().rules().spawnRange();

        for (int i = 0; i < MAX_SPAWN_TRIES; i++) {
            // Select a position, getting closer to the center of the gateway as failure count increases.
            float scaleFactor = (MAX_SPAWN_TRIES - 1 - i) / (float) MAX_SPAWN_TRIES;
            double x = pos.x() + scaleFactor * (level.random.nextDouble() - level.random.nextDouble()) * spawnRange + 0.5D;
            double y = pos.y() + scaleFactor * level.random.nextInt(3 * (int) gate.getGateway().size().getScale()) + 1;
            double z = pos.z() + scaleFactor * (level.random.nextDouble() - level.random.nextDouble()) * spawnRange + 0.5D;

            // Find the floor
            while (level.getBlockState(BlockPos.containing(x, y - 1, z)).isAir() && y > level.getMinBuildHeight()) {
                y--;
            }

            // Move up until we actually fit, to account for uneven floors with open space above them.
            while (!noBlockCollision(level, getAABB(toSpawn, x, y, z))) {
                y++;
            }

            // Skip spots that are outside the range
            if (gate.distanceToSqr(x, y, z) > gate.getGateway().getLeashRangeSq()) continue;

            if (noBlockCollision(level, getAABB(toSpawn, x, y, z))) return new Vec3(x, y, z);
        }

        return null;
    }

    public static AABB getAABB(Entity e, double x, double y, double z) {
        return e.getDimensions(Pose.STANDING).makeBoundingBox(x, y, z);
    }

    public static void register(ResourceLocation key, SpawnAlgorithm algo) {
        if (NAMED_ALGORITHMS.containsKey(key)) throw new UnsupportedOperationException("Attempted to register a spawn algorithm with duplicate key: " + key);
        if (NAMED_ALGORITHMS.containsValue(algo)) throw new UnsupportedOperationException("Attempted to register the spawn algorithm " + key + " twice.");
        NAMED_ALGORITHMS.put(key, algo);
    }

    public static boolean noBlockCollision(Level level, AABB pCollisionBox) {
        for (VoxelShape voxelshape : level.getBlockCollisions(null, pCollisionBox)) {
            if (!voxelshape.isEmpty()) {
                return false;
            }
        }

        return true;
    }

}
