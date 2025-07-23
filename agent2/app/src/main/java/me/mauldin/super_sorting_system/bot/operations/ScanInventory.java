package me.mauldin.super_sorting_system.bot.operations;

import me.mauldin.super_sorting_system.Operator.Location;
import me.mauldin.super_sorting_system.Operator.ScanInventoryOperationKind;
import me.mauldin.super_sorting_system.Operator.Vec3;
import me.mauldin.super_sorting_system.bot.Bot;
import me.mauldin.super_sorting_system.bot.InventoryUtil;

public class ScanInventory {
  public static void execute(Bot bot, ScanInventoryOperationKind op) throws Exception {
    Location loc = op.getLocation();
    Vec3 inventoryVec = loc.getVec3();
    Vec3 openFromVec = op.getOpenFrom();

    bot.navigation.navigateTo(
        openFromVec.getX(), openFromVec.getY(), openFromVec.getZ(), loc.getDim());

    bot.inventoryTracker.openWindowAt(
        inventoryVec.getX(), inventoryVec.getY(), inventoryVec.getZ());

    InventoryUtil.uploadInventoryData(bot, loc, openFromVec);

    bot.inventoryTracker.closeWindow();
  }
}
