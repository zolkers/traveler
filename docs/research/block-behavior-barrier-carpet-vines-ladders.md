# Special Block Behavior Research: Barrier, Carpet, Vines, Ladders

Research date: 2026-06-14

Scope: Minecraft Java Edition behavior for Traveler's Fabric-first 1.21.11 target. Bedrock notes are included only where they prevent wrong assumptions. The goal is to turn external block behavior into core-friendly movement contracts, not raw penalties.

Source count: 26 web sources are listed at the end.

## Executive Findings

- Barrier is a full solid collision block that renders invisible. It should block horizontal traversal like a full block, may physically support standing on top, and should be marked as an invisible/debug-sensitive surface because a human player cannot visually confirm it.
- Carpet is a 1/16 block-thick overlay. It changes floor height and clearance, prevents mob spawning, and creates important edge cases on fences, walls, stacked carpets, and low ceilings.
- Vines are non-solid climbable vegetation. They do not provide a standing collision surface, but they can slow movement, reset fall damage, allow vertical climbing, and allow a sneak-hang behavior.
- Ladders are wall-attached climbables with a thin collision shape, facing state, and Java waterlogging. They can catch falls when the lower body enters the ladder block, but falling onto the narrow top face still behaves like landing on a block.
- Minecraft mob pathfinding is not player pathfinding. Several carpet/ladder/vine quirks are mob-AI artifacts; Traveler should model player physics and navigation intent, while keeping those cases in tests only when they affect player body clearance or collision.

## Traveler Architecture Notes

The current project direction in `project.md` says Minecraft common should classify/adapt, while core should decide. These blocks fit that model:

- Minecraft common should detect exact vanilla block classes, state properties, collision/outline shapes, tags, and waterlogged state.
- Core should expose behavior objects such as `BarrierBlockBehavior`, `CarpetBlockBehavior`, `LadderBlockBehavior`, and `VineBlockBehavior`.
- Smoothing must preserve any node where the route enters, climbs, exits, hangs on, drops through, or lands near a climbable/special block.
- Debug render should color invisible obstructions, thin overlays, climbable action surfaces, and route-preserved special nodes differently.

## Barrier

Observed behavior:

- Barrier is an invisible block used to create solid boundaries.
- It is not obtainable by ordinary survival play and is not breakable in survival.
- It has extreme blast resistance and cannot be destroyed by explosions.
- It interacts with blocks and mobs as solid, can support dependent blocks, can suffocate mobs, and mobs cannot spawn on it.
- It does not visually connect to fences, panes, or similar connector blocks.
- It does not block light or beacons.
- It cannot be pushed or pulled by pistons.
- Bedrock has waterlogged barrier behavior; Java should not inherit that assumption unless the target mapping proves it.

Traveler implications:

- Resolve as a distinct behavior key, even if movement evaluation initially delegates to full-block movement.
- Treat it as solid for collision and line-of-walk.
- Allow standing on top only if the route policy permits invisible support. The safer default is to allow physical standability but add an avoidance cost or debug anomaly because it is a surprising player route.
- Never dig/break it as part of routing.
- Do not consider visual connector state when deciding passability.

Recommended tests:

- Barrier wall blocks adjacent and smoothed routes.
- Barrier floor is physically standable but flagged as invisible/special.
- Barrier cannot be used as a breakable obstacle.
- Barrier does not become passable because it is transparent to light.
- Barrier next to fence/pane remains solid even though visuals do not connect.
- Piston/movement update assumptions do not remove or move barriers in snapshots.

## Carpet

Observed behavior:

- Carpet is a thin block, currently with a 1/16 block hitbox height in Java history.
- Carpets can be placed on many supports, including non-solid blocks, and can be placed over other carpets.
- Carpets let light through and stop mobs from spawning on top.
- Hoppers can collect items through carpet.
- Carpet can be placed on fences and walls so players can jump onto them while many mobs still avoid/cannot plan through the fence/wall setup.
- Double carpet and carpet in two-block-high spaces have known mob pathfinding and clearance quirks. These are especially relevant for villagers and 1.95-block-tall mobs, but they are not the same as player movement.
- Carpet can replace or break some plant/vine cases when placed, so snapshot classification should read the final block state, not infer from the target support alone.

Traveler implications:

- Model carpet as a thin walkable overlay when it has a supporting collision below.
- Floor height must include the carpet collision top, usually support top plus 1/16.
- Clearance checks must subtract the carpet height. This matters in two-block-high spaces and under slabs/trapdoors.
- Carpet on fences/walls is not a normal one-block step. The collision stack can exceed ordinary step-up height and likely needs a jump/special transition if allowed.
- Stacked carpets should be handled as a stack of thin collision boxes, not as air and not as a full block.
- Do not import mob-AI "mob proofing" as a player movement rule.

