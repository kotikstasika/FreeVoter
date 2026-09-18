package ru.kotikstasika.freevoter.listener;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import ru.kotikstasika.freevoter.FreeVoter;
import ru.kotikstasika.freevoter.cache.VoteCache;
import ru.kotikstasika.freevoter.data.PlayerVoteData;
import ru.kotikstasika.freevoter.database.api.DatabaseManager;

@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PlayerListener implements Listener {
    FreeVoter plugin;
    DatabaseManager database;
    VoteCache cache;

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        String nickname = event.getPlayer().getName();
        database.loadPlayerDataByNickname(nickname).thenAccept(data -> {
            if (data != null) {
                cache.put(data);
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        String nickname = event.getPlayer().getName();
        PlayerVoteData data = cache.get(nickname);
        if (data != null) {
            database.savePlayerData(data).thenRun(() -> cache.remove(nickname));
        }
    }
}
