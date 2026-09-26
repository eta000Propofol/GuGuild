package com.guguild.model;

public class Guild {
    public int id;
    public String name;
    public String displayName;
    public String leaderUuid;
    public String iconBase64;
    public int activeValue;
    public String joinType;
    public int memberLimit;
    public String notice;
    public Integer titleId;
    public String titleText;
    public Long titleExpireAt;
    public long createdAt;
    public int memberCount;

    // 公会主城
    public String homeWorld;
    public double homeX;
    public double homeY;
    public double homeZ;
    public float homeYaw;
    public float homePitch;

    public boolean hasHome() {
        return homeWorld != null && !homeWorld.isEmpty();
    }
}
