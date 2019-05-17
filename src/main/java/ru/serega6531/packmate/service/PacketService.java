package ru.serega6531.packmate.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ru.serega6531.packmate.model.Packet;
import ru.serega6531.packmate.model.Pagination;
import ru.serega6531.packmate.model.Stream;
import ru.serega6531.packmate.repository.PacketRepository;

import java.util.List;

@Service
public class PacketService {

    private final PacketRepository repository;

    @Autowired
    public PacketService(PacketRepository repository) {
        this.repository = repository;
    }

    public List<Packet> getPacketsForStream(Pagination pagination, Stream stream) {
        PageRequest page = PageRequest.of(0, pagination.getPageSize(), pagination.getDirection(), "id");

        if (pagination.getDirection() == Sort.Direction.ASC) {  // более новые пакеты
            return repository.findAllByStreamAndIdGreaterThan(stream, pagination.getStartingFrom(), page);
        } else {  // более старые пакеты
            return repository.findAllByStreamAndIdLessThan(stream, pagination.getStartingFrom(), page);
        }
    }

    public Packet save(Packet packet) {
        return repository.save(packet);
    }

}
