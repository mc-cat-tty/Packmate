package ru.serega6531.packmate;

import org.pcap4j.core.PcapNativeException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@SpringBootApplication
public class PackmateApplication {

    public static void main(String[] args) {
        SpringApplication.run(PackmateApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void afterStartup(ApplicationReadyEvent event) throws PcapNativeException {
        final PcapWorker pcapWorker = event.getApplicationContext().getBean(PcapWorker.class);
        pcapWorker.start();
    }

}
