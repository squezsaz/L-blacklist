package com.banaffi.lbanamnesty.commands;

import com.banaffi.lbanamnesty.gui.BlacklistGuiManager;
import com.banaffi.lbanamnesty.lang.MessageManager;
import com.banaffi.lbanamnesty.sounds.SoundManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
public final class BlacklistCommand implements CommandExecutor {

    private final BlacklistGuiManager guiManager;
    private final SoundManager soundManager;
    private final MessageManager messages;
    private final JavaPlugin plugin;

    public BlacklistCommand(BlacklistGuiManager guiManager, SoundManager soundManager, MessageManager messages, JavaPlugin plugin) {
        this.guiManager = guiManager;
        this.soundManager = soundManager;
        this.messages = messages;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("blacklist.admin")) {
                sender.sendMessage(messages.get("noPermission"));
                return true;
            }
            plugin.reloadConfig();
            messages.reload(plugin.getConfig().getString("language", "tr"));
            sender.sendMessage(messages.get("reloadSuccess"));
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.get("onlyPlayers"));
            return true;
        }
        if (!player.hasPermission("blacklist.admin")) {
            player.sendMessage(messages.get("noPermission"));
            soundManager.playError(player);
            return true;
        }

        guiManager.open(player, 0, "");
        soundManager.playOpenGui(player);
        return true;
    }
}

