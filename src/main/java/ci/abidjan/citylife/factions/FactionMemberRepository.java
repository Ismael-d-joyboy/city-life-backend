package ci.abidjan.citylife.factions;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FactionMemberRepository extends JpaRepository<FactionMember, UUID> {
    Optional<FactionMember> findByCharacterId(UUID characterId);
    long countByFactionId(UUID factionId);
    void deleteByCharacterId(UUID characterId);
}
