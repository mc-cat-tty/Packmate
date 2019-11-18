package ru.serega6531.packmate.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.serega6531.packmate.model.Packet;
import ru.serega6531.packmate.model.Stream;

import java.util.List;

public interface PacketRepository extends JpaRepository<Packet, Long> {

    List<Packet> findAllByStream(Stream stream);

}
