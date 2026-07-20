# Description
A Wave Modifier is something that can be applied to a wave entity to modify it.  

# Dependencies
This object references the following objects:
1. [ChancedEffectInstance](../../../../../Placebo/blob/-/schema/ChancedEffectInstance.md)
2. [RandomAttributeModifier](../../../../../Placebo/blob/-/schema/RandomAttributeModifier.md)
3. [GearSet](../../../../../Placebo/blob/-/schema/GearSet.md)

# Subtypes
Wave Modifiers are subtyped, meaning each subtype declares a `"type"` key and its own parameters.

## Mob Effect Modifier
Applies an effect instance to the targets.

### Schema
```js
{
    "type": "gateways:mob_effect",
    "effect": ChancedEffectInstance  // [Mandatory] || The effect instance being applied.
}
```

This modifier uses the constant mode of `ChancedEffectInstance`.

## Attribute Modifier
Applies an attribute modifier to the targets.

### Schema
```js
{
    "type": "gateways:attribute",
    RandomAttributeModifier          // [Mandatory] || The attribute modifier being applied.
}
```

Note: the `RandomAttributeModifier` is inlined, meaning all of its fields are top-level keys in this object.

This modifier uses the constant and generated modes of `RandomAttributeModifier` - there is no `"modifier_id"` key (it is ignored if present).
Each application creates a unique id, so modifiers that apply multiple times (e.g. stacking [Endless Modifiers](./EndlessModifier.md)) stack instead of conflicting.

## Gear Set Modifier
Applies a gear set to the targets.

### Schema
```js
{
    "type": "gateways:gear_set",
    "gear_set": "string",            // [Mandatory] || Registry name of the gear set to use.
}
```

The gear set used should be deterministic, although this is not enforced.  
A translation key must be provided in a lang file for this modifier to appear properly in tooltips.  
Wave modifiers attached to specific entities are not shown in tooltips, but those used in Waves or Endless Modifiers are.