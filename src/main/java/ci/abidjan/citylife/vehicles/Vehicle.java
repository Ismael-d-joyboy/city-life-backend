package ci.abidjan.citylife.vehicles;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "vehicles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehicle {

    public enum VehicleType {
        CAR, MOTO, BUS_GBAKA, TAXI_COMPTEUR, WOROWORO, TRUCK
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "vehicle_code", nullable = false, unique = true, length = 60)
    private String vehicleCode;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 60)
    private String brand;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false, length = 30)
    private VehicleType vehicleType;

    @Column(name = "base_price", nullable = false)
    private Long basePrice;

    @Column(name = "max_speed_kmh", nullable = false)
    private Integer maxSpeedKmh;

    @Column(nullable = false)
    private Integer seats;

    @Column(name = "fuel_capacity", nullable = false)
    private Integer fuelCapacity;

    @Column(name = "trunk_capacity_grams", nullable = false)
    private Integer trunkCapacityGrams;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}