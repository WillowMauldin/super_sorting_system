package me.mauldin.super_sorting_system.bot;

import org.cloudburstmc.math.vector.Vector3i;
import org.geysermc.mcprotocollib.protocol.data.game.entity.object.Direction;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundSetCarriedItemPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundUseItemOnPacket;

public class WorldInteractions {
  // This places a block at the position given, by placing it on the bottom side of the block above
  // that
  public static void placeBlockAt(Bot bot, int hotbarSlot, int x, int y, int z) throws Exception {
    if (hotbarSlot < 0 || hotbarSlot > 8) {
      throw new Exception("world: Invalid hotbar slot " + hotbarSlot);
    }

    if (bot.inventoryTracker.getPlayerInventory()[hotbarSlot + 27] == null) {
      throw new Exception("world: no item in hotbar slot " + hotbarSlot);
    }

    bot.client.send(new ServerboundSetCarriedItemPacket(hotbarSlot));

    bot.client.send(
        new ServerboundUseItemOnPacket(
            Vector3i.from(x, y + 1, z),
            Direction.DOWN,
            Hand.MAIN_HAND,
            0.5f,
            0.5f,
            0.5f,
            false,
            false,
            1));

    bot.inventoryTracker.getPlayerInventory()[hotbarSlot + 27] = null;
  }

  public static void pushButtonAt(Bot bot, int x, int y, int z) {
    bot.client.send(
        new ServerboundUseItemOnPacket(
            Vector3i.from(x, y, z),
            Direction.DOWN,
            Hand.MAIN_HAND,
            0.5f,
            0.5f,
            0.5f,
            false,
            false,
            1));
  }
}
