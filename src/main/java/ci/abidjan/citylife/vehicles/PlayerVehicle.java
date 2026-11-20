package ci.abidjan.citylife.vehicles;

import ci.abidjan.citylife.player.Character;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "player_vehicles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerVehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "character_id", nullable = false)
    private Character character;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(name = "license_plate", nullable = false, unique = true, length = 20)
    private String licensePlate;

    @Column(name = "color_hex", nullable = false, length = 15)
    @Builder.Default
    private String colorHex = "#FFFFFF";

    @Column(name = "fuel_level", nullable = false)
    @Builder.Default
    private Integer fuelLevel = 100;

    @Column(name = "engine_health", nullable = false)
    @Builder.Default
    private Integer engineHealth = 100;

    @Column(name = "body_health", nullable = false)
    @Builder.Default
    private Integer bodyHealth = 100;

    @Column(name = "is_locked", nullable = false)
    @Builder.Default
    private boolean locked = true;

    @Column(name = "is_stored", nullable = false)
    @Builder.Default
    private boolean stored = true;

    @Column(name = "is_impounded", nullable = false)
    @Builder.Default
    private boolean impounded = false;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String district = "PLATEAU";

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

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private Map<String, Object> customizations = new HashMap<>();

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