package me.mauldin.super_sorting_system;

import me.mauldin.super_sorting_system.bot.Bot;
import me.mauldin.super_sorting_system.Operator.Agent;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class McAgent {
    public final Config config;
    public final Bot bot;
    public final Operator operator;
    public final Agent agent;
    private final ScheduledExecutorService heartbeatScheduler;

    public McAgent() throws Exception {
        this.config = new Config();
        this.operator = new Operator(this.config.getEndpoint(), this.config.getApiKey());
	this.agent = this.operator.registerAgent();
        this.bot = new Bot(this.config, this.operator, this.agent);
        this.heartbeatScheduler = Executors.newSingleThreadScheduledExecutor();
        this.heartbeatScheduler.scheduleAtFixedRate(() -> {
            try {
                this.operator.heartbeat(this.agent);
            } catch (Exception e) {
                System.out.println("Heartbeat failed: " + e);
            }
        }, 15, 15, TimeUnit.SECONDS);
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Starting agent2");
        McAgent mcAgent = new McAgent();

	while (mcAgent.bot.getIsConnected()) {
		Thread.sleep(5);
	}
    }
}
