package com.putzwirk.gesturalcombat.technique;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import com.putzwirk.gesturalcombat.Gesturalcombat;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class GestureScheduler {
    private static final List<Task> TASKS = new ArrayList<>();

    private GestureScheduler() {
    }

    public static void schedule(Level level, int delayTicks, Runnable action) {
        TASKS.add(new Task(level, level.getGameTime() + Math.max(1, delayTicks), action));
    }

    public static void tick(MinecraftServer server) {
        if (TASKS.isEmpty()) {
            return;
        }
        List<Task> due = new ArrayList<>();
        Iterator<Task> iterator = TASKS.iterator();
        while (iterator.hasNext()) {
            Task task = iterator.next();
            Level level = task.level;
            if (level == null || level.getServer() != server) {
                iterator.remove();
                continue;
            }
            if (level.getGameTime() >= task.dueTick) {
                iterator.remove();
                due.add(task);
            }
        }
        for (Task task : due) {
            task.action.run();
        }
    }

    public static void stopped() {
        TASKS.clear();
    }

    private record Task(Level level, long dueTick, Runnable action) {
    }
}
