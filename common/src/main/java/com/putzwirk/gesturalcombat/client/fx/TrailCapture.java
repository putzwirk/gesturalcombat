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
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class TrailCapture {
    private static final Map<BakedModel, Vec3[]> AXES = new IdentityHashMap<>();
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
        model.getTransforms().getTransform(displayContext).apply(leftHand, copy);
        copy.translate(-0.5f, -0.5f, -0.5f);
        Matrix4f matrix = copy.last().pose();
        Vec3[] axis = AXES.computeIfAbsent(model, TrailCapture::measure);
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 first = transform(matrix, axis[0]);
        Vec3 second = transform(matrix, axis[1]);
        first = toWorld(camera, first);
        second = toWorld(camera, second);
        Vec3 eye = owner.getEyePosition();
        Vec3 tip;
        Vec3 base;
        if (first.distanceToSqr(eye) >= second.distanceToSqr(eye)) {
            tip = first;
            base = second;
        } else {
            tip = second;
            base = first;
        }
        if (tip == null || base == null) {
            return;
        }
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

    private static Vec3 transform(Matrix4f matrix, Vec3 point) {
        Vector3f view = new Vector3f((float) point.x, (float) point.y, (float) point.z);
        matrix.transformPosition(view);
        return new Vec3(view.x, view.y, view.z);
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
        if (!any) {
            return FALLBACK;
        }
        Vec3 a = null;
        Vec3 b = null;
        double longest = -1.0;
        for (int i = 0; i < points.length; i++) {
            for (int j = i + 1; j < points.length; j++) {
                if (points[i] == null || points[j] == null) {
                    continue;
                }
                double distance = points[i].distanceToSqr(points[j]);
                if (distance > longest) {
                    longest = distance;
                    a = points[i];
                    b = points[j];
                }
            }
        }
        return a == null || b == null ? FALLBACK : new Vec3[]{a, b};
    }

    private static boolean consider(double[] best, Vec3[] points, int index, double value, Vec3 point) {
        if (value > best[index]) {
            best[index] = value;
            points[index] = point;
        }
        return true;
    }
}
