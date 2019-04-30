package ru.serega6531.packmate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.serega6531.packmate.model.Packet;
import ru.serega6531.packmate.model.Pattern;
import ru.serega6531.packmate.model.Stream;
import ru.serega6531.packmate.model.UnfinishedStream;
import ru.serega6531.packmate.repository.StreamRepository;

import java.util.*;

@Service
@Slf4j
public class StreamService {

    private final StreamRepository repository;
    private final PatternService patternService;
    private final ServicesService servicesService;
    private final PacketService packetService;
    private final StreamSubscriptionService subscriptionService;

    private final String localIp;

    @Autowired
    public StreamService(StreamRepository repository,
                         PatternService patternService,
                         ServicesService servicesService,
                         PacketService packetService,
                         StreamSubscriptionService subscriptionService,
                         @Value("${local-ip}") String localIp) {
        this.repository = repository;
        this.patternService = patternService;
        this.servicesService = servicesService;
        this.packetService = packetService;
        this.subscriptionService = subscriptionService;
        this.localIp = localIp;
    }

    @Transactional
    public void saveNewStream(UnfinishedStream unfinishedStream, List<Packet> packets) {
        Stream stream = new Stream();
        stream.setProtocol(unfinishedStream.getProtocol());
        stream.setStartTimestamp(packets.get(0).getTimestamp());
        stream.setEndTimestamp(packets.get(packets.size() - 1).getTimestamp());
        stream.setService(servicesService.findService(
                localIp,
                unfinishedStream.getFirstIp().getHostAddress(),
                unfinishedStream.getFirstPort(),
                unfinishedStream.getSecondIp().getHostAddress(),
                unfinishedStream.getSecondPort()
        ).get());

        Stream savedStream = save(stream);

        List<ru.serega6531.packmate.model.Packet> savedPackets = new ArrayList<>();
        Set<Pattern> matches = new HashSet<>();

        for (ru.serega6531.packmate.model.Packet packet : packets) {
            packet.setStream(savedStream);
            savedPackets.add(packetService.save(packet));
            matches.addAll(patternService.findMatching(packet.getContent()));
        }

        savedStream.setFoundPatterns(new ArrayList<>(matches));
        savedStream.setPackets(savedPackets);
        savedStream = save(savedStream);

        subscriptionService.broadcastNewStream(savedStream);
    }

    public Stream save(Stream stream) {
        Stream saved;
        if(stream.getId() == null) {
            saved = repository.save(stream);
            log.info("Создан стрим с id {}", saved.getId());
        } else {
            saved = repository.save(stream);
        }

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
