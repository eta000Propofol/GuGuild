package com.guguild;

import com.guguild.command.GuildCommand;
import com.guguild.config.GuildConfig;
import com.guguild.db.Database;
import com.guguild.listener.ChatInputListener;
import com.guguild.listener.GuiListener;
import com.guguild.listener.PlayerJoinListener;
import com.guguild.listener.ServiceRegisterListener;
import com.guguild.service.EconomyService;
import com.guguild.service.GuildService;
import com.guguild.service.PendingInputService;
import com.guguild.service.TitleService;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class GuGuildPlugin extends JavaPlugin {
    private static GuGuildPlugin instance;
    private GuildConfig configManager;
    private Database database;
    private EconomyService economyService;
    private TitleService titleService;
    private GuildService guildService;
    private PendingInputService pendingInputService;

    @Override
    public void onEnable() {
        instance = this;
        configManager = new GuildConfig(this);
        configManager.reload();

        economyService = new EconomyService(this);
        if (!economyService.setup()) {
            if (!economyService.isVaultPresent()) {
                getLogger().severe("未找到 Vault 插件，GuGuild 已禁用。");
                Bukkit.getPluginManager().disablePlugin(this);
                return;
            }
            getLogger().warning("Vault 已加载但经济服务尚未注册，GuGuild 将继续运行并等待经济插件注册。");
        }

        database = new Database(this);
        database.init();
        pendingInputService = new PendingInputService();
        titleService = new TitleService(this);
        guildService = new GuildService(this, database, configManager, economyService, titleService);

        PluginCommand command = getCommand("guild");
        if (command != null) {
            GuildCommand executor = new GuildCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }

        Bukkit.getPluginManager().registerEvents(new GuiListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ChatInputListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ServiceRegisterListener(this), this);

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (!economyService.isReady()) {
                economyService.setup();
            }
            database.deleteExpiredInvites(System.currentTimeMillis());
            guildService.checkExpiredTitles();
        }, 20L, 1200L);

        // 延迟到所有插件启用完成后再检测 PlayerTitle，避免因加载顺序导致的误判。
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (titleService.isAvailable()) {
                getLogger().info("检测到 PlayerTitle v" + titleService.getPluginVersion() + "，公会称号功能已启用。");
            } else {
                getLogger().warning("未检测到 PlayerTitle 插件，公会称号功能将被禁用，其余功能正常。");
            }
        }, 20L);

        getLogger().info("GuGuild 已启用。");
    }

    @Override
    public void onDisable() {
        if (database != null) {
            database.close();
        }
        instance = null;
        getLogger().info("GuGuild 已禁用。");
    }

    public static GuGuildPlugin getInstance() {
        return instance;
    }

    public GuildConfig getConfigManager() {
        return configManager;
    }

    public Database getDatabase() {
        return database;
    }

    public EconomyService getEconomyService() {
        return economyService;
    }

    public TitleService getTitleService() {
        return titleService;
    }

    public GuildService getGuildService() {
        return guildService;
    }

    public PendingInputService getPendingInputService() {
        return pendingInputService;
    }
}
