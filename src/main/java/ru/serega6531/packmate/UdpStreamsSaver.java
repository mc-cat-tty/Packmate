package ru.serega6531.packmate;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class UdpStreamsSaver {

    private final PcapWorker pcapWorker;

    @Autowired
    public UdpStreamsSaver(PcapWorker pcapWorker) {
        this.pcapWorker = pcapWorker;
    }

    @Scheduled(fixedRateString = "PT${udp-stream-check-interval}S")
    public void saveStreams() {
        final int streamsClosed = pcapWorker.closeUdpStreams();
        if(streamsClosed > 0) {
            log.info("Закрыто {} udp стримов", streamsClosed);
        }
    }

}
