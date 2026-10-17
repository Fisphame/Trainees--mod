package com.pha.trainees.client.gui;

import com.pha.trainees.Main;
import com.pha.trainees.config.ChemConfig;
import com.pha.trainees.config.ChemConfig.DifficultyLevel;
import com.pha.trainees.util.interfaces.IHoverText;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec;

import java.text.DecimalFormat;
import java.util.*;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

public class ChemConfigScreen extends Screen implements IHoverText {

    private static final DecimalFormat DF = new DecimalFormat("#0.00");
    private static final int SLIDER_WIDTH = 220;
    private static final int SLIDER_HEIGHT = 20;
    private static final int ROW_HEIGHT = 28;
    private static final int GROUP_PADDING = 8;
    private static final int INDENT = 20;

    private final Screen parentScreen;
    private final Map<String, ConfigSlider> sliders = new LinkedHashMap<>();
    private final List<AbstractWidget> widgets = new ArrayList<>();
    private final List<Button> difficultyButtons = new ArrayList<>();

    private StringWidget scoreWidget = null;

    private DifficultyLevel selectedDifficulty = DifficultyLevel.NORMAL;
    private boolean isUpdatingFromPreset = false;
    private boolean configLoaded = false;

    // 滚动相关
    private int scrollOffset = 0;
    private int maxScroll = 0;
    private int contentHeight = 0;

    public ChemConfigScreen(Screen parentScreen) {
        super(Component.translatable("gui.trainees.config.title"));
        this.parentScreen = parentScreen;

        // 检查配置是否已加载（正常情况下，在游戏内打开时为 true）
        try {
            ChemConfig.ORE_VALUABLE_RATIO_MULTIPLIER.get();
            configLoaded = true;
        } catch (IllegalStateException e) {
            configLoaded = false;
        }
    }

