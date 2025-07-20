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
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerSpawnInfo;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PositionElement;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundRespawnPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerPositionPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundForgetLevelChunkPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundLevelChunkWithLightPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundAcceptTeleportationPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosPacket;

public class Navigation extends SessionAdapter {
  private String dimension = "";
  private double x = 0;
  private double y = 0;
  private double z = 0;
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
      this.dimension = spawnInfo.getWorldName().toString();
      this.loadedChunks.clear();
    } else if (packet instanceof ClientboundLoginPacket loginPacket) {
      PlayerSpawnInfo spawnInfo = loginPacket.getCommonPlayerSpawnInfo();
      this.dimension = spawnInfo.getWorldName().toString();
      this.loadedChunks.clear();
    } else if (packet instanceof ClientboundLevelChunkWithLightPacket chunkPacket) {
      loadedChunks.add(new ChunkPos(chunkPacket.getX(), chunkPacket.getZ()));
    } else if (packet instanceof ClientboundForgetLevelChunkPacket chunkPacket) {
      loadedChunks.add(new ChunkPos(chunkPacket.getX(), chunkPacket.getZ()));
    } else if (packet instanceof ClientboundPlayerPositionPacket positionPacket) {
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

      session.send(new ServerboundAcceptTeleportationPacket(positionPacket.getId()));
    }
  }

  public String getOperatorDimension() {
    if (this.dimension == "minecraft:the_nether") {
      return "TheNether";
    } else if (this.dimension == "minecraft:the_end") {
      return "TheEnd";
    }

    return "Overworld";
  }

  public void navigateTo(int x, int y, int z, String dimension)
      throws IOException, InterruptedException, Exception {
    if (((int) Math.floor(this.x)) == x
        && ((int) Math.floor(this.y)) == y
        && ((int) Math.floor(this.z)) == z
        && dimension.equals(this.getOperatorDimension())) {
      return;
    }

    PathfindingResponse resp =
        this.operator.findPath(
            this.agent,
            new Location(new Vec3(this.x, this.y, this.z), this.getOperatorDimension()),
            new Location(new Vec3(x, y, z), dimension));

    if (!(resp instanceof PathfindingResponse.PathFound pathFound)) {
      throw new Exception("nav: error getting path");
    }
    List<PfResultNode> path = ((PathfindingResponse.PathFound) resp).getPath();

    for (PfResultNode node : path) {
      if (node instanceof PfResultNode.Vec vecNode) {
        Vec3 vec = vecNode.getVec();
        this.flyTo((int) vec.getX(), (int) vec.getY(), (int) vec.getY());
      } else if (node instanceof PfResultNode.Portal portalNode) {
        Vec3 vec = portalNode.getVec();
        this.takePortal((int) vec.getX(), (int) vec.getY(), (int) vec.getZ());
      } else {
        throw new Exception("nav: unrecognized path node");
      }
    }
  }

  private void flyTo(int x, int y, int z) throws InterruptedException {
    this.x = x + 0.5;
    this.y = y;
    this.z = z + 0.5;
    this.client.send(new ServerboundMovePlayerPosPacket(true, false, this.x, this.y, this.z));

    while (!this.isChunkLoadedAtPos(x, z)) {
      Thread.sleep(100);
    }
  }

  private void takePortal(int x, int y, int z) throws InterruptedException {
    String startingDim = this.dimension;
    Thread.sleep(700);
    this.flyTo(x, y, z);

    while (startingDim.equals(this.dimension)) {
      Thread.sleep(100);
    }

    while (!this.isChunkLoadedAtPos((int) this.x, (int) this.z)) {
      Thread.sleep(100);
    }
  }

  // x and z are game coordinates, not chunk coordinates
  private boolean isChunkLoadedAtPos(int x, int z) {
    ChunkPos pos = new ChunkPos(Math.floorDiv(x, 16), Math.floorDiv(z, 16));
    return this.loadedChunks.contains(pos);
  }

  record ChunkPos(int x, int z) {}
}
