package ru.serega6531.packmate.pcap;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.threads.InlineExecutorService;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.Pcaps;
import org.pcap4j.packet.Packet;
import ru.serega6531.packmate.model.enums.Protocol;
import ru.serega6531.packmate.model.enums.SubscriptionMessageType;
import ru.serega6531.packmate.model.pojo.SubscriptionMessage;
import ru.serega6531.packmate.service.ServicesService;
import ru.serega6531.packmate.service.StreamService;
import ru.serega6531.packmate.service.SubscriptionService;

import java.io.EOFException;
import java.io.File;
import java.net.UnknownHostException;

@Slf4j
public class FilePcapWorker extends AbstractPcapWorker {

    private final SubscriptionService subscriptionService;
    private final File file;

    public FilePcapWorker(ServicesService servicesService,
                          StreamService streamService,
                          SubscriptionService subscriptionService,
                          String localIpString,
                          String filename) throws UnknownHostException {
        super(servicesService, streamService, localIpString);
        this.subscriptionService = subscriptionService;

        file = new File(filename);
        if(!file.exists()) {
            throw new IllegalArgumentException("File " + file.getAbsolutePath() + " does not exist");
        }

        processorExecutorService = new InlineExecutorService();
    }

    @SneakyThrows
    @Override
    public void start() {
        log.info("Using file " + file.getAbsolutePath());
        pcap = Pcaps.openOffline(file.getAbsolutePath());

        applyFilter();

        loopExecutorService.execute(this::runScan);
    }

    @SneakyThrows
    private void runScan() {
        while (pcap.isOpen()) {
            try {
                final Packet packet = pcap.getNextPacketEx();
                gotPacket(packet);
            } catch (PcapNativeException e) {
                log.error("Pcap read", e);
                Thread.sleep(100);
            } catch (EOFException e) {
                stop();

                log.info("All packets processed");
                break;
            }
        }
    }

    @SneakyThrows
    public void stop() {
        if (pcap != null && pcap.isOpen()) {
            pcap.close();
            log.info("Pcap closed");
        }

        closeAllStreams(Protocol.TCP);
        closeAllStreams(Protocol.UDP);

        subscriptionService.broadcast(new SubscriptionMessage(SubscriptionMessageType.PCAP_STOPPED, null));
    }
}
