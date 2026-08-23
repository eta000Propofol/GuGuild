package com.guguild.listener;

import com.guguild.GuGuildPlugin;
import com.guguild.gui.ConfirmAction;
import com.guguild.model.GuildMember;
import com.guguild.gui.GuiFactory;
import com.guguild.service.GuildService;
import com.guguild.service.PendingInputService;
import com.guguild.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatInputListener implements Listener {
    private final GuGuildPlugin plugin;

    public ChatInputListener(GuGuildPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        PendingInputService.Action action = plugin.getPendingInputService().take(player.getUniqueId());
        if (action == null) {
            return;
        }
        event.setCancelled(true);
        String message = event.getMessage().trim();
        if ("t".equalsIgnoreCase(message)) {
            Bukkit.getScheduler().runTask(plugin, () ->
                    player.sendMessage(ColorUtil.colorize("&8[&6公会&8] &f已取消当前操作。")));
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> handle(player, action, message));
    }

    private void handle(Player player, PendingInputService.Action action, String message) {
        GuildService service = plugin.getGuildService();
        switch (action) {
            case CREATE_GUILD:
                service.createGuild(player, message);
                if (service.getGuild(player.getUniqueId()) != null) {
                    GuildMember m = service.getMember(player.getUniqueId());
                    if (m != null) {
                        GuiFactory.openGuildHome(player, m.guildId);
                    }
                }
                break;
            case SET_NOTICE:
                service.setNotice(player, message);
                break;
            case TITLE_TEXT:
                if (service.getMember(player.getUniqueId()) == null) {
                    break;
                }
                GuiFactory.openConfirm(player, service.getMember(player.getUniqueId()).guildId, ConfirmAction.TITLE_BUY, null, message);
                break;
            case INVITE_PLAYER:
                service.invite(player, message);
                break;
            default:
                break;
        }
    }
}
