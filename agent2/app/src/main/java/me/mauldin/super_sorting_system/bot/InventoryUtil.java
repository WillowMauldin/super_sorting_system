package me.mauldin.super_sorting_system.bot;

import java.util.List;
import java.util.stream.Collectors;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
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
    } else if (value == null) {
      return null;
    } else {
      return value.toString();
    }
  }
}
