package me.mauldin.super_sorting_system.bot;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import me.mauldin.super_sorting_system.Operator;
import me.mauldin.super_sorting_system.Operator.Agent;
import me.mauldin.super_sorting_system.Operator.Location;
import me.mauldin.super_sorting_system.Operator.PathfindingResponse;
import me.mauldin.super_sorting_system.Operator.PfResultNode;
import me.mauldin.super_sorting_system.Operator.Vec3;
import org.cloudburstmc.math.vector.Vector3d;
import org.geysermc.mcprotocollib.network.ClientSession;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.SessionAdapter;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.game.ClientCommand;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.GlobalPos;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerSpawnInfo;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PositionElement;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundRespawnPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerCombatKillPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerPositionPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundForgetLevelChunkPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundLevelChunkWithLightPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundClientCommandPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundAcceptTeleportationPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerRotPacket;

public class Navigation extends SessionAdapter {
  private String dimension = "";
  private boolean dimensionReady = false;

  private double x = 0;
  private double y = 0;
  private double z = 0;
  private boolean positionReady = false;

  private Set<ChunkPos> loadedChunks = Collections.synchronizedSet(new HashSet());

  private ClientSession client;
  private Operator operator;
  private Agent agent;

  public Navigation(ClientSession client, Operator operator, Agent agent) {
    this.client = client;
    this.operator = operator;
    this.agent = agent;
  }

  @Override
  public void packetReceived(Session session, Packet packet) {
    if (packet instanceof ClientboundRespawnPacket respawnPacket) {
      PlayerSpawnInfo spawnInfo = respawnPacket.getCommonPlayerSpawnInfo();
      if (spawnInfo.getLastDeathPos() != null) {
        GlobalPos deathPos = spawnInfo.getLastDeathPos();
        System.out.println(
            "nav: death pos ("
                + deathPos.getX()
                + ", "
                + deathPos.getY()
                + ", "
                + deathPos.getZ()
                + ") ["
                + deathPos.getDimension().value()
                + "]");
        session.send(new ServerboundClientCommandPacket(ClientCommand.RESPAWN));
      }
      this.dimension = spawnInfo.getWorldName().toString();
      this.dimensionReady = true;
      this.positionReady = false;
      this.loadedChunks.clear();
    } else if (packet instanceof ClientboundLoginPacket loginPacket) {
      PlayerSpawnInfo spawnInfo = loginPacket.getCommonPlayerSpawnInfo();
      if (spawnInfo.getLastDeathPos() != null) {
        GlobalPos deathPos = spawnInfo.getLastDeathPos();
        System.out.println(
            "nav: death pos ("
                + deathPos.getX()
                + ", "
                + deathPos.getY()
                + ", "
                + deathPos.getZ()
                + ") ["
                + deathPos.getDimension().value()
                + "]");
        session.send(new ServerboundClientCommandPacket(ClientCommand.RESPAWN));
      }
      this.dimension = spawnInfo.getWorldName().toString();
      this.dimensionReady = true;
      this.positionReady = false;
      this.loadedChunks.clear();
    } else if (packet instanceof ClientboundLevelChunkWithLightPacket chunkPacket) {
      loadedChunks.add(new ChunkPos(chunkPacket.getX(), chunkPacket.getZ()));
    } else if (packet instanceof ClientboundForgetLevelChunkPacket chunkPacket) {
      loadedChunks.add(new ChunkPos(chunkPacket.getX(), chunkPacket.getZ()));
    } else if (packet instanceof ClientboundPlayerCombatKillPacket) {
      this.dimensionReady = false;
      this.positionReady = false;
      System.out.println("Entered respawn screen");
      session.send(new ServerboundClientCommandPacket(ClientCommand.RESPAWN));
    } else if (packet instanceof ClientboundPlayerPositionPacket positionPacket) {
      session.send(new ServerboundAcceptTeleportationPacket(positionPacket.getId()));

      if (this.positionReady) {
        System.out.println("nav: resending position after declined tp");
        this.client.send(new ServerboundMovePlayerPosPacket(true, false, this.x, this.y, this.z));
        return;
      }

      Vector3d position = positionPacket.getPosition();
      double x = position.getX();
      double y = position.getY();
      double z = position.getZ();
      List<PositionElement> relatives = positionPacket.getRelatives();

      if (relatives.contains(PositionElement.X)) {
        this.x += x;
      } else {
        this.x = x;
      }

      if (relatives.contains(PositionElement.Y)) {
        this.y += y;
      } else {
        this.y = y;
      }

      if (relatives.contains(PositionElement.Z)) {
        this.z += z;
      } else {
        this.z = z;
      }

      System.out.println("nav: position accepted");

      this.positionReady = true;
    }
  }

  public String getOperatorDimension() {
    if (this.dimension.equals("minecraft:the_nether")) {
      return "TheNether";
    } else if (this.dimension.equals("minecraft:the_end")) {
      return "TheEnd";
    }

    return "Overworld";
  }

  public boolean isReady() {
    return this.dimensionReady && this.positionReady;
  }

  public Location getCurrentLocation() {
    return new Location(
        new Vec3(
            ((int) Math.floor(this.x)), ((int) Math.floor(this.y)), ((int) Math.floor(this.z))),
        this.getOperatorDimension());
  }

