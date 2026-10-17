package com.pha.trainees.registry;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.pha.trainees.Main;
import com.pha.trainees.api.DeepSeekClient;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ModCommand {

    // 定义游戏规则键
    public static GameRules.Key<GameRules.BooleanValue> ENCHANT_BROKEN_BLOCK_DROP;
    public static GameRules.Key<GameRules.BooleanValue> ENCHANT_BREAK_BLOCK;
    public static GameRules.Key<GameRules.BooleanValue> ALLOW_REACTION_EXPLODE;

    // 注册方法
    public static void register() {
        ENCHANT_BROKEN_BLOCK_DROP = GameRules.register(
                "enchantbrokenblockdrop",
                GameRules.Category.PLAYER,
                createBooleanRule(false)
        );

        ENCHANT_BREAK_BLOCK = GameRules.register(
                "enchantbreakblock",
                GameRules.Category.PLAYER,
                createBooleanRule(false)
        );

        ALLOW_REACTION_EXPLODE = GameRules.register(
                "allowreactionexplode",
                GameRules.Category.PLAYER,
                createBooleanRule(true)
        );
    }

    // 创建布尔规则
    private static GameRules.Type<GameRules.BooleanValue> createBooleanRule(boolean defaultValue) {
        return GameRules.BooleanValue.create(defaultValue);
    }

    public static class AskCommand {

        @SubscribeEvent
        public static void onRegisterCommands(RegisterCommandsEvent event) {
            CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
            dispatcher.register(Commands.literal("ask")
                    .then(Commands.argument("question", StringArgumentType.greedyString())
                            .executes(AskCommand::execute)
                    )
            );
        }

        private static int execute(CommandContext<CommandSourceStack> context) {
            String question = StringArgumentType.getString(context, "question");
            CommandSourceStack source = context.getSource();
            // 获取玩家（如果是玩家执行）
            if (!(source.getEntity() instanceof ServerPlayer player)) {
                source.sendFailure(Component.literal("此命令只能由玩家执行"));
                return 0;
            }

            // 提示正在思考
            player.sendSystemMessage(Component.literal("🤖 思考中...").withStyle(ChatFormatting.YELLOW));

            // 获取 DeepSeek 客户端实例
            DeepSeekClient client = Main.getDeepSeekClient();
            if (client == null) {
                player.sendSystemMessage(Component.literal("DeepSeek API 未初始化，请检查 API Key 配置").withStyle(ChatFormatting.RED));
                return 0;
            }

            // 异步调用 API（避免卡死游戏）
            client.sendChatRequest(question, response -> {
                // 此回调运行在 OkHttp 的线程中，不能直接发送消息到客户端
                // 需要切换到 Minecraft 主线程（使用 Player::sendSystemMessage 是线程安全的？实际上 Forge 要求主线程发送）
                // 正确做法：使用 player.getServer().execute() 将任务调度到主线程
                if (player.getServer() != null) {
                    player.getServer().execute(() -> {
                        // 限制响应长度（避免刷屏）
                        String trimmed = response.length() > 500 ? response.substring(0, 500) + "..." : response;
                        player.sendSystemMessage(Component.literal("💬 " + trimmed).withStyle(ChatFormatting.AQUA));
                    });
                }
            });

            return 1;
        }
    }
}