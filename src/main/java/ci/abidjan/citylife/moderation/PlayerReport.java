package ci.abidjan.citylife.moderation;

import ci.abidjan.citylife.auth.Account;
import ci.abidjan.citylife.player.Player;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "player_reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerReport {

    public enum ReasonCode {
        INSULTES, SPAM, TRICHE, AUTRE
    }

    public enum Status {
        OPEN, RESOLVED, DISMISSED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_player_id", nullable = false)
    private Player reporter;

    @Column(name = "target_username", nullable = false, length = 80)
    private String targetUsername;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_code", nullable = false, length = 30)
    private ReasonCode reasonCode;

    @Column(name = "message_excerpt", nullable = false, length = 400)
    @Builder.Default
    private String messageExcerpt = "";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.OPEN;

    @Column(name = "resolution_note", length = 300)
    private String resolutionNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by_account_id")
    private Account resolvedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
