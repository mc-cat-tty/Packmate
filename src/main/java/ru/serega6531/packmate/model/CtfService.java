package ru.serega6531.packmate.model;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Data
@Entity
@Table(name = "service")
public class CtfService {

    @Id
    private int port;

    private String name;

    private boolean decryptTls;

    private boolean processChunkedEncoding;

    private boolean ungzipHttp;

    private boolean urldecodeHttpRequests;

    private boolean mergeAdjacentPackets;

    private boolean parseWebSockets;

}