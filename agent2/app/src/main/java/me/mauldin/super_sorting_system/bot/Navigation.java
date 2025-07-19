package me.mauldin.super_sorting_system.bot;

import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerSpawnInfo;
import org.geysermc.mcprotocollib.network.event.session.SessionAdapter;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockEntityInfo;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerPositionPacket;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockEntityType;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PositionElement;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundRespawnPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundAcceptTeleportationPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtList;
import java.util.stream.Collectors;
import java.util.List;
import org.json.JSONTokener;
import org.cloudburstmc.math.vector.Vector3d;

public class Navigation extends SessionAdapter {
    private String dimension = "";
    private double x = 0;
    private double y = 0;
    private double z = 0;

    @Override
    public void packetReceived(Session session, Packet packet) {
	if (packet instanceof ClientboundRespawnPacket respawnPacket) {
	    PlayerSpawnInfo spawnInfo = respawnPacket.getCommonPlayerSpawnInfo();
	    this.dimension = spawnInfo.getWorldName().toString();
	} else if (packet instanceof ClientboundLoginPacket loginPacket) {
	    PlayerSpawnInfo spawnInfo = loginPacket.getCommonPlayerSpawnInfo();
	    this.dimension = spawnInfo.getWorldName().toString();
	} else if (packet instanceof ClientboundPlayerPositionPacket positionPacket) {
	    Vector3d position = positionPacket.getPosition();
	    double x = position.getX();
	    double y = position.getY();
	    double z = position.getZ();
	    List<PositionElement> relatives = positionPacket.getRelatives();

	    if (relatives.contains(PositionElement.X)) {
		this.x += x;
	    } else {
		this.x = x;
	    }
	    
	    if (relatives.contains(PositionElement.Y)) {
		this.y += y;
	    } else {
		this.y = y;
	    }
	    
	    if (relatives.contains(PositionElement.Z)) {
		this.z += z;
	    } else {
		this.z = z;
	    }

	    session.send(new ServerboundAcceptTeleportationPacket(positionPacket.getId()));
	}
    }

    public String getOperatorDimension() {
	if (this.dimension == "minecraft:the_nether") {
	    return "TheNether";
	} else if (this.dimension == "minecraft:the_end") {
	    return "TheEnd";
	}

	return "Overworld";
    }
}
