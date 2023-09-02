package dev.shadowsoffire.gateways.gate;

import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.shadowsoffire.placebo.codec.PlaceboCodecs;
import net.minecraft.world.entity.Entity.RemovalReason;

/**
 * Gate Rules are all of the various metadata rules for a Gateway.
 * 
 * @param spawnRange        The spawn range as a radius in blocks in which mobs may spawn around the gateway, from the center of the Gateway.
 * @param leashRange        The distance that a wave entity may be from the center of the Gateway before out-of-bounds rules are triggered.
 * @param validRemovals     The list of valid removal types, from "killed", "discarded", and "changed_dimension".
 * @param playerDamageOnly  If wave entities may only be hurt by damage that is sourced to a player.
 * @param removeOnFailure   If the wave entities will be removed if the Gateway is failed.
 * @param failOnOutOfBounds If true, when out-of-bounds rules are triggered, the Gateway will fail. If false, the entity will be re-placed using the spawn
 *                          algorithm.
 * @param spacing           The distance that this gateway must be from another Gateway.
 */
public record GateRules(double spawnRange, double leashRange, Set<RemovalReason> validRemovals, boolean playerDamageOnly, boolean removeOnFailure, boolean failOnOutOfBounds, double spacing) {

    public static GateRules DEFAULT = new GateRules(7D, 24D, Set.of(RemovalReason.KILLED, RemovalReason.DISCARDED), false, true, false, 0D);

    public static Codec<GateRules> CODEC = RecordCodecBuilder.create(inst -> inst
        .group(
            Codec.DOUBLE.optionalFieldOf("spawn_range", 7D).forGetter(GateRules::spawnRange),
            Codec.DOUBLE.optionalFieldOf("leash_range", 24D).forGetter(GateRules::leashRange),
            PlaceboCodecs.setOf(Removal.CODEC).fieldOf("valid_removal_types").forGetter(GateRules::validRemovals),
            Codec.BOOL.optionalFieldOf("player_damage_only", false).forGetter(GateRules::playerDamageOnly),
            Codec.BOOL.optionalFieldOf("remove_mobs_on_failure", true).forGetter(GateRules::removeOnFailure),
            Codec.BOOL.optionalFieldOf("fail_on_out_of_bounds", false).forGetter(GateRules::failOnOutOfBounds),
            Codec.DOUBLE.optionalFieldOf("spacing", 0D).forGetter(GateRules::spacing))
        .apply(inst, GateRules::new));

    /**
     * Limited-Scope version of {@link RemovalReason} that excludes the save-to-disk reasons.
     */
    public static enum Removal {
        KILLED,
        DISCARDED,
        CHANGED_DIMENSION;

        public static Codec<RemovalReason> CODEC = PlaceboCodecs.enumCodec(Removal.class).xmap(Removal::asReason, Removal::asRemoval);

        public RemovalReason asReason() {
            return this == KILLED ? RemovalReason.KILLED : this == DISCARDED ? RemovalReason.DISCARDED : RemovalReason.CHANGED_DIMENSION;
        }

        public static Removal asRemoval(RemovalReason reason) {
            return reason == RemovalReason.KILLED ? KILLED : reason == RemovalReason.DISCARDED ? DISCARDED : CHANGED_DIMENSION;
        }

    }

}
