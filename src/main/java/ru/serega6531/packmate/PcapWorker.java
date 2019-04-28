package ru.serega6531.packmate;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.pcap4j.core.PcapHandle;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.core.Pcaps;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.TcpPacket;
import org.pcap4j.packet.UdpPacket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.serega6531.packmate.model.CtfService;
import ru.serega6531.packmate.service.PacketService;
import ru.serega6531.packmate.service.PatternService;
import ru.serega6531.packmate.service.ServicesService;
import ru.serega6531.packmate.service.StreamService;

import javax.annotation.PreDestroy;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

@Component
@Slf4j
public class PcapWorker {

    private final ServicesService servicesService;
    private final StreamService streamService;
    private final PacketService packetService;
    private final PatternService patternService;

    private final PcapNetworkInterface device;
    private PcapHandle pcap = null;
    private final ExecutorService executorService;

    private final String localIp;

    @Autowired
    public PcapWorker(ServicesService servicesService,
                      StreamService streamService,
                      PacketService packetService,
                      PatternService patternService,
                      @Value("${interface-name}") String interfaceName,
                      @Value("${local-ip}") String localIp) throws PcapNativeException {
        this.servicesService = servicesService;
        this.streamService = streamService;
        this.packetService = packetService;
        this.patternService = patternService;

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
                while (true) {
                    if (pcap.isOpen()) {
                        try {
                            final Packet packet = pcap.getNextPacketEx();
                            processPacket(packet);
                        } catch (TimeoutException ignored) {
                        }
                    } else {
                        break;
                    }
                }
            } catch (Exception e) {
                log.error("Error while capturing packet", e);
                stop();
            }
        });
    }

    @PreDestroy
    public void stop() {
        if (pcap != null && pcap.isOpen()) {
            pcap.close();
        }

        log.info("Intercept stopped");
    }

    private void processPacket(Packet rawPacket) {
        String sourceIp = null;
        String destIp = null;
        int sourcePort = -1;
        int destPort = -1;
        byte[] content = null;

        if(rawPacket.contains(IpV4Packet.class)){
            final IpV4Packet.IpV4Header header = rawPacket.get(IpV4Packet.class).getHeader();
            sourceIp = header.getSrcAddr().getHostAddress();
            destIp = header.getDstAddr().getHostAddress();
        }

        if(rawPacket.contains(TcpPacket.class)) {
            final TcpPacket packet = rawPacket.get(TcpPacket.class);
            final TcpPacket.TcpHeader header = packet.getHeader();
            sourcePort = header.getSrcPort().valueAsInt();
            destPort = header.getDstPort().valueAsInt();
            content = packet.getRawData();
        } else if(rawPacket.contains(UdpPacket.class)) {
            final UdpPacket packet = rawPacket.get(UdpPacket.class);
            final UdpPacket.UdpHeader header = packet.getHeader();
            sourcePort = header.getSrcPort().valueAsInt();
            destPort = header.getDstPort().valueAsInt();
            content = packet.getRawData();
        }

        if(sourceIp != null && sourcePort != -1) {
            Optional<CtfService> serviceOptional = Optional.empty();

            if(sourceIp.equals(localIp)) {
                serviceOptional = servicesService.findByPort(sourcePort);
            } else if(destIp.equals(localIp)) {
                serviceOptional = servicesService.findByPort(destPort);
            }

            if(serviceOptional.isPresent()) {
                log.info("{} {}:{} -> {}:{}", serviceOptional, sourceIp, sourcePort, destIp, destPort);
            }
        }
    }
}
