package com.banaffi.lbanamnesty;

import com.banaffi.lbanamnesty.blacklist.BlacklistStore;
import com.banaffi.lbanamnesty.commands.BlacklistCommand;
import com.banaffi.lbanamnesty.gui.BlacklistGuiListener;
import com.banaffi.lbanamnesty.gui.BlacklistGuiManager;
import com.banaffi.lbanamnesty.guard.BlacklistBanGuard;
import com.banaffi.lbanamnesty.lang.MessageManager;
import com.banaffi.lbanamnesty.sounds.SoundManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class LBanAmnestyPlugin extends JavaPlugin {

    private SoundManager soundManager;
    private BlacklistStore blacklistStore;
    private BlacklistGuiManager blacklistGuiManager;
    private MessageManager messageManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.messageManager = new MessageManager(this, getConfig().getString("language", "tr"));
        this.soundManager = new SoundManager(this);
        this.blacklistStore = new BlacklistStore(this);
        this.blacklistGuiManager = new BlacklistGuiManager(this, blacklistStore, soundManager, messageManager);

        getCommand("blacklist").setExecutor(new BlacklistCommand(blacklistGuiManager, soundManager, messageManager, this));

        getServer().getPluginManager().registerEvents(new BlacklistGuiListener(blacklistGuiManager, blacklistStore, soundManager, messageManager), this);
        getServer().getPluginManager().registerEvents(blacklistGuiManager, this);
        getServer().getPluginManager().registerEvents(new BlacklistBanGuard(blacklistStore, messageManager), this);

        getLogger().info("LBanAmnesty enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("LBanAmnesty disabled.");
    }
}

