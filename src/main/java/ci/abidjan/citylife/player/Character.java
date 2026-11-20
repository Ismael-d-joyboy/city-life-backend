package ci.abidjan.citylife.player;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "characters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Character {

    public enum Gender {
        MALE, FEMALE
    }

    public enum District {
        PLATEAU, COCODY, TREICHVILLE, MARCORY, YOPOUGON, ABOBO, PORT_BOUET, BINGERVILLE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Column(name = "first_name", nullable = false, length = 60)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 60)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Gender gender;

    @Column(nullable = false)
    @Builder.Default
    private Integer health = 100;

    @Column(nullable = false)
    @Builder.Default
    private Integer stamina = 100;

    @Column(nullable = false)
    @Builder.Default
    private Integer hunger = 100;

    @Column(nullable = false)
    @Builder.Default
    private Integer thirst = 100;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private District district = District.PLATEAU;

    @Column(name = "pos_x", nullable = false)
    @Builder.Default
    private Double posX = 0.0;

    @Column(name = "pos_y", nullable = false)
    @Builder.Default
    private Double posY = 0.0;

    @Column(name = "pos_z", nullable = false)
    @Builder.Default
    private Double posZ = 0.0;

    @Column(name = "rotation_y", nullable = false)
    @Builder.Default
    private Double rotationY = 0.0;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

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