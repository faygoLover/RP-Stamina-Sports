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
 * <p>Половина толщины тела модели — 0,117 блока; бёдра (точка поворота тела) — 0,7 над ногами.
 */
public final class ExerciseAnimation implements IAnimation {
    private static final float PI = (float) Math.PI;
    /** Пиксель модели в блоках (масштаб игрока 0,9375). */
    private static final float PX = 0.9375f / 16f;
    private static final float HALF_DEPTH = 0.117f, PIVOT = 0.7f;

    private final AbstractClientPlayer player;
    /** −1 — нет упражнения. */
    int exercise = -1;
    float a, b;
    /** Гантель в левой руке. */
    boolean leftHand;
    private float shownA, shownB, runPhase;
    /** Тики эмоций, которые идут по времени: бег (со скоростью темпа), боевая стойка. */
    private float runTick, stanceTick;
    /** Темп дорожки, сглаженный сильнее (выбор шага / лёгкого бега / бега). */
    private float shownTempo;
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
        // Шкальные упражнения — сильно плавнее (кадр эмоции идёт за шкалой мягко, без рывков от нажатий).
        boolean soft = exercise == Exercise.BENCH.ordinal() || exercise == Exercise.PULLUP.ordinal()
                || exercise == Exercise.SQUAT.ordinal() || exercise == Exercise.DUMBBELL.ordinal() || exercise == Exercise.PUSHUP.ordinal();
        float kA = soft ? 1 - (float) Math.exp(-6 * dt) : k;
        shownA += (a - shownA) * kA;
        shownTempo += (a - shownTempo) * (1 - (float) Math.exp(-2.5f * dt));
        // У груши b — какой удар (не плавно), у остальных — плавно.
        shownB = exercise == Exercise.BAG.ordinal() || exercise == Exercise.DUMBBELL.ordinal() ? b : shownB + (b - shownB) * k;
        // Фаза шага: шаг ~1,4 шага в секунду, лёгкий бег быстрее (бег из эмоции — своим счётчиком runTick).
        if (exercise == Exercise.TREADMILL.ordinal()) runPhase += dt * (5.5f + 8f * shownTempo);
        runTick += dt * 20 * (0.5f + 1.5f * shownA);
        stanceTick += dt * 20;
        aimPunch();
        // Тело — по углу, на котором игрока поставил тренажёр (иначе оно «доворачивается» за головой). Свой персонаж
        // стоит на этом угле (CameraControl держит его поворот), чужие приходят с ним же с сервера.
        if (exercise >= 0 && exercise != Exercise.DUMBBELL.ordinal()) {
            Float locked = player == net.minecraft.client.Minecraft.getInstance().player ? CameraControl.lockedYaw() : null;
            float body = locked != null ? locked : player.getYRot();
            player.yBodyRot = player.yBodyRotO = body;
            player.yHeadRot = player.yHeadRotO = body;
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


    @Override
    public Vec3f get3DTransform(String part, TransformType type, float tickDelta, Vec3f v) {
        if (exercise < 0) return v;
        Vec3f e = emote(part, type, v);
        if (e != null) return e;
        // Позы всех упражнений — из эмоций Emotecraft; своя поза только у пресса (эмоции для него нет).
        if (exercise != Exercise.SITUP.ordinal()) return v;
        if (type == TransformType.BEND) return situpBend(part, v);
        return situp(part, type == TransformType.POSITION, v, Math.max(0, Math.min(1, shownA)));
    }

    // ------------------------------------------------------------------ эмоции Emotecraft

    /** Жим: насколько поднять лежащего (эмоция лежит на полу, у нас — на подушке скамьи, 8/16). */
    private static final float BENCH_LIFT = 0.62f;
    /** Высота перекладины турника над ногами, блоки. */
    private static final float PULLUP_BAR = 2.406f;
    /** Сдвиг вверх у турника, чтобы кисти были на нашей перекладине (считается один раз по эмоции). */
    private static float pullupLift = Float.NaN;

    private static boolean bendOn() {
        return dev.kosmx.playerAnim.impl.Helper.isBendEnabled();
    }

    /**
     * Поза из эмоции для этой части, или null — эмоции нет (тогда своя поза). Отрезки кадров — по разбору эмоций
     * (docs/test_findings, часть 2): жим 57 (у груди) → 67 (руки прямые); турник 59 (вис) → 78 (подбородок над
     * перекладиной); отжимания 4 (вверху) → 9,5 (внизу); приседания 0 (стоя) → 4 (внизу); гантель 10 (рука внизу) → 29
     * (вверху); бег — цикл 5…28 со скоростью темпа; груша — стойка (цикл 15…47), удар — руки из «Карате».
     */
    @org.jetbrains.annotations.Nullable
    private Vec3f emote(String part, TransformType type, Vec3f v) {
        float A = Math.max(0, Math.min(1, shownA));
        switch (Exercise.VALUES[exercise]) {
            case BENCH -> {
                if (SportEmotes.get("bench") == null) return null;
                Vec3f r = SportEmotes.sample("bench", 57 + A * 10, part, type, v);
                return part.equals("body") && type == TransformType.POSITION ? new Vec3f(r.getX(), r.getY() + BENCH_LIFT, r.getZ()) : r;
            }
            case PULLUP -> {
                if (SportEmotes.get("pullup") == null) return null;
                Vec3f r = SportEmotes.sample("pullup", 59 + A * 19, part, type, v);
                if (part.equals("body") && type == TransformType.POSITION) return new Vec3f(r.getX(), r.getY() + pullupLift(), r.getZ());
                return r;
            }
            case PUSHUP -> {
                if (SportEmotes.get("pushup") == null) return null;
                Vec3f r = SportEmotes.sample("pushup", 4 + A * 5.5f, part, type, v);
                // Глубже, чем в эмоции: внизу тело ниже, руки согнуты сильнее.
                if (part.equals("body") && type == TransformType.POSITION) return new Vec3f(r.getX(), r.getY() - 0.12f * A, r.getZ());
                if (part.endsWith("Arm") && type == TransformType.BEND) return new Vec3f(r.getX(), r.getY() * (1 + 0.45f * A), r.getZ());
                return r;
            }
            case SQUAT -> {
                if (SportEmotes.get("squat") == null) return null;
                Vec3f r = SportEmotes.sample("squat", A * 4, part, type, v);
                // Сильно глубже эмоции: бедро почти горизонтально, колени согнуты сильнее, таз ниже, корпус вперёд.
                if (part.equals("body") && type == TransformType.ROTATION) {
                    float sway = Math.max(-1, Math.min(1, shownB)) * 0.18f;
                    return new Vec3f(r.getX() - 0.25f * A, r.getY(), r.getZ() + sway);
                }
                if (part.equals("body") && type == TransformType.POSITION) return new Vec3f(r.getX(), r.getY() - 0.24f * A, r.getZ());
                if (part.endsWith("Leg") && type == TransformType.ROTATION) return new Vec3f(r.getX() - 0.6f * A, r.getY(), r.getZ());
                if (part.endsWith("Leg") && type == TransformType.BEND) return new Vec3f(r.getX(), r.getY() + 0.55f * A, r.getZ());
                return r;
            }
            case DUMBBELL -> {
                if (SportEmotes.get("dumbbell") == null) return null;
                if (shownB >= 2) {
                    // Две гантели: правая рука — эмоция, левая — её зеркало со своим подъёмом; корпус — ровно.
                    float leftA = Math.max(0, Math.min(1, shownB - 2));
                    if (part.equals("rightArm")) return SportEmotes.sample("dumbbell", 10 + A * 19, part, type, v);
                    if (part.equals("leftArm")) return SportEmotes.sampleMirrored("dumbbell", 10 + leftA * 19, part, type, v);
                    // Остальное тело — ровно, как стоит (эмоция одной руки клонит корпус и голову).
                    return v;
                }
                return leftHand ? SportEmotes.sampleMirrored("dumbbell", 10 + A * 19, part, type, v)
                        : SportEmotes.sample("dumbbell", 10 + A * 19, part, type, v);
            }
            case TREADMILL -> {
                if (SportEmotes.get("run") == null) return null;
                // Темп: первая треть — шаг, вторая — лёгкий бег, последняя — бег из эмоции; переходы плавные.
                float t = shownTempo;
                float wRun = smooth((t - 0.62f) / 0.1f), wJog = smooth((t - 0.28f) / 0.1f) - wRun, wWalk = 1 - wJog - wRun;
                Vec3f run = wRun > 0.001f ? SportEmotes.sample("run", 5 + (runTick % 23), part, type, v) : v;
                Vec3f walk = wWalk > 0.001f ? Gait.walk(part, type, v, runPhase) : v;
                Vec3f jog = wJog > 0.001f ? Gait.jog(part, type, v, runPhase) : v;
                return new Vec3f(walk.getX() * wWalk + jog.getX() * wJog + run.getX() * wRun,
                        walk.getY() * wWalk + jog.getY() * wJog + run.getY() * wRun,
                        walk.getZ() * wWalk + jog.getZ() * wJog + run.getZ() * wRun);
            }
            case BAG -> {
                if (SportEmotes.get("stance") == null) return null;
                Vec3f base = SportEmotes.sample("stance", 15 + (stanceTick % 32), part, type, v);
                int lane = Math.round(shownB);
                boolean right = lane >= 2, hook = lane == 0 || lane == 3;
                if (A > 0.01f && SportEmotes.get("karate") != null) {
                    String arm = right ? "rightArm" : "leftArm";
                    if (part.equals(arm)) {
                        Vec3f r = punchArm(part, type, v, right);
                        // Дотянуться до груши: рука вперёд на столько, сколько не хватает кулаку.
                        return type == TransformType.POSITION ? new Vec3f(r.getX(), r.getY(), r.getZ() - bagReach) : r;
                    }
                    if (part.equals("body") && type == TransformType.ROTATION)
                        return new Vec3f(base.getX(), base.getY() + bagTwist + (hook ? (right ? 0.2f : -0.2f) * A : 0), base.getZ());
                }
                return base;
            }
            default -> {
                return null;
            }
        }
    }

    /** Доворот корпуса к груше и вынос руки вперёд (пиксели) — считаются раз за кадр по кулаку из эмоции. */
    private float bagTwist, bagReach;

    /** Рука удара из «Карате»: правая — кадры 8…20, левая — 15…30 (u: 0 — удар начался, 1 — рука вернулась). */
    private Vec3f punchArm(String part, TransformType type, Vec3f v, boolean right) {
        float u = 1 - Math.max(0, Math.min(1, shownA));
        return SportEmotes.sample("karate", right ? 8 + u * 12 : 15 + u * 15, part, type, v);
    }

    /**
     * Груша: где кулак ударной руки (система игрока: вперёд — −Z, вправо — +X) без доворота; повернуть корпус так,
     * чтобы он шёл в центр груши (x = 0), и вынести руку, если до груши (передняя грань ≈ 0,75 вперёд) не хватает.
     */
    private void aimPunch() {
        bagTwist = 0;
        bagReach = 0;
        if (exercise != Exercise.BAG.ordinal() || shownA <= 0.01f || SportEmotes.get("karate") == null || SportEmotes.get("stance") == null) return;
        boolean right = Math.round(shownB) >= 2;
        String arm = right ? "rightArm" : "leftArm";
        SportEmotes.Pose pose = (p, t, d) -> p.equals(arm) ? punchArm(p, t, d, right)
                : SportEmotes.sample("stance", 15 + (stanceTick % 32), p, t, d);
        org.joml.Vector3f fist = SportEmotes.hand(pose, right, bendOn());
        if (fist.z() < -0.05f) {
            // Поворот вокруг вертикали: x' = x·cos θ + z·sin θ = 0.
            bagTwist = Math.max(-1f, Math.min(1f, (float) Math.atan(-fist.x() / fist.z())));
            float dist = (float) Math.sqrt(fist.x() * fist.x() + fist.z() * fist.z());
            bagReach = Math.max(0, Math.min(5, (0.78f - dist) / PX));
        }
    }

    private static float smooth(float x) {
        x = Math.max(0, Math.min(1, x));
        return x * x * (3 - 2 * x);
    }

    /** Турник: сдвиг тела вверх, чтобы кисти в висе (тик 59) были на нашей перекладине. */
    private float pullupLift() {
        if (Float.isNaN(pullupLift)) {
            SportEmotes.Pose raw = (p, t, d) -> SportEmotes.sample("pullup", 59, p, t, d);
            float hands = (SportEmotes.hand(raw, true, bendOn()).y() + SportEmotes.hand(raw, false, bendOn()).y()) / 2;
            pullupLift = PULLUP_BAR - hands;
        }
        return pullupLift;
    }

    /** Середина между кистями в системе игрока (для грифа); null — поза не из эмоции. */
    @org.jetbrains.annotations.Nullable
    org.joml.Vector3f handsMid() {
        if (exercise < 0 || SportEmotes.get("bench") == null) return null;
        SportEmotes.Pose pose = (p, t, d) -> {
            Vec3f r = emote(p, t, d);
            return r == null ? d : r;
        };
        return SportEmotes.hand(pose, true, bendOn()).add(SportEmotes.hand(pose, false, bendOn())).mul(0.5f);
    }

    /** Подъём штанги сейчас (для отрисовки грифа), −1 — не жмёт. */
    float benchLift() {
        return exercise == Exercise.BENCH.ordinal() ? Math.max(0, Math.min(1, shownA)) : -1;
    }

    /** Пресс: бедро поднято от пола на этот угол (рад). */
    private static final float SITUP_THIGH = 0.58f;
    /**
     * Пресс: сгиб колена (рад), чтобы стопа встала на пол у таза. Бедро и голень — по 6 пикселей, таз на ~2 пикселя
     * над полом: колено на 6·sin(бедро) + 2, голень опускается до пола — сгиб = бедро + asin(высота колена / 6).
     */
    private static final float SITUP_KNEE = SITUP_THIGH + (float) Math.asin(Math.min(1, (6 * Math.sin(SITUP_THIGH) + 2) / 6));

    /**
     * Пресс: согнутые колени (стопы на полу) и руки за головой — плечо вверх и в стороны, локоть согнут на ~90°
     * внутрь, кисти у затылка. Сгиб руки: ось поворачивает плоскость сгиба к середине тела (у левой — зеркально),
     * угол отрицательный, как у обычного сгиба локтя в эмоциях.
     */
    private static Vec3f situpBend(String part, Vec3f v) {
        return switch (part) {
            case "rightLeg", "leftLeg" -> v3(0, SITUP_KNEE, 0);
            case "rightArm" -> v3(1.15f, -1.56f, 0);
            case "leftArm" -> v3(-1.15f, -1.56f, 0);
            default -> v;
        };
    }

    /**
     * Пресс: на спине на полу, колени согнуты и подняты, стопы на полу, руки за головой. С каждой клавишей корпус
     * поднимается (A), ноги остаются на месте (их угол возвращает подъём корпуса).
     */
    private Vec3f situp(String part, boolean pos, Vec3f v, float A) {
        float up = A * 1.15f;
        switch (part) {
            case "body" -> { return pos ? v3(0, HALF_DEPTH - PIVOT, 0) : v3(PI / 2 - up, 0, 0); }
            case "head" -> { return pos ? v : v3(0.35f, 0, 0); }
            case "rightArm", "leftArm" -> {
                boolean right = part.equals("rightArm");
                // Плечо вверх (вдоль тела к голове), чуть назад и в стороны — локти разведены.
                return pos ? v : v3(-(PI + 0.25f), 0, right ? -0.37f : 0.37f);
            }
            case "rightLeg", "leftLeg" -> {
                boolean right = part.equals("rightLeg");
                return pos ? v : v3(-(up + SITUP_THIGH), 0, right ? 0.06f : -0.06f);
            }
            default -> { return v; }
        }
    }
}
