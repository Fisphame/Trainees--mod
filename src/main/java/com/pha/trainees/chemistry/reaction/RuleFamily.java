package com.pha.trainees.chemistry.reaction;

/**
 * 反应规则家族（§19.36）：把同一张反应图切成"给玩家看的不同页"。
 *
 * <p><b>为什么需要这个字段</b>：JEI 里"化学反应 / 电离 / 相变"是三个分类，但引擎眼里它们都是
 * {@link ReactionRule}——没有家族字段时，分类只能靠 id 命名或"有没有水前置"这类约定去猜，
 * 以后加一条规则就可能悄悄落错页。</p>
 *
 * <p><b>派生默认</b>（{@link ReactionRule.Builder#build()}）：</p>
 * <ul>
 *   <li>{@code quiet()} → {@link #PHASE}：quiet 目前是相变边的专属标记（唯一调用点在
 *       {@code registerPhaseTransitions()}），所以"安静规则 = 相变"这条派生永远成立；</li>
 *   <li>其余 → {@link #CHEMICAL}；</li>
 *   <li>{@link #IONIZATION} <b>不派生，必须显式声明</b>。判据本来可以写成"selfLoop + 水前置"，
 *       但那要求模型层知道"哪个物种是水"（层级倒挂），而且以后一条"水的自环分解"就会被误分。
 *       目前只有 3 条电离规则，显式写一行比猜更可靠、也更好读。</li>
 * </ul>
 */
public enum RuleFamily {

    /** 化学反应（电解类也在这里：只需条件区多一行"需通电 ≥ x kJ/mol"）。 */
    CHEMICAL,

    /** 电离：电解质在水中解离为离子（强电解质完全电离）。 */
    IONIZATION,

    /** 相变：s⇄l⇄g（含升华/沉积）。物理过程，不在"化学反应"页出现。 */
    PHASE
}
