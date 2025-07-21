package me.mauldin.super_sorting_system.bot.operations;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import me.mauldin.super_sorting_system.Operator.Item;
import me.mauldin.super_sorting_system.Operator.Location;
import me.mauldin.super_sorting_system.Operator.ScanInventoryOperationKind;
import me.mauldin.super_sorting_system.Operator.Vec3;
import me.mauldin.super_sorting_system.bot.Bot;
import me.mauldin.super_sorting_system.bot.InventoryUtil;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;

public class ScanInventory {
  public static void execute(Bot bot, ScanInventoryOperationKind op) throws Exception {
    Location loc = op.getLocation();
    Vec3 inventoryVec = loc.getVec3();
    Vec3 openFromVec = op.getOpenFrom();

    bot.navigation.navigateTo(
        openFromVec.getX(), openFromVec.getY(), openFromVec.getZ(), loc.getDim());

    bot.inventoryTracker.openWindowAt(
        inventoryVec.getX(), inventoryVec.getY(), inventoryVec.getZ());

    ItemStack[] items = bot.inventoryTracker.getContainerInventory();
    List<Item> slots =
        Arrays.stream(items)
            .map(
                stack -> {
                  if (stack == null) return null;
                  DataComponents components = stack.getDataComponentsPatch();
                  Object serializedComponents =
                      components != null ? InventoryUtil.serializeDataComponents(components) : null;
                  return new Item(stack.getId(), stack.getAmount(), serializedComponents);
                })
            .collect(Collectors.toList());

    bot.operator.inventoryScanned(slots, loc, openFromVec, bot.agent);

    bot.inventoryTracker.closeWindow();
  }
}
