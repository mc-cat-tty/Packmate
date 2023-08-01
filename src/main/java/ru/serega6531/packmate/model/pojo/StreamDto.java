package ru.serega6531.packmate.model.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import ru.serega6531.packmate.model.enums.Protocol;

import java.time.Instant;
import java.util.Set;

@Data
public class StreamDto {

    private Long id;
    private int service;
    private Protocol protocol;
    @JsonFormat(shape = JsonFormat.Shape.NUMBER, without = JsonFormat.Feature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS)
    private Instant startTimestamp;
    @JsonFormat(shape = JsonFormat.Shape.NUMBER, without = JsonFormat.Feature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS)
    private Instant endTimestamp;
    private Set<Integer> foundPatternsIds;
    private boolean favorite;
    private int ttl;
    private String userAgentHash;
    private int sizeBytes;
    private int packetsCount;

}
