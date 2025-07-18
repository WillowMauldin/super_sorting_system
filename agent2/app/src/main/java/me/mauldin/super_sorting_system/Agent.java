package me.mauldin.super_sorting_system;

public class Agent {
    public String getGreeting() {
        return "Hello World!";
    }

    public static void main(String[] args) {
        System.out.println(new Agent().getGreeting());
    }
}
