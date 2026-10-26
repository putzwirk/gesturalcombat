package com.putzwirk.gesturalcombat.client.gesture;

import com.putzwirk.gesturalcombat.core.GestureShape;

import java.util.ArrayList;
import java.util.List;

public final class GestureRecognizer {
    private static final int SAMPLES = 32;
    private static final float MIN_LENGTH = 0.02f;
    private static final float LINE_STRAIGHTNESS = 0.84f;
    private static final float LINE_DEVIATION = 0.17f;
    private static final float LINE_MAX_TURN = 85.0f;
    private static final float CARDINAL_TOLERANCE = 38.0f;
    private static final float CIRCLE_TURN = 245.0f;
    private static final float CIRCLE_CLOSURE = 0.78f;
    private static final float CIRCLE_ASPECT = 2.4f;
    private static final float CIRCLE_LENGTH_RATIO = 1.45f;
    private static final float CIRCLE_RADIUS_VARIATION = 0.85f;
    private static final float TEMPLATE_SPREAD = 0.55f;
    private static final float TEMPLATE_ACCEPT = 0.60f;

    private static final float[][] TEMPLATE_CIRCLE = buildCircle();
    private static final float[][] TEMPLATE_Z = buildPolyline(new float[][]{
            {-0.5f, 0.5f}, {0.5f, 0.5f}, {-0.5f, -0.5f}, {0.5f, -0.5f}});
    private static final float[][] TEMPLATE_S = buildPolyline(new float[][]{
            {0.5f, 0.5f}, {-0.5f, 0.5f}, {0.5f, -0.5f}, {-0.5f, -0.5f}});

    public record Result(GestureShape shape, float confidence) {
    }

    private GestureRecognizer() {
    }

    public static Result recognize(List<float[]> points) {
        if (points == null || points.size() < 2) {
            return new Result(GestureShape.UNKNOWN, 0.0f);
        }
        float length = polylineLength(points);
        if (length < MIN_LENGTH) {
            return new Result(GestureShape.UNKNOWN, 0.0f);
        }
        float[][] stroke = smooth(resample(points, SAMPLES));
        float width = extent(stroke, 0);
        float height = extent(stroke, 1);
        float diagonal = (float) Math.sqrt(width * width + height * height);
        if (diagonal < 1.0e-5f) {
            return new Result(GestureShape.UNKNOWN, 0.0f);
        }
        float chord = distance(stroke[0], stroke[SAMPLES - 1]);
        float ratio = chord / length;
        float deviation = maxChordDeviation(stroke, chord) / diagonal;
        float absoluteTurn = turningAbsolute(stroke);
        float signedTurn = turningDegrees(stroke);
        float consistency = absoluteTurn > 1.0e-3f ? Math.abs(signedTurn) / absoluteTurn : 0.0f;
        float peakTurn = maxTurnDegrees(stroke);
        float closure = chord / diagonal;

        if (isStraight(ratio, deviation, peakTurn)) {
            GestureShape shape = straightShape(principalAngle(stroke));
            if (shape == GestureShape.UNKNOWN) {
                return new Result(GestureShape.UNKNOWN, 0.0f);
            }
            return new Result(shape, straightConfidence(ratio, deviation, absoluteTurn));
        }

        float aspect = Math.min(width, height) > 1.0e-5f
                ? Math.max(width, height) / Math.min(width, height)
                : Float.MAX_VALUE;
        float lengthRatio = length / diagonal;
        float radiusVariation = radiusVariation(stroke);
        if (absoluteTurn >= CIRCLE_TURN
                && closure <= CIRCLE_CLOSURE
                && consistency >= 0.55f
                && aspect <= CIRCLE_ASPECT
                && lengthRatio >= CIRCLE_LENGTH_RATIO
                && radiusVariation <= CIRCLE_RADIUS_VARIATION) {
            float template = templateScore(stroke, TEMPLATE_CIRCLE);
            float confidence = Math.max(template,
                    circleConfidence(absoluteTurn, closure, consistency, lengthRatio));
            return new Result(GestureShape.CIRCLE, confidence);
        }

        Result v = vShape(stroke, width, height, diagonal, absoluteTurn, peakTurn);
        if (v != null) {
            return v;
        }

        float minRun = Math.max(width * 0.08f, diagonal * 0.03f);
        int reversalsX = reversals(stroke, 0, minRun);
        float amplitude = height / diagonal;
        if (reversalsX >= 2 && amplitude >= 0.12f && absoluteTurn >= 50.0f && peakTurn >= 30.0f) {
            float geometric = zigzagConfidence(reversalsX, amplitude, peakTurn, consistency);
            float template = Math.max(templateScore(stroke, TEMPLATE_Z), templateScore(stroke, TEMPLATE_S));
            float confidence = Math.max(geometric, template >= TEMPLATE_ACCEPT ? template : geometric);
            return new Result(GestureShape.ZIGZAG, confidence);
        }

        return new Result(GestureShape.UNKNOWN, 0.0f);
    }

