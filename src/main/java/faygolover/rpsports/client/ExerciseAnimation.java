package faygolover.rpsports.client;

import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonConfiguration;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.core.util.Vec3f;
import faygolover.rpsports.core.Exercise;
import net.minecraft.client.player.AbstractClientPlayer;

/**
 * Поза игрока на тренировке через player-animation-lib: не записанная анимация, а поза из состояния мини-игры
 * (docs/00 «Анимации»): насколько заполнена шкала, настолько поднята штанга.
 *
 * <p>Как библиотека применяет «body» (всё тело): сдвиг в блоках в системе игрока (вперёд — −Z, вверх — +Y, вправо —
 * +X), потом поворот вокруг точки на высоте 0,7 над ногами (бёдра). Поворот X +π/2 кладёт на спину (голова уходит
 * назад), −π/2 — лицом вниз (голова вперёд). У частей (руки, ноги, голова) — свои углы модели: xRot &lt; 0 — вперёд
 * (у поднятой вверх руки −π), сдвиг — в пикселях модели (+y — к ногам, +z — к спине).
 *
 * <p>Размеры модели в блоках: плечо 1,29 над ногами, рука от плеча 0,586, нога 0,70, половина толщины тела 0,117.
 */
public final class ExerciseAnimation implements IAnimation {
    private static final float PI = (float) Math.PI;
    /** Пиксель модели в блоках (масштаб игрока 0,9375). */
    private static final float PX = 0.9375f / 16f;
    private static final float SHOULDER = 1.29f, ARM = 0.586f, LEG = 0.70f, HALF_DEPTH = 0.117f, PIVOT = 0.7f;

    private final AbstractClientPlayer player;
    /** −1 — нет упражнения. */
    int exercise = -1;
    float a, b;
    /** Гантель в левой руке. */
    boolean leftHand;
    private float shownA, shownB, runPhase;
    private long lastNanos;

    ExerciseAnimation(AbstractClientPlayer player) {
        this.player = player;
    }

    void set(int exercise, float a, float b) {
        if (this.exercise != exercise) {
            shownA = a;
            shownB = b;
            runPhase = 0;
        }
        this.exercise = exercise;
        this.a = a;
        this.b = b;
    }

    @Override
    public boolean isActive() {
        return exercise >= 0;
    }

    /** Каждый кадр: плавно догнать позу из мини-игры (у чужих игроков она приходит ~10 раз в секунду). */
    @Override
    public void setupAnim(float tickDelta) {
        long now = System.nanoTime();
        float dt = lastNanos == 0 ? 0 : Math.min(0.1f, (now - lastNanos) / 1e9f);
        lastNanos = now;
        float k = 1 - (float) Math.exp(-14 * dt);
        shownA += (a - shownA) * k;
        // У груши b — какой удар (не плавно), у остальных — плавно.
        shownB = exercise == Exercise.BAG.ordinal() || exercise == Exercise.DUMBBELL.ordinal() ? b : shownB + (b - shownB) * k;
        if (exercise == Exercise.TREADMILL.ordinal()) runPhase += dt * (4.5f + 8.5f * shownA);
        // Тело смотрит туда же, куда игрок поставлен (иначе оно «доворачивается» за головой).
        if (exercise >= 0 && exercise != Exercise.DUMBBELL.ordinal()) {
            player.yBodyRot = player.yBodyRotO = player.getYRot();
            player.yHeadRot = player.yHeadRotO = player.getYRot();
        }
    }

    @Override
    public FirstPersonMode getFirstPersonMode(float tickDelta) {
        return exercise == Exercise.DUMBBELL.ordinal() ? FirstPersonMode.THIRD_PERSON_MODEL : FirstPersonMode.NONE;
    }

    @Override
    public FirstPersonConfiguration getFirstPersonConfiguration(float tickDelta) {
        if (shownB >= 2) return new FirstPersonConfiguration(true, true, true, true);
        return new FirstPersonConfiguration(!leftHand, leftHand, !leftHand, leftHand);
    }

    private static Vec3f v3(float x, float y, float z) {
        return new Vec3f(x, y, z);
    }

    private static float lerp(float t, float from, float to) {
        return from + (to - from) * t;
    }

    @Override
    public Vec3f get3DTransform(String part, TransformType type, float tickDelta, Vec3f v) {
        if (exercise < 0 || type == TransformType.BEND) return v;
        boolean pos = type == TransformType.POSITION;
        float A = Math.max(0, Math.min(1, shownA));
        return switch (Exercise.VALUES[exercise]) {
            case BENCH -> bench(part, pos, v, A);
            case PULLUP -> pullup(part, pos, v, A);
            case DUMBBELL -> dumbbell(part, pos, v, A);
            case BAG -> bag(part, pos, v, A);
            case TREADMILL -> treadmill(part, pos, v, A);
            case PUSHUP -> pushup(part, pos, v, A);
            case SQUAT -> squat(part, pos, v, A);
            case SITUP -> situp(part, pos, v, A);
        };
    }

