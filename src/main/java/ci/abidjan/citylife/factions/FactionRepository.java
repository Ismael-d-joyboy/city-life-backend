package ci.abidjan.citylife.factions;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FactionRepository extends JpaRepository<Faction, UUID> {
    Optional<Faction> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);
    boolean existsByTagIgnoreCase(String tag);
    boolean existsByNameIgnoreCase(String name);
}
