package ru.kotikstasika.freevoter.database.api;

import ru.kotikstasika.freevoter.data.PlayerVoteData;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface DatabaseManager {
    void initialize();

    void shutdown();

    CompletableFuture<PlayerVoteData> loadPlayerDataByNickname(String nickname);

    CompletableFuture<Void> savePlayerData(PlayerVoteData data);

    CompletableFuture<List<PlayerVoteData>> getAllPlayers();
}
