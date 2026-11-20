package ci.abidjan.citylife.economy;

import ci.abidjan.citylife.player.Character;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "wallets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "character_id", nullable = false, unique = true)
    private Character character;

    @Column(name = "cash_balance", nullable = false)
    @Builder.Default
    private Long cashBalance = 25000L;

    @Column(name = "bank_balance", nullable = false)
    @Builder.Default
    private Long bankBalance = 50000L;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String currency = "XOF";

    @Column(name = "is_frozen", nullable = false)
    @Builder.Default
    private boolean frozen = false;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;

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