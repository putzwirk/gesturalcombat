package com.putzwirk.gesturalcombat.client.anim;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class AnimationScheduler {
    private static final List<Task> TASKS = new ArrayList<>();
    private static int ticks;

    private AnimationScheduler() {
    }

    public static void cancel(int playerId) {
        TASKS.removeIf(task -> task.playerId == playerId);
    }

    public static void clear() {
        TASKS.clear();
    }

    public static void playSequence(int playerId, String[] animations, String hand, float[] lengths, float[] upswings, int startDelay, int intervalTicks) {
        cancel(playerId);
        if (animations == null || animations.length == 0) {
            return;
        }
        int step = Math.max(0, intervalTicks);
        int[] delays = new int[animations.length];
        for (int i = 0; i < animations.length; i++) {
            delays[i] = i * step;
        }
        playSequence(playerId, animations, hand, lengths, upswings, startDelay, delays);
    }

    public static void playSequence(int playerId, String[] animations, String hand, float[] lengths, float[] upswings, int startDelay, int[] delays) {
        cancel(playerId);
        if (animations == null || animations.length == 0) {
            return;
        }
        for (int i = 0; i < animations.length; i++) {
            int due = Math.max(0, startDelay) + (delays != null && i < delays.length ? Math.max(0, delays[i]) : 0);
            TASKS.add(new Task(ticks + due, playerId, animations[i], hand, lengths[i], upswings[i]));
        }
    }

    public static void tick() {
        ticks++;
        if (TASKS.isEmpty()) {
            return;
        }
        Iterator<Task> iterator = TASKS.iterator();
        while (iterator.hasNext()) {
            Task task = iterator.next();
            if (ticks >= task.dueTick) {
                iterator.remove();
                Animations.playFor(task.playerId, task.animation, task.hand, task.length, task.upswing);
            }
        }
    }

    private record Task(int dueTick, int playerId, String animation, String hand, float length, float upswing) {
    }
}
