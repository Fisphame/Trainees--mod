package com.pha.trainees.chemistry.spec;

import java.util.List;

public record ProductSpec(
        String id,
        List<FractionRule> required,      // 有效成分窗口
        List<FractionRule> limits,        // 有害/杂质上限
        double bottleMoles,               // 每瓶消耗的溶液总量（mol）
        String goodItem,                  // 合格品 item id
        String premiumItem,               // 优质品 item id
        List<FractionRule> premium,       // 优质档窗口（更窄的子区间，可为空）
        String behaviorKey,               // 行为钩子（V1 传 ""）
        String requiredNode               // 科技树节点（可前向引用未定义的 id）
) {

}
