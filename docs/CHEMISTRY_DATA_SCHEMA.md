# 化学引擎数据格式（契约）

> 这份文档是**公开契约**：说明本模组化学内容的数据格式、单位与校验规则。
> 引擎（机制）与本文档公开；**具体内容实例（物质、反应、配方）不在本仓库**，由各自的数据包提供。
> 因此第三方可以按本契约编写自己的内容包，而不依赖本模组的具体数据。

## 0. 设计原则

| 层 | 位置 | 说明 |
|---|---|---|
| 机制 | `chemistry/**`（Java，本仓库） | 动力学、容器、压力、热、气体网格、UI |
| 契约 | 本文档 + JSON Schema | 字段、单位、校验 |
| 实例 | 数据包（**不在本仓库**） | 物质、反应、配方、映射表 |
| 示例 | `data/<ns>/chemistry/`（本仓库，少量无害示例） | 让引擎可编译、可运行、可演示 |

关键性质：**引擎不硬编码任何具体物质**。数据缺失时引擎照常启动，只是没有内容。

## 1. 目录布局

```
data/<namespace>/chemistry/
  ions/<id>.json           物质定义
  reactions/<id>.json      反应规则
  fluid_map/<id>.json      流体 ↔ 物质 映射（可选）
  solid_map/<id>.json      固体物品 ↔ 物质 映射（可选）
```

- `<namespace>` 由内容包自己决定；本仓库使用 `trainees`（仅示例）。
- 加载顺序：`ions` → `fluid_map`/`solid_map` → `reactions`（反应引用物质，必须先加载物质）。

## 2. 物质（ion）

```json
{
  "phase": "SOLID",
  "molarMass": 0,
  "specificHeat": 1.0,
  "formationEnthalpy": 0,
  "formationGibbs": 0,
  "toxicity": 0,
  "flameColor": "0xFFFFFF",
  "displayColor": "0xFFFFFF",
  "form": "POWDER",
  "tags": [],
  "stableRange": { "min": 0, "max": 9999 }
}
```

| 字段 | 类型 | 单位/取值 | 默认 | 说明 |
|---|---|---|---|---|
| `phase` | enum | `SOLID` / `LIQUID` / `AQUEOUS` / `GAS` | 必填 | 物态，参与反应判定 |
| `molarMass` | number | g/mol | 0 | 计量与质量换算 |
| `specificHeat` | number | J/(mol·K) | 1.0 | 热容 |
| `formationEnthalpy` | number | kJ/mol | 0 | ΔHf° |
| `formationGibbs` | number | kJ/mol | 0 | ΔGf° |
| `toxicity` | int | 0–3 | 0 | 毒性等级（UI 与气体危害用） |
| `flameColor` | color | `0xRRGGBB` | 0xFFFFFF | 焰色/气体颜色 |
| `displayColor` | color | `0xRRGGBB` | 0xFFFFFF | 物品与 JEI 外观色 |
| `form` | enum | `POWDER`/`CRYSTAL`/`GRANULE`/`BULK`/`LIQUID`/`SOLUTION`/`GAS` | 由 `phase` 派生 | **仅渲染属性**，不参与反应判定 |
| `tags` | string[] | 标签 id 列表 | `[]` | 用于规则匹配与侵蚀类型判定（如酸碱/氧化剂/卤素离子/熔盐） |
| `stableRange` | object | 温度区间 K | `{0, 9999}` | 超出即不稳定 |

## 3. 反应规则（reaction）

```json
{
  "reactants":   [ { "ion": "ns:foo", "count": 1 } ],
  "products":    [ { "ion": "ns:bar", "count": 1 } ],
  "preconditions": [ { "ion": "ns:cat", "count": 1 } ],
  "thermo": {
    "deltaH": 0,
    "deltaG": 0,
    "equilibriumConstant": 1.0,
    "equilibriumOverridden": false
  },
  "kinetics": {
    "activationEnergy": 0,
    "preExponentialFactor": 1.0
  },
  "gates": {
    "minTemperature": 298.0,
    "maxTemperature": null,
    "pressureShift": { "deltaH": 0, "referenceAtm": 1.0 }
  },
  "electricalWork": 0,
  "flags": { "quiet": false, "selfLoop": false },
  "family": "CHEMICAL",
  "priority": { "bias": 1.0 }
}
```

