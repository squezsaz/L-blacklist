package com.banaffi.lbanamnesty.gui;

import com.banaffi.lbanamnesty.blacklist.BlacklistEntry;
import com.banaffi.lbanamnesty.blacklist.BlacklistStore;
import com.banaffi.lbanamnesty.lang.MessageManager;
import com.banaffi.lbanamnesty.sounds.SoundManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class BlacklistGuiManager implements Listener {
    private static final int PAGE_SIZE = 45;

    private final JavaPlugin plugin;
    private final BlacklistStore store;
    private final SoundManager sounds;
    private final MessageManager messages;
    private final Map<UUID, GuiSession> sessions = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> confirmTargets = new ConcurrentHashMap<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public BlacklistGuiManager(JavaPlugin plugin, BlacklistStore store, SoundManager sounds, MessageManager messages) {
        this.plugin = plugin;
        this.store = store;
        this.sounds = sounds;
        this.messages = messages;
    }

    public void open(Player player, int page, String search) {
        GuiSession session = sessions.computeIfAbsent(player.getUniqueId(), k -> new GuiSession());
        session.page = Math.max(0, page);
        session.search = search != null ? search : "";
        session.awaitingAdd = false;
        session.awaitingSearch = false;
        buildAndOpen(player, session);
    }

    public boolean isOurInventory(Inventory inv) {
        if (inv == null) return false;
        if (inv.getHolder() instanceof BlacklistGuiHolder) return true;
        if (inv.getHolder() instanceof ConfirmRemovalHolder) return true;
        return false;
    }

    public boolean isConfirmInventory(Inventory inv) {
        return inv != null && inv.getHolder() instanceof ConfirmRemovalHolder;
    }

    public Optional<GuiSession> getSession(UUID uuid) {
        return Optional.ofNullable(sessions.get(uuid));
    }

    public void close(Player player) {
        sessions.remove(player.getUniqueId());
    }

    public void changePage(Player player, int delta) {
        getSession(player.getUniqueId()).ifPresent(session -> {
            int pages = totalPages(session);
            session.page = Math.max(0, Math.min(pages - 1, session.page + delta));
            buildAndOpen(player, session);
            sounds.playPageChange(player);
        });
    }

    public void refresh(Player player) {
        getSession(player.getUniqueId()).ifPresent(session -> buildAndOpen(player, session));
    }

    public void clearSearch(Player player) {
        getSession(player.getUniqueId()).ifPresent(session -> {
            session.search = "";
            session.page = 0;
            buildAndOpen(player, session);
        });
    }

    public void promptSearch(Player player) {
        GuiSession session = sessions.computeIfAbsent(player.getUniqueId(), k -> new GuiSession());
        session.awaitingSearch = true;
        session.awaitingAdd = false;
        player.closeInventory();
        player.sendMessage(messages.get("searchPrompt"));
    }

    public void promptAdd(Player player) {
        GuiSession session = sessions.computeIfAbsent(player.getUniqueId(), k -> new GuiSession());
        session.awaitingAdd = true;
        session.awaitingSearch = false;
        player.closeInventory();
        player.sendMessage(messages.get("addPrompt"));
    }

    public void removeEntry(Player player, BlacklistEntry entry) {
        store.remove(entry.uuid());
        player.sendMessage(messages.get("removedFromBlacklist").replace("%player%", entry.name()));
        sounds.playRemoveBlacklist(player);
        unbanPlayer(entry.name());
        refresh(player);
    }

    public void addEntry(Player player, String input) {
        String[] parts = input.trim().split("\\s+", 2);
        if (parts.length == 0 || parts[0].isBlank()) {
            player.sendMessage(messages.get("mustProvideName"));
            sounds.playError(player);
            return;
        }
        String name = parts[0];
        String reason = parts.length > 1 ? parts[1] : "No reason";

        OfflinePlayer off = Bukkit.getOfflinePlayer(name);
        if (off != null && off.isOp()) {
            player.sendMessage(messages.get("cannotBlacklistOp"));
            sounds.playError(player);
            return;
        }

        BlacklistEntry created = store.addByName(name, reason, player.getName());
        String msg = messages.get("blacklistedPlayer")
                .replace("%player%", created.name())
                .replace("%reason%", reason);
        player.sendMessage(msg);
        sounds.playAddBlacklist(player);
        banPlayer(created.name(), reason);
        refresh(player);
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        GuiSession session = sessions.get(player.getUniqueId());
        if (session == null) return;

        if (session.awaitingSearch) {
            event.setCancelled(true);
            String msg = event.getMessage();
            Bukkit.getScheduler().runTask(plugin, () -> {
                if ("clear".equalsIgnoreCase(msg.trim())) {
                    clearSearch(player);
                } else {
                    session.search = msg.trim();
                    session.page = 0;
                    buildAndOpen(player, session);
                }
                session.awaitingSearch = false;
            });
        } else if (session.awaitingAdd) {
            event.setCancelled(true);
            String msg = event.getMessage();
            Bukkit.getScheduler().runTask(plugin, () -> {
                addEntry(player, msg);
                session.awaitingAdd = false;
            });
        }
    }

    private void buildAndOpen(Player player, GuiSession session) {
        List<BlacklistEntry> filtered = store.searchByName(session.search);
        int pages = totalPages(filtered);
        session.page = Math.max(0, Math.min(pages - 1, session.page));
        int start = session.page * PAGE_SIZE;
        int end = Math.min(filtered.size(), start + PAGE_SIZE);
        List<BlacklistEntry> pageEntries = filtered.subList(start, end);

        String title = messages.get("guiTitle")
                .replace("%page%", String.valueOf(session.page + 1))
                .replace("%pages%", String.valueOf(pages));
        Inventory inv = Bukkit.createInventory(new BlacklistGuiHolder(), 54, title);

        for (int i = 0; i < pageEntries.size(); i++) {
            inv.setItem(i, toSkull(pageEntries.get(i)));
        }

        // Buttons row
        inv.setItem(45, button(Material.LIME_WOOL, messages.get("btnAdd")));
        inv.setItem(46, button(Material.COMPASS, messages.get("btnSearch")));
        inv.setItem(47, button(Material.ARROW, messages.get("btnPrev")));
        inv.setItem(48, button(Material.ARROW, messages.get("btnNext")));
        inv.setItem(49, button(Material.SUNFLOWER, messages.get("btnRefresh")));
        inv.setItem(50, button(Material.PAPER, messages.get("btnPage")
                .replace("%page%", String.valueOf(session.page + 1))
                .replace("%pages%", String.valueOf(pages))));
        inv.setItem(53, button(Material.BARRIER, messages.get("btnClose")));

        player.openInventory(inv);
    }

    private int totalPages(GuiSession session) {
        return totalPages(store.searchByName(session.search));
    }

    private int totalPages(List<BlacklistEntry> entries) {
        return Math.max(1, (entries.size() + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    private ItemStack button(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack toSkull(BlacklistEntry entry) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        OfflinePlayer off = Bukkit.getOfflinePlayer(entry.uuid());
        meta.setOwningPlayer(off);
        meta.setDisplayName(ChatColor.RED + entry.name());
        List<String> lore = new ArrayList<>();
        lore.add(messages.get("infoReason").replace("%reason%", entry.reason()));
        lore.add(messages.get("infoAddedBy").replace("%addedBy%", entry.addedBy()));
        lore.add(ChatColor.GRAY + dateFormat.format(new Date(entry.addedAt())));
        lore.add(messages.get("guiLoreHint"));
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        skull.setItemMeta(meta);
        return skull;
    }

    private void banPlayer(String name, String reason) {
        // Run both AdvancedBan and vanilla ban commands; harmless if already banned.
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "advancedban:ban " + name + " " + reason);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ban " + name + " " + reason);
    }

    private void unbanPlayer(String name) {
        // Run both AdvancedBan and vanilla unban commands; harmless if not banned.
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "advancedban:unban " + name);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "pardon " + name);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "unban " + name);
    }

    public void openConfirmRemoval(Player player, BlacklistEntry entry) {
        confirmTargets.put(player.getUniqueId(), entry.uuid());
        String title = messages.get("confirmTitle").replace("%player%", entry.name());
        Inventory inv = Bukkit.createInventory(new ConfirmRemovalHolder(), 27, title);
        inv.setItem(11, button(Material.LIME_WOOL, messages.get("confirmYes")));
        inv.setItem(15, button(Material.RED_WOOL, messages.get("confirmNo")));
        player.openInventory(inv);
    }

    public Optional<BlacklistEntry> getConfirmTarget(Player player) {
        UUID uuid = confirmTargets.get(player.getUniqueId());
        if (uuid == null) return Optional.empty();
        return store.get(uuid);
    }

    public void clearConfirm(Player player) {
        confirmTargets.remove(player.getUniqueId());
    }

    public static final class GuiSession {
        int page = 0;
        String search = "";
        boolean awaitingSearch = false;
        boolean awaitingAdd = false;
    }
}

