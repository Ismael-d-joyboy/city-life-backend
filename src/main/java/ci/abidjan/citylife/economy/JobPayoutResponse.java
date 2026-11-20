package ci.abidjan.citylife.economy;

public record JobPayoutResponse(
        String actionType,
        long rewardCash,
        long rewardXp,
        long newCashBalance,
        long newBankBalance,
        long totalXp,
        String message
) {}