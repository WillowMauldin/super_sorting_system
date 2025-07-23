package me.mauldin.super_sorting_system.bot.operations;

import java.util.ArrayList;
import java.util.List;
import me.mauldin.super_sorting_system.Operator.Hold;
import me.mauldin.super_sorting_system.Operator.Location;
import me.mauldin.super_sorting_system.Operator.MoveItemsOperationKind;
import me.mauldin.super_sorting_system.Operator.Vec3;
import me.mauldin.super_sorting_system.bot.Bot;

public class MoveItems {
  public static void execute(Bot bot, MoveItemsOperationKind op) throws Exception {
    String[] sourceHolds = op.getSourceHolds();
    String[] destinationHolds = op.getDestinationHolds();
    int[] counts = op.getCounts();

    // Get source holds
    List<Hold> sourceHoldsList = new ArrayList<>();
    for (String holdId : sourceHolds) {
      Hold hold = bot.operator.getHold(holdId, bot.agent);
      sourceHoldsList.add(hold);
    }

    // Get destination holds
    List<Hold> destinationHoldsList = new ArrayList<>();
    for (String holdId : destinationHolds) {
      Hold hold = bot.operator.getHold(holdId, bot.agent);
      destinationHoldsList.add(hold);
    }

    Location lastChestLocation = null;
    Vec3 lastChestOpenFrom = null;
    boolean isChestOpen = false;

    // Process source holds - take items from chests to player inventory
    for (int i = 0; i < sourceHoldsList.size(); i++) {
      Hold hold = sourceHoldsList.get(i);
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

        // Open the chest
        bot.inventoryTracker.openWindowAt(
            holdLocation.getVec3().getX(),
            holdLocation.getVec3().getY(),
            holdLocation.getVec3().getZ());

        isChestOpen = true;
        lastChestLocation = holdLocation;
        lastChestOpenFrom = holdOpenFrom;
      }

      // Transfer items from chest to player inventory
      bot.inventoryTracker.transferItems(i, hold.getSlot(), counts[i], false);
    }

    // Process destination holds - put items from player inventory to chests
    for (int i = 0; i < destinationHoldsList.size(); i++) {
      Hold hold = destinationHoldsList.get(i);
      Location holdLocation = hold.getLocation();
      Vec3 holdOpenFrom = hold.getOpenFrom();

      // If we need to open a different chest, close current and open new one
      if (!locationEquals(holdLocation, lastChestLocation)) {
        if (isChestOpen) {
          bot.inventoryTracker.closeWindow();
          isChestOpen = false;
        }

        // Navigate to the chest opening position
        bot.navigation.navigateTo(
            holdOpenFrom.getX(), holdOpenFrom.getY(), holdOpenFrom.getZ(), holdLocation.getDim());

        // Open the chest
        bot.inventoryTracker.openWindowAt(
            holdLocation.getVec3().getX(),
            holdLocation.getVec3().getY(),
            holdLocation.getVec3().getZ());

        isChestOpen = true;
        lastChestLocation = holdLocation;
        lastChestOpenFrom = holdOpenFrom;
      }

      // Transfer items from player inventory to chest
      bot.inventoryTracker.transferItems(i, hold.getSlot(), counts[i], true);
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
