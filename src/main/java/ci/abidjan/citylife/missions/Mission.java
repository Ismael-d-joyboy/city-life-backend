package ci.abidjan.citylife.missions;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "missions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mission {

    public record MissionObjective(int step, String code, String label) {}

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "mission_code", nullable = false, unique = true, length = 60)
    private String missionCode;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(nullable = false, length = 50)
    private String district;

    @Column(name = "min_level", nullable = false)
    private Integer minLevel;

    @Column(name = "reward_cash", nullable = false)
    private Long rewardCash;

    @Column(name = "reward_xp", nullable = false)
    private Long rewardXp;

    @Column(name = "is_repeatable", nullable = false)
    private boolean repeatable;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private List<MissionObjective> objectives = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}