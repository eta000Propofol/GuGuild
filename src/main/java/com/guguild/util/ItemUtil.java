package com.guguild.util;

import com.guguild.GuGuildPlugin;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public final class ItemUtil {
    private ItemUtil() {
    }

    public static NamespacedKey key(String name) {
        return new NamespacedKey(GuGuildPlugin.getInstance(), name);
    }

    public static ItemStack createItem(Material material, String name, String action, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.colorize(name));
            if (lore != null && !lore.isEmpty()) {
                List<String> colored = new ArrayList<>();
                for (String line : lore) {
                    colored.add(ColorUtil.colorize(line));
                }
                meta.setLore(colored);
            }
            item.setItemMeta(meta);
        }
        if (action != null) {
            setAction(item, action);
        }
        return item;
    }

    public static ItemStack createSkull(OfflinePlayer player, String name, List<String> lore) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(player);
            meta.setDisplayName(ColorUtil.colorize(name));
            if (lore != null && !lore.isEmpty()) {
                List<String> colored = new ArrayList<>();
                for (String line : lore) {
                    colored.add(ColorUtil.colorize(line));
                }
                meta.setLore(colored);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public static void setAction(ItemStack item, String action) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        meta.getPersistentDataContainer().set(key("action"), org.bukkit.persistence.PersistentDataType.STRING, action);
        item.setItemMeta(meta);
    }

    public static String getAction(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(key("action"), org.bukkit.persistence.PersistentDataType.STRING);
    }

    public static void setGuildId(ItemStack item, int guildId) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        meta.getPersistentDataContainer().set(key("guild_id"), org.bukkit.persistence.PersistentDataType.INTEGER, guildId);
        item.setItemMeta(meta);
    }

    public static int getGuildId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return -1;
        }
        Integer value = item.getItemMeta().getPersistentDataContainer().get(key("guild_id"), org.bukkit.persistence.PersistentDataType.INTEGER);
        return value == null ? -1 : value;
    }

    public static void setMemberUuid(ItemStack item, String uuid) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        meta.getPersistentDataContainer().set(key("member_uuid"), org.bukkit.persistence.PersistentDataType.STRING, uuid);
        item.setItemMeta(meta);
    }

    public static String iconToBase64(ItemStack item) {
        try {
            return Base64.getEncoder().encodeToString(item.serializeAsBytes());
        } catch (Exception e) {
            GuGuildPlugin.getInstance().getLogger().warning("物品序列化失败: " + e.getMessage());
            return null;
        }
    }

    public static ItemStack iconFromBase64(String base64) {
        if (base64 == null || base64.isEmpty()) {
            return null;
        }
        try {
            return ItemStack.deserializeBytes(Base64.getDecoder().decode(base64));
        } catch (Exception e) {
            GuGuildPlugin.getInstance().getLogger().warning("物品反序列化失败: " + e.getMessage());
            return null;
        }
    }

    public static ItemStack defaultGuildIcon() {
        return createItem(Material.GOLD_INGOT, "&e公会图标", null, null);
    }
}
