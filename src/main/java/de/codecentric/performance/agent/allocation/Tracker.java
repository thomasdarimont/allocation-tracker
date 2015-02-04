package de.codecentric.performance.agent.allocation;

/**
 */
public interface Tracker {

    /**
     * Clears recorded data and starts recording.
     */
    void start();

    /**
     * Stops recording.
     */
    void stop();

    /**
     * Builds a human readable list of class names and instantiation counts.
     * <p/>
     * Note: this method will create garbage while building and sorting the top list. The amount of garbage created is
     * dictated by the amount of classes tracked, not by the amount requested.
     *
     * @param amount controls how many results are included in the top list. If <= 0 will default to DEFAULT_AMOUNT.
     * @return a newline separated String containing class names and invocation counts.
     */
    String buildTopList(int amount);
}
