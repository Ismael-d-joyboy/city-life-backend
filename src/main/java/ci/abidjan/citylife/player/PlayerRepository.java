package ci.abidjan.citylife.player;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PlayerRepository extends JpaRepository<Player, UUID> {
    boolean existsByUsername(String username);
    Optional<Player> findByAccountId(UUID accountId);
}