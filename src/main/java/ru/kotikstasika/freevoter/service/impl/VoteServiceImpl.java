package ru.kotikstasika.freevoter.service.impl;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ru.kotikstasika.freevoter.FreeVoter;
import ru.kotikstasika.freevoter.config.ConfigManager;
import ru.kotikstasika.freevoter.data.PlayerVoteData;
import ru.kotikstasika.freevoter.database.api.DatabaseManager;
import ru.kotikstasika.freevoter.service.api.VoteService;
import ru.kotikstasika.freevoter.utils.Parser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VoteServiceImpl implements VoteService {
    FreeVoter plugin;
    ConfigManager config;
    DatabaseManager database;
    HttpClient httpClient;
    Gson gson;
    Set<String> processing = ConcurrentHashMap.newKeySet();

    public VoteServiceImpl(FreeVoter plugin, ConfigManager config, DatabaseManager database) {
        this.plugin = plugin;
        this.config = config;
        this.database = database;
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new Gson();
    }

    @Override
    public CompletableFuture<Instant> getLastVoteTime(String nickname) {
        return CompletableFuture.supplyAsync(() -> {
            int page = 1;

            while (true) {
                String url = String.format("https://public-api.top-minecrafter.com/v1/servers/%d/votes?page=%d&limit=50&key=%s", config.getServerId(), page, config.getToken());

                try {
                    HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).header("Accept", "application/json").GET().build();
                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                    if (response.statusCode() != 200) {
                        handleApiError(response.statusCode());
                        return null;
                    }

                    JsonObject json = gson.fromJson(response.body(), JsonObject.class);
                    JsonObject result = json.getAsJsonObject("result");
                    JsonArray votes = result.getAsJsonArray("votes");

                    for (int i = 0; i < votes.size(); i++) {
                        JsonObject vote = votes.get(i).getAsJsonObject();
                        String voteNickname = vote.get("nickname").getAsString();
                        if (voteNickname.equalsIgnoreCase(nickname)) {
                            String votedAt = vote.get("voted_at").getAsString();
                            return Instant.parse(votedAt);
                        }
                    }

                    JsonObject pagination = result.getAsJsonObject("pagination");
                    int totalPages = pagination.get("total_pages").getAsInt();

                    if (page >= totalPages) {
                        return null;
                    }

                    page++;
                } catch (IOException | InterruptedException e) {
                    if (config.isDebug()) {
                        plugin.getLogger().severe("Ошибка API запроса: " + e.getMessage());
                    }
                    return null;
                }
            }
        }, Runnable::run);
    }

    @Override
    public void claimReward(Player player, String nickname) {
        if (!processing.add(nickname.toLowerCase())) {
            Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(Parser.color(config.getFixRaceCondishinal())));
            return;
        }

        getLastVoteTime(nickname).thenAccept(votedAt -> {
            if (votedAt == null) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    for (String line : config.getNoVotesMessage()) {
                        player.sendMessage(Parser.color(line));
                    }
                });
                return;
            }

            database.loadPlayerDataByNickname(nickname).thenAccept(data -> {
                Instant now = Instant.now();
                ZoneId msk = ZoneId.of("Europe/Moscow");
                LocalDate todayMsk = LocalDate.now(msk);
                Instant midnightMsk = todayMsk.atStartOfDay(msk).toInstant();
                Instant nextMidnightMsk = todayMsk.plusDays(1).atStartOfDay(msk).toInstant();

                if (votedAt.isBefore(midnightMsk)) {
                    Duration remaining = Duration.between(now, nextMidnightMsk);
                    long hours = remaining.toHours();
                    long minutes = remaining.toMinutesPart();
                    long seconds = remaining.toSecondsPart();
                    String timeLeft = hours + " ч. " + minutes + " м. " + seconds + " с.";

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        for (String line : config.getOldVotesMessage()) {
                            String formatted = line.replace("{time}", timeLeft);
                            player.sendMessage(Parser.color(formatted));
                        }
                    });
                    return;
                }

                Instant lastReward = data.getLastRewardAt();

                if (lastReward.isAfter(midnightMsk)) {
                    Duration remaining = Duration.between(now, nextMidnightMsk);
                    long hours = remaining.toHours();
                    long minutes = remaining.toMinutesPart();
                    long seconds = remaining.toSecondsPart();
                    String timeLeft = hours + " ч. " + minutes + " м. " + seconds + " с.";

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        for (String line : config.getHasKassedMessage()) {
                            String formatted = line.replace("{time}", timeLeft);
                            player.sendMessage(Parser.color(formatted));
                        }
                    });
                    return;
                }

                data.setRewardsClaimed(data.getRewardsClaimed() + 1);
                data.setTotalVotes(data.getTotalVotes() + 1);
                data.setLastRewardAt(now);
                data.setLastVoteAt(votedAt);

                database.savePlayerData(data).thenRun(() -> {
                    Duration untilReset = Duration.between(now, nextMidnightMsk);
                    long h = untilReset.toHours();
                    long m = untilReset.toMinutesPart();
                    long s = untilReset.toSecondsPart();
                    String resetTime = h + " ч. " + m + " м. " + s + " с.";

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        String prizeText = String.join(", ", config.getRewardMessages());

                        for (String line : config.getGiveMessage()) {
                            String formatted = line.replace("{prize}", prizeText).replace("{time}", resetTime);
                            player.sendMessage(Parser.color(formatted));
                        }

                        for (String cmd : config.getRewardCommands()) {
                            String formatted = cmd.replace("{player}", player.getName());
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), formatted);
                        }
                    });
                });
            });
        }).exceptionally(throwable -> {
            processing.remove(nickname.toLowerCase());
            return null;
        }).thenRun(() -> processing.remove(nickname.toLowerCase()));
    }

    @Override
    public CompletableFuture<PlayerVoteData> getPlayerStats(String nickname) {
        return database.loadPlayerDataByNickname(nickname);
    }

    @Override
    public CompletableFuture<List<PlayerVoteData>> getAllPlayerStats() {
        return database.getAllPlayers();
    }

    private void handleApiError(int statusCode) {
        if (!config.isDebug()) return;

        switch (statusCode) {
            case 401:
                plugin.getLogger().severe(config.getError401Console());
                break;
            case 403:
                plugin.getLogger().severe(config.getError403Console());
                break;
            case 404:
                plugin.getLogger().severe(config.getError404Console());
                break;
            case 429:
                plugin.getLogger().severe(config.getError429Console());
                break;
            default:
                plugin.getLogger().severe(config.getErrorDefault());
                break;
        }
    }
}
