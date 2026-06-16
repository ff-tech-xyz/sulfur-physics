package xyz.fftech.sulfurphysics;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.fabricmc.api.ModInitializer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.cubemob.SulfurCube;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SulfurPhysics implements ModInitializer {
    public static final String MOD_ID = "sulfur-physics";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /** Blocks inside the 3x3x3 around the explosion center are left to vanilla. */
    private static final int VANILLA_BREAK_CHEBYSHEV_RADIUS = 1;
    private static final int SETTLE_TICKS_REQUIRED = 8;
    private static final int MIN_AGE_BEFORE_SETTLE = 6;
    private static final int MAX_CARRY_TICKS = 20 * 12;
    private static final double STOP_SPEED_SQR = 0.0009D;

    private static final Map<UUID, MovingBlock> MOVING_BLOCKS = new ConcurrentHashMap<>();

    @Override
    public void onInitialize() {
        LOGGER.info("Sulfur Physics loaded; explosions now convert outer affected blocks into moving sulfur cubes.");
    }

    public static void replaceOuterExplosionBlocks(ServerLevel level, Vec3 center, List<BlockPos> affectedBlocks) {
        if (affectedBlocks.isEmpty()) {
            return;
        }

        BlockPos centerBlock = BlockPos.containing(center);
        int converted = 0;

        for (Iterator<BlockPos> iterator = affectedBlocks.iterator(); iterator.hasNext();) {
            BlockPos pos = iterator.next();
            if (isImmediateExplosionNeighbor(centerBlock, pos)) {
                continue;
            }

            BlockState state = level.getBlockState(pos);
            if (state.isAir()) {
                iterator.remove();
                continue;
            }

            if (spawnMovingSulfurCube(level, center, pos, state)) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                iterator.remove();
                converted++;
            }
        }

        if (converted > 0 && LOGGER.isDebugEnabled()) {
            LOGGER.debug("Converted {} outer explosion blocks into sulfur cubes at {}", converted, center);
        }
    }

    public static void tickMovingBlocks(ServerLevel level) {
        if (MOVING_BLOCKS.isEmpty()) {
            return;
        }

        List<UUID> finished = new ArrayList<>();
        for (Map.Entry<UUID, MovingBlock> entry : MOVING_BLOCKS.entrySet()) {
            MovingBlock moving = entry.getValue();
            if (!moving.dimension.equals(level.dimension())) {
                continue;
            }

            Entity entity = level.getEntity(entry.getKey());
            if (!(entity instanceof SulfurCube cube) || !entity.isAlive()) {
                finished.add(entry.getKey());
                continue;
            }

            moving.ageTicks++;
            Vec3 delta = cube.getDeltaMovement();
            if (moving.ageTicks >= MIN_AGE_BEFORE_SETTLE && delta.lengthSqr() <= STOP_SPEED_SQR) {
                moving.stillTicks++;
            } else {
                moving.stillTicks = 0;
            }

            if (moving.stillTicks >= SETTLE_TICKS_REQUIRED || moving.ageTicks >= MAX_CARRY_TICKS) {
                finishMovingBlock(level, cube, moving);
                finished.add(entry.getKey());
            }
        }

        for (UUID id : finished) {
            MOVING_BLOCKS.remove(id);
        }
    }

    private static boolean spawnMovingSulfurCube(ServerLevel level, Vec3 explosionCenter, BlockPos pos, BlockState state) {
        Item visualItem = state.getBlock().asItem();
        if (visualItem == Items.AIR) {
            return false;
        }

        SulfurCube cube = new SulfurCube(EntityTypes.SULFUR_CUBE, level);
        cube.setPos(pos.getX() + 0.5D, pos.getY() + 0.05D, pos.getZ() + 0.5D);
        cube.setSize(2, true);
        cube.setNoAi(true);
        cube.setInvulnerable(true);
        cube.setPersistenceRequired();
        cube.setDropChance(EquipmentSlot.BODY, 0.0F);
        cube.setInvisible(true);
        cube.equipItem(new ItemStack(visualItem));

        Vec3 outward = Vec3.atCenterOf(pos).subtract(explosionCenter);
        if (outward.lengthSqr() > 1.0E-6D) {
            cube.setDeltaMovement(outward.normalize().scale(0.18D));
        }

        boolean spawned = level.addFreshEntity(cube);
        if (spawned) {
            MOVING_BLOCKS.put(cube.getUUID(), new MovingBlock(level.dimension(), state));
        }
        return spawned;
    }

    private static void finishMovingBlock(ServerLevel level, SulfurCube cube, MovingBlock moving) {
        BlockPos target = BlockPos.containing(cube.position());
        if (!tryPlace(level, target, moving.blockState)) {
            tryPlace(level, target.below(), moving.blockState);
        }
        cube.discard();
    }

    private static boolean tryPlace(ServerLevel level, BlockPos target, BlockState state) {
        if (!level.isInWorldBounds(target)) {
            return false;
        }

        BlockState existing = level.getBlockState(target);
        if (!existing.canBeReplaced()) {
            return false;
        }

        if (!state.canSurvive(level, target)) {
            return false;
        }

        return level.setBlock(target, state, Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
    }

    private static boolean isImmediateExplosionNeighbor(BlockPos centerBlock, BlockPos pos) {
        return Math.abs(pos.getX() - centerBlock.getX()) <= VANILLA_BREAK_CHEBYSHEV_RADIUS
            && Math.abs(pos.getY() - centerBlock.getY()) <= VANILLA_BREAK_CHEBYSHEV_RADIUS
            && Math.abs(pos.getZ() - centerBlock.getZ()) <= VANILLA_BREAK_CHEBYSHEV_RADIUS;
    }

    private static final class MovingBlock {
        private final ResourceKey<Level> dimension;
        private final BlockState blockState;
        private int ageTicks;
        private int stillTicks;

        private MovingBlock(ResourceKey<Level> dimension, BlockState blockState) {
            this.dimension = dimension;
            this.blockState = blockState;
        }
    }
}
