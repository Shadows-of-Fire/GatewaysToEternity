# Description
A Wave contains all the information needed for a Gateway to spawn a single wave of mobs, including the entities, modifiers, rewards, and timers.

# Dependencies
This object references the following objects:
1. [WaveEntity](./WaveEntity.md)
2. [WaveModifier](./WaveModifier.md)
3. [Reward](./Reward.md)

# Schema
```js
{
    "entities": [             // [Mandatory] || The list of entities that will be spawned as part of this wave. May not be empty.
        WaveEntity
    ],              
    "modifiers": [            // [Optional]  || A list of wave modifiers to be applied to all spawned entities.
        WaveModifier
    ],
    "rewards": [              // [Optional]  || A list of rewards that will be provided when this wave is completed.
        Reward
    ],
    "max_wave_time": integer  // [Mandatory] || The maximum time (in ticks) that a player may take to complete this wave.
    "setup_time": integer     // [Mandatory] || The setup time (in ticks) before this wave begins, after the last wave was completed.
}
```

# Examples
A simple wave spawning two blazes and providing the loot of three when completed.
```json
{
    "entities": [{
        "entity": "minecraft:blaze",
        "count": 2
    }],
    "rewards": [{
        "type": "gateways:entity_loot",
        "entity": "minecraft:blaze",
        "rolls": 3
    }],
    "max_wave_time": 750,
    "setup_time": 150
}
```