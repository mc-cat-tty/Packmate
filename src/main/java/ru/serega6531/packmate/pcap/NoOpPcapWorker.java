package ru.serega6531.packmate.pcap;

import org.pcap4j.core.PcapNativeException;
import ru.serega6531.packmate.model.enums.Protocol;

public class NoOpPcapWorker implements PcapWorker {
    @Override
    public void start() throws PcapNativeException {
    }

    @Override
    public void stop() {
    }

    @Override
    public void closeAllStreams(Protocol protocol) {
    }

    @Override
    public int closeTimeoutStreams(Protocol protocol, long timeoutMillis) {
        return 0;
    }

    @Override
    public void setFilter(String filter) {
    }
}
