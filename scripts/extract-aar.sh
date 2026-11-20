#!/usr/bin/env bash
# 将 gomobile 产出的 snishaper-core.aar 拆解为引擎模块可用的形式：
#   * classes.jar            → core/engine/libs/snishaper-core-classes.jar（Java/Kotlin 编译依赖）
#   * jni/<abi>/*.so         → core/engine/src/main/jniLibs/<abi>/（原生库，随 APK 打包）
#
# 背景：AGP 禁止库模块以本地 .aar 文件作为依赖参与 AAR 打包（bundle/lint 均会失败），
# 拆解后 :core:engine 是普通库模块，AAR 的类与原生库经工程依赖正常传递到 :app。
# CI 在 `gomobile bind` 之后调用本脚本；本地开发同样先跑一次即可。
set -euo pipefail

cd "$(dirname "$0")/.."

AAR="core/engine/libs/snishaper-core.aar"
OUT="core/engine/build/aar-extracted"

if [ ! -f "$AAR" ]; then
  echo "error: $AAR not found (先运行 gomobile bind 或从 Release 下载)" >&2
  exit 2
fi

rm -rf "$OUT"
mkdir -p "$OUT" core/engine/libs core/engine/src/main/jniLibs
unzip -oq "$AAR" classes.jar 'jni/*' -d "$OUT"

cp "$OUT/classes.jar" core/engine/libs/snishaper-core-classes.jar
rm -rf core/engine/src/main/jniLibs/*
if [ -d "$OUT/jni" ]; then
  cp -r "$OUT/jni/"* core/engine/src/main/jniLibs/
fi

echo "extracted:"
ls -lh core/engine/libs/snishaper-core-classes.jar
find core/engine/src/main/jniLibs -name "*.so" | head
