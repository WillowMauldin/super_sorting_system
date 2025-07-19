package me.mauldin.super_sorting_system;

import me.mauldin.super_sorting_system.bot.Bot;

public class McAgent {
    public final Config config;
    public final Bot bot;
    public final Operator operator;

    public McAgent() throws Exception {
        this.config = new Config();
        this.operator = new Operator(this.config.getEndpoint(), this.config.getApiKey());
        this.bot = new Bot(this.config);
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Starting agent2");
        McAgent mcAgent = new McAgent();

	while (mcAgent.bot.getIsConnected()) {
		Thread.sleep(5);
	}
    }
}
