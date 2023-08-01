package ru.serega6531.packmate.properties;


import org.springframework.boot.context.properties.ConfigurationProperties;
import ru.serega6531.packmate.model.enums.CaptureMode;

import java.net.InetAddress;
import java.time.Duration;

@ConfigurationProperties("packmate")
public record PackmateProperties(
    CaptureMode captureMode,
    String interfaceName,
    String pcapFile,
    InetAddress localIp,
    WebProperties web,
    TimeoutProperties timeout,
    CleanupProperties cleanup,
    boolean ignoreEmptyPackets
) {

    public record WebProperties(
            String accountLogin,
            String accountPassword
    ) {}

    public record TimeoutProperties(
            Duration udpStreamTimeout,
            Duration tcpStreamTimeout,
            int checkInterval
    ){}

    public record CleanupProperties(
            boolean enabled,
            int threshold,
            int interval
    ){}

}
