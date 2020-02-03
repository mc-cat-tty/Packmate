package ru.serega6531.packmate;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
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
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@Slf4j
public class PcapWorker implements PacketListener {

    private final ServicesService servicesService;
    private final StreamService streamService;

    private final PcapNetworkInterface device;
    private PcapHandle pcap = null;
    private final ExecutorService executorService;

    private final InetAddress localIp;

    private long packetIdCounter = 0;  // оно однопоточное, так что пусть будет без atomic

    private final Map<UnfinishedStream, List<ru.serega6531.packmate.model.Packet>> unfinishedStreams = new HashMap<>();

    // в следующих мапах в сетах srcIp соответствующего пакета
    private final Map<UnfinishedStream, Set<String>> fins = new HashMap<>();
    private final Map<UnfinishedStream, Set<String>> acks = new HashMap<>();

    @Autowired
    public PcapWorker(ServicesService servicesService,
                      StreamService streamService,
                      @Value("${interface-name}") String interfaceName,
                      @Value("${local-ip}") String localIpString) throws PcapNativeException, UnknownHostException {
        this.servicesService = servicesService;
        this.streamService = streamService;

        this.localIp = InetAddress.getByName(localIpString);
        if(!(this.localIp instanceof Inet4Address)) {
            throw new IllegalArgumentException("Only ipv4 local ips are supported");
        }

        BasicThreadFactory factory = new BasicThreadFactory.Builder()
                .namingPattern("pcap-worker").build();
        executorService = Executors.newSingleThreadExecutor(factory);
        device = Pcaps.getDevByName(interfaceName);
    }

    void start() throws PcapNativeException {
        log.info("Using interface " + device.getName());
        pcap = device.openLive(65536, PcapNetworkInterface.PromiscuousMode.PROMISCUOUS, 100);

        executorService.execute(() -> {
            try {
                log.info("Intercept started");
                pcap.loop(-1, this);   // использовать другой executor?
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                // выходим
            } catch (Exception e) {
                log.error("Error while capturing packet", e);
                stop();
            }
        });
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
        Inet4Address sourceIp;
        Inet4Address destIp;
        int sourcePort;
        int destPort;
        byte ttl;
        byte[] content;
        Protocol protocol;
        boolean ack = false;
        boolean fin = false;
        boolean rst = false;

        if (rawPacket.contains(IpV4Packet.class)) {
            final IpV4Packet.IpV4Header header = rawPacket.get(IpV4Packet.class).getHeader();
            sourceIp = header.getSrcAddr();
            destIp = header.getDstAddr();
            ttl = header.getTtl();
        } else {
            return;
        }

        if (rawPacket.contains(TcpPacket.class)) {
            final TcpPacket packet = rawPacket.get(TcpPacket.class);
            final TcpPacket.TcpHeader header = packet.getHeader();
            sourcePort = header.getSrcPort().valueAsInt();
            destPort = header.getDstPort().valueAsInt();
            ack = header.getAck();
            fin = header.getFin();
            rst = header.getRst();
            content = packet.getPayload() != null ? packet.getPayload().getRawData() : new byte[0];
            protocol = Protocol.TCP;
        } else if (rawPacket.contains(UdpPacket.class)) {
            final UdpPacket packet = rawPacket.get(UdpPacket.class);
            final UdpPacket.UdpHeader header = packet.getHeader();
            sourcePort = header.getSrcPort().valueAsInt();
            destPort = header.getDstPort().valueAsInt();
            content = packet.getPayload() != null ? packet.getPayload().getRawData() : new byte[0];
            protocol = Protocol.UDP;
        } else {
            return;
        }

        String sourceIpString = sourceIp.getHostAddress();
        String destIpString = destIp.getHostAddress();

        final Optional<CtfService> serviceOptional =
                servicesService.findService(sourceIp, sourcePort, destIp, destPort);

        if (serviceOptional.isPresent()) {
            String sourceIpAndPort = sourceIpString + ":" + sourcePort;
            String destIpAndPort = destIpString + ":" + destPort;

            UnfinishedStream stream = addNewPacket(sourceIp, destIp, sourcePort, destPort, ttl, content, protocol);

            if (log.isDebugEnabled()) {
                log.debug("{} {} {}:{} -> {}:{}, номер пакета {}",
                        protocol.name().toLowerCase(), serviceOptional.get(), sourceIpString, sourcePort, destIpString, destPort,
                        unfinishedStreams.get(stream).size());
            }

            if (protocol == Protocol.TCP) {  // udp не имеет фазы закрытия, поэтому закрываем по таймауту
                checkTcpTermination(ack, fin, rst, sourceIpAndPort, destIpAndPort, stream);
            }
        } else { // сервис не найден
            if (log.isTraceEnabled()) {
                log.trace("{} {}:{} -> {}:{}", protocol.name().toLowerCase(), sourceIpString, sourcePort, destIpString, destPort);
            }
        }
    }

