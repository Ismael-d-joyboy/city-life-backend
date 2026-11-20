package ci.abidjan.citylife.properties;

import java.time.Instant;
import java.util.UUID;

public record PlayerPropertyResponse(
        UUID id,
        String propertyCode,
        String name,
        Property.PropertyType propertyType,
        String district,
        PlayerProperty.OwnershipType ownershipType,
        boolean locked,
        Instant rentDueAt,
        long newBankBalance
) {
    public static PlayerPropertyResponse from(PlayerProperty pp, long bankBalance) {
        Property p = pp.getProperty();
        return new PlayerPropertyResponse(
                pp.getId(),
                p.getPropertyCode(),
                p.getName(),
                p.getPropertyType(),
                p.getDistrict(),
                pp.getOwnershipType(),
                pp.isLocked(),
                pp.getRentDueAt(),
                bankBalance
        );
    }
}