package ru.serega6531.packmate.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.exceptions.InvalidDataException;
import org.java_websocket.exceptions.InvalidHandshakeException;
import org.java_websocket.extensions.permessage_deflate.PerMessageDeflateExtension;
import org.java_websocket.framing.DataFrame;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.HandshakeImpl1Client;
import org.java_websocket.handshake.HandshakeImpl1Server;
import ru.serega6531.packmate.model.Packet;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

@Slf4j
public class WebSocketsParser {

    private static final java.util.regex.Pattern WEBSOCKET_KEY_PATTERN =
            java.util.regex.Pattern.compile("Sec-WebSocket-Key: (.+)\\r\\n");
    private static final java.util.regex.Pattern WEBSOCKET_EXTENSIONS_PATTERN =
            java.util.regex.Pattern.compile("Sec-WebSocket-Extensions?: (.+)\\r\\n");
    private static final java.util.regex.Pattern WEBSOCKET_VERSION_PATTERN =
            java.util.regex.Pattern.compile("Sec-WebSocket-Version: (\\d+)\\r\\n");
    private static final java.util.regex.Pattern WEBSOCKET_ACCEPT_PATTERN =
            java.util.regex.Pattern.compile("Sec-WebSocket-Accept: (.+)\\r\\n");

    private static final String WEBSOCKET_EXTENSION_HEADER = "Sec-WebSocket-Extension: permessage-deflate";
    private static final String WEBSOCKET_EXTENSIONS_HEADER = "Sec-WebSocket-Extensions: permessage-deflate";
    private static final String WEBSOCKET_UPGRADE_HEADER = "upgrade: websocket\r\n";
    private static final String WEBSOCKET_CONNECTION_HEADER = "connection: upgrade\r\n";

    private final List<Packet> packets;
    private List<Framedata> frames;

    @Getter
    private boolean parsed = false;
    private int httpEnd = -1;

    public WebSocketsParser(List<Packet> packets) {
        this.packets = packets;
        detectWebSockets();
    }

    private void detectWebSockets() {
        final List<Packet> clientHandshakePackets = packets.stream()
                .takeWhile(Packet::isIncoming)
                .collect(Collectors.toList());

        final String clientHandshake = getHandshake(clientHandshakePackets);
        if (clientHandshake == null) {
            return;
        }

        for (int i = clientHandshakePackets.size(); i < packets.size(); i++) {
            if (packets.get(i).getContentString().endsWith("\r\n\r\n")) {
                httpEnd = i + 1;
                break;
            }
        }

        if (httpEnd == -1) {
            return;
        }

        final List<Packet> serverHandshakePackets = packets.subList(clientHandshakePackets.size(), httpEnd);
        final String serverHandshake = getHandshake(serverHandshakePackets);
        if (serverHandshake == null) {
            return;
        }

        HandshakeImpl1Server serverHandshakeImpl = fillServerHandshake(serverHandshake);
        HandshakeImpl1Client clientHandshakeImpl = fillClientHandshake(clientHandshake);

        if (serverHandshakeImpl == null || clientHandshakeImpl == null) {
            return;
        }

        Draft_6455 draft = new Draft_6455(new PerMessageDeflateExtension());

        try {
            draft.acceptHandshakeAsServer(clientHandshakeImpl);
            draft.acceptHandshakeAsClient(clientHandshakeImpl, serverHandshakeImpl);
        } catch (InvalidHandshakeException e) {
            log.warn("WebSocket handshake", e);
            return;
        }

        final List<Packet> wsPackets = this.packets.subList(
                httpEnd,
                this.packets.size());

        final byte[] wsContent = wsPackets.stream()
                .map(Packet::getContent)
                .reduce(ArrayUtils::addAll)
                .orElse(null);

        if (wsContent == null) {
            return;
        }

        final ByteBuffer frame = ByteBuffer.wrap(wsContent);

        try {
            frames = draft.translateFrame(frame);
        } catch (InvalidDataException e) {
            log.warn("WebSocket data", e);
            return;
        }

        parsed = true;
    }

    public List<Packet> getParsedPackets() {
        if (!parsed) {
            throw new IllegalStateException("WS is not parsed");
        }

        final List<Packet> handshakes = packets.subList(0, httpEnd);
        List<Packet> newPackets = new ArrayList<>(handshakes.size() + frames.size());
        newPackets.addAll(handshakes);

        final Packet lastPacket = packets.get(packets.size() - 1);

        for (Framedata frame : frames) {
            if(frame instanceof DataFrame) {
                newPackets.add(Packet.builder()
                        .content(frame.getPayloadData().array())
                        .incoming(true) //TODO
                        .timestamp(lastPacket.getTimestamp())
                        .ttl(lastPacket.getTtl())
                        .ungzipped(lastPacket.isUngzipped())
                        .build()
                );
            }
        }

        return newPackets;
    }

    private String getHandshake(final List<Packet> packets) {
        final String handshake = packets.stream()
                .map(Packet::getContent)
                .reduce(ArrayUtils::addAll)
                .map(String::new)
                .orElse(null);

        if (handshake == null ||
                !handshake.toLowerCase().contains(WEBSOCKET_CONNECTION_HEADER) ||
                !handshake.toLowerCase().contains(WEBSOCKET_UPGRADE_HEADER)) {
            return null;
        }

        if (!handshake.contains(WEBSOCKET_EXTENSION_HEADER) &&
                !handshake.contains(WEBSOCKET_EXTENSIONS_HEADER)) {
            return null;
        }

        return handshake;
    }

    private HandshakeImpl1Client fillClientHandshake(String clientHandshake) {
        Matcher matcher = WEBSOCKET_VERSION_PATTERN.matcher(clientHandshake);
        if (!matcher.find()) {
            return null;
        }
        String version = matcher.group(1);

        matcher = WEBSOCKET_KEY_PATTERN.matcher(clientHandshake);
        if (!matcher.find()) {
            return null;
        }
        String key = matcher.group(1);

        matcher = WEBSOCKET_EXTENSIONS_PATTERN.matcher(clientHandshake);
        if (!matcher.find()) {
            return null;
        }
        String extensions = matcher.group(1);

        HandshakeImpl1Client clientHandshakeImpl = new HandshakeImpl1Client();

        clientHandshakeImpl.put("Upgrade", "websocket");
        clientHandshakeImpl.put("Connection", "Upgrade");
        clientHandshakeImpl.put("Sec-WebSocket-Version", version);
        clientHandshakeImpl.put("Sec-WebSocket-Key", key);
        clientHandshakeImpl.put("Sec-WebSocket-Extensions", extensions);

        return clientHandshakeImpl;
    }

    private HandshakeImpl1Server fillServerHandshake(String serverHandshake) {
        Matcher matcher = WEBSOCKET_ACCEPT_PATTERN.matcher(serverHandshake);
        if (!matcher.find()) {
            return null;
        }
        String accept = matcher.group(1);

        matcher = WEBSOCKET_EXTENSIONS_PATTERN.matcher(serverHandshake);
        if (!matcher.find()) {
            return null;
        }
        String extensions = matcher.group(1);

        HandshakeImpl1Server serverHandshakeImpl = new HandshakeImpl1Server();

        serverHandshakeImpl.put("Upgrade", "websocket");
        serverHandshakeImpl.put("Connection", "Upgrade");
        serverHandshakeImpl.put("Sec-WebSocket-Accept", accept);
        serverHandshakeImpl.put("Sec-WebSocket-Extensions", extensions);

        return serverHandshakeImpl;
    }

}
