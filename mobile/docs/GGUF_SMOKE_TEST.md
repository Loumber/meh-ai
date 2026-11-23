# Первый офлайн-запуск GGUF на Android

Ноябрьская контрольная точка в [Miro](https://miro.com/app/board/uXjVESovOd8=/?moveToWidget=3458764689259956225) выполнена, когда модель выдаёт осмысленный ответ на физическом Android-устройстве в авиарежиме. Для предварительного прогона подойдёт Android Emulator. Скрипт определяет ABI через ADB и собирает `llama-simple` для `arm64-v8a` или `x86_64`. Скорость и расход памяти для отчёта снимайте на телефоне.

## Подготовка на Linux

1. В Android Studio откройте **SDK Manager → SDK Tools** и установите **NDK (Side by side)**. На компьютере нужны `git`, `curl`, `cmake` и `ninja`.
2. Подключите телефон по USB, включите отладку по USB и подтвердите подключение на телефоне. Для предварительного прогона можно запустить AVD из Device Manager.
3. Из корня репозитория подготовьте исходники и модель. Qwen2.5-0.5B-Instruct Q4_K_M (~491 МБ) — модель для этой проверки. Выбор модели для MVP остаётся отдельной задачей.

```bash
mkdir -p mobile/build/gguf-inputs
git clone --depth 1 --branch b12249 https://github.com/ggml-org/llama.cpp.git \
  mobile/build/gguf-inputs/llama.cpp

model="mobile/build/gguf-inputs/qwen2.5-0.5b-instruct-q4_k_m.gguf"
curl --fail --location --retry 2 \
  https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf \
  --output "$model"
printf '%s  %s\n' 74a4da8c9fdbcd15bd1f6d01d621410d31c6fc00986f5eb687824e7b93d7a9db "$model" \
  | sha256sum --check
```

## Запуск

Укажите путь к установленной версии NDK. Для итогового прогона включите на телефоне авиарежим и убедитесь, что Wi-Fi выключен. USB-отладка остаётся доступной. Если подключено несколько устройств, задайте `ANDROID_SERIAL` значением из `adb devices -l`.

```bash
sdk="${ANDROID_HOME:-$HOME/Android/Sdk}"
ls "$sdk/ndk"
export ANDROID_NDK_HOME="$sdk/ndk/<версия из списка>"
export ADB="$sdk/platform-tools/adb"
"$ADB" devices -l
bash mobile/scripts/gguf-smoke.sh mobile/build/gguf-inputs/llama.cpp "$model"
```

Скрипт копирует бинарник и GGUF на устройство через ADB и запускает генерацию на CPU. В конце он удаляет временные файлы с устройства. Модель и бинарник на компьютере остаются в `mobile/build/`.

## Как проверить результат

- В `inference.log` после маркера `<|im_start|>assistant` должен появиться осмысленный ответ, например `The capital of France is Paris.` Для итогового прогона проверьте `airplane_mode_on=1` и `wifi_on=0` или `wifi_on=3`. На Android 11 значение `3` означает, что авиарежим отключил Wi-Fi.
- Сохраните путь к логам, который печатает скрипт. В `environment.log` будут модель телефона, Android, ABI, объём RAM, версия NDK, коммит llama.cpp и SHA-256 весов. В `inference.log` ищите `load time`, `eval time` / `tokens per second` и `observed_peak_rss_kib`. RSS — максимум из доступных выборок `/proc`; ноль означает, что измерение недоступно.
- После подтверждения запуска следующий шаг — загрузка и генерация через JNI из Kotlin с опорой на `InferenceEngine`. Этот прогон проверяет нативный рантайм отдельно от приложения.

Источники: [llama.cpp на Android](https://github.com/ggml-org/llama.cpp/blob/master/docs/android.md), [файл модели Qwen и SHA-256](https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/blob/main/qwen2.5-0.5b-instruct-q4_k_m.gguf).
