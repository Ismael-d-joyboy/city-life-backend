package ci.abidjan.citylife.moderation;

import ci.abidjan.citylife.auth.Account;
import ci.abidjan.citylife.auth.AccountRepository;
import ci.abidjan.citylife.player.Player;
import ci.abidjan.citylife.player.PlayerRepository;
import ci.abidjan.citylife.security.AccountUserDetailsService;
import ci.abidjan.citylife.security.SecurityAuditLog;
import ci.abidjan.citylife.security.SecurityAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ModerationService {

    private final AccountRepository accountRepository;
    private final PlayerRepository playerRepository;
    private final PlayerReportRepository reportRepository;
    private final SecurityAuditService auditService;
    private final AccountUserDetailsService userDetailsService;

    @Transactional
    public ReportResponse createReport(String email, CreateReportRequest request, String ipAddress) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Compte introuvable."));
        Player reporter = playerRepository.findByAccountId(account.getId())
                .orElseThrow(() -> new IllegalStateException("Joueur introuvable."));

        String excerpt = request.messageExcerpt() != null ? request.messageExcerpt().trim() : "";
        if (excerpt.length() > 400) excerpt = excerpt.substring(0, 400);

        PlayerReport report = reportRepository.save(PlayerReport.builder()
                .reporter(reporter)
                .targetUsername(request.targetUsername().trim())
                .reasonCode(request.reasonCode())
                .messageExcerpt(excerpt)
                .status(PlayerReport.Status.OPEN)
                .build());

        auditService.log(account, "PLAYER_REPORT_SUBMITTED", SecurityAuditLog.Severity.INFO, ipAddress,
                Map.of("reportId", report.getId().toString(), "target", report.getTargetUsername(), "reason", report.getReasonCode().name()));

        UUID targetId = playerRepository.findByUsernameIgnoreCase(report.getTargetUsername())
                .map(Player::getId)
                .orElse(null);

        return ReportResponse.from(report, targetId,
                "Signalement transmis à la modération (" + report.getReasonCode().name() + "). Merci !");
    }

    @Transactional(readOnly = true)
    public List<ReportResponse> listReports(PlayerReport.Status statusFilter) {
        List<PlayerReport> list = (statusFilter != null)
                ? reportRepository.findByStatusOrderByCreatedAtDesc(statusFilter)
                : reportRepository.findAllByOrderByCreatedAtDesc();
        return list.stream()
                .map(r -> {
                    UUID targetId = playerRepository.findByUsernameIgnoreCase(r.getTargetUsername())
                            .map(Player::getId)
                            .orElse(null);
                    return ReportResponse.from(r, targetId, "");
                })
                .toList();
    }

    @Transactional
    public ReportResponse resolveReport(String moderatorEmail, UUID reportId, ResolveReportRequest request, String ipAddress) {
        Account modAccount = accountRepository.findByEmail(moderatorEmail)
                .orElseThrow(() -> new IllegalStateException("Compte modérateur introuvable."));
        PlayerReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Signalement introuvable : " + reportId));

        report.setStatus(request.status());
        report.setResolutionNote(request.resolutionNote() != null ? request.resolutionNote().trim() : "");
        report.setResolvedBy(modAccount);
        report.setResolvedAt(Instant.now());
        report = reportRepository.save(report);

        auditService.log(modAccount, "PLAYER_REPORT_RESOLVED", SecurityAuditLog.Severity.INFO, ipAddress,
                Map.of("reportId", report.getId().toString(), "status", report.getStatus().name()));

        return ReportResponse.from(report, "Signalement mis à jour : " + report.getStatus().name());
    }

    @Transactional
    public Map<String, Object> mutePlayer(String moderatorEmail, UUID playerId, MutePlayerRequest request, String ipAddress) {
        Account modAccount = accountRepository.findByEmail(moderatorEmail)
                .orElseThrow(() -> new IllegalStateException("Compte modérateur introuvable."));
        Player target = playerRepository.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Joueur introuvable : " + playerId));

        Instant until = Instant.now().plus(request.durationMinutes(), ChronoUnit.MINUTES);
        target.setMutedUntil(until);
        target.setMuteReason(request.reason().trim());
        playerRepository.save(target);

        auditService.log(modAccount, "PLAYER_MUTED", SecurityAuditLog.Severity.WARNING, ipAddress,
                Map.of("targetPlayerId", target.getId().toString(), "minutes", request.durationMinutes(), "reason", target.getMuteReason()));

        return Map.of(
                "playerId", target.getId().toString(),
                "username", target.getUsername(),
                "mutedUntil", until.toString(),
                "reason", target.getMuteReason(),
                "message", "Joueur " + target.getUsername() + " mis en sourdine pour " + request.durationMinutes() + " min."
        );
    }

    @Transactional
    public Map<String, Object> unmutePlayer(String moderatorEmail, UUID playerId, String ipAddress) {
        Account modAccount = accountRepository.findByEmail(moderatorEmail)
                .orElseThrow(() -> new IllegalStateException("Compte modérateur introuvable."));
        Player target = playerRepository.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Joueur introuvable : " + playerId));

        target.setMutedUntil(null);
        target.setMuteReason(null);
        playerRepository.save(target);

        auditService.log(modAccount, "PLAYER_UNMUTED", SecurityAuditLog.Severity.INFO, ipAddress,
                Map.of("targetPlayerId", target.getId().toString()));

        return Map.of(
                "playerId", target.getId().toString(),
                "username", target.getUsername(),
                "message", "Sourdine levée pour " + target.getUsername() + "."
        );
    }

    @Transactional
    public Map<String, Object> banPlayer(String moderatorEmail, UUID playerId, BanPlayerRequest request, String ipAddress) {
        Account modAccount = accountRepository.findByEmail(moderatorEmail)
                .orElseThrow(() -> new IllegalStateException("Compte modérateur introuvable."));
        Player target = playerRepository.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Joueur introuvable : " + playerId));

        Account targetAccount = target.getAccount();
        if (targetAccount == null) {
            throw new IllegalStateException("Compte associé introuvable.");
        }
        if (targetAccount.getRole() == Account.Role.ADMIN) {
            throw new IllegalStateException("Impossible de bannir un compte ADMIN.");
        }

        targetAccount.setStatus(Account.Status.BANNED);
        targetAccount.setBanReason(request.reason().trim());
        accountRepository.save(targetAccount);
        if (userDetailsService != null) {
            userDetailsService.evictCache(targetAccount.getEmail());
        }

        auditService.log(modAccount, "PLAYER_BANNED", SecurityAuditLog.Severity.CRITICAL, ipAddress,
                Map.of("targetPlayerId", target.getId().toString(), "reason", targetAccount.getBanReason()));

        return Map.of(
                "playerId", target.getId().toString(),
                "username", target.getUsername(),
                "status", targetAccount.getStatus().name(),
                "reason", targetAccount.getBanReason(),
                "message", "Compte " + target.getUsername() + " banni."
        );
    }

    @Transactional
    public Map<String, Object> unbanPlayer(String moderatorEmail, UUID playerId, String ipAddress) {
        Account modAccount = accountRepository.findByEmail(moderatorEmail)
                .orElseThrow(() -> new IllegalStateException("Compte modérateur introuvable."));
        Player target = playerRepository.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Joueur introuvable : " + playerId));

        Account targetAccount = target.getAccount();
        if (targetAccount == null) {
            throw new IllegalStateException("Compte associé introuvable.");
        }

        targetAccount.setStatus(Account.Status.ACTIVE);
        targetAccount.setBanReason(null);
        accountRepository.save(targetAccount);
        if (userDetailsService != null) {
            userDetailsService.evictCache(targetAccount.getEmail());
        }

        auditService.log(modAccount, "PLAYER_UNBANNED", SecurityAuditLog.Severity.INFO, ipAddress,
                Map.of("targetPlayerId", target.getId().toString()));

        return Map.of(
                "playerId", target.getId().toString(),
                "username", target.getUsername(),
                "status", targetAccount.getStatus().name(),
                "message", "Compte " + target.getUsername() + " débanni."
        );
    }
}
