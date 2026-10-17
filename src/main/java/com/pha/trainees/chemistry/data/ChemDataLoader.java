package com.pha.trainees.chemistry.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.pha.trainees.Main;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 从数据包读取化学内容（契约 {@code docs/CHEMISTRY_DATA_SCHEMA.md}）。
 *
 * <p><b>加载顺序</b>：先 {@code chemistry/ions/}，再 {@code chemistry/reactions/}
 * —— 反应按 id 引用物质，必须先有物质（契约 §1）。</p>
 *
 * <p><b>为什么用 ServerStartedEvent 而不是 reload 监听器</b>：批 2 先求稳与简单。
 * 代价是 {@code /reload} 不会重新加载化学内容（重启服务器/重进世界才生效）；
 * 接入 reload 监听器留到后续批次，那时再提供热重载。</p>
 *
 * <p>所有诊断收进 {@link ChemDataRegistry}：ERROR 会跳过该条目并打进日志（不静默）。</p>
 */
public final class ChemDataLoader {

    private static final Gson GSON = new GsonBuilder().setLenient().create();
    private static final String ION_DIR = "chemistry/ions";
    private static final String RULE_DIR = "chemistry/reactions";

    private ChemDataLoader() {
    }

    /** 读一次全部内容（覆盖式：先清空注册表与诊断）。 */
    public static void load(ResourceManager manager) {
        ChemDataRegistry.beginReload();

        // ---- 1) 物质 ----
        int ionFiles = loadDirectory(manager, ION_DIR, (id, json) -> {
            ChemDataParser.ParseResult<com.pha.trainees.chemistry.particle.IonType> result =
                    ChemDataParser.parseIon(id, json);
            ChemDataRegistry.addProblems(result.problems());
            if (result.value() != null) {
                ChemDataRegistry.putIon(result.value());
            }
        });

        // ---- 2) 反应规则（引用上面的物质） ----
        int ruleFiles = loadDirectory(manager, RULE_DIR, (id, json) -> {
            ChemDataParser.ParseResult<com.pha.trainees.chemistry.reaction.ReactionRule> result =
                    ChemDataParser.parseRule(id, json, ChemDataRegistry::ion);
            ChemDataRegistry.addProblems(result.problems());
            if (result.value() != null) {
                ChemDataRegistry.putRule(result.value());
            }
        });

        report(ionFiles, ruleFiles);

        // ---- 3) 危险杂质映射（打包机用；属于内容，公开快照里不带，缺失时优雅降级）----
        loadSingle(manager, "hazard_kinds", json -> {
            if (!json.has("kinds") || !json.get("kinds").isJsonArray()) {
                ChemDataRegistry.addProblem(new ChemDataRegistry.Problem(
                        ChemDataRegistry.Severity.WARN, new ResourceLocation(Main.MODID, "hazard_kinds"),
                        "kinds", "缺少 kinds 数组，已忽略"));
                return;
            }
            int loaded = 0;
            for (com.google.gson.JsonElement element : json.getAsJsonArray("kinds")) {
                if (!element.isJsonObject()) continue;
                com.google.gson.JsonObject entry = element.getAsJsonObject();
                if (!entry.has("ion") || !entry.has("kind")) continue;
                ChemDataRegistry.putHazardKind(entry.get("ion").getAsString(), entry.get("kind").getAsString());
                loaded++;
            }
            Main.LOGGER.info("[ChemData] 危险杂质映射：{} 条", loaded);
        });

        // ---- 4) 产品杂质上限（属于内容；缺失则退化为"不判定杂质上限"，不崩）----
        loadSingle(manager, "product_limits", json -> {
            if (!json.has("limits") || !json.get("limits").isJsonObject()) return;
            int loaded = 0;
            for (Map.Entry<String, com.google.gson.JsonElement> entry : json.getAsJsonObject("limits").entrySet()) {
                if (!entry.getValue().isJsonArray()) continue;
                java.util.List<com.pha.trainees.chemistry.spec.FractionRule> rules = new java.util.ArrayList<>();
                for (com.google.gson.JsonElement element : entry.getValue().getAsJsonArray()) {
                    if (!element.isJsonObject()) continue;
                    com.google.gson.JsonObject obj = element.getAsJsonObject();
                    if (!obj.has("ion")) continue;
                    double min = obj.has("min") ? obj.get("min").getAsDouble() : 0.0;
                    double max = obj.has("max") ? obj.get("max").getAsDouble() : 1.0;
                    rules.add(new com.pha.trainees.chemistry.spec.FractionRule(
                            obj.get("ion").getAsString(), min, max));
                }
                ChemDataRegistry.putProductLimits(entry.getKey(), rules);
                loaded++;
            }
            Main.LOGGER.info("[ChemData] 产品杂质上限：{} 个产品", loaded);
        });
    }

