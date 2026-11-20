package ci.abidjan.citylife.inventory;

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

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final AccountRepository accountRepository;
    private final PlayerRepository playerRepository;
    private final CharacterRepository characterRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final ItemRepository itemRepository;
    private final InventoryItemRepository inventoryItemRepository;

    @Transactional(readOnly = true)
    public List<Item> getCatalog() {
        return itemRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<InventoryItemResponse> getInventory(String email, UUID characterId) {
        Character character = getOwnedCharacter(email, characterId);
        return inventoryItemRepository.findByCharacterId(character.getId())
                .stream()
                .map(InventoryItemResponse::from)
                .toList();
    }

    @Transactional
    public InventoryItemResponse buyItem(String email, UUID characterId, BuyItemRequest request) {
        if (transactionRepository.existsByIdempotencyKey(request.idempotencyKey())) {
            throw new IllegalArgumentException("Achat déjà traité (double achat bloqué).");
        }

        Character character = getOwnedCharacter(email, characterId);
        Wallet wallet = walletRepository.findByCharacterId(character.getId())
                .orElseThrow(() -> new IllegalStateException("Portefeuille introuvable."));

        if (wallet.isFrozen()) {
            throw new IllegalStateException("Portefeuille gelé.");
        }

        Item item = itemRepository.findByItemCode(request.itemCode().trim())
                .orElseThrow(() -> new IllegalArgumentException("Objet introuvable : " + request.itemCode()));

        if (!item.isTradable()) {
            throw new IllegalStateException("Cet objet ne peut pas être acheté.");
        }

        long totalPrice = Math.multiplyExact(item.getBasePrice(), (long) request.quantity());
        if (wallet.getCashBalance() < totalPrice) {
            throw new IllegalStateException("Argent liquide insuffisant (" + totalPrice + " FCFA requis).");
        }

        wallet.setCashBalance(wallet.getCashBalance() - totalPrice);
        walletRepository.save(wallet);

        transactionRepository.save(Transaction.builder()
                .idempotencyKey(request.idempotencyKey())
                .sourceWallet(wallet)
                .amount(totalPrice)
                .transactionType(Transaction.Type.PURCHASE)
                .status(Transaction.Status.COMPLETED)
                .description("Achat : " + request.quantity() + "x " + item.getName())
                .metadata(Map.of("itemCode", item.getItemCode(), "quantity", request.quantity()))
                .build());

        InventoryItem inventoryItem = inventoryItemRepository
                .findByCharacterIdAndItemId(character.getId(), item.getId())
                .orElse(null);

        if (inventoryItem == null) {
            if (!item.isStackable() && request.quantity() > 1) {
                throw new IllegalStateException("Cet objet ne peut être acheté qu'à l'unité.");
            }
            inventoryItem = InventoryItem.builder()
                    .character(character)
                    .item(item)
                    .quantity(request.quantity())
                    .build();
        } else {
            if (!item.isStackable()) {
                throw new IllegalStateException("Vous possédez déjà cet objet unique.");
            }
            int newQuantity = inventoryItem.getQuantity() + request.quantity();
            if (newQuantity > item.getMaxStack()) {
                throw new IllegalStateException("Capacité maximale atteinte (" + item.getMaxStack() + ").");
            }
            inventoryItem.setQuantity(newQuantity);
        }

        inventoryItem = inventoryItemRepository.save(inventoryItem);
        return InventoryItemResponse.from(inventoryItem);
    }

    private Character getOwnedCharacter(String email, UUID characterId) {
        Account account = accountRepository.findByEmail(email).orElseThrow();
        Player player = playerRepository.findByAccountId(account.getId()).orElseThrow();
        return characterRepository.findByIdAndPlayerId(characterId, player.getId())
                .orElseThrow(() -> new IllegalArgumentException("Personnage introuvable ou accès refusé."));
    }
}