    @Override
    protected void init() {
        super.init();
        this.widgets.clear();
        this.sliders.clear();

        // ============================================================
        // 如果配置未加载（安全防护）
        // ============================================================
        if (!configLoaded) {
            StringWidget warning = new StringWidget(
                    width / 2 - 150, height / 2 - 10, 300, 20,
                    Component.translatable("gui.trainees.config.error.not_loaded").withStyle(ChatFormatting.RED),
                    Minecraft.getInstance().font
            );
            this.widgets.add(warning);

            Button closeBtn = Button.builder(
                    Component.translatable("gui.trainees.config.button.close"),
                    button -> onClose()
            ).bounds(width / 2 - 40, height / 2 + 30, 80, 20).build();
            this.widgets.add(closeBtn);

            for (AbstractWidget widget : widgets) {
                this.addRenderableWidget(widget);
            }
            return;
        }

        // ============================================================
        // 正常初始化（配置已加载）
        // ============================================================
        // 档位唯一权威（§7.5 方案 C）：存档里的档位（含手改配置文件）优先，
        // 打开界面时先按档位刷新 6 个乘数配置，再据此构建滑块 → 界面与玩法一致。
        syncMultipliersFromTier();

        int y = 20;

        // 1. 难度指标
        double score = ChemConfig.DifficultyScorer.calculateScore();
        scoreWidget = new StringWidget(
                width / 2 - 100, y, 200, 20,
                Component.translatable("gui.trainees.config.score", DF.format(score)).withStyle(ChatFormatting.GOLD),
                Minecraft.getInstance().font
        );
        this.widgets.add(scoreWidget);
        y += 28;

        // 2. 难度按钮
        difficultyButtons.clear();
        int buttonWidth = 60;
        int buttonSpacing = 10;
        int totalWidth = buttonWidth * 4 + buttonSpacing * 3;
        int startX = (width - totalWidth) / 2;

        DifficultyLevel[] levels = DifficultyLevel.values();
        for (int i = 0; i < levels.length; i++) {
            DifficultyLevel level = levels[i];
            int x = startX + i * (buttonWidth + buttonSpacing);
            Button btn = Button.builder(
                    Component.translatable("gui.trainees.config.difficulty." + level.name().toLowerCase()),
                    button -> onDifficultySelected(level)
            ).bounds(x, y, buttonWidth, 20).build();
            btn.active = level != selectedDifficulty;
            this.difficultyButtons.add(btn);
            this.widgets.add(btn);
        }
        y += 30;
        y += 5;

        // 3. 滑动条组
        y = addSliderGroup(y, tr("gui", "resource_getting", "config", "group"),
                new SliderEntry(tr("gui","ore_valuable","config", "slider"),
                        "ore_valuable", ChemConfig.ORE_VALUABLE_RATIO_MULTIPLIER, 0.1, 3.0),
                new SliderEntry(tr("gui","ore_gangue","config", "slider"),
                        "ore_gangue", ChemConfig.ORE_GANGUE_RATIO_MULTIPLIER, 0.1, 3.0)
        );

        y = addSliderGroup(y, tr("gui", "pollution", "config", "group"),
                new SliderEntry(tr("gui","pollution_speed","config", "slider"),
                        "pollution_speed", ChemConfig.POLLUTION_DIFFUSION_SPEED, 0.0, 10.0),
                new SliderEntry(tr("gui","pollution_toxicity","config", "slider"),
                        "pollution_toxicity", ChemConfig.POLLUTION_TOXICITY_THRESHOLD, 0.01, 10.0),
                new SliderEntry(tr("gui","plant_penalty","config", "slider"),
                        "plant_penalty", ChemConfig.POLLUTION_PLANT_GROWTH_PENALTY, 0.0, 1.0)
        );

        y = addSliderGroup(y, tr("gui", "energy", "config", "group"),
                new SliderEntry(tr("gui","energy_consumption","config", "slider"),
                        "energy_consumption", ChemConfig.ENERGY_CONSUMPTION_MULTIPLIER, 0.1, 10.0),
                new SliderEntry(tr("gui","energy_generation","config", "slider"),
                        "energy_generation", ChemConfig.ENERGY_GENERATION_MULTIPLIER, 0.1, 10.0)
        );

        y = addSliderGroup(y, tr("gui", "strict_mapping", "config", "group"),
                new SliderEntry(tr("gui","bucket_mapping","config", "slider"),
                        "bucket_mapping", ChemConfig.BUCKET_TO_MOL_WATER, 0.0, 640.0),
                new SliderEntry(tr("gui","ingot_mapping","config", "slider"),
                        "ingot_mapping", ChemConfig.SOLID_INGOT_TO_MOL, 0.0, 640.0),
                new SliderEntry(tr("gui","nugget_mapping","config", "slider"),
                        "nugget_mapping", ChemConfig.SOLID_NUGGET_TO_MOL, 0.0, 640.0),
                new SliderEntry(tr("gui","max_per_component","config", "slider"),
                        "max_per_component", ChemConfig.MAX_MOLES_PER_COMPONENT, 0.1, 640.0),
                new SliderEntry(tr("gui","max_total","config", "slider"),
                        "max_total", ChemConfig.MAX_TOTAL_MOLES, 0.1, 640.0)
        );

        // 引擎调度（§14.3 引擎参数）
        y = addSliderGroup(y, tr("gui", "engine", "config", "group"),
                new SliderEntry(tr("gui","engine_polling_interval","config", "slider"),
                        "enginePollingInterval", ChemConfig.ENGINE_POLLING_INTERVAL, 1, 100),
                new SliderEntry(tr("gui","engine_max_rules_per_poll","config", "slider"),
                        "engineMaxRulesPerPoll", ChemConfig.ENGINE_MAX_RULES_PER_POLL, 1, 200),
                new SliderEntry(tr("gui","engine_max_chain_per_tick","config", "slider"),
                        "engineMaxChainPerTick", ChemConfig.ENGINE_MAX_CHAIN_REACTIONS_PER_TICK, 1, 100),
                new SliderEntry(tr("gui","engine_epsilon","config", "slider"),
                        "engineEpsilon", ChemConfig.ENGINE_EPSILON, -12.0, 0.0, true)
        );

        // 开放气体网格（§19.13 扩散参数）
        y = addSliderGroup(y, tr("gui", "gas_grid", "config", "group"),
                new SliderEntry(tr("gui","gas_diffusion_interval","config", "slider"),
                        "gasDiffusionInterval", ChemConfig.GAS_DIFFUSION_INTERVAL, 1, 200),
                new SliderEntry(tr("gui","gas_diffusion_base","config", "slider"),
                        "gasDiffusionBase", ChemConfig.GAS_DIFFUSION_BASE, 0.001, 0.5),
                new SliderEntry(tr("gui","gas_precision","config", "slider"),
                        "gasPrecision", ChemConfig.GAS_PRECISION_MODE, 0, 2, 2),
                new SliderEntry(tr("gui","gas_active_radius","config", "slider"),
                        "gasActiveRadius", ChemConfig.GAS_ACTIVE_RADIUS, 1, 16),
                new SliderEntry(tr("gui","gas_leak_enabled","config", "slider"),
                        "gasLeakEnabled", ChemConfig.GAS_CONTAINER_LEAK_ENABLED),
                new SliderEntry(tr("gui","gas_leak_rate","config", "slider"),
                        "gasLeakRate", ChemConfig.GAS_CONTAINER_LEAK_RATE, 0.0001, 0.1),
                new SliderEntry(tr("gui","gas_dissolve_threshold","config", "slider"),
                        "gasDissolveThreshold", ChemConfig.GAS_DISSOLVE_THRESHOLD, -6.0, 0.0, true)
        );

        contentHeight = y + 30;

        // 4. 底部按钮
        int bottomY = height - 30;

        Button saveBtn = Button.builder(
                Component.translatable("gui.trainees.config.button.save")
                        .withStyle(ChatFormatting.GREEN),
                button -> onSave()
        ).bounds(width / 2 - 120, bottomY, 70, 20).build();

        Button resetBtn = Button.builder(
                Component.translatable("gui.trainees.config.button.reset"),
                button -> onResetToDefault()
        ).bounds(width / 2 - 40, bottomY, 80, 20).build();

        Button closeBtn = Button.builder(
                Component.translatable("gui.trainees.config.button.close"),
                button -> onClose()
        ).bounds(width / 2 + 50, bottomY, 70, 20).build();

        this.widgets.add(saveBtn);
        this.widgets.add(resetBtn);
        this.widgets.add(closeBtn);

        // 注册所有组件
        for (AbstractWidget widget : widgets) {
            this.addRenderableWidget(widget);
        }

        // 计算最大滚动距离
        maxScroll = Math.max(0, contentHeight - height + 80);

        // 加载当前值
        loadValuesFromFile();
        updateDifficultyMatching();
    }

