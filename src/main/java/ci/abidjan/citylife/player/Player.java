package ci.abidjan.citylife.player;

import ci.abidjan.citylife.auth.Account;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "players")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    private Account account;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false)
    @Builder.Default
    private Integer level = 1;

    @Column(name = "experience_points", nullable = false)
    @Builder.Default
    private Long experiencePoints = 0L;

    @Column(name = "tutorial_completed", nullable = false)
    @Builder.Default
    private boolean tutorialCompleted = false;

    @Column(name = "tutorial_step", nullable = false)
    @Builder.Default
    private Integer tutorialStep = 0;

    @Column(name = "tutorial_completed_at")
    private Instant tutorialCompletedAt;

    @Column(name = "muted_until")
    private Instant mutedUntil;

    @Column(name = "mute_reason", length = 255)
    private String muteReason;

    @Column(name = "peaceful_mode", nullable = false)
    @Builder.Default
    private boolean peacefulMode = false;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    /**
     * Recalcule le niveau joueur à partir de l'XP accumulé (palier progressif : 1 + floor(sqrt(xp / 100))).
     */
    public void recalculateLevel() {
        long xp = this.experiencePoints != null ? Math.max(0L, this.experiencePoints) : 0L;
        int computed = 1 + (int) Math.floor(Math.sqrt(xp / 100.0));
        this.level = Math.max(1, computed);
    }

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.tutorialStep == null) {
            this.tutorialStep = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}