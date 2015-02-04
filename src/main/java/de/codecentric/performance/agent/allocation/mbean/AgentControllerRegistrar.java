package de.codecentric.performance.agent.allocation.mbean;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static de.codecentric.performance.agent.allocation.AgentLogger.logInfo;

/**
 * @author Thomas Darimont
 */
public class AgentControllerRegistrar {

    private static final AgentControllerRegistrar INSTANCE = new AgentControllerRegistrar();
    private static final ObjectName AGENT_CONTROLLER_OBJECT_NAME;

    static {
        try {
            AGENT_CONTROLLER_OBJECT_NAME = new ObjectName("de.codecentric:type=AgentController");
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException(e);
        }
    }

    private final Runnable AGENT_CONTROLLER_REGISTRATION = new Runnable() {
        @Override
        public void run() {

            try {
                // retry up to a maximum of 10 minutes
                MBeanServer mbs = tryGetMbeanServer();
                if (mbs == null) return;

                mbs.registerMBean(new AgentController(), AGENT_CONTROLLER_OBJECT_NAME);
                logInfo("Registered Agent Controller MBean.");
            } catch (Exception e) {
                logInfo("Could not register Agent Controller MBean. Exception:");
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                logInfo(sw.toString());
            }
        }
    };

    private AgentControllerRegistrar() {
    }

    public static void tryRegisterMBean() {

        AgentControllerRegistrar registrar = INSTANCE;

        try {

            MBeanServer mbs = registrar.tryGetMbeanServer();
            try {

                mbs.getMBeanInfo(AGENT_CONTROLLER_OBJECT_NAME);
                //agent already installed, do nothing
            } catch (InstanceNotFoundException infe) {
                registrar.registerMBean();
            }
        } catch (Exception ie) {
            ie.printStackTrace();
        }
    }

    private void registerMBean() {
        Executors.newSingleThreadExecutor().execute(AGENT_CONTROLLER_REGISTRATION);
    }

    private MBeanServer tryGetMbeanServer() throws InterruptedException {

        int retryLimit = 60;
        MBeanServer mbs = null;
        while (mbs == null) {
            if (retryLimit-- == 0) {
                logInfo("Could not register Agent Controller MBean in 10 minutes.");
                return null;
            }
            TimeUnit.SECONDS.sleep(10);
            mbs = ManagementFactory.getPlatformMBeanServer();
        }

        return mbs;
    }
}
