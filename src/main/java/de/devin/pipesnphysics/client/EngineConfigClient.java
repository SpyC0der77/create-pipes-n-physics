package de.devin.pipesnphysics.client;

/** Client-side mirror of the server's {@code enableEngine} config, synced on login. */
public final class EngineConfigClient {
    private static boolean enabled = true;

    private EngineConfigClient() {}

    public static boolean isEnabled() { return enabled; }

    public static void receive(boolean engineEnabled) {
        enabled = engineEnabled;
    }
}
