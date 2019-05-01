package ru.serega6531.packmate.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.serega6531.packmate.model.CtfService;
import ru.serega6531.packmate.model.Stream;

import java.util.List;

public interface StreamRepository extends JpaRepository<Stream, Long> {

    List<Stream> findAllByIdGreaterThan(long streamId, Pageable pageable);

    List<Stream> findAllByIdLessThan(long streamId, Pageable pageable);

    List<Stream> findAllByService(CtfService service, Pageable pageable);

    List<Stream> findAllByServiceAndIdGreaterThan(CtfService service, long streamId, Pageable pageable);

    List<Stream> findAllByServiceAndIdLessThan(CtfService service, long streamId, Pageable pageable);

}
