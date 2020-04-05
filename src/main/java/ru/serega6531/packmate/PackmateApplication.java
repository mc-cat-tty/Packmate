package ru.serega6531.packmate;

import org.pcap4j.core.PcapNativeException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import ru.serega6531.packmate.model.enums.CaptureMode;
import ru.serega6531.packmate.service.PcapService;

@SpringBootApplication
public class PackmateApplication {

    @Value("${enable-capture}")
    private boolean enableCapture;

    @Value("${capture-mode}")
    private CaptureMode captureMode;

    public static void main(String[] args) {
        SpringApplication.run(PackmateApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void afterStartup(ApplicationReadyEvent event) throws PcapNativeException {
        if (enableCapture && captureMode == CaptureMode.LIVE) {
            final PcapService pcapService = event.getApplicationContext().getBean(PcapService.class);
            pcapService.start();
        }
    }

}