    /** Жим: наклон рук к ногам (штанга опускается к груди) и разведение в стороны, радианы. */
    static float benchTilt(float A) {
        return (1 - A) * 0.7f;
    }

    private static final float BENCH_SPREAD = 0.12f;

    /**
     * Где кисти (гриф) относительно плеч лежащего: {сдвиг к ногам, высота над бёдрами}, блоки. Та же формула, что у
     * позы рук, — штанга ({@link BenchRenderer}) ровно в руках.
     */
    public static float[] benchHands(float A) {
        float tilt = benchTilt(Math.max(0, Math.min(1, A)));
        return new float[]{ARM * (float) Math.sin(tilt), ARM * (float) Math.cos(tilt) * (float) Math.cos(BENCH_SPREAD)};
    }

    /** Подъём штанги сейчас (для отрисовки грифа), −1 — не жмёт. */
    float benchLift() {
        return exercise == Exercise.BENCH.ordinal() ? Math.max(0, Math.min(1, shownA)) : -1;
    }

    /**
     * Жим лёжа: на спине на подушке (сиденье ставит сервер так, что бёдра — на высоте спины; плечи под грифом), ноги
     * спущены к полу. Руки вверх и чуть врозь; шкала пуста — руки наклонены к ногам (штанга у груди), полна — прямые.
     */
    private Vec3f bench(String part, boolean pos, Vec3f v, float A) {
        switch (part) {
            case "body" -> { return pos ? v3(0, 0, 0) : v3(PI / 2, 0, 0); }
            case "head" -> { return pos ? v : v3(0, 0, 0); }
            case "rightArm", "leftArm" -> {
                boolean right = part.equals("rightArm");
                return pos ? v : v3(-PI / 2 + benchTilt(A), right ? BENCH_SPREAD : -BENCH_SPREAD, 0);
            }
            case "rightLeg", "leftLeg" -> { return pos ? v : v3(1.1f, 0, part.equals("rightLeg") ? 0.12f : -0.12f); }
            default -> { return v; }
        }
    }

    /**
     * Турник (перекладина ~2,4 блока): игрок стоит на земле под ней, а модель висит — тело поднято на {@code rise}.
     * A = 0 — вис на прямых руках, 1 — подбородок над перекладиной; руки укорачиваются так, чтобы кисти были на ней.
     */
    private Vec3f pullup(String part, boolean pos, Vec3f v, float A) {
        float bar = 2.406f, hang = bar - (SHOULDER + ARM - 0.03f), top = bar + 0.05f - 1.407f;
        float rise = lerp(A, hang, top);
        switch (part) {
            case "body" -> { return pos ? v3(0, rise, 0) : v3(0, 0, 0); }
            case "head" -> { return pos ? v : v3(-0.25f, 0, 0); }
            case "rightArm", "leftArm" -> {
                float s = Math.max(0, (SHOULDER + ARM - 0.03f + rise - bar) / PX);
                boolean right = part.equals("rightArm");
                return pos ? v3(v.getX(), v.getY() + s, v.getZ()) : v3(-PI, 0, right ? -0.18f : 0.18f);
            }
            case "rightLeg" -> { return pos ? v : v3(0.25f, 0, 0.03f); }
            case "leftLeg" -> { return pos ? v : v3(0.35f, 0, -0.03f); }
            default -> { return v; }
        }
    }

    /** Гантель: подъём на бицепс рабочей рукой; две гантели — обе руки (левая — из B: 2 + её подъём). */
    private Vec3f dumbbell(String part, boolean pos, Vec3f v, float A) {
        if (shownB >= 2) {
            float leftA = Math.max(0, Math.min(1, shownB - 2));
            if (part.equals("rightArm")) return pos ? v : v3(-A * 1.9f, 0, 0.08f);
            if (part.equals("leftArm")) return pos ? v : v3(-leftA * 1.9f, 0, -0.08f);
            return v;
        }
        String arm = leftHand ? "leftArm" : "rightArm";
        if (part.equals(arm)) return pos ? v : v3(-A * 1.9f, 0, leftHand ? -0.08f : 0.08f);
        return v;
    }

    /**
     * Груша: стойка, руки у подбородка. Удар — та рука, что ударила: 0 — левый боковой (Q), 1 — левый прямой (ЛКМ),
     * 2 — правый прямой (ПКМ), 3 — правый боковой (E). Корпус доворачивается за ударом.
     */
    private Vec3f bag(String part, boolean pos, Vec3f v, float A) {
        int punch = Math.round(shownB);
        boolean rightPunch = punch >= 2, hook = punch == 0 || punch == 3;
        switch (part) {
            case "body" -> { return pos ? v : v3(0, (rightPunch ? 0.28f : -0.28f) * A, 0); }
            case "head" -> { return pos ? v : v3(0.1f, 0, 0); }
            case "rightArm", "leftArm" -> {
                boolean right = part.equals("rightArm");
                float guardY = right ? -0.35f : 0.35f;
                if (right != rightPunch || A < 0.01f) return pos ? v : v3(-1.3f, guardY, 0);
                if (hook) {
                    // Боковой: рука горизонтально, проходит дугой снаружи внутрь.
                    float sweep = right ? lerp(A, 0.7f, -0.45f) : lerp(A, -0.7f, 0.45f);
                    return pos ? v : v3(-PI / 2, sweep, 0);
                }
                // Прямой: рука выпрямляется вперёд.
                return pos ? v3(v.getX(), v.getY(), v.getZ() - A * 3f) : v3(lerp(A, -1.3f, -PI / 2), lerp(A, guardY, 0), 0);
            }
            case "rightLeg" -> { return pos ? v : v3(0.2f, 0, 0.05f); }
            case "leftLeg" -> { return pos ? v : v3(-0.25f, 0, -0.05f); }
            default -> { return v; }
        }
    }

