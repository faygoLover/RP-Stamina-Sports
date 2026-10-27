package faygolover.rpsports;

import faygolover.rpsports.core.Exercise;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Настройки сервера: {@code <мир>/serverconfig/rpsports-server.toml}. Числа — RP Stamina Sports docs/00. */
public final class SportsConfig {
    private SportsConfig() {}

    private static final ForgeConfigSpec.Builder B = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.DoubleValue RECOVERY_HOURS, SLEEP_MULT, FATIGUE_STAGE, PAIN_STAGE, FATIGUE_XP,
            ERROR_FATIGUE, STRAIN_2, STRAIN_3, ORDINARY_FATIGUE, TREMBLE_STAMINA, UNLUCKY_STAMINA;
    public static final ForgeConfigSpec.ConfigValue<List<? extends Double>> FITNESS_FATIGUE;
    public static final ForgeConfigSpec.DoubleValue GYM_LOVER_XP, GYM_LOVER_RECOVERY, MAT_XP, CLOTHING_XP, ARMOR_PENALTY, ARMOR_DIFFICULTY;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> CLOTHING;
    public static final ForgeConfigSpec.DoubleValue KCAL_PER_PERCENT, PROTEIN_PER_PERCENT, AFTERBURN_HOURS, AFTERBURN_KCAL,
            PROTEIN_GOOD_RECOVERY, PROTEIN_LOW_RECOVERY;
    public static final ForgeConfigSpec.IntValue MELEE_XP_MAX_LEVEL;
    public static final ForgeConfigSpec.DoubleValue BAR_BASE, BAR_PER_STRENGTH, DUMBBELL_BASE, DUMBBELL_PER_STRENGTH;
    public static final ForgeConfigSpec.DoubleValue TREAD_WALK, TREAD_RUN, TREAD_SPRINT;
    public static final ForgeConfigSpec.DoubleValue BENCH_PRESS, BENCH_EXP_LIGHT, BENCH_EXP_HEAVY, BENCH_STRENGTH_EASE, BENCH_DRAIN,
            BENCH_DRAIN_PER_REP, DB_ZONE, DB_ZONE_EXP, DB_ZONE_MIN, DB_ZONE_MAX, DB_NARROW, DB_NARROW_MAX,
            OVERLOAD_STAMINA, OVERLOAD_EXP, OVERLOAD_MAX;

    public record ExerciseCfg(ForgeConfigSpec.DoubleValue xpStrength, ForgeConfigSpec.DoubleValue xpFitness,
                              ForgeConfigSpec.DoubleValue stamina, ForgeConfigSpec.DoubleValue fatigue) {}

    public static final Map<Exercise, ExerciseCfg> EXERCISES = new EnumMap<>(Exercise.class);

