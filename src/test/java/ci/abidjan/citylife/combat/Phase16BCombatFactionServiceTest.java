package ci.abidjan.citylife.combat;

import ci.abidjan.citylife.auth.Account;
import ci.abidjan.citylife.auth.AccountRepository;
import ci.abidjan.citylife.economy.Transaction;
import ci.abidjan.citylife.economy.TransactionRepository;
import ci.abidjan.citylife.economy.Wallet;
import ci.abidjan.citylife.economy.WalletRepository;
import ci.abidjan.citylife.factions.*;
import ci.abidjan.citylife.inventory.InventoryItem;
import ci.abidjan.citylife.inventory.InventoryItemRepository;
import ci.abidjan.citylife.inventory.Item;
import ci.abidjan.citylife.inventory.ItemRepository;
import ci.abidjan.citylife.player.Character;
import ci.abidjan.citylife.player.CharacterRepository;
import ci.abidjan.citylife.player.Player;
import ci.abidjan.citylife.player.PlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Phase16BCombatFactionServiceTest {

    @Mock private AccountRepository accountRepository;
    @Mock private PlayerRepository playerRepository;
    @Mock private CharacterRepository characterRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private ItemRepository itemRepository;
    @Mock private InventoryItemRepository inventoryItemRepository;

    @Mock private FactionRepository factionRepository;
    @Mock private FactionMemberRepository factionMemberRepository;
    @Mock private FactionTerritoryRepository territoryRepository;

    @InjectMocks private ArmoryService armoryService;
    @InjectMocks private FactionService factionService;

    private final String email = "abidjan@citylife.ci";
    private Account account;
    private Player player;
    private Character character;
    private Wallet wallet;

    @BeforeEach
    void setup() {
        account = Account.builder().id(UUID.randomUUID()).email(email).build();
        player = Player.builder().id(UUID.randomUUID()).account(account).username("Kouame").build();
        character = Character.builder()
                .id(UUID.randomUUID())
                .player(player)
                .firstName("Kouame")
                .lastName("Yao")
                .gender(Character.Gender.MALE)
                .hasWeaponLicense(false)
                .totalFinesPaid(0L)
                .build();
        wallet = Wallet.builder()
                .id(UUID.randomUUID())
                .character(character)
                .cashBalance(60000L)
                .bankBalance(50000L)
                .build();

        when(accountRepository.findByEmail(email)).thenReturn(Optional.of(account));
        when(playerRepository.findByAccountId(account.getId())).thenReturn(Optional.of(player));
        when(characterRepository.findByIdAndPlayerId(character.getId(), player.getId())).thenReturn(Optional.of(character));
    }

    @Test
    void buyWeaponWithoutLicenseIsBlockedThenSucceedsAfterPermit() {
        ArmoryBuyRequest pistolReq = new ArmoryBuyRequest(character.getId(), "WEAPON_PISTOL_9MM", 1, "buy-pistol-1");
        when(transactionRepository.existsByIdempotencyKey("buy-pistol-1")).thenReturn(false);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> armoryService.buyArmoryItem(email, pistolReq));
        assertTrue(ex.getMessage().contains("Permis"));

        // Acheter le permis (15 000 FCFA)
        when(walletRepository.findByCharacterId(character.getId())).thenReturn(Optional.of(wallet));
        when(transactionRepository.existsByIdempotencyKey("lic-1")).thenReturn(false);
        when(itemRepository.findByItemCode(ArmoryService.LICENSE_CODE)).thenReturn(Optional.of(
                Item.builder().id(UUID.randomUUID()).itemCode(ArmoryService.LICENSE_CODE).name("Permis").basePrice(15000L).build()
        ));

        ArmoryResponse licResp = armoryService.buyWeaponLicense(email, character.getId(), "lic-1");
        assertTrue(licResp.hasWeaponLicense());
        assertEquals(45000L, licResp.cashBalance());

        // Acheter le Pistolet 9mm Arcade (25 000 FCFA)
        Item pistol = Item.builder()
                .id(UUID.randomUUID())
                .itemCode("WEAPON_PISTOL_9MM")
                .name("Pistolet 9mm Arcade")
                .category(Item.Category.TOOL)
                .basePrice(25000L)
                .stackable(false)
                .maxStack(1)
                .tradable(true)
                .build();
        when(itemRepository.findByItemCode("WEAPON_PISTOL_9MM")).thenReturn(Optional.of(pistol));
        when(inventoryItemRepository.findByCharacterIdAndItemId(character.getId(), pistol.getId())).thenReturn(Optional.empty());
        when(inventoryItemRepository.save(any(InventoryItem.class))).thenAnswer(inv -> inv.getArgument(0));

        ArmoryResponse buyResp = armoryService.buyArmoryItem(email, pistolReq);
        assertEquals(20000L, buyResp.cashBalance());
        assertEquals(1, buyResp.quantityOwned());
        verify(transactionRepository, times(2)).save(any(Transaction.class));
    }

    @Test
    void policeFineDebitsExpectedAmountAndClearsWantedLevel() {
        character.setWantedLevel(2);
        when(walletRepository.findByCharacterId(character.getId())).thenReturn(Optional.of(wallet));
        when(transactionRepository.existsByIdempotencyKey("fine-1")).thenReturn(false);

        PoliceFineResponse resp = armoryService.applyPoliceFine(
                email,
                new PoliceFineRequest(character.getId(), 2, "Tir illégal hors Safe Zone", true, "fine-1")
        );

        assertEquals(10000L, resp.finePaidFcfa());
        assertEquals(50000L, resp.cashBalance());
        assertEquals(0, character.getWantedLevel());
        assertEquals(10000L, character.getTotalFinesPaid());
    }

    @Test
    void createFactionAndCaptureTerritoryPersistsOwner() {
        when(factionMemberRepository.findByCharacterId(character.getId())).thenReturn(Optional.empty());
        when(factionRepository.existsByTagIgnoreCase("LAG")).thenReturn(false);
        when(factionRepository.existsByCodeIgnoreCase("FAC_LAG")).thenReturn(false);
        when(factionRepository.existsByNameIgnoreCase("Gardiens Lagune")).thenReturn(false);

        Faction savedFaction = Faction.builder()
                .id(UUID.randomUUID())
                .code("FAC_LAG")
                .tag("LAG")
                .name("Gardiens Lagune")
                .colorHex("#F5C518")
                .leader(character)
                .build();
        when(factionRepository.save(any(Faction.class))).thenReturn(savedFaction);
        when(factionMemberRepository.save(any(FactionMember.class))).thenAnswer(inv -> inv.getArgument(0));

        FactionResponse created = factionService.createFaction(
                email,
                new CreateFactionRequest(character.getId(), "lag", "Gardiens Lagune", "#F5C518")
        );
        assertEquals("LAG", created.tag());
        assertEquals("LEADER", created.myRole());

        FactionMember member = FactionMember.builder().faction(savedFaction).character(character).role(FactionMember.Role.LEADER).build();
        FactionTerritory vridi = FactionTerritory.builder().zoneCode("ZONE_PORT_VRIDI").zoneName("Port de Vridi").build();
        when(factionMemberRepository.findByCharacterId(character.getId())).thenReturn(Optional.of(member));
        when(territoryRepository.findById("ZONE_PORT_VRIDI")).thenReturn(Optional.of(vridi));
        when(territoryRepository.save(any(FactionTerritory.class))).thenAnswer(inv -> inv.getArgument(0));

        FactionResponse.TerritoryResponse captured = factionService.captureTerritory(email, character.getId(), "ZONE_PORT_VRIDI");
        assertEquals("LAG", captured.ownerFactionTag());
        assertNotNull(captured.capturedAt());
    }
}
