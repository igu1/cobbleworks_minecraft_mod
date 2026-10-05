package me.ez.cobbleworks.common;

/** Pure production/comparator rules, shared with regression tests. */
public final class GeneratorRules {
    public static final int OUTPUT_SLOTS = 9;
    public static final int CAPACITY = OUTPUT_SLOTS * 64;

    private GeneratorRules() {}

    /** From a redstone standpoint, the machine only runs while it receives a signal. */
    public static boolean canProduce(boolean enabled, boolean powered) { return enabled && powered; }
    public static int comparator(int stored) {
        return stored <= 0 ? 0 : 1 + (int) (14L * Math.min(CAPACITY, stored) / CAPACITY);
    }
    public static int boundedProgress(int saved, int duration) {
        return Math.clamp(saved, 0, Math.max(0, duration - 1));
    }
}
