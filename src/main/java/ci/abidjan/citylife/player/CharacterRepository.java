package ci.abidjan.citylife.player;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CharacterRepository extends JpaRepository<Character, UUID> {
    List<Character> findByPlayerIdAndActiveTrue(UUID playerId);
    Optional<Character> findByIdAndPlayerId(UUID id, UUID playerId);
    long countByPlayerIdAndActiveTrue(UUID playerId);
}