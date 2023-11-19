# Description
An Endless Modifier is the core building block of an Endless Gateway, and controls what changes are applied on each endless wave.  

# Dependencies
This object references the following objects:
1. [ApplicationMode](./ApplicationMode.md)
2. [WaveEntity](./WaveEntity.md)
3. [Reward](./Reward.md)
4. [WaveModifier](./WaveModifier.md)

# Schema
```js
{
    "application_mode": ApplicationMode,  // [Mandatory] || The application mode for this modifier.
    "entities": [                         // [Optional]  || A list of additional entities that will be spawned each time the modifier is applied.
        WaveEntity
    ],
    "rewards": [                          // [Optional]  || A list of rewards that will be added to the wave rewards each tim the modifier is applied.
        Reward
    ],
    "modifiers": [                        // [Optional]  || A list of wave modifiers that will be applied to all entities each time the modifier is applied.
        WaveModifier
    ],
    "max_wave_time": integer,             // [Optional]  || The time (in ticks) that this modifier will add/remove to/from the max wave time.
    "setup_time": integer                 // [Optional]  || The time (in ticks) that this modifier will add/remove to/from the setup time.
}
```

One of the three optional components must be present for a modifier to be valid.

# Examples
An endless modifier that adds three blazes and five blaze loot rolls every 3 waves, up to 15 max applications.
```json
{
    "application_mode": {
        "type": "gateways:after_every_n_waves",
        "waves": 3,
        "max": 15
    },
    "entities": [{
        "entity": "minecraft:blaze",
        "count": 3
    }],
    "rewards": [{
        "type": "gateways:entity_loot",
        "entity": "minecraft:blaze",
        "rolls": 5
    }]
}
```