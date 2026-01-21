package com.banaffi.lbanamnesty.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class BlacklistGuiHolder implements InventoryHolder {
    @Override
    public Inventory getInventory() {
        return null; // Not used; Bukkit requires implementation.
    }
}

