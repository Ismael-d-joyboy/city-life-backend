package ci.abidjan.citylife.economy;

import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        Transaction.Type transactionType,
        Long amount,
        Transaction.Status status,
        String description,
        Instant createdAt
) {
    public static TransactionResponse from(Transaction t) {
        return new TransactionResponse(
                t.getId(),
                t.getTransactionType(),
                t.getAmount(),
                t.getStatus(),
                t.getDescription(),
                t.getCreatedAt()
        );
    }
}