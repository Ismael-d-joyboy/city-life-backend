package ci.abidjan.citylife.moderation;

import ci.abidjan.citylife.auth.Account;
import ci.abidjan.citylife.auth.AccountRepository;
import ci.abidjan.citylife.economy.*;
import ci.abidjan.citylife.player.Character;
import ci.abidjan.citylife.player.CharacterRepository;
import ci.abidjan.citylife.player.Player;
import ci.abidjan.citylife.player.PlayerRepository;
import ci.abidjan.citylife.security.SecurityAuditService;
import ci.abidjan.citylife.vehicles.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Phase16CJobsFuelModerationTest {

    @Mock private AccountRepository accountRepository;
    @Mock private PlayerRepository playerRepository;
    @Mock private CharacterRepository characterRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private VehicleRepository vehicleRepository;
    @Mock private PlayerVehicleRepository playerVehicleRepository;
    @Mock private PlayerReportRepository reportRepository;
    @Mock private SecurityAuditService auditService;

    @InjectMocks private JobPayoutService jobPayoutService;
    @InjectMocks private VehicleService vehicleService;
    @InjectMocks private ModerationService moderationService;

    private final String email = "abidjan16c@citylife.ci";
    private Account account;
    private Player player;
    private Character character;
    private Wallet wallet;

    @BeforeEach
    void setup() {
        account = Account.builder().id(UUID.randomUUID()).email(email).role(Account.Role.MODERATOR).build();
        player = Player.builder().id(UUID.randomUUID()).account(account).username("AyaAbidjan").experiencePoints(100L).build();
        character = Character.builder().id(UUID.randomUUID()).player(player).firstName("Aya").lastName("Kone").gender(Character.Gender.FEMALE).build();
        wallet = Wallet.builder().id(UUID.randomUUID()).character(character).cashBalance(20000L).bankBalance(50000L).build();
    }

    @Test
    void deliveryAndGbakaPayoutsCreditCashAndXp() {
        when(accountRepository.findByEmail(email)).thenReturn(Optional.of(account));
        when(playerRepository.findByAccountId(account.getId())).thenReturn(Optional.of(player));
        when(characterRepository.findByIdAndPlayerId(character.getId(), player.getId())).thenReturn(Optional.of(character));
        when(walletRepository.findByCharacterId(character.getId())).thenReturn(Optional.of(wallet));
        when(transactionRepository.existsByIdempotencyKey("deliv-1")).thenReturn(false);

        JobPayoutResponse resp = jobPayoutService.claimPayout(
                email,
                new JobPayoutRequest(character.getId(), JobPayoutRequest.JobActionType.DELIVERY_DROP, "deliv-1"),
                "127.0.0.1"
        );
        assertEquals(3000L, resp.rewardCash());
        assertEquals(23000L, resp.newCashBalance());
        assertEquals(130L, resp.totalXp());
        assertEquals(2000L, JobPayoutRequest.JobActionType.GBAKA_STOP.rewardCash);
    }

    @Test
    void refuelIvoireOilDebitsProportionallyAndRestoresFuel100() {
        when(accountRepository.findByEmail(email)).thenReturn(Optional.of(account));
        when(playerRepository.findByAccountId(account.getId())).thenReturn(Optional.of(player));
        when(characterRepository.findByIdAndPlayerId(character.getId(), player.getId())).thenReturn(Optional.of(character));
        when(walletRepository.findByCharacterId(character.getId())).thenReturn(Optional.of(wallet));
        when(transactionRepository.existsByIdempotencyKey("fuel-1")).thenReturn(false);

        PlayerVehicle pv = PlayerVehicle.builder()
                .id(UUID.randomUUID())
                .character(character)
                .licensePlate("4821AB01")
                .fuelLevel(50)
                .build();
        when(playerVehicleRepository.findByCharacterId(character.getId())).thenReturn(List.of(pv));

        RefuelResponse resp = vehicleService.refuelVehicle(
                email,
                character.getId(),
                new RefuelRequest(null, 50, "fuel-1")
        );

        assertEquals(100, resp.fuelLevel());
        assertEquals(1500L, resp.chargedFcfa());
        assertEquals(18500L, resp.cashBalance());
        assertEquals(100, pv.getFuelLevel());
    }

    @Test
    void submitResolveReportAndMutePlayerWorkProperly() {
        when(accountRepository.findByEmail(email)).thenReturn(Optional.of(account));
        when(playerRepository.findByAccountId(account.getId())).thenReturn(Optional.of(player));

        UUID reportId = UUID.randomUUID();
        when(reportRepository.save(any(PlayerReport.class))).thenAnswer(inv -> {
            PlayerReport r = inv.getArgument(0);
            if (r.getId() == null) r.setId(reportId);
            return r;
        });

        ReportResponse created = moderationService.createReport(
                email,
                new CreateReportRequest("Troll99", PlayerReport.ReasonCode.INSULTES, "Propos déplacés"),
                "127.0.0.1"
        );
        assertEquals(PlayerReport.Status.OPEN, created.status());

        PlayerReport stored = PlayerReport.builder()
                .id(reportId)
                .reporter(player)
                .targetUsername("Troll99")
                .reasonCode(PlayerReport.ReasonCode.INSULTES)
                .status(PlayerReport.Status.OPEN)
                .build();
        when(reportRepository.findById(reportId)).thenReturn(Optional.of(stored));

        ReportResponse resolved = moderationService.resolveReport(
                email,
                reportId,
                new ResolveReportRequest(PlayerReport.Status.RESOLVED, "Avertissement envoyé"),
                "127.0.0.1"
        );
        assertEquals(PlayerReport.Status.RESOLVED, resolved.status());
        assertNotNull(resolved.resolvedAt());

        Player target = Player.builder().id(UUID.randomUUID()).username("Troll99").build();
        when(playerRepository.findByUsernameIgnoreCase("Troll99")).thenReturn(Optional.of(target));
        when(reportRepository.findByStatusOrderByCreatedAtDesc(PlayerReport.Status.OPEN)).thenReturn(List.of(stored));
        List<ReportResponse> openList = moderationService.listReports(PlayerReport.Status.OPEN);
        assertEquals(1, openList.size());
        assertEquals(target.getId(), openList.get(0).targetPlayerId());

        when(playerRepository.findById(target.getId())).thenReturn(Optional.of(target));
        Map<String, Object> muteRes = moderationService.mutePlayer(
                email,
                target.getId(),
                new MutePlayerRequest(30, "Insultes répétées"),
                "127.0.0.1"
        );
        assertNotNull(target.getMutedUntil());
        assertEquals("Troll99", muteRes.get("username"));
    }
}
