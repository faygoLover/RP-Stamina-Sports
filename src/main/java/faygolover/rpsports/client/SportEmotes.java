package faygolover.rpsports.client;

import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.data.gson.AnimationSerializing;
import dev.kosmx.playerAnim.core.util.Vec3f;
import faygolover.rpsports.RpSports;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Эмоции Emotecraft для упражнений ({@code assets/rpsports/sport_emotes/<id>.json}, переведены из .emotecraft
 * утилитой {@code tools/EmoteConvert.java}; исходники — {@code emotes_src/}). Грузятся из jar аддона напрямую, а не через
 * реестр библиотеки: тот называет эмоции по их внутреннему имени, а у этих там ключи перевода чужого мода.
 *
 * <p>Эмоцию не проигрываем по времени, а берём кадр на нужном (дробном) тике: шкала мини-игры → тик в отрезке
 * эмоции (например, жим: 57 — штанга у груди, 67 — руки прямые). Левая рука — зеркало правой.
 */
final class SportEmotes {
    private SportEmotes() {}

    private static final Map<String, KeyframeAnimation> LOADED = new HashMap<>();
    /** Кадры: эмоция и целый тик → готовый проигрыватель (дробную часть даёт tickDelta). */
    private static final Map<String, KeyframeAnimationPlayer> FRAMES = new LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, KeyframeAnimationPlayer> e) {
            return size() > 96;
        }
    };

    @Nullable
    static KeyframeAnimation get(String id) {
        return LOADED.computeIfAbsent(id, SportEmotes::load);
    }

    @Nullable
    private static KeyframeAnimation load(String id) {
        try (InputStream in = SportEmotes.class.getResourceAsStream("/assets/rpsports/sport_emotes/" + id + ".json")) {
            if (in == null) return null;
            List<KeyframeAnimation> list = AnimationSerializing.deserializeAnimation(in);
            return list.isEmpty() ? null : list.get(0);
        } catch (Exception e) {
            RpSports.LOGGER.warn("RP Sports: не загрузилась эмоция {}: {}", id, e.toString());
            return null;
        }
    }

    /** Значение части {@code part} на тике {@code tick} (дробном). {@code def} — что вернуть, если часть не задана. */
    static Vec3f sample(String id, float tick, String part, TransformType type, Vec3f def) {
        KeyframeAnimation anim = get(id);
        if (anim == null) return def;
        int t = (int) Math.floor(tick);
        float frac = tick - t;
        KeyframeAnimationPlayer p = FRAMES.computeIfAbsent(id + "@" + t, k -> new KeyframeAnimationPlayer(anim, t));
        // Дробную часть кадра библиотека берёт из setupAnim, а не из параметра get3DTransform (без этого — ступеньки).
        p.setupAnim(frac);
        return p.get3DTransform(part, type, frac, def);
    }

    /** То же, но зеркально (левая ↔ правая): берём парную часть и отражаем. */
    static Vec3f sampleMirrored(String id, float tick, String part, TransformType type, Vec3f def) {
        String src = mirrorName(part);
        Vec3f d = mirror(type, def);
        return mirror(type, sample(id, tick, src, type, d));
    }

    static String mirrorName(String part) {
        if (part.startsWith("left")) return "right" + part.substring(4);
        if (part.startsWith("right")) return "left" + part.substring(5);
        return part;
    }

    /** Отражение слева направо: сдвиг по x, повороты вокруг y и z, ось сгиба. */
    static Vec3f mirror(TransformType type, Vec3f v) {
        return switch (type) {
            case POSITION -> new Vec3f(-v.getX(), v.getY(), v.getZ());
            case ROTATION -> new Vec3f(v.getX(), -v.getY(), -v.getZ());
            case BEND -> new Vec3f(-v.getX(), v.getY(), v.getZ());
        };
    }

    // ------------------------------------------------------------------ где кисти

    /** Источник поз частей (наша анимация упражнения). */
    interface Pose {
        Vec3f get(String part, TransformType type, Vec3f def);
    }

    private static final float PX = 1 / 16f, SCALE = 0.9375f;

    /**
     * Кисть руки в системе игрока (как у библиотеки: блоки от ног, вперёд — −Z, вверх — +Y, вправо — +X), до поворота
     * на взгляд. Цепочка как при отрисовке: точка кисти в руке → сгиб в локте → поза руки → модель → «body».
     */
    static Vector3f hand(Pose pose, boolean right, boolean bendOn) {
        String arm = right ? "rightArm" : "leftArm";
        Vec3f defPos = new Vec3f(right ? -5f : 5f, 2f, 0f);
        Vec3f pos = pose.get(arm, TransformType.POSITION, defPos);
        Vec3f rot = pose.get(arm, TransformType.ROTATION, new Vec3f(0, 0, 0));
        Vector3f h = new Vector3f((right ? -1 : 1) * PX, 10 * PX, 0);
        if (bendOn) {
            // Как у библиотеки для предметов в руке: локоть на 0,375 от плеча, ось (cos −a, 0, sin −a), угол — сгиб.
            Vec3f bend = pose.get(arm, TransformType.BEND, new Vec3f(0, 0, 0));
            float axis = -bend.getX();
            Vector3f elbow = new Vector3f(0, 0.375f, 0);
            h.sub(elbow).rotate(new Quaternionf().rotateAxis(bend.getY(), (float) Math.cos(axis), 0, (float) Math.sin(axis))).add(elbow);
        }
        h.rotate(new Quaternionf().rotationZYX(rot.getZ(), rot.getY(), rot.getX()));
        h.add(pos.getX() * PX, pos.getY() * PX, pos.getZ() * PX);
        // Модель → система игрока: сдвиг (0, −1,501), масштаб 0,9375, отражение (−1, −1, 1).
        h.add(0, -1.501f, 0).mul(-SCALE, -SCALE, SCALE);
        Vec3f bPos = pose.get("body", TransformType.POSITION, new Vec3f(0, 0, 0));
        Vec3f bRot = pose.get("body", TransformType.ROTATION, new Vec3f(0, 0, 0));
        h.sub(0, 0.7f, 0).rotate(new Quaternionf().rotationZYX(bRot.getZ(), bRot.getY(), bRot.getX()))
                .add(bPos.getX(), bPos.getY() + 0.7f, bPos.getZ());
        return h;
    }
}