    // ==================== 辅助方法 ====================

    private int addSliderGroup(int y, String groupName, SliderEntry... entries) {
        StringWidget groupLabel = new StringWidget(
                INDENT, y, 200, 15,
                Component.literal(Component.translatable(groupName).getString())
                        .withStyle(ChatFormatting.GRAY),
                Minecraft.getInstance().font
        );
        this.widgets.add(groupLabel);
        y += 18;

        for (SliderEntry entry : entries) {
            ConfigSlider slider = new ConfigSlider(
                    entry, INDENT + 20, y, SLIDER_WIDTH, SLIDER_HEIGHT
            );
            this.widgets.add(slider);
            this.sliders.put(entry.key, slider);
            y += ROW_HEIGHT;
        }
        return y + GROUP_PADDING;
    }

    private int addReadOnlyGroup(int y, String groupName, String... lines) {
        StringWidget groupLabel = new StringWidget(
                INDENT, y, 200, 15,
                Component.literal(groupName),
                Minecraft.getInstance().font
        );
        this.widgets.add(groupLabel);
        y += 18;

        for (String line : lines) {
            StringWidget label = new StringWidget(
                    INDENT + 20, y, 300, 12,
                    Component.literal("§8" + line),
                    Minecraft.getInstance().font
            );
            this.widgets.add(label);
            y += 14;
        }
        return y + GROUP_PADDING;
    }

    private void onDifficultySelected(DifficultyLevel level) {
        this.selectedDifficulty = level;
        // 档位唯一权威（§7.5 方案 C）：点击档位按钮即写档位
        ChemConfig.GAME_DIFFICULTY.set(level);

        // CUSTOM = 「我要手调」状态：保留当前数值，不套预设
        if (level != DifficultyLevel.CUSTOM) {
            this.isUpdatingFromPreset = true;

            ChemConfig.DifficultyPresets.ConfigValues preset = ChemConfig.DifficultyPresets.get(level);

            for (Map.Entry<String, ConfigSlider> entry : sliders.entrySet()) {
                String key = entry.getKey();
                ConfigSlider slider = entry.getValue();
                double value = getPresetValue(preset, key);
                if (!Double.isNaN(value)) {
                    slider.setValue(value);
                    // 立即把预设值写入配置（内存），使分数显示实时更新；
                    // 落盘仍由「保存」按钮触发（与拖动滑块行为一致）
                    slider.commitValue();
                }
            }

            this.isUpdatingFromPreset = false;
        }

        refreshButtons();
        refreshScoreDisplay();
        updateDifficultyMatching();
    }

