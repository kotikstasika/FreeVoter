package ru.kotikstasika.freevoter.service.api;

import org.bukkit.entity.Player;
import ru.kotikstasika.freevoter.data.PlayerVoteData;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface VoteService {
    CompletableFuture<Instant> getLastVoteTime(String nickname);

    void claimReward(Player player, String nickname);

    CompletableFuture<PlayerVoteData> getPlayerStats(String nickname);

    CompletableFuture<List<PlayerVoteData>> getAllPlayerStats();
}
