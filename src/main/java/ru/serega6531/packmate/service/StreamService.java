package ru.serega6531.packmate.service;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.serega6531.packmate.model.CtfService;
import ru.serega6531.packmate.model.FoundPattern;
import ru.serega6531.packmate.model.Packet;
import ru.serega6531.packmate.model.Pattern;
import ru.serega6531.packmate.model.Stream;
import ru.serega6531.packmate.model.enums.PatternActionType;
import ru.serega6531.packmate.model.enums.PatternDirectionType;
import ru.serega6531.packmate.model.enums.SubscriptionMessageType;
import ru.serega6531.packmate.model.pojo.PacketDto;
import ru.serega6531.packmate.model.pojo.StreamDto;
import ru.serega6531.packmate.model.pojo.StreamPagination;
import ru.serega6531.packmate.model.pojo.SubscriptionMessage;
import ru.serega6531.packmate.model.pojo.UnfinishedStream;
import ru.serega6531.packmate.properties.PackmateProperties;
import ru.serega6531.packmate.repository.StreamRepository;
import ru.serega6531.packmate.service.optimization.StreamOptimizer;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;

@Service
@Slf4j
public class StreamService {

    private final StreamRepository repository;
    private final PatternService patternService;
    private final ServicesService servicesService;
    private final CountingService countingService;
    private final SubscriptionService subscriptionService;
    private final ModelMapper modelMapper;
    private final boolean ignoreEmptyPackets;

    private final java.util.regex.Pattern userAgentPattern = java.util.regex.Pattern.compile("User-Agent: (.+)\\r\\n");

    @Autowired
    public StreamService(StreamRepository repository,
                         PatternService patternService,
                         ServicesService servicesService,
                         CountingService countingService,
                         SubscriptionService subscriptionService,
                         ModelMapper modelMapper,
                         PackmateProperties properties) {
        this.repository = repository;
        this.patternService = patternService;
        this.servicesService = servicesService;
        this.countingService = countingService;
        this.subscriptionService = subscriptionService;
        this.modelMapper = modelMapper;
        this.ignoreEmptyPackets = properties.ignoreEmptyPackets();
    }

    /**
     * @return был ли сохранен стрим
     */
    @Transactional(propagation = Propagation.NEVER)
    public boolean saveNewStream(UnfinishedStream unfinishedStream, List<Packet> packets) {
        final var serviceOptional = servicesService.findService(
                unfinishedStream.firstIp(),
                unfinishedStream.firstPort(),
                unfinishedStream.secondIp(),
                unfinishedStream.secondPort()
        );

        if (serviceOptional.isEmpty()) {
            log.warn("Failed to save the stream: service at port {} or {} does not exist",
                    unfinishedStream.firstPort(), unfinishedStream.secondPort());
            return false;
        }
        CtfService service = serviceOptional.get();

        if (ignoreEmptyPackets) {
            packets.removeIf(packet -> packet.getContent().length == 0);

            if (packets.isEmpty()) {
                log.debug("Stream consists only of empty packets and will not be saved");
                return false;
            }
        }

        countingService.countStream(service.getPort(), packets.size());

        int packetsSize = packets.stream().mapToInt(p -> p.getContent().length).sum();
        int packetsCount = packets.size();

        List<Packet> optimizedPackets = new StreamOptimizer(service, packets).optimizeStream();

        if (isStreamIgnored(optimizedPackets, service)) {
            log.debug("New stream is ignored");
            return false;
        }

        Optional<Packet> firstIncoming = packets.stream()
                .filter(Packet::isIncoming)
                .findFirst();

        final Stream stream = new Stream();
        stream.setProtocol(unfinishedStream.protocol());
        stream.setTtl(firstIncoming.map(Packet::getTtl).orElse(0));
        stream.setStartTimestamp(packets.get(0).getTimestamp());
        stream.setEndTimestamp(packets.get(packets.size() - 1).getTimestamp());
        stream.setService(service.getPort());

        String userAgentHash = getUserAgentHash(optimizedPackets);
        stream.setUserAgentHash(userAgentHash);

        stream.setSizeBytes(packetsSize);
        stream.setPacketsCount(packetsCount);

        Set<Pattern> foundPatterns = matchPatterns(optimizedPackets, service);
        stream.setFoundPatterns(foundPatterns);
        stream.setPackets(optimizedPackets);

        for (Packet packet : optimizedPackets) {
            packet.setStream(stream);
        }

        Stream savedStream = save(stream);

        subscriptionService.broadcast(new SubscriptionMessage(SubscriptionMessageType.NEW_STREAM, streamToDto(savedStream)));
        return true;
    }

    @Async
    @Transactional
    public void processLookbackPattern(Pattern pattern, long start, long end) {
        List<Stream> streams = findAllBetweenTimestamps(start, end);

        for (Stream stream : streams) {
            boolean found = matchPattern(stream.getPackets(), pattern);
            if (found) {
                stream.getFoundPatterns().add(pattern);
                repository.save(stream);
            }
        }

        log.info("Finished lookback for pattern '{}'", pattern.getName());
        subscriptionService.broadcast(new SubscriptionMessage(SubscriptionMessageType.FINISH_LOOKBACK, pattern.getId()));
    }

    private String getUserAgentHash(List<Packet> packets) {
        String ua = null;
        for (Packet packet : packets) {
            String content = packet.getContentString();
            final Matcher matcher = userAgentPattern.matcher(content);
            if (matcher.find()) {
                ua = matcher.group(1);
                break;
            }
        }

        if (ua != null) {
            return calculateUserAgentHash(ua);
        } else {
            return null;
        }
    }

