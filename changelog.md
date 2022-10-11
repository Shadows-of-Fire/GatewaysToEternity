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

## 1.0.0
* Backported version 2.1.3 to Minecraft 1.16.5. Makes the chronology look a little weird, doesn't it?
  * Commissioned by adam98991