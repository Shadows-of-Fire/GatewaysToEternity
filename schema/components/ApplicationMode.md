# Description
An Application Mode is utilized by an Endless Modifier to specify how and when it applies during an Endless Gateway.

# Subtypes
Application Modes are subtyped, meaning each subtype declares a `"type"` key and its own parameters.

## After Wave Mode
Adds the modifier(s) on the specified wave, remaining for all subsequent waves.

### Schema
```js
{
    "type": "gateways:after_wave",
    "wave": integer      // [Mandatory] || The wave on which the modifier(s) are applied. The modifier remains applied on subsequent waves.
}
```

## After Every N Waves Mode
Applies the modifier(s) once every N waves, stacking with prior applications, up to M total applications.

### Schema
```js
{
    "type": "gateways:after_every_n_waves",
    "waves": integer,    // [Mandatory] || The number of waves that must elapse before the modifier(s) are applied and reapplied.
    "max": integer       // [Mandatory] || The maximum number of times the modifier(s) will be applied.
}
```

## Only On Wave Mode
Applies the modifier only on the specified wave, and not on subsequent waves.

### Schema
```js
{
    "type": "gateways:only_on_wave",
    "wave": integer      // [Mandatory] || The wave on which the modifier(s) apply.
}
```

## Only On Every N Waves Mode
Applies the modifier once every N waves, but not on any others.

### Schema
```js
{
    "type": "gateways:after_every_n_waves",
    "waves": integer,    // [Mandatory] || The number of waves that must elapse between applications.
}
```