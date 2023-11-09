# Description
Gate Rules hold various behavioral flags common to all Gateway types. Every field on this object is optional, meaning that `{}` is a legal definition of a Gate Rules object.

# Schema
```js
{
    "spawn_range": float,              // [Optional] || The spawn range as a radius in blocks in which mobs may spawn around the gateway, from the edges of the gateway. Default value = 8.
    "leash_range": float,              // [Optional] || The distance that a wave entity may be from the center of the Gateway before out-of-bounds rules are triggered. Default value = 32.
    "allow_discarding": boolean,       // [Optional] || If entities marked as discarded are counted as valid kills. Default value = false.
    "allow_dim_change": boolean        // [Optional] || If entities marked as changed dimension are counted as valid kills. Default value = false.
    "player_damage_only": boolean,     // [Optional] || If wave entities may only be hurt by damage that is sourced to a player. Default value = false.
    "remove_mobs_on_failure": boolean  // [Optional] || If the wave entities will be removed if the Gateway is failed. Default value = true.
    "fail_on_out_of_bounds": boolean   // [Optional] || If true, when out-of-bounds rules are triggered, the Gateway will fail. If false, the entity will be re-placed using the spawn algorithm. Default value = false.
    "spacing": float                   // [Optional] || The distance that this gateway must be from another Gateway. Default value = 0.
}
```