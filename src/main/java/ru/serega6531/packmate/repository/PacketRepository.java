package ru.serega6531.packmate.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.serega6531.packmate.model.Packet;

public interface PacketRepository extends JpaRepository<Packet, Long> {
}