    private static Result vShape(float[][] points, float width, float height, float diagonal, float absoluteTurn, float peakTurn) {
        if (height < 0.03f || absoluteTurn < 40.0f) {
            return null;
        }
        boolean narrow = width < height * 0.30f;
        float thresholdY = Math.max(height * 0.06f, diagonal * 0.025f);
        float thresholdX = Math.max(width * 0.06f, diagonal * 0.025f);
        if (reversals(points, 1, thresholdY) != 1) {
            return null;
        }
        if (!narrow && reversals(points, 0, thresholdX) > 1) {
            return null;
        }
        int lowest = extremeIndex(points, false, height * 0.08f);
        int highest = extremeIndex(points, true, height * 0.08f);
        Result bottom = apexMatch(points, lowest, false, width, height, diagonal, absoluteTurn, narrow, GestureShape.V);
        Result top = apexMatch(points, highest, true, width, height, diagonal, absoluteTurn, narrow, GestureShape.CARET);
        if (bottom == null) {
            return top;
        }
        if (top == null) {
            return bottom;
        }
        return bottom.confidence() >= top.confidence() ? bottom : top;
    }

    private static int extremeIndex(float[][] points, boolean top, float tolerance) {
        float extreme = top ? -Float.MAX_VALUE : Float.MAX_VALUE;
        for (float[] point : points) {
            extreme = top ? Math.max(extreme, point[1]) : Math.min(extreme, point[1]);
        }
        float limit = top ? extreme - tolerance : extreme + tolerance;
        int first = -1;
        int last = -1;
        for (int i = 0; i < points.length; i++) {
            boolean near = top ? points[i][1] >= limit : points[i][1] <= limit;
            if (near) {
                if (first < 0) {
                    first = i;
                }
                last = i;
            }
        }
        if (first < 0) {
            return 0;
        }
        return (first + last) / 2;
    }

    private static Result apexMatch(float[][] points, int apex, boolean topApex, float width, float height, float diagonal, float absoluteTurn, boolean narrow, GestureShape shape) {
        if (apex < 2 || apex > points.length - 3) {
            return null;
        }
        float startX = points[0][0];
        float startY = points[0][1];
        float endX = points[points.length - 1][0];
        float endY = points[points.length - 1][1];
        float apexX = points[apex][0];
        float apexY = points[apex][1];
        float baseY = (startY + endY) * 0.5f;
        float depth = topApex ? apexY - baseY : baseY - apexY;
        if (depth < height * 0.38f || Math.abs(startY - endY) > height * 0.62f) {
            return null;
        }
        if (!narrow && Math.abs(apexX - (startX + endX) * 0.5f) > width * 0.50f) {
            return null;
        }
        float firstArm = (float) Math.sqrt((apexX - startX) * (apexX - startX) + (apexY - startY) * (apexY - startY));
        float secondArm = (float) Math.sqrt((endX - apexX) * (endX - apexX) + (endY - apexY) * (endY - apexY));
        float longer = Math.max(firstArm, secondArm);
        if (longer < 1.0e-5f || Math.min(firstArm, secondArm) / longer < 0.35f) {
            return null;
        }
        if (!narrow) {
            float leftSpan = apexX - startX;
            float rightSpan = endX - apexX;
            if (Math.abs(leftSpan) < width * 0.22f || Math.abs(rightSpan) < width * 0.22f) {
                return null;
            }
            if (leftSpan * rightSpan <= 0.0f) {
                return null;
            }
        }
        float depthScore = clamp01(depth / Math.max(height * 0.7f, 1.0e-5f));
        float symmetry = Math.min(firstArm, secondArm) / longer;
        float turnScore = clamp01((absoluteTurn - 40.0f) / 140.0f);
        float centerScore = clamp01(1.0f - Math.abs(apexX - (startX + endX) * 0.5f) / Math.max(width * 0.50f, 1.0e-5f));
        float confidence = clamp01(depthScore * 0.30f + symmetry * 0.30f + turnScore * 0.25f + centerScore * 0.15f);
        return confidence >= 0.35f ? new Result(shape, confidence) : null;
    }

