package com.putzwirk.gesturalcombat.technique;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.putzwirk.gesturalcombat.Gesturalcombat;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Mod.EventBusSubscriber(modid = Gesturalcombat.MODID)
public final class GestureScheduler {
    private static final List<Task> TASKS = new ArrayList<>();

    private GestureScheduler() {
    }

    public static void schedule(Level level, int delayTicks, Runnable action) {
        TASKS.add(new Task(level, level.getGameTime() + Math.max(1, delayTicks), action));
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || TASKS.isEmpty()) {
            return;
        }
        MinecraftServer server = event.getServer();
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

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        TASKS.clear();
    }

    private record Task(Level level, long dueTick, Runnable action) {
    }
}
