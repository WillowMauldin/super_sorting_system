package me.mauldin.super_sorting_system.bot;

import java.net.InetSocketAddress;
import me.mauldin.super_sorting_system.Config;
import net.raphimc.minecraftauth.step.java.StepMCProfile.MCProfile;
import net.raphimc.minecraftauth.step.java.StepMCToken.MCToken;
import net.raphimc.minecraftauth.step.java.session.StepFullJavaSession.FullJavaSession;
import org.geysermc.mcprotocollib.auth.GameProfile;
import org.geysermc.mcprotocollib.auth.SessionService;
import org.geysermc.mcprotocollib.network.ClientSession;
import org.geysermc.mcprotocollib.network.event.session.ConnectedEvent;
import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent;
import org.geysermc.mcprotocollib.network.event.session.SessionAdapter;
import org.geysermc.mcprotocollib.network.factory.ClientNetworkSessionFactory;
import org.geysermc.mcprotocollib.protocol.MinecraftConstants;
import org.geysermc.mcprotocollib.protocol.MinecraftProtocol;
import me.mauldin.super_sorting_system.Operator;
import me.mauldin.super_sorting_system.Operator.Agent;

public class Bot {
    private boolean isConnected;
    private Navigation navigation;
    private Operator operator;
    private Agent agent;

    public Bot(Config config, Operator operator, Agent agent) throws Exception {
	this.operator = operator;
	this.agent = agent;
	this.navigation = new Navigation();

        FullJavaSession javaSession = McAuth.getSession();
        MCProfile mcProfile = javaSession.getMcProfile();
        MCToken mcToken = mcProfile.getMcToken();

        SessionService sessionService = new SessionService();
        MinecraftProtocol protocol = new MinecraftProtocol(
                new GameProfile(mcProfile.getId(), mcProfile.getName()), mcToken.getAccessToken());

        ClientSession client = ClientNetworkSessionFactory.factory()
                .setRemoteSocketAddress(new InetSocketAddress(config.getMcServerHost(), config.getMcServerPort()))
                .setProtocol(protocol)
                .create();
        client.setFlag(MinecraftConstants.SESSION_SERVICE_KEY, sessionService);

        client.addListener(new ConnectionListeners());
        client.addListener(navigation);
        client.addListener(new SignInfoListener(navigation, this.operator, this.agent));
        client.addListener(new SessionAdapter() {
            @Override
            public void connected(ConnectedEvent event) {
                System.out.println("Connected");
            }

            @Override
            public void disconnected(DisconnectedEvent event) {
                System.out.println("Disconnected: " + event.getReason() + " (" + event.getCause() + ")");
                isConnected = false;
            }
        });

        client.connect();
	this.isConnected = true;
    }

    public boolean getIsConnected() {
	return this.isConnected;
    }
}
