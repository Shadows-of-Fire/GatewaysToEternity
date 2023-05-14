## 3.2.0
* Updated to Placebo 7.2.0
* Added some more color to the tooltips.
* The "Hold Alt to show Failures" text will no longer show if the Gateway has no failure penalties.
* Gateways will now properly fail (instead of stalling) when a mob is removed but not killed.
* Gateways now have three new fields: "spawn_algorithm", "player_damage_only", and "allow_discarding".
  * "spawn_algorithm" allows for selecting how Wave Entity spawn positions are selected.
  * "player_damage_only" can be used to enforce that the Wave Entities are only hurt by player damage (excluding fake players).
  * "allow_discarding" can be used to enable discards (entity removals that are not kills) to count as kills. This is enabled by default for the creeper gates.
  * See the schema for more information.
* The "allow_discarding" and "player_damage_only" properties will be reflected in the gate tooltip. The "spawn_algorithm" property will not.
* The JSON Schema has been updated to reflect 3.2.0, and is available [here](https://gist.github.com/Shadows-of-Fire/a45a2742b7a0842c50738d3df3ce8148).

## 3.1.1
* Forward port of 2.2.1

## 3.1.0
* Fixed an issue where gateways would fail to spawn wave entities or would spawn them too far away.
* Revamped Gate Pearl tooltips! Waves can now be scrolled through manually, and the display for elements has been adjusted.
* Added Failures, actions that trigger when the gateway is failed. These allow pack makers to increase the risk/reward ratio.
* Changed how command rewards/failures are displayed on gate tooltips. Instead of showing the raw command, a translation key must be provided.
* Gate entities that teleport will no longer be able to teleport outside of the gateway boundary.

## 3.0.2
* Forward-ported all changes from 2.1.5

## 3.0.1
* Added forge events that fire on various gateway actions.

## 3.0.0
* Updated to MC 1.19.2

## 2.2.1
* Increased the rate at which items are dropped if the number of dropped items is very high.
* Added a potential fix for infinite fog.
* Gateways will now properly fail if a gate entity is teleported to another dimension.
* Stack List Rewards will no longer crash.
* Chanced Rewards will no longer display incorrectly.

## 2.2.0
* Backported all changes from 3.1.0

## 2.1.5
* alin529: Added chinese translation
* Wave entities will now always attempt to spawn on the surface, and will not spawn midair.
* Waves will no longer be skipped if the gate is unloaded.
* Gateway particles will now show through walls to indicate the position of wave entities.

## 2.1.4
* Added forge events that fire on various gateway actions. (backport of 3.0.1)

## 2.1.3
* Added the "command" reward type, which allows for execution of a command as a reward.
  * Details are in the JSON schema document.
* Made the /open_gateway command accept an entity selector instead of a blockpos for the target location.

## 2.1.2
* Fixed spawned entities reporting their spawn reason as natural instead of spawner.

## 2.1.1
* Updated to Placebo 6.6.2

## 2.1.0
* Fixed passengers in wave nbt data.
* Added support for other mods to add sub-serializers to the wave entity types.
* Added an optional leash_range param to the Gateway definition.

## 2.0.2
* Added a "Gates Defeated" statistic.
* Fixed /open_gateway not accepting decimal positions.
* Darkosto: Fixed sounds being in stereo.
* Ignored empty stacks in reward processing, so the sound doesn't play erroneously.

## 2.0.1
* Fixed LootTableReward not supporting more than 1 roll.

## 2.0.0
* Rewrote the entire mod, updated all the textures.  Basically a brand new mod.
* Updated to 1.18.2.

## 1.1.0
* Added the /open_gateway command which allows spawning a gateway at a position.
* Fixed a bug where a gateway would crash if the summoner ID was unset.