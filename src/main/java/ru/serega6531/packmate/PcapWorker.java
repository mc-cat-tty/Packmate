package ru.serega6531.packmate;

import com.google.common.collect.*;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.pcap4j.core.*;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.TcpPacket;
import org.pcap4j.packet.UdpPacket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.serega6531.packmate.model.CtfService;
import ru.serega6531.packmate.model.enums.Protocol;
import ru.serega6531.packmate.model.pojo.UnfinishedStream;
import ru.serega6531.packmate.service.ServicesService;
import ru.serega6531.packmate.service.StreamService;

import javax.annotation.PreDestroy;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Component
@Slf4j
public class PcapWorker implements PacketListener {

    private final ServicesService servicesService;
    private final StreamService streamService;

    private final PcapNetworkInterface device;
    private PcapHandle pcap = null;
    private final ExecutorService listenerExecutorService;

    private final InetAddress localIp;

    private long packetIdCounter = 0;  // оно однопоточное, так что пусть будет без atomic

    private final ListMultimap<UnfinishedStream, ru.serega6531.packmate.model.Packet> unfinishedTcpStreams = ArrayListMultimap.create();
    private final ListMultimap<UnfinishedStream, ru.serega6531.packmate.model.Packet> unfinishedUdpStreams = ArrayListMultimap.create();

    // в следующих мапах в значениях находится srcIp соответствующего пакета
    private final SetMultimap<UnfinishedStream, ImmutablePair<Inet4Address, Integer>> fins = HashMultimap.create();
    private final SetMultimap<UnfinishedStream, ImmutablePair<Inet4Address, Integer>> acks = HashMultimap.create();

    @Autowired
    public PcapWorker(ServicesService servicesService,
                      StreamService streamService,
                      @Value("${interface-name}") String interfaceName,
                      @Value("${local-ip}") String localIpString) throws PcapNativeException, UnknownHostException {
        this.servicesService = servicesService;
        this.streamService = streamService;

        this.localIp = InetAddress.getByName(localIpString);
        if (!(this.localIp instanceof Inet4Address)) {
            throw new IllegalArgumentException("Only ipv4 local ips are supported");
        }

        BasicThreadFactory factory = new BasicThreadFactory.Builder()
                .namingPattern("pcap-worker-listener").build();
        listenerExecutorService = Executors.newSingleThreadExecutor(factory);
        device = Pcaps.getDevByName(interfaceName);
    }

    void start() throws PcapNativeException {
        log.info("Using interface " + device.getName());
        pcap = device.openLive(65536, PcapNetworkInterface.PromiscuousMode.PROMISCUOUS, 100);

        BasicThreadFactory factory = new BasicThreadFactory.Builder()
                .namingPattern("pcap-worker-loop").build();
        ExecutorService loopExecutorService = Executors.newSingleThreadExecutor(factory);
        try {
            log.info("Intercept started");
            pcap.loop(-1, this, loopExecutorService);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
            // выходим
        } catch (Exception e) {
            log.error("Error while capturing packet", e);
            stop();
        }
    }

    @PreDestroy
    @SneakyThrows
    private void stop() {
        if (pcap != null && pcap.isOpen()) {
            pcap.breakLoop();
            pcap.close();
        }

        log.info("Intercept stopped");
    }

    public void gotPacket(Packet rawPacket) {
        if (!rawPacket.contains(IpV4Packet.class)) {
            return;
        }

        if (rawPacket.contains(TcpPacket.class)) {
            gotTcpPacket(rawPacket);
        } else if (rawPacket.contains(UdpPacket.class)) {
            gotUdpPacket(rawPacket);
        }
    }

    private void gotTcpPacket(Packet rawPacket) {
        final IpV4Packet.IpV4Header ipHeader = rawPacket.get(IpV4Packet.class).getHeader();
        Inet4Address sourceIp = ipHeader.getSrcAddr();
        Inet4Address destIp = ipHeader.getDstAddr();
        byte ttl = ipHeader.getTtl();

        final TcpPacket packet = rawPacket.get(TcpPacket.class);
        final TcpPacket.TcpHeader tcpHeader = packet.getHeader();
        int sourcePort = tcpHeader.getSrcPort().valueAsInt();
        int destPort = tcpHeader.getDstPort().valueAsInt();
        boolean ack = tcpHeader.getAck();
        boolean fin = tcpHeader.getFin();
        boolean rst = tcpHeader.getRst();
        byte[] content = packet.getPayload() != null ? packet.getPayload().getRawData() : new byte[0];

        String sourceIpString = sourceIp.getHostAddress();
        String destIpString = destIp.getHostAddress();

        final Optional<CtfService> serviceOptional =
                servicesService.findService(sourceIp, sourcePort, destIp, destPort);

        if (serviceOptional.isPresent()) {
            final long time = System.currentTimeMillis();

            listenerExecutorService.execute(() -> {
                UnfinishedStream stream = addNewPacket(sourceIp, destIp, time, sourcePort, destPort, ttl, content, Protocol.TCP);

                if (log.isDebugEnabled()) {
                    log.debug("tcp {} {}:{} -> {}:{}, packet number {}",
                            serviceOptional.get(), sourceIpString, sourcePort, destIpString, destPort,
                            unfinishedTcpStreams.get(stream).size());
                }

                checkTcpTermination(ack, fin, rst, new ImmutablePair<>(sourceIp, sourcePort), new ImmutablePair<>(destIp, destPort), stream);
            });
        } else { // сервис не найден
            if (log.isTraceEnabled()) {
                log.trace("tcp {}:{} -> {}:{}", sourceIpString, sourcePort, destIpString, destPort);
            }
        }
    }

