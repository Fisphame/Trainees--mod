package com.pha.trainees.util.game;

import com.pha.trainees.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CommandBlock;
import net.minecraft.world.level.block.SpawnerBlock;
import net.minecraft.world.phys.Vec3;

public class BlockCourse {
    public static Boolean canBreak(Block block){
        if (block == Blocks.AIR) return false;
        if (block == Blocks.BEDROCK) return false;
        if (    block instanceof CommandBlock ||
                block == Blocks.STRUCTURE_BLOCK ||
                block == Blocks.STRUCTURE_VOID ||
                block == Blocks.JIGSAW ||
                block == Blocks.BARRIER ||
                block == Blocks.LIGHT ||
                block == Blocks.PLAYER_HEAD ||
                block == Blocks.PLAYER_WALL_HEAD ||
                block instanceof SpawnerBlock) return false;
        if (block == ModBlocks.MYBLOCK.get()) return false;
        return true;
    }

    public static boolean isInFire(Level level, BlockPos pos) {

        return level.getBlockState(pos).getBlock() == Blocks.FIRE ||
                level.getBlockState(pos).getBlock() == Blocks.TORCH ||
                level.getBlockState(pos).getBlock() == Blocks.WALL_TORCH ||
                level.getBlockState(pos).getBlock() == Blocks.SOUL_TORCH ||
                level.getBlockState(pos).getBlock() == Blocks.SOUL_WALL_TORCH ||
                level.getBlockState(pos).getBlock() == Blocks.CAMPFIRE ||
                level.getBlockState(pos).getBlock() == Blocks.SOUL_CAMPFIRE;
    }

    public static Vec3 getCenter(BlockPos pos) {
        return getCenter(pos.getX(), pos.getY(), pos.getZ());
    }
    public static Vec3 getCenter(double x, double y, double z) {
        return new Vec3(x + 0.5, y + 0.5, z + 0.5);
    }
}
