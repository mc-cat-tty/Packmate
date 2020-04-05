package ru.serega6531.packmate.service;

import lombok.Getter;
import org.pcap4j.core.PcapNativeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.serega6531.packmate.pcap.PcapWorker;

@Service
public class PcapService {

    @Getter
    private boolean started = false;

    private final PcapWorker worker;

    @Autowired
    public PcapService(PcapWorker worker) {
        this.worker = worker;
    }

    public synchronized void start() throws PcapNativeException {
        if(!started) {
            started = true;
            worker.start();
        }
    }

}
