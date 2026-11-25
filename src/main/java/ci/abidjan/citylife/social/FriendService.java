package ci.abidjan.citylife.social;

import ci.abidjan.citylife.auth.Account;
import ci.abidjan.citylife.auth.AccountRepository;
import ci.abidjan.citylife.player.Character;
import ci.abidjan.citylife.player.CharacterRepository;
import ci.abidjan.citylife.player.Player;
import ci.abidjan.citylife.player.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FriendService {

    private static final Duration ONLINE_THRESHOLD = Duration.ofMinutes(5);

    private final AccountRepository accountRepository;
    private final PlayerRepository playerRepository;
    private final CharacterRepository characterRepository;
    private final FriendshipRepository friendshipRepository;

    @Transactional
    public FriendResponse sendFriendRequest(String email, FriendRequestDto request) {
        Player me = getCurrentPlayerAndTouchPresence(email);
        String targetName = request.targetUsername().trim();

        Player target = playerRepository.findByUsernameIgnoreCase(targetName)
                .orElseThrow(() -> new IllegalArgumentException("Joueur introuvable : " + targetName));

        if (me.getId().equals(target.getId())) {
            throw new IllegalArgumentException("Vous ne pouvez pas vous ajouter vous-même en ami.");
        }

        Optional<Friendship> existingOpt = friendshipRepository.findBetweenPlayers(me.getId(), target.getId());
        if (existingOpt.isPresent()) {
            Friendship existing = existingOpt.get();
            if (existing.getStatus() == Friendship.Status.ACCEPTED) {
                throw new IllegalStateException("Vous êtes déjà ami avec " + target.getUsername() + ".");
            }
            if (existing.getStatus() == Friendship.Status.BLOCKED) {
                throw new IllegalStateException("Impossible d'envoyer une demande à ce joueur.");
            }
            // Si la cible avait déjà envoyé une demande en attente, on l'accepte automatiquement !
            if (existing.getRequester().getId().equals(target.getId())) {
                existing.setStatus(Friendship.Status.ACCEPTED);
                Friendship saved = friendshipRepository.save(existing);
                return toResponse(saved, target);
            }
            throw new IllegalStateException("Demande d'ami déjà envoyée à " + target.getUsername() + ".");
        }

        Friendship saved = friendshipRepository.save(Friendship.builder()
                .requester(me)
                .addressee(target)
                .status(Friendship.Status.PENDING)
                .build());

        return toResponse(saved, target);
    }

    @Transactional
    public FriendResponse acceptFriendRequest(String email, UUID friendshipId) {
        Player me = getCurrentPlayerAndTouchPresence(email);
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new IllegalArgumentException("Demande d'ami introuvable."));

        if (!friendship.getAddressee().getId().equals(me.getId())) {
            throw new IllegalStateException("Seul le destinataire peut accepter cette demande d'ami.");
        }
        if (friendship.getStatus() == Friendship.Status.ACCEPTED) {
            return toResponse(friendship, friendship.getRequester());
        }
        if (friendship.getStatus() != Friendship.Status.PENDING) {
            throw new IllegalStateException("Cette demande d'ami n'est plus en attente.");
        }

        friendship.setStatus(Friendship.Status.ACCEPTED);
        Friendship saved = friendshipRepository.save(friendship);
        return toResponse(saved, saved.getRequester());
    }

    @Transactional
    public void removeFriendship(String email, UUID friendshipId) {
        Player me = getCurrentPlayerAndTouchPresence(email);
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new IllegalArgumentException("Relation d'amitié introuvable."));

        boolean isParticipant = friendship.getRequester().getId().equals(me.getId())
                || friendship.getAddressee().getId().equals(me.getId());
        if (!isParticipant) {
            throw new IllegalStateException("Accès refusé à cette relation d'amitié.");
        }
        friendshipRepository.delete(friendship);
    }

    @Transactional
    public FriendListResponse listFriends(String email) {
        Player me = getCurrentPlayerAndTouchPresence(email);
        List<Friendship> all = friendshipRepository.findAllForPlayer(me.getId());

        List<FriendResponse> accepted = new ArrayList<>();
        List<FriendResponse> incoming = new ArrayList<>();
        List<FriendResponse> outgoing = new ArrayList<>();

        for (Friendship f : all) {
            if (f.getStatus() == Friendship.Status.BLOCKED) {
                continue;
            }
            boolean iAmRequester = f.getRequester().getId().equals(me.getId());
            Player other = iAmRequester ? f.getAddressee() : f.getRequester();
            FriendResponse dto = toResponse(f, other);

            if (f.getStatus() == Friendship.Status.ACCEPTED) {
                accepted.add(dto);
            } else if (iAmRequester) {
                outgoing.add(dto);
            } else {
                incoming.add(dto);
            }
        }

        return new FriendListResponse(accepted, incoming, outgoing);
    }

    private Player getCurrentPlayerAndTouchPresence(String email) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Compte introuvable."));
        Player player = playerRepository.findByAccountId(account.getId())
                .orElseThrow(() -> new IllegalStateException("Joueur introuvable."));
        player.setLastSeenAt(Instant.now());
        return playerRepository.save(player);
    }

    private FriendResponse toResponse(Friendship friendship, Player other) {
        Instant now = Instant.now();
        Instant seen = other.getLastSeenAt() != null ? other.getLastSeenAt() : other.getUpdatedAt();
        boolean online = seen != null && Duration.between(seen, now).compareTo(ONLINE_THRESHOLD) <= 0;

        String district = "PLATEAU";
        List<Character> chars = characterRepository.findByPlayerIdAndActiveTrue(other.getId());
        if (chars != null && !chars.isEmpty() && chars.get(0).getDistrict() != null) {
            district = chars.get(0).getDistrict().name();
        }

        return new FriendResponse(
                friendship.getId(),
                other.getId(),
                other.getUsername(),
                other.getLevel(),
                friendship.getStatus().name(),
                online,
                district,
                friendship.getUpdatedAt()
        );
    }
}
