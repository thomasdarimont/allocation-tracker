package de.codecentric.performance.agent.allocation;

import de.codecentric.performance.agent.allocation.mbean.AgentControllerRegistrar;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.ArrayList;
import java.util.List;

import static de.codecentric.performance.agent.allocation.AgentLogger.logInfo;

/**
 * Class registered as premain hook, will add a ClassFileTransformer and register an MBean for controlling the agent.
 */
public abstract class AllocationProfilingAgent {

    public static void premain(String agentArgs, Instrumentation inst) {
        new BootstrapAllocationProfilingAgent().install(agentArgs, inst);
    }

    /**
     * Class registered as agentmain hook, will add a ClassFileTransformer and register an MBean for controlling the agent.
     */
    @SuppressWarnings("unused")
    public static void agentmain(String agentArgs, Instrumentation inst) throws Exception {
        new AdhocAllocationProfilingAgent().install(agentArgs, inst);
    }

    abstract void install(String agentArgs, Instrumentation inst);

    static class BootstrapAllocationProfilingAgent extends AllocationProfilingAgent {

        public void install(String agentArgs, Instrumentation inst) {

            logInfo("Trying to install agent...");

            TrackerConfiguration config = TrackerConfiguration.parseTrackerConfiguration(agentArgs);
            AgentControllerRegistrar.tryRegisterMBean();
            inst.addTransformer(new AllocationTrackerClassFileTransformer(config), true);

            logInfo("Installed agent successfully.");
        }
    }

    static class AdhocAllocationProfilingAgent extends BootstrapAllocationProfilingAgent {

        public void install(String agentArgs, Instrumentation inst) {

            super.install(agentArgs, inst);
            tryReinstrumentClasses(agentArgs, inst);
        }


        private static void tryReinstrumentClasses(String agentArgs, Instrumentation inst) {

            if (!inst.isRetransformClassesSupported()) {
                return;
            }

            logInfo("Trying to redefine existing classes...");

            List<Class> classes = new ArrayList<Class>(128);
            Class[] candidates = inst.getAllLoadedClasses();
            for (int i = 0; i < candidates.length; i++) {
                if (candidates[i].getName().startsWith(agentArgs)) {
                    classes.add(candidates[i]);
                }
            }

            try {
                inst.retransformClasses(classes.toArray(new Class[classes.size()]));
            } catch (UnmodifiableClassException e) {
                logInfo("Could not redefine classes: " + e);
                throw new RuntimeException(e);
            }

            logInfo("Redefine completed.");
        }
    }
}