    private UnfinishedStream addNewPacket(Inet4Address sourceIp, Inet4Address destIp,
                                          int sourcePort, int destPort, byte ttl, byte[] content, Protocol protocol) {
        boolean incoming = destIp.equals(localIp);

        UnfinishedStream stream = new UnfinishedStream(sourceIp, destIp, sourcePort, destPort, protocol);

        ru.serega6531.packmate.model.Packet packet = ru.serega6531.packmate.model.Packet.builder()
                .tempId(packetIdCounter++)
                .ttl(ttl)
                .timestamp(System.currentTimeMillis())
                .incoming(incoming)
                .content(content)
                .build();

        if (unfinishedStreams.containsKey(stream)) {
            unfinishedStreams.get(stream).add(packet);
        } else {
            log.debug("Начат новый стрим");
            List<ru.serega6531.packmate.model.Packet> packets = new ArrayList<>();
            packets.add(packet);
            unfinishedStreams.put(stream, packets);
        }
        return stream;
    }

    private void checkTcpTermination(boolean ack, boolean fin, boolean rst, String sourceIpAndPort, String destIpAndPort, UnfinishedStream stream) {
        if (!fins.containsKey(stream)) {
            fins.put(stream, new HashSet<>());
        }

        if (!acks.containsKey(stream)) {
            acks.put(stream, new HashSet<>());
        }

        final Set<String> finsForStream = fins.get(stream);
        final Set<String> acksForStream = acks.get(stream);

        if (fin) {
            finsForStream.add(sourceIpAndPort);
        }

        if (ack && finsForStream.contains(destIpAndPort)) {  // проверяем destIp, потому что ищем ответ на его fin
            acksForStream.add(sourceIpAndPort);
        }

        // если соединение разорвано или закрыто с помощью fin-ack-fin-ack
        if (rst || (acksForStream.contains(sourceIpAndPort) && acksForStream.contains(destIpAndPort))) {
            streamService.saveNewStream(stream, unfinishedStreams.get(stream));

            unfinishedStreams.remove(stream);
            fins.remove(stream);
            acks.remove(stream);
        }
    }

    int closeTimeoutStreams(Protocol protocol, long timeoutMillis) {
        int streamsClosed = 0;
        final Iterator<Map.Entry<UnfinishedStream, List<ru.serega6531.packmate.model.Packet>>> iterator = unfinishedStreams.entrySet().iterator();

        while (iterator.hasNext()) {
            final Map.Entry<UnfinishedStream, List<ru.serega6531.packmate.model.Packet>> entry = iterator.next();
            final UnfinishedStream stream = entry.getKey();

            if (stream.getProtocol() == protocol) {
                final List<ru.serega6531.packmate.model.Packet> packets = entry.getValue();
                if (System.currentTimeMillis() - packets.get(packets.size() - 1).getTimestamp() > timeoutMillis) {
                    if (streamService.saveNewStream(stream, packets)) {
                        streamsClosed++;
                    }

                    iterator.remove();

                    if (protocol == Protocol.TCP) {
                        fins.remove(stream);
                        acks.remove(stream);
                    }
                }
            }
        }

        return streamsClosed;
    }

}
