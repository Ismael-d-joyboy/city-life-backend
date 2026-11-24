package ci.abidjan.citylife.rp;

import ci.abidjan.citylife.auth.Account;
import ci.abidjan.citylife.auth.AccountRepository;
import ci.abidjan.citylife.economy.Transaction;
import ci.abidjan.citylife.economy.TransactionRepository;
import ci.abidjan.citylife.economy.Wallet;
import ci.abidjan.citylife.economy.WalletRepository;
import ci.abidjan.citylife.inventory.*;
import ci.abidjan.citylife.player.*;
import ci.abidjan.citylife.player.Character;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Phase16RpServiceTest {

    @Mock private AccountRepository accountRepository;
    @Mock private PlayerRepository playerRepository;
    @Mock private CharacterRepository characterRepository;
    @Mock private CharacterAppearanceRepository appearanceRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private ItemRepository itemRepository;
    @Mock private InventoryItemRepository inventoryItemRepository;

    @InjectMocks private CharacterService characterService;
    @InjectMocks private InventoryService inventoryService;

    private Account account;
    private Player player;
    private Character character;
    private Wallet wallet;
    private CharacterAppearance appearance;

    @BeforeEach
    void setUp() {
        account = Account.builder().id(UUID.randomUUID()).email("rp@abidjan.ci").build();
        player = Player.builder().id(UUID.randomUUID()).account(account).username("Kouassi").build();
        character = Character.builder()
                .id(UUID.randomUUID())
                .player(player)
                .firstName("Kouassi")
                .lastName("Yao")
                .gender(Character.Gender.MALE)
                .health(35)
                .stamina(40)
                .hunger(30)
                .thirst(45)
                .hospitalVisits(0)
                .build();
        wallet = Wallet.builder().id(UUID.randomUUID()).character(character).cashBalance(10000L).bankBalance(50000L).currency("XOF").build();
        appearance = CharacterAppearance.builder()
                .id(UUID.randomUUID())
                .character(character)
                .skinTone("DEFAULT")
                .hairStyle("SHORT_01")
                .outfitData(new HashMap<>(Map.of("outfitIndex", 0, "headgearIndex", 0)))
                .build();
    }

    @Test
    void updateAppearance_mergesOutfitDataAndPersists() {
        when(accountRepository.findByEmail("rp@abidjan.ci")).thenReturn(Optional.of(account));
        when(playerRepository.findByAccountId(account.getId())).thenReturn(Optional.of(player));
        when(characterRepository.findByIdAndPlayerId(character.getId(), player.getId())).thenReturn(Optional.of(character));
        when(appearanceRepository.findByCharacterId(character.getId())).thenReturn(Optional.of(appearance));
        when(appearanceRepository.save(any(CharacterAppearance.class))).thenAnswer(inv -> inv.getArgument(0));
        when(walletRepository.findByCharacterId(character.getId())).thenReturn(Optional.of(wallet));

        UpdateAppearanceRequest req = new UpdateAppearanceRequest("DARK_02", "BRAIDS_01", null,
                Map.of("outfitIndex", 2, "headgearIndex", 3, "outfitName", "Boubou Traditionnel"));
        CharacterResponse res = characterService.updateAppearance("rp@abidjan.ci", character.getId(), req);

        assertEquals("DARK_02", res.skinTone());
        assertEquals("BRAIDS_01", res.hairStyle());
        assertEquals(2, res.outfitData().get("outfitIndex"));
        assertEquals(3, res.outfitData().get("headgearIndex"));
    }

    @Test
    void hospitalCare_restoresVitalsAndChargesFeeWhenCashSufficient() {
        when(accountRepository.findByEmail("rp@abidjan.ci")).thenReturn(Optional.of(account));
        when(playerRepository.findByAccountId(account.getId())).thenReturn(Optional.of(player));
        when(characterRepository.findByIdAndPlayerId(character.getId(), player.getId())).thenReturn(Optional.of(character));
        when(walletRepository.findByCharacterId(character.getId())).thenReturn(Optional.of(wallet));
        when(transactionRepository.existsByIdempotencyKey("chu-key-1")).thenReturn(false);
        when(characterRepository.save(any(Character.class))).thenAnswer(inv -> inv.getArgument(0));

        HospitalCareResponse res = characterService.hospitalCare("rp@abidjan.ci", character.getId(),
                new HospitalCareRequest("chu-key-1", true));

        assertEquals(100, res.health());
        assertEquals(100, res.stamina());
        assertEquals(75, res.hunger());
        assertEquals(2000L, res.feeCharged());
        assertEquals(8000L, res.cashBalance());
        assertEquals(1, res.hospitalVisits());
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void consumeItem_decrementsQuantityAndRestoresHunger() {
        Item garba = Item.builder()
                .id(UUID.randomUUID())
                .itemCode("FOOD_GARBA")
                .name("Plat de Garba")
                .category(Item.Category.FOOD)
                .effects(Map.of("hunger_restore", 45, "stamina_restore", 10))
                .build();
        InventoryItem invItem = InventoryItem.builder()
                .id(UUID.randomUUID())
                .character(character)
                .item(garba)
                .quantity(2)
                .build();

        when(accountRepository.findByEmail("rp@abidjan.ci")).thenReturn(Optional.of(account));
        when(playerRepository.findByAccountId(account.getId())).thenReturn(Optional.of(player));
        when(characterRepository.findByIdAndPlayerId(character.getId(), player.getId())).thenReturn(Optional.of(character));
        when(itemRepository.findByItemCode("FOOD_GARBA")).thenReturn(Optional.of(garba));
        when(inventoryItemRepository.findByCharacterIdAndItemId(character.getId(), garba.getId())).thenReturn(Optional.of(invItem));

        ConsumeItemResponse res = inventoryService.consumeItem("rp@abidjan.ci", character.getId(),
                new ConsumeItemRequest("FOOD_GARBA", 1));

        assertEquals(1, res.remainingQuantity());
        assertEquals(75, res.hunger());
        assertEquals(50, res.stamina());
        verify(inventoryItemRepository, times(1)).save(invItem);
    }
}
