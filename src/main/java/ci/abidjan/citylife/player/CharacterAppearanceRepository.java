package ci.abidjan.citylife.player;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CharacterAppearanceRepository extends JpaRepository<CharacterAppearance, UUID> {
    Optional<CharacterAppearance> findByCharacterId(UUID characterId);
}