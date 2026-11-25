# Meh AI

#### Монорепозиторий, объединяющий нативное Android-приложение и сервис с ИИ-логикой.

---
## Архитектура проекта

```
meh-ai/
├── mobile/         # Android-приложение (Kotlin, Jetpack Compose, Gradle Multi-module)
├── ai/             # ИИ-сервис и бэкенд-логика (Python)
└── .github/        # CI/CD воркфлоу и автоматизация
```

Тема интерфейса живёт в `mobile/app/src/main/java/com/meh/ai/ui/theme`: `Color.kt`, `Type.kt`, `Theme.kt`. Окно Android — `values/themes.xml` и `values-night/themes.xml`.

---

## Дизайн

Интерфейс спокойный: бумага и чернила, тонкие границы, без цветных капсул режима и без теней. Цвет сообщает состояние. Хром — переключатель Local / Cloud, кнопки, пузыри — остаётся нейтральным.

`MehAITheme` следует системной теме. Светлая система даёт светлое приложение и светлые системные бары, тёмная — тёмные. `dynamicColor` выключен: обои не подменяют палитру. `MainActivity` включает edge-to-edge и красит статус-бар и навигацию в цвет холста (`Paper` или `Night`). Фон окна до первой отрисовки Compose — тот же цвет, `window_background`.

Нажатие — тихий ripple цвета `onSurface` с альфой `0.08`. Смена индикатора режима — `180` мс, `FastOutSlowInEasing`. Бесконечная анимация только у каретки активного вывода.

### Поверхности

Соседние слои отличаются одной ступенью яркости. Глубина — из заливки, не из elevation.

| Роль | Светлая | Тёмная | Куда садится в Material 3 |
| --- | --- | --- | --- |
| Холст | `Paper` `#F7F7F8` | `Night` `#1C1C1F` | `background`, `surface` |
| Поле, карточка | `Mist` `#EFEFF2` | `NightRaised` `#26262A` | `surfaceContainer` |
| Приподнятый слой | `MistStrong` `#E2E2E6` | `NightHigh` `#323236` | `surfaceContainerHigh` |
| Верхний слой | `LineLight` `#D0D0D6` | `NightHighest` `#3A3A40` | `surfaceContainerHighest` |
| Граница | `LineLight` | `NightLine` `#45454C` | `outline` |
| Основной текст | `Ink` `#1C1C1F` | `Snow` `#F3F3F5` | `onSurface` |
| Вторичный текст | `InkMuted` `#5E5E66` | `SnowMuted` `#A7A7B0` | `onSurfaceVariant` |

Скругления в `MehShapes`: карточка и поле ввода `12.dp`, пузырь пользователя и контролы `8.dp`. Поля экрана по горизонтали — `16.dp`. Ответ агента без заливки, как документ. Реплика человека — пузырь до `85%` ширины на `surfaceContainerHigh`.

### Цвет состояния

Два акцента. Режим инференса ими не красится: Local и Cloud — текстовые вкладки с подчёркиванием `2.dp` цвета `onSurface`.

| Смысл | Светлая | Тёмная | Роль M3 | Где |
| --- | --- | --- | --- | --- |
| Подтверждение | `Amber` `#8A5A00` | `AmberNight` `#E2C48A` | `tertiary` | «ожидает подтверждения», чип «критичное», баннер ожидания |
| Ошибка и отказ | `Red` `#A33B32` | `RedNight` `#E7A8A2` | `error` | ошибка хода, «нет сети», отклонённый шаг |

Заливки тихие: `AmberWash` / `AmberNightWash` и `RedWash` / `RedNightWash` на `tertiaryContainer` и `errorContainer`. Нейтральные кнопки — заливка `primary` (`Ink` или `Snow`), текст `onPrimary`. Кнопка отправки не белеет: кружок `32.dp` цвета `surfaceContainerHigh`, меняется только яркость стрелки. «Подтвердить» — заливка `primary`. «Отклонить» — обводка `1.dp` цвета `outline`.

### Типографика

Две семьи. Пока файлов шрифтов нет, гротеск — `FontFamily.SansSerif`, моно — `FontFamily.Monospace`. Целевой вид — Inter и JetBrains Mono.

| Стиль | Размер / интерлиньяж | Вес | Где |
| --- | --- | --- | --- |
| `headlineMedium` | 24 / 32 sp | Medium | Имя агента в шапке |
| `titleMedium` | 16 / 24 sp | Medium | Заголовки секций |
| `bodyLarge` | 16 / 24 sp | Normal | Пузыри и ответ |
| `bodyMedium` | 14 / 20 sp | Normal | Подзаголовок, описания |
| `labelLarge` | 14 / 20 sp | Medium | Кнопки, вкладки режима |
| `labelMedium` | 12 / 16 sp | Medium | Чипы |
| `labelSmall` | 11 / 16 sp, трекинг `0.4.sp` | Medium | Метки `Thought`, `Action`, `Observation` |
| `MehTheme.mono.body` | 13 / 18 sp | Normal | JSON `action_input` |
| `MehTheme.mono.label` | 12 / 16 sp | Medium | Имена инструментов и моделей |

`MonoTypography` не входит в `Typography` Material 3 и читается через `MehTheme.mono`. У гротеска 14–16 sp трекинг `0.sp`. Метки шага пишутся как `Thought`, `Action`, `Observation`, без верхнего регистра.