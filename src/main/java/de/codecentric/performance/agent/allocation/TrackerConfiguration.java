package de.codecentric.performance.agent.allocation;

/**
 * @author Thomas Darimont
 */
public class TrackerConfiguration {

    private String prefixSlashed;

    private String prefixDotted;

    public static TrackerConfiguration parseTrackerConfiguration(String configString) {

        String prefix = configString;
        if (prefix == null || prefix.length() == 0) {
            AgentLogger.logInfo("Agent failed to start: Please provide a package prefixSlashed to filter.");
            throw new RuntimeException("invalid configuration");
        }
        // accepts both . and / notation, but will convert dots to slashes
        prefix = prefix.replace(".", "/");
        if (!prefix.contains("/")) {
            AgentLogger.logInfo("Agent failed to start: Please provide at least one package level prefixSlashed to filter.");
            throw new RuntimeException("invalid configuration");
        }

        if (prefix.contains("java/")) {
            AgentLogger.logInfo("You are trying to instrument JVM core classes - this might crash the JVM");
        }

        TrackerConfiguration config = new TrackerConfiguration();
        config.prefixSlashed = prefix;
        config.prefixDotted = configString;

        return config;
    }

    public String getPrefixSlashed() {
        return prefixSlashed;
    }

    public String getPrefixDotted() {
        return prefixDotted;
    }
}
