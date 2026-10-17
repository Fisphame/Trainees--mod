# 魔法机器 GUI 修改指南

> 面向"改视觉/加槽/加数值"的自己人。逻辑部分在 `menu/`，视觉部分在 `client/machine/`，
> 其余都是"注册管道"，一般不用动。

## 一、注册链路（右键 → 出界面）

```
方块 use()                                        ← 3 个方块各自的 use
  └─ NetworkHooks.openScreen(player, provider, pos)      ← ⚠ 必须这个，不能用 player.openMenu
       └─ provider: (id, inv, p) -> new MachineMenu(id, inv, pos, Kind.X)
            ├─ 服务端：用 ModMenus 的 MenuType 工厂建菜单 → 读本地方块实体当数据源
            └─ 客户端：收到开屏包 → 用同一个 MenuType 工厂建菜单（工厂里只读坐标，种类靠闭包捕获）
                 └─ MachineScreens.onClientSetup: MenuScreens.register(ModMenus.X, MachineScreen::new)
                      └─ MachineScreen 显示
```

| 文件 | 作用 | 要动吗 |
|---|---|---|
| `block/{ManaExtractor,RefinedAggregator,EnrichmentChamber}Block.use()` | 开屏 | 一般不用 |
| `registry/ModMenus` | 三个 `MenuType` + 客户端工厂 | 加新机器才动 |
| `client/machine/MachineScreens` | 菜单 → 屏幕绑定（3 行） | 加新机器才动 |
| `Main.java` | `ModMenus.MENUS.register(bus)` | 一般不用 |
| **`menu/MachineMenu`** | **槽位摆放 + 要同步的数值** | ✅ |
| **`client/machine/MachineScreen`** | **全部绘制** | ✅ |
| `menu/MachineHost` / `MachineHosts` | 数据源适配（物品/魔力/进度/流体） | 加数据才动 |

## 二、三台机器共用一套：开关是 `MachineMenu.Kind`

- **槽位**：`MachineMenu` 构造里 `switch (kind) { case X -> addSlot(...) }`
- **背包区高度**：`Kind.playerInvY()`（富集仓 140，其余 84）→ 屏幕 `imageHeight = playerInvY() + 84`
- **是否画液面**：`MachineScreen.renderBg` 里 `if (menu.kind() == Kind.ENRICHMENT)`

## 三、怎么加东西

### 1. 换贴图
`MachineScreen.renderBg` 现在**全部是 `gui.fill(...)` 代码绘制**（这样不存在"贴图尺寸与槽位对不上"）。
换贴图：

```java
// renderBg 开头
gui.blit(new ResourceLocation("trainees", "textures/gui/extractor.png"),
         leftPos, topPos, 0, 0, imageWidth, imageHeight);
```
贴图放 `src/main/resources/assets/trainees/textures/gui/`，尺寸 = `imageWidth × imageHeight`。
**注意**：`renderBg` 收到的是**屏幕坐标**，要自己加 `leftPos/topPos`（现在实现就是 `int x = leftPos;`）；
而 `renderLabels` 用的是 **GUI 局部坐标**。

### 2. 挪槽位
改 `MachineMenu` 里 `addSlot(new SlotItemHandler(库存, 索引, X, Y))` 的 X/Y —— **GUI 局部坐标**
（0,0 = 面板左上角），屏幕会自动按窗口居中平移，**不用改屏幕**。

### 3. 加一个槽
三步，缺一崩：
1. 机器方块实体的 `ItemStackHandler` 槽数 +1（`SLOT_COUNT`）
   ⚠ **库存布局变更必须写载入迁移**（`load()` 里 `forceSize()` + 按版本映射槽位）——
   聚合机就因为漏了这条，旧存档的机器一 tick 崩服（`Slot 1 not in valid range`）
2. `MachineMenu` 里给该 Kind 加一行 `addSlot(...)`
3. 富集仓还要改 `quickMoveStack` 里的 `machineSlots` 数量与目标区间（否则 Shift 转移算错）

### 4. 加"要同步的数值"（温度、转速…）
`MachineMenu` 顶部常量加一个索引、`DATA_COUNT` +1，并在匿名 `SimpleContainerData.get()` 的 `switch` 里加一档；
屏幕用 `menu.xxx()` 读。**不需要写网络包**（`addDataSlots` 自动同步）。

### 5. 液面
`MachineScreen.drawTanks(...)`：现在写死 4 个、间距 6px、高 52px。改外观就改这几个数；
要按机器类型分别画就塞进 `if (menu.kind() == ...)`。

### 6. 标题
现在用方块名（`Component.translatable(state.getBlock().getDescriptionId())`）→ 复用 `block.trainees.*`。
想给 GUI 单独命名：`use()` 里换成 `Component.translatable("gui.trainees.extractor")` + 加语言键。

## 四、四个坑（都踩过）

1. **开屏必须 `NetworkHooks.openScreen(player, provider, pos)`** —— 原版 `openMenu` 不带额外数据，
   客户端 `buf=null` → NPE；而且**界面只是静默不出现**（错误只在 `Render thread/ERROR`，不在 crash-report）
2. **客户端工厂只能读服务端写进 buf 的东西**：现在只写坐标，机器种类由 MenuType 决定（闭包捕获）
3. **槽位索引别越界**：`SlotItemHandler(handler, i, ...)` 的 i 必须 < 库存槽数，否则**开屏瞬间**抛异常
4. **`MenuScreens.register` 只能在客户端**（现挂在 `FMLClientSetupEvent` + `Dist.CLIENT`）

## 五、富集仓的流体：离散流体槽（作者 2026-10-03 定）

- 4 个流体槽：**输入 / 催化剂 / 输出 A / 输出 B**，各**只装一种流体**，整份进出（不做部分抽取）
- JSON 就写流体 id + mB：`{ "fluid": "minecraft:water", "amount": 250 }`
- **没有**"流体容器物品槽"（不靠桶中转）：流体进出走 `IFluidHandler` 能力（管道/将来的线缆），
  GUI 里由你自己画成流体槽
