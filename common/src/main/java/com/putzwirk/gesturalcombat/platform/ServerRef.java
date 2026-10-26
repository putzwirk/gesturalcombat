package com.putzwirk.gesturalcombat.platform;

import net.minecraft.server.MinecraftServer;

public final class ServerRef {
    private static MinecraftServer server;

    private ServerRef() {
    }

    public static void set(MinecraftServer value) {
        server = value;
    }

    public static MinecraftServer get() {
        return server;
    }
}
