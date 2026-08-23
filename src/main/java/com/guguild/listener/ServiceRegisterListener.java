package com.guguild.listener;

import com.guguild.GuGuildPlugin;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServiceRegisterEvent;

public class ServiceRegisterListener implements Listener {
    private final GuGuildPlugin plugin;

    public ServiceRegisterListener(GuGuildPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onServiceRegister(ServiceRegisterEvent event) {
        if (!(event.getProvider() instanceof Economy)) {
            return;
        }
        if (!plugin.getEconomyService().isReady()) {
            plugin.getEconomyService().setup();
        }
    }
}
