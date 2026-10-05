package me.ez.cobbleworks.common;

/** No external test dependencies; Gradle check executes this class on Java 25. */
public final class GeneratorRulesTest {
    private static int checks;
    public static void main(String[] args) {
        require(!GeneratorRules.canProduce(true, false), "no signal never produces");
        require(!GeneratorRules.canProduce(false, true), "manual pause overrides signal");
        require(!GeneratorRules.canProduce(false, false), "off and unpowered");
        require(GeneratorRules.canProduce(true, true), "enabled and powered");
        require(!GeneratorRules.canProduce(true, false), "signal is always required");
        require(GeneratorRules.comparator(0) == 0, "empty buffer");
        require(GeneratorRules.comparator(1) == 1, "first cobble");
        require(GeneratorRules.comparator(576) == 15, "full buffer");
        require(GeneratorRules.comparator(Integer.MAX_VALUE) == 15, "clamped count");
        require(GeneratorRules.comparator(-1) == 0, "negative count");
        int last = 0;
        for (int count = 0; count <= GeneratorRules.CAPACITY; count++) {
            int signal = GeneratorRules.comparator(count);
            require(signal >= last && signal <= 15, "monotonic comparator");
            last = signal;
        }
        for (int duration : new int[]{1, 2, 40, 1200}) {
            require(GeneratorRules.boundedProgress(-20, duration) == 0, "negative save");
            require(GeneratorRules.boundedProgress(Integer.MAX_VALUE, duration) == duration - 1, "progress cap");
        }
        System.out.println("Passed " + checks + " generator policy checks");
    }
    private static void require(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
}
