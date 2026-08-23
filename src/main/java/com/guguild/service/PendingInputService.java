package com.guguild.service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PendingInputService {
    public enum Action {
        CREATE_GUILD,
        SET_NOTICE,
        TITLE_TEXT,
        INVITE_PLAYER
    }

    private final Map<UUID, Action> pending = new ConcurrentHashMap<>();

    public void put(UUID uuid, Action action) {
        pending.put(uuid, action);
    }

    public Action take(UUID uuid) {
        return pending.remove(uuid);
    }

    /**
     * 清除待输入状态，返回是否确实存在待输入。
     */
    public boolean clear(UUID uuid) {
        return pending.remove(uuid) != null;
    }

    public boolean has(UUID uuid) {
        return pending.containsKey(uuid);
    }
}
