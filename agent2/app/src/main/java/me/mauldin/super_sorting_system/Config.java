package me.mauldin.super_sorting_system;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.json.JSONObject;

public class Config {
    private final String apiKey;
    private final String endpoint;
    private final String mcServerHost;
    private final int mcServerPort;

    public Config() throws IOException {
        this("config.json");
    }

    public Config(String configPath) throws IOException {
        String content = Files.readString(Paths.get(configPath));
        JSONObject json = new JSONObject(content);

        this.apiKey = json.getString("api_key");
        this.endpoint = json.getString("endpoint");
        this.mcServerHost = json.getString("mc_server_host");
        this.mcServerPort = json.getInt("mc_server_port");
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getMcServerHost() {
        return mcServerHost;
    }

    public int getMcServerPort() {
        return mcServerPort;
    }
}
