package me.mauldin.super_sorting_system.bot;

import java.net.InetSocketAddress;
import me.mauldin.super_sorting_system.Config;
import me.mauldin.super_sorting_system.Operator;
import me.mauldin.super_sorting_system.Operator.Agent;
import me.mauldin.super_sorting_system.Operator.Operation;
import me.mauldin.super_sorting_system.Operator.PollOperationResponse;
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

public class Bot {
  private boolean isConnected;
  private Navigation navigation;
  private Operator operator;
  private Agent agent;
  private Thread mainLoopThread;

  public Bot(Config config, Operator operator, Agent agent) throws Exception {
    this.operator = operator;
    this.agent = agent;

    FullJavaSession javaSession = McAuth.getSession();
    MCProfile mcProfile = javaSession.getMcProfile();
    MCToken mcToken = mcProfile.getMcToken();

    SessionService sessionService = new SessionService();
    MinecraftProtocol protocol =
        new MinecraftProtocol(
            new GameProfile(mcProfile.getId(), mcProfile.getName()), mcToken.getAccessToken());

    ClientSession client =
        ClientNetworkSessionFactory.factory()
            .setRemoteSocketAddress(
                new InetSocketAddress(config.getMcServerHost(), config.getMcServerPort()))
            .setProtocol(protocol)
            .create();
    client.setFlag(MinecraftConstants.SESSION_SERVICE_KEY, sessionService);

    this.navigation = new Navigation(client, this.operator, this.agent);

    client.addListener(new ConnectionListeners());
    client.addListener(navigation);
    client.addListener(new SignInfoListener(navigation, this.operator, this.agent));
    client.addListener(
        new SessionAdapter() {
          @Override
          public void connected(ConnectedEvent event) {
            System.out.println("Connected");
          }

          @Override
          public void disconnected(DisconnectedEvent event) {
            System.out.println(
                "Disconnected: " + event.getReason() + " (" + event.getCause() + ")");
            isConnected = false;
          }
        });

    client.connect();
    this.isConnected = true;

    mainLoopThread =
        new Thread(
            () -> {
              try {
                mainLoop();
              } catch (Exception e) {
                System.err.println("Main loop error: " + e.getMessage());
                e.printStackTrace();
              }
            });
    mainLoopThread.start();
  }

  public boolean getIsConnected() {
    return this.isConnected;
  }

  public void mainLoop() throws Exception {
    while (this.isConnected && !Thread.currentThread().isInterrupted()) {
      if (!this.navigation.isReady()) {
        Thread.sleep(500);
        continue;
      }
      PollOperationResponse pollResult =
          this.operator.pollOperation(this.agent, this.navigation.getCurrentLocation(), false);

      if (!(pollResult instanceof PollOperationResponse.OperationAvailable)) {
        Thread.sleep(1000);
        continue;
      }

      Operation op = ((PollOperationResponse.OperationAvailable) pollResult).getOperation();

      System.out.println(
          "acquired operation " + op.getKind().getClass().getName() + " (" + op.getId() + ")");

      Thread.sleep(1000);
    }
  }
}
