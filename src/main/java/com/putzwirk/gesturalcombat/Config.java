package com.putzwirk.gesturalcombat;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public final class Config {
    public static final ForgeConfigSpec SPEC;
    public static final Config INSTANCE;

    public final ForgeConfigSpec.BooleanValue gesturesEnabled;
    public final ForgeConfigSpec.DoubleValue damageMultiplier;
    public final ForgeConfigSpec.DoubleValue cooldownMultiplier;
    public final ForgeConfigSpec.DoubleValue xpMultiplier;
    public final ForgeConfigSpec.BooleanValue debugHitboxes;
    public final ForgeConfigSpec.BooleanValue weaponTrails;
    public final ForgeConfigSpec.BooleanValue debugCasts;

    static {
        Pair<Config, ForgeConfigSpec> pair = new ForgeConfigSpec.Builder().configure(Config::new);
        INSTANCE = pair.getLeft();
        SPEC = pair.getRight();
    }

    private Config(ForgeConfigSpec.Builder builder) {
        builder.push("gestures");
        gesturesEnabled = builder.define("enabled", true);
        damageMultiplier = builder.defineInRange("damageMultiplier", 1.0, 0.1, 10.0);
        cooldownMultiplier = builder.defineInRange("cooldownMultiplier", 1.0, 0.1, 10.0);
        xpMultiplier = builder.defineInRange("xpMultiplier", 1.0, 0.0, 20.0);
        debugHitboxes = builder.define("debugHitboxes", false);
        weaponTrails = builder.define("weaponTrails", true);
        debugCasts = builder.define("debugCasts", true);
        builder.pop();
    }
}
