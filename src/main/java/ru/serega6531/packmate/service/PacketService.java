package ru.serega6531.packmate.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.serega6531.packmate.model.Packet;
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

    public List<Packet> getPacketsForStream(Stream stream) {
        return repository.findAllByStream(stream);
    }

    public List<Packet> saveAll(List<Packet> packets) {
        return repository.saveAll(packets);
    }

}
