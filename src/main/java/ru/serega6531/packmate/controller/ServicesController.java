package ru.serega6531.packmate.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.serega6531.packmate.model.Service;
import ru.serega6531.packmate.repository.ServiceRepository;

import java.util.List;

@RestController
@RequestMapping("/api/service/manage")
public class ServicesController {

    private final ServiceRepository repository;

    @Autowired
    public ServicesController(ServiceRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Service> getServices() {
        return repository.findAll();
    }

    @DeleteMapping("/{port}")
    public void deleteService(@PathVariable int port) {
        repository.deleteById(port);
    }

    @PostMapping
    public List<Service> addService(@RequestBody Service service) {
        repository.save(service);
        return getServices();
    }

}
