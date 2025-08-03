package me.mauldin.super_sorting_system.bot;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import me.mauldin.super_sorting_system.Operator.FreeHoldResponse;
import me.mauldin.super_sorting_system.Operator.Hold;
import me.mauldin.super_sorting_system.Operator.Item;
import me.mauldin.super_sorting_system.Operator.Location;
import me.mauldin.super_sorting_system.Operator.Vec3;
import net.kyori.adventure.text.TextComponent;
import org.geysermc.mcprotocollib.protocol.data.game.item.HashedStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;
import org.json.JSONArray;
import org.json.JSONObject;

public class InventoryUtil {
  public static Object serializeDataComponents(DataComponents dataComponents) {
    return InventoryUtil.serializeValue(dataComponents);
  }

  private static Object serializeValue(Object value) {
    if (value instanceof List listVal) {
      return new JSONArray(
          (List<Object>)
              listVal.stream()
                  .map(entry -> InventoryUtil.serializeValue(entry))
                  .collect(Collectors.toList()));
    } else if (value instanceof DataComponents map) {
      JSONObject obj = new JSONObject();

      obj.put("_raw", value.toString());

      for (var entry : map.getDataComponents().entrySet()) {
        obj.put(
            entry.getKey().getKey().toString(),
            InventoryUtil.serializeValue(entry.getValue().getValue()));
      }

      return obj;
    } else if (value instanceof ItemStack stack) {
      JSONObject obj = new JSONObject();

      obj.put("id", stack.getId());
      obj.put("amount", stack.getAmount());

      DataComponents dataComponents = stack.getDataComponentsPatch();
      if (dataComponents != null) {
        obj.put("data_components", InventoryUtil.serializeValue(dataComponents));
      }

      return obj;
    } else if (value instanceof TextComponent tc) {
      JSONObject obj = new JSONObject();

      obj.put("text_content", tc.content());

      return obj;
    } else if (value == null) {
      return null;
    } else {
      return value.toString();
    }
  }

  /**
   * Clears the bot's inventory by transferring all items to available holds. Returns true if
   * inventory was successfully cleared, false otherwise.
   */
  public static boolean clearInventory(Bot bot) throws Exception {
    // Check if inventory is already empty
    ItemStack[] playerInventory = bot.inventoryTracker.getPlayerInventory();
    if (playerInventory == null) {
      return true; // No inventory to clear
    }

    // Check each inventory slot (0-35: 27 main inventory + 9 hotbar)
    for (int invSlot = 0; invSlot < playerInventory.length; invSlot++) {
      ItemStack contents = playerInventory[invSlot];
      if (contents == null) {
        continue; // Empty slot, skip
      }

      // Try to get a free hold for this item
      FreeHoldResponse response = bot.operator.getFreeHold(bot.agent);

      if (!(response instanceof FreeHoldResponse.HoldAcquired holdAcquired)) {
        System.err.println("Could not acquire a free hold to clear inventory!");
        return false;
      }

      Hold hold = holdAcquired.getHold();
      Location location = hold.getLocation();
      Vec3 openFrom = hold.getOpenFrom();
      String holdId = hold.getId();

      try {
        // Navigate to the chest opening position
        bot.navigation.navigateTo(
            openFrom.getX(), openFrom.getY(), openFrom.getZ(), location.getDim());

        // Open the chest
        bot.inventoryTracker.openWindowAt(
            location.getVec3().getX(), location.getVec3().getY(), location.getVec3().getZ());

        // Transfer the entire stack from player inventory to chest
        bot.inventoryTracker.transferItems(invSlot, hold.getSlot(), Integer.MAX_VALUE, true);

        InventoryUtil.uploadInventoryData(bot, location, openFrom);

        // Close the chest
        bot.inventoryTracker.closeWindow();

        // Release the hold
        bot.operator.releaseHold(holdId);

      } catch (Exception e) {
        // Release the hold even if operation failed
        try {
          bot.operator.releaseHold(holdId);
        } catch (Exception releaseException) {
          System.err.println(
              "Failed to release hold after error: " + releaseException.getMessage());
        }
        throw e; // Re-throw the original exception
      }
    }

    return true;
  }

  public static void uploadInventoryData(Bot bot, Location loc, Vec3 openFromVec) throws Exception {
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
  }

  public static HashedStack itemStackToHashedStack(ItemStack itemStack) {
    HashMap<DataComponentType<?>, Integer> addedComponents = new HashMap<>();
    HashSet<DataComponentType<?>> removedComponents = new HashSet<>();

    DataComponents dataComponents = itemStack.getDataComponentsPatch();

    if (dataComponents != null) {
      for (var entry : dataComponents.getDataComponents().entrySet()) {
        if (entry.getValue() == null) {
          removedComponents.add(entry.getKey());
        } else {
          addedComponents.put(entry.getKey(), entry.hashCode());
        }
      }
    }

    return new HashedStack(
        itemStack.getId(), itemStack.getAmount(), addedComponents, removedComponents);
  }
}
