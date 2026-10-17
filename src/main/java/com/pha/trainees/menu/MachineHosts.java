package com.pha.trainees.menu;

import com.pha.trainees.blockentity.EnrichmentChamberBlockEntity;
import com.pha.trainees.blockentity.ManaExtractorBlockEntity;
import com.pha.trainees.blockentity.RefinedAggregatorBlockEntity;
import com.pha.trainees.machine.FaceConfig;
import com.pha.trainees.mana.IManaStorage;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * 把三台机器方块实体适配成 {@link MachineHost}（§17.1 魔法机器 GUI 化）。
 *
 * <p>这么做的好处：**方块实体一行都不用改**——它们本来就有
 * {@code getInventory() / getManaStorage() / getProgressTicks()}（富集仓还有 {@code getFluidHandler()}
 * 与 {@code getFaceConfig()}），适配层只是把这些接到菜单要的接口上。将来第四台机器加一个分支即可。</p>
 *
 * <p>{@code totalTicks()} 返回 0：机器的"本次总时长"没有对外暴露，进度条的满值改由菜单
 * **在客户端侧观察**（看到进度往上跳时把当前值当满值）——免得为了一个进度条去改三台机器的存档结构。</p>
 */
public final class MachineHosts {

    private MachineHosts() {
    }

    @Nullable
    public static MachineHost of(@Nullable BlockEntity be) {
        if (be instanceof com.pha.trainees.blockentity.EssenceNetworkPortBlockEntity port) {
            return port;   // 端口自己实现 MachineHost（没有物品槽，只有网络信息）
        }
        if (be instanceof ManaExtractorBlockEntity extractor) {
            return new Adapter(extractor.getInventory(), extractor.getManaStorage(),
                    extractor.getProgressTicks(), null, null, null);
        }
        if (be instanceof RefinedAggregatorBlockEntity aggregator) {
            return new Adapter(aggregator.getInventory(), aggregator.getManaStorage(),
                    aggregator.getProgressTicks(), null, null, null);
        }
        if (be instanceof EnrichmentChamberBlockEntity chamber) {
            return new Adapter(chamber.getInventory(), chamber.getManaStorage(),
                    chamber.getProgressTicks(), chamber.getFluidHandler(),
                    chamber.getFaceConfig(), chamber::onFaceConfigChanged);
        }
        return null;
    }

    private record Adapter(ItemStackHandler items, IManaStorage manaStorage, int progressTicks,
                           @Nullable IFluidHandler fluidHandler,
                           @Nullable FaceConfig faceConfig,
                           @Nullable Runnable facesChanged) implements MachineHost {

        @Override
        public ItemStackHandler inventory() {
            return items;
        }

        @Override
        public IManaStorage mana() {
            return manaStorage;
        }

        @Override
        public int progress() {
            return progressTicks;
        }

        @Override
        public int totalTicks() {
            return 0;
        }

        @Override
        public IFluidHandler fluids() {
            return fluidHandler;
        }

        @Override
        public FaceConfig faces() {
            return faceConfig;
        }

        @Override
        public void onFaceConfigChanged() {
            if (facesChanged != null) {
                facesChanged.run();
            }
        }
    }
}
