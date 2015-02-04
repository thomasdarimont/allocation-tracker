package de.codecentric.performance.agent.allocation.mbean;

public interface AgentControllerMBean {

  void start();

  void stop();

  String printTop(int amount);

}
