#!/usr/bin/env bash
# 公开仓库的"不含真实内容"闸门（CI 与本地都用它）
#
# 设计要点：**不依赖任何真实 id 清单**——真实物质 id 一律是 `trainees:<名字>`，
# 而公开发布的 demo 内容一律是 `trainees:demo_*`。于是规则可以自包含：
#   Java 与化学数据目录里，`trainees:` 前缀只允许出现在 demo_ 上。
# 这样既不需要把物质清单放进取证，也不会因为清单更新而漏检。
#
# 白名单（与 scripts/make-public-snapshot.sh 保持一致，改一处要同步改另一处）：
#   ElectrolysisCellBlockEntity / AmbientAir / SpecBasisAdapter / SpecMatcher / DebugCommand /
#   ProductSpecs —— 课本级常识、UI 文案、产品**安全上限**（详见快照脚本里的理由注释）
set -euo pipefail
cd "$(dirname "$0")/.."

fail=0
report() { echo "❌ $*"; fail=1; }

echo "== 1) 检查化学相关代码与化学数据里的 trainees: 前缀 =="
# 只查**化学相关**文件：物品/结构 id（train_i、trainer_altar…）与此无关，误报会淹没有用信号。
# 注意：要跳过**纯注释行**——注释里可能出现 `trainees:form` 这类通用谓词名（不是物质）。
hits=$(grep -rInE 'trainees:[a-zA-Z0-9_]+' \
         src/main/java/com/pha/trainees/chemistry \
         src/main/java/com/pha/trainees/registry/ModChemistry.java \
         src/main/resources/data/trainees/chemistry 2>/dev/null \
       | grep -vE 'trainees:(demo|defective)_' \
       | grep -vE 'ElectrolysisCellBlockEntity\.java|AmbientAir\.java|SpecBasisAdapter\.java|SpecMatcher\.java|DebugCommand\.java|ProductSpecs\.java' \
       | awk '{ line=$0; sub(/^[^:]*:[0-9]+:/, "", line); if (line ~ /^[[:space:]]*(\/\/|\*)/) next; print }' \
       || true)
if [ -n "$hits" ]; then
  report "发现非 demo 的物质 id："
  printf '%s\n' "$hits" | head -30
fi

echo "== 2) 化学数据目录只允许 demo_* =="
non_demo=$(find src/main/resources/data/trainees/chemistry -name '*.json' ! -name 'demo_*' 2>/dev/null || true)
if [ -n "$non_demo" ]; then
  report "发现非 demo 的化学数据文件："
  printf '%s\n' "$non_demo" | head -30
fi

echo "== 3) 不应存在的私有文件 =="
stray=$(find . \( -name 'trainees开发蓝本.txt' -o -name 'tmp[0-9]*.txt' -o -name 'HANDOFF.md' \) 2>/dev/null || true)
if [ -n "$stray" ]; then
  report "发现私有文档："
  printf '%s\n' "$stray"
fi

echo "== 4) 不应包含单元测试源码（含真实数值）=="
if [ -d src/test ]; then
  report "存在 src/test 目录"
fi

echo "== 5) 不应包含导出产物 =="
if [ -d run/chemdata-dump ]; then
  report "存在 run/chemdata-dump"
fi

if [ "$fail" -ne 0 ]; then
  echo "== 结论：未通过 ❌ =="
  exit 1
fi
echo "== 结论：通过 ✅（无真实内容、无私有文档、无导出产物）=="
