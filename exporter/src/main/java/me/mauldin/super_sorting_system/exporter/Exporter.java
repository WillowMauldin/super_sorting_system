package me.mauldin.super_sorting_system.exporter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.Item;
import net.minecraft.registry.BuiltinRegistries;
import net.minecraft.registry.Registries;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;

public class Exporter implements ModInitializer {
	public static final String MOD_ID = "exporter";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Beginning export process");

		ArrayList<ExportedItem> exportedItems = new ArrayList<>();

		LOGGER.info("Exporting items");
		for (var entry : Registries.ITEM.getEntrySet()) {
			var item = entry.getValue();
			var registryKey = entry.getKey();

			int rawId = Item.getRawId(item);
			String displayName = item.getName().getString();
			String key = registryKey.getValue().toString();
			int maxCount = item.getMaxCount();

			exportedItems.add(new ExportedItem(rawId, displayName, key, maxCount));
		}

		exportedItems.sort((a, b) -> Integer.compare(a.rawId(), b.rawId()));

		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String export = gson.toJson(exportedItems);

		Path writeToPath = FabricLoader.getInstance().getGameDir().resolve("item_export.json");

		try {
			FileUtils.writeStringToFile(new File(writeToPath.toUri()), export, "UTF-8");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		LOGGER.info("Item export successful. Written to {}", writeToPath.toAbsolutePath());
	}
}

record ExportedItem(int rawId, String displayName, String key, int maxCount) {}