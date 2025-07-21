package me.mauldin.super_sorting_system;

import me.mauldin.super_sorting_system.Operator.Agent;
import me.mauldin.super_sorting_system.bot.Bot;

public class McAgent {
  public final Config config;
  public final Bot bot;
  public final Operator operator;
  public final Agent agent;

  public McAgent() throws Exception {
    this.config = new Config();
    this.operator = new Operator(this.config.getEndpoint(), this.config.getApiKey());
    this.agent = this.operator.registerAgent();
    this.bot = new Bot(this.config, this.operator, this.agent);
  }

  public static void main(String[] args) throws Exception {
    System.out.println("Starting agent2");
    McAgent mcAgent = new McAgent();
  }
}
