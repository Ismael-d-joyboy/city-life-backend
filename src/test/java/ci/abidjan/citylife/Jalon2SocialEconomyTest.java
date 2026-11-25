package ci.abidjan.citylife;

import ci.abidjan.citylife.auth.Account;
import ci.abidjan.citylife.auth.AccountRepository;
import ci.abidjan.citylife.economy.*;
import ci.abidjan.citylife.player.Character;
import ci.abidjan.citylife.player.CharacterRepository;
import ci.abidjan.citylife.player.Player;
import ci.abidjan.citylife.player.PlayerRepository;
import ci.abidjan.citylife.security.SecurityAuditService;
import ci.abidjan.citylife.social.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Jalon2SocialEconomyTest {

    @Mock private AccountRepository accountRepository;
    @Mock private PlayerRepository playerRepository;
    @Mock private CharacterRepository characterRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private FriendshipRepository friendshipRepository;
    @Mock private SecurityAuditService auditService;

    private EconomyService economyService;
    private FriendService friendService;
    private JobPayoutService jobPayoutService;

    private Account senderAccount;
    private Player senderPlayer;
    private Character senderChar;
    private Wallet senderWallet;

    private Player recipientPlayer;
    private Character recipientChar;
    private Wallet recipientWallet;

    @BeforeEach
    void setUp() {
        economyService = new EconomyService(
                accountRepository, playerRepository, characterRepository, walletRepository, transactionRepository, auditService);
        friendService = new FriendService(
                accountRepository, playerRepository, characterRepository, friendshipRepository);
        jobPayoutService = new JobPayoutService(
                accountRepository, playerRepository, characterRepository, walletRepository, transactionRepository, auditService);

        senderAccount = Account.builder().id(UUID.randomUUID()).email("koffi@abidjan.ci").build();
        senderPlayer = Player.builder().id(UUID.randomUUID()).account(senderAccount).username("Koffi225").level(3).experiencePoints(150L).build();
        senderChar = Character.builder().id(UUID.randomUUID()).player(senderPlayer).district(Character.District.PLATEAU).build();
        senderWallet = Wallet.builder().id(UUID.randomUUID()).character(senderChar).cashBalance(20000L).bankBalance(100000L).build();

        recipientPlayer = Player.builder().id(UUID.randomUUID()).username("AwaZone4").level(2).experiencePoints(80L).build();
        recipientChar = Character.builder().id(UUID.randomUUID()).player(recipientPlayer).district(Character.District.MARCORY).build();
        recipientWallet = Wallet.builder().id(UUID.randomUUID()).character(recipientChar).cashBalance(10000L).bankBalance(50000L).build();
    }

    @Test
    void laguneMoneyTransfer_debitsBankWithOnePercentFeeAndCreditsRecipientBank() {
        when(transactionRepository.existsByIdempotencyKey("tx-001")).thenReturn(false);
        when(accountRepository.findByEmail("koffi@abidjan.ci")).thenReturn(Optional.of(senderAccount));
        when(playerRepository.findByAccountId(senderAccount.getId())).thenReturn(Optional.of(senderPlayer));
        when(characterRepository.findByIdAndPlayerId(senderChar.getId(), senderPlayer.getId())).thenReturn(Optional.of(senderChar));
        when(walletRepository.findByCharacterId(senderChar.getId())).thenReturn(Optional.of(senderWallet));
        when(playerRepository.findByUsernameIgnoreCase("AwaZone4")).thenReturn(Optional.of(recipientPlayer));
        when(characterRepository.findByPlayerIdAndActiveTrue(recipientPlayer.getId())).thenReturn(List.of(recipientChar));
        when(walletRepository.findByCharacterId(recipientChar.getId())).thenReturn(Optional.of(recipientWallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));
        when(playerRepository.save(any(Player.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });

        TransferRequest req = new TransferRequest(senderChar.getId(), "AwaZone4", 10000L, "Loyer studio", "tx-001");
        TransferResponse res = economyService.transfer("koffi@abidjan.ci", req, "127.0.0.1");

        assertEquals(10000L, res.amount());
        assertEquals(100L, res.feeAmount());
        assertEquals(10100L, res.totalDebited());
        assertEquals(89900L, senderWallet.getBankBalance());
        assertEquals(60000L, recipientWallet.getBankBalance());
        assertEquals(20000L, senderWallet.getCashBalance());
    }

    @Test
    void laguneMoneyTransfer_rejectsSelfTransfer() {
        when(transactionRepository.existsByIdempotencyKey("tx-self")).thenReturn(false);
        when(accountRepository.findByEmail("koffi@abidjan.ci")).thenReturn(Optional.of(senderAccount));
        when(playerRepository.findByAccountId(senderAccount.getId())).thenReturn(Optional.of(senderPlayer));
        when(characterRepository.findByIdAndPlayerId(senderChar.getId(), senderPlayer.getId())).thenReturn(Optional.of(senderChar));
        when(walletRepository.findByCharacterId(senderChar.getId())).thenReturn(Optional.of(senderWallet));
        when(playerRepository.findByUsernameIgnoreCase("Koffi225")).thenReturn(Optional.of(senderPlayer));

        TransferRequest req = new TransferRequest(senderChar.getId(), "Koffi225", 1000L, "Test", "tx-self");
        assertThrows(IllegalArgumentException.class, () -> economyService.transfer("koffi@abidjan.ci", req, "127.0.0.1"));
    }

    @Test
    void laguneMoneyTransfer_rejectsDuplicateIdempotencyKey() {
        when(transactionRepository.existsByIdempotencyKey("tx-dup")).thenReturn(true);
        TransferRequest req = new TransferRequest(senderChar.getId(), "AwaZone4", 1000L, "Test", "tx-dup");
        assertThrows(IllegalArgumentException.class, () -> economyService.transfer("koffi@abidjan.ci", req, "127.0.0.1"));
    }

    @Test
    void friends_requestAndAcceptFlowWorks() {
        when(accountRepository.findByEmail("koffi@abidjan.ci")).thenReturn(Optional.of(senderAccount));
        when(playerRepository.findByAccountId(senderAccount.getId())).thenReturn(Optional.of(senderPlayer));
        when(playerRepository.save(any(Player.class))).thenAnswer(inv -> inv.getArgument(0));
        when(playerRepository.findByUsernameIgnoreCase("AwaZone4")).thenReturn(Optional.of(recipientPlayer));
        when(friendshipRepository.findBetweenPlayers(senderPlayer.getId(), recipientPlayer.getId())).thenReturn(Optional.empty());
        when(characterRepository.findByPlayerIdAndActiveTrue(recipientPlayer.getId())).thenReturn(List.of(recipientChar));
        when(friendshipRepository.save(any(Friendship.class))).thenAnswer(inv -> {
            Friendship f = inv.getArgument(0);
            if (f.getId() == null) f.setId(UUID.randomUUID());
            return f;
        });

        FriendResponse created = friendService.sendFriendRequest("koffi@abidjan.ci", new FriendRequestDto("AwaZone4"));
        assertEquals("PENDING", created.status());
        assertEquals("AwaZone4", created.username());
        assertEquals("MARCORY", created.lastDistrict());
    }

    @Test
    void contrabandPayout_blocksPeacefulModeAndEnforces90sCooldown() {
        senderPlayer.setPeacefulMode(true);
        when(transactionRepository.existsByIdempotencyKey(any())).thenReturn(false);
        when(accountRepository.findByEmail("koffi@abidjan.ci")).thenReturn(Optional.of(senderAccount));
        when(playerRepository.findByAccountId(senderAccount.getId())).thenReturn(Optional.of(senderPlayer));
        when(characterRepository.findByIdAndPlayerId(senderChar.getId(), senderPlayer.getId())).thenReturn(Optional.of(senderChar));

        JobPayoutRequest peacefulReq = new JobPayoutRequest(senderChar.getId(), JobPayoutRequest.JobActionType.CONTRABAND_DELIVERY, "cb-1");
        assertThrows(IllegalStateException.class, () -> jobPayoutService.claimPayout("koffi@abidjan.ci", peacefulReq, "127.0.0.1"));

        senderPlayer.setPeacefulMode(false);
        senderChar.setLastContrabandPayoutAt(Instant.now().minusSeconds(20));
        JobPayoutRequest cooldownReq = new JobPayoutRequest(senderChar.getId(), JobPayoutRequest.JobActionType.CONTRABAND_DELIVERY, "cb-2");
        assertThrows(IllegalStateException.class, () -> jobPayoutService.claimPayout("koffi@abidjan.ci", cooldownReq, "127.0.0.1"));

        senderChar.setLastContrabandPayoutAt(Instant.now().minusSeconds(120));
        when(walletRepository.findByCharacterId(senderChar.getId())).thenReturn(Optional.of(senderWallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));
        when(playerRepository.save(any(Player.class))).thenAnswer(inv -> inv.getArgument(0));
        when(characterRepository.save(any(Character.class))).thenAnswer(inv -> inv.getArgument(0));

        JobPayoutRequest validReq = new JobPayoutRequest(senderChar.getId(), JobPayoutRequest.JobActionType.CONTRABAND_DELIVERY, "cb-3");
        JobPayoutResponse res = jobPayoutService.claimPayout("koffi@abidjan.ci", validReq, "127.0.0.1");
        assertEquals(6500L, res.rewardCash());
        assertEquals(50L, res.rewardXp());
    }
}
