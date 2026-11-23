#!/usr/bin/env bash
set -euo pipefail

if [[ $# != 2 || -z ${ANDROID_NDK_HOME:-} ]]; then
    echo "Usage: ANDROID_NDK_HOME=/path/to/ndk bash $0 /path/to/llama.cpp /path/to/qwen.gguf" >&2
    exit 2
fi

source_dir=$(cd "$1" && pwd)
model=$2
toolchain="$ANDROID_NDK_HOME/build/cmake/android.toolchain.cmake"
if [[ ! -f $source_dir/examples/simple/CMakeLists.txt || ! -s $model || ! -f $toolchain ]]; then
    echo 'Expected a llama.cpp source checkout, a nonempty GGUF file, and an Android NDK.' >&2
    exit 2
fi

adb=${ADB:-adb} # ADB can hold the full path to platform-tools/adb.
abi=$("$adb" shell getprop ro.product.cpu.abi | tr -d '\r')
case "$abi" in
    arm64-v8a|x86_64) ;;
    *) echo "Unsupported primary Android ABI: $abi" >&2; exit 2 ;;
esac

mobile_dir=$(cd "$(dirname "$0")/.." && pwd)
mkdir -p "$mobile_dir/build/gguf-smoke"
run_dir=$(mktemp -d "$mobile_dir/build/gguf-smoke/$abi.XXXXXX")
echo "Run logs: $run_dir"

{
    git -C "$source_dir" rev-parse HEAD
    cat "$ANDROID_NDK_HOME/source.properties"
    sha256sum "$model"
    "$adb" shell 'getprop ro.product.model; getprop ro.build.version.release; getprop ro.product.cpu.abi'
    "$adb" shell 'grep ^MemTotal /proc/meminfo'
} | tee "$run_dir/environment.log"

cmake -S "$source_dir" -B "$run_dir/native" -G Ninja \
    -DCMAKE_BUILD_TYPE=Release \
    -DCMAKE_TOOLCHAIN_FILE="$toolchain" \
    -DANDROID_ABI="$abi" -DANDROID_PLATFORM=android-26 -DANDROID_STL=c++_static \
    -DBUILD_SHARED_LIBS=OFF -DGGML_NATIVE=OFF -DGGML_OPENMP=OFF -DGGML_LLAMAFILE=OFF \
    -DLLAMA_BUILD_COMMON=ON -DLLAMA_BUILD_EXAMPLES=ON -DLLAMA_BUILD_TESTS=OFF \
    -DLLAMA_BUILD_TOOLS=OFF -DLLAMA_BUILD_SERVER=OFF -DLLAMA_BUILD_APP=OFF \
    -DLLAMA_CURL=OFF 2>&1 | tee "$run_dir/build.log"
cmake --build "$run_dir/native" --target llama-simple 2>&1 | tee -a "$run_dir/build.log"

remote_dir=$("$adb" shell 'mktemp -d /data/local/tmp/meh-ai-gguf.XXXXXX' | tr -d '\r')
trap '"$adb" shell rm -rf "$remote_dir" >/dev/null 2>&1 || true' EXIT
"$adb" push "$run_dir/native/bin/llama-simple" "$remote_dir/llama-simple"
"$adb" push "$model" "$remote_dir/model.gguf"

"$adb" shell -T "cd '$remote_dir' && sh -s" <<'ANDROID' 2>&1 | tee "$run_dir/inference.log"
set -eu
printf 'airplane_mode_on=%s\n' "$(settings get global airplane_mode_on)"
printf 'wifi_on=%s\n' "$(settings get global wifi_on)"
chmod 700 ./llama-simple

# llama-simple accepts a raw prompt, so format the single Qwen turn here.
prompt='<|im_start|>user
What is the capital of France? Answer in one short sentence.<|im_end|>
<|im_start|>assistant
'
./llama-simple -m model.gguf -ngl 0 -n 32 "$prompt" &
pid=$!
peak_kib=0
while kill -0 "$pid" 2>/dev/null; do
    hwm=$(awk '/^VmHWM:/ { print $2 }' "/proc/$pid/status" 2>/dev/null || true)
    if [ "${hwm:-0}" -gt "$peak_kib" ]; then
        peak_kib=$hwm
    fi
    sleep 0.1
done
status=0
wait "$pid" || status=$?
printf '\nobserved_peak_rss_kib=%s\n' "$peak_kib"
exit "$status"
ANDROID
