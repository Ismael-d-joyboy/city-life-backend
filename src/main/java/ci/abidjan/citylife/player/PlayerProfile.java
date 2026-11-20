package ci.abidjan.citylife.player;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "player_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false, unique = true)
    private Player player;

    @Column(length = 500)
    @Builder.Default
    private String bio = "";

    @Column(name = "current_job", nullable = false, length = 80)
    @Builder.Default
    private String currentJob = "SANS_EMPLOI";

    @Column(name = "reputation_score", nullable = false)
    @Builder.Default
    private Integer reputationScore = 0;

    @Column(name = "playtime_minutes", nullable = false)
    @Builder.Default
    private Long playtimeMinutes = 0L;

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