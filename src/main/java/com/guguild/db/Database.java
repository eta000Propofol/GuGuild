package com.guguild.db;

import com.guguild.GuGuildPlugin;
import com.guguild.model.Guild;
import com.guguild.model.GuildInvite;
import com.guguild.model.GuildMember;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Database {
    private final GuGuildPlugin plugin;
    private final File file;

    public Database(GuGuildPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.db");
    }

    private Connection open() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite JDBC driver not found", e);
        }
        return DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
    }

    public void init() {
        String[] schema = {
                "CREATE TABLE IF NOT EXISTS guilds (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "name TEXT NOT NULL UNIQUE," +
                        "display_name TEXT NOT NULL," +
                        "leader_uuid TEXT NOT NULL UNIQUE," +
                        "icon_base64 TEXT," +
                        "active_value INTEGER NOT NULL DEFAULT 0," +
                        "join_type TEXT NOT NULL DEFAULT 'INVITE'," +
                        "member_limit INTEGER NOT NULL DEFAULT 5," +
                        "notice TEXT NOT NULL DEFAULT ''," +
                        "title_id INTEGER," +
                        "title_text TEXT," +
                        "title_expire_at INTEGER," +
                        "created_at INTEGER NOT NULL)",
                "CREATE TABLE IF NOT EXISTS members (" +
                        "guild_id INTEGER NOT NULL," +
                        "player_uuid TEXT NOT NULL," +
                        "player_name TEXT NOT NULL," +
                        "role TEXT NOT NULL," +
                        "joined_at INTEGER NOT NULL," +
                        "last_signin_date TEXT," +
                        "total_signins INTEGER NOT NULL DEFAULT 0," +
                        "PRIMARY KEY (guild_id, player_uuid))",
                "CREATE TABLE IF NOT EXISTS invites (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "guild_id INTEGER NOT NULL," +
                        "inviter_uuid TEXT NOT NULL," +
                        "target_uuid TEXT NOT NULL," +
                        "target_name TEXT NOT NULL," +
                        "created_at INTEGER NOT NULL," +
                        "expires_at INTEGER NOT NULL)",
                "CREATE INDEX IF NOT EXISTS idx_members_uuid ON members(player_uuid)",
                "CREATE INDEX IF NOT EXISTS idx_invites_target ON invites(target_uuid)"
        };
        try (Connection c = open(); Statement s = c.createStatement()) {
            for (String sql : schema) {
                s.executeUpdate(sql);
            }
            migrateGuildHomeColumns(c);
        } catch (SQLException e) {
            plugin.getLogger().severe("数据库初始化失败: " + e.getMessage());
        }
    }

    private void migrateGuildHomeColumns(Connection c) {
        List<String> columns = new ArrayList<>();
        try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery("PRAGMA table_info(guilds)")) {
            while (rs.next()) {
                columns.add(rs.getString("name"));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("读取 guilds 表结构失败: " + e.getMessage());
            return;
        }
        try (Statement s = c.createStatement()) {
            if (!columns.contains("home_world")) {
                s.executeUpdate("ALTER TABLE guilds ADD COLUMN home_world TEXT");
            }
            if (!columns.contains("home_x")) {
                s.executeUpdate("ALTER TABLE guilds ADD COLUMN home_x REAL");
            }
            if (!columns.contains("home_y")) {
                s.executeUpdate("ALTER TABLE guilds ADD COLUMN home_y REAL");
            }
            if (!columns.contains("home_z")) {
                s.executeUpdate("ALTER TABLE guilds ADD COLUMN home_z REAL");
            }
            if (!columns.contains("home_yaw")) {
                s.executeUpdate("ALTER TABLE guilds ADD COLUMN home_yaw REAL");
            }
            if (!columns.contains("home_pitch")) {
                s.executeUpdate("ALTER TABLE guilds ADD COLUMN home_pitch REAL");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("迁移 guilds 主城字段失败: " + e.getMessage());
        }
    }

    public void updateGuildHome(int guildId, String world, double x, double y, double z, float yaw, float pitch) {
        String sql = "UPDATE guilds SET home_world=?, home_x=?, home_y=?, home_z=?, home_yaw=?, home_pitch=? WHERE id=?";
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, world);
            ps.setDouble(2, x);
            ps.setDouble(3, y);
            ps.setDouble(4, z);
            ps.setFloat(5, yaw);
            ps.setFloat(6, pitch);
            ps.setInt(7, guildId);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("updateGuildHome失败: " + e.getMessage());
        }
    }

    public void close() {
        // connections are opened per call; nothing to close here.
    }

    public int insertGuild(Guild g) {
        String sql = "INSERT INTO guilds(name,display_name,leader_uuid,icon_base64,active_value,join_type,member_limit,notice,title_id,title_text,title_expire_at,created_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, g.name);
            ps.setString(2, g.displayName);
            ps.setString(3, g.leaderUuid);
            ps.setString(4, g.iconBase64);
            ps.setInt(5, g.activeValue);
            ps.setString(6, g.joinType);
            ps.setInt(7, g.memberLimit);
            ps.setString(8, g.notice == null ? "" : g.notice);
            if (g.titleId == null) {
                ps.setNull(9, java.sql.Types.INTEGER);
            } else {
                ps.setInt(9, g.titleId);
            }
            ps.setString(10, g.titleText);
            if (g.titleExpireAt == null) {
                ps.setNull(11, java.sql.Types.INTEGER);
            } else {
                ps.setLong(11, g.titleExpireAt);
            }
            ps.setLong(12, g.createdAt);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("insertGuild失败: " + e.getMessage());
        }
        return -1;
    }

    public void insertMember(GuildMember m) {
        String sql = "INSERT OR REPLACE INTO members(guild_id,player_uuid,player_name,role,joined_at,last_signin_date,total_signins) VALUES(?,?,?,?,?,?,?)";
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, m.guildId);
            ps.setString(2, m.uuid);
            ps.setString(3, m.name);
            ps.setString(4, m.role);
            ps.setLong(5, m.joinedAt);
            ps.setString(6, m.lastSigninDate);
            ps.setInt(7, m.totalSignins);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("insertMember失败: " + e.getMessage());
        }
    }

    public Guild findGuildByName(String normalizedName) {
        String sql = "SELECT * FROM guilds WHERE name=?";
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, normalizedName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapGuild(rs);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("findGuildByName失败: " + e.getMessage());
        }
        return null;
    }

    public Guild findGuildById(int id) {
        String sql = "SELECT * FROM guilds WHERE id=?";
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapGuild(rs);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("findGuildById失败: " + e.getMessage());
        }
        return null;
    }

    public Guild findGuildByPlayer(UUID uuid) {
        String sql = "SELECT g.* FROM guilds g JOIN members m ON m.guild_id=g.id WHERE m.player_uuid=?";
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapGuild(rs);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("findGuildByPlayer失败: " + e.getMessage());
        }
        return null;
    }

    public GuildMember findMember(UUID uuid) {
        String sql = "SELECT * FROM members WHERE player_uuid=?";
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapMember(rs);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("findMember失败: " + e.getMessage());
        }
        return null;
    }

    public List<GuildMember> listMembers(int guildId) {
        String sql = "SELECT * FROM members WHERE guild_id=? ORDER BY CASE role WHEN 'LEADER' THEN 0 WHEN 'VICE' THEN 1 ELSE 2 END, joined_at ASC";
        List<GuildMember> list = new ArrayList<>();
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, guildId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapMember(rs));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("listMembers失败: " + e.getMessage());
        }
        return list;
    }

    public int countMembers(int guildId) {
        String sql = "SELECT COUNT(*) AS cnt FROM members WHERE guild_id=?";
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, guildId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("cnt");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("countMembers失败: " + e.getMessage());
        }
        return 0;
    }

    public List<Guild> listGuilds() {
        String sql = "SELECT g.*, (SELECT COUNT(*) FROM members m WHERE m.guild_id=g.id) AS member_count FROM guilds g ORDER BY g.active_value DESC, g.created_at ASC";
        List<Guild> list = new ArrayList<>();
        try (Connection c = open(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) {
                Guild g = mapGuild(rs);
                g.memberCount = rs.getInt("member_count");
                list.add(g);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("listGuilds失败: " + e.getMessage());
        }
        return list;
    }

    public void addActiveValue(int guildId, int delta) {
        String sql = "UPDATE guilds SET active_value=active_value+? WHERE id=?";
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, delta);
            ps.setInt(2, guildId);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("addActiveValue失败: " + e.getMessage());
        }
    }

    public void updateJoinType(int guildId, String joinType) {
        String sql = "UPDATE guilds SET join_type=? WHERE id=?";
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, joinType);
            ps.setInt(2, guildId);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("updateJoinType失败: " + e.getMessage());
        }
    }

    public void updateMemberLimit(int guildId, int memberLimit) {
        String sql = "UPDATE guilds SET member_limit=? WHERE id=?";
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, memberLimit);
            ps.setInt(2, guildId);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("updateMemberLimit失败: " + e.getMessage());
        }
    }

    public boolean upgradeMemberLimit(int guildId, int cost) {
        String sql = "UPDATE guilds SET active_value=active_value-?, member_limit=member_limit+1 WHERE id=? AND active_value>=?";
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, cost);
            ps.setInt(2, guildId);
            ps.setInt(3, cost);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().severe("upgradeMemberLimit失败: " + e.getMessage());
            return false;
        }
    }

    public void updateIcon(int guildId, String iconBase64) {
        String sql = "UPDATE guilds SET icon_base64=? WHERE id=?";
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, iconBase64);
            ps.setInt(2, guildId);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("updateIcon失败: " + e.getMessage());
        }
    }

    public void updateNotice(int guildId, String notice) {
        String sql = "UPDATE guilds SET notice=? WHERE id=?";
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, notice);
            ps.setInt(2, guildId);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("updateNotice失败: " + e.getMessage());
        }
    }

    public void updateGuildTitle(int guildId, int titleId, String titleText, long expireAt) {
        String sql = "UPDATE guilds SET title_id=?, title_text=?, title_expire_at=? WHERE id=?";
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, titleId);
            ps.setString(2, titleText);
            ps.setLong(3, expireAt);
            ps.setInt(4, guildId);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("updateGuildTitle失败: " + e.getMessage());
        }
    }

    public void clearGuildTitle(int guildId) {
        String sql = "UPDATE guilds SET title_id=NULL, title_text=NULL, title_expire_at=NULL WHERE id=?";
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, guildId);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("clearGuildTitle失败: " + e.getMessage());
        }
    }

    public void updateLeader(int guildId, String leaderUuid) {
        String sql = "UPDATE guilds SET leader_uuid=? WHERE id=?";
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, leaderUuid);
            ps.setInt(2, guildId);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("updateLeader失败: " + e.getMessage());
        }
    }

    public void updateMemberRole(int guildId, UUID uuid, String role) {
        String sql = "UPDATE members SET role=? WHERE guild_id=? AND player_uuid=?";
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, role);
            ps.setInt(2, guildId);
            ps.setString(3, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("updateMemberRole失败: " + e.getMessage());
        }
    }

    public void updateMemberSignin(int guildId, UUID uuid, String today) {
        String sql = "UPDATE members SET last_signin_date=?, total_signins=total_signins+1 WHERE guild_id=? AND player_uuid=?";
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, today);
            ps.setInt(2, guildId);
            ps.setString(3, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("updateMemberSignin失败: " + e.getMessage());
        }
    }

    public void deleteGuild(int guildId) {
        try (Connection c = open()) {
            c.setAutoCommit(false);
            try (PreparedStatement p1 = c.prepareStatement("DELETE FROM members WHERE guild_id=?");
                 PreparedStatement p2 = c.prepareStatement("DELETE FROM invites WHERE guild_id=?");
                 PreparedStatement p3 = c.prepareStatement("DELETE FROM guilds WHERE id=?")) {
                p1.setInt(1, guildId);
                p1.executeUpdate();
                p2.setInt(1, guildId);
                p2.executeUpdate();
                p3.setInt(1, guildId);
                p3.executeUpdate();
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("deleteGuild失败: " + e.getMessage());
        }
    }

    public void deleteMember(int guildId, UUID uuid) {
        String sql = "DELETE FROM members WHERE guild_id=? AND player_uuid=?";
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, guildId);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("deleteMember失败: " + e.getMessage());
        }
    }

    public int insertInvite(GuildInvite invite) {
        String sql = "INSERT INTO invites(guild_id,inviter_uuid,target_uuid,target_name,created_at,expires_at) VALUES(?,?,?,?,?,?)";
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, invite.guildId);
            ps.setString(2, invite.inviterUuid);
            ps.setString(3, invite.targetUuid);
            ps.setString(4, invite.targetName);
            ps.setLong(5, invite.createdAt);
            ps.setLong(6, invite.expiresAt);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("insertInvite失败: " + e.getMessage());
        }
        return -1;
    }

    public List<GuildInvite> findInvitesByTarget(UUID targetUuid) {
        String sql = "SELECT * FROM invites WHERE target_uuid=?";
        List<GuildInvite> list = new ArrayList<>();
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, targetUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapInvite(rs));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("findInvitesByTarget失败: " + e.getMessage());
        }
        return list;
    }

    public void deleteInvitesByTarget(UUID targetUuid) {
        String sql = "DELETE FROM invites WHERE target_uuid=?";
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, targetUuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("deleteInvitesByTarget失败: " + e.getMessage());
        }
    }

    public void deleteInvite(int inviteId) {
        String sql = "DELETE FROM invites WHERE id=?";
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, inviteId);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("deleteInvite失败: " + e.getMessage());
        }
    }

    public void deleteExpiredInvites(long now) {
        String sql = "DELETE FROM invites WHERE expires_at<=?";
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, now);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("deleteExpiredInvites失败: " + e.getMessage());
        }
    }

    private Guild mapGuild(ResultSet rs) throws SQLException {
        Guild g = new Guild();
        g.id = rs.getInt("id");
        g.name = rs.getString("name");
        g.displayName = rs.getString("display_name");
        g.leaderUuid = rs.getString("leader_uuid");
        g.iconBase64 = rs.getString("icon_base64");
        g.activeValue = rs.getInt("active_value");
        g.joinType = rs.getString("join_type");
        g.memberLimit = rs.getInt("member_limit");
        g.notice = rs.getString("notice");
        int titleId = rs.getInt("title_id");
        g.titleId = rs.wasNull() ? null : titleId;
        g.titleText = rs.getString("title_text");
        long titleExpireAt = rs.getLong("title_expire_at");
        g.titleExpireAt = rs.wasNull() ? null : titleExpireAt;
        g.createdAt = rs.getLong("created_at");
        g.homeWorld = rs.getString("home_world");
        g.homeX = rs.getDouble("home_x");
        g.homeY = rs.getDouble("home_y");
        g.homeZ = rs.getDouble("home_z");
        g.homeYaw = rs.getFloat("home_yaw");
        g.homePitch = rs.getFloat("home_pitch");
        return g;
    }

    private GuildMember mapMember(ResultSet rs) throws SQLException {
        GuildMember m = new GuildMember();
        m.guildId = rs.getInt("guild_id");
        m.uuid = rs.getString("player_uuid");
        m.name = rs.getString("player_name");
        m.role = rs.getString("role");
        m.joinedAt = rs.getLong("joined_at");
        m.lastSigninDate = rs.getString("last_signin_date");
        m.totalSignins = rs.getInt("total_signins");
        return m;
    }

    private GuildInvite mapInvite(ResultSet rs) throws SQLException {
        GuildInvite invite = new GuildInvite();
        invite.id = rs.getInt("id");
        invite.guildId = rs.getInt("guild_id");
        invite.inviterUuid = rs.getString("inviter_uuid");
        invite.targetUuid = rs.getString("target_uuid");
        invite.targetName = rs.getString("target_name");
        invite.createdAt = rs.getLong("created_at");
        invite.expiresAt = rs.getLong("expires_at");
        return invite;
    }
}