    /**
     * 档位唯一权威（§7.5 方案 C）：打开界面时按存档档位刷新 6 个乘数配置。
     * CUSTOM 档不刷新——此时乘数配置本身就是手调真源。
     */
    private void syncMultipliersFromTier() {
        DifficultyLevel tier = ChemConfig.GAME_DIFFICULTY.get();
        if (tier == DifficultyLevel.CUSTOM) return;
        ChemConfig.DifficultyPresets.ConfigValues preset = ChemConfig.DifficultyPresets.get(tier);
        ChemConfig.ORE_VALUABLE_RATIO_MULTIPLIER.set(preset.oreValuableMultiplier);
        ChemConfig.ORE_GANGUE_RATIO_MULTIPLIER.set(preset.oreGangueMultiplier);
        ChemConfig.POLLUTION_DIFFUSION_SPEED.set(preset.pollutionDiffusionSpeed);
        ChemConfig.POLLUTION_TOXICITY_THRESHOLD.set(preset.pollutionToxicityThreshold);
        ChemConfig.ENERGY_CONSUMPTION_MULTIPLIER.set(preset.energyConsumptionMultiplier);
        ChemConfig.ENERGY_GENERATION_MULTIPLIER.set(preset.energyGenerationMultiplier);
    }

    private double getPresetValue(ChemConfig.DifficultyPresets.ConfigValues preset, String key) {
        switch (key) {
            case "ore_valuable": return preset.oreValuableMultiplier;
            case "ore_gangue": return preset.oreGangueMultiplier;
            case "pollution_speed": return preset.pollutionDiffusionSpeed;
            case "pollution_toxicity": return preset.pollutionToxicityThreshold;
            case "plant_penalty": return ChemConfig.POLLUTION_PLANT_GROWTH_PENALTY.get();
            case "energy_consumption": return preset.energyConsumptionMultiplier;
            case "energy_generation": return preset.energyGenerationMultiplier;
            default: return Double.NaN;
        }
    }

    private void refreshButtons() {
        DifficultyLevel[] levels = DifficultyLevel.values();
        for (int i = 0; i < difficultyButtons.size() && i < levels.length; i++) {
            difficultyButtons.get(i).active = levels[i] != selectedDifficulty;
        }
    }

    private void refreshScoreDisplay() {
        double score = ChemConfig.DifficultyScorer.calculateScore();
        if (scoreWidget != null) {
            scoreWidget.setMessage(Component.translatable("gui.trainees.config.score", DF.format(score))
                    .withStyle(ChatFormatting.GOLD));
        }
    }

    private void updateDifficultyMatching() {
        Map<String, Double> currentValues = new HashMap<>();
        for (Map.Entry<String, ConfigSlider> entry : sliders.entrySet()) {
            currentValues.put(entry.getKey(), entry.getValue().getValue());
        }

        boolean matched = false;
        for (DifficultyLevel level : DifficultyLevel.values()) {
            if (level == DifficultyLevel.CUSTOM) continue;
            ChemConfig.DifficultyPresets.ConfigValues preset = ChemConfig.DifficultyPresets.get(level);
            if (matchesPreset(currentValues, preset)) {
                this.selectedDifficulty = level;
                matched = true;
                break;
            }
        }

        if (!matched) {
            this.selectedDifficulty = DifficultyLevel.CUSTOM;
        }

        // C-1：滑块拖到非预设组合 → 档位自动转 CUSTOM；恰好等于某预设 → 回归该档位。
        // 写档位后分数与玩法（矿石/污染/能源）都按同一权威口径解析。
        ChemConfig.GAME_DIFFICULTY.set(this.selectedDifficulty);

        refreshButtons();
        refreshScoreDisplay();
    }

    private boolean matchesPreset(Map<String, Double> current, ChemConfig.DifficultyPresets.ConfigValues preset) {
        double epsilon = 0.001;
        for (Map.Entry<String, Double> entry : current.entrySet()) {
            String key = entry.getKey();
            double value = entry.getValue();
            double presetValue = getPresetValue(preset, key);
            if (Double.isNaN(presetValue)) continue;
            if (Math.abs(value - presetValue) > epsilon) {
                return false;
            }
        }
        return true;
    }

