package me.ez.cobbleworks;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Server-authoritative settings, synced by NeoForge. */
public final class Config {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.IntValue CYCLE_TICKS;
    public static final ModConfigSpec.IntValue OUTPUT_PER_CYCLE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.translation("cobbleworks.configuration.generator").push("generator");
        ENABLED = b.comment("Enable cobblestone production (inventory access stays available)")
                .translation("cobbleworks.configuration.enabled").define("enabled", true);
        CYCLE_TICKS = b.comment("Ticks per production cycle; 20 ticks = one second")
                .translation("cobbleworks.configuration.cycle_ticks").defineInRange("cycle_ticks", 40, 1, 1200);
        OUTPUT_PER_CYCLE = b.comment("Cobblestone produced per completed cycle")
                .translation("cobbleworks.configuration.output_per_cycle").defineInRange("output_per_cycle", 1, 1, 64);
        b.pop();
        SPEC = b.build();
    }

    private Config() {}
}
