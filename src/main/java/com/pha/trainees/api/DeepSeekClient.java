package com.pha.trainees.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import okhttp3.*;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class DeepSeekClient {
    private static final String API_URL = "https://api.deepseek.com/chat/completions";
    private final String apiKey;
    private final OkHttpClient httpClient;
    private final Gson gson;
    private final ExecutorService executor;

    public DeepSeekClient(String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = new OkHttpClient();
        this.gson = new Gson();
        this.executor = Executors.newSingleThreadExecutor(); // 单独线程处理网络请求
    }

    /**
     * 异步发送聊天请求
     * @param userMessage 用户消息
     * @param callback 回调函数，参数为 AI 回复内容（将在主线程调用）
     */
    public void sendChatRequest(String userMessage, Consumer<String> callback) {
        executor.submit(() -> {
            try {
                String response = doRequest(userMessage);
                // 将结果回调到调用者线程（通常是游戏主线程）
                // 假设 callback 内部会通过 Minecraft 的 schedule 或直接在主线程调用
                callback.accept(response);
            } catch (Exception e) {
                callback.accept("错误: " + e.getMessage());
            }
        });
    }

    private String doRequest(String userMessage) throws IOException {
        JsonObject requestBody = getJsonObject(userMessage);

        String json = gson.toJson(requestBody);

        Request request = new Request.Builder()
                .url(API_URL)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + apiKey)
                .post(RequestBody.create(json, MediaType.parse("application/json")))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("API 请求失败: " + response.code() + " " + response.message());
            }
            String responseBody = response.body().string();
            JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
            JsonArray choices = jsonResponse.getAsJsonArray("choices");
            if (choices.size() > 0) {
                JsonObject choice = choices.get(0).getAsJsonObject();
                JsonObject message = choice.getAsJsonObject("message");
                return message.get("content").getAsString();
            } else {
                return "未收到有效回复";
            }
        }
    }

    private static @NotNull JsonObject getJsonObject(String userMessage) {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", "deepseek-chat");  // 注意模型名废弃时间
        requestBody.addProperty("stream", false);
        requestBody.addProperty("temperature", 0.7);

        JsonArray messages = new JsonArray();
        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role", "system");
        systemMsg.addProperty("content", "You are a helpful Minecraft assistant. Respond concisely.");
        messages.add(systemMsg);

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", userMessage);
        messages.add(userMsg);

        requestBody.add("messages", messages);
        return requestBody;
    }

    // 可选：优雅关闭线程池
    public void shutdown() {
        executor.shutdown();
    }
}