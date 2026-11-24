package ci.abidjan.citylife.moderation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PlayerReportRepository extends JpaRepository<PlayerReport, UUID> {
    List<PlayerReport> findAllByOrderByCreatedAtDesc();
    List<PlayerReport> findByStatusOrderByCreatedAtDesc(PlayerReport.Status status);
}
