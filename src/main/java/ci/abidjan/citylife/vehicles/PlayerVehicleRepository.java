package ci.abidjan.citylife.vehicles;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PlayerVehicleRepository extends JpaRepository<PlayerVehicle, UUID> {
    List<PlayerVehicle> findByCharacterId(UUID characterId);
    boolean existsByLicensePlate(String licensePlate);
}