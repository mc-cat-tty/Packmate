package ru.serega6531.packmate.model.pojo;

import lombok.Data;

@Data
public class ServiceDto {

    private int port;
    private String name;
    private boolean decryptTls;
    private boolean processChunkedEncoding;
    private boolean ungzipHttp;
    private boolean urldecodeHttpRequests;
    private boolean mergeAdjacentPackets;
    private boolean parseWebSockets;

}
