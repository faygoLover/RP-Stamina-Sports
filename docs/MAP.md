# RP Stamina Sports — карта

Версия на момент написания: 0.1.14 (16.11.2026). Факты по классам, событиям, пакетам — `docs/INDEX.md`.

## 1. Что делает и чего не делает

Спорт: тренажёры (скамья со штангой, турник, груша, беговая дорожка), гантели и упражнения без снаряда
(отжимания, приседания, пресс). Каждое упражнение — мини-игра на клиенте; удачный повтор тратит выносливость,
утомляет мышцы зоны и даёт опыт Силы, Телосложения, ближнего боя. Мышцы восстанавливаются по времени тела.
Тренировка до нуля выносливости даёт плохие события (гантель на ногу, штанга придавила).

За этим — не сюда:
- сама выносливость, её траты и восстановление — RP Stamina (этот мод — её аддон и зависит от неё напрямую);
- навыки Сила и Телосложение, их уровни — RP Perks (сюда только `RpCoreAPI.grantXp`);
- травмы от плохих событий — мод тела через `RpCoreAPI.body().bruise/strain`;
- мини-игры лечения и КПК — в своих модах, общего набора мини-игр на семейство нет.

## 2. Как течёт работа

**Начать.** ПКМ по тренажёру (`block/MachineBlock`) / ПКМ гантелью (`item/DumbbellItem`) / клавиша «Упражнения»
(`client/ExerciseSelector` → пакет `Start`) → `server/SportsService.useMachine | useDumbbell | startBodyweight`
→ `SportsService.start`: проверки (руки свободны, тело позволяет — `BodyProvider.canExert`, тренажёр не занят),
`place` ставит игрока, создаётся `server/Session` в `SportsService.SESSIONS` → пакет `Open` клиенту →
`client/ClientSession` запускает мини-игру (`client/games/…Game`), камеру и позу.

**Повтор.** мини-игра засчитала повтор → пакет `Rep` → `SportsService.onRep`: не чаще половины
`Exercise.repSeconds`, трата выносливости (`RpCoreAPI.stamina()`), усталость зон (`server/Muscles`), опыт
(`RpCoreAPI.grantXp`), рекорд (`server/Records`) → пакет `Status` клиенту. Ход движения для анимации у других
игроков — пакет `Progress` → `Anim` всем рядом.

**Закончить.** Ctrl, отошёл, сломали тренажёр, выход, смена измерения → `SportsService.stop` → пакет `Stopped`.
Выносливость на нуле во время подхода → `server/BadEvents`.

**Тик.** `RpSports.onPlayerTick` → `SportsService.tick` (каждый тик: идущая сессия) и `secondTick` (раз в секунду:
восстановление мышц по `RpCoreAPI.bodyRate()`, эффект «Боль в мышцах»). Обычная нагрузка RP Stamina тоже
утомляет мышцы: `StaminaConsumeEvent` → `Muscles.ordinary`.

**Прокрутка времени.** `RpTimeSkipEvent` → `RpSports.onTimeSkip`: мышцы восстанавливаются за прошедшее, во сне быстрее.

**Данные игрока.** `capability/SportsData` описан для RP Core (`SportsData.register()` → `PlayerStore`): Core сам
сохраняет, копирует при возрождении (после смерти — только рекорды, из Края — всё) и зовёт `forget` на выходе.
Клиенту мышцы **не отправляются**: игрок видит только ощущения.

## 3. Карта пакетов

| Пакет | За что отвечает | С чего начать читать |
|---|---|---|
| `rpsports` | вход мода, подписки, команды, настройки | `RpSports`, `SportsConfig` |
| `core` | перечни без Minecraft: упражнения и зоны мышц с числами по умолчанию | `Exercise`, `Zone` |
| `server` | вся серверная механика | `SportsService` (сессии), `Muscles` (усталость), `BadEvents`, `Records`, `Session` |
| `capability` | данные игрока | `SportsData` |
| `block` | тренажёры и доска рекордов | `MachineBlock`; многоблочные — `MultiBlockMachine`; скамья — `BenchBlock`, `BenchBlockEntity` |
| `item` | гантели и блины | `DumbbellItem`, `PlateItem` |
| `menu` | окно блинов скамьи | `BenchMenu` |
| `entity` | невидимое сиденье скамьи | `SeatEntity` |
| `registry` | регистрация блоков, предметов, эффекта, вкладки | `SportsRegistry` |
| `network` | канал и все пакеты | `SportsNet` |
| `client` | клиент: сессия, камера, позы, HUD | `ClientSetup` (подписки), `ClientSession`, `ExerciseAnimation` |
| `client.games` | мини-игры, по одной на упражнение | `Minigame` (основа), `Qte` (рисование), потом нужная игра |
| `mixin` | камера вокруг персонажа; клавиши мини-игры с абсолютным приоритетом | `CameraMixin`, `KeyboardHandlerMixin` |
| `gametest` | игровые тесты данных игрока | `SportsEdgeTests` |

## 4. Хочу добавить… — рецепты

