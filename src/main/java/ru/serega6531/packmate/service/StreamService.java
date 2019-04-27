package ru.serega6531.packmate.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.serega6531.packmate.model.Stream;
import ru.serega6531.packmate.repository.StreamRepository;

import java.util.List;

@Service
public class StreamService {

    private final StreamRepository repository;

    @Autowired
    public StreamService(StreamRepository repository) {
        this.repository = repository;
    }

    public List<Stream> findAll() {
        return repository.findAll();
    }

    public List<Stream> findAllByServicePort(int port) {
        return repository.findAllByService_Port(port);
    }

}
