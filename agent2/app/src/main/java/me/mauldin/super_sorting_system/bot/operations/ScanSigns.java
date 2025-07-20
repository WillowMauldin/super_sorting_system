package me.mauldin.super_sorting_system.bot.operations;

import java.util.List;
import me.mauldin.super_sorting_system.Operator.Location;
import me.mauldin.super_sorting_system.Operator.ScanSignsOperationKind;
import me.mauldin.super_sorting_system.Operator.Vec3;
import me.mauldin.super_sorting_system.bot.Bot;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.HandPreference;
import org.geysermc.mcprotocollib.protocol.data.game.setting.ChatVisibility;
import org.geysermc.mcprotocollib.protocol.data.game.setting.ParticleStatus;
import org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundClientInformationPacket;

public class ScanSigns {
  public static void execute(Bot bot, ScanSignsOperationKind op) throws Exception {
    Location loc = op.getLocation();
    Vec3 vec = loc.getVec3();
    bot.navigation.navigateTo(vec.getX(), vec.getY(), vec.getZ(), loc.getDim());

    Vec3 portalVec = op.getTakePortal();
    if (portalVec != null) {
      bot.navigation.takePortal(portalVec.getX(), portalVec.getY(), portalVec.getZ());
    }

    bot.client.send(
        new ServerboundClientInformationPacket(
            "en_us",
            16,
            ChatVisibility.FULL,
            true,
            List.of(),
            HandPreference.RIGHT_HAND,
            false,
            true,
            ParticleStatus.ALL));

    Thread.sleep(1000);
    while (bot.signInfo.getMsSinceLastChunk() < 1000) {
      Thread.sleep(500);
    }

    bot.client.send(
        new ServerboundClientInformationPacket(
            "en_us",
            16,
            ChatVisibility.FULL,
            true,
            List.of(),
            HandPreference.RIGHT_HAND,
            false,
            true,
            ParticleStatus.ALL));
  }
}
