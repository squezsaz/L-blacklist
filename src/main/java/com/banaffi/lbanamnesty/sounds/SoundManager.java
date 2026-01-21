package com.banaffi.lbanamnesty.sounds;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class SoundManager {
    private final JavaPlugin plugin;

    public SoundManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        plugin.reloadConfig();
    }

    public void playOpenGui(Player player) { play(player, "sounds.openGui"); }
    public void playAddBlacklist(Player player) { play(player, "sounds.addBlacklist"); }
    public void playRemoveBlacklist(Player player) { play(player, "sounds.removeBlacklist"); }
    public void playPageChange(Player player) { play(player, "sounds.pageChange"); }
    public void playError(Player player) { play(player, "sounds.error"); }
    public void playSuccess(Player player) { play(player, "sounds.success"); }

    private void play(Player player, String configPath) {
        String name = plugin.getConfig().getString(configPath, "");
        if (name == null || name.isBlank()) return;
        try {
            Sound sound = Sound.valueOf(name.toUpperCase());
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (IllegalArgumentException ignored) {
            // Invalid sound in config; ignore silently.
        }
    }
}

