package ru.serega6531.packmate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.serega6531.packmate.model.*;
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
    private final boolean ignoreEmptyPackets;

    @Autowired
    public StreamService(StreamRepository repository,
                         PatternService patternService,
                         ServicesService servicesService,
                         PacketService packetService,
                         StreamSubscriptionService subscriptionService,
                         @Value("${local-ip}") String localIp,
                         @Value("${ignore-empty-packets}") boolean ignoreEmptyPackets) {
        this.repository = repository;
        this.patternService = patternService;
        this.servicesService = servicesService;
        this.packetService = packetService;
        this.subscriptionService = subscriptionService;
        this.localIp = localIp;
        this.ignoreEmptyPackets = ignoreEmptyPackets;
    }

    @Transactional
    public void saveNewStream(UnfinishedStream unfinishedStream, List<Packet> packets) {
        final Optional<CtfService> serviceOptional = servicesService.findService(
                localIp,
                unfinishedStream.getFirstIp().getHostAddress(),
                unfinishedStream.getFirstPort(),
                unfinishedStream.getSecondIp().getHostAddress(),
                unfinishedStream.getSecondPort()
        );

        if (!serviceOptional.isPresent()) {
            log.warn("Не удалось сохранить стрим: сервиса на порту {} или {} не существует",
                    unfinishedStream.getFirstPort(), unfinishedStream.getSecondPort());
            return;
        }

        Stream stream = new Stream();
        stream.setProtocol(unfinishedStream.getProtocol());
        stream.setStartTimestamp(packets.get(0).getTimestamp());
        stream.setEndTimestamp(packets.get(packets.size() - 1).getTimestamp());
        stream.setService(serviceOptional.get());

        if(ignoreEmptyPackets) {
            packets.removeIf(packet -> packet.getContent().length == 0);

            if(packets.isEmpty()) {
                log.debug("Стрим состоит только из пустых пакетов и не будет сохранен");
                return;
            }
        }

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
        if (stream.getId() == null) {
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

    @Transactional
    public void setFavorite(long id, boolean favorite) {
        final Optional<Stream> streamOptional = repository.findById(id);
        if(streamOptional.isPresent()) {
            final Stream stream = streamOptional.get();
            stream.setFavorite(favorite);
            repository.save(stream);
        }
    }

    public List<Stream> findAll(Pagination pagination) {
        PageRequest page = PageRequest.of(0, pagination.getPageSize(), pagination.getDirection(), "id");

        if(pagination.isFetchLatest()) { // последние стримы
            return repository.findAllByFavorite(page, pagination.isFavorites());
        } else {
            if (pagination.getDirection() == Sort.Direction.ASC) {  // более новые стримы
                return repository.findAllByIdGreaterThanAndFavorite(pagination.getStartingFrom(), pagination.isFavorites(), page);
            } else {  // более старые стримы
                return repository.findAllByIdLessThanAndFavorite(pagination.getStartingFrom(), pagination.isFavorites(), page);
            }
        }
    }

    public List<Stream> findAllByService(Pagination pagination, CtfService service) {
        PageRequest page = PageRequest.of(0, pagination.getPageSize(), pagination.getDirection(), "id");

        if(pagination.isFetchLatest()) { // последние стримы
            return repository.findAllByServiceAndFavorite(service, pagination.isFavorites(), page);
        } else {
            if (pagination.getDirection() == Sort.Direction.ASC) {  // более новые стримы
                return repository.findAllByServiceAndIdGreaterThanAndFavorite(service, pagination.getStartingFrom(), pagination.isFavorites(), page);
            } else {  // более старые стримы
                return repository.findAllByServiceAndIdLessThanAndFavorite(service, pagination.getStartingFrom(), pagination.isFavorites(), page);
            }
        }
    }

}
