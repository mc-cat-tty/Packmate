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
import ru.serega6531.packmate.model.Protocol;
import ru.serega6531.packmate.model.UnfinishedStream;
import ru.serega6531.packmate.service.ServicesService;
import ru.serega6531.packmate.service.StreamService;

import javax.annotation.PreDestroy;
import java.net.Inet4Address;
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

    private final String localIp;

    private long packetIdCounter = 0;  // оно однопоточное, так что пусть будет без atomic

    private final Map<UnfinishedStream, List<ru.serega6531.packmate.model.Packet>> unfinishedStreams = new HashMap<>();

    // в следующих мапах в сетах srcIp соответствующего пакета
    private final Map<UnfinishedStream, Set<String>> fins = new HashMap<>();
    private final Map<UnfinishedStream, Set<String>> acks = new HashMap<>();

    @Autowired
    public PcapWorker(ServicesService servicesService,
                      StreamService streamService,
                      @Value("${interface-name}") String interfaceName,
                      @Value("${local-ip}") String localIp) throws PcapNativeException {
        this.servicesService = servicesService;
        this.streamService = streamService;

        this.localIp = localIp;

        BasicThreadFactory factory = new BasicThreadFactory.Builder()
                .namingPattern("pcap-worker").build();
        executorService = Executors.newSingleThreadExecutor(factory);
        device = Pcaps.getDevByName(interfaceName);
    }

    public void start() throws PcapNativeException {
        System.out.println("Using interface " + device.getName());
        pcap = device.openLive(65536, PcapNetworkInterface.PromiscuousMode.PROMISCUOUS, 100);

        executorService.execute(() -> {
            try {
                log.info("Intercept started");
                pcap.loop(-1, this);   // использовать другой executor?
            } catch (InterruptedException ignored) {
                // выходим
            } catch (Exception e) {
                log.error("Error while capturing packet", e);
                stop();
            }
        });
    }

    @PreDestroy
    @SneakyThrows
    public void stop() {
        if (pcap != null && pcap.isOpen()) {
            pcap.breakLoop();
            pcap.close();
        }

        log.info("Intercept stopped");
    }

    public void gotPacket(Packet rawPacket) {
        Inet4Address sourceIp = null;
        Inet4Address destIp = null;
        String sourceIpString = null;
        String destIpString = null;
        int sourcePort = -1;
        int destPort = -1;
        byte[] content = null;
        Protocol protocol = null;
        boolean ack = false;
        boolean fin = false;
        boolean rst = false;

        if (rawPacket.contains(IpV4Packet.class)) {
            final IpV4Packet.IpV4Header header = rawPacket.get(IpV4Packet.class).getHeader();
            sourceIp = header.getSrcAddr();
            destIp = header.getDstAddr();
            sourceIpString = header.getSrcAddr().getHostAddress();
            destIpString = header.getDstAddr().getHostAddress();
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
        }

        if (sourceIpString != null && sourcePort != -1) {
            final Optional<CtfService> serviceOptional =
                    servicesService.findService(localIp, sourceIpString, sourcePort, destIpString, destPort);

            if (serviceOptional.isPresent()) {
                String sourceIpAndPort = sourceIpString + ":" + sourcePort;
                String destIpAndPort = destIpString + ":" + destPort;

                boolean incoming = destIpString.equals(localIp);

                UnfinishedStream stream = new UnfinishedStream(sourceIp, destIp, sourcePort, destPort, protocol);

                ru.serega6531.packmate.model.Packet packet = ru.serega6531.packmate.model.Packet.builder()
                        .tempId(packetIdCounter++)
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

                log.debug("{} {} {}:{} -> {}:{}, номер пакета {}",
                        protocol.name().toLowerCase(), serviceOptional.get(), sourceIpString, sourcePort, destIpString, destPort,
                        unfinishedStreams.get(stream).size());

                if (protocol == Protocol.TCP) {
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

                    if (rst || (acksForStream.contains(sourceIpAndPort) && acksForStream.contains(destIpAndPort))) {
                        streamService.saveNewStream(stream, unfinishedStreams.get(stream));

                        unfinishedStreams.remove(stream);
                        fins.remove(stream);
                        acks.remove(stream);
                    }
                }
            } else {
                log.trace("{} {}:{} -> {}:{}", protocol.name().toLowerCase(), sourceIpString, sourcePort, destIpString, destPort);
            }
        }
    }

    public int closeTimeoutStreams(Protocol protocol, long timeoutMillis) {
        int streamsClosed = 0;
        final Iterator<Map.Entry<UnfinishedStream, List<ru.serega6531.packmate.model.Packet>>> iterator = unfinishedStreams.entrySet().iterator();

        while (iterator.hasNext()) {
            final Map.Entry<UnfinishedStream, List<ru.serega6531.packmate.model.Packet>> entry = iterator.next();
            final UnfinishedStream stream = entry.getKey();

            if (stream.getProtocol() == protocol) {
                final List<ru.serega6531.packmate.model.Packet> packets = entry.getValue();
                if (System.currentTimeMillis() - packets.get(packets.size() - 1).getTimestamp() > timeoutMillis) {
                    if(streamService.saveNewStream(stream, packets)) {
                        streamsClosed++;
                    }

                    iterator.remove();

                    if(protocol == Protocol.TCP) {
                        fins.remove(stream);
                        acks.remove(stream);
                    }
                }
            }
        }

        return streamsClosed;
    }

}
