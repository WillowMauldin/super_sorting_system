package me.mauldin.super_sorting_system.bot.operations;

import me.mauldin.super_sorting_system.Operator.Hold;
import me.mauldin.super_sorting_system.Operator.LoadShulkerOperationKind;
import me.mauldin.super_sorting_system.Operator.Location;
import me.mauldin.super_sorting_system.Operator.Vec3;
import me.mauldin.super_sorting_system.bot.Bot;
import me.mauldin.super_sorting_system.bot.InventoryUtil;
import me.mauldin.super_sorting_system.bot.WorldInteractions;

public class LoadShulker {
  public static void execute(Bot bot, LoadShulkerOperationKind op) throws Exception {
    Location shulkerStationLocation = op.getShulkerStationLocation();
    String shulkerHold = op.getShulkerHold();
    String[] sourceHolds = op.getSourceHolds();

    // Get shulker hold information
    Hold shulkerHoldData = bot.operator.getHold(shulkerHold, bot.agent);
    Location shulkerChestLocation = shulkerHoldData.getLocation();
    int shulkerChestSlot = shulkerHoldData.getSlot();
    Vec3 shulkerOpenFrom = shulkerHoldData.getOpenFrom();

    // Grab Shulker from chest
    bot.navigation.navigateTo(
        shulkerOpenFrom.getX(),
        shulkerOpenFrom.getY(),
        shulkerOpenFrom.getZ(),
        shulkerChestLocation.getDim());
    bot.inventoryTracker.openWindowAt(
        shulkerChestLocation.getVec3().getX(),
        shulkerChestLocation.getVec3().getY(),
        shulkerChestLocation.getVec3().getZ());

    // Transfer shulker from chest to player slot 27 (first hotbar slot)
    bot.inventoryTracker.transferItems(27, shulkerChestSlot, 1, false);

    // Send chest data and close
    InventoryUtil.uploadInventoryData(bot, shulkerChestLocation, shulkerOpenFrom);
    bot.inventoryTracker.closeWindow();

    // Grab Items to load from source holds
    Location lastChestLocation = null;
    Vec3 lastChestOpenFrom = null;
    boolean isChestOpen = false;

    for (int invSlot = 0; invSlot < sourceHolds.length; invSlot++) {
      if (sourceHolds[invSlot] == null) continue;

      // Get source hold information
      Hold sourceHold = bot.operator.getHold(sourceHolds[invSlot], bot.agent);
      Location sourceLocation = sourceHold.getLocation();
      int sourceSlot = sourceHold.getSlot();
      Vec3 sourceOpenFrom = sourceHold.getOpenFrom();

      // If we need to open a different chest, close current and open new one
      if (!locationEquals(sourceLocation, lastChestLocation)) {
        if (isChestOpen) {
          InventoryUtil.uploadInventoryData(bot, lastChestLocation, lastChestOpenFrom);
          bot.inventoryTracker.closeWindow();
          isChestOpen = false;
        }

        // Navigate to the chest opening position
        bot.navigation.navigateTo(
            sourceOpenFrom.getX(),
            sourceOpenFrom.getY(),
            sourceOpenFrom.getZ(),
            sourceLocation.getDim());

        // Open the chest
        bot.inventoryTracker.openWindowAt(
            sourceLocation.getVec3().getX(),
            sourceLocation.getVec3().getY(),
            sourceLocation.getVec3().getZ());

        isChestOpen = true;
        lastChestLocation = sourceLocation;
        lastChestOpenFrom = sourceOpenFrom;
      }

      // Transfer items from chest to player inventory
      bot.inventoryTracker.transferItems(invSlot, sourceSlot, Integer.MAX_VALUE, false);
    }

    // Close the final chest if one is open
    if (isChestOpen) {
      InventoryUtil.uploadInventoryData(bot, lastChestLocation, lastChestOpenFrom);
      bot.inventoryTracker.closeWindow();
    }

    // Navigate to shulker station and place shulker
    Vec3 stationVec = shulkerStationLocation.getVec3();
    bot.navigation.navigateTo(
        stationVec.getX(), stationVec.getY(), stationVec.getZ(), shulkerStationLocation.getDim());

    // Place shulker block using WorldInteractions
    WorldInteractions.placeBlockAt(
        bot, 0, stationVec.getX(), stationVec.getY() + 2, stationVec.getZ());

    // Wait for placement to complete
    Thread.sleep(250);

    // Open the placed shulker
    bot.inventoryTracker.openWindowAt(stationVec.getX(), stationVec.getY() + 2, stationVec.getZ());

    // Transfer items from player inventory to shulker
    for (int playerSlot = 0; playerSlot < 27; playerSlot++) {
      // Transfer from player inventory slot to shulker slot
      if (bot.inventoryTracker.getPlayerInventory()[playerSlot] != null) {
        bot.inventoryTracker.transferItems(playerSlot, playerSlot, Integer.MAX_VALUE, true);
      }
    }

    // Close shulker
    bot.inventoryTracker.closeWindow();

    // Break shulker using button (piston mechanism)
    WorldInteractions.pushButtonAt(
        bot, stationVec.getX(), stationVec.getY() + 4, stationVec.getZ());

    // Wait for piston to fully retract and shulker to be collected
    Thread.sleep(250);

    // Wait for shulker to appear in inventory (hotbar slot 0)
    for (int attempts = 0; attempts < 100; attempts++) {
      Thread.sleep(50);
      // The shulker should appear in the player's hotbar slot 0
      if (bot.inventoryTracker.getPlayerInventory()[27] != null) {
        break;
      }

      if (attempts > 20) {
        throw new Exception("did not get shulker back in inventory");
      }
    }

    // Put shulker back in its storage slot
    bot.navigation.navigateTo(
        shulkerOpenFrom.getX(),
        shulkerOpenFrom.getY(),
        shulkerOpenFrom.getZ(),
        shulkerChestLocation.getDim());
    bot.inventoryTracker.openWindowAt(
        shulkerChestLocation.getVec3().getX(),
        shulkerChestLocation.getVec3().getY(),
        shulkerChestLocation.getVec3().getZ());

    // Transfer shulker back to chest
    bot.inventoryTracker.transferItems(27, shulkerChestSlot, 1, true);

    // Send chest data and close
    InventoryUtil.uploadInventoryData(bot, shulkerChestLocation, shulkerOpenFrom);
    bot.inventoryTracker.closeWindow();
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
