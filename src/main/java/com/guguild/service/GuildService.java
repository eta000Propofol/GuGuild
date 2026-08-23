package com.guguild.service;

import com.guguild.GuGuildPlugin;
import com.guguild.config.GuildConfig;
import com.guguild.db.Database;
import com.guguild.model.Guild;
import com.guguild.model.GuildInvite;
import com.guguild.model.GuildMember;
import com.guguild.model.JoinType;
import com.guguild.model.Role;
import com.guguild.util.ColorUtil;
import com.guguild.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

public class GuildService {
    private final GuGuildPlugin plugin;
    private final Database database;
    private final GuildConfig config;
    private final EconomyService economy;
    private final TitleService title;

    public GuildService(GuGuildPlugin plugin, Database database, GuildConfig config, EconomyService economy, TitleService title) {
        this.plugin = plugin;
        this.database = database;
        this.config = config;
        this.economy = economy;
        this.title = title;
    }

    public Guild getGuild(UUID uuid) {
        return database.findGuildByPlayer(uuid);
    }

    public GuildMember getMember(UUID uuid) {
        return database.findMember(uuid);
    }

    public Guild getGuildById(int id) {
        return database.findGuildById(id);
    }

    public List<Guild> listGuilds() {
        return database.listGuilds();
    }

    public int countMembers(int guildId) {
        return database.countMembers(guildId);
    }

    public List<GuildMember> listMembers(int guildId) {
        return database.listMembers(guildId);
    }

    public void createGuild(Player player, String rawName) {
        if (getGuild(player.getUniqueId()) != null) {
            msg(player, "&c你已经在一个公会中，请先退出。");
            return;
        }
        String displayName = ColorUtil.colorize(rawName.trim());
        String normalized = ColorUtil.normalizeName(rawName);
        if (normalized.isEmpty()) {
            msg(player, "&c公会名不能为空。");
            return;
        }
        if (ColorUtil.visibleLength(rawName) > config.getGuildNameMaxLength()) {
            msg(player, "&c公会名去颜色后不能超过 " + config.getGuildNameMaxLength() + " 个字符。");
            return;
        }
        if (database.findGuildByName(normalized) != null) {
            msg(player, "&c已存在同名公会。");
            return;
        }
        double cost = config.getCreateCost();
        if (!economy.isReady()) {
            msg(player, "&c经济系统不可用，请联系管理员。");
            return;
        }
        if (!economy.has(player, cost)) {
            msg(player, "&c你的余额不足，创建公会需要 " + config.getCurrencyName() + " x" + cost + "。");
            return;
        }
        if (!economy.withdraw(player, cost)) {
            msg(player, "&c扣款失败，请稍后再试。");
            return;
        }

        ItemStack hand = player.getInventory().getItemInMainHand();
        String iconBase64 = null;
        if (hand != null && hand.getType() != Material.AIR) {
            iconBase64 = ItemUtil.iconToBase64(hand.clone());
        }

        Guild guild = new Guild();
        guild.name = normalized;
        guild.displayName = displayName;
        guild.leaderUuid = player.getUniqueId().toString();
        guild.iconBase64 = iconBase64;
        guild.activeValue = 0;
        guild.joinType = JoinType.INVITE;
        guild.memberLimit = config.getMemberLimitBase();
        guild.notice = "";
        guild.createdAt = System.currentTimeMillis();
        int guildId = database.insertGuild(guild);
        if (guildId <= 0) {
            economy.deposit(player, cost);
            msg(player, "&c公会创建失败，已退还龙门币。");
            return;
        }

        GuildMember leader = new GuildMember();
        leader.guildId = guildId;
        leader.uuid = player.getUniqueId().toString();
        leader.name = player.getName();
        leader.role = Role.LEADER;
        leader.joinedAt = System.currentTimeMillis();
        database.insertMember(leader);
        msg(player, "&a公会 " + displayName + " &a创建成功！");
    }

