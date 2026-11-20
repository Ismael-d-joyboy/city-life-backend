package ci.abidjan.citylife.properties;

import ci.abidjan.citylife.auth.Account;
import ci.abidjan.citylife.auth.AccountRepository;
import ci.abidjan.citylife.economy.Transaction;
import ci.abidjan.citylife.economy.TransactionRepository;
import ci.abidjan.citylife.economy.Wallet;
import ci.abidjan.citylife.economy.WalletRepository;
import ci.abidjan.citylife.player.Character;
import ci.abidjan.citylife.player.CharacterRepository;
import ci.abidjan.citylife.player.Player;
import ci.abidjan.citylife.player.PlayerRepository;
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
public class PropertyService {

    private final AccountRepository accountRepository;
    private final PlayerRepository playerRepository;
    private final CharacterRepository characterRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final PropertyRepository propertyRepository;
    private final PlayerPropertyRepository playerPropertyRepository;

    @Transactional(readOnly = true)
    public List<Property> getCatalog() {
        return propertyRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<PlayerPropertyResponse> getMyProperties(String email, UUID characterId) {
        Character character = getOwnedCharacter(email, characterId);
        Wallet wallet = walletRepository.findByCharacterId(character.getId()).orElseThrow();
        return playerPropertyRepository.findByCharacterId(character.getId())
                .stream()
                .map(pp -> PlayerPropertyResponse.from(pp, wallet.getBankBalance()))
                .toList();
    }

    @Transactional
    public PlayerPropertyResponse acquireProperty(String email, UUID characterId, AcquirePropertyRequest request) {
        if (transactionRepository.existsByIdempotencyKey(request.idempotencyKey())) {
            throw new IllegalArgumentException("Opération immobilière déjà traitée (doublon bloqué).");
        }

        Character character = getOwnedCharacter(email, characterId);
        Wallet wallet = walletRepository.findByCharacterId(character.getId())
                .orElseThrow(() -> new IllegalStateException("Portefeuille introuvable."));

        if (wallet.isFrozen()) {
            throw new IllegalStateException("Portefeuille gelé.");
        }

        Property property = propertyRepository.findByPropertyCode(request.propertyCode().trim())
                .orElseThrow(() -> new IllegalArgumentException("Bien introuvable : " + request.propertyCode()));

        if (playerPropertyRepository.existsByCharacterIdAndPropertyId(character.getId(), property.getId())) {
            throw new IllegalStateException("Vous possédez ou louez déjà ce bien.");
        }

        boolean isRent = request.ownershipType() == PlayerProperty.OwnershipType.RENTED;
        long price = isRent ? property.getRentPrice() : property.getPurchasePrice();

        if (wallet.getBankBalance() < price) {
            throw new IllegalStateException("Solde bancaire insuffisant (" + price + " FCFA requis).");
        }

        wallet.setBankBalance(wallet.getBankBalance() - price);
        walletRepository.save(wallet);

        PlayerProperty pp = PlayerProperty.builder()
                .character(character)
                .property(property)
                .ownershipType(request.ownershipType())
                .locked(false)
                .rentDueAt(isRent ? Instant.now().plus(30, ChronoUnit.DAYS) : null)
                .build();
        pp = playerPropertyRepository.save(pp);

        transactionRepository.save(Transaction.builder()
                .idempotencyKey(request.idempotencyKey())
                .sourceWallet(wallet)
                .amount(price)
                .transactionType(Transaction.Type.PURCHASE)
                .status(Transaction.Status.COMPLETED)
                .description((isRent ? "Location : " : "Achat immobilier : ") + property.getName())
                .metadata(Map.of("propertyCode", property.getPropertyCode(), "ownershipType", request.ownershipType().name()))
                .build());

        return PlayerPropertyResponse.from(pp, wallet.getBankBalance());
    }

    private Character getOwnedCharacter(String email, UUID characterId) {
        Account account = accountRepository.findByEmail(email).orElseThrow();
        Player player = playerRepository.findByAccountId(account.getId()).orElseThrow();
        return characterRepository.findByIdAndPlayerId(characterId, player.getId())
                .orElseThrow(() -> new IllegalArgumentException("Personnage introuvable ou accès refusé."));
    }
}