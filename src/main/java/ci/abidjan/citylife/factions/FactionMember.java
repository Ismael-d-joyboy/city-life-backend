package ci.abidjan.citylife.factions;

import ci.abidjan.citylife.player.Character;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "faction_members")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FactionMember {

    public enum Role {
        LEADER, OFFICER, MEMBER
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "faction_id", nullable = false)
    private Faction faction;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "character_id", nullable = false, unique = true)
    private Character character;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Role role = Role.MEMBER;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    @PrePersist
    protected void onCreate() {
        this.joinedAt = Instant.now();
    }
}