    static {
        B.comment(" RP Stamina — Sports addon. Силы зон мышц (100 % — свежая, 0 % — выжата) игрок не видит.").push("muscles");
        RECOVERY_HOURS = B.comment(" За сколько часов реального времени зона восстанавливается с 0 до 100 %, пока не работает")
                .defineInRange("recovery_hours", 1.0, 0.05, 48.0);
        SLEEP_MULT = B.comment(" Во сне (и при прокрутке времени во сне) — во столько раз быстрее").defineInRange("sleep_multiplier", 2.0, 1.0, 20.0);
        FATIGUE_STAGE = B.comment(" Ниже — скрытая «Усталость»: «Руки устали», дрожь, опыт за эту зону ×fatigue_xp").defineInRange("fatigue_below", 30.0, 0.0, 100.0);
        PAIN_STAGE = B.comment(" Ниже — «Боль в мышцах»: опыта нет, боль (RP Medicine) или значок").defineInRange("pain_below", 10.0, 0.0, 100.0);
        FATIGUE_XP = B.comment(" Опыт при скрытой усталости").defineInRange("fatigue_xp", 0.5, 0.0, 1.0);
        ERROR_FATIGUE = B.comment(" Ошибка утомляет во столько раз сильнее удачного повтора").defineInRange("error_fatigue", 2.0, 1.0, 10.0);
        STRAIN_2 = B.comment(" С RP Medicine: столько % «сверх нуля» — растяжение").defineInRange("strain_after", 20.0, 0.0, 1000.0);
        STRAIN_3 = B.comment(" С RP Medicine: столько % «сверх нуля» — надрыв").defineInRange("tear_after", 40.0, 0.0, 1000.0);
        FITNESS_FATIGUE = B.comment(" Как быстро устают мышцы по Телосложению 0..10").defineList("fitness_fatigue",
                List.of(1.5, 1.4, 1.3, 1.2, 1.1, 1.0, 0.92, 0.84, 0.76, 0.68, 0.6), o -> o instanceof Number);
        ORDINARY_FATIGUE = B.comment(" Обычная нагрузка (бег, плавание, добыча, удары): % силы зоны за 100 выносливости")
                .defineInRange("ordinary_fatigue_per_100", 0.15, 0.0, 10.0);
        B.pop();

        B.push("stamina");
        TREMBLE_STAMINA = B.comment(" Выносливость ниже этой доли — дрожь, мини-игра труднее").defineInRange("tremble_below", 0.25, 0.0, 1.0);
        UNLUCKY_STAMINA = B.comment(" Неудачливый: плохое событие уже при такой доле выносливости").defineInRange("unlucky_bad_event_below", 0.05, 0.0, 1.0);
        TREAD_WALK = B.comment(" Дорожка: трата выносливости в секунду при ходьбе, беге, рывке").defineInRange("treadmill_walk_per_second", 3.3, 0.0, 1000.0);
        TREAD_RUN = B.defineInRange("treadmill_run_per_second", 16.0, 0.0, 1000.0);
        TREAD_SPRINT = B.defineInRange("treadmill_sprint_per_second", 23.3, 0.0, 1000.0);
        B.pop();

        B.comment(" Опыт и одежда").push("xp");
        GYM_LOVER_XP = B.comment(" Любитель спорта: множитель опыта за тренировки").defineInRange("gym_lover_xp", 1.5, 0.0, 10.0);
        GYM_LOVER_RECOVERY = B.comment(" Любитель спорта: мышцы восстанавливаются во столько раз быстрее").defineInRange("gym_lover_recovery", 2.0, 1.0, 10.0);
        MAT_XP = B.comment(" Коврик: прибавка опыта упражнений без снаряда").defineInRange("mat_xp_bonus", 0.1, 0.0, 10.0);
        CLOTHING = B.comment(" Спортивная одежда: предметы (id), каждый в своём слоте брони — +clothing_xp_bonus опыта, тяжелее не делает")
                .defineListAllowEmpty(List.of("sports_clothing"), List.of(), o -> o instanceof String);
        CLOTHING_XP = B.defineInRange("clothing_xp_bonus", 0.05, 0.0, 10.0);
        ARMOR_PENALTY = B.comment(" Обычная броня или одежда: за каждую вещь — столько трат выносливости и усталости сверху")
                .defineInRange("armor_penalty", 0.25, 0.0, 10.0);
        ARMOR_DIFFICULTY = B.comment(" … и настолько уже зоны мини-игр").defineInRange("armor_difficulty", 0.1, 0.0, 1.0);
        MELEE_XP_MAX_LEVEL = B.comment(" Груша учит ближнему бою только до этого уровня").defineInRange("bag_melee_max_level", 4, 0, 10);
        B.pop();

        B.comment(" Калории тренировки (RP Culinary)").push("nutrition");
        KCAL_PER_PERCENT = B.comment(" Восстановление мышц: ккал за каждый 1 % силы зоны").defineInRange("kcal_per_percent", 6.0, 0.0, 1000.0);
        PROTEIN_PER_PERCENT = B.comment(" Восстановление мышц: белка (единиц запаса 0–120) за 1 %").defineInRange("protein_per_percent", 0.1, 0.0, 100.0);
        AFTERBURN_HOURS = B.comment(" Дожигание после тренировки, часов").defineInRange("afterburn_hours", 2.0, 0.0, 24.0);
        AFTERBURN_KCAL = B.comment(" Дожигание: ккал в секунду (≈ +20 % обмена веществ)").defineInRange("afterburn_kcal_per_second", 0.021, 0.0, 100.0);
        PROTEIN_GOOD_RECOVERY = B.comment(" Белка хватает (запас от 35) — восстановление во столько раз").defineInRange("protein_good_recovery", 1.5, 0.1, 10.0);
        PROTEIN_LOW_RECOVERY = B.comment(" Мало белка (ниже 20) — во столько раз").defineInRange("protein_low_recovery", 0.5, 0.0, 10.0);
        B.pop();

        B.comment(" Свой вес снаряда по Силе: база + за уровень").push("weights");
        BAR_BASE = B.defineInRange("bar_base_kg", 25.0, 0.0, 1000.0);
        BAR_PER_STRENGTH = B.defineInRange("bar_per_strength_kg", 9.5, 0.0, 1000.0);
        DUMBBELL_BASE = B.defineInRange("dumbbell_base_kg", 4.0, 0.0, 1000.0);
        DUMBBELL_PER_STRENGTH = B.defineInRange("dumbbell_per_strength_kg", 1.6, 0.0, 1000.0);
        B.pop();

        B.comment(" Силовые мини-игры (docs/10). Отношение = вес снаряда / свой вес. Читаются и клиентом (серверный конфиг приходит при входе)")
                .push("strength_games");
        BENCH_PRESS = B.comment(" Жим: доля подъёма за нажатие E при отношении 1 и Силе 3").defineInRange("bench_per_press", 0.081, 0.001, 1.0);
        BENCH_EXP_LIGHT = B.comment(" Жим: степень отношения до 1 (лёгкий вес — меньше нажатий)").defineInRange("bench_exponent_light", 0.76, 0.0, 5.0);
        BENCH_EXP_HEAVY = B.comment(" Жим: степень отношения выше 1 (перегруз; цену перегруза задаёт выносливость)")
                .defineInRange("bench_exponent_heavy", 0.45, 0.0, 5.0);
        BENCH_STRENGTH_EASE = B.comment(" Жим: за каждый уровень Силы от 3 — нажатие сильнее на столько").defineInRange("bench_strength_ease", 0.02, 0.0, 1.0);
        BENCH_DRAIN = B.comment(" Жим: подъём утекает в секунду (на 4 с повтора нужно ~(0,25 + утечка) / доля нажатий в секунду)")
                .defineInRange("bench_drain_per_second", 0.26, 0.0, 10.0);
        BENCH_DRAIN_PER_REP = B.comment(" … и утечка растёт на столько за каждое поднятие").defineInRange("bench_drain_per_rep", 0.05, 0.0, 10.0);
        DB_ZONE = B.comment(" Гантель: ширина зелёной зоны при отношении 1 и Силе 3 (кольцо наполняется за 1,33 с: 0,13 ≈ 173 мс)")
                .defineInRange("dumbbell_zone", 0.13, 0.001, 1.0);
        DB_ZONE_EXP = B.comment(" Гантель: степень отношения (зона = dumbbell_zone / отношение^степень)").defineInRange("dumbbell_zone_exponent", 2.35, 0.0, 10.0);
        DB_ZONE_MIN = B.comment(" Гантель: самая узкая и самая широкая зона (0,012 ≈ 16 мс — кадр при 60 FPS)")
                .defineInRange("dumbbell_zone_min", 0.012, 0.0, 1.0);
        DB_ZONE_MAX = B.defineInRange("dumbbell_zone_max", 0.2, 0.0, 1.0);
        DB_NARROW = B.comment(" Гантель: каждый удачный повтор сужает зону на такую долю, но не больше dumbbell_narrow_max")
                .defineInRange("dumbbell_narrow_per_rep", 0.08, 0.0, 1.0);
        DB_NARROW_MAX = B.defineInRange("dumbbell_narrow_max", 0.5, 0.0, 0.95);
        OVERLOAD_STAMINA = B.comment(" Перегруз (отношение выше 1), жим и гантели: за каждую попытку, удачную или нет, сверху",
                        " overload_stamina × (отношение − 1)^overload_exponent выносливости, не больше overload_stamina_max (×2 — ~420)")
                .defineInRange("overload_stamina", 420.0, 0.0, 100000.0);
        OVERLOAD_EXP = B.defineInRange("overload_exponent", 2.0, 0.0, 10.0);
        OVERLOAD_MAX = B.defineInRange("overload_stamina_max", 1000.0, 0.0, 100000.0);
        B.pop();

        B.comment(" Упражнения: опыт за удачный повтор, трата выносливости, множитель усталости").push("exercises");
        for (Exercise e : Exercise.VALUES) {
            B.push(e.id);
            EXERCISES.put(e, new ExerciseCfg(
                    B.defineInRange("xp_strength", e.xpStrength, 0.0, 10000.0),
                    B.defineInRange("xp_fitness", e.xpFitness, 0.0, 10000.0),
                    B.defineInRange("stamina_per_rep", (double) e.stamina, 0.0, 10000.0),
                    B.defineInRange("fatigue_multiplier", 1.0, 0.0, 100.0)));
            B.pop();
        }
        B.pop();
        SPEC = B.build();
    }

    public static <T> T get(ForgeConfigSpec.ConfigValue<T> v) {
        return SPEC.isLoaded() ? v.get() : v.getDefault();
    }

    /** Множитель усталости по Телосложению. */
    public static double fitnessFatigue(int fitness) {
        List<? extends Number> l = get(FITNESS_FATIGUE);
        if (l.isEmpty()) return 1;
        return l.get(Math.max(0, Math.min(l.size() - 1, fitness))).doubleValue();
    }
}
