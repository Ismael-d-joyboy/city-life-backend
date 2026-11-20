package ci.abidjan.citylife.properties;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "properties")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Property {

    public enum PropertyType {
        APARTMENT, VILLA, STUDIO, SHOP, GARAGE, WAREHOUSE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "property_code", nullable = false, unique = true, length = 60)
    private String propertyCode;

    @Column(nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "property_type", nullable = false, length = 30)
    private PropertyType propertyType;

    @Column(nullable = false, length = 50)
    private String district;

    @Column(name = "purchase_price", nullable = false)
    private Long purchasePrice;

    @Column(name = "rent_price", nullable = false)
    private Long rentPrice;

    @Column(name = "storage_capacity_grams", nullable = false)
    private Integer storageCapacityGrams;

    @Column(name = "entrance_x", nullable = false)
    private Double entranceX;

    @Column(name = "entrance_y", nullable = false)
    private Double entranceY;

    @Column(name = "entrance_z", nullable = false)
    private Double entranceZ;

    @Column(name = "is_unique", nullable = false)
    private boolean unique;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}