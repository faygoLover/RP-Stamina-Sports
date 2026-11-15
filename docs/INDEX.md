# RP Stamina Sports — указатель кода

Создано `python _family/tools/mapgen.py` 15.11.2026, версия 0.1.13-1.20.1-forge, коммит `e5d6bc8`.
**Руками не править**: пересоздаётся скриптом. Смысл, потоки работы и рецепты — в `docs/MAP.md`.
Разбор по шаблонам, без компиляции: необычно оформленная регистрация может отсутствовать в списках.

Всего: 45 файлов, 5365 строк Java.

## 1. Пакеты

| Пакет | Файлов | Строк |
|---|---:|---:|
| `faygolover.rpsports` | 2 | 317 |
| `faygolover.rpsports.block` | 6 | 522 |
| `faygolover.rpsports.capability` | 1 | 130 |
| `faygolover.rpsports.client` | 11 | 1284 |
| `faygolover.rpsports.client.games` | 10 | 1317 |
| `faygolover.rpsports.core` | 2 | 111 |
| `faygolover.rpsports.entity` | 1 | 59 |
| `faygolover.rpsports.item` | 2 | 89 |
| `faygolover.rpsports.menu` | 1 | 96 |
| `faygolover.rpsports.mixin` | 2 | 48 |
| `faygolover.rpsports.network` | 1 | 152 |
| `faygolover.rpsports.registry` | 1 | 143 |
| `faygolover.rpsports.server` | 5 | 1097 |

## 2. Классы

### `faygolover.rpsports`
- `RpSports`, 176 — RP Stamina — Sports addon: тренажёры и упражнения без снаряда с мини-играми, усталость мышц по зонам, рост Силы и Телосложения (дизайн — docs/00–09).
- `SportsConfig`, 141 — Настройки сервера: /serverconfig/rpsports-server.toml.

