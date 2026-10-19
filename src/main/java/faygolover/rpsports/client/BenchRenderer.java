package faygolover.rpsports.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import faygolover.rpsports.block.BenchBlockEntity;
import faygolover.rpsports.block.MachineBlock;
import faygolover.rpsports.entity.SeatEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Штанга на скамье: гриф и блины (из окна блинов, по сторонам, от середины к краю). Пока на скамье лежит игрок, гриф
 * у него в руках — по кистям из позы (эмоция жима); иначе — на крюках стоек. Координаты — как у модели «на север» (голова у x = 0, ноги — к +x, гриф поперёк — вдоль z).
 */
public class BenchRenderer implements BlockEntityRenderer<BenchBlockEntity> {
    private static final ResourceLocation BAR_TEX = new ResourceLocation("minecraft", "block/light_gray_concrete");
    private static final ResourceLocation PLATE_TEX = new ResourceLocation("minecraft", "block/black_concrete");
    /** Крюки стоек — на 13,6/16 от края блока со стойкой и высоте 17,6/16. */
    private static final float RACK_X = 13.6f / 16, RACK_Y = 17.6f / 16;
    /** Гриф вдоль z: от −10/16 до 26/16; стойки — z 0…1 и 15…16, блины — снаружи. */
    private static final float BAR_FROM = -10 / 16f, BAR_TO = 26 / 16f, INNER_L = -0.5f / 16, INNER_R = 16.5f / 16;

    public BenchRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public boolean shouldRenderOffScreen(BenchBlockEntity be) {
        return true;
    }

