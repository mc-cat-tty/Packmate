package ru.serega6531.packmate.tasks;

import org.pcap4j.core.PcapNativeException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import ru.serega6531.packmate.model.enums.CaptureMode;
import ru.serega6531.packmate.service.PcapService;
import ru.serega6531.packmate.service.ServicesService;

@Component
public class StartupListener {

    @Value("${enable-capture}")
    private boolean enableCapture;

    @Value("${capture-mode}")
    private CaptureMode captureMode;

    private final PcapService pcapService;
    private final ServicesService servicesService;

    public StartupListener(PcapService pcapService, ServicesService servicesService) {
        this.pcapService = pcapService;
        this.servicesService = servicesService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void afterStartup() throws PcapNativeException {
        if (enableCapture) {
            servicesService.updateFilter();

            if (captureMode == CaptureMode.LIVE) {
                pcapService.start();
            }
        }
    }

}
