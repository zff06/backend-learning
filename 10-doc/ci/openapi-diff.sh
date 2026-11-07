#!/usr/bin/env bash
# 练习 3：把文档校验接进 CI（独立脚本版）
#
# 两种接法，选一种即可：
#   A. 纯测试版（推荐，已在 10-doc/src/test/java/top/zff06/doc/OpenApiContractTest.java 实现）
#      CI 里只要 mvn -pl 10-doc test，契约变更就会让构建红掉。
#   B. 本脚本：真起一次服务，把 openapi.json 导出成文件，和基线做 diff。
#      好处是顺带验证"应用能起来 + 文档端点真的开着"，坏处是要占一个端口。
#
# 用法：bash 10-doc/ci/openapi-diff.sh [导出目录]
# 退出码：0 契约未变；1 契约有变更（附 diff）

set -euo pipefail

MODULE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT_DIR="${1:-$MODULE_DIR/target/openapi}"
PORT="${OPENAPI_PORT:-8090}"
BASELINE="$MODULE_DIR/src/test/resources/openapi-baseline.json"
MVN="D:/Tools/apache-maven-3.9.16/bin/mvn.cmd"

mkdir -p "$OUT_DIR"

# SERVER__PORT 会覆盖 server.port，CI 容器里常被预置，必须排掉
echo "==> 启动应用（端口 $PORT）"
env -u SERVER__PORT -u SERVER__HOST "$MVN" -B -q -pl 10-doc spring-boot:run \
  -Dspring-boot.run.arguments="--server.port=$PORT" \
  > "$OUT_DIR/app.log" 2>&1 &
APP_PID=$!

cleanup() {
  kill "$APP_PID" 2>/dev/null || true
  wait "$APP_PID" 2>/dev/null || true
}
trap cleanup EXIT

echo "==> 等待文档端点就绪"
for _ in $(seq 1 60); do
  if curl -sf --noproxy '*' "http://localhost:$PORT/v3/api-docs" -o "$OUT_DIR/openapi.json"; then
    break
  fi
  sleep 1
done

if [ ! -s "$OUT_DIR/openapi.json" ]; then
  echo "文档端点没起来，看 $OUT_DIR/app.log" >&2
  exit 2
fi

# 排序 key：path / schema / property 都是 Map，顺序不稳定，不排序会被顺序变化误触发
# 顺手去掉 servers：它的 url 是"当前实例的地址 + 端口"，每次换端口都会变，不属于接口契约
echo "==> 规范化（key 排序、剔除 servers）"
python - "$OUT_DIR/openapi.json" "$OUT_DIR/openapi.sorted.json" <<'PY'
import json, sys

def norm(node):
    if isinstance(node, dict):
        return {k: norm(node[k]) for k in sorted(node) if k != 'servers'}
    if isinstance(node, list):
        return [norm(x) for x in node]
    return node

data = json.load(open(sys.argv[1], encoding='utf-8'))
with open(sys.argv[2], 'w', encoding='utf-8', newline='\n') as f:
    json.dump(norm(data), f, ensure_ascii=False, indent=2)
    f.write('\n')
PY

echo "==> 与基线比较"
if diff -u "$BASELINE" "$OUT_DIR/openapi.sorted.json" > "$OUT_DIR/openapi.diff"; then
  echo "契约未变更"
  exit 0
fi

echo "接口契约有变更，请确认是否需要通知调用方："
cat "$OUT_DIR/openapi.diff"
echo
echo "若变更是预期内的：mvn -pl 10-doc test -Ddoc.baseline.update=true 更新基线并一起提交"
exit 1