  public void navigateTo(int x, int y, int z, String dimension)
      throws IOException, InterruptedException, Exception {
    System.out.println(
        "nav: starting navigation to (" + x + ", " + y + ", " + z + ") [" + dimension + "]");
    if (((int) Math.floor(this.x)) == x
        && ((int) Math.floor(this.y)) == y
        && ((int) Math.floor(this.z)) == z
        && dimension.equals(this.getOperatorDimension())) {
      System.out.println("nav: exiting - already at destination");
      return;
    }

    PathfindingResponse resp =
        this.operator.findPath(
            this.agent, this.getCurrentLocation(), new Location(new Vec3(x, y, z), dimension));

    if (!(resp instanceof PathfindingResponse.PathFound pathFound)) {
      throw new Exception("nav: error getting path");
    }
    List<PfResultNode> path = ((PathfindingResponse.PathFound) resp).getPath();

    for (PfResultNode node : path) {
      if (node instanceof PfResultNode.Vec vecNode) {
        Vec3 vec = vecNode.getVec();
        this.flyTo((int) vec.getX(), (int) vec.getY(), (int) vec.getZ());
      } else if (node instanceof PfResultNode.Portal portalNode) {
        Vec3 vec = portalNode.getVec();
        this.takePortal((int) vec.getX(), (int) vec.getY(), (int) vec.getZ());
      } else {
        throw new Exception("nav: unrecognized path node");
      }
    }

    System.out.println("nav: complete");
    Thread.sleep(100);
  }

  public void flyTo(int x, int y, int z) throws InterruptedException {
    double xf = x + 0.5;
    double yf = y;
    double zf = z + 0.5;
    System.out.println("nav: fly to (" + x + ", " + y + ", " + z + ")");
    Thread.sleep(100);

    this.setLocationWithPacket(xf, yf, zf);
    Thread.sleep(100);

    while (!this.isChunkLoadedAtPos(x, z)) {
      this.setLocationWithPacket(xf, yf, zf);
      Thread.sleep(100);
    }
    System.out.println("nav: chunk loaded");
  }

  private void setLocationWithPacket(double x, double y, double z) {
    this.x = x;
    this.y = y;
    this.z = z;

    // Relies on /gamerule disablePlayerMovementCheck true
    this.client.send(new ServerboundMovePlayerPosPacket(true, false, this.x, this.y, this.z));
  }

  public void takePortal(int x, int y, int z) throws Exception {
    double xf = x + 0.5;
    double yf = y;
    double zf = z + 0.5;

    double startingX = this.x;
    double startingY = this.y;
    double startingZ = this.z;

    System.out.println("nav: taking portal from " + this.getOperatorDimension() + "...");
    String startingDim = this.dimension;
    this.setLocationWithPacket(xf, yf, zf);

    for (int i = 0; i < 3 && startingDim.equals(this.dimension); i++) {
      System.out.println(
          "nav: taking portal from " + this.getOperatorDimension() + "... (" + (i + 1) + "/3)");

      this.setLocationWithPacket(startingX, startingY, startingZ);
      Thread.sleep(700 + (i * 750));
      this.setLocationWithPacket(xf, yf, zf);

      long startTime = System.nanoTime();

      while (startingDim.equals(this.dimension)) {
        long elapsedMs = (System.nanoTime() - startTime) / 1000000;
        if (elapsedMs > 3000) {
          break;
        }

        this.setLocationWithPacket(xf, yf, zf);
        Thread.sleep(100);
      }
    }

    if (startingDim.equals(this.dimension)) {
      throw new Exception("nav: failed to take portal");
    }

    System.out.println("nav: dimension transferred");

    while (!this.isChunkLoadedAtPos((int) this.x, (int) this.z)) {
      Thread.sleep(100);
    }
    System.out.println("nav: portal taken");
  }

  public void lookTowards(int towardsX, int towardsY, int towardsZ) {
    /*
     * The yaw and pitch of player (in degrees), standing at point (x0, y0, z0) and looking towards point (x, y, z) can be calculated with:
     *
     * dx = x-x0
     * dy = y-y0
     * dz = z-z0
     * r = sqrt( dx*dx + dy*dy + dz*dz )
     * yaw = -atan2(dx,dz)/PI*180
     * if yaw < 0 then
     *    yaw = 360 + yaw
     * pitch = -arcsin(dy/r)/PI*180
     */

    // Calculate deltas
    double dx = (towardsX + 0.5) - this.x;
    double dy = towardsY - this.y;
    double dz = (towardsZ + 0.5) - this.z;

    // Calculate distance
    double r = Math.sqrt(dx * dx + dy * dy + dz * dz);

    // Calculate yaw (horizontal rotation)
    double yaw = -Math.atan2(dx, dz) / Math.PI * 180;
    if (yaw < 0) {
      yaw = 360 + yaw;
    }

    // Calculate pitch (vertical rotation)
    double pitch = -Math.asin(dy / r) / Math.PI * 180;

    this.client.send(new ServerboundMovePlayerRotPacket(true, false, (float) yaw, (float) pitch));
  }

  // x and z are game coordinates, not chunk coordinates
  public boolean isChunkLoadedAtPos(int x, int z) {
    ChunkPos pos = new ChunkPos(Math.floorDiv(x, 16), Math.floorDiv(z, 16));
    return this.loadedChunks.contains(pos);
  }

  record ChunkPos(int x, int z) {}
}
