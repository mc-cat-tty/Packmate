package ru.serega6531.packmate.model.pojo;

import lombok.Getter;

import java.util.Map;

@Getter
public class CountersHolder {

    private final Map<Integer, Integer> servicesPackets;
    private final Map<Integer, Integer> servicesStreams;

    private final int totalPackets;
    private final int totalStreams;

    public CountersHolder(Map<Integer, Integer> servicesPackets, Map<Integer, Integer> servicesStreams,
                          int totalPackets, int totalStreams) {
        this.servicesPackets = servicesPackets;
        this.servicesStreams = servicesStreams;
        this.totalPackets = totalPackets;
        this.totalStreams = totalStreams;
    }
}
