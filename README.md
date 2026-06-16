# Sulfur Physics

Server-side Fabric mod for Minecraft 26.2 that uses sulfur cubes to simulate explosion block physics in a lag-friendlier way.

## Explosion behavior

- Blocks in the immediate 3x3x3 area around the explosion center are left to vanilla explosion handling, so the core blast breaks normally and can drop items normally.
- Other blocks that vanilla says would be affected by the explosion are removed with drops suppressed.
- Each removed outer block is represented by an invisible sulfur cube carrying that block's item, then pushed by the same explosion.
- When a carried block stops moving, the mod attempts to place the original block state at the cube's final position, then just below it. If neither spot can accept the block, the carried block breaks silently.

This keeps the expensive far-field blast from generating normal block drops while still giving explosions visible moving block debris.
