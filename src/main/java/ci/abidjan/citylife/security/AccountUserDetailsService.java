package ci.abidjan.citylife.security;

import ci.abidjan.citylife.auth.Account;
import ci.abidjan.citylife.auth.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class AccountUserDetailsService implements UserDetailsService {

    private static final long CACHE_TTL_MS = 60_000L;
    private static final int MAX_CACHE_SIZE = 2048;

    private record CachedAccount(Account account, long cachedAtMs) {}

    private final AccountRepository accountRepository;
    private final Map<String, CachedAccount> accountCache = new ConcurrentHashMap<>();

    public void evictCache(String email) {
        if (email != null) {
            accountCache.remove(email.toLowerCase().trim());
        }
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String key = email.toLowerCase().trim();
        long now = System.currentTimeMillis();

        CachedAccount cached = accountCache.get(key);
        Account account;
        if (cached != null && (now - cached.cachedAtMs()) < CACHE_TTL_MS) {
            account = cached.account();
        } else {
            account = accountRepository.findByEmail(key)
                    .orElseThrow(() -> new UsernameNotFoundException("Compte introuvable : " + email));
            if (accountCache.size() >= MAX_CACHE_SIZE) {
                accountCache.clear();
            }
            accountCache.put(key, new CachedAccount(account, now));
        }

        if (account.getStatus() == Account.Status.BANNED) {
            throw new LockedException("Ce compte est banni : " + account.getBanReason());
        }
        if (account.getStatus() == Account.Status.SUSPENDED) {
            throw new DisabledException("Ce compte est temporairement suspendu.");
        }

        return new User(
                account.getEmail(),
                account.getPasswordHash(),
                List.of(new SimpleGrantedAuthority("ROLE_" + account.getRole().name()))
        );
    }
}