# Description
Boss Event Settings hold flags relating to the boss bar used by Gateways. Every field on this object is optional, meaning that `{}` is a legal definition of a Boss Event Settings object.

# Schema
```js
{
    "mode": "string",  // [Optional] || The drawing mode of the boss bar. Either "boss_bar" or "name_plate". Default value = "boss_bar".
    "fog": boolean     // [Optional] || If the current mode is "boss_bar", this controls if fog is enabled. Has no effect when the mode is "name_plate". Default value = true.
}
```