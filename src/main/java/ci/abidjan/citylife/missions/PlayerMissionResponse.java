package ci.abidjan.citylife.missions;

import java.time.Instant;
import java.util.UUID;

public record PlayerMissionResponse(
        UUID id,
        String missionCode,
        String title,
        PlayerMission.Status status,
        Long rewardCash,
        Long rewardXp,
        Instant startedAt,
        Instant completedAt
) {
    public static PlayerMissionResponse from(PlayerMission pm) {
        Mission m = pm.getMission();
        return new PlayerMissionResponse(
                pm.getId(),
                m.getMissionCode(),
                m.getTitle(),
                pm.getStatus(),
                m.getRewardCash(),
                m.getRewardXp(),
                pm.getStartedAt(),
                pm.getCompletedAt()
        );
    }
}