    private void loadValuesFromFile() {
        // 滑块初始化时已经从 supplier 读取值，无需额外操作
    }

    // ==================== 保存与关闭 ====================

    // 关键修正：保存当前存档配置
    private void onSave() {
        // 1. 确保所有滑块的值已提交
        for (Map.Entry<String, ConfigSlider> entry : sliders.entrySet()) {
            entry.getValue().commitValue();
        }

        // 2. 保存配置到当前存档（SERVER_SPEC）
        ChemConfig.SERVER_SPEC.save();

        // 3. 提示玩家
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.displayClientMessage(
                    Component.translatable("gui.trainees.config.save.success")
                            .withStyle(ChatFormatting.GREEN),
                    true
            );
        }

        Main.LOGGER.info("Chemical configuration saved to current world.");
    }

    private void onResetToDefault() {
        onDifficultySelected(DifficultyLevel.NORMAL);
    }

    public void onClose() {
        Minecraft.getInstance().setScreen(parentScreen);
    }

    // ==================== 渲染与交互（含滚动） ====================

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);

        // 应用滚动偏移
        var poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.translate(0, -scrollOffset, 0);

        for (AbstractWidget widget : widgets) {
            widget.render(guiGraphics, mouseX, mouseY + scrollOffset, partialTick);
        }

        poseStack.popPose();

        // 标题（不滚动）
        String title = Component.translatable("gui.trainees.config.title").getString();
        guiGraphics.drawString(
                Minecraft.getInstance().font,
                title,
                (width - Minecraft.getInstance().font.width(title)) / 2,
                5,
                0xFFFFFF,
                false
        );

        // 存档名提示（不滚动）
        if (Minecraft.getInstance().getSingleplayerServer() != null) {
            String worldName = Minecraft.getInstance().getSingleplayerServer().getWorldData().getLevelName();
            guiGraphics.drawString(
                    Minecraft.getInstance().font,
                    Component.translatable("gui.trainees.config.world", worldName),
                    10, height - 10,
                    0x888888,
                    false
            );
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 鼠标点击需要加上滚动偏移
        return super.mouseClicked(mouseX, mouseY + scrollOffset, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta != 0) {
            int newOffset = (int) (scrollOffset - delta * 15);
            scrollOffset = Math.max(0, Math.min(maxScroll, newOffset));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    // ==================== 内部类：滑动条 ====================

    /** 滑块值展示模式 */
    private static final int MODE_PLAIN = 0;
    private static final int MODE_ON_OFF = 1;   // 布尔开关（开/关文字）
    private static final int MODE_PRECISION = 2; // 气体网格 A/B/C 精度档位

    private static final DecimalFormat DF_DYN = new DecimalFormat("0.########");
    private static final DecimalFormat DF_SCI = new DecimalFormat("0.00E0");

    private static class SliderEntry {
        final String displayName;      // 翻译键
        final String key;              // 内部键（难度预设/匹配用）
        final DoubleSupplier supplier; // 域值提供者（bool 为 0/1；log 为指数）
        final DoubleConsumer consumer; // 域值消费者（内部负责类型转换）
        final double min;
        final double max;
        final boolean integer;         // 整数吸附（IntValue）
        final int mode;                // 展示模式
        final boolean logScale;        // 对数刻度（min/max 为指数）

        SliderEntry(String displayName, String key,
                    ForgeConfigSpec.DoubleValue configValue,
                    double min, double max) {
            this(displayName, key, configValue::get, configValue::set,
                    min, max, false, MODE_PLAIN, false);
        }

        /** 对数刻度：min/max 为指数（如 -12 ~ 0），显示与设置按 10^x */
        SliderEntry(String displayName, String key,
                    ForgeConfigSpec.DoubleValue configValue,
                    double minExp, double maxExp, boolean logScale) {
            this(displayName, key,
                    () -> Math.log10(configValue.get()),
                    d -> configValue.set(Math.pow(10, d)),
                    minExp, maxExp, false, MODE_PLAIN, true);
        }

        SliderEntry(String displayName, String key,
                    ForgeConfigSpec.IntValue configValue,
                    int min, int max) {
            this(displayName, key,
                    () -> (double) configValue.get(),
                    d -> configValue.set((int) Math.round(d)),
                    min, max, true, MODE_PLAIN, false);
        }

        /** 整数档位枚举（如精度 A/B/C） */
        SliderEntry(String displayName, String key,
                    ForgeConfigSpec.IntValue configValue,
                    int min, int max, int mode) {
            this(displayName, key,
                    () -> (double) configValue.get(),
                    d -> configValue.set((int) Math.round(d)),
                    min, max, true, mode, false);
        }

        SliderEntry(String displayName, String key,
                    ForgeConfigSpec.BooleanValue configValue) {
            this(displayName, key,
                    () -> configValue.get() ? 1.0 : 0.0,
                    d -> configValue.set(d >= 0.5),
                    0, 1, false, MODE_ON_OFF, false);
        }

        SliderEntry(String displayName, String key,
                    DoubleSupplier supplier, DoubleConsumer consumer,
                    double min, double max) {
            this(displayName, key, supplier, consumer,
                    min, max, false, MODE_PLAIN, false);
        }

        private SliderEntry(String displayName, String key,
                            DoubleSupplier supplier, DoubleConsumer consumer,
                            double min, double max,
                            boolean integer, int mode, boolean logScale) {
            this.displayName = displayName;
            this.key = key;
            this.supplier = supplier;
            this.consumer = consumer;
            this.min = min;
            this.max = max;
            this.integer = integer;
            this.mode = mode;
            this.logScale = logScale;
        }
    }

    private class ConfigSlider extends AbstractSliderButton {
        private final String displayName;
        private final String key;
        private final DoubleConsumer consumer;
        private final double min;
        private final double max;
        private final boolean integer;
        private final int mode;
        private final boolean logScale;
        private double currentValue;

        public ConfigSlider(SliderEntry entry,
                            int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty(), 0);
            this.displayName = entry.displayName;
            this.key = entry.key;
            this.consumer = entry.consumer;
            this.min = entry.min;
            this.max = entry.max;
            this.integer = entry.integer;
            this.mode = entry.mode;
            this.logScale = entry.logScale;

            double raw = entry.supplier.getAsDouble();
            this.currentValue = Math.max(min, Math.min(max, raw));
            this.value = clamp01((currentValue - min) / (max - min));
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            String display = Component.translatable(displayName).getString();
            String lo = formatBound(min);
            String hi = formatBound(max);
            String range = lo.isEmpty() && hi.isEmpty()
                    ? ""
                    : " §8[" + lo + " - " + hi + "]";
            this.setMessage(Component.literal(display + ": " + formatValue(currentValue) + range));
        }

        private String formatValue(double v) {
            switch (mode) {
                case MODE_ON_OFF:
                    return v >= 0.5
                            ? "§a" + Component.translatable("gui.trainees.config.toggle.on").getString()
                            : "§8" + Component.translatable("gui.trainees.config.toggle.off").getString();
                case MODE_PRECISION:
                    return precisionLabel((int) Math.round(v));
                default:
                    return logScale ? "§f" + DF_SCI.format(Math.pow(10, v)) : "§f" + fmt(v);
            }
        }

        private String formatBound(double v) {
            if (mode == MODE_ON_OFF || mode == MODE_PRECISION) return "";
            if (logScale) return DF_SCI.format(Math.pow(10, v));
            return fmt(v);
        }

        private String precisionLabel(int index) {
            String letter = switch (index) {
                case 0 -> "a";
                case 1 -> "b";
                default -> "c";
            };
            return "§f" + Component.translatable("gui.trainees.config.precision." + letter).getString();
        }

        private String fmt(double v) {
            return integer ? String.valueOf((long) Math.round(v)) : DF_DYN.format(v);
        }

        @Override
        protected void applyValue() {
            double v = min + (max - min) * value;
            if (integer) {
                v = Math.round(v);
                // 重新校准滑条刻度，避免非整数刻度停留
                this.value = clamp01((v - min) / (max - min));
            }
            currentValue = v;
            consumer.accept(currentValue);

            if (!isUpdatingFromPreset) {
                updateDifficultyMatching();
                refreshScoreDisplay();
            }
        }

        public double getValue() {
            return currentValue;
        }

        public void setValue(double value) {
            this.currentValue = Math.max(min, Math.min(max, value));
            this.value = clamp01((this.currentValue - min) / (max - min));
            updateMessage();
        }

        public void commitValue() {
            consumer.accept(currentValue);
        }
    }

    private static double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }
}