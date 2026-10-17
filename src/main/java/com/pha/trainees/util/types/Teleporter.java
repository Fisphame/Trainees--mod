package com.pha.trainees.util.types;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.util.ITeleporter;

import java.util.function.Function;

public class Teleporter implements ITeleporter {
    private final BlockPos pos;

    public Teleporter(BlockPos pos) {
        this.pos = pos;
    }

    @Override
    public Entity placeEntity(Entity entity, ServerLevel currentLevel, ServerLevel destLevel, float yaw,
                              Function<Boolean, Entity> repositionEntity) {
        Entity newEntity = repositionEntity.apply(false);

        if (newEntity != null) {
            newEntity.teleportTo(
                    pos.getX() + 0.5,
                    pos.getY(),
                    pos.getZ() + 0.5
            );
            newEntity.setYRot(yaw);
            newEntity.setXRot(0);
            newEntity.setDeltaMovement(0, 0, 0);

            // 对于玩家，强制同步位置
            if (newEntity instanceof ServerPlayer serverPlayer) {
                serverPlayer.connection.teleport(
                        pos.getX() + 0.5,
                        pos.getY(),
                        pos.getZ() + 0.5,
                        yaw, 0
                );
            }
        }

        return newEntity;
    }
}
