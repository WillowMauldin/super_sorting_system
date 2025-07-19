package me.mauldin.super_sorting_system;

import net.raphimc.minecraftauth.step.java.session.StepFullJavaSession.FullJavaSession;

public class Agent {
    private final Config config;
    private final FullJavaSession javaSession;

    public Agent() throws Exception {
        this.config = new Config();
	this.javaSession = McAuth.getSession();
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Starting agent2");
        Agent agent = new Agent();
    }
}