Recommended tests:

- Carpet on a full block produces a walkable surface at `y + 1.0625`.
- Carpet in a two-block tunnel reduces body clearance.
- Double carpet remains physically walkable for the player profile but consumes extra clearance.
- Carpet over fence/wall is not treated like flat ground.
- Carpet over non-solid support is rejected unless the shape stack supplies standability.
- Carpet does not create a route through a headroom violation.

## Vines

Observed behavior:

- Vines are climbable, non-solid vegetation blocks.
- In Java, vines are not waterloggable; Bedrock differs.
- Vines can be placed on sides of full-cube, entity-blocking blocks and on the bottom side of a block.
- Multiple vine faces can occupy the same block space.
- Vines can be climbed by standing next to them and holding jump. If there is a solid block behind the vines, forward movement can also climb.
- Vines slow/cancel sprint movement.
- Vines absorb/reset fall damage, even without a solid surface nearby.
- Sneaking on vines lets the player hang, even when vines are not adjacent to a solid surface.
- Blocks can replace vines in the same block space.

Traveler implications:

- Vines are not standing surfaces; shape may be empty for collision.
- Treat vines as passable for ordinary body clearance but not neutral: they affect speed and fall handling.
- Add climbable route actions for entering, ascending, descending, hanging, and exiting.
- Preserve vine entry/exit/fall-catch nodes during smoothing.
- Directional faces matter. A vine on the wrong side is not equivalent to a ladder facing the player.
- Hanging vines need a route action distinct from wall-backed climb: sneak-hang/fall-catch is valid even when forward-climb is not.

Recommended tests:

- Vine block is passable horizontally but applies a special behavior key.
- Wall-backed vine can generate vertical climb edges when player controls support it.
- Hanging vine can catch a fall or hold with sneak but does not create a normal standing node.
- Multi-face vine resolves all attached directions.
- Unsupported/removable vine states are read from the snapshot and not inferred.
- Smoothing cannot cut from pre-vine to post-vine without preserving the climb/catch action node.

## Ladders

Observed behavior:

- Ladders are wall-attached climbable blocks with `facing` state.
- Java ladders are waterloggable in modern versions.
- A ladder affects the player when the lower body is inside the ladder block.
- Pushing against a wall while inside the ladder block moves the player upward; in a 1x1 shaft, any movement key can climb.
- Holding jump while occupying the shaft can also ascend.
- Downward speed is clamped to ladder descent speed, and entering the ladder area during a fall can prevent fall damage.
- Falling onto the narrow top surface of a ladder still incurs normal fall damage.
- Sneaking holds the player on the ladder.
- Mobs can climb ladders if pushed or if the ladder is directly in their path, but they generally do not intentionally use ladders as planned routes.
- Java has a special case where an opened trapdoor directly above a ladder also becomes climbable.
- A player on a slab in front of a ladder may still need to jump to get onto it despite the half-block height difference.

Traveler implications:

- Ladders need a behavior with facing, waterlogged, and climb actions.
- Do not model ladder as just a thin full block. It has collision and climb logic.
- Entry positioning matters: the player must put the lower body in the ladder block, not merely stand near the block center.
- Vertical ladder edges should have their own action metadata and probably their own navigation steering mode.
- Ladder top landings and ladder-face exits should be preserved through smoothing.
- Java waterlogged ladders should stay a separate case from Bedrock no-climb behavior.
- Trapdoor-over-ladder is a cross-block behavior; the ladder resolver may need nearby-state context.

Recommended tests:

- Four facings produce correct entry side and climb axis.
- 1x1 ladder shaft can ascend by jump/forward-style control.
- Side entry with continued perpendicular motion does not become a stable climb.
- Falling into ladder block is a fall-catch action; falling onto ladder top is not.
- Sneak-hold creates a valid pause/hang state.
- Waterlogged Java ladder keeps a distinct behavior and fluid flag.
- Open trapdoor directly above ladder extends climbability.
- Slab-in-front transition requires jump/action metadata.

## Cross-Block Requirements

- Add behavior keys before adding ad hoc route penalties.
- Extend resolver order carefully: air, carpet/thin overlays, ladder/vine climbables, slab/stair/full block, then fallback.
- Use `BlockShape` from Minecraft collision shape, but do not let shape alone decide everything. Vines may have no collision but still have movement behavior.
- Add a `ClimbableBlockBehavior` abstraction only if ladder and vine share enough action logic. Keep per-block details for facing, support, collision, and waterlogging.
- Surface search should distinguish:
  - passable body volume;
  - walkable standing surface;
  - climbable action volume;
  - fall-damage-resetting volume;
  - invisible solid obstruction.