    public void joinFree(Player player, int guildId) {
        if (getGuild(player.getUniqueId()) != null) {
            msg(player, "&c你已经在一个公会中，请先退出。");
            return;
        }
        Guild guild = database.findGuildById(guildId);
        if (guild == null) {
            msg(player, "&c公会不存在。");
            return;
        }
        if (!JoinType.FREE.equals(guild.joinType)) {
            msg(player, "&c该公会仅限邀请加入。");
            return;
        }
        if (database.countMembers(guildId) >= guild.memberLimit) {
            msg(player, "&c该公会人数已满。");
            return;
        }
        addMember(guild, player.getUniqueId(), player.getName(), Role.MEMBER);
        msg(player, "&a你已加入公会 " + guild.displayName + " &a！");
        notifyGuild(guild, "&e玩家 &f" + player.getName() + " &e加入了公会。");
    }

    public void joinGuildByName(Player player, String nameInput) {
        Guild guild = database.findGuildByName(ColorUtil.normalizeName(nameInput));
        if (guild == null) {
            msg(player, "&c公会不存在。");
            return;
        }
        joinFree(player, guild.id);
    }

    public void invite(Player player, String targetName) {
        GuildMember member = getMember(player.getUniqueId());
        if (member == null) {
            msg(player, "&c你还没有公会。");
            return;
        }
        if (!isLeaderOrVice(member.role)) {
            msg(player, "&c只有会长或副会长可以邀请玩家。");
            return;
        }
        Guild guild = database.findGuildById(member.guildId);
        if (guild == null) {
            msg(player, "&c公会不存在。");
            return;
        }
        if (database.countMembers(guild.id) >= guild.memberLimit) {
            msg(player, "&c公会人数已满，无法邀请。");
            return;
        }
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            msg(player, "&c目标玩家不在线。");
            return;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            msg(player, "&c你不能邀请自己。");
            return;
        }
        if (database.findMember(target.getUniqueId()) != null) {
            msg(player, "&c目标玩家已经在其他公会中。");
            return;
        }
        long now = System.currentTimeMillis();
        database.deleteInvitesByTarget(target.getUniqueId());
        GuildInvite invite = new GuildInvite();
        invite.guildId = guild.id;
        invite.inviterUuid = player.getUniqueId().toString();
        invite.targetUuid = target.getUniqueId().toString();
        invite.targetName = target.getName();
        invite.createdAt = now;
        invite.expiresAt = now + config.getInviteExpireSeconds() * 1000L;
        database.insertInvite(invite);
        msg(player, "&a已向 " + target.getName() + " 发出邀请。");
        msg(target, "&e你收到公会 " + guild.displayName + " &e的邀请，使用 &f/guild accept " + guild.name + " &e接受。");
    }

    public void accept(Player player, String nameInput) {
        if (getGuild(player.getUniqueId()) != null) {
            msg(player, "&c你已经在一个公会中，请先退出。");
            return;
        }
        Guild guild = database.findGuildByName(ColorUtil.normalizeName(nameInput));
        if (guild == null) {
            msg(player, "&c公会不存在。");
            return;
        }
        GuildInvite matched = null;
        for (GuildInvite invite : database.findInvitesByTarget(player.getUniqueId())) {
            if (invite.guildId == guild.id) {
                matched = invite;
                break;
            }
        }
        if (matched == null) {
            msg(player, "&c你没有来自该公会的有效邀请。");
            return;
        }
        if (matched.expiresAt <= System.currentTimeMillis()) {
            database.deleteInvitesByTarget(player.getUniqueId());
            msg(player, "&c邀请已过期。");
            return;
        }
        if (database.countMembers(guild.id) >= guild.memberLimit) {
            msg(player, "&c该公会人数已满。");
            return;
        }
        database.deleteInvitesByTarget(player.getUniqueId());
        addMember(guild, player.getUniqueId(), player.getName(), Role.MEMBER);
        msg(player, "&a你已加入公会 " + guild.displayName + " &a！");
        notifyGuild(guild, "&e玩家 &f" + player.getName() + " &e加入了公会。");
    }

    public void decline(Player player, String nameInput) {
        Guild guild = database.findGuildByName(ColorUtil.normalizeName(nameInput));
        if (guild == null) {
            msg(player, "&c公会不存在。");
            return;
        }
        for (GuildInvite invite : database.findInvitesByTarget(player.getUniqueId())) {
            if (invite.guildId == guild.id) {
                database.deleteInvite(invite.id);
                msg(player, "&a你已拒绝该公会邀请。");
                return;
            }
        }
        msg(player, "&c你没有来自该公会的邀请。");
    }

    public void performLeave(Player player) {
        GuildMember member = getMember(player.getUniqueId());
        if (member == null) {
            msg(player, "&c你还没有公会。");
            return;
        }
        if (Role.LEADER.equals(member.role)) {
            msg(player, "&c会长不能直接退出，请先转让会长或解散公会。");
            return;
        }
        Guild guild = database.findGuildById(member.guildId);
        removeTitleIfNeeded(guild, player.getUniqueId());
        database.deleteMember(member.guildId, player.getUniqueId());
        msg(player, "&a你已退出公会。");
        if (guild != null) {
            notifyGuild(guild, "&e玩家 &f" + player.getName() + " &e退出了公会。");
        }
    }

    public void performDisband(Player player) {
        GuildMember member = getMember(player.getUniqueId());
        if (member == null) {
            msg(player, "&c你还没有公会。");
            return;
        }
        if (!Role.LEADER.equals(member.role)) {
            msg(player, "&c只有会长可以解散公会。");
            return;
        }
        Guild guild = database.findGuildById(member.guildId);
        if (guild == null) {
            msg(player, "&c公会不存在。");
            return;
        }
        if (title.isAvailable() && guild.titleId != null) {
            for (GuildMember m : database.listMembers(guild.id)) {
                title.remove(UUID.fromString(m.uuid), guild.titleId);
            }
        }
        database.deleteGuild(guild.id);
        msg(player, "&c公会已解散。");
    }

    public void performKick(Player player, UUID targetUuid) {
        GuildMember kicker = getMember(player.getUniqueId());
        if (kicker == null) {
            msg(player, "&c你还没有公会。");
            return;
        }
        if (!isLeaderOrVice(kicker.role)) {
            msg(player, "&c只有会长或副会长可以踢人。");
            return;
        }
        GuildMember target = database.findMember(targetUuid);
        if (target == null || target.guildId != kicker.guildId) {
            msg(player, "&c目标玩家不在你的公会。");
            return;
        }
        if (Role.LEADER.equals(target.role)) {
            msg(player, "&c不能踢出会长。");
            return;
        }
        if (Role.VICE.equals(kicker.role) && !Role.MEMBER.equals(target.role)) {
            msg(player, "&c副会长只能踢普通成员。");
            return;
        }
        Guild guild = database.findGuildById(kicker.guildId);
        removeTitleIfNeeded(guild, targetUuid);
        database.deleteMember(target.guildId, targetUuid);
        msg(player, "&a已踢出 " + target.name + "。");
        Player targetPlayer = Bukkit.getPlayer(targetUuid);
        if (targetPlayer != null) {
            msg(targetPlayer, "&c你已被踢出公会" + (guild == null ? "" : " " + guild.displayName) + "。");
        }
        if (guild != null) {
            notifyGuild(guild, "&e玩家 &f" + target.name + " &e被移出公会。");
        }
    }

    public void performTransfer(Player player, UUID targetUuid) {
        GuildMember leader = getMember(player.getUniqueId());
        if (leader == null) {
            msg(player, "&c你还没有公会。");
            return;
        }
        if (!Role.LEADER.equals(leader.role)) {
            msg(player, "&c只有会长可以转让会长。");
            return;
        }
        GuildMember target = database.findMember(targetUuid);
        if (target == null || target.guildId != leader.guildId) {
            msg(player, "&c目标玩家不在你的公会。");
            return;
        }
        if (target.uuid.equals(leader.uuid)) {
            msg(player, "&c你已经是会长。");
            return;
        }
        Guild guild = database.findGuildById(leader.guildId);
        database.updateLeader(leader.guildId, targetUuid.toString());
        database.updateMemberRole(leader.guildId, player.getUniqueId(), Role.MEMBER);
        database.updateMemberRole(leader.guildId, targetUuid, Role.LEADER);
        msg(player, "&a你已将会长转让给 " + target.name + "。");
        Player targetPlayer = Bukkit.getPlayer(targetUuid);
        if (targetPlayer != null) {
            msg(targetPlayer, "&a你已成为公会会长。");
        }
        if (guild != null) {
            notifyGuild(guild, "&e会长已变更为 &f" + target.name + "&e。");
        }
    }

    public void setVice(Player player, String targetName) {
        GuildMember leader = getMember(player.getUniqueId());
        if (leader == null) {
            msg(player, "&c你还没有公会。");
            return;
        }
        if (!Role.LEADER.equals(leader.role)) {
            msg(player, "&c只有会长可以设置副会长。");
            return;
        }
        GuildMember target = findMemberByName(leader.guildId, targetName);
        if (target == null) {
            msg(player, "&c目标玩家不在你的公会。");
            return;
        }
        if (Role.LEADER.equals(target.role)) {
            msg(player, "&c不能修改会长的职位。");
            return;
        }
        if (Role.VICE.equals(target.role)) {
            database.updateMemberRole(leader.guildId, UUID.fromString(target.uuid), Role.MEMBER);
            msg(player, "&a已取消 " + target.name + " 的副会长。");
        } else {
            database.updateMemberRole(leader.guildId, UUID.fromString(target.uuid), Role.VICE);
            msg(player, "&a已将 " + target.name + " 设为副会长。");
        }
    }

    public void setJoinType(Player player, String type) {
        GuildMember member = getMember(player.getUniqueId());
        if (member == null) {
            msg(player, "&c你还没有公会。");
            return;
        }
        if (!Role.LEADER.equals(member.role)) {
            msg(player, "&c只有会长可以调整加入方式。");
            return;
        }
        String joinType = type.equalsIgnoreCase("free") ? JoinType.FREE : JoinType.INVITE;
        database.updateJoinType(member.guildId, joinType);
        msg(player, "&a加入方式已切换为：" + (JoinType.FREE.equals(joinType) ? "自由加入" : "仅邀请") + "。");
    }

    public void toggleJoinType(Player player) {
        GuildMember member = getMember(player.getUniqueId());
        if (member == null) {
            msg(player, "&c你还没有公会。");
            return;
        }
        if (!Role.LEADER.equals(member.role)) {
            msg(player, "&c只有会长可以调整加入方式。");
            return;
        }
        Guild guild = database.findGuildById(member.guildId);
        if (guild == null) {
            return;
        }
        String next = JoinType.FREE.equals(guild.joinType) ? JoinType.INVITE : JoinType.FREE;
        database.updateJoinType(member.guildId, next);
        msg(player, "&a加入方式已切换为：" + (JoinType.FREE.equals(next) ? "自由加入" : "仅邀请") + "。");
    }

    public void upgradeMemberLimit(Player player) {
        GuildMember member = getMember(player.getUniqueId());
        if (member == null) {
            msg(player, "&c你还没有公会。");
            return;
        }
        if (!isLeaderOrVice(member.role)) {
            msg(player, "&c只有会长或副会长可以扩容。");
            return;
        }
        Guild guild = database.findGuildById(member.guildId);
        if (guild == null) {
            return;
        }
        if (guild.memberLimit >= config.getMemberLimitMax()) {
            msg(player, "&c公会人数上限已达到 " + config.getMemberLimitMax() + "。");
            return;
        }
        int cost = config.getMemberSlotCost();
        if (guild.activeValue < cost) {
            msg(player, "&c活跃值不足，扩容需要 " + cost + " 活跃值。");
            return;
        }
        if (!database.upgradeMemberLimit(guild.id, cost)) {
            msg(player, "&c扩容失败，请稍后再试。");
            return;
        }
        msg(player, "&a扩容成功，人数上限提升到 " + (guild.memberLimit + 1) + "。");
    }

    public void setIcon(Player player) {
        GuildMember member = getMember(player.getUniqueId());
        if (member == null) {
            msg(player, "&c你还没有公会。");
            return;
        }
        if (!Role.LEADER.equals(member.role)) {
            msg(player, "&c只有会长可以设置公会图标。");
            return;
        }
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() == Material.AIR) {
            msg(player, "&c请手持一个物品再设置图标。");
            return;
        }
        Guild guild = database.findGuildById(member.guildId);
        if (guild == null) {
            return;
        }
        boolean firstSet = guild.iconBase64 == null || guild.iconBase64.isEmpty();
        double cost = firstSet ? 0 : config.getIconChangeCost();
        if (cost > 0) {
            if (!economy.isReady()) {
                msg(player, "&c经济系统暂不可用，请稍后再试。");
                return;
            }
            if (!economy.has(player, cost)) {
                msg(player, "&c余额不足，修改图标需要 " + config.getCurrencyName() + " x" + cost + "。");
                return;
            }
            if (!economy.withdraw(player, cost)) {
                msg(player, "&c扣款失败，请稍后再试。");
                return;
            }
        }
        String base64 = ItemUtil.iconToBase64(hand.clone());
        if (base64 == null) {
            if (cost > 0) {
                economy.deposit(player, cost);
            }
            msg(player, "&c图标保存失败。");
            return;
        }
        database.updateIcon(guild.id, base64);
        msg(player, "&a公会图标已更新。");
    }

    public void sign(Player player) {
        GuildMember member = getMember(player.getUniqueId());
        if (member == null) {
            msg(player, "&c你还没有公会。");
            return;
        }
        String today = LocalDate.now(config.getZoneId()).toString();
        if (today.equals(member.lastSigninDate)) {
            msg(player, "&e你今天已经签到过了。");
            return;
        }
        int points = config.getSigninPoints();
        database.updateMemberSignin(member.guildId, player.getUniqueId(), today);
        database.addActiveValue(member.guildId, points);
        msg(player, "&a签到成功，公会活跃值 +" + points + "。");
    }

    public void setNotice(Player player, String rawNotice) {
        GuildMember member = getMember(player.getUniqueId());
        if (member == null) {
            msg(player, "&c你还没有公会。");
            return;
        }
        if (!isLeaderOrVice(member.role)) {
            msg(player, "&c只有会长或副会长可以设置公告。");
            return;
        }
        String notice = ColorUtil.colorize(rawNotice.trim());
        if (ColorUtil.visibleLength(notice) > config.getNoticeMaxLength()) {
            msg(player, "&c公告去颜色后不能超过 " + config.getNoticeMaxLength() + " 个字符。");
            return;
        }
        database.updateNotice(member.guildId, notice);
        msg(player, "&a公会公告已更新。");
    }

    public void purchaseOrRenewTitle(Player player, String rawText) {
        if (!title.isAvailable()) {
            msg(player, "&c服务器未安装 PlayerTitle 插件，无法购买公会称号。");
            return;
        }
        GuildMember member = getMember(player.getUniqueId());
        if (member == null) {
            msg(player, "&c你还没有公会。");
            return;
        }
        if (!Role.LEADER.equals(member.role)) {
            msg(player, "&c只有会长可以购买公会称号。");
            return;
        }
        Guild guild = database.findGuildById(member.guildId);
        if (guild == null) {
            return;
        }
        String titleText = ColorUtil.colorize(rawText.trim());
        if (titleText.isEmpty() || ColorUtil.visibleLength(titleText) > config.getTitleMaxLength()) {
            msg(player, "&c称号文字去颜色后不能为空且不能超过 " + config.getTitleMaxLength() + " 个字符。");
            return;
        }
        double cost = config.getTitleCost();
        if (!economy.isReady()) {
            msg(player, "&c经济系统暂不可用，请稍后再试。");
            return;
        }
        if (!economy.has(player, cost)) {
            msg(player, "&c余额不足，购买公会称号需要 " + config.getCurrencyName() + " x" + cost + "。");
            return;
        }
        if (!economy.withdraw(player, cost)) {
            msg(player, "&c扣款失败，请稍后再试。");
            return;
        }

        long now = System.currentTimeMillis();
        long expireAt;
        if (guild.titleId != null && guild.titleExpireAt != null && guild.titleExpireAt > now) {
            expireAt = guild.titleExpireAt + config.getTitleDurationDays() * 86400000L;
        } else {
            expireAt = now + config.getTitleDurationDays() * 86400000L;
        }

        int titleId = guild.titleId == null ? -1 : guild.titleId;
        if (titleId > 0) {
            title.updateTitleName(titleId, titleText);
        }
        if (titleId <= 0) {
            titleId = title.createHiddenTitle(titleText);
            if (titleId <= 0) {
                economy.deposit(player, cost);
                msg(player, "&c称号创建失败，已退还龙门币。");
                return;
            }
        }

        int remainingDays = Math.max(1, (int) Math.ceil((expireAt - now) / 86400000.0));
        for (GuildMember m : database.listMembers(guild.id)) {
            UUID uuid = UUID.fromString(m.uuid);
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                title.grantAndEquip(p, uuid, m.name, titleId, remainingDays);
            } else {
                title.grant(uuid, m.name, titleId, remainingDays);
            }
        }
        database.updateGuildTitle(guild.id, titleId, titleText, expireAt);
        msg(player, "&a公会称号购买/续费成功，全体成员已生效。");
    }

    public void applyTitleOnJoin(Player player) {
        Guild guild = getGuild(player.getUniqueId());
        if (guild == null || guild.titleId == null || guild.titleExpireAt == null || guild.titleExpireAt <= System.currentTimeMillis()) {
            return;
        }
        int remainingDays = remainingDays(guild);
        UUID uuid = player.getUniqueId();
        if (title.hasTitle(uuid, guild.titleId)) {
            title.equip(player, guild.titleId);
        } else {
            title.grantAndEquip(player, uuid, player.getName(), guild.titleId, remainingDays);
        }
    }

    public void checkExpiredTitles() {
        if (!title.isAvailable()) {
            return;
        }
        long now = System.currentTimeMillis();
        for (Guild guild : database.listGuilds()) {
            if (guild.titleId != null && guild.titleExpireAt != null && guild.titleExpireAt <= now) {
                for (GuildMember member : database.listMembers(guild.id)) {
                    title.remove(UUID.fromString(member.uuid), guild.titleId);
                }
                database.clearGuildTitle(guild.id);
                plugin.getLogger().info("公会 " + guild.name + " 的称号已到期并移除。");
            }
        }
    }

    public void adminDelete(String nameInput) {
        Guild guild = database.findGuildByName(ColorUtil.normalizeName(nameInput));
        if (guild == null) {
            return;
        }
        if (title.isAvailable() && guild.titleId != null) {
            for (GuildMember member : database.listMembers(guild.id)) {
                title.remove(UUID.fromString(member.uuid), guild.titleId);
            }
        }
        database.deleteGuild(guild.id);
    }

    public void adminGiveActive(String nameInput, int amount) {
        Guild guild = database.findGuildByName(ColorUtil.normalizeName(nameInput));
        if (guild == null) {
            return;
        }
        database.addActiveValue(guild.id, amount);
    }

    private void addMember(Guild guild, UUID uuid, String name, String role) {
        GuildMember member = new GuildMember();
        member.guildId = guild.id;
        member.uuid = uuid.toString();
        member.name = name;
        member.role = role;
        member.joinedAt = System.currentTimeMillis();
        database.insertMember(member);
        if (guild.titleId != null && guild.titleExpireAt != null && guild.titleExpireAt > System.currentTimeMillis()) {
            int days = remainingDays(guild);
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                title.grantAndEquip(p, uuid, name, guild.titleId, days);
            } else {
                title.grant(uuid, name, guild.titleId, days);
            }
        }
    }

    private void removeTitleIfNeeded(Guild guild, UUID uuid) {
        if (guild == null || guild.titleId == null) {
            return;
        }
        title.remove(uuid, guild.titleId);
    }

    private int remainingDays(Guild guild) {
        long now = System.currentTimeMillis();
        return Math.max(1, (int) Math.ceil((guild.titleExpireAt - now) / 86400000.0));
    }

    private GuildMember findMemberByName(int guildId, String name) {
        for (GuildMember member : database.listMembers(guildId)) {
            if (member.name.equalsIgnoreCase(name)) {
                return member;
            }
        }
        return null;
    }

    private boolean isLeaderOrVice(String role) {
        return Role.LEADER.equals(role) || Role.VICE.equals(role);
    }

    private void notifyGuild(Guild guild, String message) {
        for (GuildMember member : database.listMembers(guild.id)) {
            Player p = Bukkit.getPlayer(UUID.fromString(member.uuid));
            if (p != null) {
                msg(p, message);
            }
        }
    }

    private void msg(CommandSender sender, String text) {
        sender.sendMessage(ColorUtil.colorize("&8[&6公会&8] &f" + text));
    }
}


