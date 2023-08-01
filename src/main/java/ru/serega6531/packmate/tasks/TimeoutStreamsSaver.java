package ru.serega6531.packmate.tasks;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.serega6531.packmate.model.enums.Protocol;
import ru.serega6531.packmate.pcap.PcapWorker;
import ru.serega6531.packmate.properties.PackmateProperties;

import java.time.Duration;

@Component
@Slf4j
@ConditionalOnProperty(name = "packmate.capture-mode", havingValue = "LIVE")
public class TimeoutStreamsSaver {

    private final PcapWorker pcapWorker;
    private final Duration udpStreamTimeoutMillis;
    private final Duration tcpStreamTimeoutMillis;

    @Autowired
    public TimeoutStreamsSaver(PcapWorker pcapWorker,
                               PackmateProperties properties) {
        this.pcapWorker = pcapWorker;
        this.udpStreamTimeoutMillis = properties.timeout().udpStreamTimeout();
        this.tcpStreamTimeoutMillis = properties.timeout().tcpStreamTimeout();
    }

    @Scheduled(fixedRateString = "PT${packmate.timeout.check-interval}S", initialDelayString = "PT${packmate.timeout.check-interval}S")
    public void saveStreams() {
        int streamsClosed = pcapWorker.closeTimeoutStreams(Protocol.UDP, udpStreamTimeoutMillis);
        if (streamsClosed > 0) {
            log.info("{} udp streams closed", streamsClosed);
        }

        streamsClosed = pcapWorker.closeTimeoutStreams(Protocol.TCP, tcpStreamTimeoutMillis);
        if (streamsClosed > 0) {
            log.info("{} tcp streams closed", streamsClosed);
        }
    }

}
