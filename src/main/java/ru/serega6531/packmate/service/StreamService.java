package ru.serega6531.packmate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.serega6531.packmate.model.*;
import ru.serega6531.packmate.model.enums.SubscriptionMessageType;
import ru.serega6531.packmate.model.pojo.Pagination;
import ru.serega6531.packmate.model.pojo.SubscriptionMessage;
import ru.serega6531.packmate.model.pojo.UnfinishedStream;
import ru.serega6531.packmate.repository.StreamRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

@Service
@Slf4j
public class StreamService {

    private final StreamRepository repository;
    private final PatternService patternService;
    private final ServicesService servicesService;
    private final CountingService countingService;
    private final SubscriptionService subscriptionService;

    private final boolean ignoreEmptyPackets;

    private final java.util.regex.Pattern userAgentPattern = java.util.regex.Pattern.compile("User-Agent: (.+)\\r\\n");

    @Autowired
    public StreamService(StreamRepository repository,
                         PatternService patternService,
                         ServicesService servicesService,
                         CountingService countingService,
                         SubscriptionService subscriptionService,
                         @Value("${ignore-empty-packets}") boolean ignoreEmptyPackets) {
        this.repository = repository;
        this.patternService = patternService;
        this.servicesService = servicesService;
        this.countingService = countingService;
        this.subscriptionService = subscriptionService;
        this.ignoreEmptyPackets = ignoreEmptyPackets;
    }

    /**
     * @return был ли сохранен стрим
     */
    @Transactional(propagation = Propagation.NEVER)
    public boolean saveNewStream(UnfinishedStream unfinishedStream, List<Packet> packets) {
        final var serviceOptional = servicesService.findService(
                unfinishedStream.getFirstIp(),
                unfinishedStream.getFirstPort(),
                unfinishedStream.getSecondIp(),
                unfinishedStream.getSecondPort()
        );

        if (serviceOptional.isEmpty()) {
            log.warn("Failed to save the stream: service at port {} or {} does not exist",
                    unfinishedStream.getFirstPort(), unfinishedStream.getSecondPort());
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

        Optional<Packet> firstIncoming = packets.stream()
                .filter(Packet::isIncoming)
                .findFirst();

        final Stream stream = new Stream();
        stream.setProtocol(unfinishedStream.getProtocol());
        stream.setTtl(firstIncoming.isPresent() ? firstIncoming.get().getTtl() : 0);
        stream.setStartTimestamp(packets.get(0).getTimestamp());
        stream.setEndTimestamp(packets.get(packets.size() - 1).getTimestamp());
        stream.setService(service.getPort());

        countingService.countStream(service.getPort(), packets.size());

        packets = new StreamOptimizer(service, packets).optimizeStream();
        processUserAgent(packets, stream);

        Stream savedStream = save(stream);

        Set<Pattern> foundPatterns = getFoundPatterns(packets, savedStream);
        savedStream.setFoundPatterns(foundPatterns);
        savedStream.setPackets(packets);
        savedStream = save(savedStream);

        subscriptionService.broadcast(new SubscriptionMessage(SubscriptionMessageType.NEW_STREAM, savedStream));
        return true;
    }

    private void processUserAgent(List<Packet> packets, Stream stream) {
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
            stream.setUserAgentHash(calculateUserAgentHash(ua));
        }
    }

    private String calculateUserAgentHash(String ua) {
        char[] alphabet = "abcdefghijklmnopqrstuvwxyz0123456789".toCharArray();
        int l = alphabet.length;
        int hashCode = ua.hashCode();
        if(hashCode == Integer.MIN_VALUE) {  // abs(MIN_VALUE) вернет то же значение
            hashCode = Integer.MAX_VALUE;
        }
        final int hash = Math.abs(hashCode) % (l * l * l);
        return "" + alphabet[hash % l] + alphabet[(hash / l) % l] + alphabet[(hash / (l * l)) % l];
    }

    private Set<Pattern> getFoundPatterns(List<Packet> packets, Stream savedStream) {
        Set<Pattern> foundPatterns = new HashSet<>();

        for (Packet packet : packets) {
            packet.setStream(savedStream);
            final Set<FoundPattern> matches = patternService.findMatches(packet.getContent(), packet.isIncoming());
            packet.setMatches(matches);
            foundPatterns.addAll(matches.stream()
                    .map(FoundPattern::getPatternId)
                    .map(patternService::find)
                    .collect(Collectors.toList()));
        }

        return foundPatterns;
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

    public Optional<Stream> find(long id) {
        return repository.findById(id);
    }

    @SuppressWarnings("UnusedReturnValue")
    @Transactional
    public void setFavorite(long id, boolean favorite) {
        repository.setFavorite(id, favorite);
    }

    @SuppressWarnings("ConstantConditions")
    public List<Stream> findAll(Pagination pagination, Optional<Integer> service, boolean onlyFavorites) {
        PageRequest page = PageRequest.of(0, pagination.getPageSize(), pagination.getDirection(), "id");

        Specification<Stream> spec;
        if(pagination.getDirection() == Sort.Direction.ASC) {
            spec = streamIdGreaterThan(pagination.getStartingFrom());
        } else {
            spec = streamIdLessThan(pagination.getStartingFrom());
        }

        if(service.isPresent()) {
            spec = spec.and(streamServiceEquals(service.get()));
        }

        if(onlyFavorites) {
            spec = spec.and(streamIsFavorite());
        }

        if(pagination.getPattern() != null) {
            spec = spec.and(streamPatternsContains(pagination.getPattern()));
        }

        return repository.findAll(spec, page).getContent();
    }

    private Specification<Stream> streamServiceEquals(long service) {
        return (root, query, cb) -> cb.equal(root.get("service"), service);
    }

    private Specification<Stream> streamIsFavorite() {
        return (root, query, cb) -> cb.equal(root.get("favorite"), true);
    }

    private Specification<Stream> streamIdGreaterThan(long id) {
        return (root, query, cb) -> cb.greaterThan(root.get("id"), id);
    }

    private Specification<Stream> streamIdLessThan(long id) {
        return (root, query, cb) -> cb.lessThan(root.get("id"), id);
    }

    private Specification<Stream> streamPatternsContains(Pattern pattern) {
        return (root, query, cb) -> cb.isMember(pattern, root.get("foundPatterns"));
    }

}
