package ru.serega6531.packmate.model.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.Instant;
import java.util.Set;

@Data
public class PacketDto {

    private Long id;
    private Set<FoundPatternDto> matches;
    @JsonFormat(shape = JsonFormat.Shape.NUMBER, without = JsonFormat.Feature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS)
    private Instant timestamp;
    private boolean incoming;
    private boolean ungzipped;
    private boolean webSocketParsed;
    private boolean tlsDecrypted;
    private boolean hasHttpBody;
    private byte[] content;

}
