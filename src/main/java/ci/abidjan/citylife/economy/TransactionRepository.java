package ci.abidjan.citylife.economy;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    boolean existsByIdempotencyKey(String idempotencyKey);
    List<Transaction> findTop20BySourceWalletIdOrTargetWalletIdOrderByCreatedAtDesc(UUID sourceWalletId, UUID targetWalletId);
}