    private String calculateUserAgentHash(String ua) {
        char[] alphabet = "abcdefghijklmnopqrstuvwxyz0123456789".toCharArray();
        int l = alphabet.length;
        int hashCode = ua.hashCode();
        if (hashCode == Integer.MIN_VALUE) {  // abs(MIN_VALUE) вернет то же значение
            hashCode = Integer.MAX_VALUE;
        }
        final int hash = Math.abs(hashCode) % (l * l * l);
        return "" + alphabet[hash % l] + alphabet[(hash / l) % l] + alphabet[(hash / (l * l)) % l];
    }

    private Set<Pattern> matchPatterns(List<Packet> packets, CtfService service) {
        Set<Pattern> foundPatterns = new HashSet<>();

        for (Packet packet : packets) {
            PatternDirectionType direction = packet.isIncoming() ? PatternDirectionType.INPUT : PatternDirectionType.OUTPUT;
            final Set<FoundPattern> matches = patternService.findMatches(packet.getContent(), service,
                    direction, PatternActionType.FIND);

            packet.setMatches(matches);
            matches.forEach(m -> m.setPacket(packet));

            foundPatterns.addAll(matches.stream()
                    .map(FoundPattern::getPatternId)
                    .map(patternService::find)
                    .toList());
        }

        return foundPatterns;
    }

    private boolean matchPattern(List<Packet> packets, Pattern pattern) {
        boolean matched = false;

        for (Packet packet : packets) {
            PatternDirectionType direction = packet.isIncoming() ? PatternDirectionType.INPUT : PatternDirectionType.OUTPUT;

            if (pattern.getDirectionType() != PatternDirectionType.BOTH && pattern.getDirectionType() != direction) {
                continue;
            }

            final Set<FoundPattern> matches = patternService.matchOne(packet.getContent(), pattern);

            if (!matches.isEmpty()) {
                packet.getMatches().addAll(matches);
                matches.forEach(m -> m.setPacket(packet));

                matched = true;
            }
        }

        return matched;
    }

    private boolean isStreamIgnored(List<Packet> packets, CtfService service) {
        for (Packet packet : packets) {
            PatternDirectionType direction = packet.isIncoming() ? PatternDirectionType.INPUT : PatternDirectionType.OUTPUT;
            final Set<FoundPattern> matches = patternService.findMatches(packet.getContent(), service,
                    direction, PatternActionType.IGNORE);
            if (!matches.isEmpty()) {
                return true;
            }
        }

        return false;
    }

    private Stream save(Stream stream) {
        Stream saved;
        if (stream.getId() == null) {
            saved = repository.save(stream);
            log.debug("Saved stream with id {}", saved.getId());
        } else {
            saved = repository.save(stream);
        }

        return saved;
    }

    @Transactional
    public List<PacketDto> getPackets(long streamId, @Nullable Long startingFrom, int pageSize) {
        return repository.getPackets(streamId, startingFrom, Pageable.ofSize(pageSize))
                .stream()
                .map(this::packetToDto)
                .toList();
    }

    /**
     * @return Number of deleted rows
     */
    @Transactional
    public long cleanupOldStreams(ZonedDateTime before) {
        return repository.deleteByEndTimestampBeforeAndFavoriteIsFalse(before.toEpochSecond() * 1000);
    }

    @Transactional
    public void setFavorite(long id, boolean favorite) {
        repository.setFavorite(id, favorite);
    }

    @Transactional
    public List<StreamDto> findAll(StreamPagination pagination, Optional<Integer> service, boolean onlyFavorites) {
        PageRequest page = PageRequest.of(0, pagination.getPageSize(), Sort.Direction.DESC, "id");

        Specification<Stream> spec = Specification.where(null);

        if (pagination.getStartingFrom() != null) {
            spec = spec.and(streamIdLessThan(pagination.getStartingFrom()));
        }

        if (service.isPresent()) {
            spec = spec.and(streamServiceEquals(service.get()));
        }

        if (onlyFavorites) {
            spec = spec.and(streamIsFavorite());
        }

        if (pagination.getPattern() != null) {
            spec = spec.and(streamPatternsContains(pagination.getPattern()));
        }

        return repository.findAll(spec, page)
                .getContent()
                .stream()
                .map(this::streamToDto)
                .toList();
    }

     public List<Stream> findAllBetweenTimestamps(long start, long end) {
         Specification<Stream> spec = streamTimestampBetween(start, end);
         return repository.findAll(spec);
     }

    public StreamDto streamToDto(Stream stream) {
        return modelMapper.map(stream, StreamDto.class);
    }

    public PacketDto packetToDto(Packet packet) {
        return modelMapper.map(packet, PacketDto.class);
    }

    private Specification<Stream> streamServiceEquals(long service) {
        return (root, query, cb) -> cb.equal(root.get("service"), service);
    }

    private Specification<Stream> streamIsFavorite() {
        return (root, query, cb) -> cb.equal(root.get("favorite"), true);
    }

    private Specification<Stream> streamIdLessThan(long id) {
        return (root, query, cb) -> cb.lessThan(root.get("id"), id);
    }

    private Specification<Stream> streamTimestampBetween(long start, long end) {
        return (root, query, cb) -> cb.between(root.get("startTimestamp"), start, end);
    }

    private Specification<Stream> streamPatternsContains(Pattern pattern) {
        return (root, query, cb) -> cb.isMember(pattern, root.get("foundPatterns"));
    }

}
