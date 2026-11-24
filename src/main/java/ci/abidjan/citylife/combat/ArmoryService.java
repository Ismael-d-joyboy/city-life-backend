package ci.abidjan.citylife.combat;

import ci.abidjan.citylife.auth.Account;
import ci.abidjan.citylife.auth.AccountRepository;
import ci.abidjan.citylife.economy.Transaction;
import ci.abidjan.citylife.economy.TransactionRepository;
import ci.abidjan.citylife.economy.Wallet;
import ci.abidjan.citylife.economy.WalletRepository;
import ci.abidjan.citylife.inventory.InventoryItem;
import ci.abidjan.citylife.inventory.InventoryItemRepository;
import ci.abidjan.citylife.inventory.Item;
import ci.abidjan.citylife.inventory.ItemRepository;
import ci.abidjan.citylife.player.Character;
import ci.abidjan.citylife.player.CharacterRepository;
import ci.abidjan.citylife.player.Player;
import ci.abidjan.citylife.player.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ArmoryService {

    public static final String LICENSE_CODE = "LICENSE_WEAPON_PERMIT";
    public static final long LICENSE_PRICE_FCFA = 15000L;

    private static final Set<String> ARMORY_CODES = Set.of(
            LICENSE_CODE,
            "WEAPON_BATON",
            "WEAPON_BAT",
            "WEAPON_TASER",
            "WEAPON_PISTOL_9MM",
            "WEAPON_TACTICAL_RIFLE",
            "AMMO_9MM_ARCADE",
            "AMMO_RIFLE_ARCADE"
    );

    private static final Set<String> LICENSE_REQUIRED_CODES = Set.of(
            "WEAPON_TASER",
            "WEAPON_PISTOL_9MM",
            "WEAPON_TACTICAL_RIFLE"
    );

    private final AccountRepository accountRepository;
    private final PlayerRepository playerRepository;
    private final CharacterRepository characterRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final ItemRepository itemRepository;
    private final InventoryItemRepository inventoryItemRepository;

    @Transactional(readOnly = true)
    public List<Item> getArmoryCatalog() {
        return itemRepository.findAll().stream()
                .filter(i -> ARMORY_CODES.contains(i.getItemCode()))
                .toList();
    }

    @Transactional
    public ArmoryResponse buyWeaponLicense(String email, UUID characterId, String idempotencyKey) {
        Character character = getOwnedCharacter(email, characterId);
        Wallet wallet = walletRepository.findByCharacterId(character.getId())
                .orElseThrow(() -> new IllegalStateException("Portefeuille introuvable."));

        if (character.isHasWeaponLicense()) {
            return new ArmoryResponse(
                    LICENSE_CODE,
                    "Permis de Port d'Arme",
                    1,
                    true,
                    0L,
                    wallet.getCashBalance(),
                    wallet.getBankBalance(),
                    "Vous possédez déjà votre Permis de Port d'Arme."
            );
        }

        if (transactionRepository.existsByIdempotencyKey(idempotencyKey)) {
            throw new IllegalArgumentException("Demande de permis déjà traitée.");
        }

        if (wallet.isFrozen()) {
            throw new IllegalStateException("Portefeuille gelé.");
        }

        long price = itemRepository.findByItemCode(LICENSE_CODE)
                .map(Item::getBasePrice)
                .orElse(LICENSE_PRICE_FCFA);

        if (wallet.getCashBalance() < price) {
            throw new IllegalStateException("Argent liquide insuffisant (" + price + " FCFA requis pour le permis).");
        }

        wallet.setCashBalance(wallet.getCashBalance() - price);
        walletRepository.save(wallet);

        transactionRepository.save(Transaction.builder()
                .idempotencyKey(idempotencyKey)
                .sourceWallet(wallet)
                .amount(price)
                .transactionType(Transaction.Type.PURCHASE)
                .status(Transaction.Status.COMPLETED)
                .description("Armurerie Lagune : Permis de Port d'Arme")
                .metadata(Map.of("itemCode", LICENSE_CODE, "service", "ARMORY_LICENSE"))
                .build());

        character.setHasWeaponLicense(true);
        characterRepository.save(character);

        return new ArmoryResponse(
                LICENSE_CODE,
                "Permis de Port d'Arme",
                1,
                true,
                price,
                wallet.getCashBalance(),
                wallet.getBankBalance(),
                "Permis de Port d'Arme délivré (" + price + " FCFA) !"
        );
    }

    @Transactional
    public ArmoryResponse buyArmoryItem(String email, ArmoryBuyRequest request) {
        String code = request.itemCode().trim();
        if (LICENSE_CODE.equals(code)) {
            return buyWeaponLicense(email, request.characterId(), request.idempotencyKey());
        }

        if (!ARMORY_CODES.contains(code)) {
            throw new IllegalArgumentException("Article non disponible à l'Armurerie Lagune : " + code);
        }

        if (transactionRepository.existsByIdempotencyKey(request.idempotencyKey())) {
            throw new IllegalArgumentException("Achat déjà traité (double achat bloqué).");
        }

        Character character = getOwnedCharacter(email, request.characterId());
        if (LICENSE_REQUIRED_CODES.contains(code) && !character.isHasWeaponLicense()) {
            throw new IllegalStateException("Permis de Port d'Arme requis pour acheter cet équipement.");
        }

        Wallet wallet = walletRepository.findByCharacterId(character.getId())
                .orElseThrow(() -> new IllegalStateException("Portefeuille introuvable."));

        if (wallet.isFrozen()) {
            throw new IllegalStateException("Portefeuille gelé.");
        }

        Item item = itemRepository.findByItemCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Objet armurerie introuvable : " + code));

        int qty = Math.max(1, request.quantity());
        long totalPrice = Math.multiplyExact(item.getBasePrice(), (long) qty);
        if (wallet.getCashBalance() < totalPrice) {
            throw new IllegalStateException("Argent liquide insuffisant (" + totalPrice + " FCFA requis).");
        }

        InventoryItem existing = inventoryItemRepository.findByCharacterIdAndItemId(character.getId(), item.getId())
                .orElse(null);

        if (existing != null && !item.isStackable()) {
            throw new IllegalStateException("Vous possédez déjà : " + item.getName() + ".");
        }

        wallet.setCashBalance(wallet.getCashBalance() - totalPrice);
        walletRepository.save(wallet);

        transactionRepository.save(Transaction.builder()
                .idempotencyKey(request.idempotencyKey())
                .sourceWallet(wallet)
                .amount(totalPrice)
                .transactionType(Transaction.Type.PURCHASE)
                .status(Transaction.Status.COMPLETED)
                .description("Armurerie Lagune : " + qty + "x " + item.getName())
                .metadata(Map.of("itemCode", item.getItemCode(), "quantity", qty, "shop", "ARMURERIE_LAGUNE"))
                .build());

        int newQty;
        if (existing == null) {
            existing = InventoryItem.builder()
                    .character(character)
                    .item(item)
                    .quantity(item.isStackable() ? qty : 1)
                    .build();
            newQty = existing.getQuantity();
        } else {
            newQty = Math.min(item.getMaxStack(), existing.getQuantity() + qty);
            existing.setQuantity(newQty);
        }
        inventoryItemRepository.save(existing);

        return new ArmoryResponse(
                item.getItemCode(),
                item.getName(),
                newQty,
                character.isHasWeaponLicense(),
                totalPrice,
                wallet.getCashBalance(),
                wallet.getBankBalance(),
                item.getName() + " acheté (" + totalPrice + " FCFA) !"
        );
    }

    @Transactional
    public PoliceFineResponse applyPoliceFine(String email, PoliceFineRequest request) {
        Character character = getOwnedCharacter(email, request.characterId());
        Wallet wallet = walletRepository.findByCharacterId(character.getId())
                .orElseThrow(() -> new IllegalStateException("Portefeuille introuvable."));

        int stars = Math.max(1, Math.min(3, request.wantedStars()));
        long expectedFine = stars * 5000L;
        long paid = 0L;

        if (!transactionRepository.existsByIdempotencyKey(request.idempotencyKey())) {
            if (!wallet.isFrozen() && wallet.getCashBalance() > 0) {
                paid = Math.min(wallet.getCashBalance(), expectedFine);
                wallet.setCashBalance(wallet.getCashBalance() - paid);
                walletRepository.save(wallet);

                transactionRepository.save(Transaction.builder()
                        .idempotencyKey(request.idempotencyKey())
                        .sourceWallet(wallet)
                        .amount(paid)
                        .transactionType(Transaction.Type.FINE)
                        .status(Transaction.Status.COMPLETED)
                        .description("Contravention Police Nationale (" + stars + " étoile(s)) : " + request.reason())
                        .metadata(Map.of("wantedStars", stars, "reason", request.reason(), "custody", request.shortCustody()))
                        .build());
            }

            character.setWantedLevel(0);
            long totalPaid = character.getTotalFinesPaid() != null ? character.getTotalFinesPaid() : 0L;
            character.setTotalFinesPaid(totalPaid + paid);
            characterRepository.save(character);
        }

        String msg = request.shortCustody()
                ? "Garde à vue au Commissariat terminée — amende de " + paid + " FCFA réglée."
                : "Contravention Police réglée (" + paid + " FCFA). Casier blanchi.";

        return new PoliceFineResponse(
                paid,
                wallet.getCashBalance(),
                wallet.getBankBalance(),
                character.getTotalFinesPaid(),
                msg
        );
    }

    private Character getOwnedCharacter(String email, UUID characterId) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Compte introuvable."));
        Player player = playerRepository.findByAccountId(account.getId())
                .orElseThrow(() -> new IllegalStateException("Joueur introuvable."));
        return characterRepository.findByIdAndPlayerId(characterId, player.getId())
                .orElseThrow(() -> new IllegalArgumentException("Personnage introuvable ou accès refusé."));
    }
}
