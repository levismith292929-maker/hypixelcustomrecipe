# Datapack-to-mod migration

This work is being developed on the `merge-datapack-into-mod` branch. The existing `main` branch is intentionally left untouched until the migration is complete and verified.

## Goal

The Fabric mod must be the only runtime installation required. Features from the old datapack are being reimplemented as mod code and mod-owned data resources. Datapack advancement triggers are not part of the intended final design; item abilities will use Fabric item interaction callbacks instead.

## Datapack inventory

The source datapack currently contains 213 files:

- 92 functions: player tick/update, stats, mana, HUD, damage, AOTE/AOTV teleport raycasts, Dragon breath, Hyperion implosion, Astraea shield/orbs, Necron's Scythe, Zombie Sword, pets, and Ore Tracker scanning.
- 43 item modifiers: vanilla armor and weapon stat definitions plus attribute cleanup.
- 38 recipes: custom weapons, pets, and vanilla armor/weapon recipe overrides.
- 14 advancements: currently used as item-use and Stat Viewer triggers; replace with Java item-use handling.
- 8 tags: block, entity, and item groupings used by the systems.
- Pack metadata and packaging workflow.

## Completed in this branch

- Added `DatapackItems`, registering real `hypixel:` item IDs for AOTV, AOTD, Astraea, Hyperion, Necron's Scythe, Zombie Sword, Ore Tracker, four pets, and Pet Remover.
- Kept the existing registered `hypixel:aote` item and all existing mod items/armor.
- Migrated the 13 custom-item crafting recipes into the mod's `data/hypixel/recipe` resources and changed their outputs to the registered item IDs.
- Connected the new item registration class to mod initialization.
- Enabled the existing GitHub Actions build workflow to run on this migration branch and on pull requests to main.

## Remaining migration work

- Implement Java/Fabric item-use callbacks for AOTE, AOTV, AOTD, Astraea, Hyperion, Necron's Scythe, Zombie Sword, Ore Tracker, pets, and Pet Remover.
- Port the stats model, mana pool/regeneration, cooldown tracking, armor and weapon stat calculations, damage processing, critical hits, and strength modifiers.
- Port HUD/actionbar and mana display, plus the Stat Viewer.
- Port pet bonuses and pet effects.
- Port Ore Tracker's dimension-aware nearest-target scan.
- Port remaining item modifiers and vanilla armor/weapon recipe overrides as mod-owned behavior/resources.
- Preserve all existing Dragon/Necron armor, Star Shards/anvil upgrades, fragments, Necron Core, AOTE, and critical-hit behavior.
- Remove any dependency on the datapack's load/tick functions and advancement triggers.
- Build on Minecraft 26.2 / Java 25 and test all migrated features before merging to main.

## Verification status

The first migration slice is committed, but a complete build and gameplay verification have not yet been confirmed. Do not treat this branch as a finished replacement for the datapack yet.
