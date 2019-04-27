package ru.serega6531.packmate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.serega6531.packmate.model.CtfService;
import ru.serega6531.packmate.repository.ServiceRepository;

import java.util.List;

@Service
@Slf4j
public class ServicesService {

    private final ServiceRepository repository;

    @Autowired
    public ServicesService(ServiceRepository repository) {
        this.repository = repository;
    }

    public List<CtfService> findAll() {
        return repository.findAll();
    }

    public void deleteById(int id) {
        repository.deleteById(id);
    }

    public CtfService save(CtfService service) {
        log.info("Добавлен новый сервис {} на порту {}", service.getName(), service.getPort());
        return repository.save(service);
    }

}
