package ru.serega6531.packmate.service;

import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import ru.serega6531.packmate.model.CtfService;
import ru.serega6531.packmate.model.enums.SubscriptionMessageType;
import ru.serega6531.packmate.model.pojo.ServiceDto;
import ru.serega6531.packmate.model.pojo.SubscriptionMessage;
import ru.serega6531.packmate.repository.ServiceRepository;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class ServicesService {

    private final ServiceRepository repository;
    private final SubscriptionService subscriptionService;
    private final PcapService pcapService;

    private final InetAddress localIp;

    private final Map<Integer, CtfService> services = new HashMap<>();
    private final ModelMapper modelMapper;

    @Autowired
    public ServicesService(ServiceRepository repository,
                           SubscriptionService subscriptionService,
                           @Lazy PcapService pcapService,
                           ModelMapper modelMapper,
                           @Value("${local-ip}") String localIpString) throws UnknownHostException {
        this.repository = repository;
        this.subscriptionService = subscriptionService;
        this.pcapService = pcapService;
        this.modelMapper = modelMapper;
        this.localIp = InetAddress.getByName(localIpString);

        repository.findAll().forEach(s -> services.put(s.getPort(), s));
        log.info("Loaded {} services", services.size());
    }

    public CtfService find(int id) {
        return services.get(id);
    }

    public Optional<CtfService> findService(InetAddress firstIp, int firstPort, InetAddress secondIp, int secondPort) {
        if (firstIp.equals(localIp)) {
            return findByPort(firstPort);
        } else if (secondIp.equals(localIp)) {
            return findByPort(secondPort);
        }

        return Optional.empty();
    }

    private Optional<CtfService> findByPort(int port) {
        return Optional.ofNullable(services.get(port));
    }

    public Collection<CtfService> findAll() {
        return services.values();
    }

    public void deleteByPort(int port) {
        log.info("Removed service at port {}", port);

        services.remove(port);
        repository.deleteById(port);

        subscriptionService.broadcast(new SubscriptionMessage(SubscriptionMessageType.DELETE_SERVICE, port));

        pcapService.updateFilter(findAll());
    }

    public CtfService save(CtfService service) {
        log.info("Added or edited service '{}' at port {}", service.getName(), service.getPort());

        final CtfService saved = repository.save(service);
        services.put(saved.getPort(), saved);

        subscriptionService.broadcast(new SubscriptionMessage(SubscriptionMessageType.SAVE_SERVICE, toDto(saved)));

        pcapService.updateFilter(findAll());

        return saved;
    }

    public ServiceDto toDto(CtfService service) {
        return modelMapper.map(service, ServiceDto.class);
    }

    public CtfService fromDto(ServiceDto dto) {
        return modelMapper.map(dto, CtfService.class);
    }

}
