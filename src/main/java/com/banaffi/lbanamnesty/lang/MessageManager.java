package com.banaffi.lbanamnesty.lang;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class MessageManager {

    private final JavaPlugin plugin;
    private FileConfiguration messages;
    private String language;

    public MessageManager(JavaPlugin plugin, String languageCode) {
        this.plugin = plugin;
        this.language = languageCode == null ? "tr" : languageCode.toLowerCase();
        this.messages = loadMessages();
    }

    private FileConfiguration loadMessages() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    public void reload(String languageCode) {
        this.language = languageCode == null ? "tr" : languageCode.toLowerCase();
        this.messages = loadMessages();
    }

    public String get(String key) {
        String path = "messages." + key + "." + language;
        String fallbackPath = "messages." + key + ".tr";
        String msg = messages.getString(path, messages.getString(fallbackPath, key));
        if (msg == null) return key;
        return msg.replace("&", "§");
    }
}


