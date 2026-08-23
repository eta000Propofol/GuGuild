package com.guguild.listener;

import com.guguild.GuGuildPlugin;
import com.guguild.gui.ConfirmAction;
import com.guguild.gui.GuiFactory;
import com.guguild.gui.GuiHolder;
import com.guguild.gui.GuiType;
import com.guguild.service.GuildService;
import com.guguild.service.PendingInputService;
import com.guguild.util.ItemUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class GuiListener implements Listener {
    private final GuGuildPlugin plugin;

    public GuiListener(GuGuildPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!(event.getInventory().getHolder() instanceof GuiHolder holder)) {
            return;
        }
        event.setCancelled(true);
        ItemStack current = event.getCurrentItem();
        if (current == null) {
            return;
        }
        String action = ItemUtil.getAction(current);
        if (action == null) {
            return;
        }
        GuildService service = plugin.getGuildService();
        PendingInputService pending = plugin.getPendingInputService();

        switch (holder.type) {
            case GUILD_LIST:
                handleGuildList(player, holder, current, action, service, pending);
                break;
            case GUILD_HOME:
                handleGuildHome(player, holder, action, service, pending);
                break;
            case MEMBER_LIST:
                handleMemberList(player, holder, action, service, pending);
                break;
            case SETTINGS:
                handleSettings(player, holder, action, service);
                break;
            case TITLE_MANAGE:
                handleTitle(player, holder, action, pending);
                break;
            case CONFIRM:
                handleConfirm(player, holder, action, service);
                break;
            default:
                break;
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof GuiHolder) {
            event.setCancelled(true);
        }
    }

    private void handleGuildList(Player player, GuiHolder holder, ItemStack current, String action, GuildService service, PendingInputService pending) {
        switch (action) {
            case "prev_page":
                GuiFactory.openGuildList(player, holder.page - 1);
                break;
            case "next_page":
                GuiFactory.openGuildList(player, holder.page + 1);
                break;
            case "refresh":
                GuiFactory.openGuildList(player, holder.page);
                break;
            case "create_guild":
                player.closeInventory();
                pending.put(player.getUniqueId(), PendingInputService.Action.CREATE_GUILD);
                player.sendMessage(com.guguild.util.ColorUtil.colorize("&8[&6公会&8] &f请在聊天栏输入公会名，支持 & 颜色码；输入 &ft&f 取消。"));
                break;
            case "join_guild":
                int guildId = ItemUtil.getGuildId(current);
                if (guildId > 0) {
                    service.joinFree(player, guildId);
                    if (service.getGuild(player.getUniqueId()) != null) {
                        GuiFactory.openGuildHome(player, guildId);
                    } else {
                        GuiFactory.openGuildList(player, holder.page);
                    }
                }
                break;
            default:
                break;
        }
    }

    private void handleGuildHome(Player player, GuiHolder holder, String action, GuildService service, PendingInputService pending) {
        switch (action) {
            case "back_list":
                GuiFactory.openGuildList(player, 0);
                break;
            case "sign":
                service.sign(player);
                GuiFactory.openGuildHome(player, holder.guildId);
                break;
            case "open_members":
                GuiFactory.openMemberList(player, holder.guildId, 0);
                break;
            case "invite_player":
                player.closeInventory();
                pending.put(player.getUniqueId(), PendingInputService.Action.INVITE_PLAYER);
                player.sendMessage(com.guguild.util.ColorUtil.colorize("&8[&6公会&8] &f请在聊天栏输入要邀请的玩家名；输入 &ft&f 取消。"));
                break;
            case "set_notice":
                player.closeInventory();
                pending.put(player.getUniqueId(), PendingInputService.Action.SET_NOTICE);
                player.sendMessage(com.guguild.util.ColorUtil.colorize("&8[&6公会&8] &f请在聊天栏输入新公告；输入 &ft&f 取消。"));
                break;
            case "title_manage":
                GuiFactory.openTitleGui(player, holder.guildId);
                break;
            case "open_settings":
                GuiFactory.openSettings(player, holder.guildId);
                break;
            case "leave_guild":
                GuiFactory.openConfirm(player, holder.guildId, ConfirmAction.LEAVE, null, null);
                break;
            case "refresh":
                GuiFactory.openGuildHome(player, holder.guildId);
                break;
            default:
                break;
        }
    }

    private void handleMemberList(Player player, GuiHolder holder, String action, GuildService service, PendingInputService pending) {
        switch (action) {
            case "prev_page":
                GuiFactory.openMemberList(player, holder.guildId, holder.page - 1);
                break;
            case "next_page":
                GuiFactory.openMemberList(player, holder.guildId, holder.page + 1);
                break;
            case "back_home":
                GuiFactory.openGuildHome(player, holder.guildId);
                break;
            case "invite_player":
                player.closeInventory();
                pending.put(player.getUniqueId(), PendingInputService.Action.INVITE_PLAYER);
                player.sendMessage(com.guguild.util.ColorUtil.colorize("&8[&6公会&8] &f请在聊天栏输入要邀请的玩家名；输入 &ft&f 取消。"));
                break;
            default:
                break;
        }
    }

    private void handleSettings(Player player, GuiHolder holder, String action, GuildService service) {
        switch (action) {
            case "toggle_join":
                service.toggleJoinType(player);
                GuiFactory.openSettings(player, holder.guildId);
                break;
            case "upgrade_limit":
                GuiFactory.openConfirm(player, holder.guildId, ConfirmAction.UPGRADE_LIMIT, null, null);
                break;
            case "set_icon":
                player.sendMessage(com.guguild.util.ColorUtil.colorize("&8[&6公会&8] &f请手持物品后输入 &f/guild seticon"));
                break;
            case "set_vice":
                player.sendMessage(com.guguild.util.ColorUtil.colorize("&8[&6公会&8] &f请使用 &f/guild setvice <玩家>"));
                break;
            case "kick_hint":
                player.sendMessage(com.guguild.util.ColorUtil.colorize("&8[&6公会&8] &f请使用 &f/guild kick <玩家>"));
                break;
            case "disband":
                GuiFactory.openConfirm(player, holder.guildId, ConfirmAction.DISBAND, null, null);
                break;
            case "back_home":
                GuiFactory.openGuildHome(player, holder.guildId);
                break;
            default:
                break;
        }
    }

    private void handleTitle(Player player, GuiHolder holder, String action, PendingInputService pending) {
        switch (action) {
            case "title_buy":
                player.closeInventory();
                pending.put(player.getUniqueId(), PendingInputService.Action.TITLE_TEXT);
                player.sendMessage(com.guguild.util.ColorUtil.colorize("&8[&6公会&8] &f请在聊天栏输入公会称号文字，支持 & 颜色码；输入 &ft&f 取消。"));
                break;
            case "back_home":
                GuiFactory.openGuildHome(player, holder.guildId);
                break;
            default:
                break;
        }
    }

    private void handleConfirm(Player player, GuiHolder holder, String action, GuildService service) {
        if ("cancel".equals(action)) {
            if (service.getGuild(player.getUniqueId()) != null) {
                GuiFactory.openGuildHome(player, holder.guildId);
            } else {
                GuiFactory.openGuildList(player, 0);
            }
            return;
        }
        if (!"confirm".equals(action)) {
            return;
        }

        switch (holder.confirmAction) {
            case DISBAND:
                service.performDisband(player);
                GuiFactory.openGuildList(player, 0);
                break;
            case LEAVE:
                service.performLeave(player);
                GuiFactory.openGuildList(player, 0);
                break;
            case KICK:
                if (holder.targetUuid != null) {
                    service.performKick(player, holder.targetUuid);
                }
                GuiFactory.openGuildHome(player, holder.guildId);
                break;
            case TRANSFER:
                if (holder.targetUuid != null) {
                    service.performTransfer(player, holder.targetUuid);
                }
                GuiFactory.openGuildHome(player, holder.guildId);
                break;
            case UPGRADE_LIMIT:
                service.upgradeMemberLimit(player);
                GuiFactory.openGuildHome(player, holder.guildId);
                break;
            case TITLE_BUY:
                service.purchaseOrRenewTitle(player, holder.titleText);
                GuiFactory.openTitleGui(player, holder.guildId);
                break;
            default:
                break;
        }
    }
}