| Что | Где | Образец | Не забыть |
|---|---|---|---|
| новое упражнение | 1) значение в `core/Exercise` (вид, камера, числа, зоны); 2) мини-игра в `client/games`, наследник `Minigame`, и её выбор по упражнению там же; 3) поза в `client/ExerciseAnimation`; 4) раздел в `SportsConfig`; 5) если тренажёр — блок в `SportsRegistry` и `MachineBlock` | `PUSHUP` + `PushupGame` (без снаряда); `PULLUP` + `PullupGame` (тренажёр) | lang ru и en (`rpsports.exercise.<id>`); `ExerciseSelector` — для упражнений без снаряда; рекорды подхватятся по id |
| новый тренажёр из нескольких блоков | `block/MultiBlockMachine` + регистрация | беговая дорожка | модель, форма через `Shapes2` |
| новую зону мышц | `core/Zone` | `Zone.ABS` | зона пишется в NBT по имени — старые не переименовывать; `Exercise.fatigue` |
| новое плохое событие | `server/BadEvents` | «гантель на ногу» | травма — через `RpCoreAPI.body()`, с запасным эффектом без мода тела |
| настройку | `SportsConfig` (обычный `ForgeConfigSpec`) | секция упражнения | описание по-русски |
| новый пакет | `network/SportsNet`, **в конец** списка регистрации | `Status` | сервер не верит клиенту: частота повторов и расстояние проверяются в `SportsService` |
| новое поле в данных игрока | `capability/SportsData`: поле, `save`, `load`, `copyFrom` (и `copyRecords`, если переживает смерть) | `records` | тест `SportsEdgeTests` найдёт несохранённое поле сам — внести его в список исключений только осознанно |
| команду | `RpSports.registerCommands` | `/rpsports rest` | `RpCoreAPI.registerCommand` в `commonSetup` |

## 5. Данные

| Что | Где хранится | Ключ | Что при смерти | Как попадает клиенту |
|---|---|---|---|---|
| мышцы и рекорды игрока | у игрока, `PlayerStore` RP Core | `rpsports:muscles` | остаются только рекорды; из Края — всё | не отправляется |
| рекорды сервера (доска) | мир, `SavedData` | `rpsports_records` | — | по запросу доски |
| идущее упражнение | память сервера, `SportsService.SESSIONS` | — | прекращается | пакеты `Open`, `Status`, `Stopped` |

Не сохраняются нарочно: `lastWorked`, `recoveredPercent` (служебные). Ключи не переименовывать.

## 6. Связи

- Берёт из Core: `stamina()` (трата), `body()` (можно ли нагружать, ушиб, растяжение), `nutrition()`, `weightKg`,
  `skill`, `hasTrait`, `grantXp`, `handsFree`, `bodyRate`, `dayLengthSeconds`; события `RpTimeSkipEvent`,
  `RpCharacterWipeEvent`; данные — `PlayerStore`; виджет мини-игры — элемент общего HUD.
- Даёт в Core: ничего не регистрирует как поставщик.
- **RP Stamina — прямая зависимость** (разрешена в `links_allow.txt`): событие `StaminaConsumeEvent`, тег
  принудительной траты `rpstamina.force`.
- player-animation-lib — позы упражнений (`client/ExerciseAnimations`, при `isLoaded("playeranimator")`); без неё
  упражнения работают без поз. Emotecraft — `client/SportEmotes`.

## 7. Конфиги

`serverconfig/rpsports-server.toml` — `SportsConfig`, обычный `ForgeConfigSpec`, секция на упражнение (числа по
умолчанию — из `core/Exercise`). Заплатки сервера — в `S5 Packs/layers/server/files/serverconfig`.

## 8. Сеть

Канал `rpsports:main` (`network/SportsNet`). На сервер: `Start`, `Rep`, `Progress`, `StopRequest`. Клиенту: `Open`,
`Status`, `Anim`, `Stopped`. Клиент сообщает об удачном повторе сам — сервер ограничивает частоту
(`Exercise.repSeconds / 2`) и проверяет, что сессия идёт и игрок на месте.

## 9. Ловушки

- **Мини-игра живёт на клиенте**, сервер знает только «повтор удался / не удался». Любая награда за повтор
  должна проходить серверную проверку частоты, иначе её можно накрутить.
- **Клавиши мини-игры забираются до всех назначений** (`KeyboardHandlerMixin`): новая клавиша в мини-игре должна
  попасть в список перехвата, иначе откроется инвентарь.
- **Смена измерения** прекращает упражнение отдельной подпиской (`RpSports.onDimension`), а не через `PlayerStore`.
- **Вес «свой»** для штанги и гантелей считается от Силы (`SportsService.ownWeight`); упражнения с весом тела
  зависят от веса игрока из мода питания или тела.
- Скамья занята, пока на ней `SeatEntity`: при правке посадки проверять `benchBusy`.

## 10. Тесты

`python ../_family/family.py verify --tests "RP Stamina Sports"` — 5 игровых тестов (`SportsEdgeTests`): запись и
чтение всех полей, ветка `ForgeCaps.rpsports:muscles`, смерть, возврат из Края, перезаход.
Механика упражнений тестами не покрыта — проверяется в игре.

## 11. Документы в docs/

| Файл | Состояние |
|---|---|
| `MAP.md`, `INDEX.md`, `test_findings.md` | действуют |
| `00_plan.md` | действует как описание дизайна (упражнения, зоны, анимации, рекорды) |
| `01`–`08` (еда и выносливость, перки, навыки, черты, тело, каталог) | история: общий дизайн тела и навыков, реализован в Core, Perks, Culinary |
| `09_deploy_plan.md` | история: план выкладки 14.10, выполнен |
| `10_strength_balance.md`, `sim.py` | действуют: баланс роста Силы и расчёт к нему |