    @Override
    public void render(BenchBlockEntity be, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (!be.getBlockState().hasProperty(MachineBlock.FACING)) return;
        Direction facing = be.getBlockState().getValue(MachineBlock.FACING);
        float barX = RACK_X, barY = RACK_Y;
        AbstractClientPlayer lifter = lifter(be);
        if (lifter != null) {
            org.joml.Vector3f mid = ExerciseAnimations.benchHands(lifter);
            if (mid != null) {
                // Кисти из эмоции: система игрока → мир → блок «на север».
                org.joml.Vector3f w = mid.rotate(new org.joml.Quaternionf().rotationY((float) Math.toRadians(180 - net.minecraft.util.Mth.rotLerp(partial, lifter.yBodyRotO, lifter.yBodyRot))));
                double px = net.minecraft.util.Mth.lerp(partial, lifter.xo, lifter.getX()) - be.getBlockPos().getX() - 0.5;
                double py = net.minecraft.util.Mth.lerp(partial, lifter.yo, lifter.getY()) - be.getBlockPos().getY();
                double pz = net.minecraft.util.Mth.lerp(partial, lifter.zo, lifter.getZ()) - be.getBlockPos().getZ() - 0.5;
                org.joml.Vector3f local = new org.joml.Vector3f((float) (px + w.x()), (float) (py + w.y()), (float) (pz + w.z()))
                        .rotate(new org.joml.Quaternionf().rotationY((float) Math.toRadians(angle(facing))));
                barX = local.x() + 0.5f;
                barY = local.y();
            }
            // Позы из эмоции нет (эмоции не загрузились) — гриф остаётся на стойке.
        }
        TextureAtlasSprite bar = sprite(BAR_TEX), plate = sprite(PLATE_TEX);
        VertexConsumer vc = buffers.getBuffer(RenderType.entitySolid(InventoryMenu.BLOCK_ATLAS));
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-angle(facing)));
        pose.translate(-0.5, 0, -0.5);
        float r = 0.5f / 16;
        box(pose, vc, bar, barX - r, barY - r, BAR_FROM, barX + r, barY + r, BAR_TO, light, overlay);
        for (int side = 0; side < 2; side++) {
            float z = side == 0 ? INNER_L : INNER_R;
            for (int i = 0; i < BenchBlockEntity.PER_SIDE; i++) {
                double kg = be.plateKg(side, i);
                if (kg <= 0) continue;
                float[] size = plateSize(kg);
                float pr = size[0] / 16, t = size[1] / 16;
                float z0 = side == 0 ? z - t : z, z1 = side == 0 ? z : z + t;
                box(pose, vc, plate, barX - pr, barY - pr, z0, barX + pr, barY + pr, z1, light, overlay);
                z = side == 0 ? z0 : z1;
            }
            // Замок за блинами.
            float zc0 = side == 0 ? z - 1f / 16 : z, zc1 = side == 0 ? z : z + 1f / 16;
            box(pose, vc, bar, barX - 1f / 16, barY - 1f / 16, zc0, barX + 1f / 16, barY + 1f / 16, zc1, light, overlay);
        }
        pose.popPose();
    }

    /** Радиус и толщина блина, целые пиксели. */
    private static float[] plateSize(double kg) {
        if (kg >= 20) return new float[]{6, 2};
        if (kg >= 10) return new float[]{5, 2};
        if (kg >= 5) return new float[]{4, 1};
        return new float[]{3, 1};
    }

    private static float angle(Direction facing) {
        return switch (facing) {
            case EAST -> 90;
            case SOUTH -> 180;
            case WEST -> 270;
            default -> 0;
        };
    }

    /** Кто лежит на этой скамье: игрок на сиденье рядом с ней, у которого идёт жим. */
    private static AbstractClientPlayer lifter(BenchBlockEntity be) {
        var level = Minecraft.getInstance().level;
        if (level == null) return null;
        var c = be.getBlockPos().getCenter();
        for (AbstractClientPlayer p : level.players()) {
            if (p.getVehicle() instanceof SeatEntity && p.distanceToSqr(c) < 2.5 && ExerciseAnimations.benchLift(p) >= 0) return p;
        }
        return null;
    }

    private static TextureAtlasSprite sprite(ResourceLocation id) {
        return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(id);
    }

    private static void box(PoseStack pose, VertexConsumer vc, TextureAtlasSprite s, float x0, float y0, float z0,
                            float x1, float y1, float z1, int light, int overlay) {
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();
        float dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
        // Низ, верх, север, юг, запад, восток; UV — по размеру грани (1 блок = вся текстура, не больше).
        quad(vc, m, n, light, overlay, s, dx, dz, 0, -1, 0, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
        quad(vc, m, n, light, overlay, s, dx, dz, 0, 1, 0, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0);
        quad(vc, m, n, light, overlay, s, dx, dy, 0, 0, -1, x1, y1, z0, x1, y0, z0, x0, y0, z0, x0, y1, z0);
        quad(vc, m, n, light, overlay, s, dx, dy, 0, 0, 1, x0, y1, z1, x0, y0, z1, x1, y0, z1, x1, y1, z1);
        quad(vc, m, n, light, overlay, s, dz, dy, -1, 0, 0, x0, y1, z0, x0, y0, z0, x0, y0, z1, x0, y1, z1);
        quad(vc, m, n, light, overlay, s, dz, dy, 1, 0, 0, x1, y1, z1, x1, y0, z1, x1, y0, z0, x1, y1, z0);
    }

    private static void quad(VertexConsumer vc, Matrix4f m, Matrix3f n, int light, int overlay, TextureAtlasSprite s,
                             float w, float h, float nx, float ny, float nz, float... p) {
        float u0 = s.getU0(), v0 = s.getV0();
        float u1 = s.getU(Math.min(16, w * 16)), v1 = s.getV(Math.min(16, h * 16));
        float[][] uv = {{u0, v0}, {u0, v1}, {u1, v1}, {u1, v0}};
        for (int i = 0; i < 4; i++) {
            vc.vertex(m, p[i * 3], p[i * 3 + 1], p[i * 3 + 2]).color(1f, 1f, 1f, 1f).uv(uv[i][0], uv[i][1])
                    .overlayCoords(overlay).uv2(light).normal(n, nx, ny, nz).endVertex();
        }
    }
}
