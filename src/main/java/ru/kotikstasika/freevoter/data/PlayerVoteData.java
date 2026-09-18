package ru.kotikstasika.freevoter.data;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PlayerVoteData {
    String nickname;
    int totalVotes;
    int rewardsClaimed;
    Instant lastVoteAt;
    Instant lastRewardAt;
}
