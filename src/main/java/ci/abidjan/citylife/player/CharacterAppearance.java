package ci.abidjan.citylife.player;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "character_appearance")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CharacterAppearance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "character_id", nullable = false, unique = true)
    private Character character;

    @Column(name = "skin_tone", nullable = false, length = 30)
    @Builder.Default
    private String skinTone = "DEFAULT";

    @Column(name = "hair_style", nullable = false, length = 50)
    @Builder.Default
    private String hairStyle = "SHORT_01";

    @Column(name = "hair_color", nullable = false, length = 30)
    @Builder.Default
    private String hairColor = "#1A1A1A";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "outfit_data", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private Map<String, Object> outfitData = new HashMap<>(Map.of(
            "top", "tshirt_basic",
            "bottom", "jeans_basic",
            "shoes", "sneakers_01"
    ));

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        this.updatedAt = Instant.now();
    }
}