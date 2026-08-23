package com.guguild.model;

public class GuildInvite {
    public int id;
    public int guildId;
    public String inviterUuid;
    public String targetUuid;
    public String targetName;
    public long createdAt;
    public long expiresAt;
}
