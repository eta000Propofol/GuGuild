package com.guguild.command;

import com.guguild.GuGuildPlugin;
import com.guguild.gui.ConfirmAction;
import com.guguild.gui.GuiFactory;
import com.guguild.model.Guild;
import com.guguild.model.GuildMember;
import com.guguild.service.GuildService;
import com.guguild.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GuildCommand implements CommandExecutor, TabCompleter {
    private final GuGuildPlugin plugin;

    public GuildCommand(GuGuildPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        GuildService service = plugin.getGuildService();
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                msg(sender, "&c控制台请使用 /guild admin 命令。");
                return true;
            }
            Guild guild = service.getGuild(player.getUniqueId());
            if (guild == null) {
                GuiFactory.openGuildList(player, 0);
            } else {
                GuiFactory.openGuildHome(player, guild.id);
            }
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "help":
                sendHelp(sender);
                return true;
            case "top":
            case "list":
                return requirePlayer(sender, player -> GuiFactory.openGuildList(player, 0));
            case "create":
                return requirePlayer(sender, player -> service.createGuild(player, joinArgs(args, 1)));
            case "home":
            case "info":
                return requirePlayer(sender, player -> {
                    Guild guild = service.getGuild(player.getUniqueId());
                    if (guild == null) {
                        GuiFactory.openGuildList(player, 0);
                    } else {
                        GuiFactory.openGuildHome(player, guild.id);
                    }
                });
            case "members":
                return requirePlayer(sender, player -> {
                    Guild guild = service.getGuild(player.getUniqueId());
                    if (guild == null) {
                        msg(player, "&c你还没有公会。");
                    } else {
                        GuiFactory.openMemberList(player, guild.id, 0);
                    }
                });
            case "invite":
                return requirePlayer(sender, player -> service.invite(player, joinArgs(args, 1)));
            case "accept":
                return requirePlayer(sender, player -> service.accept(player, joinArgs(args, 1)));
            case "decline":
                return requirePlayer(sender, player -> service.decline(player, joinArgs(args, 1)));
            case "join":
                return requirePlayer(sender, player -> service.joinGuildByName(player, joinArgs(args, 1)));
            case "leave":
                return requirePlayer(sender, player -> {
                    Guild guild = service.getGuild(player.getUniqueId());
                    if (guild == null) {
                        msg(player, "&c你还没有公会。");
                    } else {
                        GuiFactory.openConfirm(player, guild.id, ConfirmAction.LEAVE, null, null);
                    }
                });
            case "disband":
                return requirePlayer(sender, player -> {
                    Guild guild = service.getGuild(player.getUniqueId());
                    if (guild == null) {
                        msg(player, "&c你还没有公会。");
                    } else {
                        GuiFactory.openConfirm(player, guild.id, ConfirmAction.DISBAND, null, null);
                    }
                });
            case "seticon":
                return requirePlayer(sender, player -> service.setIcon(player));
            case "sethome":
                return requirePlayer(sender, player -> service.setHome(player));
            case "tp":
            case "visit":
                if (args.length < 2) {
                    msg(sender, "&c用法: /guild tp <公会>");
                    return true;
                }
                return requirePlayer(sender, player -> service.teleportHome(player, joinArgs(args, 1)));
            case "setjointype":
                if (args.length < 2) {
                    msg(sender, "&c用法: /guild setjointype <invite|free>");
                    return true;
                }
                return requirePlayer(sender, player -> service.setJoinType(player, args[1]));
            case "setvice":
                if (args.length < 2) {
                    msg(sender, "&c用法: /guild setvice <玩家>");
                    return true;
                }
                return requirePlayer(sender, player -> service.setVice(player, args[1]));
            case "kick":
                if (args.length < 2) {
                    msg(sender, "&c用法: /guild kick <玩家>");
                    return true;
                }
                return requirePlayer(sender, player -> {
                    Guild guild = service.getGuild(player.getUniqueId());
                    GuildMember target = findMemberByName(service, guild, args[1]);
                    if (guild == null) {
                        msg(player, "&c你还没有公会。");
                    } else if (target == null) {
                        msg(player, "&c目标玩家不在你的公会。");
                    } else {
                        GuiFactory.openConfirm(player, guild.id, ConfirmAction.KICK, UUID.fromString(target.uuid), null);
                    }
                });
            case "transfer":
                if (args.length < 2) {
                    msg(sender, "&c用法: /guild transfer <玩家>");
                    return true;
                }
                return requirePlayer(sender, player -> {
                    Guild guild = service.getGuild(player.getUniqueId());
                    GuildMember target = findMemberByName(service, guild, args[1]);
                    if (guild == null) {
                        msg(player, "&c你还没有公会。");
                    } else if (target == null) {
                        msg(player, "&c目标玩家不在你的公会。");
                    } else {
                        GuiFactory.openConfirm(player, guild.id, ConfirmAction.TRANSFER, UUID.fromString(target.uuid), null);
                    }
                });
            case "sign":
                return requirePlayer(sender, player -> service.sign(player));
            case "notice":
                return requirePlayer(sender, player -> service.setNotice(player, joinArgs(args, 1)));
            case "title":
                return handleTitle(sender, args);
            case "admin":
                return handleAdmin(sender, args);
            default:
                sendHelp(sender);
                return true;
        }
    }

    private boolean handleTitle(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            msg(sender, "&c该命令只能由玩家执行。");
            return true;
        }
        GuildService service = plugin.getGuildService();
        Guild guild = service.getGuild(player.getUniqueId());
        if (guild == null) {
            msg(player, "&c你还没有公会。");
            return true;
        }
        if (args.length >= 2 && args[1].equalsIgnoreCase("buy")) {
            if (args.length < 3) {
                msg(player, "&c用法: /guild title buy <文字>");
                return true;
            }
            String text = joinArgs(args, 2);
            GuiFactory.openConfirm(player, guild.id, ConfirmAction.TITLE_BUY, null, text);
            return true;
        }
        GuiFactory.openTitleGui(player, guild.id);
        return true;
    }

    private boolean handleAdmin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("guild.admin")) {
            msg(sender, "&c你没有权限执行该命令。");
            return true;
        }
        if (args.length < 2) {
            msg(sender, "&c用法: /guild admin reload | delete <公会> | giveactive <公会> <数值>");
            return true;
        }
        GuildService service = plugin.getGuildService();
        switch (args[1].toLowerCase()) {
            case "reload":
                plugin.getConfigManager().reload();
                msg(sender, "&a配置已重载。");
                return true;
            case "delete":
                if (args.length < 3) {
                    msg(sender, "&c用法: /guild admin delete <公会>");
                    return true;
                }
                service.adminDelete(joinArgs(args, 2));
                msg(sender, "&a已尝试删除公会。");
                return true;
            case "giveactive":
                if (args.length < 4) {
                    msg(sender, "&c用法: /guild admin giveactive <公会> <数值>");
                    return true;
                }
                try {
                    int amount = Integer.parseInt(args[args.length - 1]);
                    String name = joinArgsRange(args, 2, args.length - 1);
                    service.adminGiveActive(name, amount);
                    msg(sender, "&a已调整活跃值。");
                } catch (NumberFormatException e) {
                    msg(sender, "&c数值格式错误。");
                }
                return true;
            default:
                msg(sender, "&c用法: /guild admin reload | delete <公会> | giveactive <公会> <数值>");
                return true;
        }
    }

    private boolean requirePlayer(CommandSender sender, java.util.function.Consumer<Player> action) {
        if (!(sender instanceof Player player)) {
            msg(sender, "&c该命令只能由玩家执行。");
            return true;
        }
        action.accept(player);
        return true;
    }

    private GuildMember findMemberByName(GuildService service, Guild guild, String name) {
        if (guild == null) {
            return null;
        }
        for (GuildMember member : service.listMembers(guild.id)) {
            if (member.name.equalsIgnoreCase(name)) {
                return member;
            }
        }
        return null;
    }

    private String joinArgs(String[] args, int from) {
        return joinArgsRange(args, from, args.length);
    }

    private String joinArgsRange(String[] args, int from, int toExclusive) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < toExclusive; i++) {
            if (i > from) {
                sb.append(' ');
            }
            sb.append(args[i]);
        }
        return sb.toString();
    }

    private void sendHelp(CommandSender sender) {
        msg(sender, "&6===== GuGuild 公会插件 =====");
        msg(sender, "&f/guild &7打开公会界面");
        msg(sender, "&f/guild create <名称> &7创建公会");
        msg(sender, "&f/guild top &7公会列表");
        msg(sender, "&f/guild sign &7每日签到");
        msg(sender, "&f/guild invite <玩家> &7邀请玩家");
        msg(sender, "&f/guild accept <公会> &7接受邀请");
        msg(sender, "&f/guild seticon &7用手持物品设置图标");
        msg(sender, "&f/guild sethome &7设置公会主城(会长, 2000龙门币)");
        msg(sender, "&f/guild tp <公会> &7传送到公会主城参观");
        msg(sender, "&f/guild setjointype <invite|free> &7切换加入方式");
        msg(sender, "&f/guild title buy <文字> &7购买公会称号");
        msg(sender, "&f/guild admin reload &7重载配置(管理员)");
    }

    private void msg(CommandSender sender, String text) {
        sender.sendMessage(ColorUtil.colorize("&8[&6公会&8] &f" + text));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> result = new ArrayList<>();
        if (args.length == 1) {
            String[] subs = {"help", "top", "list", "create", "home", "info", "members", "invite", "accept", "decline", "join", "leave", "disband", "seticon", "sethome", "tp", "visit", "setjointype", "setvice", "kick", "transfer", "sign", "notice", "title", "admin"};
            for (String s : subs) {
                if (s.startsWith(args[0].toLowerCase())) {
                    result.add(s);
                }
            }
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "setjointype":
                    result.add("invite");
                    result.add("free");
                    break;
                case "title":
                    result.add("buy");
                    break;
                case "admin":
                    result.add("reload");
                    result.add("delete");
                    result.add("giveactive");
                    break;
                default:
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                            result.add(p.getName());
                        }
                    }
                    break;
            }
        }
        return result;
    }
}


