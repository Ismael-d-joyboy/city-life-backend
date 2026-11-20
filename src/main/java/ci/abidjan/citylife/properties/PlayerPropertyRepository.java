package ci.abidjan.citylife.properties;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PlayerPropertyRepository extends JpaRepository<PlayerProperty, UUID> {
    List<PlayerProperty> findByCharacterId(UUID characterId);
    boolean existsByCharacterIdAndPropertyId(UUID characterId, UUID propertyId);
}