    private void gotUdpPacket(Packet rawPacket) {
        final IpV4Packet.IpV4Header ipHeader = rawPacket.get(IpV4Packet.class).getHeader();
        Inet4Address sourceIp = ipHeader.getSrcAddr();
        Inet4Address destIp = ipHeader.getDstAddr();
        byte ttl = ipHeader.getTtl();

        final UdpPacket packet = rawPacket.get(UdpPacket.class);
        final UdpPacket.UdpHeader udpHeader = packet.getHeader();
        int sourcePort = udpHeader.getSrcPort().valueAsInt();
        int destPort = udpHeader.getDstPort().valueAsInt();
        byte[] content = packet.getPayload() != null ? packet.getPayload().getRawData() : new byte[0];

        String sourceIpString = sourceIp.getHostAddress();
        String destIpString = destIp.getHostAddress();

        final Optional<CtfService> serviceOptional =
                servicesService.findService(sourceIp, sourcePort, destIp, destPort);

        if (serviceOptional.isPresent()) {
            final long time = System.currentTimeMillis();

            listenerExecutorService.execute(() -> {
                UnfinishedStream stream = addNewPacket(sourceIp, destIp, time, sourcePort, destPort, ttl, content, Protocol.UDP);

                if (log.isDebugEnabled()) {
                    log.debug("udp {} {}:{} -> {}:{}, packet number {}",
                            serviceOptional.get(), sourceIpString, sourcePort, destIpString, destPort,
                            unfinishedUdpStreams.get(stream).size());
                }
            });
        } else { // сервис не найден
            if (log.isTraceEnabled()) {
                log.trace("udp {}:{} -> {}:{}", sourceIpString, sourcePort, destIpString, destPort);
            }
        }
    }

    private UnfinishedStream addNewPacket(Inet4Address sourceIp, Inet4Address destIp, long time,
                                          int sourcePort, int destPort, byte ttl, byte[] content, Protocol protocol) {
        var incoming = destIp.equals(localIp);
        var stream = new UnfinishedStream(sourceIp, destIp, sourcePort, destPort, protocol);

        var packet = ru.serega6531.packmate.model.Packet.builder()
                .tempId(packetIdCounter++)
                .ttl(ttl)
                .timestamp(time)
                .incoming(incoming)
                .content(content)
                .build();

        final var streams = (protocol == Protocol.TCP) ? this.unfinishedTcpStreams : this.unfinishedUdpStreams;

        if (!streams.containsKey(stream)) {
            log.debug("New stream started");
        }

        streams.put(stream, packet);
        return stream;
    }

    /**
     * Udp не имеет фазы закрытия, поэтому закрывается только по таймауту
     */
    private void checkTcpTermination(boolean ack, boolean fin, boolean rst,
                                     ImmutablePair<Inet4Address, Integer> sourceIpAndPort,
                                     ImmutablePair<Inet4Address, Integer> destIpAndPort,
                                     UnfinishedStream stream) {

        if (fin) {
            fins.put(stream, sourceIpAndPort);
        }

        if (ack && fins.containsEntry(stream, destIpAndPort)) {  // проверяем destIp, потому что ищем ответ на его fin
            acks.put(stream, sourceIpAndPort);
        }

        // если соединение разорвано с помощью rst или закрыто с помощью fin-ack-fin-ack
        if (rst || (acks.containsEntry(stream, sourceIpAndPort) && acks.containsEntry(stream, destIpAndPort))) {
            streamService.saveNewStream(stream, unfinishedTcpStreams.get(stream));

            unfinishedTcpStreams.removeAll(stream);
            fins.removeAll(stream);
            acks.removeAll(stream);
        }
    }

    @SneakyThrows
    int closeTimeoutStreams(Protocol protocol, long timeoutMillis) {
        return listenerExecutorService.submit(() -> {
            int streamsClosed = 0;

            final long time = System.currentTimeMillis();
            final var streams = (protocol == Protocol.TCP) ? this.unfinishedTcpStreams : this.unfinishedUdpStreams;

            final var oldStreams = Multimaps.asMap(streams).entrySet().stream()
                    .filter(entry -> {
                        final var packets = entry.getValue();
                        return time - packets.get(packets.size() - 1).getTimestamp() > timeoutMillis;
                    })
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            for (var entry : oldStreams.entrySet()) {
                final UnfinishedStream stream = entry.getKey();
                final var packets = entry.getValue();

                if (streamService.saveNewStream(stream, packets)) {
                    streamsClosed++;
                }

                if (protocol == Protocol.TCP) {
                    fins.removeAll(stream);
                    acks.removeAll(stream);
                }

                streams.removeAll(stream);
            }

            return streamsClosed;
        }).get();
    }

}
