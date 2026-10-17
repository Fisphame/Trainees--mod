package com.pha.trainees.event;


import com.pha.trainees.Main;
import com.pha.trainees.util.game.structure.ActiveStructureManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = Main.MODID)
public class StructureBreakHandler {

    // 修改 StructureBreakHandler 中的事件监听
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Level level = (Level) event.getLevel();
        BlockPos pos = event.getPos();

        // 只在服务器端处理
        if (level.isClientSide) return;

        // 检查这个方块是否属于任何激活的结构
        ActiveStructureManager manager = ActiveStructureManager.get(level);

        // 获取所有激活的结构
        List<ActiveStructureManager.ActiveStructureData> activeStructures =
                manager.getActiveStructures(level, null);

        for (ActiveStructureManager.ActiveStructureData data : activeStructures) {
            // 安全地计算距离，避免空指针
            if (data != null && data.matchPos != null) {
                int radius = 3;
                if (pos.closerThan(data.matchPos, radius)) {
                    // 安全地移除激活状态
                    manager.removeActiveStructureSafely(level, data.structureId, data.matchPos);
                    break;
                }
            }
        }
    }

    private static boolean isBlockPartOfStructure(BlockPos blockPos, BlockPos structureCenter) {
        // 简单的范围检查，需要根据你的具体结构调整
        int radius = 5;
        return blockPos.closerThan(structureCenter, radius);
    }
}
