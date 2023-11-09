# Description
A Gateway is the core object you will create when working with Gateways to Eternity.

# Dependencies
This object references the following objects:
1. [Size](./components/Size.md)
2. [Color](../../../../Placebo/blob/-/schema/Color.md)
3. [WaveEntity](./components/WaveEntity.md)
4. [Wave](./components/Wave.md)
5. [Reward](./components/Reward.md)
6. [Failure](./components/Failure.md)
7. [SpawnAlgorithm](./components/SpawnAlgorithm.md)
8. [GateRules](./components/GateRules.md)
9. [BossEventSettings](./components/BossEventSettings.md)
10. [EndlessModifier](./components/EndlessModifier.md)

# Subtypes
Gateways are subtyped, meaning each subtype declares a `"type"` key and its own parameters.

## Normal Gateways
A Normal (or "Classic") Gateway defines a predefined list of waves, modifiers, rewards, and failure penalties.

### Schema
```js
{
    "size": Size,                        // [Mandatory] || The size of the Gateway.
    "color": Color,                      // [Mandatory] || The color of the Gateway.
    "waves": [                           // [Mandatory] || The list of waves for this Gateway.
        Wave
    ],
    "rewards": [                         // [Optional]  || Rewards that will be granted upon completion of the entire gateway. Default value = empty list.
        Reward
    ],
    "failures": [                        // [Optional]  || Failure penalties that will be applied if the Gateway is not completed. Default value = empty list.
        Failure
    ]
    "spawn_algorithm": SpawnAlgorithm,   // [Optional]  || The Spawn Algorithm to use. Default value = "gateways:open_field".
    "rules": GateRules,                  // [Optional]  || Potential rule edits for this Gateway. Default value = The default GateRules object.
    "boss_event": BossEventSettings,     // [Optional]  || Potential boss event settings for this Gateway. Default value = The default BossEventSettings object.
}
```

## Endless Gateways
An Endless Gateway defines a base waves and a series of Endless Modifiers, which will augment the Gateway as it runs.  
The Gateway will continue running until a wave fails, at which point it will close.

### Schema
```js
{
    "size": Size,                        // [Mandatory] || The size of the Gateway.
    "color": Color,                      // [Mandatory] || The color of the Gateway.
    "base_wave": Wave,                   // [Mandatory] || The base wave for this Gateway. It will be augmented by the provided Endless Modifiers as waves pass.
    "modifiers": [                       // [Mandatory] || The modifiers, which define the rules for how the wave will change over time.
        EndlessModifier
    ],
    "failures": [                        // [Optional]  || Failure penalties that will be applied if the Gateway is not completed. Default value = empty list.
        Failure
    ]
    "spawn_algorithm": SpawnAlgorithm,   // [Optional]  || The Spawn Algorithm to use. Default value = "gateways:open_field".
    "rules": GateRules,                  // [Optional]  || Potential rule edits for this Gateway. Default value = The default GateRules object.
    "boss_event": BossEventSettings,     // [Optional]  || Potential boss event settings for this Gateway. Default value = The default BossEventSettings object.
}
```

Note that it is almost guaranteed that an Endless Gateway fails at some point (unless it is kept open until the end of time), so failures should be added cautiously since users may not always avoid them.