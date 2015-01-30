package de.codecentric.performance.agent.allocation;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.management.*;

import de.codecentric.performance.agent.allocation.mbean.Agent;

/**
 * Class registered as premain hook, will add a ClassFileTransformer and register an MBean for controlling the agent.
 */
public class AllocationProfilingAgent {

    private static final ObjectName AGENT_OBJECT_NAME;

    static{
        try {
            AGENT_OBJECT_NAME = new ObjectName("de.codecentric:type=Agent");
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException(e);
        }
    }

  public static void premain(String agentArgs, Instrumentation inst) {
    String prefix = agentArgs;
    if (prefix == null || prefix.length() == 0) {
      AgentLogger.log("Agent failed to start: Please provide a package prefix to filter.");
      return;
    }
    // accepts both . and / notation, but will convert dots to slashes
    prefix = prefix.replace(".", "/");
    if (!prefix.contains("/")) {
      AgentLogger.log("Agent failed to start: Please provide at least one package level prefix to filter.");
      return;
    }
    registerMBean();
    inst.addTransformer(new AllocationTrackerClassFileTransformer(prefix), true);
  }

 /**
  * Class registered as agentmain hook, will add a ClassFileTransformer and register an MBean for controlling the agent.
  */
 @SuppressWarnings("unused")
  public static void agentmain(String args, Instrumentation inst) throws Exception {

      MBeanServer mbs = tryGetMbeanServer();

      if(mbs != null){
          try {
              mbs.getMBeanInfo(AGENT_OBJECT_NAME);
              //agent already installed, do nothing
          }catch(InstanceNotFoundException infe){
              //install new agent
              AgentLogger.log("Trying to attach agent dynamically...");
              premain(args, inst);
              AgentLogger.log("Attached agent successfully.");
              if(inst.isRetransformClassesSupported()){
                  AgentLogger.log("Trying to redefine existing classes...");

                  List<Class> classes = new ArrayList<>(128);
                  Class[] candidates = inst.getAllLoadedClasses();
                  for (int i = 0; i < candidates.length; i++) {
                      if(candidates[i].getName().startsWith(args)) {
                          classes.add(candidates[i]);
                      }
                  }

                  inst.retransformClasses(classes.toArray(new Class[classes.size()]));
                  AgentLogger.log("Redefine completed.");
              }
          }
      }
  }

  /*
   * Starts a new thread which will try to connect to the Platform Mbean Server.
   */
  private static void registerMBean() {
    Thread thread = new Thread() {
      @Override
      public void run() {
        try {
          // retry up to a maximum of 10 minutes
            MBeanServer mbs = tryGetMbeanServer();
            if (mbs == null) return;

            mbs.registerMBean(new Agent(), AGENT_OBJECT_NAME);
          AgentLogger.log("Registered Agent MBean.");
        } catch (Exception e) {
          AgentLogger.log("Could not register Agent MBean. Exception:");
          StringWriter sw = new StringWriter();
          e.printStackTrace(new PrintWriter(sw));
          AgentLogger.log(sw.toString());
        }
      }
    };
    thread.setDaemon(true);
    thread.start();
  }

    private static MBeanServer tryGetMbeanServer() throws InterruptedException {

        int retryLimit = 60;
        MBeanServer mbs = null;
        while (mbs == null) {
          if (retryLimit-- == 0) {
            AgentLogger.log("Could not register Agent MBean in 10 minutes.");
              return null;
          }
          TimeUnit.SECONDS.sleep(10);
          mbs = ManagementFactory.getPlatformMBeanServer();
        }

        return mbs;
    }
}
