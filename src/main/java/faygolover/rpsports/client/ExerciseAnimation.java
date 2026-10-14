package faygolover.rpsports.client;

import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonConfiguration;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.core.util.Vec3f;
import faygolover.rpsports.core.Exercise;

/**
 * Поза игрока на тренировке через player-animation-lib: не записанная анимация, а поза из состояния мини-игры
 * (docs/00 «Анимации») — насколько заполнена шкала, настолько поднята штанга. Части модели: head, torso, rightArm,
 * leftArm, rightLeg, leftLeg; «body» — всё тело (сдвиг в блоках, поворот вокруг точки на высоте 0,7).
 */
public final class ExerciseAnimation implements IAnimation {
    private static final float PI = (float) Math.PI;

    /** −1 — нет упражнения. */
    int exercise = -1;
    float a, b, prevA, prevB;
    /** Гантель в левой руке. */
    boolean leftHand;
    private int ticks;

    void set(int exercise, float a, float b) {
        if (this.exercise != exercise) {
            prevA = a;
            prevB = b;
        }
        this.exercise = exercise;
        this.a = a;
        this.b = b;
    }

    @Override
    public boolean isActive() {
        return exercise >= 0;
    }

    @Override
    public void tick() {
        ticks++;
        prevA += (a - prevA) * 0.5f;
        prevB += (b - prevB) * 0.5f;
    }

    @Override
    public void setupAnim(float tickDelta) {}

    @Override
    public FirstPersonMode getFirstPersonMode(float tickDelta) {
        return exercise == Exercise.DUMBBELL.ordinal() ? FirstPersonMode.THIRD_PERSON_MODEL : FirstPersonMode.NONE;
    }

    @Override
    public FirstPersonConfiguration getFirstPersonConfiguration(float tickDelta) {
        return new FirstPersonConfiguration(!leftHand, leftHand, !leftHand, leftHand);
    }

    private static Vec3f rot(Vec3f v, float x, float y, float z) {
        return new Vec3f(x, y, z);
    }

    @Override
    public Vec3f get3DTransform(String part, TransformType type, float tickDelta, Vec3f v) {
        if (exercise < 0 || type == TransformType.BEND) return v;
        float A = prevA + (a - prevA) * tickDelta;
        float t = ticks + tickDelta;
        Exercise e = Exercise.VALUES[exercise];
        boolean pos = type == TransformType.POSITION;
        switch (e) {
            case BENCH -> {
                // Лёжа на спине; руки вверх: шкала пуста — штанга у груди, полна — руки прямые.
                if (part.equals("body")) return pos ? new Vec3f(0, -0.45f, 0) : new Vec3f(-PI / 2, 0, 0);
                if (part.equals("rightArm") || part.equals("leftArm")) return pos ? v : rot(v, -1.0f - A * (PI / 2 - 1.0f), 0, part.equals("rightArm") ? 0.1f : -0.1f);
                if (part.endsWith("Leg")) return pos ? v : rot(v, 0, 0, part.equals("rightLeg") ? 0.05f : -0.05f);
            }
            case PULLUP -> {
                // Руки вверх на перекладину, тело поднимается вместе с кольцом.
                if (part.equals("body")) return pos ? new Vec3f(0, A * 0.55f, 0) : v;
                if (part.equals("rightArm") || part.equals("leftArm")) return pos ? v : rot(v, -PI + A * 0.5f, 0, part.equals("rightArm") ? -0.2f : 0.2f);
                if (part.endsWith("Leg")) return pos ? v : rot(v, 0.25f, 0, 0);
            }
            case DUMBBELL -> {
                String arm = leftHand ? "leftArm" : "rightArm";
                if (part.equals(arm)) return pos ? v : rot(v, -A * 1.9f, 0, leftHand ? 0.1f : -0.1f);
            }
            case BAG -> {
                // Стойка; удар той рукой, что нажал.
                boolean right = b >= 0.5f;
                if (part.equals("rightArm")) return pos ? v : rot(v, right ? -0.6f - A * 1.0f : -0.6f, 0, 0);
                if (part.equals("leftArm")) return pos ? v : rot(v, !right ? -0.6f - A * 1.0f : -0.6f, 0, 0);
                if (part.equals("torso")) return pos ? v : rot(v, 0, right ? -A * 0.3f : A * 0.3f, 0);
            }
            case TREADMILL -> {
                // Бег на месте со скоростью темпа.
                float phase = t * (0.15f + A * 0.6f);
                float swing = (float) Math.sin(phase) * (0.3f + A * 0.7f);
                if (part.equals("rightLeg")) return pos ? v : rot(v, swing, 0, 0);
                if (part.equals("leftLeg")) return pos ? v : rot(v, -swing, 0, 0);
                if (part.equals("rightArm")) return pos ? v : rot(v, -swing * 0.8f, 0, 0);
                if (part.equals("leftArm")) return pos ? v : rot(v, swing * 0.8f, 0, 0);
            }
            case PUSHUP -> {
                // Лицом вниз; вниз-вверх на каждое нажатие.
                if (part.equals("body")) return pos ? new Vec3f(0, -0.95f + (1 - A) * 0.3f, 0) : new Vec3f(PI / 2, 0, 0);
                if (part.equals("rightArm") || part.equals("leftArm")) return pos ? v : rot(v, -PI / 2 + A * 0.6f, 0, 0);
            }
            case SQUAT -> {
                // Глубина приседа — шкала, корпус клонится по равновесию.
                if (part.equals("body")) return pos ? new Vec3f(0, -A * 0.45f, 0) : new Vec3f(0, 0, b * 0.3f);
                if (part.endsWith("Leg")) return pos ? v : rot(v, -A * 1.4f, 0, 0);
                if (part.equals("torso")) return pos ? v : rot(v, A * 0.5f, 0, 0);
                if (part.equals("rightArm") || part.equals("leftArm")) return pos ? v : rot(v, -A * 1.3f, 0, 0);
            }
            case SITUP -> {
                // Лёжа на спине, колени согнуты; корпус поднимается с каждой клавишей.
                if (part.equals("body")) return pos ? new Vec3f(0, -0.85f, 0) : new Vec3f(-PI / 2 + A * 1.0f, 0, 0);
                if (part.endsWith("Leg")) return pos ? v : rot(v, -0.9f - A * 0.6f, 0, 0);
                if (part.equals("rightArm") || part.equals("leftArm")) return pos ? v : rot(v, -0.4f, 0, part.equals("rightArm") ? 0.6f : -0.6f);
            }
        }
        return v;
    }
}
