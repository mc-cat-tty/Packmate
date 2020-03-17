package ru.serega6531.packmate.model.pojo;

import lombok.Getter;

import java.util.Map;

@Getter
public class CountersHolder {

    private Map<Integer, Integer> servicesPackets;
    private Map<Integer, Integer> servicesStreams;

    private int totalPackets;
    private int totalStreams;

    public CountersHolder(Map<Integer, Integer> servicesPackets, Map<Integer, Integer> servicesStreams,
                          int totalPackets, int totalStreams) {
        this.servicesPackets = servicesPackets;
        this.servicesStreams = servicesStreams;
        this.totalPackets = totalPackets;
        this.totalStreams = totalStreams;
    }
}
