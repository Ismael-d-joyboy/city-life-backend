package ci.abidjan.citylife.missions;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlayerMissionRepository extends JpaRepository<PlayerMission, UUID> {
    List<PlayerMission> findByCharacterId(UUID characterId);
    Optional<PlayerMission> findByCharacterIdAndMissionIdAndStatus(UUID characterId, UUID missionId, PlayerMission.Status status);
    boolean existsByCharacterIdAndMissionIdAndStatus(UUID characterId, UUID missionId, PlayerMission.Status status);
}