| 字段 | 单位/取值 | 默认 | 说明 |
|---|---|---|---|
| `reactants` / `products` | 数组，`count` 为**整数计量数** | 必填（至少各一项） | **精确匹配**：反应物数量必须齐 |
| `preconditions` | 数组 | `[]` | 催化剂等：**要求存在但不消耗** |
| `thermo.deltaH` / `deltaG` | kJ/mol | 0 | 用于平衡常数与驱动力 |
| `thermo.equilibriumConstant` | — | 引擎配置默认 | 仅当 `equilibriumOverridden = true` 时按范特霍夫使用 |
| `kinetics.activationEnergy` | kJ/mol | 引擎配置默认 | 阿伦尼乌斯壁垒 |
| `kinetics.preExponentialFactor` | — | 引擎配置默认 | 指前因子 A |
| `gates.minTemperature` | K | 引擎配置默认 | 低于此温度**不可运行** |
| `gates.maxTemperature` | K 或 `null` | `null`（= +∞） | 高于此温度不可运行 |
| `gates.pressureShift` | `deltaH` kJ/mol + `referenceAtm` | 0 / 1.0 | >0 时温度门槛按克劳修斯–克拉珀龙随压强移动 |
| `electricalWork` | kJ/mol | 0 | >0 = 电解规则：**未通电不可运行**，通电时 ΔG_eff = ΔG − W |
| `flags.quiet` | bool | false | 相变等"安静规则"：不写失败诊断（避免刷屏） |
| `flags.selfLoop` | bool | false | 分解（自环）规则 |
| `family` | `CHEMICAL` / `IONIZATION` / `PHASE` | 由 `quiet` 派生 | 决定 JEI 归类；**电离必须显式声明** |
| `priority.bias` | number | 1.0 | 静态优先级偏置 |

### 3.1 数值可以不是常量：四种写法

引擎允许参数是**计算式**。数据包里有四种写法：

| 写法 | 例子 | 用途 |
|---|---|---|
| 常量 | `"deltaH": -92.3` | 绝大多数规则 |
| **自动计算** | `"deltaH": "computed"` | **推荐**：由物质的 ΔHf°/ΔGf° 按能量守恒算出（ΔH = Σ产物 − Σ反应物）。这条保持了本模组"禁止手填经验值"的纪律，也意味着数据里少一个现实数值 |
| 引用引擎配置 | `"deltaH": { "config": "chemistry.defaultDeltaH" }` | 让整合包用配置统一调（`thermo.deltaH` / `thermo.deltaG` / `electricalWork` 支持；本批编译器仅识别并警告，尚未接入） |
| 引用命名函数 | `"priority": { "function": "ns:aqueous_acid_bias" }` | **动态偏置**（见 3.2） |

**支持 `"computed"` 的字段**：`thermo.deltaH`、`thermo.deltaG`、`electricalWork`
（对应引擎里的 `deltaHComputed()` / `deltaGComputed()` / `electricalWorkComputed()`）。

### 3.2 动态优先级：为什么需要"命名函数"

现行引擎里有一种参数**无法用数字表达**：`dynamicPriorityBias` 是**以容器状态为自变量的函数**（例如"酸度越高越优先"）。JSON 表达不了函数，所以契约规定：

- **函数体属于机制（本仓库公开）**：引擎提供一个偏置函数注册表，函数按 id 注册（如 `trainees:aqueous_acid_bias`）。
- **数据只引用 id**（上表第三种写法）。
- 未注册的 id → 加载时**报错并拒绝该规则**（不静默降级）。

这条是"机制公开 / 实例私有"能成立的关键：**能表达为数字的进数据，不能表达的进引擎并公开。**

## 4. 映射表（fluid_map / solid_map）

流体与固体物品要和物质挂钩（倒桶、放物品）。此前这些映射写在 Java 里；改为数据后：

```json
{ "fluid": "ns:some_fluid", "ion": "ns:some_substance", "molesPerBucket": 64 }
```
```json
{ "item": "ns:some_item", "ion": "ns:some_ion", "molesPerItem": 1 }
```

> 注意：`molesPerBucket` / `molesPerItem` 这类**换算率**属于内容（要私有），而"映射表的格式"属于契约（公开）。

## 5. 校验与报错

加载时逐条校验，失败**不静默**：

| 情形 | 处理 |
|---|---|
| 未知字段 | 警告（便于版本演进），忽略 |
| 缺少必填字段（`phase`、`reactants`/`products`） | 错误，跳过该条 |
| 引用了不存在的物质 id | 错误，跳过该条（错误信息含规则 id 与物质 id） |
| `count` ≤ 0 或非整数 | 错误，跳过 |
| `minTemperature` > `maxTemperature` | 错误，跳过 |
| `family` 缺省且 `flags.quiet = true` | 派生为 `PHASE`（并记录一条 info） |
| 引用未注册的命名函数 | 错误，跳过 |

引擎提供诊断命令（查看加载失败清单与原因），便于内容作者自检。

## 6. 版本与兼容

- 每个文件可带 `"schemaVersion": 1`；缺省视为 1。
- 引擎对**未知字段**只警告、不报错（向后兼容）；对**未知的枚举值**报错。
- 契约只增不改：字段语义变更必须新增字段名，旧名保留为别名。

## 7. 最小示例（示例包，勿照做实验）

```json
{
  "phase": "LIQUID",
  "molarMass": 18.0,
  "specificHeat": 75.3,
  "formationEnthalpy": -285.8,
  "formationGibbs": -237.1,
  "tags": []
}
```

> 本仓库只带**教学示例**（水、食盐一类无害条目），用于让引擎可运行、可演示、可测试。
> 完整内容由各自的数据包提供。
