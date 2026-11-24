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

    @Transactional
    public ConsumeItemResponse consumeItem(String email, UUID characterId, ConsumeItemRequest request) {
        Character character = getOwnedCharacter(email, characterId);
        Item item = itemRepository.findByItemCode(request.itemCode().trim())
                .orElseThrow(() -> new IllegalArgumentException("Objet introuvable : " + request.itemCode()));

        InventoryItem invItem = inventoryItemRepository
                .findByCharacterIdAndItemId(character.getId(), item.getId())
                .orElseThrow(() -> new IllegalStateException("Vous ne possédez pas cet objet dans votre inventaire."));

        int qty = Math.max(1, request.quantity());
        if (invItem.getQuantity() < qty) {
            throw new IllegalStateException("Quantité insuffisante dans l'inventaire.");
        }

        Map<String, Object> effects = item.getEffects();
        int hungerAdd = readIntEffect(effects, "hunger_restore") * qty;
        int thirstAdd = readIntEffect(effects, "thirst_restore") * qty;
        int healthAdd = readIntEffect(effects, "health_restore") * qty;
        int staminaAdd = readIntEffect(effects, "stamina_restore") * qty;

        if (hungerAdd <= 0 && thirstAdd <= 0 && healthAdd <= 0 && staminaAdd <= 0) {
            throw new IllegalStateException("Cet objet ne peut pas être consommé.");
        }

        character.setHunger(clamp100((character.getHunger() != null ? character.getHunger() : 100) + hungerAdd));
        character.setThirst(clamp100((character.getThirst() != null ? character.getThirst() : 100) + thirstAdd));
        character.setHealth(clamp100((character.getHealth() != null ? character.getHealth() : 100) + healthAdd));
        character.setStamina(clamp100((character.getStamina() != null ? character.getStamina() : 100) + staminaAdd));
        characterRepository.save(character);

        int remaining = invItem.getQuantity() - qty;
        if (remaining <= 0) {
            inventoryItemRepository.delete(invItem);
            remaining = 0;
        } else {
            invItem.setQuantity(remaining);
            inventoryItemRepository.save(invItem);
        }

        return new ConsumeItemResponse(
                item.getItemCode(),
                item.getName(),
                remaining,
                character.getHealth(),
                character.getStamina(),
                character.getHunger(),
                character.getThirst(),
                item.getName() + " consommé ! Santé " + character.getHealth() + "% | Faim " + character.getHunger() + "%."
        );
    }

    private static int readIntEffect(Map<String, Object> effects, String key) {
        if (effects == null || !effects.containsKey(key) || effects.get(key) == null) return 0;
        Object val = effects.get(key);
        if (val instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(val.toString());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static int clamp100(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private Character getOwnedCharacter(String email, UUID characterId) {
        Account account = accountRepository.findByEmail(email).orElseThrow();
        Player player = playerRepository.findByAccountId(account.getId()).orElseThrow();
        return characterRepository.findByIdAndPlayerId(characterId, player.getId())
                .orElseThrow(() -> new IllegalArgumentException("Personnage introuvable ou accès refusé."));
    }
}