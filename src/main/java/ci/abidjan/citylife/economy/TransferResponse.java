package ci.abidjan.citylife.economy;

import java.util.UUID;

public record TransferResponse(
        UUID transactionId,
        String recipientUsername,
        Long amount,
        Long feeAmount,
        Long totalDebited,
        Long newCashBalance,
        Long newBankBalance,
        String reason,
        String message
) {
}
