package me.mauldin.super_sorting_system;

import net.raphimc.minecraftauth.step.java.session.StepFullJavaSession.FullJavaSession;
import me.mauldin.super_sorting_system.bot.Bot;

public class Agent {
    private final Config config;
    private final Bot bot;

    public Agent() throws Exception {
        this.config = new Config();
	this.bot = new Bot(this.config);
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Starting agent2");
        Agent agent = new Agent();
    }
}
