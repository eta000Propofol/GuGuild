package com.guguild.service;

import cn.handyplus.title.api.PlayerTitleApi;
import cn.handyplus.title.api.param.TitleListParam;
import cn.handyplus.title.api.param.TitleRequireParam;
import cn.handyplus.title.constants.BuyTypeEnum;
import cn.handyplus.title.constants.TitlePositionEnum;
import cn.handyplus.title.entity.TitlePlayer;
import com.guguild.GuGuildPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TitleService {
    private final GuGuildPlugin plugin;

    public TitleService(GuGuildPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isAvailable() {
        return Bukkit.getPluginManager().getPlugin("PlayerTitle") != null;
    }

    public String getPluginVersion() {
        if (!isAvailable()) {
            return null;
        }
        Plugin titlePlugin = Bukkit.getPluginManager().getPlugin("PlayerTitle");
        return titlePlugin == null ? null : titlePlugin.getDescription().getVersion();
    }

    public int createHiddenTitle(String titleText) {
        if (!isAvailable()) {
            return -1;
        }
        try {
            TitleRequireParam require = TitleRequireParam.builder()
                    .amount(0)
                    .buyType(BuyTypeEnum.ACTIVITY)
                    .build();

            TitleListParam param = new TitleListParam();
            param.setTitleName(titleText);
            param.setDescription("GuGuild 公会称号");
            param.setDay(0);
            param.setIsHide(1);
            param.setPosition(TitlePositionEnum.FRONT);
            param.setRequires(Collections.singletonList(require));
            param.setBuffs(null);
            param.setParticle(null);
            param.setIsPrefixAndSuffix(false);

            Integer id = PlayerTitleApi.addTitle(param);
            return id == null ? -1 : id;
        } catch (Throwable t) {
            plugin.getLogger().warning("创建 PlayerTitle 称号失败(" + (t.getClass() == null ? "unknown" : t.getClass().getName())
                    + "): " + (t.getMessage() == null ? t.toString() : t.getMessage()));
            return -1;
        }
    }

    public boolean hasTitle(UUID uuid, int titleId) {
        if (!isAvailable() || titleId <= 0) {
            return false;
        }
        try {
            return PlayerTitleApi.playerExistTitleId(uuid, titleId);
        } catch (Throwable t) {
            plugin.getLogger().warning("检查 PlayerTitle 称号失败: " + t.getMessage());
            return false;
        }
    }

    public void updateTitleName(int titleId, String titleText) {
        if (!isAvailable() || titleId <= 0) {
            return;
        }
        try {
            PlayerTitleApi.updateTitleName(titleId, titleText, false);
        } catch (Throwable t) {
            plugin.getLogger().warning("更新 PlayerTitle 称号名称失败: " + t.getMessage());
        }
    }

    /**
     * 授予公会称号。
     * PlayerTitle 里按“永久”写入（其内部 days 传 0 等于 36500 天），
     * 真实的一个月期限由 GuGuild 自己的 title_expire_at 管理。
     * 这样 PlayerTitle 每 60 秒的到期检查不会把公会称号摘掉。
     */
    public void grant(UUID uuid, String playerName, int titleId) {
        if (!isAvailable() || titleId <= 0) {
            return;
        }
        try {
            PlayerTitleApi.setPlayerTitle(playerName, uuid, titleId, 0);
        } catch (Throwable t) {
            plugin.getLogger().warning("授予 PlayerTitle 称号失败: " + t.getMessage());
        }
    }

    public void equip(Player player, int titleId) {
        if (!isAvailable() || titleId <= 0) {
            return;
        }
        try {
            List<TitlePlayer> titles = PlayerTitleApi.findPlayerTitle(player.getUniqueId());
            if (titles == null) {
                return;
            }
            for (TitlePlayer title : titles) {
                if (title.getTitleId() != null && title.getTitleId() == titleId) {
                    PlayerTitleApi.playerUseTitle(player.getUniqueId(), title.getId());
                    break;
                }
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("装备 PlayerTitle 称号失败: " + t.getMessage());
        }
    }

    public void grantAndEquip(Player player, UUID uuid, String playerName, int titleId) {
        grant(uuid, playerName, titleId);
        equip(player, titleId);
    }

    public void remove(UUID uuid, int titleId) {
        if (!isAvailable() || titleId <= 0) {
            return;
        }
        try {
            PlayerTitleApi.removePlayerTitle(uuid, titleId);
        } catch (Throwable t) {
            plugin.getLogger().warning("移除 PlayerTitle 称号失败: " + t.getMessage());
        }
        // 移除后刷新在线玩家的称号缓存，否则聊天前缀会继续显示已删除的称号
        refreshCache(Bukkit.getPlayer(uuid));
    }

    /**
     * 读取玩家当前“正在展示”的称号 id（直接读 PlayerTitle 的内存缓存）。
     * 返回 -1 表示缓存里没有正在展示的称号。
     */
    public int getShownTitleId(UUID uuid) {
        if (!isAvailable() || uuid == null) {
            return -1;
        }
        try {
            Map<?, ?> showMap = showCacheMap();
            if (showMap == null) {
                return -1;
            }
            Object cached = showMap.get(uuid);
            if (cached == null) {
                return -1;
            }
            Object id = cached.getClass().getMethod("getTitleId").invoke(cached);
            return id instanceof Integer ? (Integer) id : -1;
        } catch (Throwable t) {
            plugin.getLogger().warning("读取 PlayerTitle 称号缓存失败: " + t.getMessage());
            return -1;
        }
    }

    private Map<?, ?> showCacheMap() throws Throwable {
        Class<?> clazz = Class.forName("cn.handyplus.title.constants.TitleConstants");
        Object value = clazz.getField("TITLE_PLAYER_SHOW_MAP").get(null);
        return value instanceof Map<?, ?> map ? map : null;
    }

    /**
     * 刷新玩家当前的称号缓存。removePlayerTitle 只会删除数据库记录，
     * 已装备的称号仍缓存在 PlayerTitle 内存中，需要调用其内部 setCache 刷新。
     */
    private void refreshCache(Player player) {
        if (player == null || !isAvailable()) {
            return;
        }
        try {
            Class<?> clazz = Class.forName("cn.handyplus.title.util.TitleUtil");
            clazz.getMethod("setCache", org.bukkit.entity.Player.class).invoke(null, player);
        } catch (Throwable t) {
            plugin.getLogger().warning("刷新 PlayerTitle 称号缓存失败: " + t.getMessage());
        }
    }
}
