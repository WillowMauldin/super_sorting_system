package me.mauldin.super_sorting_system.bot;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import me.mauldin.super_sorting_system.Config;
import me.mauldin.super_sorting_system.Operator;
import me.mauldin.super_sorting_system.Operator.Agent;
import me.mauldin.super_sorting_system.Operator.DropItemsOperationKind;
import me.mauldin.super_sorting_system.Operator.ImportInventoryOperationKind;
import me.mauldin.super_sorting_system.Operator.MoveItemsOperationKind;
import me.mauldin.super_sorting_system.Operator.Operation;
import me.mauldin.super_sorting_system.Operator.OperationKind;
import me.mauldin.super_sorting_system.Operator.PollOperationResponse;
import me.mauldin.super_sorting_system.Operator.ScanInventoryOperationKind;
import me.mauldin.super_sorting_system.Operator.ScanSignsOperationKind;
import me.mauldin.super_sorting_system.bot.operations.DropItems;
import me.mauldin.super_sorting_system.bot.operations.ImportInventory;
import me.mauldin.super_sorting_system.bot.operations.MoveItems;
import me.mauldin.super_sorting_system.bot.operations.ScanInventory;
import me.mauldin.super_sorting_system.bot.operations.ScanSigns;
import net.raphimc.minecraftauth.step.java.StepMCProfile.MCProfile;
import net.raphimc.minecraftauth.step.java.StepMCToken.MCToken;
import net.raphimc.minecraftauth.step.java.session.StepFullJavaSession.FullJavaSession;
import org.geysermc.mcprotocollib.auth.GameProfile;
import org.geysermc.mcprotocollib.auth.SessionService;
import org.geysermc.mcprotocollib.network.ClientSession;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.ConnectedEvent;
import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent;
import org.geysermc.mcprotocollib.network.event.session.SessionAdapter;
import org.geysermc.mcprotocollib.network.factory.ClientNetworkSessionFactory;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.MinecraftConstants;
import org.geysermc.mcprotocollib.protocol.MinecraftProtocol;
import org.geysermc.mcprotocollib.protocol.packet.configuration.clientbound.ClientboundFinishConfigurationPacket;

public class Bot {
  private boolean isConnected;
  public final Navigation navigation;
  public final SignInfoListener signInfo;
  public final Operator operator;
  public final Agent agent;
  private Thread mainLoopThread;
  public final ClientSession client;
  public final InventoryTracker inventoryTracker;
  private final ScheduledExecutorService heartbeatScheduler;

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

    client =
        ClientNetworkSessionFactory.factory()
            .setRemoteSocketAddress(
                new InetSocketAddress(config.getMcServerHost(), config.getMcServerPort()))
            .setProtocol(protocol)
            .create();
    client.setFlag(MinecraftConstants.SESSION_SERVICE_KEY, sessionService);

    this.navigation = new Navigation(client, this.operator, this.agent);
    this.signInfo = new SignInfoListener(navigation, this.operator, this.agent);
    this.inventoryTracker = new InventoryTracker(client, navigation, this.operator, this.agent);

    client.addListener(new ConnectionListeners());
    client.addListener(navigation);
    client.addListener(signInfo);
    client.addListener(inventoryTracker);

    this.heartbeatScheduler = Executors.newSingleThreadScheduledExecutor();
    this.heartbeatScheduler.scheduleAtFixedRate(
        () -> {
          try {
            this.operator.heartbeat(this.agent);
          } catch (Exception e) {
            System.out.println("Heartbeat failed: " + e);
          }
        },
        15,
        15,
        TimeUnit.SECONDS);
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
            shutdown();
          }

          @Override
          public void packetReceived(Session session, Packet packet) {
            if (packet instanceof ClientboundFinishConfigurationPacket) {
              mainLoopThread.start();
            }
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
  }

  public boolean getIsConnected() {
    return this.isConnected;
  }

  public void shutdown() {
    if (this.isConnected) {
      this.isConnected = false;
      client.disconnect("Bot shutdown");
    }

    this.signInfo.shutdown();
    this.heartbeatScheduler.shutdown();
    mainLoopThread.interrupt();
  }

  private boolean atHome = false;

  public void mainLoop() throws Exception {
    while (this.isConnected && !Thread.currentThread().isInterrupted()) {
      if (!this.navigation.isReady()) {
        Thread.sleep(500);
        continue;
      }

      // Attempt to clear inventory before polling for operations
      boolean inventoryCleared = false;
      try {
        inventoryCleared = InventoryUtil.clearInventory(this);
      } catch (Exception e) {
        System.err.println("Error clearing inventory: " + e.getMessage());
        inventoryCleared = false;
      }

      PollOperationResponse pollResult =
          this.operator.pollOperation(
              this.agent, this.navigation.getCurrentLocation(), inventoryCleared);

      if (!(pollResult instanceof PollOperationResponse.OperationAvailable)) {
        if (!atHome) {
          try {
            var signConfig = this.operator.getSignConfig();
            var complexes = signConfig.getData().optJSONObject("complexes");

            if (complexes != null && complexes.length() > 0) {
              String complexKey = complexes.keys().next();
              var complex = complexes.getJSONObject(complexKey);

              if (complex.has("Tower")) {
                var tower = complex.getJSONObject("Tower");
                String dimension = tower.getString("dimension");
                var origin = tower.getJSONObject("origin");
                this.navigation.navigateTo(
                    origin.getInt("x"), origin.getInt("y"), origin.getInt("z"), dimension);
              } else if (complex.has("FlatFloor")) {
                var flatFloor = complex.getJSONObject("FlatFloor");
                String dimension = flatFloor.getString("dimension");
                var bounds = flatFloor.getJSONArray("bounds");
                var firstBound = bounds.getJSONObject(0);
                int yLevel = flatFloor.getInt("y_level");
                this.navigation.navigateTo(
                    firstBound.getInt("x"), yLevel + 1, firstBound.getInt("z"), dimension);
              }
            }

            atHome = true;
          } catch (Exception e) {
            System.err.println("Failed to navigate home: " + e.getMessage());
            e.printStackTrace();
          }
        }
        Thread.sleep(1000);
        continue;
      }

      atHome = false; // Reset home flag when starting an operation
      Operation op = ((PollOperationResponse.OperationAvailable) pollResult).getOperation();
      OperationKind kind = op.getKind();

      System.out.println(
          "acquired operation " + op.getKind().getClass().getName() + " (" + op.getId() + ")");

      try {

        if (kind instanceof ScanSignsOperationKind scanSignsKind) {
          ScanSigns.execute(this, scanSignsKind);
        } else if (kind instanceof ScanInventoryOperationKind scanInventoryKind) {
          ScanInventory.execute(this, scanInventoryKind);
        } else if (kind instanceof MoveItemsOperationKind moveItemsKind) {
          MoveItems.execute(this, moveItemsKind);
        } else if (kind instanceof DropItemsOperationKind dropItemsKind) {
          DropItems.execute(this, dropItemsKind);
        } else if (kind instanceof ImportInventoryOperationKind importInventoryKind) {
          ImportInventory.execute(this, importInventoryKind);
        } else {
          throw new Exception("unrecognized operation kind");
        }

        this.operator.operationComplete(this.agent, op, "Complete");
      } catch (Exception e) {
        System.out.println("operation failed");
        e.printStackTrace();

        this.operator.operationComplete(this.agent, op, "Aborted");
      }
    }
  }
}
