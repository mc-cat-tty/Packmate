package ru.serega6531.packmate.configuration;

import org.modelmapper.ModelMapper;
import org.pcap4j.core.PcapNativeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import ru.serega6531.packmate.model.enums.CaptureMode;
import ru.serega6531.packmate.pcap.FilePcapWorker;
import ru.serega6531.packmate.pcap.LivePcapWorker;
import ru.serega6531.packmate.pcap.NoOpPcapWorker;
import ru.serega6531.packmate.pcap.PcapWorker;
import ru.serega6531.packmate.service.ServicesService;
import ru.serega6531.packmate.service.StreamService;
import ru.serega6531.packmate.service.SubscriptionService;

import java.net.UnknownHostException;

@Configuration
@EnableScheduling
@EnableAsync
public class ApplicationConfiguration {

    @Bean(destroyMethod = "stop")
    @Autowired
    public PcapWorker pcapWorker(ServicesService servicesService,
                                 StreamService streamService,
                                 SubscriptionService subscriptionService,
                                 @Value("${local-ip}") String localIpString,
                                 @Value("${interface-name}") String interfaceName,
                                 @Value("${pcap-file}") String filename,
                                 @Value("${capture-mode}") CaptureMode captureMode) throws PcapNativeException, UnknownHostException {
        return switch (captureMode) {
            case LIVE -> new LivePcapWorker(servicesService, streamService, localIpString, interfaceName);
            case FILE -> new FilePcapWorker(servicesService, streamService, subscriptionService, localIpString, filename);
            case VIEW -> new NoOpPcapWorker();
        };
    }

    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }

}
