package com.guguild.config;

import com.guguild.GuGuildPlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.time.ZoneId;

public class GuildConfig {
    private final GuGuildPlugin plugin;
    private FileConfiguration config;
    private ZoneId zoneId;

    public GuildConfig(GuGuildPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();
        this.zoneId = null;
    }

    private int getInt(String path, int def) {
        return config.getInt(path, def);
    }

    private String getString(String path, String def) {
        return config.getString(path, def);
    }

    public int getCreateCost() {
        return getInt("create-cost", 100000);
    }

    public int getIconChangeCost() {
        return getInt("icon-change-cost", 1000);
    }

    public int getTitleCost() {
        return getInt("title-cost", 10000);
    }

    public int getTitleDurationDays() {
        return getInt("title-duration-days", 30);
    }

    public int getSigninPoints() {
        return getInt("signin-points", 5);
    }

    public int getMemberLimitBase() {
        return getInt("member-limit-base", 5);
    }

    public int getMemberLimitMax() {
        return getInt("member-limit-max", 20);
    }

    public int getMemberSlotCost() {
        return getInt("member-slot-cost", 50);
    }

    public String getCurrencyName() {
        return getString("currency-name", "龙门币");
    }

    public int getGuildNameMaxLength() {
        return getInt("guild-name-max-length", 16);
    }

    public int getTitleMaxLength() {
        return getInt("title-max-length", 16);
    }

    public int getNoticeMaxLength() {
        return getInt("notice-max-length", 64);
    }

    public int getInviteExpireSeconds() {
        return getInt("invite-expire-seconds", 60);
    }

    public String getTimezone() {
        return getString("timezone", "Asia/Shanghai");
    }

    /**
     * 安全获取签到时区，配置无效时回退到服务器系统时区，避免因配置错误导致异常。
     */
    public ZoneId getZoneId() {
        if (zoneId == null) {
            try {
                zoneId = ZoneId.of(getTimezone());
            } catch (Exception e) {
                plugin.getLogger().warning("config.yml 的 timezone 配置无效（" + getTimezone() + "），已使用服务器系统时区。");
                zoneId = ZoneId.systemDefault();
            }
        }
        return zoneId;
    }
}

