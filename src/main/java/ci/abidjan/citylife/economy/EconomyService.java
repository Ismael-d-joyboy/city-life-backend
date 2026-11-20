package ci.abidjan.citylife.economy;

import ci.abidjan.citylife.auth.Account;
import ci.abidjan.citylife.auth.AccountRepository;
import ci.abidjan.citylife.player.Character;
import ci.abidjan.citylife.player.CharacterRepository;
import ci.abidjan.citylife.player.Player;
import ci.abidjan.citylife.player.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EconomyService {

    private final AccountRepository accountRepository;
    private final PlayerRepository playerRepository;
    private final CharacterRepository characterRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    public WalletResponse deposit(String email, BankOperationRequest request) {
        if (transactionRepository.existsByIdempotencyKey(request.idempotencyKey())) {
            throw new IllegalArgumentException("Opération déjà traitée (doublon bloqué).");
        }

        Wallet wallet = getOwnedWallet(email, request.characterId());
        if (wallet.isFrozen()) {
            throw new IllegalStateException("Ce portefeuille est gelé.");
        }
        if (wallet.getCashBalance() < request.amount()) {
            throw new IllegalStateException("Solde en liquide insuffisant.");
        }

        wallet.setCashBalance(wallet.getCashBalance() - request.amount());
        wallet.setBankBalance(wallet.getBankBalance() + request.amount());
        wallet = walletRepository.save(wallet);

        transactionRepository.save(Transaction.builder()
                .idempotencyKey(request.idempotencyKey())
                .sourceWallet(wallet)
                .targetWallet(wallet)
                .amount(request.amount())
                .transactionType(Transaction.Type.DEPOSIT)
                .status(Transaction.Status.COMPLETED)
                .description("Dépôt à la banque : " + request.amount() + " FCFA")
                .metadata(Map.of("channel", "MOBILE_BANKING"))
                .build());

        return WalletResponse.from(wallet);
    }

    @Transactional
    public WalletResponse withdraw(String email, BankOperationRequest request) {
        if (transactionRepository.existsByIdempotencyKey(request.idempotencyKey())) {
            throw new IllegalArgumentException("Opération déjà traitée (doublon bloqué).");
        }

        Wallet wallet = getOwnedWallet(email, request.characterId());
        if (wallet.isFrozen()) {
            throw new IllegalStateException("Ce portefeuille est gelé.");
        }
        if (wallet.getBankBalance() < request.amount()) {
            throw new IllegalStateException("Solde bancaire insuffisant.");
        }

        wallet.setBankBalance(wallet.getBankBalance() - request.amount());
        wallet.setCashBalance(wallet.getCashBalance() + request.amount());
        wallet = walletRepository.save(wallet);

        transactionRepository.save(Transaction.builder()
                .idempotencyKey(request.idempotencyKey())
                .sourceWallet(wallet)
                .targetWallet(wallet)
                .amount(request.amount())
                .transactionType(Transaction.Type.WITHDRAWAL)
                .status(Transaction.Status.COMPLETED)
                .description("Retrait bancaire : " + request.amount() + " FCFA")
                .metadata(Map.of("channel", "ATM"))
                .build());

        return WalletResponse.from(wallet);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getHistory(String email, UUID characterId) {
        Wallet wallet = getOwnedWallet(email, characterId);
        return transactionRepository
                .findTop20BySourceWalletIdOrTargetWalletIdOrderByCreatedAtDesc(wallet.getId(), wallet.getId())
                .stream()
                .map(TransactionResponse::from)
                .toList();
    }

    private Wallet getOwnedWallet(String email, UUID characterId) {
        Account account = accountRepository.findByEmail(email).orElseThrow();
        Player player = playerRepository.findByAccountId(account.getId()).orElseThrow();
        Character character = characterRepository.findByIdAndPlayerId(characterId, player.getId())
                .orElseThrow(() -> new IllegalArgumentException("Personnage introuvable ou accès refusé."));
        return walletRepository.findByCharacterId(character.getId())
                .orElseThrow(() -> new IllegalStateException("Portefeuille introuvable."));
    }
}