package ru.serega6531.packmate.service;

import lombok.Getter;
import org.pcap4j.core.PcapNativeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.serega6531.packmate.model.enums.SubscriptionMessageType;
import ru.serega6531.packmate.model.pojo.SubscriptionMessage;
import ru.serega6531.packmate.pcap.PcapWorker;

@Service
public class PcapService {

    @Getter
    private boolean started = false;

    private final SubscriptionService subscriptionService;
    private final PcapWorker worker;

    @Autowired
    public PcapService(SubscriptionService subscriptionService, PcapWorker worker) {
        this.subscriptionService = subscriptionService;
        this.worker = worker;
    }

    public synchronized void start() throws PcapNativeException {
        if(!started) {
            started = true;
            subscriptionService.broadcast(new SubscriptionMessage(SubscriptionMessageType.PCAP_STARTED, null));
            worker.start();
        }
    }

}