    private static boolean isStraight(float ratio, float deviation, float peakTurn) {
        return ratio >= LINE_STRAIGHTNESS
                && deviation <= LINE_DEVIATION
                && peakTurn <= LINE_MAX_TURN;
    }

    private static GestureShape straightShape(float angle) {
        if (angle >= -CARDINAL_TOLERANCE && angle <= CARDINAL_TOLERANCE) {
            return GestureShape.LINE_H;
        }
        if (Math.abs(angle) >= 180.0f - CARDINAL_TOLERANCE) {
            return GestureShape.LINE_H;
        }
        if (angle >= 90.0f - CARDINAL_TOLERANCE && angle <= 90.0f + CARDINAL_TOLERANCE) {
            return GestureShape.LINE_V_UP;
        }
        if (angle >= -90.0f - CARDINAL_TOLERANCE && angle <= -90.0f + CARDINAL_TOLERANCE) {
            return GestureShape.LINE_V_DOWN;
        }
        return GestureShape.UNKNOWN;
    }

    private static float straightConfidence(float ratio, float deviation, float absoluteTurn) {
        float straightness = clamp01((ratio - 0.72f) / 0.24f);
        float flatness = clamp01(1.0f - deviation / 0.22f);
        float steadiness = clamp01(1.0f - absoluteTurn / 90.0f);
        return clamp01(straightness * 0.50f + flatness * 0.25f + steadiness * 0.25f);
    }

    private static float circleConfidence(float turning, float closure, float consistency, float lengthRatio) {
        float turnScore = clamp01((turning - 200.0f) / 160.0f);
        float closureScore = clamp01(1.0f - closure / CIRCLE_CLOSURE);
        float consistencyScore = clamp01((consistency - 0.40f) / 0.50f);
        float lengthScore = clamp01((lengthRatio - 1.2f) / 1.0f);
        return clamp01(turnScore * 0.35f + closureScore * 0.25f + consistencyScore * 0.25f + lengthScore * 0.15f);
    }

    private static float zigzagConfidence(int reversals, float amplitude, float peakTurn, float consistency) {
        float reversalScore = clamp01(0.52f + 0.14f * Math.max(0, reversals - 2));
        float amplitudeScore = clamp01(amplitude / 0.36f);
        float cornerScore = clamp01((peakTurn - 25.0f) / 95.0f);
        float alternationScore = clamp01((0.9f - consistency) / 0.5f);
        return clamp01(reversalScore * 0.38f + amplitudeScore * 0.24f + cornerScore * 0.24f + alternationScore * 0.14f);
    }

    private static float templateScore(float[][] stroke, float[][] template) {
        float[][] normalized = normalizeSquare(stroke);
        float forward = meanDistance(normalized, template);
        float backward = meanDistance(reverse(normalized), template);
        float distance = Math.min(forward, backward);
        return clamp01(1.0f - distance / TEMPLATE_SPREAD);
    }

    private static float principalAngle(float[][] points) {
        float centerX = 0.0f;
        float centerY = 0.0f;
        for (float[] point : points) {
            centerX += point[0];
            centerY += point[1];
        }
        centerX /= points.length;
        centerY /= points.length;
        float xx = 0.0f;
        float yy = 0.0f;
        float xy = 0.0f;
        for (float[] point : points) {
            float dx = point[0] - centerX;
            float dy = point[1] - centerY;
            xx += dx * dx;
            yy += dy * dy;
            xy += dx * dy;
        }
        float angle = 0.5f * (float) Math.toDegrees(Math.atan2(2.0f * xy, xx - yy));
        float dx = points[points.length - 1][0] - points[0][0];
        float dy = points[points.length - 1][1] - points[0][1];
        double radians = Math.toRadians(angle);
        if (Math.cos(radians) * dx + Math.sin(radians) * dy < 0.0f) {
            angle += angle > 0.0f ? -180.0f : 180.0f;
        }
        while (angle > 180.0f) {
            angle -= 360.0f;
        }
        while (angle <= -180.0f) {
            angle += 360.0f;
        }
        return angle;
    }

