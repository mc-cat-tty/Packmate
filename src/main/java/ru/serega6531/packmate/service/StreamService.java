package ru.serega6531.packmate.service;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.serega6531.packmate.model.*;
import ru.serega6531.packmate.model.enums.SubscriptionMessageType;
import ru.serega6531.packmate.model.pojo.Pagination;
import ru.serega6531.packmate.model.pojo.SubscriptionMessage;
import ru.serega6531.packmate.model.pojo.UnfinishedStream;
import ru.serega6531.packmate.repository.StreamRepository;
import ru.serega6531.packmate.utils.Bytes;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipException;

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

    private final byte[] GZIP_HEADER = {0x1f, (byte) 0x8b, 0x08};
    private final java.util.regex.Pattern userAgentPattern = java.util.regex.Pattern.compile("User-Agent: (.+)\\r\\n");

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

    /**
     * @return был ли сохранен стрим
     */
    @Transactional
    public boolean saveNewStream(UnfinishedStream unfinishedStream, List<Packet> packets) {
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
            return false;
        }
        CtfService service = serviceOptional.get();

        Optional<Packet> firstIncoming = packets.stream()
                .filter(Packet::isIncoming)
                .findFirst();

        Stream stream = new Stream();
        stream.setProtocol(unfinishedStream.getProtocol());
        stream.setTtl(firstIncoming.isPresent() ? firstIncoming.get().getTtl() : 0);
        stream.setStartTimestamp(packets.get(0).getTimestamp());
        stream.setEndTimestamp(packets.get(packets.size() - 1).getTimestamp());
        stream.setService(service.getPort());

        if (ignoreEmptyPackets) {
            packets.removeIf(packet -> packet.getContent().length == 0);

            if (packets.isEmpty()) {
                log.debug("Стрим состоит только из пустых пакетов и не будет сохранен");
                return false;
            }
        }

        if (service.isUngzipHttp()) {
            unpackGzip(packets);
        }

        if (service.isUrldecodeHttpRequests()) {
            urldecodeRequests(packets);
        }

        if (service.isMergeAdjacentPackets()) {
            mergeAdjacentPackets(packets);
        }

        String ua = null;
        for (Packet packet : packets) {
            String content = new String(packet.getContent());
            final Matcher matcher = userAgentPattern.matcher(content);
            if (matcher.find()) {
                ua = matcher.group(1);
                break;
            }
        }

        if (ua != null) {
            stream.setUserAgentHash(calculateUserAgentHash(ua));
        }

        Stream savedStream = save(stream);

        Set<Pattern> foundPatterns = new HashSet<>();

        for (ru.serega6531.packmate.model.Packet packet : packets) {
            packet.setStream(savedStream);
            final Set<FoundPattern> matches = patternService.findMatches(packet.getContent(), packet.isIncoming());
            packet.setMatches(matches);
            foundPatterns.addAll(matches.stream()
                    .map(FoundPattern::getPatternId)
                    .map(patternService::find)
                    .collect(Collectors.toList()));
        }

        savedStream.setFoundPatterns(foundPatterns);
        savedStream.setPackets(packetService.saveAll(packets));
        savedStream = save(savedStream);

        subscriptionService.broadcast(new SubscriptionMessage(SubscriptionMessageType.NEW_STREAM, savedStream));
        return true;
    }

    private void mergeAdjacentPackets(List<Packet> packets) {
        int start = 0;
        int packetsInRow = 0;
        boolean incoming = true;

        for (int i = 0; i < packets.size(); i++) {
            Packet packet = packets.get(i);
            if (packet.isIncoming() != incoming) {
                if (packetsInRow > 1) {
                    final List<Packet> cut = packets.subList(start, i);
                    final long timestamp = cut.get(0).getTimestamp();
                    final boolean ungzipped = cut.stream().anyMatch(Packet::isUngzipped);
                    //noinspection OptionalGetWithoutIsPresent
                    final byte[] content = cut.stream()
                            .map(Packet::getContent)
                            .reduce(ArrayUtils::addAll)
                            .get();

                    packets.removeAll(cut);
                    packets.add(start, Packet.builder()
                            .incoming(incoming)
                            .timestamp(timestamp)
                            .ungzipped(ungzipped)
                            .content(content)
                            .build());

                    i++;
                }
                start = i;
                packetsInRow = 1;
            } else {
                packetsInRow++;
            }

            incoming = packet.isIncoming();
        }

        if (packetsInRow > 1) {
            final List<Packet> cut = packets.subList(start, packets.size());
            final long timestamp = cut.get(0).getTimestamp();
            final boolean ungzipped = cut.stream().anyMatch(Packet::isUngzipped);
            //noinspection OptionalGetWithoutIsPresent
            final byte[] content = cut.stream()
                    .map(Packet::getContent)
                    .reduce(ArrayUtils::addAll)
                    .get();

            packets.removeAll(cut);
            packets.add(Packet.builder()
                    .incoming(incoming)
                    .timestamp(timestamp)
                    .ungzipped(ungzipped)
                    .content(content)
                    .build());
        }
    }

    @SneakyThrows
    private void urldecodeRequests(List<Packet> packets) {
        boolean httpStarted = false;

        for (Packet packet : packets) {
            if (packet.isIncoming()) {
                String content = new String(packet.getContent());
                if (content.startsWith("HTTP/")) {
                    httpStarted = true;
                }

                if (httpStarted) {
                    content = URLDecoder.decode(content, StandardCharsets.UTF_8.toString());
                    packet.setContent(content.getBytes());
                }
            } else {
                httpStarted = false;
            }
        }
    }

    private void unpackGzip(List<Packet> packets) {
        boolean gzipStarted = false;
        int gzipStartPacket = 0;
        int gzipEndPacket;

        for (int i = 0; i < packets.size(); i++) {
            Packet packet = packets.get(i);

            if (packet.isIncoming() && gzipStarted) {
                gzipEndPacket = i - 1;

                List<Packet> cut = packets.subList(gzipStartPacket, gzipEndPacket + 1);

                Packet decompressed = decompressGzipPackets(cut);
                if (decompressed != null) {
                    packets.removeAll(cut);
                    packets.add(gzipStartPacket, decompressed);
                    gzipStarted = false;
                    i = gzipStartPacket + 1;
                }
            } else if (!packet.isIncoming()) {
                String content = new String(packet.getContent());

                int contentPos = content.indexOf("\r\n\r\n");
                boolean http = content.startsWith("HTTP/");

                if (http && gzipStarted) {
                    gzipEndPacket = i - 1;
                    List<Packet> cut = packets.subList(gzipStartPacket, gzipEndPacket + 1);

                    Packet decompressed = decompressGzipPackets(cut);
                    if (decompressed != null) {
                        packets.removeAll(cut);
                        packets.add(gzipStartPacket, decompressed);
                        gzipStarted = false;
                        i = gzipStartPacket + 1;
                    }
                }

                if (contentPos != -1) {   // начало body
                    String headers = content.substring(0, contentPos);
                    boolean gziped = headers.contains("Content-Encoding: gzip\r\n");
                    if (gziped) {
                        gzipStarted = true;
                        gzipStartPacket = i;
                    }
                }
            }
        }

        if (gzipStarted) {
            gzipEndPacket = packets.size() - 1;
            List<Packet> cut = packets.subList(gzipStartPacket, gzipEndPacket + 1);

            Packet decompressed = decompressGzipPackets(cut);
            if (decompressed != null) {
                packets.removeAll(cut);
                packets.add(gzipStartPacket, decompressed);
            }
        }
    }

    private Packet decompressGzipPackets(List<Packet> packets) {
        //noinspection OptionalGetWithoutIsPresent
        final byte[] content = packets.stream()
                .map(Packet::getContent)
                .reduce(ArrayUtils::addAll)
                .get();

        final int gzipStart = Bytes.indexOf(content, GZIP_HEADER);
        byte[] httpHeader = Arrays.copyOfRange(content, 0, gzipStart);
        byte[] gzipBytes = Arrays.copyOfRange(content, gzipStart, content.length);

        try {
            final GZIPInputStream gzipStream = new GZIPInputStream(new ByteArrayInputStream(gzipBytes));
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            IOUtils.copy(gzipStream, out);
            byte[] newContent = ArrayUtils.addAll(httpHeader, out.toByteArray());

            log.debug("Разархивирован gzip: {} -> {} байт", gzipBytes.length, out.size());

            return Packet.builder()
                    .incoming(false)
                    .timestamp(packets.get(0).getTimestamp())
                    .ungzipped(true)
                    .content(newContent)
                    .build();
        } catch (ZipException e) {
            log.warn("Не удалось разархивировать gzip, оставляем как есть", e);
        } catch (IOException e) {
            log.error("decompress gzip", e);
        }

        return null;
    }

    public Stream save(Stream stream) {
        Stream saved;
        if (stream.getId() == null) {
            saved = repository.save(stream);
            log.debug("Создан стрим с id {}", saved.getId());
        } else {
            saved = repository.save(stream);
        }

        return saved;
    }

    public Optional<Stream> find(long id) {
        return repository.findById(id);
    }

    private String calculateUserAgentHash(String ua) {
        char[] alphabet = "abcdefghijklmnopqrstuvwxyz0123456789".toCharArray();
        int l = alphabet.length;
        final int hash = Math.abs(ua.hashCode()) % (l * l * l);
        return "" + alphabet[hash % l] + alphabet[(hash / l) % l] + alphabet[(hash / (l * l)) % l];
    }

    @SuppressWarnings("UnusedReturnValue")
    @Transactional
    public Stream setFavorite(long id, boolean favorite) {
        final Optional<Stream> streamOptional = repository.findById(id);
        if (streamOptional.isPresent()) {
            final Stream stream = streamOptional.get();
            stream.setFavorite(favorite);
            return repository.save(stream);
        }

        return null;
    }

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
