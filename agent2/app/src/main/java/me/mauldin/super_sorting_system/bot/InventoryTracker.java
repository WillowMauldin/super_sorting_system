package me.mauldin.super_sorting_system.bot;

import java.util.Arrays;
import java.util.HashMap;
import me.mauldin.super_sorting_system.Operator;
import me.mauldin.super_sorting_system.Operator.Agent;
import org.cloudburstmc.math.vector.Vector3i;
import org.geysermc.mcprotocollib.network.ClientSession;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.SessionAdapter;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.game.entity.object.Direction;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ClickItemAction;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerActionType;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerType;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.DropItemAction;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundContainerClosePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundContainerSetContentPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundContainerSetSlotPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundOpenScreenPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClickPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClosePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundUseItemOnPacket;

/*
 *
 * Player Inventory
 * 0: Crafting Output
 * 1-4: 2x2 Crafting Input
 * 5-8: Armor
 * 9-35: Main Inventory
 * 36-44: Hotbar
 * 45: Offhand
 *
 * generic_9x3 (single chest, barrel, etc) / shulker box
 * 0-26: Chest
 * 27-53: Main Inventory
 * 54-62: Hotbar
 *
 */

public class InventoryTracker extends SessionAdapter {
  private final ClientSession client;
  private final Operator operator;
  private final Agent agent;
  private final Navigation navigation;

  // 36 items correspending to 27 main inventory slots then 9 hotbar slots
  private ItemStack[] playerInventory;
  // 27 items corresponding to the open container
  private ItemStack[] containerInventory;

  private int stateId = 0;
  private int currentlyOpenScreen = 0;

  public InventoryTracker(
      ClientSession client, Navigation navigation, Operator operator, Agent agent) {
    this.client = client;
    this.navigation = navigation;
    this.operator = operator;
    this.agent = agent;
  }

  @Override
  public void packetReceived(Session session, Packet packet) {
    if (packet instanceof ClientboundOpenScreenPacket openScreenPacket) {
      System.out.println("inv: screen opening");
      boolean supportedType =
          openScreenPacket.getType() == ContainerType.GENERIC_9X3
              || openScreenPacket.getType() == ContainerType.SHULKER_BOX;
      if (!supportedType) {
        System.out.println(
            "inv: container other than generic 9x3 or shulker box opened, initiating close.");
        this.closeWindow();
      }

      this.currentlyOpenScreen = openScreenPacket.getContainerId();
      this.containerInventory = null;
    } else if (packet instanceof ClientboundContainerSetContentPacket setContentPacket) {
      System.out.println("set container content");
      if (setContentPacket.getContainerId() != this.currentlyOpenScreen) {
        System.out.println(
            "inv: container other than currently tracked got set content packet, initiating close");
        this.closeWindow();
        return;
      }

      this.stateId = setContentPacket.getStateId();

      if (setContentPacket.getContainerId() == 0) {
        // Player inventory
        this.playerInventory = Arrays.copyOfRange(setContentPacket.getItems(), 9, 45);
      } else {
        // Container inventory
        this.containerInventory = Arrays.copyOfRange(setContentPacket.getItems(), 0, 27);
        // Player inventory
        this.playerInventory = Arrays.copyOfRange(setContentPacket.getItems(), 27, 63);
      }

      synchronized (this) {
        this.notifyAll();
      }
    } else if (packet instanceof ClientboundContainerSetSlotPacket setSlotPacket) {
      if (setSlotPacket.getContainerId() != this.currentlyOpenScreen) {
        System.out.println(
            "inv: container other than currently tracked got set slot packet, initiating close");
        this.closeWindow();
        return;
      }

      this.stateId = setSlotPacket.getStateId();
      int slot = setSlotPacket.getSlot();

      if (setSlotPacket.getContainerId() == 0) {
        // Player inventory

        int ourSlot = slot - 9;
        if (ourSlot < 0 || ourSlot > 35) {
          // We don't track these slots
          return;
        }

        this.playerInventory[ourSlot] = setSlotPacket.getItem();
      } else {
        if (slot < 27) {
          // Container inventory

          this.containerInventory[slot] = setSlotPacket.getItem();
        } else {
          int ourSlot = slot - 27;
          if (ourSlot > 35) {
            return;
          }

          this.playerInventory[ourSlot] = setSlotPacket.getItem();
        }
      }
    } else if (packet instanceof ClientboundContainerClosePacket closePacket) {
      if (closePacket.getContainerId() != this.currentlyOpenScreen) {
        System.out.println(
            "inv: container other than currently tracked got close packet, resetting state");
      }

      this.currentlyOpenScreen = 0;
      this.containerInventory = null;
    }
  }

