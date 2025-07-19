package me.mauldin.super_sorting_system.bot;

import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.network.event.session.SessionAdapter;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundPingPacket;
import org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundPongPacket;

public class ConnectionListeners extends SessionAdapter {
    @Override
    public void packetReceived(Session session, Packet packet) {
	if (packet instanceof ClientboundPingPacket pingPacket) {
	    session.send(new ServerboundPongPacket(
			pingPacket.getId()
	    ));
	}
    }
}
