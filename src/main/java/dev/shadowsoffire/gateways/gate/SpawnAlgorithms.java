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
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.phys.Vec3;

public class SpawnAlgorithms {

    public interface SpawnAlgorithm {
        /**
         * Spawns a Wave Entity, including all passengers.
         *
         * @param level   The level the entity will be spawned in.
         * @param pos     The block position of the Gateway entity.
         * @param gate    The controlling Gateway entity.
         * @param toSpawn The wave entity being spawned.
         * @return The newly-created entity, or null, if the entity could not be spawned or a suitable spawn location could not be found.
         */
        @Nullable
        Vec3 spawn(ServerLevel level, Vec3 pos, GatewayEntity gate, WaveEntity toSpawn);
    }

    public static final BiMap<ResourceLocation, SpawnAlgorithm> NAMED_ALGORITHMS = HashBiMap.create();

    static {
        NAMED_ALGORITHMS.put(Gateways.loc("open_field"), SpawnAlgorithms::openField);
        NAMED_ALGORITHMS.put(Gateways.loc("inward_spiral"), SpawnAlgorithms::inwardSpiral);
    }

    public static final Codec<SpawnAlgorithm> CODEC = ExtraCodecs.stringResolverCodec(sa -> NAMED_ALGORITHMS.inverse().get(sa).toString(), key -> NAMED_ALGORITHMS.get(new ResourceLocation(key)));
    public static final int MAX_SPAWN_TRIES = 15;

    /**
     * The Open Field Algorithm selects random spawn positions within the spawn radius, and places entities on the ground.<br>
     * This algorithm will likely fail if the working area is not mostly empty.<br>
     */
    @Nullable
    public static Vec3 openField(ServerLevel level, Vec3 pos, GatewayEntity gate, WaveEntity toSpawn) {

        double spawnRange = gate.getGateway().spawnRange();

        int tries = 0;
        double x = pos.x() + (-1 + 2 * level.random.nextDouble()) * spawnRange;
        double y = pos.y() + level.random.nextInt(3) - 1;
        double z = pos.z() + (-1 + 2 * level.random.nextDouble()) * spawnRange;
        while (!level.noCollision(toSpawn.getAABB(x, y, z)) && tries++ < MAX_SPAWN_TRIES) {
            x = pos.x() + (level.random.nextDouble() - level.random.nextDouble()) * spawnRange + 0.5D;
            y = pos.y() + level.random.nextInt(3 * (int) gate.getGateway().size().getScale()) + 1;
            z = pos.z() + (level.random.nextDouble() - level.random.nextDouble()) * spawnRange + 0.5D;
        }

        while (level.getBlockState(BlockPos.containing(x, y - 1, z)).isAir() && y > level.getMinBuildHeight()) {
            y--;
        }

        while (!level.noCollision(toSpawn.getAABB(x, y, z))) {
            y++;
        }

        if (gate.distanceToSqr(x, y, z) > gate.getGateway().getLeashRangeSq()) return null;

        if (level.noCollision(toSpawn.getAABB(x, y, z))) return new Vec3(x, y, z);

        return null;
    }

    /**
     * The Inward Spiral Algorithm selects random spawn positions within the spawn radius, but reduces the spawn radius each attempt.<br>
     * On the final attempt, the wave entity will attempt to spawn exactly on the position of the Gateway itself.<br>
     * Spawned entities will still be placed on the ground.<br>
     * This algorithm will work in most scenarios, but may enable non-ideal cheese mechanisms such as dropping all wave entities into a mob grinder.
     */
    @Nullable
    public static Vec3 inwardSpiral(ServerLevel level, Vec3 pos, GatewayEntity gate, WaveEntity toSpawn) {

        double spawnRange = gate.getGateway().spawnRange();

        int tries = 0;
        double x = pos.x() + (-1 + 2 * level.random.nextDouble()) * spawnRange;
        double y = pos.y() + level.random.nextInt(3) - 1;
        double z = pos.z() + (-1 + 2 * level.random.nextDouble()) * spawnRange;
        while (!level.noCollision(toSpawn.getAABB(x, y, z)) && tries++ < MAX_SPAWN_TRIES) {
            float scaleFactor = (MAX_SPAWN_TRIES - 1 - tries) / (float) MAX_SPAWN_TRIES;
            x = pos.x() + scaleFactor * (level.random.nextDouble() - level.random.nextDouble()) * spawnRange + 0.5D;
            y = pos.y() + scaleFactor * level.random.nextInt(3 * (int) gate.getGateway().size().getScale()) + 1;
            z = pos.z() + scaleFactor * (level.random.nextDouble() - level.random.nextDouble()) * spawnRange + 0.5D;
        }

        while (level.getBlockState(BlockPos.containing(x, y - 1, z)).isAir() && y > level.getMinBuildHeight()) {
            y--;
        }

        while (!level.noCollision(toSpawn.getAABB(x, y, z))) {
            y++;
        }

        if (gate.distanceToSqr(x, y, z) > gate.getGateway().getLeashRangeSq()) return null;

        if (level.noCollision(toSpawn.getAABB(x, y, z))) return new Vec3(x, y, z);

        return null;
    }

}
