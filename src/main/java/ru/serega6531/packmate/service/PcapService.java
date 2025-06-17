package ru.serega6531.packmate.service;

import lombok.extern.slf4j.Slf4j;
import ru.serega6531.packmate.model.pojo.PcapDto;

import org.pcap4j.core.PcapNativeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.serega6531.packmate.model.enums.SubscriptionMessageType;
import ru.serega6531.packmate.model.pojo.ServiceDto;
import ru.serega6531.packmate.model.pojo.SubscriptionMessage;
import ru.serega6531.packmate.pcap.NoOpPcapWorker;
import ru.serega6531.packmate.pcap.PcapWorker;

import java.util.Collection;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PcapService {

    private boolean started = false;

    private final SubscriptionService subscriptionService;
    private final PcapWorker worker;

    @Autowired
    public PcapService(SubscriptionService subscriptionService, PcapWorker worker) {
        this.subscriptionService = subscriptionService;
        this.worker = worker;
    }

    public boolean isStarted() {
        return started || worker instanceof NoOpPcapWorker;
    }

    public synchronized void start(PcapDto dto) throws PcapNativeException {
        if(!started) {
            started = true;
            subscriptionService.broadcast(new SubscriptionMessage(SubscriptionMessageType.PCAP_STARTED, null));
            worker.start(dto);
        }
    }

    public void updateFilter(Collection<ServiceDto> services) {
        String filter;

        if (services.isEmpty()) {
            filter = "tcp or udp";
        } else {
            final String ports = services.stream()
                    .map(ServiceDto::getPort)
                    .map(p -> "port " + p)
                    .collect(Collectors.joining(" or "));

            final String format = "(tcp or udp) and (%s)";
            filter = String.format(format, ports);
        }

        log.debug("New filter: " + filter);

        worker.setFilter(filter);
    }

    public String getExecutorState() {
        return worker.getExecutorState();
    }

}
