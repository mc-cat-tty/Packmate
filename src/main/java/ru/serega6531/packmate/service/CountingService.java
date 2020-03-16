package ru.serega6531.packmate.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.serega6531.packmate.model.pojo.Counter;

import java.util.HashMap;
import java.util.Map;

@Service
public class CountingService {

    private Map<Integer, Counter> servicesPackets = new HashMap<>();
    private Map<Integer, Counter> servicesStreams = new HashMap<>();

    private Counter totalPackets = new Counter();
    private Counter totalStreams = new Counter();

    void countStream(int serviceId, int packets) {
        getCounter(servicesPackets, serviceId).increment(packets);
        getCounter(servicesStreams, serviceId).increment();

        totalPackets.increment(packets);
        totalStreams.increment();
    }

    @Scheduled(cron = "0 * * ? * *")
    public void sendCounters() {
        //TODO
    }

    private Counter getCounter(Map<Integer, Counter> counters, int serviceId) {
        return counters.computeIfAbsent(serviceId, c -> new Counter());
    }

}
