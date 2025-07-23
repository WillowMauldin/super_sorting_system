package me.mauldin.super_sorting_system.bot.operations;

import java.util.ArrayList;
import java.util.List;
import me.mauldin.super_sorting_system.Operator.DropItemsOperationKind;
import me.mauldin.super_sorting_system.Operator.Hold;
import me.mauldin.super_sorting_system.Operator.Location;
import me.mauldin.super_sorting_system.Operator.Vec3;
import me.mauldin.super_sorting_system.bot.Bot;

public class DropItems {
  public static void execute(Bot bot, DropItemsOperationKind op) throws Exception {
    String[] sourceHolds = op.getSourceHolds();
    Location dropFrom = op.getDropFrom();
    Vec3 aimTowards = op.getAimTowards();

    // Get source holds
    List<Hold> sourceHoldsList = new ArrayList<>();
    for (String holdId : sourceHolds) {
      Hold hold = bot.operator.getHold(holdId, bot.agent);
      sourceHoldsList.add(hold);
    }

    Location lastChestLocation = null;
    Vec3 lastChestOpenFrom = null;
    boolean isChestOpen = false;

    // Process source holds - take items from chests to player inventory
    for (int invSlot = 0; invSlot < sourceHoldsList.size(); invSlot++) {
      Hold hold = sourceHoldsList.get(invSlot);
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

      // Transfer all items from chest slot to player inventory slot
      // Using Integer.MAX_VALUE to transfer the entire stack
      bot.inventoryTracker.transferItems(invSlot, hold.getSlot(), Integer.MAX_VALUE, false);
    }

    // Close the final chest if one is open
    if (isChestOpen) {
      bot.inventoryTracker.closeWindow();
    }

    // Navigate to drop position
    Vec3 dropVec = dropFrom.getVec3();
    bot.navigation.navigateTo(dropVec.getX(), dropVec.getY(), dropVec.getZ(), dropFrom.getDim());

    // Drop items from player inventory slots
    for (int invSlot = 0; invSlot < sourceHolds.length; invSlot++) {
      bot.inventoryTracker.dropItems(invSlot);
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
