package com.putzwirk.gesturalcombat.platform;

public final class Platform {
    private static IPlatform platform;
    private static INetworkTransport network;

    private Platform() {
    }

    public static void install(IPlatform platform, INetworkTransport network) {
        Platform.platform = platform;
        Platform.network = network;
    }

    public static IPlatform get() {
        if (platform == null) {
            throw new IllegalStateException("gesturalcombat platform not installed");
        }
        return platform;
    }

    public static INetworkTransport net() {
        if (network == null) {
            throw new IllegalStateException("gesturalcombat network not installed");
        }
        return network;
    }
}
