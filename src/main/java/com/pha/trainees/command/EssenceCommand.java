package com.pha.trainees.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.pha.trainees.Main;
import com.pha.trainees.blockentity.AltarCoreBlockEntity;
import com.pha.trainees.mana.PlayerMana;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 精炼精华（RE）调试命令（§17.1）。
 *
 * <p><b>为什么单独一条命令</b>：魔法线不是化学线。以前塞在 {@code /chemtester} 下面——
 * 那个前缀是化学讲堂的，作者 2026-10-02 明确要求分开。于是：
 * <b>{@code /essence add|info|clear}</b>，准星对准祭坛主方块。</p>
 *
 * <p>V1 用调试命令产生精华（与 §17.1 对电力的做法一致：理论阶段不建产魔方块），
 * 正式的来源是"金质物献祭"（{@code AltarCoreBlock#use}）。</p>
 */
public final class EssenceCommand {

    /** 交互距离：与原版创造模式手臂长度同量级。 */
    private static final double REACH = 8.0;

    private EssenceCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("essence")
                .then(Commands.literal("add")
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 1_000_000_000))
                                .executes(EssenceCommand::add)
                        )
                )
                .then(Commands.literal("info")
                        .executes(EssenceCommand::info)
                )
                .then(Commands.literal("clear")
                        .executes(EssenceCommand::clear)
                )
                .then(Commands.literal("self")
                        .then(Commands.argument("amount", IntegerArgumentType.integer(0, 1_000_000_000))
                                .executes(EssenceCommand::self)
                        )
                )
        );
    }

    /** /essence self &lt;amount&gt;：设定**自己**的精华（调试用：方便测 R 注能那一路）。 */
    private static int self(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Player player = context.getSource().getPlayerOrException();
        int amount = IntegerArgumentType.getInteger(context, "amount");
        PlayerMana.set(player, amount);
        PlayerMana.sync(player);   // HUD 要立刻反映（§17.1 P1.5）
        context.getSource().sendSuccess(() -> Component.literal("§d自身精华 → §f" + PlayerMana.get(player)
                + "§7/§f" + PlayerMana.getCapacity(player) + "§7 RE"), true);
        return 1;
    }

    /** 准星所指的祭坛主方块；不是就报错并返回 null。 */
    @Nullable
    private static AltarCoreBlockEntity target(Player player, CommandContext<CommandSourceStack> context) {
        HitResult hit = player.pick(REACH, 1.0f, false);
        if (hit instanceof BlockHitResult blockHit
                && player.level().getBlockEntity(blockHit.getBlockPos()) instanceof AltarCoreBlockEntity core) {
            return core;
        }
        context.getSource().sendFailure(Component.literal("§c请将准星对准一个祭坛主方块（altar_core_block）"));
        return null;
    }

    /** /essence add &lt;amount&gt;：灌注精华。 */
    private static int add(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Player player = context.getSource().getPlayerOrException();
        AltarCoreBlockEntity core = target(player, context);
        if (core == null) return 0;
        int amount = IntegerArgumentType.getInteger(context, "amount");
        int accepted = core.getManaStorage().receiveMana(amount, false);
        context.getSource().sendSuccess(() -> Component.literal(
                "§d注入 " + accepted + " RE§7（精炼精华 §f" + core.getManaStored()
                        + "§7/§f" + core.getManaCapacity() + "§7）"), true);
        return 1;
    }

    /** /essence info：读祭坛当前精华。 */
    private static int info(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Player player = context.getSource().getPlayerOrException();
        AltarCoreBlockEntity core = target(player, context);
        if (core == null) return 0;
        context.getSource().sendSuccess(() -> Component.literal(
                "§d=== 祭坛精华 ===\n§7存量 §f" + core.getManaStored() + " RE§7 / §f"
                        + core.getManaCapacity() + " RE§7（§f"
                        + String.format("%.1f", core.getManaFillRatio() * 100) + "%§7）"), false);
        return 1;
    }

    /** /essence clear：清空。 */
    private static int clear(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Player player = context.getSource().getPlayerOrException();
        AltarCoreBlockEntity core = target(player, context);
        if (core == null) return 0;
        core.setMana(0);
        context.getSource().sendSuccess(() -> Component.literal("§d已清空祭坛精华"), true);
        return 1;
    }
}
