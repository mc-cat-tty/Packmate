package ru.serega6531.packmate.pcap;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.Pcaps;
import org.pcap4j.packet.Packet;
import ru.serega6531.packmate.service.ServicesService;
import ru.serega6531.packmate.service.StreamService;

import java.io.EOFException;
import java.io.File;
import java.net.UnknownHostException;

@Slf4j
public class FilePcapWorker extends AbstractPcapWorker {

    private final File file;

    public FilePcapWorker(ServicesService servicesService,
                          StreamService streamService,
                          String localIpString,
                          String filename) throws UnknownHostException {
        super(servicesService, streamService, localIpString);

        file = new File(filename);
        if(!file.exists()) {
            throw new IllegalArgumentException("File " + file.getAbsolutePath() + " does not exist");
        }
    }

    @SneakyThrows
    @Override
    public void start() {
        pcap = Pcaps.openOffline(file.getAbsolutePath());

        while (pcap.isOpen()) {
            try {
                final Packet packet = pcap.getNextPacketEx();
                gotPacket(packet);
            } catch (PcapNativeException e) {
                log.error("Pcap read", e);
                Thread.sleep(100);
            } catch (EOFException e) {
                log.info("All packets processed");
                stop();
                break;
            }
        }
    }

    @SneakyThrows
    public void stop() {
        if (pcap != null && pcap.isOpen()) {
            pcap.close();
        }

        //TODO закрывать все стримы
        log.info("Pcap closed");
    }
}
