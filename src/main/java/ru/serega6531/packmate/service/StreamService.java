package ru.serega6531.packmate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.serega6531.packmate.model.Pattern;
import ru.serega6531.packmate.model.Stream;
import ru.serega6531.packmate.repository.StreamRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Slf4j
public class StreamService {

    private final StreamRepository repository;
    private final PatternService patternService;

    @Autowired
    public StreamService(StreamRepository repository, PatternService patternService) {
        this.repository = repository;
        this.patternService = patternService;
    }

    public Stream save(Stream stream) {
        if(!stream.getPackets().isEmpty()) {
            Set<Pattern> matches = new HashSet<>();

            stream.getPackets().forEach(packet -> {
                matches.addAll(patternService.findMatching(packet.getContent()));
            });

            stream.setFoundPatterns(matches);
        }

        final Stream saved = repository.save(stream);
        log.info("Создан стрим с id {}", saved.getId());
        return saved;
    }

    public Optional<Stream> find(long id) {
        return repository.findById(id);
    }

    public List<Stream> findAll() {
        return repository.findAll();
    }

    public List<Stream> findAllByServicePort(int port) {
        return repository.findAllByService_Port(port);
    }

}
