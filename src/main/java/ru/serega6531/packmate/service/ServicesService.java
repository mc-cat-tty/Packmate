package ru.serega6531.packmate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.serega6531.packmate.model.CtfService;
import ru.serega6531.packmate.model.enums.SubscriptionMessageType;
import ru.serega6531.packmate.model.pojo.SubscriptionMessage;
import ru.serega6531.packmate.repository.ServiceRepository;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class ServicesService {

    private final ServiceRepository repository;
    private final StreamSubscriptionService subscriptionService;

    private final Map<Integer, CtfService> services = new HashMap<>();

    @Autowired
    public ServicesService(ServiceRepository repository, StreamSubscriptionService subscriptionService) {
        this.repository = repository;
        this.subscriptionService = subscriptionService;

        repository.findAll().forEach(s -> services.put(s.getPort(), s));
        log.info("Loaded {} services", services.size());
    }

    public Optional<CtfService> findService(String localIp, String firstIp, int firstPort, String secondIp, int secondPort) {
        if (firstIp.equals(localIp)) {
            return findByPort(firstPort);
        } else if (secondIp.equals(localIp)) {
            return findByPort(secondPort);
        }

        return Optional.empty();
    }

    public Optional<CtfService> findByPort(int port) {
        return Optional.ofNullable(services.get(port));
    }

    public Collection<CtfService> findAll() {
        return services.values();
    }

    public void deleteByPort(int port) {
        log.info("Удален сервис на порту {}", port);
        services.remove(port);
        repository.deleteById(port);
        subscriptionService.broadcast(new SubscriptionMessage(SubscriptionMessageType.DELETE_SERVICE, port));
    }

    public CtfService save(CtfService service) {
        log.info("Добавлен или изменен сервис {} на порту {}", service.getName(), service.getPort());
        final CtfService saved = repository.save(service);
        services.put(saved.getPort(), service);
        subscriptionService.broadcast(new SubscriptionMessage(SubscriptionMessageType.SAVE_SERVICE, saved));
        return saved;
    }

}
