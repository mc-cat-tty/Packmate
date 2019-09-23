package ru.serega6531.packmate.service;

import com.google.common.primitives.Bytes;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.serega6531.packmate.model.*;
import ru.serega6531.packmate.repository.StreamRepository;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
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
    private final boolean unpackGzippedHttp;
    private final boolean ignoreEmptyPackets;

    private final byte[] GZIP_HEADER = {0x1f, (byte) 0x8b, 0x08};

    @Autowired
    public StreamService(StreamRepository repository,
                         PatternService patternService,
                         ServicesService servicesService,
                         PacketService packetService,
                         StreamSubscriptionService subscriptionService,
                         @Value("${local-ip}") String localIp,
                         @Value("${unpack-gzipped-http}") boolean unpackGzippedHttp,
                         @Value("${ignore-empty-packets}") boolean ignoreEmptyPackets) {
        this.repository = repository;
        this.patternService = patternService;
        this.servicesService = servicesService;
        this.packetService = packetService;
        this.subscriptionService = subscriptionService;
        this.localIp = localIp;
        this.unpackGzippedHttp = unpackGzippedHttp;
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

        Optional<Packet> firstIncoming = packets.stream()
                .filter(Packet::isIncoming)
                .findFirst();

        Stream stream = new Stream();
        stream.setProtocol(unfinishedStream.getProtocol());
        stream.setTtl(firstIncoming.isPresent() ? firstIncoming.get().getTtl() : 0);
        stream.setStartTimestamp(packets.get(0).getTimestamp());
        stream.setEndTimestamp(packets.get(packets.size() - 1).getTimestamp());
        stream.setService(serviceOptional.get());

        if (ignoreEmptyPackets) {
            packets.removeIf(packet -> packet.getContent().length == 0);

            if (packets.isEmpty()) {
                log.debug("Стрим состоит только из пустых пакетов и не будет сохранен");
                return false;
            }
        }

        if(unpackGzippedHttp) {
            boolean gzipStarted = false;
            //byte[] gzipContent = null;
            int gzipStartPacket = 0;
            int gzipEndPacket;

            for (int i = 0; i < packets.size(); i++) {
                Packet packet = packets.get(i);

                if (packet.isIncoming() && gzipStarted) {
                    gzipEndPacket = i - 1;

                    List<Packet> cut = packets.subList(gzipStartPacket, gzipEndPacket + 1);

                    Packet decompressed = decompressGzipPackets(cut);
                    if(decompressed != null) {
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
                        if(decompressed != null) {
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
                if(decompressed != null) {
                    packets.removeAll(cut);
                    packets.add(gzipStartPacket, decompressed);
                }
            }
        }

        Stream savedStream = save(stream);

        List<ru.serega6531.packmate.model.Packet> savedPackets = new ArrayList<>();
        Set<Pattern> matches = new HashSet<>();

        for (ru.serega6531.packmate.model.Packet packet : packets) {
            packet.setStream(savedStream);
            savedPackets.add(packetService.save(packet));
            matches.addAll(patternService.findMatching(packet.getContent(), packet.isIncoming()));
        }

        savedStream.setFoundPatterns(new ArrayList<>(matches));
        savedStream.setPackets(savedPackets);
        savedStream = save(savedStream);

        subscriptionService.broadcastNewStream(savedStream);
        return true;
    }

    private Packet decompressGzipPackets(List<Packet> packets) {
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
        if (streamOptional.isPresent()) {
            final Stream stream = streamOptional.get();
            stream.setFavorite(favorite);
            repository.save(stream);
        }
    }

    public List<Stream> findFavorites(Pagination pagination) {
        PageRequest page = PageRequest.of(0, pagination.getPageSize(), pagination.getDirection(), "id");

        if (pagination.getPattern() != null) { // задан паттерн для поиска
            if (pagination.getDirection() == Sort.Direction.ASC) {  // более новые стримы
                return repository.findAllByIdGreaterThanAndFavoriteIsTrueAndFoundPatternsContaining(pagination.getStartingFrom(), pagination.getPattern(), page);
            } else {  // более старые стримы
                return repository.findAllByIdLessThanAndFavoriteIsTrueAndFoundPatternsContaining(pagination.getStartingFrom(), pagination.getPattern(), page);
            }
        } else {
            if (pagination.getDirection() == Sort.Direction.ASC) {  // более новые стримы
                return repository.findAllByIdGreaterThanAndFavoriteIsTrue(pagination.getStartingFrom(), page);
            } else {  // более старые стримы
                return repository.findAllByIdLessThanAndFavoriteIsTrue(pagination.getStartingFrom(), page);
            }
        }
    }

    public List<Stream> findFavoritesByService(Pagination pagination, CtfService service) {
        PageRequest page = PageRequest.of(0, pagination.getPageSize(), pagination.getDirection(), "id");

        if (pagination.getPattern() != null) { // задан паттерн для поиска
            if (pagination.getDirection() == Sort.Direction.ASC) {  // более новые стримы
                return repository.findAllByServiceAndIdGreaterThanAndFavoriteIsTrueAndFoundPatternsContaining(service, pagination.getStartingFrom(), pagination.getPattern(), page);
            } else {  // более старые стримы
                return repository.findAllByServiceAndIdLessThanAndFavoriteIsTrueAndFoundPatternsContaining(service, pagination.getStartingFrom(), pagination.getPattern(), page);
            }
        } else {
            if (pagination.getDirection() == Sort.Direction.ASC) {  // более новые стримы
                return repository.findAllByServiceAndIdGreaterThanAndFavoriteIsTrue(service, pagination.getStartingFrom(), page);
            } else {  // более старые стримы
                return repository.findAllByServiceAndIdLessThanAndFavoriteIsTrue(service, pagination.getStartingFrom(), page);
            }
        }
    }

    public List<Stream> findAll(Pagination pagination) {
        PageRequest page = PageRequest.of(0, pagination.getPageSize(), pagination.getDirection(), "id");

        if (pagination.getPattern() != null) { // задан паттерн для поиска
            if (pagination.getDirection() == Sort.Direction.ASC) {  // более новые стримы
                return repository.findAllByIdGreaterThanAndFoundPatternsContaining(pagination.getStartingFrom(), pagination.getPattern(), page);
            } else {  // более старые стримы
                return repository.findAllByIdLessThanAndFoundPatternsContaining(pagination.getStartingFrom(), pagination.getPattern(), page);
            }
        } else {
            if (pagination.getDirection() == Sort.Direction.ASC) {  // более новые стримы
                return repository.findAllByIdGreaterThan(pagination.getStartingFrom(), page);
            } else {  // более старые стримы
                return repository.findAllByIdLessThan(pagination.getStartingFrom(), page);
            }
        }
    }

    public List<Stream> findAllByService(Pagination pagination, CtfService service) {
        PageRequest page = PageRequest.of(0, pagination.getPageSize(), pagination.getDirection(), "id");

        if (pagination.getPattern() != null) { // задан паттерн для поиска
            if (pagination.getDirection() == Sort.Direction.ASC) {  // более новые стримы
                return repository.findAllByServiceAndIdGreaterThanAndFoundPatternsContaining(service, pagination.getStartingFrom(), pagination.getPattern(), page);
            } else {  // более старые стримы
                return repository.findAllByServiceAndIdLessThanAndFoundPatternsContaining(service, pagination.getStartingFrom(), pagination.getPattern(), page);
            }
        } else {
            if (pagination.getDirection() == Sort.Direction.ASC) {  // более новые стримы
                return repository.findAllByServiceAndIdGreaterThan(service, pagination.getStartingFrom(), page);
            } else {  // более старые стримы
                return repository.findAllByServiceAndIdLessThan(service, pagination.getStartingFrom(), page);
            }
        }
    }

}
