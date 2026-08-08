# Description
A Spawn Algorithm is the mechanism by which wave entities are placed into the world by a Gateway.

# Schema
```js
"string" // [Mandatory] || The name of the spawn algorithm.
```

# Algorithms

Each spawn algorithm has its own name and placement rules.

## Open Field Algorithm
Name: `gateways:open_field`

The Open Field algorithm attempts to place the entity randomly within the Gateway's spawn range. It is recommended that you use this algorithm when you want to enforce usage of a spacious arena.

If placement fails after fifteen attempts, the Gateway will fail with the `SPAWN_FAILED` reason.

## Inward Spiral Algorithm
Name: `gateways:inward_spiral`

The Inward Spiral algorithm attempts to place the entity within the Gateway's spawn range, but reduces the spawn range as spawn attempts fail. It is recommended that you use this algorithm when space constrained,
or you want to allow using the Gateway in a small area.

If the first fourteen attempts fail, the entity will be placed at the center of the gateway. The final placement is still subject to the leash range check, so in rare cases this algorithm can also fail with the `SPAWN_FAILED` reason.