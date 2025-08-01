package me.mauldin.super_sorting_system.bot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import me.mauldin.super_sorting_system.Operator;
import me.mauldin.super_sorting_system.Operator.Agent;
import me.mauldin.super_sorting_system.Operator.Location;
import me.mauldin.super_sorting_system.Operator.ScanRegion;
import me.mauldin.super_sorting_system.Operator.Sign;
import me.mauldin.super_sorting_system.Operator.Vec2;
import me.mauldin.super_sorting_system.Operator.Vec3;
import org.cloudburstmc.nbt.NbtList;
import org.cloudburstmc.nbt.NbtMap;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.SessionAdapter;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockEntityInfo;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockEntityType;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundChunkBatchFinishedPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundLevelChunkWithLightPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundChunkBatchReceivedPacket;

public class SignInfoListener extends SessionAdapter {
  private final Navigation navigation;
  private final Operator operator;
  private final Agent agent;
  private List<ScanRegion> pendingRegions;
  private final ScheduledExecutorService uploadScheduler;
  private long chunkLastSeenAt = System.nanoTime();

  public SignInfoListener(Navigation navigation, Operator operator, Agent agent) {
    this.navigation = navigation;
    this.operator = operator;
    this.agent = agent;
    this.pendingRegions = Collections.synchronizedList(new ArrayList());
    this.uploadScheduler = Executors.newSingleThreadScheduledExecutor();
    this.uploadScheduler.scheduleAtFixedRate(this::uploadSignData, 5, 5, TimeUnit.SECONDS);
  }

  public long getMsSinceLastChunk() {
    return (System.nanoTime() - this.chunkLastSeenAt) / 1000000;
  }

  @Override
  public void packetReceived(Session session, Packet packet) {
    if (packet instanceof ClientboundLevelChunkWithLightPacket chunkPacket) {
      int x = chunkPacket.getX() * 16;
      int z = chunkPacket.getZ() * 16;

      List<Sign> signs = new ArrayList();
      for (BlockEntityInfo entity : chunkPacket.getBlockEntities()) {
        if (!(entity.getType() == BlockEntityType.SIGN
            || entity.getType() == BlockEntityType.HANGING_SIGN)) {
          continue;
        }

        NbtMap data = entity.getNbt();
        List<String> front = (NbtList<String>) ((NbtMap) data.get("front_text")).get("messages");
        List<String> back = (NbtList<String>) ((NbtMap) data.get("back_text")).get("messages");

        Vec3 vec3 = new Vec3(entity.getX() + x, entity.getY(), entity.getZ() + z);
        Location loc = new Location(vec3, this.navigation.getOperatorDimension());

        signs.add(new Sign(front, loc));
        signs.add(new Sign(back, loc));
      }

      Vec2[] bounds = {
        new Vec2(x, z), new Vec2(x + 15, z + 15),
      };
      this.pendingRegions.add(
          new ScanRegion(signs, bounds, this.navigation.getOperatorDimension()));
      this.chunkLastSeenAt = System.nanoTime();
    } else if (packet instanceof ClientboundChunkBatchFinishedPacket) {
      // Nowhere else uses chunk data, so handle this here
      // The server won't send more chunks until we've acknowledged it
      session.send(new ServerboundChunkBatchReceivedPacket(5));
    }
  }

  private void uploadSignData() {
    synchronized (this.pendingRegions) {
      if (this.pendingRegions.size() == 0) {
        return;
      }

      try {
        this.operator.sendSignScanData(agent, this.pendingRegions);
        this.pendingRegions.clear();
      } catch (IOException e) {
        System.out.println("Failed to upload sign data: " + e);
      } catch (InterruptedException e) {
        System.out.println("Failed to upload sign data: " + e);
      }
    }
  }

  public void shutdown() {
    this.uploadScheduler.shutdown();
  }
}
