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

    /** Броня и одежда не для спорта: множитель трат выносливости и усталости. */
    double armorMult(double penalty) {
        return 1 + heavy * penalty;
    }
}
