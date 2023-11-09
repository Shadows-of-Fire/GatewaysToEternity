# Description
A Spawn Algorithm is the mechanism by which wave entities are placed into the world by a Gateway.

# Schema
```js
"string": // [Mandatory] || The name of the spawn algorithm.
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

The placement of this algorithm will never fail, as if it fails the first fourteen attempts, it will place the entity at the center of the gateway.