    private static float radiusVariation(float[][] points) {
        float minX = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        for (float[] point : points) {
            minX = Math.min(minX, point[0]);
            maxX = Math.max(maxX, point[0]);
            minY = Math.min(minY, point[1]);
            maxY = Math.max(maxY, point[1]);
        }
        float centerX = (minX + maxX) * 0.5f;
        float centerY = (minY + maxY) * 0.5f;
        float mean = 0.0f;
        float[] radii = new float[points.length];
        for (int i = 0; i < points.length; i++) {
            float dx = points[i][0] - centerX;
            float dy = points[i][1] - centerY;
            radii[i] = (float) Math.sqrt(dx * dx + dy * dy);
            mean += radii[i];
        }
        mean /= points.length;
        if (mean < 1.0e-5f) {
            return 1.0f;
        }
        float variance = 0.0f;
        for (float radius : radii) {
            float delta = radius - mean;
            variance += delta * delta;
        }
        return (float) Math.sqrt(variance / points.length) / mean;
    }

    private static int reversals(float[][] points, int axis, float threshold) {
        int direction = 0;
        float against = 0.0f;
        int count = 0;
        for (int i = 1; i < points.length; i++) {
            float delta = points[i][axis] - points[i - 1][axis];
            if (Math.abs(delta) < 1.0e-5f) {
                continue;
            }
            int sign = delta > 0.0f ? 1 : -1;
            if (direction == 0) {
                direction = sign;
                continue;
            }
            if (sign == direction) {
                against = 0.0f;
                continue;
            }
            against += Math.abs(delta);
            if (against >= threshold) {
                count++;
                direction = sign;
                against = 0.0f;
            }
        }
        return count;
    }

    private static float turningDegrees(float[][] points) {
        float total = 0.0f;
        for (int i = 1; i < points.length - 1; i++) {
            float angle = turningAt(points, i);
            if (!Float.isNaN(angle)) {
                total += angle;
            }
        }
        return total;
    }

    private static float turningAbsolute(float[][] points) {
        float total = 0.0f;
        for (int i = 1; i < points.length - 1; i++) {
            float angle = turningAt(points, i);
            if (!Float.isNaN(angle)) {
                total += Math.abs(angle);
            }
        }
        return total;
    }

    private static float maxTurnDegrees(float[][] points) {
        float max = 0.0f;
        for (int i = 1; i < points.length - 1; i++) {
            float angle = turningAt(points, i);
            if (!Float.isNaN(angle)) {
                max = Math.max(max, Math.abs(angle));
            }
        }
        return max;
    }

    private static float turningAt(float[][] points, int i) {
        float ax = points[i][0] - points[i - 1][0];
        float ay = points[i][1] - points[i - 1][1];
        float bx = points[i + 1][0] - points[i][0];
        float by = points[i + 1][1] - points[i][1];
        if (Math.abs(ax) + Math.abs(ay) < 1.0e-6f || Math.abs(bx) + Math.abs(by) < 1.0e-6f) {
            return Float.NaN;
        }
        return (float) Math.toDegrees(Math.atan2(ax * by - ay * bx, ax * bx + ay * by));
    }

