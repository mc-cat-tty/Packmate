package ru.serega6531.packmate.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.serega6531.packmate.model.CtfService;
import ru.serega6531.packmate.model.Pattern;
import ru.serega6531.packmate.model.Stream;

import java.util.List;

public interface StreamRepository extends JpaRepository<Stream, Long> {

    List<Stream> findAllByIdGreaterThanAndFavoriteIsTrue(long streamId, Pageable pageable);

    List<Stream> findAllByIdLessThanAndFavoriteIsTrue(long streamId, Pageable pageable);

    List<Stream> findAllByIdGreaterThanAndFavoriteIsTrueAndFoundPatternsContaining(long streamId, Pattern pattern, Pageable pageable);

    List<Stream> findAllByIdLessThanAndFavoriteIsTrueAndFoundPatternsContaining(long streamId, Pattern pattern, Pageable pageable);

    List<Stream> findAllByServiceAndIdGreaterThanAndFavoriteIsTrue(CtfService service, long streamId, Pageable pageable);

    List<Stream> findAllByServiceAndIdLessThanAndFavoriteIsTrue(CtfService service, long streamId, Pageable pageable);

    List<Stream> findAllByServiceAndIdGreaterThanAndFavoriteIsTrueAndFoundPatternsContaining(CtfService service, long streamId, Pattern pattern, Pageable pageable);

    List<Stream> findAllByServiceAndIdLessThanAndFavoriteIsTrueAndFoundPatternsContaining(CtfService service, long streamId, Pattern pattern, Pageable pageable);

    List<Stream> findAllByIdGreaterThan(long streamId, Pageable pageable);

    List<Stream> findAllByIdLessThan(long streamId, Pageable pageable);

    List<Stream> findAllByIdGreaterThanAndFoundPatternsContaining(long streamId, Pattern pattern, Pageable pageable);

    List<Stream> findAllByIdLessThanAndFoundPatternsContaining(long streamId, Pattern pattern, Pageable pageable);

    List<Stream> findAllByServiceAndIdGreaterThan(CtfService service, long streamId, Pageable pageable);

    List<Stream> findAllByServiceAndIdLessThan(CtfService service, long streamId, Pageable pageable);

    List<Stream> findAllByServiceAndIdGreaterThanAndFoundPatternsContaining(CtfService service, long streamId, Pattern pattern, Pageable pageable);

    List<Stream> findAllByServiceAndIdLessThanAndFoundPatternsContaining(CtfService service, long streamId, Pattern pattern, Pageable pageable);


}
