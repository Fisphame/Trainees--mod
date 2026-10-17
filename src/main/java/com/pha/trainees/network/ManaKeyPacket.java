package com.pha.trainees.network;

import com.pha.trainees.config.ChemConfig;
import com.pha.trainees.mana.IManaStorage;
import com.pha.trainees.mana.ManaCapabilities;
import com.pha.trainees.mana.PlayerMana;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 玩家魔力按键包（C2S，§17.1）：R = 往视线里的方块**注能**，潜行+R = 从它**抽取**。
 *
 * <p>服务端**自己再射线一次**——客户端可以伪造包，绝不能信它说"我瞄着谁"。
 * 转移量取「配置单次上限」与「双方可用量」的较小者：
 * 注能只扣对方**真正收下**的量、抽取只取自身**放得下**的量，两边都不会凭空吞掉精华。</p>
 */
public record ManaKeyPacket(boolean extract) {

    /** 基础作用距离（格）：加点系统的「注能距离」以后在这里加。 */
    private static final double REACH = 5.0;

    public static void encode(ManaKeyPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.extract);
    }

    public static ManaKeyPacket decode(FriendlyByteBuf buf) {
        return new ManaKeyPacket(buf.readBoolean());
    }

    public static void handle(ManaKeyPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        ServerPlayer sender = context.getSender();
        context.enqueueWork(() -> {
            if (sender == null) return;

            HitResult hit = sender.pick(com.pha.trainees.perk.PerkEffects.channelReach(sender), 1.0f, false);
            if (!(hit instanceof BlockHitResult blockHit)) {
                feedback(sender, "message.trainees.mana.no_target");
                return;
            }
            BlockPos pos = blockHit.getBlockPos();
            BlockEntity be = sender.level().getBlockEntity(pos);
            IManaStorage target = be == null
                    ? null
                    : be.getCapability(ManaCapabilities.mana()).orElse(null);
            if (target == null) {
                feedback(sender, "message.trainees.mana.no_target");
                return;
            }

            // 单次转移量 = 配置基数 × 加点倍率（注能/抽取各自有「效率」属性）
            int limit = (int) Math.ceil(ChemConfig.MANA_KEY_TRANSFER.get()
                    * com.pha.trainees.perk.PerkEffects.channelMultiplier(sender, msg.extract));
            int moved;
            if (msg.extract) {
                int space = PlayerMana.getCapacity(sender) - PlayerMana.get(sender);
                if (space <= 0) {
                    feedback(sender, "message.trainees.mana.self_full");
                    return;
                }
                moved = target.extractMana(Math.min(limit, space), false);
                if (moved <= 0) {
                    feedback(sender, "message.trainees.mana.target_empty");
                    return;
                }
                PlayerMana.receive(sender, moved);
            } else {
                int have = PlayerMana.get(sender);
                if (have <= 0) {
                    feedback(sender, "message.trainees.mana.empty_self");
                    return;
                }
                moved = target.receiveMana(Math.min(limit, have), false);
                if (moved <= 0) {
                    feedback(sender, "message.trainees.mana.target_full");
                    return;
                }
                PlayerMana.extract(sender, moved);
            }

            // 反馈：动作条读数 + 音效 + 粒子（紫水晶音与附魔粒子，和祭坛献祭同一套"魔法味"）
            sender.displayClientMessage(Component.translatable(
                    msg.extract ? "message.trainees.mana.drain" : "message.trainees.mana.inject",
                    moved, PlayerMana.get(sender), PlayerMana.getCapacity(sender)), true);
            PlayerMana.sync(sender);   // HUD（§17.1 P1.5）
            if (sender.level() instanceof ServerLevel serverLevel) {
                serverLevel.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.6f,
                        msg.extract ? 1.4f : 1.0f);
                serverLevel.sendParticles(ParticleTypes.ENCHANT,
                        pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                        8, 0.3, 0.3, 0.3, 0.02);
            }
        });
        context.setPacketHandled(true);
    }

    private static void feedback(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(key), true);
    }
}
