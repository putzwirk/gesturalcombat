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
        for (int i = 0; i < animations.length; i++) {
            TASKS.add(new Task(ticks + Math.max(0, startDelay) + i * step, playerId, animations[i], hand, lengths[i], upswings[i]));
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
