package ci.abidjan.citylife.factions;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "faction_territories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FactionTerritory {

    @Id
    @Column(name = "zone_code", nullable = false, length = 40)
    private String zoneCode;

    @Column(name = "zone_name", nullable = false, length = 80)
    private String zoneName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_faction_id")
    private Faction ownerFaction;

    @Column(name = "captured_at")
    private Instant capturedAt;
}
