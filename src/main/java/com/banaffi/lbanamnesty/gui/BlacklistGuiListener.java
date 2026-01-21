package com.banaffi.lbanamnesty.gui;

import com.banaffi.lbanamnesty.blacklist.BlacklistEntry;
import com.banaffi.lbanamnesty.blacklist.BlacklistStore;
import com.banaffi.lbanamnesty.lang.MessageManager;
import com.banaffi.lbanamnesty.sounds.SoundManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class BlacklistGuiListener implements Listener {
    private final BlacklistGuiManager manager;
    private final BlacklistStore store;
    private final SoundManager sounds;
    private final MessageManager messages;

    public BlacklistGuiListener(BlacklistGuiManager manager, BlacklistStore store, SoundManager sounds, MessageManager messages) {
        this.manager = manager;
        this.store = store;
        this.sounds = sounds;
        this.messages = messages;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory inv = event.getView().getTopInventory();
        if (!manager.isOurInventory(inv)) return;

        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inv.getSize()) return;

        // Confirmation GUI handling
        if (manager.isConfirmInventory(inv)) {
            handleConfirmClick(player, slot);
            return;
        }

        manager.getSession(player.getUniqueId()).ifPresent(session -> {
            List<BlacklistEntry> entries = store.searchByName(session.search);
            int index = session.page * 45 + slot;

            switch (slot) {
                case 45 -> manager.promptAdd(player);
                case 46 -> manager.promptSearch(player);
                case 47 -> manager.changePage(player, -1);
                case 48 -> manager.changePage(player, 1);
                case 49 -> manager.refresh(player);
                case 53 -> player.closeInventory();
                case 50 -> { /* page indicator; no-op */ }
                default -> {
                    if (slot < 45) {
                        handleEntryClick(player, event.getClick(), entries, index);
                    }
                }
            }
        });
    }

    private void handleEntryClick(Player player, ClickType click, List<BlacklistEntry> entries, int index) {
        if (index < 0 || index >= entries.size()) return;
        BlacklistEntry entry = entries.get(index);
        if (click.isShiftClick()) {
            manager.removeEntry(player, entry);
        } else if (click.isLeftClick()) {
            manager.openConfirmRemoval(player, entry);
        } else if (click.isRightClick()) {
            player.sendMessage("§c" + entry.name());
            player.sendMessage(messages.get("infoReason").replace("%reason%", entry.reason()));
            player.sendMessage(messages.get("infoAddedBy").replace("%addedBy%", entry.addedBy()));
            sounds.playOpenGui(player);
        }
    }

    private void handleConfirmClick(Player player, int slot) {
        if (slot == 11) {
            manager.getConfirmTarget(player).ifPresent(entry -> manager.removeEntry(player, entry));
            manager.clearConfirm(player);
            manager.getSession(player.getUniqueId()).ifPresent(session -> manager.open(player, session.page, session.search));
        } else if (slot == 15) {
            manager.clearConfirm(player);
            manager.getSession(player.getUniqueId()).ifPresent(session -> manager.open(player, session.page, session.search));
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!manager.isOurInventory(event.getView().getTopInventory())) return;
        manager.getSession(player.getUniqueId()).ifPresent(session -> {
            if (!session.awaitingAdd && !session.awaitingSearch) {
                manager.close(player);
            }
        });
    }
}