    private static float maxChordDeviation(float[][] points, float chord) {
        float[] first = points[0];
        float[] last = points[points.length - 1];
        float dx = last[0] - first[0];
        float dy = last[1] - first[1];
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 1.0e-6f) {
            return chord;
        }
        float max = 0.0f;
        for (float[] point : points) {
            float px = point[0] - first[0];
            float py = point[1] - first[1];
            float cross = Math.abs(px * dy - py * dx) / len;
            max = Math.max(max, cross);
        }
        return max;
    }

    private static float[][] smooth(float[][] points) {
        float[][] out = new float[points.length][2];
        for (int i = 0; i < points.length; i++) {
            int from = Math.max(0, i - 1);
            int to = Math.min(points.length - 1, i + 1);
            float weight = 0.0f;
            for (int j = from; j <= to; j++) {
                float w = j == i ? 2.0f : 1.0f;
                out[i][0] += points[j][0] * w;
                out[i][1] += points[j][1] * w;
                weight += w;
            }
            out[i][0] /= weight;
            out[i][1] /= weight;
        }
        return out;
    }

    private static float[][] resample(List<float[]> points, int count) {
        int size = points.size();
        float[][] out = new float[count][2];
        if (size == 1) {
            for (int i = 0; i < count; i++) {
                out[i][0] = points.get(0)[0];
                out[i][1] = points.get(0)[1];
            }
            return out;
        }
        float[] cumulative = new float[size];
        for (int i = 1; i < size; i++) {
            cumulative[i] = cumulative[i - 1] + distance(points.get(i - 1), points.get(i));
        }
        float total = cumulative[size - 1];
        if (total < 1.0e-6f) {
            for (int i = 0; i < count; i++) {
                out[i][0] = points.get(0)[0];
                out[i][1] = points.get(0)[1];
            }
            return out;
        }
        float step = total / (count - 1);
        int index = 0;
        for (int i = 0; i < count; i++) {
            float target = Math.min(step * i, total);
            while (index < size - 2 && cumulative[index + 1] < target) {
                index++;
            }
            float segment = cumulative[index + 1] - cumulative[index];
            float t = segment > 1.0e-6f ? (target - cumulative[index]) / segment : 0.0f;
            t = Math.max(0.0f, Math.min(1.0f, t));
            float ax = points.get(index)[0];
            float ay = points.get(index)[1];
            float bx = points.get(index + 1)[0];
            float by = points.get(index + 1)[1];
            out[i][0] = ax + (bx - ax) * t;
            out[i][1] = ay + (by - ay) * t;
        }
        return out;
    }

    private static float polylineLength(List<float[]> points) {
        float total = 0.0f;
        for (int i = 1; i < points.size(); i++) {
            total += distance(points.get(i - 1), points.get(i));
        }
        return total;
    }

    private static float[][] normalizeSquare(float[][] points) {
        float minX = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        for (float[] point : points) {
            minX = Math.min(minX, point[0]);
            maxX = Math.max(maxX, point[0]);
            minY = Math.min(minY, point[1]);
            maxY = Math.max(maxY, point[1]);
        }
        float centerX = (minX + maxX) * 0.5f;
        float centerY = (minY + maxY) * 0.5f;
        float spanX = Math.max(maxX - minX, 1.0e-5f);
        float spanY = Math.max(maxY - minY, 1.0e-5f);
        float[][] out = new float[points.length][2];
        for (int i = 0; i < points.length; i++) {
            out[i][0] = (points[i][0] - centerX) / spanX;
            out[i][1] = (points[i][1] - centerY) / spanY;
        }
        return out;
    }

    private static float[][] reverse(float[][] points) {
        float[][] out = new float[points.length][2];
        for (int i = 0; i < points.length; i++) {
            out[i][0] = points[points.length - 1 - i][0];
            out[i][1] = points[points.length - 1 - i][1];
        }
        return out;
    }

    private static float meanDistance(float[][] a, float[][] b) {
        float total = 0.0f;
        int count = Math.min(a.length, b.length);
        for (int i = 0; i < count; i++) {
            float dx = a[i][0] - b[i][0];
            float dy = a[i][1] - b[i][1];
            total += (float) Math.sqrt(dx * dx + dy * dy);
        }
        return total / count;
    }

    private static float extent(float[][] points, int axis) {
        float min = Float.MAX_VALUE;
        float max = -Float.MAX_VALUE;
        for (float[] point : points) {
            min = Math.min(min, point[axis]);
            max = Math.max(max, point[axis]);
        }
        return max - min;
    }

    private static float distance(float[] a, float[] b) {
        float dx = a[0] - b[0];
        float dy = a[1] - b[1];
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private static float clamp01(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private static float[][] buildCircle() {
        float[][] points = new float[SAMPLES][2];
        for (int i = 0; i < SAMPLES; i++) {
            double angle = Math.PI * 2.0 * i / (SAMPLES - 1);
            points[i][0] = (float) Math.cos(angle) * 0.5f;
            points[i][1] = (float) Math.sin(angle) * 0.5f;
        }
        return normalizeSquare(points);
    }

    private static float[][] buildPolyline(float[][] corners) {
        List<float[]> points = new ArrayList<>();
        for (int i = 0; i < corners.length - 1; i++) {
            float ax = corners[i][0];
            float ay = corners[i][1];
            float bx = corners[i + 1][0];
            float by = corners[i + 1][1];
            int steps = 8;
            for (int step = 0; step < steps; step++) {
                float t = (float) step / steps;
                points.add(new float[]{ax + (bx - ax) * t, ay + (by - ay) * t});
            }
        }
        points.add(new float[]{corners[corners.length - 1][0], corners[corners.length - 1][1]});
        return normalizeSquare(resample(points, SAMPLES));
    }
}
