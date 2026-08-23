package com.guguild.gui;

import com.guguild.GuGuildPlugin;
import com.guguild.model.Guild;
import com.guguild.model.GuildMember;
import com.guguild.model.JoinType;
import com.guguild.model.Role;
import com.guguild.util.ColorUtil;
import com.guguild.util.ItemUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public final class GuiFactory {
    private static final int PAGE_SIZE = 45;

    private GuiFactory() {
    }

    private static Component title(String text) {
        return LegacyComponentSerializer.legacySection().deserialize(ColorUtil.colorize(text));
    }

    private static ItemStack glass() {
        return ItemUtil.createItem(Material.GRAY_STAINED_GLASS_PANE, " ", null, null);
    }

    private static void fillGlass(Inventory inv) {
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, glass());
        }
    }

    private static ItemStack button(Material material, String name, String action, String... lore) {
        List<String> lines = new ArrayList<>();
        for (String line : lore) {
            lines.add(line);
        }
        return ItemUtil.createItem(material, name, action, lines);
    }

    private static ItemStack info(Material material, String name, String... lore) {
        List<String> lines = new ArrayList<>();
        for (String line : lore) {
            lines.add(line);
        }
        return ItemUtil.createItem(material, name, null, lines);
    }

    public static void openGuildList(Player player, int page) {
        GuGuildPlugin plugin = GuGuildPlugin.getInstance();
        List<Guild> guilds = plugin.getGuildService().listGuilds();
        int totalPages = Math.max(1, (int) Math.ceil(guilds.size() / (double) PAGE_SIZE));
        if (page < 0) {
            page = 0;
        }
        if (page >= totalPages) {
            page = totalPages - 1;
        }

        GuiHolder holder = new GuiHolder(GuiType.GUILD_LIST, page, 0, null, null, null);
        Inventory inv = Bukkit.createInventory(holder, 54, title("公会列表"));
        holder.setInventory(inv);
        fillGlass(inv);

        int start = page * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE; i++) {
            int index = start + i;
            if (index >= guilds.size()) {
                break;
            }
            inv.setItem(i, buildGuildItem(guilds.get(index)));
        }

        if (page > 0) {
            inv.setItem(45, button(Material.ARROW, "&a上一页", "prev_page"));
        }
        inv.setItem(47, button(Material.ENDER_PEARL, "&b刷新", "refresh"));
        inv.setItem(49, info(Material.PAPER, "&f第 " + (page + 1) + " / " + totalPages + " 页"));
        if (plugin.getGuildService().getGuild(player.getUniqueId()) == null) {
            inv.setItem(51, button(Material.EMERALD, "&a创建公会", "create_guild", "&7点击后在聊天栏输入公会名"));
        }
        if (page < totalPages - 1) {
            inv.setItem(53, button(Material.ARROW, "&a下一页", "next_page"));
        }
        player.openInventory(inv);
    }

    private static ItemStack buildGuildItem(Guild guild) {
        ItemStack item = guild.iconBase64 == null || guild.iconBase64.isEmpty()
                ? ItemUtil.defaultGuildIcon()
                : ItemUtil.iconFromBase64(guild.iconBase64);
        if (item == null) {
            item = ItemUtil.defaultGuildIcon();
        }
        item = item.clone();
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.colorize(guild.displayName));
            List<String> lore = new ArrayList<>();
            lore.add(ColorUtil.colorize("&7活跃值: &e" + guild.activeValue));
            lore.add(ColorUtil.colorize("&7成员: &e" + guild.memberCount + "&7/" + guild.memberLimit));
            lore.add(ColorUtil.colorize("&7加入方式: &e" + (JoinType.FREE.equals(guild.joinType) ? "自由加入" : "仅邀请")));
            String titleStatus = guild.titleExpireAt != null && guild.titleExpireAt > System.currentTimeMillis()
                    ? "&a有效"
                    : "&7无";
            lore.add(ColorUtil.colorize("&7公会称号: " + titleStatus));
            lore.add(ColorUtil.colorize(JoinType.FREE.equals(guild.joinType) ? "&e点击加入" : "&c仅邀请"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        ItemUtil.setGuildId(item, guild.id);
        ItemUtil.setAction(item, "join_guild");
        return item;
    }

    public static void openGuildHome(Player player, int guildId) {
        GuGuildPlugin plugin = GuGuildPlugin.getInstance();
        Guild guild = plugin.getGuildService().getGuildById(guildId);
        if (guild == null) {
            openGuildList(player, 0);
            return;
        }
        GuildMember member = plugin.getGuildService().getMember(player.getUniqueId());
        if (member == null || member.guildId != guildId) {
            openGuildList(player, 0);
            return;
        }

        GuiHolder holder = new GuiHolder(GuiType.GUILD_HOME, 0, guildId, null, null, null);
        Inventory inv = Bukkit.createInventory(holder, 54, title(guild.displayName));
        holder.setInventory(inv);
        fillGlass(inv);

        ItemStack icon = guild.iconBase64 == null || guild.iconBase64.isEmpty()
                ? ItemUtil.defaultGuildIcon()
                : ItemUtil.iconFromBase64(guild.iconBase64);
        if (icon == null) {
            icon = ItemUtil.defaultGuildIcon();
        }
        inv.setItem(0, icon);
        inv.setItem(1, info(Material.NAME_TAG, guild.displayName, "&7公会名称"));
        inv.setItem(2, info(Material.NETHER_STAR, "&e活跃值: " + guild.activeValue, "&7成员每日签到可增加"));
        inv.setItem(3, info(Material.IRON_DOOR, "&b加入方式: " + (JoinType.FREE.equals(guild.joinType) ? "自由加入" : "仅邀请")));
        inv.setItem(4, info(Material.PLAYER_HEAD, "&b成员: " + plugin.getGuildService().countMembers(guildId) + " / " + guild.memberLimit));
        String titleStatus = guild.titleExpireAt == null ? "&7无"
                : guild.titleExpireAt > System.currentTimeMillis() ? "&a" + guild.titleText + " &7(至 " + new SimpleDateFormat("yyyy-MM-dd").format(new Date(guild.titleExpireAt)) + ")"
                : "&c已过期";
        inv.setItem(5, info(Material.EMERALD, titleStatus, "&7公会称号"));
        inv.setItem(6, info(Material.BOOK, guild.notice == null || guild.notice.isEmpty() ? "&7暂无公告" : guild.notice, "&7公会公告"));
        inv.setItem(7, info(Material.PAPER, "&f" + member.name, "&7你的职位: " + roleName(member.role)));
        inv.setItem(8, info(Material.OAK_SIGN, "&7帮助", "&f/guild help"));

        inv.setItem(9, button(Material.EXPERIENCE_BOTTLE, "&a每日签到", "sign", "&7公会活跃值 +" + plugin.getConfigManager().getSigninPoints()));
        inv.setItem(11, button(Material.PLAYER_HEAD, "&a成员列表", "open_members", "&7查看成员签到情况"));
        inv.setItem(13, button(Material.WRITABLE_BOOK, "&a邀请玩家", "invite_player", "&7点击后在聊天栏输入玩家名"));
        if (Role.LEADER.equals(member.role) || Role.VICE.equals(member.role)) {
            inv.setItem(15, button(Material.BOOK, "&a修改公告", "set_notice", "&7点击后在聊天栏输入公告"));
        }
        if (Role.LEADER.equals(member.role)) {
            inv.setItem(17, button(Material.EMERALD, "&a公会称号", "title_manage", "&7购买/续费/查看"));
        }

        List<GuildMember> members = plugin.getGuildService().listMembers(guildId);
        for (int i = 0; i < Math.min(27, members.size()); i++) {
            GuildMember m = members.get(i);
            OfflinePlayer offline = Bukkit.getOfflinePlayer(UUID.fromString(m.uuid));
            List<String> lore = new ArrayList<>();
            lore.add("&7职位: " + roleName(m.role));
            lore.add("&7今日: " + (isSignedToday(m) ? "&a已签到" : "&7未签到"));
            lore.add("&7累计签到: " + m.totalSignins + " 次");
            inv.setItem(18 + i, ItemUtil.createSkull(offline, "&f" + m.name, lore));
        }

        inv.setItem(45, button(Material.ARROW, "&a返回公会列表", "back_list"));
        inv.setItem(47, button(Material.EXPERIENCE_BOTTLE, "&a每日签到", "sign"));
        inv.setItem(49, info(Material.PAPER, "&f公会主页"));
        if (Role.LEADER.equals(member.role) || Role.VICE.equals(member.role)) {
            inv.setItem(51, button(Material.ANVIL, "&a公会设置", "open_settings"));
        } else {
            inv.setItem(51, button(Material.BARRIER, "&c退出公会", "leave_guild", "&7退出后需要重新加入"));
        }
        inv.setItem(53, button(Material.PLAYER_HEAD, "&a成员列表", "open_members"));
        player.openInventory(inv);
    }

    public static void openMemberList(Player player, int guildId, int page) {
        GuGuildPlugin plugin = GuGuildPlugin.getInstance();
        Guild guild = plugin.getGuildService().getGuildById(guildId);
        if (guild == null) {
            openGuildList(player, 0);
            return;
        }
        GuildMember member = plugin.getGuildService().getMember(player.getUniqueId());
        if (member == null || member.guildId != guildId) {
            openGuildList(player, 0);
            return;
        }
        List<GuildMember> members = plugin.getGuildService().listMembers(guildId);
        int totalPages = Math.max(1, (int) Math.ceil(members.size() / (double) PAGE_SIZE));
        if (page < 0) {
            page = 0;
        }
        if (page >= totalPages) {
            page = totalPages - 1;
        }

        GuiHolder holder = new GuiHolder(GuiType.MEMBER_LIST, page, guildId, null, null, null);
        Inventory inv = Bukkit.createInventory(holder, 54, title("公会成员"));
        holder.setInventory(inv);
        fillGlass(inv);

        int start = page * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE; i++) {
            int index = start + i;
            if (index >= members.size()) {
                break;
            }
            GuildMember m = members.get(index);
            OfflinePlayer offline = Bukkit.getOfflinePlayer(UUID.fromString(m.uuid));
            List<String> lore = new ArrayList<>();
            lore.add("&7职位: " + roleName(m.role));
            lore.add("&7今日: " + (isSignedToday(m) ? "&a已签到" : "&7未签到"));
            lore.add("&7累计签到: " + m.totalSignins + " 次");
            ItemStack skull = ItemUtil.createSkull(offline, "&f" + m.name, lore);
            ItemUtil.setMemberUuid(skull, m.uuid);
            inv.setItem(i, skull);
        }

        if (page > 0) {
            inv.setItem(45, button(Material.ARROW, "&a上一页", "prev_page"));
        }
        inv.setItem(47, button(Material.ARROW, "&a返回主页", "back_home"));
        inv.setItem(49, info(Material.PAPER, "&f第 " + (page + 1) + " / " + totalPages + " 页"));
        if (Role.LEADER.equals(member.role) || Role.VICE.equals(member.role)) {
            inv.setItem(51, button(Material.WRITABLE_BOOK, "&a邀请玩家", "invite_player"));
        }
        if (page < totalPages - 1) {
            inv.setItem(53, button(Material.ARROW, "&a下一页", "next_page"));
        }
        player.openInventory(inv);
    }

    public static void openSettings(Player player, int guildId) {
        GuGuildPlugin plugin = GuGuildPlugin.getInstance();
        Guild guild = plugin.getGuildService().getGuildById(guildId);
        if (guild == null) {
            openGuildList(player, 0);
            return;
        }
        GuildMember member = plugin.getGuildService().getMember(player.getUniqueId());
        if (member == null || member.guildId != guildId) {
            openGuildList(player, 0);
            return;
        }

        GuiHolder holder = new GuiHolder(GuiType.SETTINGS, 0, guildId, null, null, null);
        Inventory inv = Bukkit.createInventory(holder, 54, title("公会设置"));
        holder.setInventory(inv);
        fillGlass(inv);

        if (Role.LEADER.equals(member.role)) {
            inv.setItem(10, button(Material.IRON_DOOR, "&a切换加入方式", "toggle_join", "&7当前: " + (JoinType.FREE.equals(guild.joinType) ? "自由加入" : "仅邀请")));
        }
        if (Role.LEADER.equals(member.role)) {
            inv.setItem(12, button(Material.GOLD_INGOT, "&a设置图标", "set_icon", "&7手持物品后使用 /guild seticon"));
        }
        if (Role.LEADER.equals(member.role) || Role.VICE.equals(member.role)) {
            inv.setItem(14, button(Material.BARREL, "&a扩大会员上限", "upgrade_limit", "&7当前 " + guild.memberLimit + " 人", "&7消耗 " + plugin.getConfigManager().getMemberSlotCost() + " 活跃值 +1 上限", "&7最高 " + plugin.getConfigManager().getMemberLimitMax() + " 人"));
        }
        if (Role.LEADER.equals(member.role)) {
            inv.setItem(16, button(Material.PLAYER_HEAD, "&a任免副会长", "set_vice", "&7使用 /guild setvice <玩家>"));
        }
        if (Role.LEADER.equals(member.role) || Role.VICE.equals(member.role)) {
            inv.setItem(22, button(Material.BARRIER, "&c踢出成员", "kick_hint", "&7使用 /guild kick <玩家>"));
        }
        if (Role.LEADER.equals(member.role)) {
            inv.setItem(24, button(Material.DIAMOND, "&c解散公会", "disband"));
        }

        inv.setItem(45, button(Material.ARROW, "&a返回主页", "back_home"));
        inv.setItem(49, info(Material.PAPER, "&f公会设置"));
        if (Role.LEADER.equals(member.role)) {
            inv.setItem(53, button(Material.BARRIER, "&c解散公会", "disband"));
        }
        player.openInventory(inv);
    }

    public static void openTitleGui(Player player, int guildId) {
        GuGuildPlugin plugin = GuGuildPlugin.getInstance();
        Guild guild = plugin.getGuildService().getGuildById(guildId);
        if (guild == null) {
            openGuildList(player, 0);
            return;
        }
        GuildMember member = plugin.getGuildService().getMember(player.getUniqueId());
        if (member == null || member.guildId != guildId || !Role.LEADER.equals(member.role)) {
            openGuildHome(player, guildId);
            return;
        }

        GuiHolder holder = new GuiHolder(GuiType.TITLE_MANAGE, 0, guildId, null, null, null);
        Inventory inv = Bukkit.createInventory(holder, 54, title("公会称号"));
        holder.setInventory(inv);
        fillGlass(inv);

        if (guild.titleExpireAt == null || guild.titleExpireAt <= System.currentTimeMillis()) {
            inv.setItem(13, info(Material.BARRIER, "&c当前无公会称号"));
        } else {
            inv.setItem(13, info(Material.EMERALD, guild.titleText, "&7到期时间: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(guild.titleExpireAt))));
        }
        inv.setItem(31, button(Material.EMERALD, "&a购买/续费公会称号", "title_buy",
                "&7价格: " + plugin.getConfigManager().getTitleCost() + " " + plugin.getConfigManager().getCurrencyName(),
                "&7期限: " + plugin.getConfigManager().getTitleDurationDays() + " 天",
                "&7点击后在聊天栏输入称号文字"));
        inv.setItem(45, button(Material.ARROW, "&a返回主页", "back_home"));
        inv.setItem(49, info(Material.PAPER, "&f公会称号"));
        player.openInventory(inv);
    }

    public static void openConfirm(Player player, int guildId, ConfirmAction action, UUID targetUuid, String titleText) {
        GuGuildPlugin plugin = GuGuildPlugin.getInstance();
        String description;
        String confirmName;
        switch (action) {
            case DISBAND:
                description = "&c确认要解散公会吗？此操作不可恢复。";
                confirmName = "&c确认解散";
                break;
            case LEAVE:
                description = "&c确认要退出当前公会吗？";
                confirmName = "&c确认退出";
                break;
            case KICK:
                description = "&c确认要踢出该成员吗？";
                confirmName = "&c确认踢出";
                break;
            case TRANSFER:
                description = "&c确认要转让会长吗？";
                confirmName = "&c确认转让";
                break;
            case UPGRADE_LIMIT:
                description = "&a确认消耗 " + plugin.getConfigManager().getMemberSlotCost() + " 活跃值扩容 1 个位置吗？";
                confirmName = "&a确认扩容";
                break;
            case TITLE_BUY:
                description = "&a确认花费 " + plugin.getConfigManager().getTitleCost() + " " + plugin.getConfigManager().getCurrencyName() + " 购买/续费称号吗？";
                confirmName = "&a确认购买";
                break;
            default:
                return;
        }

        GuiHolder holder = new GuiHolder(GuiType.CONFIRM, 0, guildId, targetUuid, titleText, action);
        Inventory inv = Bukkit.createInventory(holder, 54, title("确认操作"));
        holder.setInventory(inv);
        fillGlass(inv);
        inv.setItem(22, info(Material.PAPER, description));
        inv.setItem(45, button(Material.BARRIER, "&c取消", "cancel"));
        inv.setItem(49, info(Material.PAPER, "&f确认操作"));
        inv.setItem(53, button(Material.EMERALD, confirmName, "confirm"));
        player.openInventory(inv);
    }

    private static String roleName(String role) {
        if (Role.LEADER.equals(role)) {
            return "&c会长";
        }
        if (Role.VICE.equals(role)) {
            return "&e副会长";
        }
        return "&7成员";
    }

    private static boolean isSignedToday(GuildMember member) {
        if (member.lastSigninDate == null) {
            return false;
        }
        GuGuildPlugin plugin = GuGuildPlugin.getInstance();
        java.time.ZoneId zone = plugin.getConfigManager().getZoneId();
        return member.lastSigninDate.equals(java.time.LocalDate.now(zone).toString());
    }
}

