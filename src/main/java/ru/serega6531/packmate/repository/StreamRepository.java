package ru.serega6531.packmate.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.serega6531.packmate.model.Stream;

import java.util.List;

public interface StreamRepository extends JpaRepository<Stream, Long> {

    List<Stream> findAllByService_Port(int port);

}
