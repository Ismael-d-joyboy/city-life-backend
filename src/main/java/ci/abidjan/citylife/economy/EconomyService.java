package ci.abidjan.citylife.economy;

import ci.abidjan.citylife.auth.Account;
import ci.abidjan.citylife.auth.AccountRepository;
import ci.abidjan.citylife.player.Character;
import ci.abidjan.citylife.player.CharacterRepository;
import ci.abidjan.citylife.player.Player;
import ci.abidjan.citylife.player.PlayerRepository;
import ci.abidjan.citylife.security.SecurityAuditLog;
import ci.abidjan.citylife.security.SecurityAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EconomyService {

    public static final long MIN_TRANSFER_AMOUNT = 100L;
    public static final long MAX_TRANSFER_AMOUNT = 500_000L;
    public static final long MIN_TRANSFER_FEE = 50L;

    private final AccountRepository accountRepository;
    private final PlayerRepository playerRepository;
    private final CharacterRepository characterRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final SecurityAuditService auditService;

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

    @Transactional
    public TransferResponse transfer(String email, TransferRequest request, String ipAddress) {
        if (transactionRepository.existsByIdempotencyKey(request.idempotencyKey())) {
            throw new IllegalArgumentException("Transfert Lagune Money déjà traité (doublon bloqué).");
        }

        long amount = request.amount() != null ? request.amount() : 0L;
        if (amount < MIN_TRANSFER_AMOUNT || amount > MAX_TRANSFER_AMOUNT) {
            throw new IllegalArgumentException("Montant invalide : compris entre 100 et 500 000 FCFA.");
        }

        Account senderAccount = accountRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Compte introuvable."));
        Player senderPlayer = playerRepository.findByAccountId(senderAccount.getId())
                .orElseThrow(() -> new IllegalStateException("Joueur expéditeur introuvable."));
        Character senderChar = characterRepository.findByIdAndPlayerId(request.characterId(), senderPlayer.getId())
                .orElseThrow(() -> new IllegalArgumentException("Personnage introuvable ou accès refusé."));
        Wallet senderWallet = walletRepository.findByCharacterId(senderChar.getId())
                .orElseThrow(() -> new IllegalStateException("Portefeuille expéditeur introuvable."));
        if (senderWallet.isFrozen()) {
            throw new IllegalStateException("Votre compte est gelé.");
        }

        String recipientPseudo = request.recipientUsername().trim();
        Player recipientPlayer = playerRepository.findByUsernameIgnoreCase(recipientPseudo)
                .orElseThrow(() -> new IllegalArgumentException("Destinataire introuvable : " + recipientPseudo));
        if (recipientPlayer.getId().equals(senderPlayer.getId())) {
            throw new IllegalArgumentException("Impossible d'envoyer un transfert Lagune Money à vous-même.");
        }

        List<Character> recipientChars = characterRepository.findByPlayerIdAndActiveTrue(recipientPlayer.getId());
        if (recipientChars == null || recipientChars.isEmpty()) {
            throw new IllegalStateException("Le destinataire n'a pas encore de personnage actif.");
        }
        Wallet recipientWallet = walletRepository.findByCharacterId(recipientChars.get(0).getId())
                .orElseThrow(() -> new IllegalStateException("Portefeuille destinataire introuvable."));
        if (recipientWallet.isFrozen()) {
            throw new IllegalStateException("Le compte destinataire est actuellement gelé.");
        }

        // Frais symboliques 1 % (minimum 50 FCFA) — débit & crédit sur compte Banque (bank_balance)
        long fee = Math.max(MIN_TRANSFER_FEE, Math.round(amount * 0.01));
        long totalDebited = amount + fee;
        if (senderWallet.getBankBalance() < totalDebited) {
            throw new IllegalStateException("Solde bancaire insuffisant (requis : " + totalDebited + " FCFA dont " + fee + " FCFA de frais).");
        }

        senderWallet.setBankBalance(senderWallet.getBankBalance() - totalDebited);
        recipientWallet.setBankBalance(recipientWallet.getBankBalance() + amount);
        senderWallet = walletRepository.save(senderWallet);
        walletRepository.save(recipientWallet);

        senderPlayer.setLastSeenAt(Instant.now());
        playerRepository.save(senderPlayer);

        String cleanReason = (request.reason() != null && !request.reason().isBlank())
                ? request.reason().trim()
                : "Transfert Lagune Money";
        String description = "Lagune Money -> " + recipientPlayer.getUsername() + " : " + amount + " FCFA (frais " + fee + ") • " + cleanReason;

        Transaction tx = transactionRepository.save(Transaction.builder()
                .idempotencyKey(request.idempotencyKey())
                .sourceWallet(senderWallet)
                .targetWallet(recipientWallet)
                .amount(amount)
                .transactionType(Transaction.Type.TRANSFER)
                .status(Transaction.Status.COMPLETED)
                .description(description)
                .metadata(Map.of(
                        "channel", "LAGUNE_MONEY",
                        "fee", fee,
                        "totalDebited", totalDebited,
                        "senderUsername", senderPlayer.getUsername(),
                        "recipientUsername", recipientPlayer.getUsername(),
                        "reason", cleanReason
                ))
                .build());

        auditService.log(senderAccount, "LAGUNE_MONEY_TRANSFER", SecurityAuditLog.Severity.INFO, ipAddress,
                Map.of("txId", tx.getId() != null ? tx.getId().toString() : "n/a",
                        "to", recipientPlayer.getUsername(),
                        "amount", amount,
                        "fee", fee));

        return new TransferResponse(
                tx.getId(),
                recipientPlayer.getUsername(),
                amount,
                fee,
                totalDebited,
                senderWallet.getCashBalance(),
                senderWallet.getBankBalance(),
                cleanReason,
                "Transfert Lagune Money envoyé à " + recipientPlayer.getUsername() + " : " + amount + " FCFA (frais : " + fee + " FCFA)."
        );
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransferHistory(String email, UUID characterId) {
        Wallet wallet = getOwnedWallet(email, characterId);
        return transactionRepository
                .findTop20ByTransactionTypeAndSourceWalletIdOrTransactionTypeAndTargetWalletIdOrderByCreatedAtDesc(
                        Transaction.Type.TRANSFER, wallet.getId(),
                        Transaction.Type.TRANSFER, wallet.getId())
                .stream()
                .map(TransactionResponse::from)
                .toList();
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