package ci.abidjan.citylife.security;

import ci.abidjan.citylife.auth.*;
import ci.abidjan.citylife.combat.ArmoryService;
import ci.abidjan.citylife.combat.PoliceFineRequest;
import ci.abidjan.citylife.combat.PoliceFineResponse;
import ci.abidjan.citylife.economy.*;
import ci.abidjan.citylife.inventory.InventoryItemRepository;
import ci.abidjan.citylife.inventory.ItemRepository;
import ci.abidjan.citylife.moderation.*;
import ci.abidjan.citylife.player.*;
import ci.abidjan.citylife.player.Character;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Jalon0SecurityEconomyTest {

    @Mock private AccountRepository accountRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PlayerRepository playerRepository;
    @Mock private PlayerProfileRepository playerProfileRepository;
    @Mock private CharacterRepository characterRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private ItemRepository itemRepository;
    @Mock private InventoryItemRepository inventoryItemRepository;
    @Mock private PlayerReportRepository reportRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;
    @Mock private SecurityAuditService auditService;
    @Mock private AccountUserDetailsService userDetailsService;

    @InjectMocks private JobPayoutService jobPayoutService;
    @InjectMocks private ArmoryService armoryService;
    @InjectMocks private AuthService authService;
    @InjectMocks private ModerationService moderationService;

    private final String email = "jalon0@citylife.ci";
    private Account account;
    private Player player;
    private Character character;
    private Wallet wallet;

    @BeforeEach
    void setup() {
        account = Account.builder()
                .id(UUID.randomUUID())
                .email(email)
                .passwordHash("$2a$10$hash")
                .role(Account.Role.MODERATOR)
                .status(Account.Status.ACTIVE)
                .build();
        player = Player.builder()
                .id(UUID.randomUUID())
                .account(account)
                .username("KoffiJalon0")
                .level(1)
                .experiencePoints(0L)
                .build();
        character = Character.builder()
                .id(UUID.randomUUID())
                .player(player)
                .firstName("Koffi")
                .lastName("Brou")
                .gender(Character.Gender.MALE)
                .district(Character.District.ADJAME)
                .manawaBagStreak(0)
                .build();
        wallet = Wallet.builder()
                .id(UUID.randomUUID())
                .character(character)
                .cashBalance(0L)
                .bankBalance(50000L)
                .build();
    }

    @Test
    void manawaBonusWithoutFiveBagsIsBlockedAndFiveBagsSucceedsWithLevelUp() {
        when(accountRepository.findByEmail(email)).thenReturn(Optional.of(account));
        when(playerRepository.findByAccountId(account.getId())).thenReturn(Optional.of(player));
        when(characterRepository.findByIdAndPlayerId(character.getId(), player.getId())).thenReturn(Optional.of(character));
        when(transactionRepository.existsByIdempotencyKey(any())).thenReturn(false);

        // Tentative d'exploit MANAWA_BONUS_5 avec 0 sac -> bloquée
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                jobPayoutService.claimPayout(email,
                        new JobPayoutRequest(character.getId(), JobPayoutRequest.JobActionType.MANAWA_BONUS_5, "exploit-1"),
                        "127.0.0.1"));
        assertTrue(ex.getMessage().contains("5 sacs"));

        // Avec 5 sacs validés côté serveur + 85 XP existants -> bonus validé + passage niveau 2
        character.setManawaBagStreak(5);
        player.setExperiencePoints(85L);
        when(walletRepository.findByCharacterId(character.getId())).thenReturn(Optional.of(wallet));

        JobPayoutResponse resp = jobPayoutService.claimPayout(email,
                new JobPayoutRequest(character.getId(), JobPayoutRequest.JobActionType.MANAWA_BONUS_5, "legit-5"),
                "127.0.0.1");
        assertEquals(2500L, resp.rewardCash());
        assertEquals(0, character.getManawaBagStreak());
        assertEquals(2, player.getLevel());
    }

    @Test
    void policeFineDebitsBankWhenCashIsZero() {
        when(accountRepository.findByEmail(email)).thenReturn(Optional.of(account));
        when(playerRepository.findByAccountId(account.getId())).thenReturn(Optional.of(player));
        when(characterRepository.findByIdAndPlayerId(character.getId(), player.getId())).thenReturn(Optional.of(character));
        when(walletRepository.findByCharacterId(character.getId())).thenReturn(Optional.of(wallet));
        when(transactionRepository.existsByIdempotencyKey("fine-bank-1")).thenReturn(false);

        PoliceFineResponse resp = armoryService.applyPoliceFine(email,
                new PoliceFineRequest(character.getId(), 2, "Tir hors Safe Zone", true, "fine-bank-1"));

        assertEquals(10000L, resp.finePaidFcfa());
        assertEquals(0L, resp.cashBalance());
        assertEquals(40000L, resp.bankBalance());
    }

    @Test
    void deleteAccountRequiresCorrectPasswordThenDeletesAndEvictsCache() {
        when(accountRepository.findByEmail(email)).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("WrongPass", account.getPasswordHash())).thenReturn(false);
        when(passwordEncoder.matches("Secret123!", account.getPasswordHash())).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () ->
                authService.deleteAccount(email, new DeleteAccountRequest("WrongPass"), "127.0.0.1"));
        verify(accountRepository, never()).delete(any());

        Map<String, Object> res = authService.deleteAccount(email, new DeleteAccountRequest("Secret123!"), "127.0.0.1");
        assertEquals(true, res.get("deleted"));
        verify(accountRepository).delete(account);
        verify(userDetailsService).evictCache(email);
    }

    @Test
    void moderationBanUnbanAndUnmuteUpdateStatusAndEvictCache() {
        Account targetAcc = Account.builder()
                .id(UUID.randomUUID())
                .email("troll@citylife.ci")
                .role(Account.Role.PLAYER)
                .status(Account.Status.ACTIVE)
                .build();
        Player targetPlayer = Player.builder()
                .id(UUID.randomUUID())
                .account(targetAcc)
                .username("TrollAbidjan")
                .mutedUntil(Instant.now().plusSeconds(3600))
                .build();

        when(accountRepository.findByEmail(email)).thenReturn(Optional.of(account));
        when(playerRepository.findById(targetPlayer.getId())).thenReturn(Optional.of(targetPlayer));

        moderationService.unmutePlayer(email, targetPlayer.getId(), "127.0.0.1");
        assertNull(targetPlayer.getMutedUntil());

        moderationService.banPlayer(email, targetPlayer.getId(), new BanPlayerRequest("Triche avérée"), "127.0.0.1");
        assertEquals(Account.Status.BANNED, targetAcc.getStatus());
        verify(userDetailsService).evictCache("troll@citylife.ci");

        moderationService.unbanPlayer(email, targetPlayer.getId(), "127.0.0.1");
        assertEquals(Account.Status.ACTIVE, targetAcc.getStatus());
    }
}
