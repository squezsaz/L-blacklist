package com.banaffi.lbanamnesty.guard;

import com.banaffi.lbanamnesty.blacklist.BlacklistStore;
import com.banaffi.lbanamnesty.lang.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;

import java.util.Locale;
import java.util.UUID;

public final class BlacklistBanGuard implements Listener {
    private final BlacklistStore store;
    private final MessageManager messages;

    public BlacklistBanGuard(BlacklistStore store, MessageManager messages) {
        this.store = store;
        this.messages = messages;
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        String msg = event.getMessage();
        if (shouldBlock(msg)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(messages.get("unbanBlocked"));
        }
    }

    @EventHandler
    public void onServerCommand(ServerCommandEvent event) {
        String cmd = event.getCommand();
        if (shouldBlock(cmd)) {
            event.setCancelled(true);
            event.getSender().sendMessage(messages.get("unbanBlocked"));
        }
    }

    private boolean shouldBlock(String raw) {
        if (raw == null || raw.isBlank()) return false;
        String lower = raw.toLowerCase(Locale.ROOT).trim();
        String[] parts = lower.split("\\s+");
        if (parts.length < 2) return false;

        String base = parts[0].startsWith("/") ? parts[0].substring(1) : parts[0];
        if (!(base.equals("pardon") || base.equals("unban") || base.equals("advancedban:unban"))) {
            return false;
        }

        String target = parts[1];
        OfflinePlayer off = Bukkit.getOfflinePlayer(target);
        UUID uuid = off.getUniqueId();
        return store.isBlacklisted(uuid);
    }
}

