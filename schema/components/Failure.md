# Description
A Failure is an event that occurs when a Gateway fails for any reason.  

# Dependencies
This object references the following objects:
1. [WaveEntity](./WaveEntity.md)

# Subtypes
Failures are subtyped, meaning each subtype declares a `"type"` key and its own parameters.

## Explosion Failure
Triggers an explosion at the position of the Gateway.

### Schema
```js
{
    "type": "gateways:explosion",
    "strength": float,             // [Mandatory] || Strength of the explosion. Creepers are 3, TNT is 4.
    "fire": boolean,               // [Mandatory] || If the explosion will cause fire to spawn.
    "block_damage": boolean        // [Mandatory] || If the explosion will damage blocks.
}
```

## Mob Effect Failure
Applies an effect instance to all nearby players.

### Schema
```js
{
    "type": "gateways:mob_effect",
    "effect": "string",            // [Mandatory] || Registry name of the mob effect.
    "duration": integer,           // [Mandatory] || Duration, in ticks, of the effect.
    "amplifier": integer           // [Mandatory] || Effect amplifier. A value of zero corresponds to level 1.
}
```

## Summon Failure
Summons a wave entity using the spawn algorithm of the Gateway.

### Schema
```js
{
    "type": "gateways:summon",
    "entity": WaveEntity           // [Mandatory] || The entity to summon.
}
```

## Command Failure
Executes a command when the Gateway fails. The keyphrase `<summoner>` will be replaced with the summoning player's name before command execution.  
If the summoner is absent, the closest player will be used instead.

The command will be executed as the gateway entity with a permission level of 2. If you need to execute it as the player, use `execute as <summoner> run ...`.

### Schema
```js
{
    "type": "gateways:command",
    "command": "string",           // [Mandatory] || The command string, without a leading slash.
    "desc": "string"               // [Mandatory] || Lang Key (or english text) which will be used to display the failure in the tooltip.
}
```

## Chanced Failure
Provides a chance to trigger any other failure.

### Schema
```js
{
    "type": "gateways:chanced",
    "chance": float,               // [Mandatory] || The chance the reward is granted, in the range [0, 1].  0.5 is 50%
    "failure": Failure             // [Mandatory] || The underlying failure.
}
```