    /**
     * 读**单个** JSON 文件（`data/&lt;ns&gt;/chemistry/&lt;name&gt;.json`）；文件不存在时静默跳过。
     *
     * <p>直接按"目录 + 文件名"枚举，避免手拼 {@code ResourceLocation} 与路径前缀不一致的坑。</p>
     */
    private static void loadSingle(ResourceManager manager, String name,
                                   java.util.function.Consumer<JsonObject> consumer) {
        String directory = "chemistry/" + name;
        Map<ResourceLocation, Resource> found = manager.listResources(directory,
                p -> p.getPath().equals(directory + ".json"));
        for (Map.Entry<ResourceLocation, Resource> entry : found.entrySet()) {
            try (BufferedReader reader = entry.getValue().openAsReader()) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                if (json != null) consumer.accept(json);
            } catch (Exception e) {
                Main.LOGGER.warn("[ChemData] 读取 {} 失败：{}", entry.getKey(), e.getMessage());
            }
        }
    }

    private interface EntryConsumer {
        void accept(ResourceLocation id, JsonObject json);
    }

    /** 读一个目录下的全部 .json；返回读到的文件数（含解析失败的）。 */
    private static int loadDirectory(ResourceManager manager, String directory, EntryConsumer consumer) {
        Map<ResourceLocation, Resource> found;
        try {
            found = manager.listResources(directory, path -> path.getPath().endsWith(".json"));
        } catch (Exception e) {
            Main.LOGGER.error("[ChemData] 枚举 {} 失败", directory, e);
            return 0;
        }

        List<ResourceLocation> sorted = new ArrayList<>(found.keySet());
        sorted.sort(Comparator.comparing(ResourceLocation::toString));   // 稳定顺序，便于复现与对比

        int count = 0;
        for (ResourceLocation file : sorted) {
            count++;
            ResourceLocation id = contentId(file, directory);
            if (id == null) continue;
            try (BufferedReader reader = found.get(file).openAsReader()) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                if (json == null) {
                    ChemDataRegistry.addProblem(new ChemDataRegistry.Problem(
                            ChemDataRegistry.Severity.ERROR, file, "", "空文件或不是 JSON 对象"));
                    continue;
                }
                consumer.accept(id, json);
            } catch (Exception e) {
                ChemDataRegistry.addProblem(new ChemDataRegistry.Problem(
                        ChemDataRegistry.Severity.ERROR, file, "", "读取失败: " + e.getMessage()));
            }
        }
        return count;
    }

    /**
     * 把资源路径换算成内容 id。
     * 例：{@code trainees:chemistry/ions/demo_alpha.json} → {@code trainees:demo_alpha}
     */
    private static ResourceLocation contentId(ResourceLocation file, String directory) {
        String path = file.getPath();
        String prefix = directory + "/";
        if (!path.startsWith(prefix) || !path.endsWith(".json")) return null;
        String name = path.substring(prefix.length(), path.length() - ".json".length());
        return new ResourceLocation(file.getNamespace(), name);
    }

    private static void report(int ionFiles, int ruleFiles) {
        int errors = 0;
        int warns = 0;
        for (ChemDataRegistry.Problem problem : ChemDataRegistry.problems()) {
            if (problem.severity() == ChemDataRegistry.Severity.ERROR) {
                errors++;
                Main.LOGGER.error("[ChemData] {}", problem);
            } else {
                warns++;
                Main.LOGGER.debug("[ChemData] {}", problem);
            }
        }
        Main.LOGGER.info("[ChemData] 加载完成：物质 {} 个（{} 文件）、反应 {} 条（{} 文件）；警告 {}、错误 {}",
                ChemDataRegistry.ionCount(), ionFiles, ChemDataRegistry.ruleCount(), ruleFiles, warns, errors);
    }
}
