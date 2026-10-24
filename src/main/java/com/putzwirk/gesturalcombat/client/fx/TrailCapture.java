package com.putzwirk.gesturalcombat.client.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.ForgeHooksClient;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class TrailCapture {
    private static final Map<BakedModel, Vec3[]> EXTREMES = new IdentityHashMap<>();
    private static final Direction[] DIRECTIONS = {
            null, Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
    };
    private static final double EDGE_START = 0.55;
    private static final Vec3[] FALLBACK = {
            new Vec3(0.5, 0.05, 0.5),
            new Vec3(0.5, 1.0, 0.5)
    };

    private static Player owner;
    private static ItemStack stack = ItemStack.EMPTY;
    private static ItemDisplayContext context;
    private static boolean active;

    private TrailCapture() {
    }

    public static void begin(LivingEntity entity, ItemStack itemStack, ItemDisplayContext displayContext, boolean leftHand) {
        if (!(entity instanceof Player player) || itemStack.isEmpty() || displayContext == null) {
            return;
        }
        if (!displayContext.firstPerson() && displayContext != ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                && displayContext != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
            return;
        }
        boolean renderedOffhand = leftHand != (player.getMainArm() == HumanoidArm.LEFT);
        if (!TrailManager.active(player.getId(), renderedOffhand)) {
            return;
        }
        owner = player;
        stack = itemStack;
        context = displayContext;
        active = true;
    }

    public static void end() {
        active = false;
        owner = null;
        stack = ItemStack.EMPTY;
        context = null;
    }

    public static void sample(ItemStack itemStack, ItemDisplayContext displayContext, boolean leftHand,
                              PoseStack poseStack, BakedModel model) {
        if (!active || owner == null || displayContext != context || model == null) {
            return;
        }
        if (!ItemStack.matches(itemStack, stack)) {
            return;
        }
        PoseStack copy = new PoseStack();
        copy.last().pose().set(poseStack.last().pose());
        copy.last().normal().set(poseStack.last().normal());
        ForgeHooksClient.handleCameraTransforms(copy, model, displayContext, leftHand);
        copy.translate(-0.5f, -0.5f, -0.5f);
        Matrix4f matrix = copy.last().pose();
        Vec3[] extremes = EXTREMES.computeIfAbsent(model, TrailCapture::measure);
        Vec3 tip = null;
        Vec3 base = null;
        double far = -1.0;
        double near = Double.MAX_VALUE;
        for (Vec3 point : extremes) {
            Vector3f view = new Vector3f((float) point.x, (float) point.y, (float) point.z);
            matrix.transformPosition(view);
            double distance = view.x * view.x + view.y * view.y + view.z * view.z;
            Vec3 transformed = new Vec3(view.x, view.y, view.z);
            if (distance > far) {
                far = distance;
                tip = transformed;
            }
            if (distance < near) {
                near = distance;
                base = transformed;
            }
        }
        if (tip == null || base == null) {
            return;
        }
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        tip = toWorld(camera, tip);
        base = toWorld(camera, base);
        if (camera.getPosition().distanceToSqr(tip) > 100.0) {
            return;
        }
        if (tip.distanceToSqr(base) < 1.0e-4) {
            return;
        }
        TrailManager.sample(owner.getId(), base.lerp(tip, EDGE_START), tip);
    }

    private static Vec3 toWorld(Camera camera, Vec3 view) {
        Vector3f look = camera.getLookVector();
        Vector3f up = camera.getUpVector();
        Vector3f left = camera.getLeftVector();
        Vec3 right = new Vec3(-left.x(), -left.y(), -left.z());
        Vec3 upAxis = new Vec3(up.x(), up.y(), up.z());
        Vec3 back = new Vec3(-look.x(), -look.y(), -look.z());
        return camera.getPosition()
                .add(right.scale(view.x))
                .add(upAxis.scale(view.y))
                .add(back.scale(view.z));
    }

    private static Vec3[] measure(BakedModel model) {
        double[] best = {-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE,
                -Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE};
        Vec3[] points = new Vec3[6];
        RandomSource random = RandomSource.create();
        boolean any = false;
        for (Direction direction : DIRECTIONS) {
            List<BakedQuad> quads = model.getQuads(null, direction, random);
            if (quads == null) {
                continue;
            }
            for (BakedQuad quad : quads) {
                int[] data = quad.getVertices();
                int stride = data.length / 4;
                for (int vertex = 0; vertex < 4; vertex++) {
                    int offset = vertex * stride;
                    float x = Float.intBitsToFloat(data[offset]);
                    float y = Float.intBitsToFloat(data[offset + 1]);
                    float z = Float.intBitsToFloat(data[offset + 2]);
                    Vec3 point = new Vec3(x, y, z);
                    any |= consider(best, points, 0, x, point);
                    any |= consider(best, points, 1, -x, point);
                    any |= consider(best, points, 2, y, point);
                    any |= consider(best, points, 3, -y, point);
                    any |= consider(best, points, 4, z, point);
                    any |= consider(best, points, 5, -z, point);
                }
            }
        }
        return any ? points : FALLBACK;
    }

    private static boolean consider(double[] best, Vec3[] points, int index, double value, Vec3 point) {
        if (value > best[index]) {
            best[index] = value;
            points[index] = point;
        }
        return true;
    }
}
