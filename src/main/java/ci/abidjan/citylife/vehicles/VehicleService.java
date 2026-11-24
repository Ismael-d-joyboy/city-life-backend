package ci.abidjan.citylife.vehicles;

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

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private final AccountRepository accountRepository;
    private final PlayerRepository playerRepository;
    private final CharacterRepository characterRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final VehicleRepository vehicleRepository;
    private final PlayerVehicleRepository playerVehicleRepository;

    private final SecureRandom random = new SecureRandom();

    @Transactional(readOnly = true)
    public List<Vehicle> getCatalog() {
        return vehicleRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<PlayerVehicleResponse> getMyVehicles(String email, UUID characterId) {
        Character character = getOwnedCharacter(email, characterId);
        return playerVehicleRepository.findByCharacterId(character.getId())
                .stream()
                .map(PlayerVehicleResponse::from)
                .toList();
    }

    @Transactional
    public PlayerVehicleResponse buyVehicle(String email, UUID characterId, BuyVehicleRequest request) {
        if (transactionRepository.existsByIdempotencyKey(request.idempotencyKey())) {
            throw new IllegalArgumentException("Achat déjà traité (doublon bloqué).");
        }

        Character character = getOwnedCharacter(email, characterId);
        Wallet wallet = walletRepository.findByCharacterId(character.getId())
                .orElseThrow(() -> new IllegalStateException("Portefeuille introuvable."));

        if (wallet.isFrozen()) {
            throw new IllegalStateException("Portefeuille gelé.");
        }

        Vehicle vehicle = vehicleRepository.findByVehicleCode(request.vehicleCode().trim())
                .orElseThrow(() -> new IllegalArgumentException("Véhicule introuvable : " + request.vehicleCode()));

        if (wallet.getBankBalance() < vehicle.getBasePrice()) {
            throw new IllegalStateException("Solde bancaire insuffisant (" + vehicle.getBasePrice() + " FCFA requis).");
        }

        wallet.setBankBalance(wallet.getBankBalance() - vehicle.getBasePrice());
        walletRepository.save(wallet);

        String plate = generateIvorianPlate();
        PlayerVehicle playerVehicle = PlayerVehicle.builder()
                .character(character)
                .vehicle(vehicle)
                .licensePlate(plate)
                .colorHex(request.colorHex() != null ? request.colorHex() : "#FFFFFF")
                .district(character.getDistrict().name())
                .build();
        playerVehicle = playerVehicleRepository.save(playerVehicle);

        transactionRepository.save(Transaction.builder()
                .idempotencyKey(request.idempotencyKey())
                .sourceWallet(wallet)
                .amount(vehicle.getBasePrice())
                .transactionType(Transaction.Type.PURCHASE)
                .status(Transaction.Status.COMPLETED)
                .description("Achat véhicule : " + vehicle.getName() + " (" + plate + ")")
                .metadata(Map.of("vehicleCode", vehicle.getVehicleCode(), "licensePlate", plate))
                .build());

        return PlayerVehicleResponse.from(playerVehicle);
    }

    @Transactional
    public RefuelResponse refuelVehicle(String email, UUID characterId, RefuelRequest request) {
        Character character = getOwnedCharacter(email, characterId);
        Wallet wallet = walletRepository.findByCharacterId(character.getId())
                .orElseThrow(() -> new IllegalStateException("Portefeuille introuvable."));

        int currentFuel = Math.max(0, Math.min(100, request.currentFuelPercent()));
        int missing = 100 - currentFuel;
        if (missing <= 1) {
            return new RefuelResponse(100, 0L, wallet.getCashBalance(), wallet.getBankBalance(),
                    "Réservoir déjà plein à 100 % !");
        }

        // Tarif IVOIRE OIL : 30 FCFA / % (plafonné à 3 000 FCFA le plein complet)
        long cost = Math.max(300L, Math.min(3000L, missing * 30L));
        long charged = 0L;

        if (!transactionRepository.existsByIdempotencyKey(request.idempotencyKey())) {
            if (wallet.isFrozen()) {
                throw new IllegalStateException("Portefeuille gelé.");
            }
            if (wallet.getCashBalance() < cost) {
                throw new IllegalStateException("Cash insuffisant (" + cost + " FCFA requis chez IVOIRE OIL).");
            }

            charged = cost;
            wallet.setCashBalance(wallet.getCashBalance() - charged);
            walletRepository.save(wallet);

            transactionRepository.save(Transaction.builder()
                    .idempotencyKey(request.idempotencyKey())
                    .sourceWallet(wallet)
                    .amount(charged)
                    .transactionType(Transaction.Type.PURCHASE)
                    .status(Transaction.Status.COMPLETED)
                    .description("Station IVOIRE OIL : plein de carburant (" + missing + " %)")
                    .metadata(Map.of("service", "IVOIRE_OIL_REFUEL", "missingPercent", missing))
                    .build());

            if (request.playerVehicleId() != null) {
                playerVehicleRepository.findByIdAndCharacterId(request.playerVehicleId(), character.getId())
                        .ifPresent(pv -> {
                            pv.setFuelLevel(100);
                            playerVehicleRepository.save(pv);
                        });
            } else {
                List<PlayerVehicle> owned = playerVehicleRepository.findByCharacterId(character.getId());
                if (!owned.isEmpty()) {
                    PlayerVehicle pv = owned.get(0);
                    pv.setFuelLevel(100);
                    playerVehicleRepository.save(pv);
                }
            }
        }

        return new RefuelResponse(
                100,
                charged,
                wallet.getCashBalance(),
                wallet.getBankBalance(),
                "Plein IVOIRE OIL effectué (" + charged + " FCFA) — Réservoir à 100 % !"
        );
    }

    @Transactional
    public PlayerVehicleResponse customizeVehicle(String email, UUID characterId, UUID vehicleId, CustomizeVehicleRequest request) {
        Character character = getOwnedCharacter(email, characterId);
        PlayerVehicle pv = playerVehicleRepository.findByIdAndCharacterId(vehicleId, character.getId())
                .orElseThrow(() -> new IllegalArgumentException("Véhicule introuvable dans votre garage."));
        pv.setColorHex(request.colorHex().trim().toUpperCase());
        pv = playerVehicleRepository.save(pv);
        return PlayerVehicleResponse.from(pv);
    }

    private String generateIvorianPlate() {
        String plate;
        do {
            int digits = 1000 + random.nextInt(9000);
            char l1 = (char) ('A' + random.nextInt(26));
            char l2 = (char) ('A' + random.nextInt(26));
            plate = digits + "" + l1 + l2 + "01";
        } while (playerVehicleRepository.existsByLicensePlate(plate));
        return plate;
    }

    private Character getOwnedCharacter(String email, UUID characterId) {
        Account account = accountRepository.findByEmail(email).orElseThrow();
        Player player = playerRepository.findByAccountId(account.getId()).orElseThrow();
        return characterRepository.findByIdAndPlayerId(characterId, player.getId())
                .orElseThrow(() -> new IllegalArgumentException("Personnage introuvable ou accès refusé."));
    }
}