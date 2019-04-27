package ru.serega6531.packmate.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.serega6531.packmate.model.CtfService;
import ru.serega6531.packmate.service.ServicesService;

import java.util.List;

@RestController
@RequestMapping("/api/service/")
public class ServiceController {

    private final ServicesService service;

    @Autowired
    public ServiceController(ServicesService service) {
        this.service = service;
    }

    @GetMapping
    public List<CtfService> getServices() {
        return service.findAll();
    }

    @DeleteMapping("/{port}")
    public void deleteService(@PathVariable int port) {
        service.deleteById(port);
    }

    @PostMapping
    public CtfService addService(@RequestBody CtfService ctfService) {
        return service.save(ctfService);
    }

}
