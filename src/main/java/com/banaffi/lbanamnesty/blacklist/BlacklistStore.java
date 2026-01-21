package com.banaffi.lbanamnesty.blacklist;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.stream.Collectors;

public final class BlacklistStore {
    private static final String ROOT = "blacklist";

    private final JavaPlugin plugin;

    public BlacklistStore(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isBlacklisted(UUID uuid) {
        return plugin.getConfig().contains(ROOT + "." + uuid);
    }

    public Optional<BlacklistEntry> get(UUID uuid) {
        String path = ROOT + "." + uuid;
        if (!plugin.getConfig().contains(path)) return Optional.empty();
        String name = plugin.getConfig().getString(path + ".name", "Unknown");
        String reason = plugin.getConfig().getString(path + ".reason", "No reason");
        String addedBy = plugin.getConfig().getString(path + ".addedBy", "Unknown");
        long addedAt = plugin.getConfig().getLong(path + ".addedAt", 0L);
        return Optional.of(new BlacklistEntry(uuid, name, reason, addedBy, addedAt));
    }

    public List<BlacklistEntry> listAll() {
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection(ROOT);
        if (sec == null) return new ArrayList<>();
        List<BlacklistEntry> out = new ArrayList<>();
        for (String key : sec.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                get(uuid).ifPresent(out::add);
            } catch (IllegalArgumentException ignored) {
            }
        }
        out.sort(Comparator.comparing(BlacklistEntry::name, String.CASE_INSENSITIVE_ORDER));
        return out;
    }

    public List<BlacklistEntry> searchByName(String query) {
        if (query == null || query.isBlank()) return listAll();
        String q = query.toLowerCase(Locale.ROOT);
        return listAll().stream()
                .filter(e -> e.name() != null && e.name().toLowerCase(Locale.ROOT).contains(q))
                .collect(Collectors.toList());
    }

    public BlacklistEntry addByName(String name, String reason, String addedBy) {
        OfflinePlayer off = Bukkit.getOfflinePlayer(name);
        UUID uuid = off.getUniqueId();
        long now = System.currentTimeMillis();
        String path = ROOT + "." + uuid;
        plugin.getConfig().set(path + ".name", off.getName() != null ? off.getName() : name);
        plugin.getConfig().set(path + ".reason", reason != null && !reason.isBlank() ? reason : "No reason");
        plugin.getConfig().set(path + ".addedBy", addedBy != null ? addedBy : "Unknown");
        plugin.getConfig().set(path + ".addedAt", now);
        plugin.saveConfig();
        return new BlacklistEntry(uuid, off.getName() != null ? off.getName() : name, reason, addedBy, now);
    }

    public boolean remove(UUID uuid) {
        String path = ROOT + "." + uuid;
        if (!plugin.getConfig().contains(path)) return false;
        plugin.getConfig().set(path, null);
        plugin.saveConfig();
        return true;
    }
}

