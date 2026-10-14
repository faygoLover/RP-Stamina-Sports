package faygolover.rpsports.block;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

/** Поворот формы блока, заданной для «на север», к нужной стороне. */
final class Shapes2 {
    private Shapes2() {}

    private static final Map<VoxelShape, Map<Direction, VoxelShape>> CACHE = new java.util.IdentityHashMap<>();

    static synchronized VoxelShape rotate(VoxelShape north, Direction facing) {
        return CACHE.computeIfAbsent(north, k -> new EnumMap<>(Direction.class)).computeIfAbsent(facing, f -> {
            VoxelShape[] out = {Shapes.empty()};
            int turns = switch (f) {
                case EAST -> 1;
                case SOUTH -> 2;
                case WEST -> 3;
                default -> 0;
            };
            north.forAllBoxes((x1, y1, z1, x2, y2, z2) -> {
                double ax1 = x1, az1 = z1, ax2 = x2, az2 = z2;
                for (int t = 0; t < turns; t++) {
                    double nx1 = 1 - az2, nz1 = ax1, nx2 = 1 - az1, nz2 = ax2;
                    ax1 = nx1; az1 = nz1; ax2 = nx2; az2 = nz2;
                }
                out[0] = Shapes.or(out[0], Shapes.box(ax1, y1, az1, ax2, y2, az2));
            });
            return out[0];
        });
    }
}
