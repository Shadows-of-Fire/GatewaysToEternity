# Description
A Wave Entity is the information needed to spawn one or more entities of a single type.  
It is used in most places that Gateways attempts to spawn an entity for one reason or another.  

# Dependencies
This object references the following objects:
1. [CompoundTag](../../../../../Placebo/blob/-/schema/CompoundTag.md)
2. [WaveModifier](./WaveModifier.md)

# Subtypes
Wave Entities are subtyped, meaning each subtype declares a `"type"` key and its own parameters.
If no type key is provided, `"gateways:standard"` is used.

## Standard Wave Entity

### Schema
```js
{
    "type": "gateways:standard",
    "entity": "string",       // [Mandatory] || Registry name of the entity being spawned.
    "desc": "string",         // [Optional]  || Lang key used when this wave entity is displayed in a tooltip. If absent, the default lang key of the entity type used.
    "nbt": CompoundTag        // [Optional]  || NBT data that will be applied to the entity. Default value = empty NBT.
    "modifiers": [            // [Optional]  || A list of wave modifiers for the entity. Default value = no modifiers.
        WaveModifier
    ],
    "count": integer          // [Optional]  || The number of copies of this entity to spawn. Default value = 1.
}
```

# Examples
The Necrotic Farmer used by the Gateway of the Emerald Grove.
```json
{
    "entity": "minecraft:zombie_villager",
    "desc": "name.gateways.necrotic_farmer",
    "nbt": {
        "CustomNameVisible": 1,
        "CustomName": "{\"translate\":\"name.gateways.necrotic_farmer\",\"color\":\"red\"}",
        "VillagerData": {
            "profession": "minecraft:farmer"
        }
    },
    "modifiers": [
        {
            "type": "gateways:gear_set",
            "gear_set": "gateways:iron"
        }
    ],
    "finalize_spawn": false
}
```