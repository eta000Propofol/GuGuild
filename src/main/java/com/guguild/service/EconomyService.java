package com.guguild.service;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

public class EconomyService {
    private final Plugin plugin;
    private Economy economy;
    private boolean vaultPresent;
    private boolean vaultMissingLogged;
    private boolean providerWarned;

    public EconomyService(Plugin plugin) {
        this.plugin = plugin;
    }

    public boolean setup() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            vaultPresent = false;
            if (!vaultMissingLogged) {
                plugin.getLogger().severe("未找到 Vault 插件。请确认 Vault.jar 已放入 plugins 文件夹并已成功加载。");
                vaultMissingLogged = true;
            }
            return false;
        }
        vaultPresent = true;
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null || rsp.getProvider() == null) {
            economy = null;
            if (!providerWarned) {
                plugin.getLogger().warning("Vault 已加载，但尚未注册任何 Economy 经济服务。");
                plugin.getLogger().warning("Vault 只是经济接口，必须再安装一个经济插件，例如 EssentialsX、CMI、XConomy、EzEconomy 等。");
                plugin.getLogger().warning("GuGuild 将继续运行并等待经济插件注册，经济相关功能在注册前不可用。");
                providerWarned = true;
            }
            return false;
        }
        Economy newEconomy = rsp.getProvider();
        if (economy == null || economy != newEconomy) {
            economy = newEconomy;
            plugin.getLogger().info("已连接到 Vault 经济服务: " + economy.getName());
        }
        return true;
    }

    public boolean isVaultPresent() {
        return vaultPresent;
    }

    public boolean isReady() {
        return economy != null;
    }

    public boolean has(Player player, double amount) {
        return economy != null && economy.has(player, amount);
    }

    public boolean withdraw(Player player, double amount) {
        if (economy == null) {
            return false;
        }
        EconomyResponse response = economy.withdrawPlayer(player, amount);
        return response.transactionSuccess();
    }

    public void deposit(Player player, double amount) {
        if (economy != null) {
            economy.depositPlayer(player, amount);
        }
    }

    public String format(double amount) {
        if (economy == null) {
            return String.valueOf(amount);
        }
        return economy.format(amount);
    }
}
