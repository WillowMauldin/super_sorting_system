package me.mauldin.super_sorting_system.bot;

import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.network.event.session.SessionAdapter;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockEntityInfo;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundLevelChunkWithLightPacket;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockEntityType;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundChunkBatchFinishedPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundChunkBatchReceivedPacket;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtList;
import java.util.stream.Collectors;
import java.util.List;
import org.json.JSONTokener;

public class SignInfoListener extends SessionAdapter {
    @Override
    public void packetReceived(Session session, Packet packet) {
	if (packet instanceof ClientboundLevelChunkWithLightPacket chunkPacket) {
	    int x = chunkPacket.getX() * 16;
	    int z = chunkPacket.getZ() * 16;
	    for (BlockEntityInfo entity : chunkPacket.getBlockEntities()) {
		if (!(entity.getType() == BlockEntityType.SIGN || entity.getType() == BlockEntityType.HANGING_SIGN)) {
		    continue;
		}

		
		NbtMap data = entity.getNbt();
		List<String> front = SignInfoListener.mapMessages((NbtList<String>) ((NbtMap) data.get("front_text")).get("messages"));
		List<String> back = SignInfoListener.mapMessages((NbtList<String>) ((NbtMap) data.get("back_text")).get("messages"));
		int signX = entity.getX() + x;
		int signY = entity.getY();
		int signZ = entity.getZ() + z;
		System.out.println("Sign data: " + front.toString() + "(" + signX + ", " + signY + ", " + signZ + ")");
	    }
	    // Nowhere else uses chunk data, so handle this here
	    // The server won't send more chunks until we've acknowledged it
	} else if (packet instanceof ClientboundChunkBatchFinishedPacket) {
	    session.send(new ServerboundChunkBatchReceivedPacket(5));
	}
    }

    private static List<String> mapMessages(NbtList<String> list) {
	return list.stream()
	    .map(message -> {
		return (String) new org.json.JSONTokener(message).nextValue();
	    })
	    .collect(Collectors.toList());
    }
}
