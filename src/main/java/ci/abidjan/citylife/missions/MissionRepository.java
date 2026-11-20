package ci.abidjan.citylife.missions;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MissionRepository extends JpaRepository<Mission, UUID> {
    Optional<Mission> findByMissionCode(String missionCode);
}