# Description
A Size is a preset for how large a Gateway is. It controls the Gate Pearl texture, base spawn range, and hitbox of the Gateway.

# Schema
```js
"string": // [Mandatory] || The size. One of "small", "medium", or "large".
```

# Sizes

Each size has two properties: a scale, and an AABB. The scale changes the following things:
* The size of the rendered Gateway.
* Y-level selection variance when spawning mobs.
* Idle spawn particle range.
* Radius at which completion items are spawned.

The AABB controls the hitbox of the Gateway, which is mostly used in exclusion zone checking against other Gateways. 