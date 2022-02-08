package ru.serega6531.packmate.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import ru.serega6531.packmate.model.Packet;
import ru.serega6531.packmate.model.Stream;

import javax.persistence.QueryHint;
import java.util.List;

public interface StreamRepository extends JpaRepository<Stream, Long>, JpaSpecificationExecutor<Stream> {

    @Query("UPDATE Stream SET favorite = :favorite WHERE id = :id")
    @Modifying
    void setFavorite(long id, boolean favorite);

    long deleteByEndTimestampBeforeAndFavoriteIsFalse(long threshold);

    @Query("SELECT DISTINCT p FROM Packet p " +
            "LEFT JOIN FETCH p.matches " +
            "WHERE p.stream.id = :streamId " +
            "AND (:startingFrom IS NULL OR p.id > :startingFrom) " +
            "ORDER BY p.id"
    )
    @QueryHints(@QueryHint(name = org.hibernate.jpa.QueryHints.HINT_PASS_DISTINCT_THROUGH, value = "false"))
    List<Packet> getPackets(long streamId, Long startingFrom, Pageable pageable);

}
