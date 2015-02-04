package de.codecentric.performance.agent.allocation.mbean;

import de.codecentric.performance.agent.allocation.AgentLogger;
import de.codecentric.performance.agent.allocation.Tracker;
import de.codecentric.performance.agent.allocation.TrackerSettings;

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;

import static de.codecentric.performance.agent.allocation.AgentLogger.logInfo;

public class AgentController implements AgentControllerMBean {

    private static final Tracker TRACKER;

    static {

        TRACKER = AccessController.<Tracker>doPrivileged(new PrivilegedAction<Tracker>() {
            @Override
            public Tracker run() {

                try {
                    ClassLoader cl = AgentController.class.getClassLoader();
                    Class<?> trackerClass = cl.loadClass(TrackerSettings.TRACKER_CLASS.toString());

                    Field[] fields = trackerClass.getDeclaredFields();

                    Field trackerField = null;
                    for (Field field : fields) {
                        if (field.getType().isAssignableFrom(Tracker.class)) {
                            trackerField = field;
                            break;
                        }
                    }

                    if (trackerField == null) {
                        throw new RuntimeException("Could not extract Tracker from: " + trackerClass);
                    }

                    trackerField.setAccessible(true);

                    return (Tracker) trackerField.get(null);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException("Could not load Tracker class: " + TrackerSettings.TRACKER_CLASS.toString());
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Could not access Tracker field: " + e.getMessage());
                }
            }
        });
    }

    @Override
    public void start() {
        logInfo("Agent is now tracking.");
        TRACKER.start();
    }

    @Override
    public void stop() {
        logInfo("Agent is no longer tracking.");
        TRACKER.stop();
    }

    @Override
    public String printTop(int amount) {

        String topList = TRACKER.buildTopList(amount);

        if (AgentLogger.LOG_TOP_LIST) {
            logInfo("Agent saw these allocations:");
            logInfo(topList);
        }

        return topList;
    }

}
