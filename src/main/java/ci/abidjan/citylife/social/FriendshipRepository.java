package ci.abidjan.citylife.social;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FriendshipRepository extends JpaRepository<Friendship, UUID> {

    @Query("""
            SELECT f FROM Friendship f
            JOIN FETCH f.requester
            JOIN FETCH f.addressee
            WHERE f.requester.id = :playerId OR f.addressee.id = :playerId
            ORDER BY f.updatedAt DESC
            """)
    List<Friendship> findAllForPlayer(@Param("playerId") UUID playerId);

    @Query("""
            SELECT f FROM Friendship f
            WHERE (f.requester.id = :a AND f.addressee.id = :b)
               OR (f.requester.id = :b AND f.addressee.id = :a)
            """)
    Optional<Friendship> findBetweenPlayers(@Param("a") UUID a, @Param("b") UUID b);
}
