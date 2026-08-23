package com.guguild.listener;

import com.guguild.GuGuildPlugin;
import com.guguild.util.ColorUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerJoinListener implements Listener {
    private final GuGuildPlugin plugin;

    public PlayerJoinListener(GuGuildPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getGuildService().applyTitleOnJoin(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // 防止遗留的“等待输入”状态在玩家下次上线后被误触发
        plugin.getPendingInputService().clear(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        // 等待输入期间如果玩家改输入指令，则取消原操作
        if (plugin.getPendingInputService().clear(event.getPlayer().getUniqueId())) {
            event.getPlayer().sendMessage(ColorUtil.colorize("&8[&6公会&8] &f已取消当前操作。"));
        }
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        // 等待输入期间如果玩家打开了其他界面，则取消原操作
        if (event.getPlayer() instanceof Player player) {
            if (plugin.getPendingInputService().clear(player.getUniqueId())) {
                player.sendMessage(ColorUtil.colorize("&8[&6公会&8] &f已取消当前操作。"));
            }
        }
    }
}
