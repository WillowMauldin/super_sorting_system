package me.mauldin.super_sorting_system.bot.operations;

import java.util.ArrayList;
import java.util.List;
import me.mauldin.super_sorting_system.Operator.Hold;
import me.mauldin.super_sorting_system.Operator.ImportInventoryOperationKind;
import me.mauldin.super_sorting_system.Operator.Location;
import me.mauldin.super_sorting_system.Operator.Vec3;
import me.mauldin.super_sorting_system.bot.Bot;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;

public class ImportInventory {
  public static void execute(Bot bot, ImportInventoryOperationKind op) throws Exception {
    Vec3 chestLocation = op.getChestLocation();
    Location nodeLocation = op.getNodeLocation();
    String[] destinationHolds = op.getDestinationHolds();

    // Navigate to the node location
    Vec3 nodeVec = nodeLocation.getVec3();
    bot.navigation.navigateTo(
        nodeVec.getX(), nodeVec.getY(), nodeVec.getZ(), nodeLocation.getDim());

    // Open the source chest
    bot.inventoryTracker.openWindowAt(
        chestLocation.getX(), chestLocation.getY(), chestLocation.getZ());

    // Get all items from the source chest
    ItemStack[] containerInventory = bot.inventoryTracker.getContainerInventory();
    List<Integer> itemSlots = new ArrayList<>();

    // Find all non-empty slots in the chest (slots 0-26 for a single chest)
    for (int slot = 0; slot < 27 && slot < containerInventory.length; slot++) {
      if (containerInventory[slot] != null) {
        itemSlots.add(slot);
      }
    }

    int takenItemsCount = 0;

    // Transfer items from chest to player inventory, limited by destination holds
    for (int chestSlot : itemSlots) {
      if (takenItemsCount >= destinationHolds.length) {
        break;
      }

      // Transfer entire stack from chest slot to player inventory slot
      bot.inventoryTracker.transferItems(takenItemsCount, chestSlot, Integer.MAX_VALUE, false);
      takenItemsCount++;
    }

    // Close the source chest
    bot.inventoryTracker.closeWindow();

    // Get destination holds
    List<Hold> destinationHoldsList = new ArrayList<>();
    for (int i = 0; i < takenItemsCount; i++) {
      Hold hold = bot.operator.getHold(destinationHolds[i], bot.agent);
      destinationHoldsList.add(hold);
    }

    Location lastChestLocation = null;
    Vec3 lastChestOpenFrom = null;
    boolean isChestOpen = false;

    // Transfer items from player inventory to destination chests
    for (int i = 0; i < takenItemsCount; i++) {
      Hold hold = destinationHoldsList.get(i);
      Location holdLocation = hold.getLocation();
      Vec3 holdOpenFrom = hold.getOpenFrom();

      // If we need to open a different chest, close current and open new one
      if (lastChestLocation == null || !locationEquals(holdLocation, lastChestLocation)) {
        if (isChestOpen) {
          bot.inventoryTracker.closeWindow();
          isChestOpen = false;
        }

        // Navigate to the chest opening position
        bot.navigation.navigateTo(
            holdOpenFrom.getX(), holdOpenFrom.getY(), holdOpenFrom.getZ(), holdLocation.getDim());

        // Open the destination chest
        bot.inventoryTracker.openWindowAt(
            holdLocation.getVec3().getX(),
            holdLocation.getVec3().getY(),
            holdLocation.getVec3().getZ());

        isChestOpen = true;
        lastChestLocation = holdLocation;
        lastChestOpenFrom = holdOpenFrom;
      }

      // Transfer entire stack from player inventory to chest
      bot.inventoryTracker.transferItems(i, hold.getSlot(), Integer.MAX_VALUE, true);
    }

    // Close the final chest if one is open
    if (isChestOpen) {
      bot.inventoryTracker.closeWindow();
    }
  }

  private static boolean locationEquals(Location loc1, Location loc2) {
    if (loc1 == null || loc2 == null) {
      return false;
    }
    Vec3 vec1 = loc1.getVec3();
    Vec3 vec2 = loc2.getVec3();
    return vec1.getX() == vec2.getX()
        && vec1.getY() == vec2.getY()
        && vec1.getZ() == vec2.getZ()
        && loc1.getDim().equals(loc2.getDim());
  }
}