- Smoothing must preserve behavior changes and action nodes. This is already aligned with the current `SurfaceSmoothingPolicy` direction.
- Debug output should report why a block is special: thin surface, climbable, invisible solid, fall-catch, waterlogged, or clearance-reducing.

## Implementation Sketch

Suggested core keys:

```text
BARRIER
CARPET
LADDER
VINE
```

Suggested behavior classes:

```text
BarrierBlockBehavior
CarpetBlockBehavior
LadderBlockBehavior(facing)
VineBlockBehavior(faces, hasCeilingAttachment)
```

Suggested route metadata:

```text
WALK_THIN_SURFACE
CLIMB_ENTER
CLIMB_ASCEND
CLIMB_DESCEND
CLIMB_EXIT
FALL_CATCH
SNEAK_HOLD
INVISIBLE_SOLID
```

Suggested resolver responsibilities:

- `BarrierBlockBehaviorResolver`: match `Blocks.BARRIER`; keep Java waterlogging assumptions out unless present in mapped state.
- `CarpetBlockBehaviorResolver`: match `CarpetBlock` or wool-carpet tag; preserve exact shape height.
- `LadderBlockBehaviorResolver`: match `LadderBlock`; read `FACING` and `WATERLOGGED`.
- `VineBlockBehaviorResolver`: match `VineBlock`; read directional face properties and `UP`.

## Source Index

1. Minecraft Wiki - Barrier: https://minecraft.fandom.com/wiki/Barrier
2. Minecraft Wiki - Carpet: https://minecraft.fandom.com/wiki/Carpet
3. Minecraft Wiki - Vines: https://minecraft.wiki/w/Vines
4. Minecraft Wiki/Fandom - Vines: https://minecraft.fandom.com/wiki/Vines
5. Minecraft Wiki/Fandom - Ladder: https://minecraft.fandom.com/wiki/Ladder
6. Minecraft Wiki/Fandom - Tag: https://minecraft.fandom.com/wiki/Tag
7. Minecraft Wiki/Fandom - Hitbox: https://minecraft.fandom.com/wiki/Hitbox
8. Minecraft.net - Taking Inventory: Ladder: https://www.minecraft.net/en-us/article/taking-inventory-ladder
9. Minecraft.net - Taking Inventory: Carpet: https://www.minecraft.net/en-us/article/carpet
10. Minecraft.net - Taking Inventory: Vines: https://www.minecraft.net/en-us/article/taking-inventory--vines
11. Yarn 1.21.11 - LadderBlock API: https://maven.fabricmc.net/docs/yarn-1.21.11%2Bbuild.3/net/minecraft/block/LadderBlock.html
12. Yarn 1.21.11 - VineBlock API: https://maven.fabricmc.net/docs/yarn-1.21.11%2Bbuild.3/net/minecraft/block/VineBlock.html
13. Yarn 1.21.11 - CarpetBlock API: https://maven.fabricmc.net/docs/yarn-1.21.11%2Bbuild.3/net/minecraft/block/CarpetBlock.html
14. Yarn 1.21.11 - BarrierBlock API: https://maven.fabricmc.net/docs/yarn-1.21.11%2Bbuild.3/net/minecraft/block/BarrierBlock.html
15. Minecraft Parkour Wiki - Ladders and Vines: https://www.mcpk.wiki/wiki/Ladders_and_Vines/en
16. Minecraft Parkour Wiki - Version Differences: https://www.mcpk.wiki/wiki/Version_Differences
17. PrismarineJS - mineflayer-pathfinder README: https://github.com/PrismarineJS/mineflayer-pathfinder
18. PrismarineJS - mineflayer-pathfinder movements source: https://raw.githubusercontent.com/PrismarineJS/mineflayer-pathfinder/master/lib/movements.js
19. PrismarineJS - mineflayer-pathfinder issue 41, climbing vines support: https://github.com/PrismarineJS/mineflayer-pathfinder/issues/41
20. PrismarineJS - minecraft-data README: https://github.com/PrismarineJS/minecraft-data
21. PrismarineJS - minecraft-data issue 258, block collision boxes: https://github.com/PrismarineJS/minecraft-data/issues/258
22. Mojira MC-176379 - ladder/vine fall distance reset: https://bugs.mojang.com/browse/MC-176379
23. Mojira MC-177062 - collision loss in scaffolding/ladders/vines: https://bugs.mojang.com/browse/MC-177062
24. Mojira MC-175387 - mobs and ladder pathfinding: https://bugs.mojang.com/browse/MC-175387
25. Mojira MC-97799 - carpet and mob movement in two-block-high spaces: https://bugs.mojang.com/browse/MC/issues/MC-97799
26. Mojira MC-109108 - vines and mob fall-damage setup: https://bugs.mojang.com/browse/MC-109108
