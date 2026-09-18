package ru.kotikstasika.freevoter.cache;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import ru.kotikstasika.freevoter.data.PlayerVoteData;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VoteCache {
    Map<String, PlayerVoteData> cacheByNickname = new ConcurrentHashMap<>();

    public PlayerVoteData get(String nickname) {
        return cacheByNickname.get(nickname.toLowerCase());
    }

    public void put(PlayerVoteData data) {
        if (data.getNickname() != null && !data.getNickname().isEmpty()) {
            cacheByNickname.put(data.getNickname().toLowerCase(), data);
        }
    }

    public void remove(String nickname) {
        cacheByNickname.remove(nickname.toLowerCase());
    }
}
