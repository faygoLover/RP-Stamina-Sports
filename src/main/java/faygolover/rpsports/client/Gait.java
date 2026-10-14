package faygolover.rpsports.client;

import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.core.util.Vec3f;

/**
 * Шаг и лёгкий бег на дорожке — свои позы по мотивам Fresh Animations: Player (FreshLX,
 * https://modrinth.com/resourcepack/fresh-animations-player) — та же идея: ноги и руки в противофазе со смещённой
 * фазой, на каждом шаге корпус подпрыгивает и чуть поворачивается, у бега — наклон вперёд и согнутые колени.
 * {@code ls} — фаза шага (радианы, полный цикл — два шага).
 */
final class Gait {
    private Gait() {}

    private static final float PI = (float) Math.PI;

    private static float rad(float deg) {
        return deg * PI / 180;
    }

    private static float cos(float x) {
        return (float) Math.cos(x);
    }

    private static float sin(float x) {
        return (float) Math.sin(x);
    }

    /** Шаг: ноги ±40°, руки ±48°, лёгкое покачивание. */
    static Vec3f walk(String part, TransformType type, Vec3f v, float ls) {
        boolean rot = type == TransformType.ROTATION, pos = type == TransformType.POSITION;
        switch (part) {
            case "rightLeg" -> {
                if (rot) return new Vec3f(rad(-40 * cos(ls + cos(ls) / 2.5f)), 0, rad(1));
                if (pos) return new Vec3f(v.getX(), v.getY() - Math.max(0, 1.2f * sin(ls + 0.6f)), v.getZ());
            }
            case "leftLeg" -> {
                if (rot) return new Vec3f(rad(40 * cos(ls - cos(ls) / 2.5f)), 0, rad(-1));
                if (pos) return new Vec3f(v.getX(), v.getY() - Math.max(0, -1.2f * sin(ls + 0.6f)), v.getZ());
            }
            case "rightArm" -> {
                if (rot) return new Vec3f(rad(48 * cos(PI / 7 + ls)), rad(4 * cos(ls)), rad(2 + 4 * cos(PI / 5 + ls)));
            }
            case "leftArm" -> {
                if (rot) return new Vec3f(rad(-48 * cos(PI / 7 + ls)), rad(4 * cos(ls)), rad(-2 + 4 * cos(PI / 5 + ls)));
            }
            case "body" -> {
                if (rot) return new Vec3f(rad(2 + 2 * cos(ls * 2)), rad(8 * cos(ls)), rad(-2 * sin(ls)));
                if (pos) return new Vec3f(0, 0.03f * (sin(PI / 4 + ls * 2) + 0.8f), 0);
            }
            case "head" -> {
                if (rot) return new Vec3f(rad(-2 * cos(ls * 2)), rad(-6 * cos(ls)), 0);
            }
            default -> { }
        }
        return v;
    }

    /** Лёгкий бег: ноги ±55° с наклоном вперёд, колени согнуты, руки согнуты и шире, корпус наклонён. */
    static Vec3f jog(String part, TransformType type, Vec3f v, float ls) {
        boolean rot = type == TransformType.ROTATION, pos = type == TransformType.POSITION, bend = type == TransformType.BEND;
        switch (part) {
            case "rightLeg" -> {
                if (rot) return new Vec3f(rad(15 - 55 * cos(ls + cos(ls) / 4)), 0, rad(1));
                if (bend) return new Vec3f(0, 0.4f + 0.5f * Math.max(0, sin(ls + 0.4f)), 0);
                if (pos) return new Vec3f(v.getX(), v.getY() - Math.max(0, 1.8f * sin(ls + 0.3f)), v.getZ() - 1.2f);
            }
            case "leftLeg" -> {
                if (rot) return new Vec3f(rad(15 + 55 * cos(ls - cos(ls) / 4)), 0, rad(-1));
                if (bend) return new Vec3f(0, 0.4f + 0.5f * Math.max(0, -sin(ls + 0.4f)), 0);
                if (pos) return new Vec3f(v.getX(), v.getY() - Math.max(0, -1.8f * sin(ls + 0.3f)), v.getZ() - 1.2f);
            }
            case "rightArm" -> {
                if (rot) return new Vec3f(rad(-15 + 55 * cos(PI / 7 + ls)), rad(-8), rad(6 + 4 * cos(PI / 5 + ls)));
                if (bend) return new Vec3f(0, -1.1f, 0);
            }
            case "leftArm" -> {
                if (rot) return new Vec3f(rad(-15 - 55 * cos(PI / 7 + ls)), rad(8), rad(-6 + 4 * cos(PI / 5 + ls)));
                if (bend) return new Vec3f(0, -1.1f, 0);
            }
            case "body" -> {
                if (rot) return new Vec3f(-rad(8 + 2 * cos(ls * 2)), rad(10 * cos(ls)), rad(-3 * sin(ls)));
                if (pos) return new Vec3f(0, 0.06f * -cos(PI / 4 + ls * 2), 0);
            }
            case "head" -> {
                if (rot) return new Vec3f(rad(8 - 2 * cos(ls * 2)), rad(-8 * cos(ls)), 0);
            }
            default -> { }
        }
        return v;
    }
}
