package ru.serega6531.packmate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.serega6531.packmate.model.CtfService;
import ru.serega6531.packmate.repository.ServiceRepository;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class ServicesService {

    private final ServiceRepository repository;
    private final Map<Integer, CtfService> services = new HashMap<>();

    @Autowired
    public ServicesService(ServiceRepository repository) {
        this.repository = repository;
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
    }

    public CtfService save(CtfService service) {
        log.info("Добавлен новый сервис {} на порту {}", service.getName(), service.getPort());
        services.put(service.getPort(), service);
        return repository.save(service);
    }

}
