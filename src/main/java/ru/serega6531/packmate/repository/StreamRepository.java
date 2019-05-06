package ru.serega6531.packmate.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.serega6531.packmate.model.CtfService;
import ru.serega6531.packmate.model.Stream;

import java.util.List;

public interface StreamRepository extends JpaRepository<Stream, Long> {

    List<Stream> findAllByFavorite(Pageable pageable, boolean favorite);

    List<Stream> findAllByIdGreaterThanAndFavorite(long streamId, boolean favorite, Pageable pageable);

    List<Stream> findAllByIdLessThanAndFavorite(long streamId, boolean favorite, Pageable pageable);

    List<Stream> findAllByServiceAndFavorite(CtfService service, boolean favorite, Pageable pageable);

    List<Stream> findAllByServiceAndIdGreaterThanAndFavorite(CtfService service, long streamId, boolean favorite, Pageable pageable);

    List<Stream> findAllByServiceAndIdLessThanAndFavorite(CtfService service, long streamId, boolean favorite, Pageable pageable);

}
