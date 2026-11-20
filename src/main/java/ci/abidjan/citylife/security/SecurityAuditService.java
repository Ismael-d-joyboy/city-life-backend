package ci.abidjan.citylife.security;

import ci.abidjan.citylife.auth.Account;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class SecurityAuditService {

    private final SecurityAuditLogRepository auditLogRepository;

    public void log(Account account, String eventType, SecurityAuditLog.Severity severity, String ipAddress, Map<String, Object> details) {
        SecurityAuditLog entry = SecurityAuditLog.builder()
                .account(account)
                .eventType(eventType)
                .severity(severity)
                .ipAddress(ipAddress)
                .details(details != null ? details : Map.of())
                .build();
        auditLogRepository.save(entry);
    }
}