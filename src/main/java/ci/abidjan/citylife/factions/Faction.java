package ci.abidjan.citylife.factions;

import ci.abidjan.citylife.player.Character;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "factions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Faction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 16)
    private String code;

    @Column(nullable = false, unique = true, length = 8)
    private String tag;

    @Column(nullable = false, unique = true, length = 80)
    private String name;

    @Column(name = "color_hex", nullable = false, length = 16)
    @Builder.Default
    private String colorHex = "#F5C518";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leader_character_id")
    private Character leader;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
