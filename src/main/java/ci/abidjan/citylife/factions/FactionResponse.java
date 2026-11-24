package ci.abidjan.citylife.factions;

import java.time.Instant;
import java.util.UUID;

public record FactionResponse(
        UUID id,
        String code,
        String tag,
        String name,
        String colorHex,
        long memberCount,
        String myRole,
        String message
) {
    public record TerritoryResponse(
            String zoneCode,
            String zoneName,
            String ownerFactionCode,
            String ownerFactionTag,
            String ownerFactionName,
            Instant capturedAt,
            String message
    ) {
        public static TerritoryResponse from(FactionTerritory t, String msg) {
            Faction owner = t.getOwnerFaction();
            return new TerritoryResponse(
                    t.getZoneCode(),
                    t.getZoneName(),
                    owner != null ? owner.getCode() : "",
                    owner != null ? owner.getTag() : "",
                    owner != null ? owner.getName() : "Zone neutre",
                    t.getCapturedAt(),
                    msg
            );
        }
    }
}
