package ci.abidjan.citylife.tutorial;

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
import ci.abidjan.citylife.security.SecurityAuditLog;
import ci.abidjan.citylife.security.SecurityAuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TutorialServiceTest {

    @Mock private AccountRepository accountRepository;
    @Mock private PlayerRepository playerRepository;
    @Mock private CharacterRepository characterRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private SecurityAuditService auditService;

    @InjectMocks
    private TutorialService tutorialService;

    private Account account;
    private Player player;
    private Character character;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        UUID accountId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        UUID characterId = UUID.randomUUID();

        account = Account.builder()
                .id(accountId)
                .email("kouassi@abidjan.ci")
                .passwordHash("hashed")
                .build();

        player = Player.builder()
                .id(playerId)
                .account(account)
                .username("Kouassi225")
                .level(1)
                .experiencePoints(0L)
                .tutorialCompleted(false)
                .tutorialStep(0)
                .build();

        character = Character.builder()
                .id(characterId)
                .player(player)
                .firstName("Kouassi")
                .lastName("Abidjan")
                .gender(Character.Gender.MALE)
                .build();

        wallet = Wallet.builder()
                .id(UUID.randomUUID())
                .character(character)
                .cashBalance(25_000L)
                .bankBalance(50_000L)
                .frozen(false)
                .build();
    }

    @Test
    void updateStep_advancesMonotonicallyAndSaves() {
        when(accountRepository.findByEmail("kouassi@abidjan.ci")).thenReturn(Optional.of(account));
        when(playerRepository.findByAccountId(account.getId())).thenReturn(Optional.of(player));
        when(characterRepository.findByPlayerIdAndActiveTrue(player.getId())).thenReturn(List.of(character));
        when(walletRepository.findByCharacterId(character.getId())).thenReturn(Optional.of(wallet));

        TutorialStatusResponse res = tutorialService.updateStep("kouassi@abidjan.ci", new UpdateTutorialStepRequest(3));

        assertThat(res.tutorialCompleted()).isFalse();
        assertThat(res.tutorialStep()).isEqualTo(3);
        assertThat(res.totalSteps()).isEqualTo(TutorialService.TOTAL_STEPS);
        assertThat(player.getTutorialStep()).isEqualTo(3);
        verify(playerRepository).save(player);
    }

    @Test
    void completeTutorial_creditsUniqueRewardAndCreatesSystemGrantTransaction() {
        when(accountRepository.findByEmail("kouassi@abidjan.ci")).thenReturn(Optional.of(account));
        when(playerRepository.findByAccountId(account.getId())).thenReturn(Optional.of(player));
        when(transactionRepository.existsByIdempotencyKey("tut-key-001")).thenReturn(false);
        when(characterRepository.findByIdAndPlayerId(character.getId(), player.getId())).thenReturn(Optional.of(character));
        when(walletRepository.findByCharacterId(character.getId())).thenReturn(Optional.of(wallet));

        CompleteTutorialRequest req = new CompleteTutorialRequest(character.getId(), "tut-key-001", false);
        TutorialStatusResponse res = tutorialService.completeTutorial("kouassi@abidjan.ci", req, "127.0.0.1");

        assertThat(res.tutorialCompleted()).isTrue();
        assertThat(res.tutorialStep()).isEqualTo(TutorialService.TOTAL_STEPS);
        assertThat(res.rewardGranted()).isTrue();
        assertThat(res.rewardCash()).isEqualTo(10_000L);
        assertThat(res.rewardXp()).isEqualTo(50L);
        assertThat(res.cashBalance()).isEqualTo(35_000L);
        assertThat(res.totalXp()).isEqualTo(50L);
        assertThat(player.isTutorialCompleted()).isTrue();
        assertThat(player.getTutorialCompletedAt()).isNotNull();

        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txCaptor.capture());
        Transaction savedTx = txCaptor.getValue();
        assertThat(savedTx.getIdempotencyKey()).isEqualTo("tut-key-001");
        assertThat(savedTx.getAmount()).isEqualTo(10_000L);
        assertThat(savedTx.getTransactionType()).isEqualTo(Transaction.Type.SYSTEM_GRANT);
        assertThat(savedTx.getTargetWallet()).isEqualTo(wallet);

        verify(auditService).log(eq(account), eq("TUTORIAL_COMPLETED"), eq(SecurityAuditLog.Severity.INFO), eq("127.0.0.1"), any());
    }

    @Test
    void completeTutorial_whenAlreadyCompleted_doesNotCreditTwice() {
        player.setTutorialCompleted(true);
        player.setTutorialStep(TutorialService.TOTAL_STEPS);
        player.setTutorialCompletedAt(Instant.now());

        when(accountRepository.findByEmail("kouassi@abidjan.ci")).thenReturn(Optional.of(account));
        when(playerRepository.findByAccountId(account.getId())).thenReturn(Optional.of(player));
        when(characterRepository.findByIdAndPlayerId(character.getId(), player.getId())).thenReturn(Optional.of(character));
        when(walletRepository.findByCharacterId(character.getId())).thenReturn(Optional.of(wallet));

        CompleteTutorialRequest req = new CompleteTutorialRequest(character.getId(), "tut-key-002", false);
        TutorialStatusResponse res = tutorialService.completeTutorial("kouassi@abidjan.ci", req, "127.0.0.1");

        assertThat(res.tutorialCompleted()).isTrue();
        assertThat(res.rewardGranted()).isFalse();
        assertThat(res.rewardCash()).isZero();
        assertThat(wallet.getCashBalance()).isEqualTo(25_000L);
        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void completeTutorial_withDuplicateIdempotencyKey_throwsConflict() {
        when(accountRepository.findByEmail("kouassi@abidjan.ci")).thenReturn(Optional.of(account));
        when(playerRepository.findByAccountId(account.getId())).thenReturn(Optional.of(player));
        when(transactionRepository.existsByIdempotencyKey("dup-key")).thenReturn(true);

        CompleteTutorialRequest req = new CompleteTutorialRequest(character.getId(), "dup-key", false);

        assertThatThrownBy(() -> tutorialService.completeTutorial("kouassi@abidjan.ci", req, "127.0.0.1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("doublon");

        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void completeTutorial_whenSkipped_marksCompletedWithZeroReward() {
        player.setTutorialStep(2);
        when(accountRepository.findByEmail("kouassi@abidjan.ci")).thenReturn(Optional.of(account));
        when(playerRepository.findByAccountId(account.getId())).thenReturn(Optional.of(player));
        when(characterRepository.findByIdAndPlayerId(character.getId(), player.getId())).thenReturn(Optional.of(character));
        when(walletRepository.findByCharacterId(character.getId())).thenReturn(Optional.of(wallet));

        CompleteTutorialRequest req = new CompleteTutorialRequest(character.getId(), "skip-key", true);
        TutorialStatusResponse res = tutorialService.completeTutorial("kouassi@abidjan.ci", req, "127.0.0.1");

        assertThat(res.tutorialCompleted()).isTrue();
        assertThat(res.rewardGranted()).isFalse();
        assertThat(res.rewardCash()).isZero();
        assertThat(wallet.getCashBalance()).isEqualTo(25_000L);
        assertThat(player.getExperiencePoints()).isZero();
        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
        verify(auditService).log(eq(account), eq("TUTORIAL_SKIPPED"), eq(SecurityAuditLog.Severity.INFO), eq("127.0.0.1"), any());
    }
}
