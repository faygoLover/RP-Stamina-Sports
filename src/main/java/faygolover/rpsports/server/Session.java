package faygolover.rpsports.server;

import faygolover.rpsports.core.Exercise;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** Идущее упражнение игрока (только сервер, не сохраняется). */
final class Session {
    final Exercise exercise;
    @Nullable final BlockPos pos;
    final InteractionHand hand;
    final double weightKg;
    final double ratio;
    final int strength, fitness;
    final double ease;
    final int program;
    final boolean onMat;
    final int clothing, heavy;
    long started;
    long lastRep;
    int reps;
    int errors;
    float a, b;
    long lastRelay;
    @Nullable UUID spotter;
    @Nullable Integer seatId;
    /** Плохое событие уже случилось — не повторять, ждать конца. */
    boolean failed;
    /** Гантели в обеих руках: {@link #hand} — основная (правая), вторая — {@link #weightKg2}, {@link #ratio2}. */
    boolean dual;
    double weightKg2, ratio2;
    /** Какой рукой был последний повтор (у двух гантелей) и когда — по каждой руке (0 — основная, 1 — вторая). */
    InteractionHand lastHand = InteractionHand.MAIN_HAND;
    final long[] lastRepHand = new long[2];
    /** Серия удачных подряд, лучшая серия и объём подхода (повторы × вес) — для рекордов. */
    int series, bestSeries;
    double volume;
    /** Выносливость повтора, которую ещё не списали: тратится плавно за ~секунду, а не рывком. */
    double pendingStamina;
    /** Точка, где игрок стоит всё упражнение (null — может ходить: гантели; или сидит на сиденье: скамья). */
    @Nullable net.minecraft.world.phys.Vec3 anchor;
    float yaw;

    Session(Exercise exercise, @Nullable BlockPos pos, InteractionHand hand, double weightKg, double ratio, int strength,
            int fitness, double ease, int program, boolean onMat, int clothing, int heavy) {
        this.exercise = exercise;
        this.pos = pos;
        this.hand = hand;
        this.weightKg = weightKg;
        this.ratio = ratio;
        this.strength = strength;
        this.fitness = fitness;
        this.ease = ease;
        this.program = program;
        this.onMat = onMat;
        this.clothing = clothing;
        this.heavy = heavy;
    }

    /** Рабочая рука гантели сейчас. */
    InteractionHand workHand() {
        return dual ? lastHand : hand;
    }

    /** Броня и одежда не для спорта: множитель трат выносливости и усталости. */
    double armorMult(double penalty) {
        return 1 + heavy * penalty;
    }
}
