package ci.abidjan.citylife.inventory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID> {
    List<InventoryItem> findByCharacterId(UUID characterId);
    Optional<InventoryItem> findByCharacterIdAndItemId(UUID characterId, UUID itemId);
}