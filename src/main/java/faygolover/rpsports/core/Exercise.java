package faygolover.rpsports.core;

import java.util.EnumMap;
import java.util.Map;

/**
 * Упражнения (docs/00 п. 2–9) и их числа по умолчанию: опыт и усталость за один удачный повтор, трата выносливости.
 * Числа меняются в конфиге (секция упражнения).
 */
public enum Exercise {
    BENCH("bench", Kind.MACHINE, Camera.FRONT, 5.0, 35, 6, 0, 0, zones(Zone.CHEST, 0.55, Zone.LEFT_ARM, 0.45, Zone.RIGHT_ARM, 0.45)),
    DUMBBELL("dumbbell", Kind.ITEM, Camera.NONE, 2.5, 10, 0.6, 0.4, 0, zones()),
    PULLUP("pullup", Kind.MACHINE, Camera.FRONT, 4.0, 40, 3, 2, 0, zones(Zone.LEFT_ARM, 0.45, Zone.RIGHT_ARM, 0.45, Zone.CHEST, 0.45)),
    BAG("bag", Kind.MACHINE, Camera.SIDE, 1.5, 12, 0.25, 1, 0.1, zones(Zone.ABS, 0.06)),
    TREADMILL("treadmill", Kind.MACHINE, Camera.SIDE, 20.0, 0, 0, 20, 0, zones(Zone.LEGS, 1.2)),
    PUSHUP("pushup", Kind.BODYWEIGHT, Camera.SIDE, 3.0, 20, 1.25, 0.5, 0, zones(Zone.LEFT_ARM, 0.35, Zone.RIGHT_ARM, 0.35, Zone.CHEST, 0.35)),
    SQUAT("squat", Kind.BODYWEIGHT, Camera.SIDE, 4.0, 18, 0.7, 1.7, 0, zones(Zone.LEGS, 0.45, Zone.ABS, 0.1)),
    SITUP("situp", Kind.BODYWEIGHT, Camera.SIDE, 6.0, 15, 1, 2, 0, zones(Zone.ABS, 0.7));

    /** Как начинается: тренажёр (блок), предмет в руке, без снаряда (клавиша «Упражнения»). */
    public enum Kind { MACHINE, ITEM, BODYWEIGHT }

    /** Камера на время упражнения: 3-е лицо спереди, сбоку, без изменений. */
    public enum Camera { FRONT, SIDE, NONE }

    public static final Exercise[] VALUES = values();

    public final String id;
    public final Kind kind;
    public final Camera camera;
    /** Обычная длительность повтора, секунд (сервер не принимает повторы чаще половины этого). */
    public final double repSeconds;
    public final double stamina;
    public final double xpStrength, xpFitness, xpMelee;
    /** Усталость за удачный повтор, % силы зоны. У гантели — рабочая рука, у груши — бьющая (+0,1 % за удар). */
    public final Map<Zone, Double> fatigue;

    Exercise(String id, Kind kind, Camera camera, double repSeconds, double stamina, double xpStrength, double xpFitness,
             double xpMelee, Map<Zone, Double> fatigue) {
        this.id = id;
        this.kind = kind;
        this.camera = camera;
        this.repSeconds = repSeconds;
        this.stamina = stamina;
        this.xpStrength = xpStrength;
        this.xpFitness = xpFitness;
        this.xpMelee = xpMelee;
        this.fatigue = fatigue;
    }

    /** Упражнение с весом снаряда (отношение веса к «своему»). */
    public boolean weighted() {
        return this == BENCH || this == DUMBBELL;
    }

    /** Упражнение с весом своего тела (сложность зависит от веса игрока; коврик даёт бонус — без снаряда). */
    public boolean bodyweight() {
        return kind == Kind.BODYWEIGHT || this == PULLUP;
    }

    public String key() {
        return "rpsports.exercise." + id;
    }

    public static Exercise byId(String id) {
        for (Exercise e : VALUES) if (e.id.equals(id)) return e;
        return null;
    }

    private static Map<Zone, Double> zones(Object... kv) {
        Map<Zone, Double> m = new EnumMap<>(Zone.class);
        for (int i = 0; i + 1 < kv.length; i += 2) m.put((Zone) kv[i], ((Number) kv[i + 1]).doubleValue());
        return m;
    }
}
