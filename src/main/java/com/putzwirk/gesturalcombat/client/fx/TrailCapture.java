package com.putzwirk.gesturalcombat.client.fx;

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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.ForgeHooksClient;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class TrailCapture {
    private static final Map<BakedModel, AABB> BOUNDS = new IdentityHashMap<>();
    private static final AABB FALLBACK = new AABB(0.45, 0.05, 0.45, 0.55, 1.0, 0.55);
    private static final Direction[] DIRECTIONS = {
            null, Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
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
        AABB bounds = BOUNDS.computeIfAbsent(model, TrailCapture::measure);
        Vec3 reference = Vec3.ZERO;
        Vec3 tip = null;
        Vec3 base = null;
        double far = -1.0;
        double near = Double.MAX_VALUE;
        for (int index = 0; index < 8; index++) {
            double x = (index & 1) == 0 ? bounds.minX : bounds.maxX;
            double y = (index & 2) == 0 ? bounds.minY : bounds.maxY;
            double z = (index & 4) == 0 ? bounds.minZ : bounds.maxZ;
            Vector3f point = new Vector3f((float) x, (float) y, (float) z);
            matrix.transformPosition(point);
            Vec3 world = new Vec3(point.x, point.y, point.z);
            double distance = world.distanceToSqr(reference);
            if (distance > far) {
                far = distance;
                tip = world;
            }
            if (distance < near) {
                near = distance;
                base = world;
            }
        }
        if (tip == null || base == null) {
            return;
        }
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Quaternionf inverse = new Quaternionf(camera.rotation()).conjugate();
        Vec3 offset = camera.getPosition();
        tip = rotate(tip, inverse).add(offset);
        base = rotate(base, inverse).add(offset);
        if (offset.distanceToSqr(tip) > 100.0) {
            return;
        }
        if (!finite(tip) || !finite(base) || tip.distanceToSqr(base) < 1.0e-4) {
            return;
        }
        TrailManager.sample(owner.getId(), base, tip);
    }

    private static Vec3 rotate(Vec3 point, Quaternionf rotation) {
        Vector3f vector = new Vector3f((float) point.x, (float) point.y, (float) point.z);
        vector.rotate(rotation);
        return new Vec3(vector.x, vector.y, vector.z);
    }

    private static boolean finite(Vec3 value) {
        return Double.isFinite(value.x) && Double.isFinite(value.y) && Double.isFinite(value.z);
    }

    private static AABB measure(BakedModel model) {
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        double maxZ = -Double.MAX_VALUE;
        boolean any = false;
        RandomSource random = RandomSource.create();
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
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    minZ = Math.min(minZ, z);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                    maxZ = Math.max(maxZ, z);
                    any = true;
                }
            }
        }
        if (!any) {
            return FALLBACK;
        }
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }
}
