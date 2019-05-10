package ru.serega6531.packmate;

import org.pcap4j.core.PcapNativeException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

@SpringBootApplication
@EnableScheduling
@EnableWebSocket
public class PackmateApplication {

    @Value("${enable-capture}")
    private boolean enableCapture;

    public static void main(String[] args) {
        SpringApplication.run(PackmateApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void afterStartup(ApplicationReadyEvent event) throws PcapNativeException {
        if(enableCapture) {
            final PcapWorker pcapWorker = event.getApplicationContext().getBean(PcapWorker.class);
            pcapWorker.start();
        }
    }

}