### `faygolover.rpsports.block`
- `BenchBlock`, 62 — Скамья со штангой в два блока.
- `BenchBlockEntity`, 137 — Блины на грифе скамьи: два ряда по #PER_SIDE ячеек — левая и правая сторона, от середины к краю, по одному блину в ячейке.
- `MachineBlock`, 89 — Тренажёр: поворачивается к игроку при установке, ПКМ — начать упражнение.
- `MultiBlockMachine`, 135 — Тренажёр из нескольких блоков: сетка cols × rows.
- `RecordsBoardBlock`, 62 — Доска рекордов (docs/00 п.
- `Shapes2`, 37 — Поворот формы блока, заданной для «на север», к нужной стороне.

### `faygolover.rpsports.capability`
- `SportsData`, 130 — Мышцы игрока (сервер): силы зон 0–100, стадии «Усталость» и «Боль в мышцах» (держатся до полного восстановления), перегрузка «сверх нуля», дожигание.

### `faygolover.rpsports.client`
- `BenchRenderer`, 145 — Штанга на скамье: гриф и блины (из окна блинов, по сторонам, от середины к краю).
- `BenchScreen`, 45 — Окно блинов скамьи на текстуре сундука: два ряда по 6 ячеек (лево / право), справа — подписи, в заголовке — вес.
- `CameraControl`, 94 — Камера на время упражнения (docs/00): вид от 3-го лица спереди (штанга, турник) или сбоку (остальное), гантели — без изменений.
- `ClientSession`, 145 — Своё упражнение на клиенте: мини-игра, клавиши с абсолютным приоритетом (E, R, T, F, у приседаний A и D, у груши и дорожки ЛКМ и ПКМ), Ctrl — закончить, камера и поза.
- `ClientSetup`, 113 — Клиент: виджет мини-игры в общем HUD, клавиша «Упражнения», мышь у груши и дорожки, камера и позы каждый кадр.
- `ExerciseAnimation`, 326 — Поза игрока на тренировке через player-animation-lib: не записанная анимация, а поза из состояния мини-игры (docs/00 «Анимации»): насколько заполнена шкала, настолько поднята штанга.
- `ExerciseAnimations`, 80 — Позы на тренировке у всех игроков рядом.
- `ExerciseSelector`, 51 — Маленький выбор упражнений без снаряда у прицела (клавиша «Упражнения»): 1 — отжимания, 2 — приседания, 3 — пресс.
- `Gait`, 93 — Шаг и лёгкий бег на дорожке — свои позы по мотивам Fresh Animations: Player (FreshLX, https://modrinth.com/resourcepack/fresh-animations-player) — та же идея: ноги и руки в противофазе со смещённой фазой, на каждом шаге корпус под…
- `SportEmotes`, 127 — Эмоции Emotecraft для упражнений (assets/rpsports/sport_emotes/ .json, переведены из .emotecraft утилитой tools/EmoteConvert.java; исходники — emotes_src/).
- `SportsHud`, 65 — Виджет мини-игры — элемент общего HUD RP Core: маленький, внизу по центру, над полоской выносливости.

### `faygolover.rpsports.client.games`
- `BagGame`, 175 — Груша — как Guitar Hero: четыре дорожки ударов, сверху падают ячейки, бить, когда ячейка на линии.
- `BenchGame`, 95 — Штанга: долбить E — кольцо подъёма вокруг клавиши растёт (от Силы и веса) и пустеет; тонкое внешнее кольцо — время на это поднятие.
- `DumbbellGame`, 134 — Гантели — как мешок Амбу: держать клавишу, кольцо вокруг неё растёт; отпустить в светлой зоне.
- `Minigame`, 191 — Мини-игра упражнения (клиент) — QTE в духе A Way Out: минимум текста, нарисованные клавиши, которые нажимаются, кольца и дуги.
- `PullupGame`, 122 — Турник: из центра круга расширяется кольцо (скорость каждый раз случайная, до ×2).
- `PushupGame`, 127 — Отжимания: маятник.
- `Qte`, 141 — Рисование QTE в духе A Way Out: золотые значки — клавиша в кружке, кольцо вокруг (заполнение, время), дуга с точкой и зоной (тайминг), стрелки, мелкий текст.
- `SitupGame`, 101 — Пресс — как стратагемы в Helldivers: показана цепочка стрелок, ввести её по порядку (стрелки или WASD), пока не кончилось время.
- `SquatGame`, 113 — Приседания: держишь пробел — опускаешься, отпускаешь — встаёшь; дошёл до низа и встал — повтор.
- `TreadmillGame`, 118 — Беговая дорожка: шаги по очереди — ЛКМ и ПКМ или Q и E (левая, правая); чем чаще, тем выше темп.

### `faygolover.rpsports.core`
- `Exercise` (enum), 76 — Упражнения (docs/00 п.
- `Zone` (enum), 35 — Зоны мышц (docs/00 п.

### `faygolover.rpsports.entity`
- `SeatEntity`, 59 — Невидимое сиденье на скамье: игрок «сидит», пока жмёт и отдыхает между подходами, — RP Stamina восстанавливает выносливость как сидя.

### `faygolover.rpsports.item`
- `DumbbellItem`, 43 — Гантель: ПКМ — начать подъёмы на бицепс той рукой, в которой она.
- `PlateItem`, 46 — Блин для штанги: вешается парой на скамью.

### `faygolover.rpsports.menu`
- `BenchMenu`, 96 — Окно блинов скамьи: верхний ряд — левая сторона грифа, нижний — правая (от середины к краю), по одному блину в ячейке.

### `faygolover.rpsports.mixin`
- `CameraMixin`, 25 — Камера на тренировке вращается вокруг персонажа мышью (CameraControl): подменяем углы первого setRotation в Camera.setup, дальше игра сама отодвигает камеру назад (и разворачивает для вида спереди).
- `KeyboardHandlerMixin`, 23 — Клавиши мини-игры (E, R, T, F, A/D, Ctrl) — с абсолютным приоритетом: забираем их до всех назначений игры и других модов (инвентарь на E, бег на Ctrl и т.

### `faygolover.rpsports.network`
- `SportsNet`, 152 — Сеть аддона.

### `faygolover.rpsports.registry`
- `SportsRegistry`, 143 — Блоки (тренажёры), предметы (гантели, блины), сиденье, эффект «Боль в мышцах», вкладка творческого режима.

### `faygolover.rpsports.server`
- `BadEvents`, 130 — Плохие события, когда тренировался до нуля выносливости (docs/00): гантель на ногу, штанга придавила (если нет страхующего), сорвался с турника, лента выкинула, груша по голове, носом в пол, ноги подкосились, свело живот.
- `Muscles`, 170 — Силы зон мышц: усталость, стадии, перегрузка, восстановление (docs/00 «Силы зоны»).
- `Records`, 106 — Рекорды (docs/00 п.
- `Session`, 73 — Идущее упражнение игрока (только сервер, не сохраняется).
- `SportsService`, 618 — Тренировки на сервере (RP Stamina Sports docs/00): начать — ПКМ по тренажёру, ПКМ гантелью, клавиша «Упражнения»; закончить — Ctrl, отойти, слезть; повтор: выносливость (RP Stamina), усталость зон, опыт Силы и Телосложения за кажд…

Классов без описания: 0 из 45.

## 3. Подписки на события

| Событие | Кто слушает |
|---|---|
| `AttachCapabilitiesEvent` | `RpSports.attachCaps` |
| `ClientPlayerNetworkEvent.LoggingOut` | `ClientSetup.onLogout` |
| `EntityRenderersEvent.RegisterRenderers` | `ClientSetup.renderers` |
| `FMLClientSetupEvent` | `ClientSetup.setup` |
| `FMLCommonSetupEvent` | `RpSports.commonSetup` |
| `InputEvent.InteractionKeyMappingTriggered` | `ClientSetup.onInteraction` |
| `InputEvent.MouseButton.Pre` | `ClientSetup.onMouse` |
| `PlayerEvent.Clone` | `RpSports.onClone` |
| `PlayerEvent.PlayerChangedDimensionEvent` | `RpSports.onDimension` |
| `PlayerEvent.PlayerLoggedOutEvent` | `RpSports.onLogout` |
| `RegisterCapabilitiesEvent` | `RpSports.registerCaps` |
| `RegisterCommandsEvent` | `RpSports.registerCommands` |
| `RegisterKeyMappingsEvent` | `ClientSetup.keys` |
| `RenderGuiOverlayEvent.Post` | `ClientSetup.onOverlay` |
| `RpTimeSkipEvent` | `RpSports.onTimeSkip` |
| `StaminaConsumeEvent` | `RpSports.onStaminaSpent` |
| `TickEvent.ClientTickEvent` | `ClientSetup.onClientTick` |
| `TickEvent.PlayerTickEvent` | `RpSports.onPlayerTick` |
| `TickEvent.RenderTickEvent` | `ClientSetup.onRenderTick` |
| `faygolover.rpcore.api.event.RpCharacterWipeEvent` | `RpSports.onWipe` |
| `net.minecraftforge.client.event.MovementInputUpdateEvent` | `ClientSetup.onMovement` |

## 4. Сеть

Канал: `faygolover/rpsports/network/SportsNet.java`, имя `main`, версия протокола `не объявлена константой`.
- `Start` (направление не указано в регистрации)
- `Open` (направление не указано в регистрации)
- `Rep` (направление не указано в регистрации)
- `Progress` (направление не указано в регистрации)
- `Status` (направление не указано в регистрации)
- `Anim` (направление не указано в регистрации)
- `StopRequest` (направление не указано в регистрации)
- `Stopped` (направление не указано в регистрации)

Классы с обработчиками пакетов (1): `SportsNet`.

## 5. Команды

- `faygolover/rpsports/RpSports.java`: `records` `rpsports` `status` `rest`

## 6. Данные

- capability: `RpSports`
- capability: `SportsData`, ключ `rpsports:muscles`
- SavedData: `Records`, файл "rpsports_records"

## 7. Конфиги

- server: `SportsConfig`, вызовов define 52

## 8. Что берёт из RP Core и что ему даёт

Методы `RpCoreAPI` (сколько вызовов): `body` 6, `bodyScale` 1, `dayLengthSeconds` 1, `grantXp` 3, `handsFree` 1, `hasTrait` 8, `nutrition` 2, `registerCommand` 3, `registerModule` 1, `skill` 1, `stamina` 5, `timeRunning` 1, `weightKg` 1.

События Core: `RpCharacterWipeEvent`, `RpTimeSkipEvent`.

## 9. Связи с другими модами

Проверки `isLoaded`: `playeranimator` (ExerciseAnimations).

Пакеты других модов семейства в коде: `rpcore` — 7 файлов; `rpstamina` — 1 файлов.

Миксины (2):
- `CameraMixin` (client) → Camera.class
- `KeyboardHandlerMixin` (client) → KeyboardHandler.class

## 10. Зарегистрированные id

- `faygolover/rpsports/registry/SportsRegistry.java` (11): `bench` `pullup_bar` `punching_bag` `treadmill` `gym_mat` `records_board` `dumbbell_` `plate_` `seat` `muscle_pain` `main`