  public void closeWindow() {
    if (this.currentlyOpenScreen == 0) {
      return;
    }

    this.client.send(new ServerboundContainerClosePacket(this.currentlyOpenScreen));

    this.currentlyOpenScreen = 0;
    this.containerInventory = null;
  }

  public void openWindowAt(int x, int y, int z) throws InterruptedException {
    System.out.println("inv: opening window at (" + x + ", " + y + ", " + z + ")");
    this.closeWindow();

    while (!this.navigation.isChunkLoadedAtPos(x, z)) {
      Thread.sleep(100);
    }

    while (this.currentlyOpenScreen == 0 || this.containerInventory == null) {
      System.out.println("sending open packet");
      this.client.send(
          new ServerboundUseItemOnPacket(
              Vector3i.from(x, y, z),
              Direction.DOWN,
              Hand.MAIN_HAND,
              0.5f,
              0.5f,
              0.5f,
              false,
              false,
              0));

      synchronized (this) {
        this.wait(1000);
      }
    }
    System.out.println("inv: window opened");
  }

  public ItemStack[] getContainerInventory() {
    return this.containerInventory;
  }

  public ItemStack[] getPlayerInventory() {
    return this.playerInventory;
  }

  public void transferItems(int playerSlot, int invSlot, int count, boolean toChest)
      throws Exception {
    if (this.containerInventory == null) {
      throw new Exception("unable to transfer items, container inventory not present");
    }

    if (this.playerInventory == null) {
      throw new Exception("unable to transfer items, player inventory not present");
    }

    if (this.currentlyOpenScreen == 0) {
      throw new Exception("unable to transfer items, no currently open screen");
    }

    if (playerSlot < 0 || playerSlot >= 36) {
      throw new Exception("unable to transfer items, invalid player slot: " + playerSlot);
    }

    if (invSlot < 0 || invSlot >= 27) {
      throw new Exception("unable to transfer items, invalid inv slot");
    }

    // TODO: allow moving stackable items onto each other
    // maybe check for max stack size but we don't have info about that here
    if (toChest && this.playerInventory[playerSlot] == null) {
      throw new Exception("unable to transfer items to chest, player slot empty");
    }

    if (toChest
        && count != Integer.MAX_VALUE
        && this.playerInventory[playerSlot].getAmount() < count) {
      throw new Exception("unable to transfer items to chest, player slot has less than count");
    }

    if (!toChest && this.containerInventory[invSlot] == null) {
      throw new Exception("unable to transfer items to player, chest slot empty");
    }

    if (!toChest
        && count != Integer.MAX_VALUE
        && this.containerInventory[invSlot].getAmount() < count) {
      throw new Exception("unable to transfer items to chest, chest slot has less than count");
    }

    ItemStack itemToMove;

    if (toChest) {
      itemToMove = this.playerInventory[playerSlot];
    } else {
      itemToMove = this.containerInventory[invSlot];
    }

    int rawPlayerSlot = playerSlot + 27;
    int toSlotRaw = toChest ? invSlot : rawPlayerSlot;
    int fromSlotRaw = toChest ? rawPlayerSlot : invSlot;

    // Pickup whole stack
    HashMap<Integer, ItemStack> changedSlotsFrom = new HashMap();
    changedSlotsFrom.put(fromSlotRaw, null);
    this.client.send(
        new ServerboundContainerClickPacket(
            this.currentlyOpenScreen,
            this.stateId,
            fromSlotRaw,
            ContainerActionType.CLICK_ITEM,
            ClickItemAction.LEFT_CLICK,
            itemToMove,
            changedSlotsFrom));

    if (toChest) {
      this.playerInventory[playerSlot] = null;
    } else {
      this.containerInventory[playerSlot] = null;
    }

    ItemStack heldItem = itemToMove;

    // Calculate actual count to transfer (Integer.MAX_VALUE means entire stack)
    int actualCount = (count == Integer.MAX_VALUE) ? itemToMove.getAmount() : count;

    // Drop until we are holding the desired count
    int toDropCount = itemToMove.getAmount() - actualCount;
    for (int i = 1; i <= toDropCount; i++) {
      heldItem =
          new ItemStack(
              heldItem.getId(), heldItem.getAmount() - 1, heldItem.getDataComponentsPatch());
      ItemStack slotItem = new ItemStack(heldItem.getId(), i, heldItem.getDataComponentsPatch());
      changedSlotsFrom.put(fromSlotRaw, slotItem);

      this.client.send(
          new ServerboundContainerClickPacket(
              this.currentlyOpenScreen,
              this.stateId,
              fromSlotRaw,
              ContainerActionType.CLICK_ITEM,
              ClickItemAction.RIGHT_CLICK,
              heldItem,
              changedSlotsFrom));

      if (toChest) {
        this.playerInventory[playerSlot] = slotItem;
      } else {
        this.containerInventory[invSlot] = slotItem;
      }
    }

    // Drop in new space
    ItemStack oldDestinationItem;
    if (toChest) {
      oldDestinationItem = this.containerInventory[invSlot];
    } else {
      oldDestinationItem = this.playerInventory[playerSlot];
    }

    ItemStack destinationItem =
        new ItemStack(
            heldItem.getId(),
            heldItem.getAmount()
                + (oldDestinationItem == null ? 0 : oldDestinationItem.getAmount()),
            heldItem.getDataComponentsPatch());

    HashMap<Integer, ItemStack> changedSlotsTo = new HashMap();
    changedSlotsTo.put(toSlotRaw, destinationItem);
    this.client.send(
        new ServerboundContainerClickPacket(
            this.currentlyOpenScreen,
            this.stateId,
            fromSlotRaw,
            ContainerActionType.CLICK_ITEM,
            ClickItemAction.LEFT_CLICK,
            null,
            changedSlotsTo));

    if (toChest) {
      this.containerInventory[invSlot] = destinationItem;
    } else {
      this.playerInventory[playerSlot] = destinationItem;
    }
  }

  public void dropItems(int playerSlot) throws Exception {
    if (this.currentlyOpenScreen != 0) {
      throw new Exception("unable to drop items, in inventory");
    }

    if (playerSlot < 0 || playerSlot >= 36) {
      throw new Exception("unable to transfer items, invalid player slot: " + playerSlot);
    }

    if (this.playerInventory == null) {
      throw new Exception("unable to drop items, player inventory not present");
    }

    if (this.playerInventory[playerSlot] == null) {
      throw new Exception("unable to drop items, slot empty");
    }

    ItemStack itemToDrop = this.playerInventory[playerSlot];
    int rawSlot = playerSlot + 9;

    // Drop whole stack
    HashMap<Integer, ItemStack> changedSlots = new HashMap();
    changedSlots.put(rawSlot, null);
    this.client.send(
        new ServerboundContainerClickPacket(
            this.currentlyOpenScreen,
            this.stateId,
            rawSlot,
            ContainerActionType.DROP_ITEM,
            DropItemAction.DROP_SELECTED_STACK,
            null,
            changedSlots));
  }
}
