package com.guguild.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public class GuiHolder implements InventoryHolder {
    private Inventory inventory;
    public GuiType type;
    public int page;
    public int guildId;
    public UUID targetUuid;
    public String titleText;
    public ConfirmAction confirmAction;

    public GuiHolder(GuiType type, int page, int guildId, UUID targetUuid, String titleText, ConfirmAction confirmAction) {
        this.type = type;
        this.page = page;
        this.guildId = guildId;
        this.targetUuid = targetUuid;
        this.titleText = titleText;
        this.confirmAction = confirmAction;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }
}
