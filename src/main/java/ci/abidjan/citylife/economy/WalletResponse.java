package ci.abidjan.citylife.economy;

import java.util.UUID;

public record WalletResponse(
        UUID walletId,
        UUID characterId,
        Long cashBalance,
        Long bankBalance,
        String currency,
        boolean frozen
) {
    public static WalletResponse from(Wallet w) {
        return new WalletResponse(
                w.getId(),
                w.getCharacter().getId(),
                w.getCashBalance(),
                w.getBankBalance(),
                w.getCurrency(),
                w.isFrozen()
        );
    }
}