    /** Дорожка: бег на месте со скоростью темпа. */
    private Vec3f treadmill(String part, boolean pos, Vec3f v, float A) {
        float swing = (float) Math.sin(runPhase) * (0.3f + A * 0.6f);
        switch (part) {
            case "rightLeg" -> { return pos ? v : v3(swing, 0, 0); }
            case "leftLeg" -> { return pos ? v : v3(-swing, 0, 0); }
            case "rightArm" -> { return pos ? v : v3(-swing * 0.8f, 0, 0.05f); }
            case "leftArm" -> { return pos ? v : v3(swing * 0.8f, 0, -0.05f); }
            case "head" -> { return pos ? v : v3(0, 0, 0); }
            default -> { return v; }
        }
    }

    /**
     * Отжимания: упор лёжа, тело — прямая доска от носков на полу до плеч; руки вертикально. A = 0 — вверху (руки
     * прямые), 1 — внизу (грудь у пола). Наклон доски τ: бёдра на высоте LEG·sin τ, плечи — 1,29·sin τ.
     */
    private Vec3f pushup(String part, boolean pos, Vec3f v, float A) {
        float tau = lerp(A, (float) Math.asin(ARM / SHOULDER), (float) Math.asin(0.25f / SHOULDER));
        float hip = LEG * (float) Math.sin(tau);
        float plank = PI / 2 - tau;
        switch (part) {
            case "body" -> { return pos ? v3(0, hip - PIVOT, 0) : v3(-plank, 0, 0); }
            case "head" -> { return pos ? v : v3(-plank * 0.6f, 0, 0); }
            case "rightArm", "leftArm" -> {
                float s = Math.max(0, (ARM - SHOULDER * (float) Math.sin(tau)) / PX);
                boolean right = part.equals("rightArm");
                return pos ? v3(v.getX(), v.getY(), v.getZ() + s) : v3(-plank, 0, right ? 0.12f : -0.12f);
            }
            case "rightLeg", "leftLeg" -> { return pos ? v : v3(0, 0, part.equals("rightLeg") ? 0.03f : -0.03f); }
            default -> { return v; }
        }
    }

    /**
     * Приседания: глубина — шкала (A), ступни на полу; корпус чуть вперёд, руки вперёд для равновесия; качает в
     * сторону по маркеру равновесия (B, −1…1).
     */
    private Vec3f squat(String part, boolean pos, Vec3f v, float A) {
        float drop = 0.45f * A, lean = 0.45f * A;
        float hip = 0.703f - drop;
        float theta = (float) Math.acos(Math.max(-1, Math.min(1, hip / LEG)));
        float sway = Math.max(-1, Math.min(1, shownB)) * 0.18f;
        switch (part) {
            case "body" -> { return pos ? v3(0, -drop, 0) : v3(-lean, 0, sway); }
            case "head" -> { return pos ? v : v3(-lean * 0.8f, 0, 0); }
            case "rightArm", "leftArm" -> {
                boolean right = part.equals("rightArm");
                return pos ? v : v3(-(1.3f * A + lean), 0, right ? 0.05f : -0.05f);
            }
            case "rightLeg", "leftLeg" -> {
                boolean right = part.equals("rightLeg");
                return pos ? v : v3(-(theta + lean), 0, right ? 0.12f * A : -0.12f * A);
            }
            default -> { return v; }
        }
    }

    /**
     * Пресс: на спине на полу, колени вверх, руки за головой. С каждой клавишей корпус поднимается (A), ноги остаются
     * на месте (их угол возвращает подъём корпуса).
     */
    private Vec3f situp(String part, boolean pos, Vec3f v, float A) {
        float up = A * 1.15f;
        switch (part) {
            case "body" -> { return pos ? v3(0, HALF_DEPTH - PIVOT, 0) : v3(PI / 2 - up, 0, 0); }
            case "head" -> { return pos ? v : v3(0.35f, 0, 0); }
            case "rightArm", "leftArm" -> {
                boolean right = part.equals("rightArm");
                return pos ? v : v3(-PI + 0.3f, 0, right ? -0.6f : 0.6f);
            }
            case "rightLeg", "leftLeg" -> {
                boolean right = part.equals("rightLeg");
                return pos ? v : v3(-(up + 0.55f), 0, right ? 0.06f : -0.06f);
            }
            default -> { return v; }
        }
    }
}
