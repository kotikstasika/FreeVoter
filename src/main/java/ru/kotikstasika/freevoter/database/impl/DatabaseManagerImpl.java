//ГПТ (i hate database)

package ru.kotikstasika.freevoter.database.impl;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import ru.kotikstasika.freevoter.FreeVoter;
import ru.kotikstasika.freevoter.config.ConfigManager;
import ru.kotikstasika.freevoter.data.PlayerVoteData;
import ru.kotikstasika.freevoter.database.api.DatabaseManager;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DatabaseManagerImpl implements DatabaseManager {
    final FreeVoter plugin;
    final ConfigManager config;
    final ExecutorService saveExecutor;
    HikariDataSource dataSource;

    public DatabaseManagerImpl(FreeVoter plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
        this.saveExecutor = Executors.newFixedThreadPool(2);
    }

    @Override
    public void initialize() {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(String.format("jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC", config.getDbHost(), config.getDbPort(), config.getDbName()));
        hikariConfig.setUsername(config.getDbUser());
        hikariConfig.setPassword(config.getDbPassword());
        hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
        hikariConfig.setMaximumPoolSize(5);
        hikariConfig.setMinimumIdle(1);
        hikariConfig.setConnectionTimeout(5000);
        hikariConfig.setPoolName("FreeVoter-Pool");

        this.dataSource = new HikariDataSource(hikariConfig);
        CompletableFuture.runAsync(this::createTable, saveExecutor);
    }

    @Override
    public void shutdown() {
        saveExecutor.shutdown();
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS freevoter_players (" + "nickname VARCHAR(64) PRIMARY KEY," + "total_votes INT DEFAULT 0," + "rewards_claimed INT DEFAULT 0," + "last_vote_at BIGINT DEFAULT 0," + "last_reward_at BIGINT DEFAULT 0" + ")";
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            plugin.getLogger().severe("Не удалось создать таблицу: " + e.getMessage());
        }
    }

    @Override
    public CompletableFuture<PlayerVoteData> loadPlayerDataByNickname(String nickname) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement("SELECT * FROM freevoter_players WHERE LOWER(nickname) = LOWER(?)")) {
                ps.setString(1, nickname);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    return mapRow(rs);
                }
                return PlayerVoteData.builder().nickname(nickname).totalVotes(0).rewardsClaimed(0).lastVoteAt(Instant.EPOCH).lastRewardAt(Instant.EPOCH).build();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }, saveExecutor);
    }

    @Override
    public CompletableFuture<Void> savePlayerData(PlayerVoteData data) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO freevoter_players (nickname, total_votes, rewards_claimed, last_vote_at, last_reward_at) " + "VALUES (?, ?, ?, ?, ?) " + "ON DUPLICATE KEY UPDATE " + "total_votes = VALUES(total_votes), " + "rewards_claimed = VALUES(rewards_claimed), " + "last_vote_at = VALUES(last_vote_at), " + "last_reward_at = VALUES(last_reward_at)";
            try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, data.getNickname());
                ps.setInt(2, data.getTotalVotes());
                ps.setInt(3, data.getRewardsClaimed());
                ps.setLong(4, data.getLastVoteAt().toEpochMilli());
                ps.setLong(5, data.getLastRewardAt().toEpochMilli());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Не удалось сохранить данные: " + e.getMessage());
            }
        }, saveExecutor);
    }

    @Override
    public CompletableFuture<List<PlayerVoteData>> getAllPlayers() {
        return CompletableFuture.supplyAsync(() -> {
            List<PlayerVoteData> players = new ArrayList<>();
            try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT * FROM freevoter_players ORDER BY rewards_claimed DESC")) {
                while (rs.next()) {
                    players.add(mapRow(rs));
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return players;
        }, saveExecutor);
    }

    private PlayerVoteData mapRow(ResultSet rs) throws SQLException {
        return PlayerVoteData.builder().nickname(rs.getString("nickname")).totalVotes(rs.getInt("total_votes")).rewardsClaimed(rs.getInt("rewards_claimed")).lastVoteAt(Instant.ofEpochMilli(rs.getLong("last_vote_at"))).lastRewardAt(Instant.ofEpochMilli(rs.getLong("last_reward_at"))).build();
    }
}
