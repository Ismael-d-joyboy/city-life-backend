package ci.abidjan.citylife.inventory;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Item {

    public enum Category {
        FOOD, DRINK, TOOL, PHONE, KEY, DOCUMENT, CLOTHING, MISC
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "item_code", nullable = false, unique = true, length = 60)
    private String itemCode;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private Category category;

    @Column(name = "base_price", nullable = false)
    private Long basePrice;

    @Column(name = "weight_grams", nullable = false)
    private Integer weightGrams;

    @Column(name = "is_stackable", nullable = false)
    private boolean stackable;

    @Column(name = "max_stack", nullable = false)
    private Integer maxStack;

    @Column(name = "is_tradable", nullable = false)
    private boolean tradable;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private Map<String, Object> effects = new HashMap<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}